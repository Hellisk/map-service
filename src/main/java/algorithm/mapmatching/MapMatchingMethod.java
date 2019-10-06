package algorithm.mapmatching;

import org.apache.log4j.Logger;
import util.object.spatialobject.Trajectory;
import util.object.structure.Pair;
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
public interface MapMatchingMethod {

	Logger LOG = Logger.getLogger(MapMatchingMethod.class);

	/**
	 * The map-matching function used in offline scenario.
	 *
	 * @param traj Input trajectory.
	 * @return Output map-matching result. Must include route match result in offline matching.
	 */
	default SimpleTrajectoryMatchResult offlineMatching(Trajectory traj) {
		LOG.error("Offline map-matching is not supported");
		return new SimpleTrajectoryMatchResult("", new ArrayList<>(), new ArrayList<>());
	}

	/**
	 * The map-matching function used in offline scenario.
	 *
	 * @param traj Input trajectory.
	 * @return Output map-matching result. Must include route match result in offline matching.
	 */
	default Pair<List<Double>, SimpleTrajectoryMatchResult> onlineMatching(Trajectory traj) {
		LOG.error("Online map-matching is not supported");
		return new Pair<>(new ArrayList<>(),
				new SimpleTrajectoryMatchResult("", new ArrayList<>(), new ArrayList<>()));
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
	default List<SimpleTrajectoryMatchResult> parallelMatching(Stream<Trajectory> inputTrajectory,
															   int numOfThreads, boolean isOnline)
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
		List<SimpleTrajectoryMatchResult> result = new ArrayList<>();
		if (isOnline) {
			ForkJoinTask<Stream<Pair<List<Double>, SimpleTrajectoryMatchResult>>> taskResult =
					forkJoinPool.submit(() -> inputTrajectory.parallel().map(this::onlineMatching));

			LOG.info("Current number of threads for map-matching: " + forkJoinPool.getParallelism());
			while (!taskResult.isDone())
				Thread.sleep(5);

			// pull matching result
			List<Pair<List<Double>, SimpleTrajectoryMatchResult>> tempRes = taskResult.get().collect(Collectors.toList());
			for (Pair<List<Double>, SimpleTrajectoryMatchResult> tempRe : tempRes) {
				result.add(tempRe._2());
			}

			/* summarize latency */
			List<Double> latencies = new ArrayList<>();
			for (Pair<List<Double>, SimpleTrajectoryMatchResult> tempRe : tempRes) {
				latencies.addAll(tempRe._1());
			}
			if (latencies.size() > 0) {
				// get mean value
				double sum = 0;
				for (Double latency : latencies) {
					sum += latency;
				}
				LOG.info("Mean of latency is " + sum / latencies.size());
			}

		} else {
			ForkJoinTask<Stream<SimpleTrajectoryMatchResult>> taskResult =
					forkJoinPool.submit(() -> inputTrajectory.parallel().map(this::offlineMatching));

			LOG.info("Current number of threads for map-matching: " + forkJoinPool.getParallelism());
			while (!taskResult.isDone())
				Thread.sleep(5);
			result = taskResult.get().collect(Collectors.toList());
		}
		return result;
	}

	/**
	 * Conduct map-matching in sequential mode.
	 *
	 * @param inputTrajectory The input trajectory list.
	 * @param isOnline        If the current map-matching process is online or offline.
	 * @return List of map-matching results.
	 */
	default List<SimpleTrajectoryMatchResult> sequentialMatching(List<Trajectory> inputTrajectory, boolean isOnline) {
		if (inputTrajectory == null) {
			throw new IllegalArgumentException("Trajectory list for map-matching must not be null.");
		}
		int trajSize = inputTrajectory.size();
		double currPercentage = 0;
		List<SimpleTrajectoryMatchResult> resultList = new ArrayList<>();
		int completeCount = 0;
		for (Trajectory currTraj : inputTrajectory) {
			if (currTraj.getID().equals("1953"))
				System.out.println("TEST");
			if (isOnline) {
				resultList.add(onlineMatching(currTraj)._2());
			} else
				resultList.add(offlineMatching(currTraj));
			completeCount++;
			if (currPercentage != Math.floor(completeCount / ((double) trajSize / 100))) {
				currPercentage = Math.floor(completeCount / ((double) trajSize / 100));
				LOG.info(currPercentage + "% of trajectories are map-matched.");
			}
		}
		return resultList;
	}

    default double trajectoryTime(Trajectory trajectory) {
        return trajectory.get(trajectory.size() - 1).time() - trajectory.get(0).time();
    }
}
