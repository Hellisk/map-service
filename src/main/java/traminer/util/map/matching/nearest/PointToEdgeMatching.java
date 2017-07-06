package traminer.util.map.matching.nearest;

import traminer.util.exceptions.MapMatchingException;
import traminer.util.map.matching.MapMatchingMethod;
import traminer.util.map.matching.PointNodePair;
import traminer.util.map.roadnetwork.RoadNetworkGraph;
import traminer.util.map.roadnetwork.RoadNode;
import traminer.util.map.roadnetwork.RoadWay;
import traminer.util.spatial.distance.EuclideanDistanceFunction;
import traminer.util.spatial.distance.SegmentDistanceFunction;
import traminer.util.spatial.objects.st.STPoint;
import traminer.util.trajectory.Trajectory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Implementation of Nearest-Neighbor of Point-to-Curve map matching. 
 * Simply match every trajectory point to its nearest edge in the 
 * road network graph.
 * <p>
 * Note that this algorithm does not take the road network edges
 * connectivity into account.
 * 
 * @author uqdalves
 */
@SuppressWarnings("serial")
public class PointToEdgeMatching implements MapMatchingMethod {
    /**
     * The distance method to use between points and segments
     */
    private final SegmentDistanceFunction distanceFunction;

    /**
     * Creates a new map-matching method with default
     * Euclidean distance function.
     */
    public PointToEdgeMatching() {
        this.distanceFunction = new EuclideanDistanceFunction();
    }

    /**
     * Creates a new nearest map-matching method with the
     * given distance function.
     *
     * @param distFunc The point-to-edge distance function to use.
     */
    public PointToEdgeMatching(SegmentDistanceFunction distFunc) {
        this.distanceFunction = distFunc;
    }

    @Override
    public RoadWay doMatching(
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

        // find the closest road edge from p
        RoadWay resultWay = new RoadWay(trajectory.getId());
        double dist, minDist;
        RoadNode nearesti, nearestj;
        for (STPoint p : trajectory) {
            minDist = INFINITY;
            nearesti = nearestj = null;
            for (RoadWay way : roadNetworkGraph.getWays()) {
                // get road way edges
                RoadNode ni, nj;
                for (int i = 0; i < way.size() - 1; i++) {
                    ni = way.getNode(i);
                    nj = way.getNode(i + 1);
                    dist = distanceFunction.pointToSegmentDistance(p.x(), p.y(),
                            ni.lon(), ni.lat(), nj.lon(), nj.lat());
                    if (dist < minDist) {
                        minDist = dist;
                        nearesti = ni;
                        nearestj = nj;
                    }
                }
            }
            // map p to the edge ni -> nj
            if (nearesti != null && nearestj != null) {
                resultWay.addNode(nearesti);
                resultWay.addNode(nearestj);
            }
        }

        return resultWay;
    }

    // nodes list is taken as a sorted list of Way points
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

        // simply match every point to its closest edge
        List<PointNodePair> matchedPairs = new ArrayList<>();
        double dist, minDist;
        RoadNode nearesti, nearestj;
        Iterator<RoadNode> nodesItr = nodesList.iterator();
        for (STPoint p : pointsList) {
            minDist = INFINITY;
            nearesti = nearestj = null;
            // get road edges
            RoadNode ni, nj;
            ni = nodesItr.next();
            while (nodesItr.hasNext()) {
                nj = nodesItr.next();
                dist = distanceFunction.pointToSegmentDistance(p.x(), p.y(),
                        ni.lon(), ni.lat(), nj.lon(), nj.lat());
                if (dist < minDist) {
                    minDist = dist;
                    nearesti = ni;
                    nearestj = nj;
                }
                ni = nj;
            }
            // map p to the edge ni -> nj
            if (nearesti != null && nearestj != null) {
                matchedPairs.add(new PointNodePair(p, nearesti.clone()));
                matchedPairs.add(new PointNodePair(p, nearestj.clone()));
            }
        }

        return matchedPairs;
    }
}
