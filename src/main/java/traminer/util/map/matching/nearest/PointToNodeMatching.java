package traminer.util.map.matching.nearest;

import traminer.util.exceptions.MapMatchingException;
import traminer.util.map.matching.MapMatchingMethod;
import traminer.util.map.matching.PointNodePair;
import traminer.util.map.roadnetwork.RoadNetworkGraph;
import traminer.util.map.roadnetwork.RoadNode;
import traminer.util.spatial.distance.EuclideanDistanceFunction;
import traminer.util.spatial.distance.PointDistanceFunction;
import traminer.util.spatial.objects.st.STPoint;
import traminer.util.trajectory.Trajectory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Implementation of Nearest-Neighbor Point-to-Point map matching.
 * Simply match every trajectory point to its nearest node in the  
 * road network graph.
 * <p>
 * Note that this algorithm does not take the road network nodes
 * connectivity into account.
 * 
 * @author uqdalves
 */
@SuppressWarnings("serial")
public class PointToNodeMatching implements MapMatchingMethod {
    /**
     * The distance method to use between points
     */
    private final PointDistanceFunction distanceFunction;

    /**
     * Creates a new nearest map-matching method with default
     * Euclidean distance function.
     */
    public PointToNodeMatching() {
        this.distanceFunction = new EuclideanDistanceFunction();
    }

    /**
     * Creates a new nearest map-matching method with the
     * given distance function.
     *
     * @param distFunc The point-to-node distance function to use.
     */
    public PointToNodeMatching(PointDistanceFunction distFunc) {
        this.distanceFunction = distFunc;
    }

    @Override
    public List<PointNodePair> doMatching(
            final Trajectory trajectory,
            final RoadNetworkGraph roadNetworkGraph) throws MapMatchingException {
        // make sure there is data to match
        if (trajectory == null || trajectory.isEmpty()) {
            throw new MapMatchingException(
                    "Trajectory for map-matching must not be empty nor null.");
        }
        if (roadNetworkGraph == null || roadNetworkGraph.isEmpty()) {
            throw new MapMatchingException(
                    "Road-Network-Graph for map-matching must not be empty nor null.");
        }

        // find the closest road node from p
        List<PointNodePair> result = new ArrayList<>(trajectory.size());
        double dist, minDist;
        RoadNode nearestNode;
        for (STPoint p : trajectory) {
            dist = minDist = INFINITY;
            nearestNode = null;
            for (RoadNode node : roadNetworkGraph.getNodes()) {
                dist = distanceFunction.pointToPointDistance(
                        node.lon(), node.lat(), p.x(), p.y());
                if (dist < minDist) {
                    minDist = dist;
                    nearestNode = node;
                }
            }
            // make sure it returns a copy of the objects
            if (nearestNode != null) {
                result.add(new PointNodePair(p.clone(), nearestNode.clone(), dist));
            }
        }

        return result;
    }

    @Override
    public List<PointNodePair> doMatching(
            final Collection<STPoint> pointsList,
            final Collection<RoadNode> nodesList) throws MapMatchingException {
        // make sure there is data to match
        if (pointsList == null || pointsList.isEmpty()) {
            throw new MapMatchingException(
                    "Points list for map-matching must not be empty nor null.");
        }
        if (nodesList == null || nodesList.isEmpty()) {
            throw new MapMatchingException(
                    "Nodes list for map-matching must not be empty nor null.");
        }

        // simply match every point to its closest node
        List<PointNodePair> matchedPairs = new ArrayList<>();
        double dist, minDist;
        RoadNode nearestNode;
        for (STPoint p : pointsList) {
            dist = minDist = INFINITY;
            nearestNode = null;
            for (RoadNode node : nodesList) {
                dist = distanceFunction.pointToPointDistance(
                        node.lon(), node.lat(), p.x(), p.y());
                if (dist < minDist) {
                    minDist = dist;
                    nearestNode = node;
                }
            }
            // make sure it returns a copy of the objects
            if (nearestNode != null) {
                matchedPairs.add(new PointNodePair(p.clone(), nearestNode.clone(), dist));
            }
        }

        return matchedPairs;
    }
}
