package traminer.util.map.matching;

import traminer.util.exceptions.MapMatchingException;
import traminer.util.map.MapInterface;
import traminer.util.map.roadnetwork.RoadNetworkGraph;
import traminer.util.map.roadnetwork.RoadNode;
import traminer.util.map.roadnetwork.RoadWay;
import traminer.util.spatial.objects.st.STPoint;
import traminer.util.trajectory.Trajectory;

import java.util.Collection;
import java.util.List;

/**
 * Interface for map-matching algorithms.
 *
 * @author uqdalves
 */
public interface MapMatchingMethod extends MapInterface {
    /**
     * Matches the trajectory sample points to the
     * road network graph.
     *
     * @param trajectory       The trajectory to match.
     * @param roadNetwrokGraph The road network graph to find the match.
     * @return A road network way that best matches the trajectory.
     * @throws MapMatchingException
     */
    RoadWay doMatching(
            final Trajectory trajectory,
            final RoadNetworkGraph roadNetwrokGraph) throws MapMatchingException;

    /**
     * Matches the given list of spatial points to the
     * road network nodes.
     *
     * @param pointsList The spatial points to match.
     * @param nodesList  The road network nodes to find the match.
     * @return A list containing the road nodes that best
     * matches each point in the given collection.
     * @throws MapMatchingException
     */
    List<MatchPair> doMatching(
            final Collection<STPoint> pointsList,
            final Collection<RoadNode> nodesList) throws MapMatchingException;
}
