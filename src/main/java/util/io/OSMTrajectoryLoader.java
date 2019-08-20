package util.io;

import algorithm.mapmatching.hmm.HMMMapMatching;
import org.apache.log4j.Logger;
import util.function.DistanceFunction;
import util.function.EuclideanDistanceFunction;
import util.object.roadnetwork.RoadNetworkGraph;
import util.object.roadnetwork.RoadWay;
import util.object.spatialobject.Rect;
import util.object.spatialobject.Trajectory;
import util.object.spatialobject.TrajectoryPoint;
import util.object.structure.MatchResultWithUnmatchedTraj;
import util.settings.BaseProperty;

import java.io.File;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Load the original trajectory data from the raw files.
 *
 * @author Hellisk
 * @since 5/07/2017
 */
public class OSMTrajectoryLoader {
	
	private static final Logger LOG = Logger.getLogger(OSMTrajectoryLoader.class);
	private int numOfTraj;
	private int trajMinLengthSec;
	private int sampleMaxIntervalSec;
	private DistanceFunction distFunc = new EuclideanDistanceFunction();
	
	public OSMTrajectoryLoader(int numOfTraj, int trajMinLengthSec, int sampleMaxIntervalSec) {
		this.numOfTraj = numOfTraj;
		this.trajMinLengthSec = trajMinLengthSec;
		this.sampleMaxIntervalSec = sampleMaxIntervalSec;
	}
	
	/**
	 * Read raw trajectories and assign visit count to the given map, each trajectory must be inside the map.
	 *
	 * @param rawMap        Input map
	 * @param inputTrajList Input trajectory list
	 * @param property      property file used for map-matching
	 */
	public void trajectoryVisitAssignmentWithMapMatching(RoadNetworkGraph rawMap, List<Trajectory> inputTrajList, BaseProperty property) throws ExecutionException, InterruptedException {
		Map<String, Integer> id2VisitCountMapping = new HashMap<>();   // a mapping between the road ID and the number of trajectory
		// visited
		Map<String, RoadWay> id2RoadWayMapping = new HashMap<>();   // a mapping between the road ID and the road way
		
		initializeMapping(rawMap, id2VisitCountMapping, id2RoadWayMapping);
		
		// apply map-matching with default settings
		property.setPropertyIfAbsence("algorithm.mapmatching.hmm.Beta", "0.08");
		property.setPropertyIfAbsence("algorithm.mapmatching.hmm.Sigma", "4");
		property.setPropertyIfAbsence("algorithm.mapmatching.CandidateRange", "50");
		property.setPropertyIfAbsence("algorithm.mapmatching.hmm.UTurnPenalty", "50");
		property.setPropertyIfAbsence("algorithm.mapmatching.hmm.RankLength", "1");
		property.setPropertyIfAbsence("algorithm.mapmatching.hmm.BackwardsFactor", "0.2");
		HMMMapMatching hmmMapMatching = new HMMMapMatching(rawMap, property);
		Stream<Trajectory> tempTrajStream = inputTrajList.stream();
		// parallel processing
		ForkJoinPool forkJoinPool = ForkJoinPool.commonPool();
		ForkJoinTask<Stream<List<String>>> matchedResultStream = forkJoinPool.submit(() -> tempTrajStream.parallel().map
				(trajectory -> {
					MatchResultWithUnmatchedTraj result = hmmMapMatching.doMatching(trajectory);
					// matching result is empty or result contains breaks, waive the current trajectory
					if (result.getMatchResult().getCompleteMatchRouteAtRank(0).getRoadIDList().size() == 0
							|| !result.getUnmatchedTrajectoryList().isEmpty())    // no or broken
						// matching result
						return null;
					return result.getMatchResult().getCompleteMatchRouteAtRank(0).getRoadIDList();
				}));
		
		for (List<String> routeMatch : matchedResultStream.get().collect(Collectors.toList())) {
			if (routeMatch != null)
				for (String s : routeMatch) {
					if (s != null && !s.equals("")) {
						int currCount = id2VisitCountMapping.get(s);
						id2VisitCountMapping.replace(s, currCount + 1);
					}
				}
		}
		
		DecimalFormat decimalFormat = new DecimalFormat(".00000");
		int visitThreshold = 5;
		int totalHighVisitCount = 0;  // count the total number of edges whose visit is less than a given threshold
		int totalVisitCount = 0;  // count the total number of edges whose visit is less than a given threshold
		rawMap.setMaxVisitCount(0);
		for (RoadWay w : rawMap.getWays()) {
			int currCount = id2VisitCountMapping.get(w.getID());
			w.setVisitCount(currCount);
			rawMap.updateMaxVisitCount(currCount);
			if (currCount > 0) {
				totalVisitCount++;
				if (currCount > visitThreshold) {
					totalHighVisitCount++;
				}
			}
		}
		LOG.debug("Beijing trajectories loaded. Total number of trajectories: " + inputTrajList.size() + ", max visit count: " + rawMap.getMaxVisitCount()
				+ ", roads visited percentage: " + decimalFormat.format(totalVisitCount / (double) rawMap.getWays().size() * 100)
				+ "%, visit more than " + visitThreshold + " times :"
				+ decimalFormat.format(totalHighVisitCount / (double) rawMap.getWays().size() * 100) + "%");
	}
	
	/**
	 * Read raw trajectories and write them as input files
	 *
	 * @param rawTrajFilePath  The path for raw trajectory file.
	 * @param outputTrajFolder The folder for output trajectories.
	 */
	public List<Trajectory> loadTrajectories(RoadNetworkGraph currMap, String rawTrajFilePath, String outputTrajFolder) throws InterruptedException,
			ExecutionException {
		
		Stream<File> trajFileStream = IOService.getFiles(rawTrajFilePath);
		Rect extendMapBoundary = currMap.getBoundary().extendByDist(50);    // default max distance between trajectory and road way
		ForkJoinPool forkJoinPool = ForkJoinPool.commonPool();
		ForkJoinTask<Stream<Trajectory>> trajStream = forkJoinPool.submit(() -> trajFileStream.parallel().map
				(trajFile -> {
					List<String> trajLines = IOService.readFile(trajFile);
					String id = trajFile.getName().substring(trajFile.getName().lastIndexOf('_') + 1, trajFile.getName().lastIndexOf('.'));
					List<TrajectoryPoint> trajPointList = new ArrayList<>();
					long prevTime = Long.MIN_VALUE;
					boolean isValidTraj = true;
					for (String line : trajLines) {
						String[] pointInfo = line.split(" ");
						long currTime;
						if (pointInfo[2].contains("."))
							currTime = Long.parseLong(pointInfo[2].substring(0, pointInfo[2].lastIndexOf('.')));
						else
							currTime = Long.parseLong(pointInfo[2]);
						if (pointInfo.length != 3) {
							LOG.error("Current trajectory point parse error: " + line + " in trajectory " + id);
							continue;
						}
						if (currTime <= prevTime) {
							LOG.warn("Current trajectory point time is problematic: " + currTime + ">=" + prevTime + " in trajectory " + id);
							continue;
						}
						TrajectoryPoint currPoint = new TrajectoryPoint(Double.parseDouble(pointInfo[0]), Double.parseDouble(pointInfo[1])
								, currTime, distFunc);
						if (!extendMapBoundary.contains(currPoint.x(), currPoint.y())) {    // the current trajectory contains point that
							// is not included in the map area
							isValidTraj = false;
							break;
						}
						trajPointList.add(currPoint);
					}
					if (!isValidTraj) {
						LOG.warn("Trajectory " + id + " is invalid as some of its point is out of bound");
						return null;
					} else if (trajPointList.size() < 2) {
						LOG.warn("Input trajectory is less than two points: " + id);
						return null;
					} else
						return new Trajectory(id, trajPointList);
				}));
		while (!trajStream.isDone()) {
			Thread.sleep(5);
		}
		List<Trajectory> fullTrajList = trajStream.get().collect(Collectors.toList());
		fullTrajList.removeIf(Objects::isNull);
		LOG.info("Trajectory read finished. Total number of trajectories: " + fullTrajList.size());
		if (numOfTraj == -1 && trajMinLengthSec == -1 && sampleMaxIntervalSec == -1) {
			TrajectoryWriter.writeTrajectories(fullTrajList, outputTrajFolder);
			return fullTrajList;
		} else {    // default setting changed
			List<Trajectory> resultList = new ArrayList<>();
			for (Trajectory currTraj : fullTrajList) {
				if (numOfTraj != -1 && resultList.size() >= numOfTraj)
					break;
				if (trajMinLengthSec != -1 && (currTraj.get(currTraj.size() - 1).time() - currTraj.get(0).time()) < trajMinLengthSec)
					continue;
				if (sampleMaxIntervalSec != -1) {
					long prevTime = currTraj.get(0).time();
					boolean isValidTraj = true;
					for (int i = 1; i < currTraj.size(); i++) {
						if (currTraj.get(i).time() - prevTime > sampleMaxIntervalSec) {
							isValidTraj = false;
							break;
						}
					}
					if (!isValidTraj)
						continue;
				}
				resultList.add(currTraj);
			}
			TrajectoryWriter.writeTrajectories(resultList, outputTrajFolder);
			return resultList;
		}
	}
	
	private void initializeMapping(RoadNetworkGraph roadGraph, Map<String, Integer> id2VisitCountMapping, Map<String, RoadWay> id2RoadWayMapping) {
		for (RoadWay w : roadGraph.getWays())
			if (!id2VisitCountMapping.containsKey(w.getID())) {
				id2VisitCountMapping.put(w.getID(), 0);
				id2RoadWayMapping.put(w.getID(), w);
			} else LOG.error("ERROR! The same road ID occurs twice: " + w.getID());
	}
}