package traminer.util.map.matching;

import traminer.util.exceptions.MapMatchingException;
import traminer.util.map.MapInterface;
import traminer.util.map.matching.parallel.ParallelMapMatching;
import traminer.util.map.matching.parallel.SpatialAwareMatching;
import traminer.util.map.roadnetwork.RoadNetworkGraph;
import traminer.util.map.roadnetwork.RoadWay;
import traminer.util.spatial.structures.SpatialIndexModel;
import traminer.util.trajectory.Trajectory;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * A Map-Matching service to match trajectories
 * to a road network graph.
 *
 * @author uqdalves
 */
@SuppressWarnings("serial")
public final class MapMatchingService implements MapInterface {
    /**
     * The map matching algorithm to use in this service.
     */
    private final MapMatchingMethod matchingMethod;

    /**
     * Creates a new map-matching service to match trajectories
     * to a road network graph.
     *
     * @param matchingMethod The map matching algorithm
     *                       to use in this service.
     */
    public MapMatchingService(MapMatchingMethod matchingMethod) {
        this.matchingMethod = matchingMethod;
    }

    /**
     * Sequentially match every trajectory in the Stream
     * to the road network graph.
     *
     * @param trajectories     The stream of trajectories to match.
     * @param roadNetworkGraph The road network graph to match to.
     * @return A Stream of road ways, the ID of each way is the ID of
     * its corresponding trajectory.
     */
    public Stream<RoadWay> doMatching(
            final Stream<Trajectory> trajectories,
            final RoadNetworkGraph roadNetworkGraph) {
        Stream<RoadWay> matchWays =
                trajectories.sequential().map(new Function<Trajectory, RoadWay>() {
                    @Override
                    public RoadWay apply(Trajectory trajectory) {
                        RoadWay matchWay = matchingMethod.doMatching(
                                trajectory, roadNetworkGraph);
                        matchWay.setId(trajectory.getId());
                        return matchWay;
                    }
                });
        return matchWays;
    }

    /**
     * Match every trajectory in the stream to the road network
     * graph in a parallel fashion.
     *
     * @param trajectories     The stream of trajectories to match.
     * @param roadNetworkGraph The road network graph to match to.
     * @param numThreads       Number of parallel threads.
     * @return A Stream of road ways, the ID of each way
     * is the ID of its corresponding trajectory.
     */
    public Stream<RoadWay> doParallelMatching(
            final Stream<Trajectory> trajectories,
            final RoadNetworkGraph roadNetworkGraph,
            final int numThreads) {
        ParallelMapMatching mapMatching =
                new ParallelMapMatching(matchingMethod);
        try {
            Stream<RoadWay> result = mapMatching
                    .doMatching(trajectories, roadNetworkGraph, numThreads);
            return result;
        } catch (InterruptedException | ExecutionException e) {
            throw new MapMatchingException(
                    "Error executing parallel map-matghing.");
        }
    }

    /**
     * Perform a spatial-aware map-matching. Partition the
     * data space and match every trajectory to the road
     * network graph in parallel.
     * <p>
     * This method uses a spatial partitioning model to divide
     * the data space, and performs the map-matching in each
     * spatial partition in a parallel fashion.
     *
     * @param trajectories     The stream of trajectories to match.
     * @param roadNetworkGraph The road network graph to match to.
     * @param spatialModel     The space partitioning model to use.
     * @return A Map of road ways, key (trajectory ID), value (match road way).
     */
    public Map<String, RoadWay> doSpatialAwareMatching(
            final Stream<Trajectory> trajectories,
            final RoadNetworkGraph roadNetworkGraph,
            final SpatialIndexModel spatialModel) {
        SpatialAwareMatching mapMatching =
                new SpatialAwareMatching(matchingMethod, spatialModel);
        return mapMatching.doMatching(trajectories, roadNetworkGraph);
    }
}
