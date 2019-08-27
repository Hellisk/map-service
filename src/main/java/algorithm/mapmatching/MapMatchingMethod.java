package algorithm.mapmatching;

import org.apache.log4j.Logger;
import util.object.spatialobject.Trajectory;
import util.object.structure.SimpleTrajectoryMatchResult;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Parent class of all map-matching algorithms.
 *
 * @author uqpchao
 * Created 26/08/2019
 */
public abstract class MapMatchingMethod {
	
	private static final Logger LOG = Logger.getLogger(MapMatchingMethod.class);
	
	/**
	 * The map-matching function used in offline scenario.
	 *
	 * @param traj Input trajectory.
	 * @return Output map-matching result. Must include route match result in offline matching.
	 */
	public SimpleTrajectoryMatchResult offlineMatching(Trajectory traj) {
		LOG.error("Offline map-matching is not supported");
		return new SimpleTrajectoryMatchResult("", new ArrayList<>(), new ArrayList<>());
	}
	
	/**
	 * The map-matching function used in offline scenario.
	 *
	 * @param traj Input trajectory.
	 * @return Output map-matching result. Must include route match result in offline matching.
	 */
	public SimpleTrajectoryMatchResult onlineMatching(Trajectory traj) {
		LOG.error("Online map-matching is not supported");
		return new SimpleTrajectoryMatchResult("", new ArrayList<>(), new ArrayList<>());
	}
	
	/**
	 * Conduct map-matching in parallel mode.
	 *
	 * @param inputTrajectory The input trajectory stream.
	 * @param numOfThreads    The required number of threads. =-1 if full utilisation is expected.
	 * @param isOnline        If the current map-matching process is online or offline.
	 * @return List of map-matching results.
	 * @throws ExecutionException   Errors during parallel processing.
	 * @throws InterruptedException Concurrent error.
	 */
	List<SimpleTrajectoryMatchResult> parallelMatching(Stream<Trajectory> inputTrajectory, int numOfThreads, boolean isOnline)
			throws ExecutionException, InterruptedException {
		
		if (inputTrajectory == null) {
			throw new IllegalArgumentException("Trajectory stream for map-matching must not be null.");
		}
		
		// parallel processing
		ForkJoinPool forkJoinPool;
		if (numOfThreads == -1) {
			forkJoinPool = ForkJoinPool.commonPool();
		} else {
			forkJoinPool = new ForkJoinPool(numOfThreads);
		}
		LOG.info("Current number of threads for map-matching: " + forkJoinPool.getPoolSize());
		ForkJoinTask<Stream<SimpleTrajectoryMatchResult>> taskResult =
				forkJoinPool.submit(() -> inputTrajectory.parallel().map(isOnline ? this::onlineMatching : this::offlineMatching));
		while (!taskResult.isDone())
			Thread.sleep(5);
		return taskResult.get().collect(Collectors.toList());
	}
	
	/**
	 * Conduct map-matching in sequential mode.
	 *
	 * @param inputTrajectory The input trajectory list.
	 * @param isOnline        If the current map-matching process is online or offline.
	 * @return List of map-matching results.
	 */
	List<SimpleTrajectoryMatchResult> sequentialMatching(List<Trajectory> inputTrajectory, boolean isOnline) {
		if (inputTrajectory == null) {
			throw new IllegalArgumentException("Trajectory list for map-matching must not be null.");
		}
		int trajSize = inputTrajectory.size();
		
		List<SimpleTrajectoryMatchResult> resultList = new ArrayList<>();
		int completeCount = 0;
		for (Trajectory currTraj : inputTrajectory) {
			if (isOnline)
				resultList.add(onlineMatching(currTraj));
			else
				resultList.add(offlineMatching(currTraj));
			completeCount++;
			if (completeCount % (trajSize / 100) == 0)
				LOG.info(completeCount / (trajSize / 100) + "% of trajectories are map-matched.");
		}
		return resultList;
	}
}
