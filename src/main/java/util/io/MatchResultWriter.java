package util.io;

import org.apache.log4j.Logger;
import util.function.DistanceFunction;
import util.object.roadnetwork.RoadNetworkGraph;
import util.object.roadnetwork.RoadNode;
import util.object.spatialobject.Trajectory;
import util.object.spatialobject.TrajectoryPoint;
import util.object.structure.*;

import java.io.File;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Stream;

/**
 * Write map-matching result to the file.
 *
 * @author uqpchao
 * Created 7/04/2019
 */
public class MatchResultWriter {
	
	private static final Logger LOG = Logger.getLogger(TrajectoryReader.class);
	
	/**
	 * Writer for writing matching results. The format follows the <tt>MultipleTrajectoryMatchResult.toString()</tt> format.
	 *
	 * @param matchingList The matching results.
	 * @param fileFolder   The output folder path.
	 */
	public static void writeMultipleMatchResults(List<MultipleTrajectoryMatchResult> matchingList, String fileFolder) {
		if (matchingList == null)
			throw new NullPointerException("The input matching result list is empty.");
		
		IOService.createFolder(fileFolder);
		IOService.cleanFolder(fileFolder);
		
		File folder = new File(fileFolder);
		Stream<MultipleTrajectoryMatchResult> trajectoryMatchingStream = matchingList.stream();
		
		// parallel processing
		ForkJoinPool forkJoinPool = ForkJoinPool.commonPool();
		forkJoinPool.submit(() -> trajectoryMatchingStream.parallel().forEach(x -> IOService.writeFile(x.toString(), fileFolder
				, "matchresult_" + x.getTrajID() + ".txt")));
		while (Objects.requireNonNull(folder.list()).length != matchingList.size()) {
			try {
				Thread.sleep(5);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		LOG.debug("Matching results written, total file count: " + matchingList.size());
	}
	
	/**
	 * Writer for writing matching results. The format follows the <tt>SimpleTrajectoryMatchResult.toString()</tt> format.
	 *
	 * @param matchingList The matching results.
	 * @param fileFolder   The output folder path.
	 */
	public static void writeMatchResults(List<SimpleTrajectoryMatchResult> matchingList, String fileFolder) {
		if (matchingList == null)
			throw new NullPointerException("The input matching result list is empty.");
		
		IOService.createFolder(fileFolder);
		IOService.cleanFolder(fileFolder);
		
		File folder = new File(fileFolder);
		Stream<SimpleTrajectoryMatchResult> trajectoryMatchingStream = matchingList.stream();
		
		// parallel processing
		ForkJoinPool forkJoinPool = ForkJoinPool.commonPool();
		forkJoinPool.submit(() -> trajectoryMatchingStream.parallel().forEach(x -> IOService.writeFile(x.toString(), fileFolder
				, "matchresult_" + x.getTrajID() + ".txt")));
		while (Objects.requireNonNull(folder.list()).length != matchingList.size()) {
			try {
				Thread.sleep(5);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		LOG.debug("Matching results written, total file count: " + matchingList.size());
	}
	
	/**
	 * Writer for writing point match results. The result format follows the <tt>PointMatch.toString()</tt> format.
	 *
	 * @param pointMatchResultList The matching results.
	 * @param fileFolder           The output folder path.
	 */
	public static void writePointMatchResults(List<Pair<Integer, List<PointMatch>>> pointMatchResultList, String fileFolder) {
		if (pointMatchResultList == null)
			throw new NullPointerException("The input point match result list is empty.");
		
		IOService.createFolder(fileFolder);
		IOService.cleanFolder(fileFolder);
		
		File folder = new File(fileFolder);
		Stream<Pair<Integer, List<PointMatch>>> trajectoryMatchingStream = pointMatchResultList.stream();
		
		// parallel processing
		ForkJoinPool forkJoinPool = ForkJoinPool.commonPool();
		forkJoinPool.submit(() -> trajectoryMatchingStream.parallel().forEach(x -> {
			List<String> output = new ArrayList<>();
			for (PointMatch pointMatch : x._2()) {
				output.add(pointMatch.toString());
			}
			IOService.writeFile(output, fileFolder, "pointmatch_" + x._1() + ".txt");
		}));
		while (Objects.requireNonNull(folder.list()).length != pointMatchResultList.size()) {
			try {
				Thread.sleep(5);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		LOG.debug("Point matching results written, total file count: " + pointMatchResultList.size());
	}
	
	/**
	 * Write route match results. Each line of the result consists of a road ID.
	 *
	 * @param routeMatchResultList The matching results.
	 * @param fileFolder           The output folder path.
	 */
	public static void writeRouteMatchResults(List<Pair<Integer, List<String>>> routeMatchResultList, String fileFolder) {
		if (routeMatchResultList == null)
			throw new NullPointerException("The input point match result list is empty.");
		
		IOService.createFolder(fileFolder);
		IOService.cleanFolder(fileFolder);
		
		File folder = new File(fileFolder);
		Stream<Pair<Integer, List<String>>> trajectoryMatchingStream = routeMatchResultList.stream();
		
		// parallel processing
		ForkJoinPool forkJoinPool = ForkJoinPool.commonPool();
		forkJoinPool.submit(() -> trajectoryMatchingStream.parallel().forEach(x -> IOService.writeFile(x._2(), fileFolder,
				"routematch_" + x._1() + ".txt")));
		while (Objects.requireNonNull(folder.list()).length != routeMatchResultList.size()) {
			try {
				Thread.sleep(5);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		LOG.debug("Point matching results written, total file count: " + routeMatchResultList.size());
	}
	
	/**
	 * Merge the matching results generated before and after the result updateGoh and write new map-matching results to the file. Only used
	 * in map updateGoh.
	 *
	 * @param oldMatchResults The original map-matching result before result updateGoh.
	 * @param newMatchResults The map-matching results affected by the result updateGoh.
	 * @param fileFolder      The output file folder.
	 * @return The merged map-matching results.
	 */
	public static List<MultipleTrajectoryMatchResult> writeAndMergeMatchResults(List<MultipleTrajectoryMatchResult> oldMatchResults,
																				List<MultipleTrajectoryMatchResult> newMatchResults, String fileFolder) {
		Set<String> newMatchResultIDList = new HashSet<>();
		for (MultipleTrajectoryMatchResult mr : newMatchResults)
			newMatchResultIDList.add(mr.getTrajID());
		oldMatchResults.removeIf(next -> newMatchResultIDList.contains(next.getTrajID()));
		oldMatchResults.addAll(newMatchResults);
		writeMultipleMatchResults(oldMatchResults, fileFolder);
		return oldMatchResults;
	}
	
	/**
	 * Writer for writing travel history. The history record is comprised of a list of road id and corresponding enter and leave
	 * timestamps. Only the results whose travel route is continuous are output.
	 *
	 * @param matchingList The matching results.
	 * @param fileFolder   The output folder path.
	 */
	public static void writeTravelHistoryResults(List<Trajectory> trajList, List<SimpleTrajectoryMatchResult> matchingList,
												 RoadNetworkGraph roadMap, String fileFolder) {
		if (matchingList == null)
			throw new NullPointerException("The input matching result list is empty.");
		
		IOService.createFolder(fileFolder);
		IOService.cleanFolder(fileFolder);
		
		HashMap<String, Trajectory> id2TrajMapping = new HashMap<>();
		for (Trajectory currTraj : trajList) {
			if (id2TrajMapping.containsKey(currTraj.getID()))
				throw new IllegalArgumentException("The same trajectory occurred twice in the input: " + currTraj.getID());
			id2TrajMapping.put(currTraj.getID(), currTraj);
		}
		for (SimpleTrajectoryMatchResult currMatchResult : matchingList) {
			if (!id2TrajMapping.containsKey(currMatchResult.getTrajID()))
				throw new IllegalArgumentException("The corresponding trajectory is not found for the match result: " + currMatchResult.getTrajID());
			Pair<List<Triplet<String, Long, Long>>, Boolean> resultList = isContinuous(id2TrajMapping.get(currMatchResult.getTrajID()),
					currMatchResult, roadMap);
			if (!resultList._2())
				continue;
			StringBuilder line = new StringBuilder();
			for (Triplet<String, Long, Long> currTriplet : resultList._1()) {
				line.append(currTriplet._1()).append(" ").append(currTriplet._2()).append(" ").append(currTriplet._3()).append("\n");
			}
			line.deleteCharAt(line.length() - 1);
			IOService.writeFile(line.substring(0, line.length()), fileFolder, "result_" + currMatchResult.getTrajID() + ".txt");
		}
		LOG.debug("Matching results written, total file count: " + matchingList.size());
	}
	
	private static Pair<List<Triplet<String, Long, Long>>, Boolean> isContinuous(Trajectory traj,
																				 SimpleTrajectoryMatchResult currMatchResult,
																				 RoadNetworkGraph roadMap) {
		DistanceFunction distFunc = roadMap.getDistanceFunction();
		List<Triplet<String, Long, Long>> resultList = new ArrayList<>();
		List<PointMatch> pointMatchList = currMatchResult.getPointMatchResultList();
		List<String> routeMatchList = currMatchResult.getRouteMatchResultList();
		if (traj.size() != pointMatchList.size() || routeMatchList.size() == 0 || !pointMatchList.get(0).getRoadID().equals(routeMatchList.get(0)))
			return new Pair<>(resultList, false);
		Iterator<String> iter = routeMatchList.iterator();
		String prevRoad = null;
		while (iter.hasNext()) {
			String currRoad = iter.next();
			if (prevRoad == null) {    // first road
				prevRoad = currRoad;
			} else {
				if (currRoad.equals(prevRoad)) {    // same route id occurred twice
					iter.remove();
				} else if (!roadMap.getWayByID(prevRoad).getToNode().getOutGoingWayList().contains(roadMap.getWayByID(currRoad)))    // the next road is not
					// connected.
					return new Pair<>(resultList, false);
				else {
					prevRoad = currRoad;
				}
			}
		}
		
		// The whole route is continuous, start assigning time info
		// TODO debug
		int index = 0;
		long enterTime = traj.get(index).time(), leaveTime;
		String currRoad = routeMatchList.get(0);
		RoadNode currEndPoint = roadMap.getWayByID(currRoad).getToNode();
		RoadNode prevEndPoint;
		TrajectoryPoint prevPoint = traj.get(0);
		PointMatch prevMatch = pointMatchList.get(0);
		
		for (int i = 1; i < traj.size(); i++) {
			String currMatchRoad = pointMatchList.get(i).getRoadID();
			if (currMatchRoad.equals("null") || currMatchRoad.equals("") || currMatchRoad.equals(currRoad) || currMatchRoad.length() == 0)
				continue;
			double distToPrevPoint = distFunc.distance(prevMatch.getMatchPoint(), currEndPoint.toPoint());
			double distToCurrPoint = distFunc.distance(pointMatchList.get(i).getMatchPoint(), currEndPoint.toPoint());
			long startTime = prevPoint.time();
			do {
				leaveTime =
						startTime + (long) (distToPrevPoint / (distToCurrPoint + distToPrevPoint) * (traj.get(i).time() - startTime));
				if (enterTime == leaveTime)
					return new Pair<>(resultList, false);
				resultList.add(new Triplet<>(currRoad, enterTime, leaveTime));
				if (resultList.size() == routeMatchList.size())
					return new Pair<>(resultList, true);
				currRoad = routeMatchList.get(resultList.size());
				prevEndPoint = currEndPoint;
				currEndPoint = roadMap.getWayByID(currRoad).getToNode();
				enterTime = leaveTime;
				startTime = enterTime;
				distToPrevPoint = distFunc.distance(prevEndPoint.toPoint(), currEndPoint.toPoint());
				distToCurrPoint = distFunc.distance(pointMatchList.get(i).getMatchPoint(), currEndPoint.toPoint());
			} while (!currMatchRoad.equals(currRoad));
			prevPoint = traj.get(i);
			prevMatch = pointMatchList.get(i);
		}
		if (traj.get(traj.size() - 1).time() != enterTime)
			resultList.add(new Triplet<>(routeMatchList.get(routeMatchList.size() - 1), enterTime, traj.get(traj.size() - 1).time()));
		return new Pair<>(resultList, true);
	}
}