package util.io;

import algorithm.mapmatching.hmm.HMMMapMatching;
import org.apache.log4j.Logger;
import util.function.GreatCircleDistanceFunction;
import util.object.roadnetwork.RoadNetworkGraph;
import util.object.roadnetwork.RoadWay;
import util.object.spatialobject.Point;
import util.object.spatialobject.Segment;
import util.object.spatialobject.Trajectory;
import util.object.spatialobject.TrajectoryPoint;
import util.object.structure.MatchResultWithUnmatchedTraj;
import util.object.structure.Pair;
import util.object.structure.PointMatch;
import util.settings.BaseProperty;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
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
public class BeijingTrajectoryLoader {
	
	private static final Logger LOG = Logger.getLogger(BeijingTrajectoryLoader.class);
	private int numOfTraj;
	private int trajMinLengthSec;
	private int sampleMaxIntervalSec;
	private GreatCircleDistanceFunction distFunc = new GreatCircleDistanceFunction();
	
	public BeijingTrajectoryLoader(int numOfTraj, int trajMinLengthSec, int sampleMaxIntervalSec) {
		this.numOfTraj = numOfTraj;
		this.trajMinLengthSec = trajMinLengthSec;
		this.sampleMaxIntervalSec = sampleMaxIntervalSec;
	}
	
	/**
	 * Read raw trajectories and assign visit count to the given map, each trajectory must be inside the map.
	 *
	 * @param rawMap              Input map
	 * @param inputRouteMatchList Input trajectory matching result list
	 */
	public void trajectoryVisitAssignment(RoadNetworkGraph rawMap, List<Pair<Integer, List<String>>> inputRouteMatchList) {
		Map<String, Integer> id2VisitCountMapping = new HashMap<>();   // a mapping between the road ID and the number of trajectory
		// visited
		Map<String, RoadWay> id2RoadWayMapping = new HashMap<>();   // a mapping between the road ID and the road way
		
		initializeMapping(rawMap, id2VisitCountMapping, id2RoadWayMapping);
		
		for (Pair<Integer, List<String>> routeMatch : inputRouteMatchList) {
			for (String s : routeMatch._2()) {
				int currCount = id2VisitCountMapping.get(s);
				id2VisitCountMapping.replace(s, currCount + 1);
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
		LOG.debug("Beijing trajectories loaded. Total number of trajectories: " + inputRouteMatchList.size() + ", max visit count: " + rawMap.getMaxVisitCount()
				+ ", roads visited percentage: " + decimalFormat.format(totalVisitCount / (double) rawMap.getWays().size() * 100)
				+ "%, visit more than " + visitThreshold + " times :"
				+ decimalFormat.format(totalHighVisitCount / (double) rawMap.getWays().size() * 100) + "%");
	}
	
	/**
	 * Read raw trajectories and filter them with a given size map, all trajectories that pass through the map area for a long period of
	 * time are outputted(only the sub-trajectory that actually passes). The map-matching result is outputted simultaneously.
	 *
	 * @param roadGraph                Input given map
	 * @param rawTrajFilePath          The path for raw trajectory file.
	 * @param outputTrajFolder         The folder for output trajectories.
	 * @param outputGTRouteMatchFolder The folder for output route match results.
	 * @throws IOException IO exception
	 */
	public void readTrajWithGTMatchResult(RoadNetworkGraph roadGraph, String rawTrajFilePath, String outputTrajFolder,
										  String outputGTRouteMatchFolder, String outputGTPointMatchFolder) throws IOException {
		final Map<String, Integer> id2VisitCountMapping = new LinkedHashMap<>();   // a mapping between the road ID and the number of
		// trajectory visited
		final Map<String, RoadWay> id2RoadWayMapping = new LinkedHashMap<>();   // a mapping between the road ID and the road way
		
		initializeMapping(roadGraph, id2VisitCountMapping, id2RoadWayMapping);
		
		DecimalFormat df = new DecimalFormat("0.00000");    // the format of the input trajectory points
		
		BufferedReader brTrajectory = new BufferedReader(new FileReader(rawTrajFilePath));
		List<Trajectory> resultTrajList = new ArrayList<>();
		List<Pair<Integer, List<String>>> gtRouteMatchList = new ArrayList<>();
		List<Pair<Integer, List<PointMatch>>> gtPointMatchList = new ArrayList<>();
		String line;
		// statistics
		int tripID = 0;
		long maxTimeDiff = 0;   // the maximum time difference
		long totalTimeDiff = 0;  // total time difference
		long totalNumOfPoint = 0;
		int numOfCompleteTraj = 0;
		int numOfPartialTraj = 0;
		// reset the cursor to the start of the current file
		while ((line = brTrajectory.readLine()) != null && (numOfTraj == -1 || tripID < numOfTraj)) {
			Map<Long, Pair<PointMatch, Double>> time2PointMatch = new LinkedHashMap<>();    // point match result for each trajectory
			String[] trajectoryInfo = line.split(",");
			String[] trajectoryPointList = trajectoryInfo[28].split("\\|");
			String[] pointMatchInfo = trajectoryInfo[29].split("\\|");
			String[] matchedRoadWayID = trajectoryInfo[4].split("\\|");

//			// test whether the matching result pass through the area
			// continuous is required for better map-matching and map update quality
//			if (doesNotHaveContinuousEnclosedSequence(id2RoadWayMapping, matchedRoadWayID))
//				continue;
			// continuous is not required for other applications
			if (doesNotHaveEnclosedSequence(id2RoadWayMapping, matchedRoadWayID))
				continue;
			
			Trajectory newTraj = new Trajectory(distFunc);
			
			String[] firstTrajectoryPoint = trajectoryPointList[0].split(":");
			double firstLon = Double.parseDouble(firstTrajectoryPoint[0]) / 100000;
			double firstLat = Double.parseDouble(firstTrajectoryPoint[1]) / 100000;
			long firstTime = Long.parseLong(firstTrajectoryPoint[3]);
			int currIndex = 0;
			double lon = firstLon;
			double lat = firstLat;
			while (!roadGraph.getBoundary().contains(lon, lat) && currIndex < trajectoryPointList.length - 1) {
				currIndex++;
				String[] currTrajectoryPoint = trajectoryPointList[currIndex].split(":");
				lon = firstLon + (Double.parseDouble(currTrajectoryPoint[0]) / 100000);
				lat = firstLat + (Double.parseDouble(currTrajectoryPoint[1]) / 100000);
			}
			if (currIndex == trajectoryPointList.length - 1)  // the current trajectory is out of range
				continue;
			int startIndex = currIndex;
			String[] currTrajectoryPoint = trajectoryPointList[currIndex].split(":");
			double currSpeed = Double.parseDouble(currTrajectoryPoint[2]);
			double currHeading = Double.parseDouble(currTrajectoryPoint[4]);
			currHeading = currHeading > 180 ? currHeading - 360 : currHeading;
			long currTimeOffset = Long.parseLong(currTrajectoryPoint[3]);
			long time = startIndex == 0 ? firstTime : firstTime + currTimeOffset;
			time2PointMatch.put(time, null);
			newTraj.add(Double.parseDouble(df.format(lon)), Double.parseDouble(df.format(lat)), time, currSpeed, currHeading);
			long currMaxTimeDiff = 0;
			long currTotalTimeDiff = 0;
			long prevTimeOffset = time - firstTime;
			for (currIndex = currIndex + 1; currIndex < trajectoryPointList.length; currIndex++) {
				currTrajectoryPoint = trajectoryPointList[currIndex].split(":");
				lon = firstLon + (Double.parseDouble(currTrajectoryPoint[0]) / 100000);
				lat = firstLat + (Double.parseDouble(currTrajectoryPoint[1]) / 100000);
//				// remove close consecutive points
//                double distance = distFunc.pointToPointDistance(prevLon, prevLat, lon, lat);
//                if (distance < 2 * SIGMA)
//                    continue;
				currTimeOffset = Long.parseLong(currTrajectoryPoint[3]);
				long currTimeDiff = currTimeOffset - prevTimeOffset;
				time = firstTime + currTimeOffset;
				// the new point is inside the area and satisfies the time constraint
				if (roadGraph.getBoundary().contains(lon, lat) && currTimeDiff <= (sampleMaxIntervalSec == -1 ? Long.MAX_VALUE :
						sampleMaxIntervalSec)) {
					currMaxTimeDiff = Math.max(currMaxTimeDiff, currTimeDiff);
					currTotalTimeDiff += currTimeDiff;
					currSpeed = Double.parseDouble(currTrajectoryPoint[2]);
					currHeading = Double.parseDouble(currTrajectoryPoint[4]);
					currHeading = currHeading > 180 ? currHeading - 360 : currHeading;
					prevTimeOffset = currTimeOffset;
					if (time2PointMatch.containsKey(time)) {
						LOG.error("The current timestamp " + time + " appears multiple times in this trajectory: " + tripID + ". Skip the" +
								" current point.");
						continue;
					}
					time2PointMatch.put(time, null);
					newTraj.add(Double.parseDouble(df.format(lon)), Double.parseDouble(df.format(lat)), time, currSpeed, currHeading);
//                        prevLon = lon;
//                        prevLat = lat;
				} else {
					break;
				}
			}
			if (newTraj.duration() >= trajMinLengthSec && newTraj.length() >= 3 * trajMinLengthSec && newTraj.length() > 0) {   // the
				// minimum average speed should be larger than 10.8km/h
				newTraj.setID(tripID + "");
				Pair<Integer, List<String>> newRouteMatchResult = new Pair<>(tripID, new ArrayList<>());
				Pair<Integer, List<PointMatch>> newPointMatchResult = new Pair<>(tripID, new ArrayList<>());
				// test whether the matching result pass through the area and continuous
				if (startIndex == 0 && currIndex == trajectoryPointList.length) {
					
					// continuous is required for higher quality map-matching and map update input
//					if (isNotContinuouslyEnclosed(id2RoadWayMapping, matchedRoadWayID)) {
//						continue;
//					}
					if (isNotEnclosed(id2RoadWayMapping, matchedRoadWayID)) {
						continue;
					}
					parsePointMatchResult(pointMatchInfo, time2PointMatch, roadGraph);
					for (Map.Entry<Long, Pair<PointMatch, Double>> pointMatchPair : time2PointMatch.entrySet()) {
						if (pointMatchPair.getValue() != null)
							newPointMatchResult._2().add(pointMatchPair.getValue()._1());
						else
							LOG.warn("Timestamps " + pointMatchPair.getKey() + " does not have point match result.");
					}
					if (newTraj.size() != newPointMatchResult._2().size())
						LOG.warn("The trajectory point size is not equivalent to the point match size.");
					
					for (String s : matchedRoadWayID) {
						if (id2VisitCountMapping.containsKey(s)) {
							int currCount = id2VisitCountMapping.get(s);
							id2VisitCountMapping.replace(s, currCount + 1);
							newRouteMatchResult._2().add(s);
						} else {
							LOG.warn("The current trajectory is fully inside the map but the ground-truth result is not.");
						}
					}
					if (newRouteMatchResult._2().size() == 0)
						continue;
					Set<String> pointMatchRoadIDSet = new HashSet<>();
					for (PointMatch pointMatch : newPointMatchResult._2()) {
						pointMatchRoadIDSet.add(pointMatch.getRoadID());
					}
					if (newRouteMatchResult._2().size() != pointMatchRoadIDSet.size())
						LOG.warn("Different point/route match result size: " + newRouteMatchResult._2().size() + "," + pointMatchRoadIDSet.size());
					numOfCompleteTraj++;
				} else {
//                    continue;
					// only part of the trajectory is selected as the raw trajectory
					for (Iterator<TrajectoryPoint> iterator = newTraj.iterator(); iterator.hasNext(); ) {
						TrajectoryPoint currPoint = iterator.next();
						if (!isInsideInnerGraph(currPoint.x(), currPoint.y(), roadGraph, 1)) {
							iterator.remove();
							time2PointMatch.remove(currPoint.time());
						}
					}
					
					parsePointMatchResult(pointMatchInfo, time2PointMatch, roadGraph);
					for (Map.Entry<Long, Pair<PointMatch, Double>> pointMatchPair : time2PointMatch.entrySet()) {
						if (pointMatchPair.getValue() != null)
							newPointMatchResult._2().add(pointMatchPair.getValue()._1());
						else
							LOG.warn("Timestamps " + pointMatchPair.getKey() + " does not have point match result.");
					}
					if (newTraj.size() != newPointMatchResult._2().size())
						LOG.warn("The trajectory point size is not equivalent to the point match size.");
					
					for (String s : matchedRoadWayID) {
						if (id2VisitCountMapping.containsKey(s)) {
							int currCount = id2VisitCountMapping.get(s);
							id2VisitCountMapping.replace(s, currCount + 1);
							newRouteMatchResult._2().add(s);
						} else if (newRouteMatchResult._2().size() > 0)
							break;
					}
					if (newRouteMatchResult._2().size() == 0)
						continue;
					Set<String> pointMatchRoadIDSet = new HashSet<>();
					for (PointMatch pointMatch : newPointMatchResult._2()) {
						pointMatchRoadIDSet.add(pointMatch.getRoadID());
					}
					if (newRouteMatchResult._2().size() != pointMatchRoadIDSet.size())
						LOG.warn("Different point/route match result size: " + newRouteMatchResult._2().size() + "," + pointMatchRoadIDSet.size());
					numOfPartialTraj++;
				}
				if (newTraj.size() < 2)
					continue;
				trajectoryValidityCheck(newTraj);
				
				resultTrajList.add(newTraj);
				gtRouteMatchList.add(newRouteMatchResult);
				gtPointMatchList.add(newPointMatchResult);
				maxTimeDiff = Math.max(maxTimeDiff, currMaxTimeDiff);
				totalTimeDiff += currTotalTimeDiff;
				totalNumOfPoint += newTraj.size();
				tripID++;
			}
		}
		trajectoryVisitAssignment(roadGraph, gtRouteMatchList);
		writeTrajAndGTMatchResults(resultTrajList, gtRouteMatchList, gtPointMatchList, outputTrajFolder, outputGTRouteMatchFolder, outputGTPointMatchFolder);
		
		LOG.debug(tripID + " trajectories extracted, including " + numOfCompleteTraj + " complete trajectories and " + numOfPartialTraj +
				" partial ones. The average length is " + (int) (totalNumOfPoint / tripID));
		LOG.debug("The maximum sampling interval is " + maxTimeDiff + "s, and the average time interval is "
				+ totalTimeDiff / (totalNumOfPoint - tripID) + ".");
	}
	
	/**
	 * parse point match result, each timestamps may have multiple point match result, store the one with the shortest distance. Format:
	 * road_id:distance_to_road_start:distance_to_trajectory_point:point_match_lon:point_match_lat:timestamps
	 *
	 * @param pointMatchList  The point match result string.
	 * @param time2PointMatch The point matching result for each timestamps, =null initially.
	 */
	private void parsePointMatchResult(String[] pointMatchList, Map<Long, Pair<PointMatch, Double>> time2PointMatch,
									   RoadNetworkGraph roadGraph) {
		for (String s : pointMatchList) {
			String[] matchInfo = s.split(":");
			long currTime = Long.parseLong(matchInfo[5]);
			if (time2PointMatch.containsKey(currTime)) {
				// current timestamps is found in our trajectory
				Pair<PointMatch, Double> currPointMatchPair = time2PointMatch.get(currTime);
				if (currPointMatchPair != null) {
					// another point match already exists, consider replacing it
					double currDist = Double.parseDouble(matchInfo[2]);
					if (currPointMatchPair._2() > currDist) {
						// replace the existing one
						Point matchPoint = new Point(Double.parseDouble(matchInfo[3]), Double.parseDouble(matchInfo[4]),
								roadGraph.getDistanceFunction());
						String roadID = matchInfo[0];
						boolean isSegmentFound = false;
						List<Segment> segmentList = roadGraph.getWayByID(roadID).getEdges();
						for (Segment seg : segmentList) {
							if (distFunc.distance(matchPoint, seg) <= 0.05) {
								// the point is on the segment
								PointMatch currPointMatch = new PointMatch(matchPoint, seg, roadID);
								isSegmentFound = true;
								time2PointMatch.replace(currTime, new Pair<>(currPointMatch, currDist));
								break;
							}
						}
						if (!isSegmentFound)
							throw new IllegalArgumentException("None of the segments in the road " + roadID + " contains the " +
									"match point " + matchPoint.toString() + ".");
					} else if (currPointMatchPair._2() == currDist) {
						LOG.warn("The current trajectory point has two point matching with the same distance: " +
								currPointMatchPair._2() + "," + currPointMatchPair._1().toString() + "," + s + ".");
					}
				} else {
					// insert the current point match to the result
					Point matchPoint = new Point(Double.parseDouble(matchInfo[3]), Double.parseDouble(matchInfo[4]),
							roadGraph.getDistanceFunction());
					String roadID = matchInfo[0];
					double currDist = Double.parseDouble(matchInfo[2]);
					boolean isSegmentFound = false;
					List<Segment> segmentList = roadGraph.getWayByID(roadID).getEdges();
					for (Segment seg : segmentList) {
						if (distFunc.distance(matchPoint, seg) <= 0.05) {
							// the point is on the segment
							PointMatch currPointMatch = new PointMatch(matchPoint, seg, roadID);
							isSegmentFound = true;
							time2PointMatch.replace(currTime, new Pair<>(currPointMatch, currDist));
							break;
						}
					}
					if (!isSegmentFound)
						throw new IllegalArgumentException("None of the segments in the road " + roadID + " contains the " +
								"match point " + matchPoint.toString() + ".");
				}
			}
		}
	}
	
	/**
	 * Map-matching the trajectory to the ground-truth map so as to generate the ground-truth matching result. Used when the provided
	 * ground-truth result is not reliable.
	 *
	 * @param roadGraph                The resized map
	 * @param rawGrantMap              The map cropped by the bounding box.
	 * @param rawTrajFilePath          The raw trajectory file path.
	 * @param outputTrajFolder         The folder for output trajectories.
	 * @param outputGTRouteMatchFolder The folder for output ground-truth route match result.
	 * @param prop                     The map-matching property.
	 */
	public void readTrajAndGenerateGTRouteMatchResult(RoadNetworkGraph roadGraph, RoadNetworkGraph rawGrantMap,
													  String rawTrajFilePath, String outputTrajFolder, String outputGTRouteMatchFolder,
													  BaseProperty prop) throws IOException, InterruptedException, ExecutionException {
		final Map<String, Integer> id2VisitCountSmallMapping = new HashMap<>();   // a mapping between the road ID and the number of
		// trajectory visited in current map
		final Map<String, Integer> id2VisitCountLargeMapping = new HashMap<>();   // a mapping between the road ID and the number of
		// trajectory visited in the original map
		final Map<String, RoadWay> id2RoadWayMapping = new HashMap<>();   // a mapping between the road ID and the road way
		
		initializeMapping(roadGraph, id2VisitCountSmallMapping, id2RoadWayMapping);
		
		DecimalFormat df = new DecimalFormat("0.00000");    // the format of the input trajectory points
		
		BufferedReader brTrajectory = new BufferedReader(new FileReader(rawTrajFilePath));
		
		List<Trajectory> tempTrajList = new ArrayList<>();
		List<Pair<Integer, List<String>>> gtRouteMatchList = new ArrayList<>();
		String line;
		int tripID = 0;
		long totalNumOfPoint = 0;
		// reset the cursor to the start of the current file
		while ((line = brTrajectory.readLine()) != null && (numOfTraj == -1 || tempTrajList.size() < 1.5 * numOfTraj)) {
			String[] trajectoryInfo = line.split(",");
			String[] rawTrajectoryPointID = trajectoryInfo[28].split("\\|");
			String[] matchedRoadWayID = trajectoryInfo[4].split("\\|");
			
			// test whether the matching result pass through the area and continuous
			if (doesNotHaveContinuousEnclosedSequence(id2RoadWayMapping, matchedRoadWayID))
				continue;
			
			Trajectory newTraj = new Trajectory(distFunc);
			
			String[] firstTrajectoryPoint = rawTrajectoryPointID[0].split(":");
			double firstLon = Double.parseDouble(firstTrajectoryPoint[0]) / 100000;
			double firstLat = Double.parseDouble(firstTrajectoryPoint[1]) / 100000;
			long firstTime = Long.parseLong(firstTrajectoryPoint[3]);
			int currIndex = 0;
			double lon = firstLon;
			double lat = firstLat;
			while (!isInsideInnerGraph(lon, lat, roadGraph, 2) && currIndex < rawTrajectoryPointID.length - 1) {
				currIndex++;
				String[] currTrajectoryPoint = rawTrajectoryPointID[currIndex].split(":");
				lon = firstLon + (Double.parseDouble(currTrajectoryPoint[0]) / 100000);
				lat = firstLat + (Double.parseDouble(currTrajectoryPoint[1]) / 100000);
			}
			if (currIndex == rawTrajectoryPointID.length - 1)  // the current trajectory is out of range
				continue;
			int startIndex = currIndex;
			String[] currTrajectoryPoint = rawTrajectoryPointID[currIndex].split(":");
			double currSpeed = Double.parseDouble(currTrajectoryPoint[2]);
			double currHeading = Double.parseDouble(currTrajectoryPoint[4]);
			currHeading = currHeading > 180 ? currHeading - 360 : currHeading;
			long currTimeOffset = Long.parseLong(currTrajectoryPoint[3]);
			long time = startIndex == 0 ? firstTime : firstTime + currTimeOffset;
			newTraj.add(Double.parseDouble(df.format(lon)), Double.parseDouble(df.format(lat)), time, currSpeed, currHeading);
			long prevTimeOffset = time - firstTime;
			for (currIndex = currIndex + 1; currIndex < rawTrajectoryPointID.length; currIndex++) {
				currTrajectoryPoint = rawTrajectoryPointID[currIndex].split(":");
				lon = firstLon + (Double.parseDouble(currTrajectoryPoint[0]) / 100000);
				lat = firstLat + (Double.parseDouble(currTrajectoryPoint[1]) / 100000);
//                double distance = distFunc.pointToPointDistance(prevLon, prevLat, lon, lat);
//                if (distance < 2 * SIGMA)
//                    continue;
				currTimeOffset = Long.parseLong(currTrajectoryPoint[3]);
				long currTimeDiff = currTimeOffset - prevTimeOffset;
				time = firstTime + currTimeOffset;
				// the new point is inside the area and satisfies the time constraint
				if (isInsideInnerGraph(Double.parseDouble(df.format(lon)), Double.parseDouble(df.format(lat)), roadGraph, 2)
						&& currTimeDiff <= (sampleMaxIntervalSec == -1 ? Long.MAX_VALUE : sampleMaxIntervalSec)) {
					currSpeed = Double.parseDouble(currTrajectoryPoint[2]);
					currHeading = Double.parseDouble(currTrajectoryPoint[4]);
					currHeading = currHeading > 180 ? currHeading - 360 : currHeading;
					prevTimeOffset = currTimeOffset;
					newTraj.add(Double.parseDouble(df.format(lon)), Double.parseDouble(df.format(lat)), time, currSpeed, currHeading);
//                        prevLon = lon;
//                        prevLat = lat;
				} else {
					break;
				}
			}
			
			if (newTraj.duration() >= trajMinLengthSec && newTraj.length() >= 3 * trajMinLengthSec) {   // the minimum average
				// speed should be larger than 10.8km/h
				newTraj.setID(tripID + "");
				trajectoryValidityCheck(newTraj);
				if (newTraj.size() < 2)
					continue;
				tempTrajList.add(newTraj);
				tripID++;
			}
		}
		LOG.info("Trajectory filter finished, total number of candidates: " + tripID + ". Start the ground-truth map-matching.");
		
		// start the generation of ground-truth map-matching result
		HMMMapMatching hmm = new HMMMapMatching(rawGrantMap, prop);
		Stream<Trajectory> tempTrajStream = tempTrajList.stream();
		// parallel processing
		ForkJoinPool forkJoinPool = ForkJoinPool.commonPool();
		ForkJoinTask<Stream<Pair<Integer, String[]>>> matchedResultStream = forkJoinPool.submit(() -> tempTrajStream.parallel().map
				(trajectory -> {
					MatchResultWithUnmatchedTraj result = hmm.doMatching(trajectory);
					// matching result is empty or result contains breaks, waive the current trajectory
					if (result.getMatchResult().getCompleteMatchRouteAtRank(0).getRoadIDList().size() == 0
							|| !result.getUnmatchedTrajectoryList().isEmpty())    // no or broken
						// matching result
						return null;
					String[] bestMatchWayList = result.getMatchResult().getCompleteMatchRouteAtRank(0).getRoadIDList().toArray(new String[0]);
					// test whether the matching result is included in the map
					if (doesNotHaveContinuousEnclosedSequence(id2RoadWayMapping, bestMatchWayList))
						return null;
					return new Pair<>(Integer.parseInt(trajectory.getID()), bestMatchWayList);
				}));
		while (!matchedResultStream.isDone()) {
			Thread.sleep(5);
		}
		
		HashMap<Integer, String[]> id2MatchResult = new HashMap<>();
		List<Trajectory> resultTrajList = new ArrayList<>();
		int matchedResultCount = 0;
		List<Pair<Integer, String[]>> matchedResultList = matchedResultStream.get().collect(Collectors.toList());
		for (Pair<Integer, String[]> matchedResult : matchedResultList) {
			if (matchedResult != null) {
				matchedResultCount++;
				id2MatchResult.put(matchedResult._1(), matchedResult._2());
			}
		}
		LOG.debug("Ground-truth matching complete. Total number of valid matching result: " + matchedResultCount);
		
		tripID = 0;     // reset the trip ID for final trajectory id assignment
		for (Trajectory currTraj : tempTrajList) {
			if (id2MatchResult.containsKey(Integer.parseInt(currTraj.getID()))) {
				String[] matchedRoadWayID = id2MatchResult.get(Integer.parseInt(currTraj.getID()));
				// test whether the matching result pass through the area and continuous
				if (!isNotContinuouslyEnclosed(id2RoadWayMapping, matchedRoadWayID)) {
					Pair<Integer, List<String>> newMatchResult = new Pair<>(tripID, Arrays.asList(matchedRoadWayID));
					currTraj.setID(tripID + "");
					resultTrajList.add(currTraj);
					gtRouteMatchList.add(newMatchResult);
					for (String s : matchedRoadWayID) {
						int currCount = id2VisitCountSmallMapping.get(s);
						id2VisitCountSmallMapping.replace(s, currCount + 1);
					}
					totalNumOfPoint += currTraj.size();
					tripID++;
				}
			}
		}
		if (numOfTraj != -1 && tempTrajList.size() == 1.5 * numOfTraj && tripID < numOfTraj)
			throw new IllegalArgumentException("The cache for trajectory filter is too small. The final trajectory size is :" + tripID);
		LOG.info("Ground-truth trajectory result generated.");
		
		writeTrajAndGTMatchResults(resultTrajList, gtRouteMatchList, null, outputTrajFolder, outputGTRouteMatchFolder,
				null);
		
		// visit statistics
		int visitThreshold = 5;
		int totalHighVisitSmallCount = 0;
		int totalHighVisitLargeCount = 0;
		int totalVisitSmallCount = 0;
		int totalVisitLargeCount = 0;
		for (RoadWay w : rawGrantMap.getWays()) {
			id2VisitCountLargeMapping.put(w.getID(), w.getVisitCount());
		}
		roadGraph.setMaxVisitCount(0);
		for (RoadWay w : roadGraph.getWays()) {
			int visitSmallCount = id2VisitCountSmallMapping.get(w.getID());
			w.setVisitCount(visitSmallCount);
			roadGraph.updateMaxVisitCount(visitSmallCount);
			if (visitSmallCount > 0) {
				totalVisitSmallCount++;
				if (visitSmallCount >= 5) {
					totalHighVisitSmallCount++;
				}
			}
			if (id2VisitCountLargeMapping.containsKey(w.getID())) {
				int visitLargeCount = id2VisitCountLargeMapping.get(w.getID());
				if (visitLargeCount > 0) {
					totalVisitLargeCount++;
					if (visitLargeCount >= visitThreshold) {
						totalHighVisitLargeCount++;
					}
				}
			} else
				LOG.error("Road in new map doesn't exist in the original map");
		}
		LOG.debug(tripID + " trajectories extracted, the average length: " + (int) (totalNumOfPoint / tripID) + ", max visit " +
				"count: " + roadGraph.getMaxVisitCount() + ".");
		LOG.debug("Visit percentage: " + df.format((totalVisitSmallCount / (double) roadGraph.getWays().size()) * 100) + "%/" +
				df.format((totalVisitLargeCount / (double) roadGraph.getWays().size()) * 100) + "%, high visit(>=" + visitThreshold +
				"times): " + df.format((totalHighVisitSmallCount / (double) roadGraph.getWays().size()) * 100) + "%/" +
				df.format((totalHighVisitLargeCount / (double) roadGraph.getWays().size()) * 100) + "%.");
	}
	
	/**
	 * Remove erroneous point within a trajectory, including duplicated points and rewinding points.
	 *
	 * @param trajectory Input trajectory.
	 */
	private void trajectoryValidityCheck(Trajectory trajectory) {
		Iterator<TrajectoryPoint> iter = trajectory.iterator();
		TrajectoryPoint prevPoint = iter.next();
		while (iter.hasNext()) {
			TrajectoryPoint currPoint = iter.next();
			if (currPoint.time() == prevPoint.time()) {
				if (currPoint.x() == prevPoint.x() && currPoint.y() == prevPoint.y()) {       // duplicated point, remove it
					iter.remove();
				} else {
					LOG.error("Two points with the same timestamps.");
					iter.remove();
				}
			} else if (currPoint.time() < prevPoint.time()) {
				LOG.error("Current point is earlier than the previous point.");
				iter.remove();
				
			} else {
				prevPoint = currPoint;
			}
		}
	}
	
	/**
	 * Write the raw trajectories and the corresponding route and point matching results to the given folders.
	 *
	 * @param outputTrajList               Extracted trajectory list.
	 * @param gtRouteMatchResultList       The corresponding ground-truth route matching results.
	 * @param gtPointMatchResultList       The corresponding ground-truth point matching results.
	 * @param outputTrajFolder             Output folder for trajectories.
	 * @param outputRouteMatchResultFolder Output folder for ground-truth route matching results.
	 * @param outputPointMatchResultFolder Output folder for ground-truth point matching results.
	 */
	private void writeTrajAndGTMatchResults(List<Trajectory> outputTrajList, List<Pair<Integer, List<String>>> gtRouteMatchResultList,
											List<Pair<Integer, List<PointMatch>>> gtPointMatchResultList, String outputTrajFolder,
											String outputRouteMatchResultFolder, String outputPointMatchResultFolder) {
		if (outputTrajList == null || outputTrajList.isEmpty())
			throw new NullPointerException("The output trajectory result list is empty.");
		if (gtRouteMatchResultList == null || gtRouteMatchResultList.isEmpty())
			throw new NullPointerException("The output trajectory matching result list is empty.");
		if (outputTrajList.size() != gtRouteMatchResultList.size())
			throw new IllegalArgumentException("The counts of the output trajectories and their matching results are inconsistent.");
		TrajectoryWriter.writeTrajectories(outputTrajList, outputTrajFolder);
		MatchResultWriter.writeRouteMatchResults(gtRouteMatchResultList, outputRouteMatchResultFolder);
		if (gtPointMatchResultList != null && outputPointMatchResultFolder != null)
			MatchResultWriter.writePointMatchResults(gtPointMatchResultList, outputPointMatchResultFolder);
	}
	
	/**
	 * Check whether the map-matching result satisfies the conditions that all roads must be included in the map area and the
	 * map-matching result must be continuous.
	 *
	 * @param id2RoadWayMapping The road ID and the corresponding road way object.
	 * @param matchedRoadWayID  The list of ground-truth map-matching result.
	 * @return False if all map-matching result satisfy the requirement, otherwise true.
	 */
	private boolean isNotContinuouslyEnclosed(Map<String, RoadWay> id2RoadWayMapping, String[] matchedRoadWayID) {
		RoadWay prevMatchRoad = null;
		for (String s : matchedRoadWayID) {
			if (!id2RoadWayMapping.containsKey(s)) { // current match road is not included in the map
				return true;
			} else if (prevMatchRoad != null) {    // check the connectivity of the match roadID
				RoadWay currRoad = id2RoadWayMapping.get(s);
				if (!prevMatchRoad.getToNode().getID().equals(currRoad.getFromNode().getID()) && !prevMatchRoad.equals(currRoad)) {
					// break happens
//                    System.out.println("Matching result is not continuous.");
					return true;
				} else
					prevMatchRoad = id2RoadWayMapping.get(s);
			} else {
				prevMatchRoad = id2RoadWayMapping.get(s);
			}
		}
		return false;
	}
	
	/**
	 * Check whether the map-matching result satisfies the conditions that all roads must be included in the map area.
	 *
	 * @param id2RoadWayMapping The road ID and the corresponding road way object.
	 * @param matchedRoadWayID  The list of ground-truth map-matching result.
	 * @return False if all map-matching result satisfy the requirement, otherwise true.
	 */
	private boolean isNotEnclosed(Map<String, RoadWay> id2RoadWayMapping, String[] matchedRoadWayID) {
		for (String s : matchedRoadWayID) {
			if (!id2RoadWayMapping.containsKey(s)) { // current match road is not included in the map
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Check whether the map-matching result contains a continuous matching sequence that is enclosed in the map area.
	 *
	 * @param id2RoadWayMapping The road ID and the corresponding road way object.
	 * @param matchedRoadWayID  The list of ground-truth map-matching result ID.
	 * @return False if the ground-truth matching result satisfies the requirement, otherwise true.
	 */
	private boolean doesNotHaveContinuousEnclosedSequence(Map<String, RoadWay> id2RoadWayMapping, String[] matchedRoadWayID) {
		RoadWay prevMatchRoad = null;
		double roadLengthSum = 0;
		for (String s : matchedRoadWayID) {
			if (id2RoadWayMapping.containsKey(s)) { // current match road is included in the map
				RoadWay currRoad = id2RoadWayMapping.get(s);
				if (prevMatchRoad != null) {    // check the connectivity of the match roadID
					if (!prevMatchRoad.getToNode().getID().equals(currRoad.getFromNode().getID()) && !prevMatchRoad.equals(currRoad)) {
						// break happens
//                        System.out.println("Matching result is not continuous.");
						return true;
					} else {
						prevMatchRoad = currRoad;
						roadLengthSum += currRoad.getLength();
						if (roadLengthSum > 5 * trajMinLengthSec)     // the matching result is long enough
							return false;
					}
				} else {
					prevMatchRoad = id2RoadWayMapping.get(s);
					roadLengthSum += currRoad.getLength();
				}
			} else if (prevMatchRoad != null) {
				prevMatchRoad = null;
				roadLengthSum = 0;
			}
		}
		return true;
	}
	
	/**
	 * Check whether the map-matching result contains a matching sequence that is enclosed in the map area.
	 *
	 * @param id2RoadWayMapping The road ID and the corresponding road way object.
	 * @param matchedRoadWayID  The list of ground-truth map-matching result ID.
	 * @return False if the ground-truth matching result satisfies the requirement, otherwise true.
	 */
	private boolean doesNotHaveEnclosedSequence(Map<String, RoadWay> id2RoadWayMapping, String[] matchedRoadWayID) {
		RoadWay prevMatchRoad = null;
		double roadLengthSum = 0;
		for (String s : matchedRoadWayID) {
			if (id2RoadWayMapping.containsKey(s)) { // current match road is included in the map
				RoadWay currRoad = id2RoadWayMapping.get(s);
				if (prevMatchRoad != null) {    // check the connectivity of the match roadID
					prevMatchRoad = currRoad;
					roadLengthSum += currRoad.getLength();
					if (roadLengthSum > 5 * trajMinLengthSec)     // the matching result is long enough
						return false;
					
				} else {
					prevMatchRoad = id2RoadWayMapping.get(s);
					roadLengthSum += currRoad.getLength();
				}
			} else if (prevMatchRoad != null) {
				prevMatchRoad = null;
				roadLengthSum = 0;
			}
		}
		return true;
	}
	
	private void initializeMapping(RoadNetworkGraph roadGraph, Map<String, Integer> id2VisitCountMapping, Map<String, RoadWay> id2RoadWayMapping) {
		for (RoadWay w : roadGraph.getWays())
			if (!id2VisitCountMapping.containsKey(w.getID())) {
				id2VisitCountMapping.put(w.getID(), 0);
				id2RoadWayMapping.put(w.getID(), w);
			} else LOG.error("ERROR! The same road ID occurs twice: " + w.getID());
	}
	
	/**
	 * Check if the point is around the margin of the map area to avoid unreasonable map-matching/inference results. Return false if the
	 * point is near the margin.
	 *
	 * @param pointX     The x/longitude of the point.
	 * @param pointY     The y/latitude of the point.
	 * @param roadGraph  The map area.
	 * @param percentage The percentage of the map margin compared with the map size.
	 * @return True if the point is not around the margin.
	 */
	private boolean isInsideInnerGraph(double pointX, double pointY, RoadNetworkGraph roadGraph, double percentage) {
		double lonDiff = (roadGraph.getMaxLon() - roadGraph.getMinLon()) / 100 * percentage;
		double latDiff = (roadGraph.getMaxLat() - roadGraph.getMinLat()) / 100 * percentage;
		if (pointX >= roadGraph.getMinLon() + lonDiff && pointX <= roadGraph.getMaxLon() - lonDiff)
			return pointY >= roadGraph.getMinLat() + latDiff && pointY <= roadGraph.getMaxLat() - latDiff;
		return false;
	}
}