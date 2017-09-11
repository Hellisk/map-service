package traminer.util.map.matching.parallel;

import traminer.util.exceptions.MapMatchingException;
import traminer.util.map.MapInterface;
import traminer.util.map.matching.MapMatchingMethod;
import traminer.util.map.matching.PointNodePair;
import traminer.util.map.roadnetwork.RoadNetworkGraph;
import traminer.util.map.roadnetwork.RoadNode;
import traminer.util.map.roadnetwork.RoadWay;
import traminer.util.spatial.objects.st.STPoint;
import traminer.util.spatial.structures.SpatialIndexModel;
import traminer.util.trajectory.Trajectory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Service to perform spatial-aware map-matching. 
 * <p>
 * Partition the data space and match every trajectory to the road 
 * network graph in parallel. 
 * <p>
 * This method uses a spatial partitioning model to divide 
 * the data space, and performs the map-matching in each 
 * spatial partition in a parallel fashion. 
 * 
 * @author uqdalves
 */
@SuppressWarnings("serial")
public class SpatialAwareMatching implements MapInterface {
    /**
     * The map-matching method to use
     */
    private final MapMatchingMethod matchingMethod;
    /**
     * The space partitioning model to use
     */
    private final SpatialIndexModel spatialModel;
    /**
     * Spatial partitions
     */
    private volatile HashMap<String, MatchPartition> partitionMap;
    /**
     * Matched ways result
     */
    private volatile HashMap<String, RoadWay> resultWays;

    /**
     * Creates a new service to perform spatial-aware map-matching.
     *
     * @param matchingMethod The map-matching method to use.
     * @param spatialModel   The space partitioning model to use.
     */
    public SpatialAwareMatching(
            MapMatchingMethod matchingMethod,
            SpatialIndexModel spatialModel) {
        this.matchingMethod = matchingMethod;
        this.spatialModel = spatialModel;
        this.partitionMap = new HashMap<>();
        this.resultWays = new HashMap<>();
    }

    /**
     * Match every trajectory in the stream to the road network
     * graph in parallel. This method uses a spatial index model
     * to divide the data space, and performs the map-matching
     * in each spatial partition in a parallel fashion.
     * <p>
     * This method uses boundary objects replication by default.
     * Therefore the result map may contain more than one copy
     * of the matched trajectories.
     *
     * @param trajectories     The stream of trajectories to match.
     * @param roadNetworkGraph The road network graph to match to.
     * @return A Map of road ways, key (trajectory ID), value (match road way).
     * @throws MapMatchingException
     */
    public Map<String, RoadWay> doMatching(
            Stream<Trajectory> trajectories,
            RoadNetworkGraph roadNetworkGraph) throws MapMatchingException {
        // make sure there is data to match
        if (trajectories == null) {
            throw new MapMatchingException(
                    "Trajectory stream for map-matching must not be null.");
        }
        if (roadNetworkGraph == null || roadNetworkGraph.isEmpty()) {
            throw new MapMatchingException(
                    "Road-Network-Graph for map-matching must not be empty nor null.");
        }

        // map each road node to its intersecting partition
        roadNetworkGraph.getNodes().parallelStream()
                .filter(node -> (node != null))
                .forEach(node -> {
                    String index = spatialModel.search(node.lon(), node.lat());
                    // adds the node to its partition. Creates the
                    // partition if it doesn't exist.
                    synchronized (partitionMap) {
                        if (partitionMap.containsKey(index)) {
                            partitionMap.get(index).addNode(node);
                        }
                        // creates and add a new partition
                        else {
                            MatchPartition partition = new MatchPartition();
                            partition.addNode(node);
                            partitionMap.put(index, partition);
                        }
                    }
                });

        println("Nodes partitioning completed..");

        // map each trajectory point to its intersecting partition
        trajectories.parallel()
                .filter(trajectory -> !(trajectory == null || trajectory.isEmpty()))
                .forEach(trajectory -> {
                    for (STPoint p : trajectory) {
                        String index = spatialModel.search(p.x(), p.y());
                        p.setParentId(trajectory.getId());
                        // adds the point to its partition. Creates the
                        // partition if it doesn't exist.
                        synchronized (partitionMap) {
                            if (partitionMap.containsKey(index)) {
                                partitionMap.get(index).addPoint(p);
                            }
                            // creates a new partition and add
                            else {
                                MatchPartition partition = new MatchPartition();
                                partition.addPoint(p);
                                partitionMap.put(index, partition);
                            }
                        }
                    }
                });

        println("Trajectories partitioning completed..");

        // do map matching in each spatial partition
        partitionMap.values().parallelStream()
                // select only partitions that contains data to match
                .filter(partition -> !(partition == null || partition.isBroken()))
                // do the matching
                .flatMap(partition -> {
                    List<PointNodePair> matchNodes = matchingMethod
                            .doMatching(partition.pointList, partition.nodeList);
                    return matchNodes.stream();

                })
                // group nodes by parent trajectory ID
                .forEach(matchPair -> {
                    // the trajectory this point belongs to
                    String parentId = matchPair.getPoint().getParentId();
                    // the node matched to this point
                    RoadNode node = matchPair.getNode();
                    synchronized (partitionMap) {
                        // group nodes by trajectory id
                        if (resultWays.containsKey(parentId)) {
                            resultWays.get(parentId).addNode(node);
                        } else {
                            RoadWay way = new RoadWay(parentId);
                            way.addNode(node);
                            resultWays.put(parentId, way);
                        }
                    }
                });

        // post-process the result (sort the way nodes)
        resultWays.values().forEach(way -> way.sortByTimeStamp());

        println("Matching completed..");

        return resultWays;
    }

    /**
     * A partition for parallel map-matching.
     * Contains the list of road network nodes and
     * trajectory points in a spatial partition.
     *
     * @author uqdalves
     */
    private final class MatchPartition implements Serializable {
        /**
         * List of trajectory points in this partition
         */
        public List<STPoint> pointList = new ArrayList<>();
        /** List of road nodes in this partition */
        public List<RoadNode> nodeList = new ArrayList<>();

        /**
         * Add a trajectory point to this partition.
         * @param point
         */
        public void addPoint(STPoint point) {
            if (point != null) pointList.add(point);
        }

        /**
         * Add a road node to this partition.
         * @param node
         */
        public void addNode(RoadNode node) {
            if (node != null) nodeList.add(node);
        }

        /**
         * Check whether it is possible to do a
         * matching in this partition, that is, if
         * this partition contains both points and nodes.
         *
         * @return True if this partition has neither points
         * nor nodes for map-matching, false otherwise.
         */
        public boolean isBroken() {
            if (pointList == null || pointList.isEmpty())
                return true;
            return nodeList == null || nodeList.isEmpty();
        }
    }
}
