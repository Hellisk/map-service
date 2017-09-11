package traminer.util.map.matching.parallel;

import traminer.util.exceptions.MapMatchingException;
import traminer.util.map.MapInterface;
import traminer.util.map.matching.MapMatchingMethod;
import traminer.util.map.matching.PointNodePair;
import traminer.util.map.roadnetwork.RoadNetworkGraph;
import traminer.util.trajectory.Trajectory;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.stream.Stream;

/**
 * A service to perform map-matching in parallel. 
 * <p>
 * This service broadcasts the road graph and map-matching
 * methods to all threads to avoid concurrency lock.
 * 
 * @author uqdalves
 */
@SuppressWarnings("serial")
public class ParallelMapMatching implements MapInterface {
    /**
     * The map-matching method to use
     */
    private final MapMatchingMethod matchingMethod;
    /**
     * broadcast the road network graph and
     * the map-matching method to all threads
     */
    private static final ThreadLocal<RoadNetworkGraph> roadGraphThread =
            new ThreadLocal<RoadNetworkGraph>();
    private static final ThreadLocal<MapMatchingMethod> matchingMethodThread =
            new ThreadLocal<MapMatchingMethod>();

    /**
     * Create a service to perform map-matching in parallel.
     *
     * @param matchingMethod The map matching algorithm
     *                       to use in this service.
     */
    public ParallelMapMatching(MapMatchingMethod matchingMethod) {
        this.matchingMethod = matchingMethod;
    }

    /**
     * Match every trajectory in the stream to the road network
     * graph in a parallel fashion.
     *
     * @param trajectories     The stream of trajectories to match.
     * @param roadNetworkGraph The road network graph to match to.
     * @param numThreads       Number of parallel threads.
     * @return A Stream with the lists of road nodes that best
     * matches each point of the trajectories.
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public Stream<List<PointNodePair>> doMatching(
            Stream<Trajectory> trajectories,
            RoadNetworkGraph roadNetworkGraph,
            final int numThreads) throws InterruptedException, ExecutionException, MapMatchingException {
        // make sure there is data to match
        if (trajectories == null) {
            throw new MapMatchingException(
                    "Trajectory stream for map-matching must not be null.");
        }
        if (roadNetworkGraph == null || roadNetworkGraph.isEmpty()) {
            throw new MapMatchingException(
                    "Road-Network-Graph for map-matching must not be empty nor null.");
        }

        ForkJoinPool forkJoinPool = new ForkJoinPool(numThreads);
        ForkJoinTask<Stream<List<PointNodePair>>> taskResult =
                forkJoinPool.submit(() -> {
                    matchingMethodThread.set(matchingMethod);
                    roadGraphThread.set(roadNetworkGraph);
                    Stream<List<PointNodePair>> matchPairsStream =
                            trajectories.parallel().map(trajectory -> {
                                List<PointNodePair> matchPairs = matchingMethodThread.get()
                                        .doMatching(trajectory, roadGraphThread.get());
                                try {
                                    Thread.sleep(5);
                                } catch (InterruptedException e) {
                                }
                                return matchPairs;
                            });
                    return matchPairsStream;
                });

        Stream<List<PointNodePair>> result = taskResult.get();

        return result;
    }
}
