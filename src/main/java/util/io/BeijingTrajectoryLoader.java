package util.io;

import algorithm.mapmatching.hmm.NewsonHMM2009;
import org.apache.log4j.Logger;
import util.function.GreatCircleDistanceFunction;
import util.object.roadnetwork.RoadNetworkGraph;
import util.object.roadnetwork.RoadWay;
import util.object.spatialobject.Trajectory;
import util.object.spatialobject.TrajectoryPoint;
import util.object.structure.MatchResultWithUnmatchedTraj;
import util.object.structure.Pair;
import util.settings.BaseProperty;

import java.io.BufferedReader;
import java.io.File;
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
	 * @param rawMap   Input map
	 * @param filePath Input path for raw trajectories
	 */
	public void trajectoryVisitAssignment(RoadNetworkGraph rawMap, String filePath) {
		Map<String, Integer> id2VisitCountMapping = new HashMap<>();   // a mapping between the road ID and the number of trajectory
		// visited
		Map<String, RoadWay> id2RoadWayMapping = new HashMap<>();   // a mapping between the road ID and the road way
		
		initializeMapping(rawMap, id2VisitCountMapping, id2RoadWayMapping);
		int tripID = 0;
		
		try {
			BufferedReader br = new BufferedReader(new FileReader(new File(filePath))); // use BufferedReader instead of IOService
			// .readFile in case of OutOfMemory.
			String line;
			// create folders for further writing
			while ((line = br.readLine()) != null) {
				String[] trajectoryInfo = line.split(",");
				String[] rawTrajectory = trajectoryInfo[28].split("\\|");
				String[] matchedTrajectory = trajectoryInfo[4].split("\\|");
				
				// test whether the matching result is included in the map
				if (isMatchResultNotEnclosed(id2RoadWayMapping, matchedTrajectory)) {
					continue;
				}
				
				// test whether the raw trajectory is within the map area
				boolean isInsideTrajectory = true;
				String[] firstTrajectoryPoint = rawTrajectory[0].split(":");
				double firstLon = Double.parseDouble(firstTrajectoryPoint[0]) / 100000;
				double firstLat = Double.parseDouble(firstTrajectoryPoint[1]) / 100000;
				if (rawMap.getBoundary().contains(firstLon, firstLat)) {
					long prevTimeDiff = 0;
					for (int i = 1; i < rawTrajectory.length; i++) {
						String[] currTrajectoryPoint = rawTrajectory[i].split(":");
						double lon = firstLon + (Double.parseDouble(currTrajectoryPoint[0]) / 100000);
						double lat = firstLat + (Double.parseDouble(currTrajectoryPoint[1]) / 100000);
						long currTime = Long.parseLong(currTrajectoryPoint[3]);
						long currTimeDiff = currTime - prevTimeDiff;
						if (rawMap.getBoundary().contains(lon, lat) && (sampleMaxIntervalSec == -1 || currTimeDiff <= sampleMaxIntervalSec)) {
							prevTimeDiff = currTime;
						} else {
							isInsideTrajectory = false;
							break;
						}
					}
				} else {
					continue;   // the first point is outside the road map area, skip the current trajectory
				}
				
				if (isInsideTrajectory) {   // the current trajectory is selected
					for (String s : matchedTrajectory) {
						int currCount = id2VisitCountMapping.get(s);
						id2VisitCountMapping.replace(s, currCount + 1);
					}
					tripID++;
				}
			}
			br.close();
		} catch (IOException e) {
			LOG.error("Error reading input file.", e);
			e.printStackTrace();
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
		LOG.debug("Beijing trajectories loaded. Total number of trajectories: " + tripID + ", max visit count: " + rawMap.getMaxVisitCount()
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
	public void readTrajWithGTRouteMatchResult(RoadNetworkGraph roadGraph, String rawTrajFilePath, String outputTrajFolder,
											   String outputGTRouteMatchFolder) throws IOException {
		final Map<String, Integer> id2VisitCountMapping = new LinkedHashMap<>();   // a mapping between the road ID and the number of
		// trajectory visited
		final Map<String, RoadWay> id2RoadWayMapping = new LinkedHashMap<>();   // a mapping between the road ID and the road way
		
		initializeMapping(roadGraph, id2VisitCountMapping, id2RoadWayMapping);
		
		DecimalFormat df = new DecimalFormat("0.00000");    // the format of the input trajectory points
		
		BufferedReader brTrajectory = new BufferedReader(new FileReader(rawTrajFilePath));
		List<Trajectory> resultTrajList = new ArrayList<>();
		List<Pair<Integer, List<String>>> gtRouteMatchList = new ArrayList<>();
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
			String[] trajectoryInfo = line.split(",");
			String[] rawTrajectoryPointID = trajectoryInfo[28].split("\\|");
			String[] matchedRoadWayID = trajectoryInfo[4].split("\\|");
			
			// test whether the matching result pass through the area and continuous
			if (isMatchResultNotContinuous(id2RoadWayMapping, matchedRoadWayID))
				continue;
			
			Trajectory newTraj = new Trajectory(distFunc);
			
			String[] firstTrajectoryPoint = rawTrajectoryPointID[0].split(":");
			double firstLon = Double.parseDouble(firstTrajectoryPoint[0]) / 100000;
			double firstLat = Double.parseDouble(firstTrajectoryPoint[1]) / 100000;
			long firstTime = Long.parseLong(firstTrajectoryPoint[3]);
			int currIndex = 0;
			double lon = firstLon;
			double lat = firstLat;
			while (!roadGraph.getBoundary().contains(lon, lat) && currIndex < rawTrajectoryPointID.length - 1) {
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
			long currTimeOffset = Long.parseLong(currTrajectoryPoint[3]);
			long time = startIndex == 0 ? firstTime : firstTime + currTimeOffset;
			newTraj.add(Double.parseDouble(df.format(lon)), Double.parseDouble(df.format(lat)), time, currSpeed, currHeading);
			long currMaxTimeDiff = 0;
			long currTotalTimeDiff = 0;
			long prevTimeOffset = time - firstTime;
			for (currIndex = currIndex + 1; currIndex < rawTrajectoryPointID.length; currIndex++) {
				currTrajectoryPoint = rawTrajectoryPointID[currIndex].split(":");
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
					currMaxTimeDiff = currMaxTimeDiff > currTimeDiff ? currMaxTimeDiff : currTimeDiff;
					currTotalTimeDiff += currTimeDiff;
					currSpeed = Double.parseDouble(currTrajectoryPoint[2]);
					currHeading = Double.parseDouble(currTrajectoryPoint[4]);
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
				Pair<Integer, List<String>> newMatchResult = new Pair<>(tripID, new ArrayList<>());
				// test whether the matching result pass through the area and continuous
				if (startIndex == 0 && currIndex == rawTrajectoryPointID.length) {
					if (isMatchResultNotEnclosed(id2RoadWayMapping, matchedRoadWayID)) {
						continue;
					}
					for (String s : matchedRoadWayID) {
						if (id2VisitCountMapping.containsKey(s)) {
							int currCount = id2VisitCountMapping.get(s);
							id2VisitCountMapping.replace(s, currCount + 1);
							newMatchResult._2().add(s);
						} else {
							System.out.println("WARNING! The current trajectory is fully inside the map but the ground-truth result is " +
									"not.");
						}
					}
					if (newMatchResult._2().size() == 0)
						continue;
					numOfCompleteTraj++;
				} else {
//                    continue;
					// only part of the trajectory is selected as the raw trajectory
					newTraj.removeIf(point -> !isInsideInnerGraph(point.x(), point.y(), roadGraph, 1));
					for (String s : matchedRoadWayID) {
						if (id2VisitCountMapping.containsKey(s)) {
							int currCount = id2VisitCountMapping.get(s);
							id2VisitCountMapping.replace(s, currCount + 1);
							newMatchResult._2().add(s);
						} else if (newMatchResult._2().size() > 0)
							break;
					}
					if (newMatchResult._2().size() == 0)
						continue;
					numOfPartialTraj++;
				}
				trajectoryValidityCheck(newTraj);
				
				if (newTraj.size() < 2)
					continue;
				resultTrajList.add(newTraj);
				gtRouteMatchList.add(newMatchResult);
				maxTimeDiff = maxTimeDiff > currMaxTimeDiff ? maxTimeDiff : currMaxTimeDiff;
				totalTimeDiff += currTotalTimeDiff;
				totalNumOfPoint += newTraj.size();
				tripID++;
			}
		}
		writeTrajAndRouteMatchResults(resultTrajList, gtRouteMatchList, outputTrajFolder, outputGTRouteMatchFolder);
		
		roadGraph.setMaxVisitCount(0);
		for (RoadWay w : roadGraph.getWays()) {
			w.setVisitCount(id2VisitCountMapping.get(w.getID()));
			roadGraph.updateMaxVisitCount(id2VisitCountMapping.get(w.getID()));
		}
		LOG.debug(tripID + " trajectories extracted, including " + numOfCompleteTraj + " complete trajectories and " + numOfPartialTraj +
				" partial ones. The average length is " + (int) (totalNumOfPoint / tripID));
		LOG.debug("The maximum sampling interval is " + maxTimeDiff + "s, and the average time interval is "
				+ totalTimeDiff / (totalNumOfPoint - tripID) + ".");
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
			if (isMatchResultNotContinuous(id2RoadWayMapping, matchedRoadWayID))
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
		NewsonHMM2009 hmm = new NewsonHMM2009(rawGrantMap, prop);
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
					if (isMatchResultNotContinuous(id2RoadWayMapping, bestMatchWayList))
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
				if (!isMatchResultNotEnclosed(id2RoadWayMapping, matchedRoadWayID)) {
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
		
		writeTrajAndRouteMatchResults(resultTrajList, gtRouteMatchList, outputTrajFolder, outputGTRouteMatchFolder);
		
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
	 * Write the raw trajectories and the corresponding route matching results to the given folders.
	 *
	 * @param outputTrajList               Extracted trajectory list.
	 * @param gtRouteMatchResultList       The corresponding ground-truth route matching results.
	 * @param outputTrajFolder             Output folder for trajectories.
	 * @param outputRouteMatchResultFolder Output folder for ground-truth route matching results.
	 */
	private void writeTrajAndRouteMatchResults(List<Trajectory> outputTrajList, List<Pair<Integer, List<String>>> gtRouteMatchResultList,
											   String outputTrajFolder, String outputRouteMatchResultFolder) {
		if (outputTrajList == null || outputTrajList.isEmpty())
			throw new NullPointerException("The output trajectory result list is empty.");
		if (gtRouteMatchResultList == null || gtRouteMatchResultList.isEmpty())
			throw new NullPointerException("The output trajectory matching result list is empty.");
		if (outputTrajList.size() != gtRouteMatchResultList.size())
			throw new IllegalArgumentException("The counts of the output trajectories and their matching results are inconsistent.");
		TrajectoryWriter.writeTrajectories(outputTrajList, outputTrajFolder);
		MatchResultWriter.writeRouteMatchResults(gtRouteMatchResultList, outputRouteMatchResultFolder);
	}
	
	/**
	 * Check whether the ground-truth map-matching result satisfies the conditions that all roads must be included in the map area and the
	 * map-matching result must be continuous.
	 *
	 * @param id2RoadWayMapping The road ID and the corresponding road way object.
	 * @param matchedRoadWayID  The list of ground-truth map-matching result.
	 * @return False if all map-matching result satisfy the requirement, otherwise true.
	 */
	private boolean isMatchResultNotEnclosed(Map<String, RoadWay> id2RoadWayMapping, String[] matchedRoadWayID) {
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
	 * Check whether the ground-truth map-matching result contains a continuous matching sequence that is enclosed in the map area.
	 *
	 * @param id2RoadWayMapping The road ID and the corresponding road way object.
	 * @param matchedRoadWayID  The list of ground-truth map-matching result ID.
	 * @return False if the ground-truth matching result satisfies the requirement, otherwise true.
	 */
	private boolean isMatchResultNotContinuous(Map<String, RoadWay> id2RoadWayMapping, String[] matchedRoadWayID) {
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
	
	private void initializeMapping(RoadNetworkGraph roadGraph, Map<String, Integer> id2VisitCountMapping, Map<String, RoadWay> id2RoadWayMapping) {
		for (RoadWay w : roadGraph.getWays())
			if (!id2VisitCountMapping.containsKey(w.getID())) {
				id2VisitCountMapping.put(w.getID(), 0);
				id2RoadWayMapping.put(w.getID(), w);
			} else LOG.error("ERROR! The same road ID occurs twice: " + w.getID());
	}
	
	private boolean isInsideInnerGraph(double pointX, double pointY, RoadNetworkGraph roadGraph, double percentage) {
		double lonDiff = (roadGraph.getMaxLon() - roadGraph.getMinLon()) / 100 * percentage;
		double latDiff = (roadGraph.getMaxLat() - roadGraph.getMinLat()) / 100 * percentage;
		if (pointX >= roadGraph.getMinLon() + lonDiff && pointX <= roadGraph.getMaxLon() - lonDiff)
			return pointY >= roadGraph.getMinLat() + latDiff && pointY <= roadGraph.getMaxLat() - latDiff;
		return false;
	}
//
//	public void generateGTMatchResult(RoadNetworkGraph roadNetworkGraph, String rawTrajectories, double minDist) throws IOException, ExecutionException, InterruptedException {
//		LOG.info("Generated ground-truth result required, start generating matching result.");
//		BufferedReader brTrajectory = new BufferedReader(new FileReader(rawTrajectories + "beijingTrajectory"));
//		BufferedWriter bwRawTrajectory = new BufferedWriter(new FileWriter(rawTrajectories + "beijingTrajectoryNew"));
//
////        NewsonHMM2009 mapMatching = new NewsonHMM2009(CANDIDATE_RANGE, GAP_EXTENSION_RANGE, RANK_LENGTH, roadNetworkGraph);
//		String line;
//		int tripCount = 0;
//		List<Pair<Trajectory, String>> inputTrajList = new ArrayList<>();
//
//		// reset the cursor to the start of the current file
//		while ((line = brTrajectory.readLine()) != null) {
//			String[] trajectoryInfo = line.split(",");
//			String rawTrajectoryPoints = trajectoryInfo[28];
//			String[] rawTrajectoryPointID = rawTrajectoryPoints.split("\\|");
//
//			// generate trajectory object
//			Trajectory traj = new Trajectory(distFunc);
//			String[] firstTrajectoryPoint = rawTrajectoryPointID[0].split(":");
//			double firstLon = Double.parseDouble(firstTrajectoryPoint[0]) / 100000;
//			double firstLat = Double.parseDouble(firstTrajectoryPoint[1]) / 100000;
//			long firstTime = Long.parseLong(firstTrajectoryPoint[3]);
//			double currSpeed = Double.parseDouble(firstTrajectoryPoint[2]);
//			double currHeading = Double.parseDouble(firstTrajectoryPoint[4]);
//			TrajectoryPoint currPoint = new TrajectoryPoint(firstLon, firstLat, firstTime, currSpeed, currHeading, distFunc);
//			traj.add(currPoint);
//			double prevLon = firstLon;
//			double prevLat = firstLat;
//			for (int i = 1; i < rawTrajectoryPointID.length; i++) {
//				String[] currTrajectoryPoint = rawTrajectoryPointID[i].split(":");
//				double lon = firstLon + (Double.parseDouble(currTrajectoryPoint[0]) / 100000);
//				double lat = firstLat + (Double.parseDouble(currTrajectoryPoint[1]) / 100000);
//				long currTime = Long.parseLong(currTrajectoryPoint[3]);
//				double distance = distFunc.pointToPointDistance(prevLon, prevLat, lon, lat);
//				if (distance < minDist)
//					continue;
//				long time = firstTime + currTime;
//				currSpeed = Double.parseDouble(currTrajectoryPoint[2]);
//				currHeading = Double.parseDouble(currTrajectoryPoint[4]);
//				currPoint = new TrajectoryPoint(lon, lat, time, currSpeed, currHeading, distFunc);
//				traj.add(currPoint);
//				prevLon = lon;
//				prevLat = lat;
//			}
//			inputTrajList.add(new Pair<>(traj, rawTrajectoryPoints));
//		}
//
//		LOG.info("Start ground-truth generation, total number of input trajectory: " + inputTrajList.size());
//
//		Stream<Pair<Trajectory, String>> inputTrajStream = inputTrajList.stream();
//		NewsonHMM2009 hmm = new NewsonHMM2009(CANDIDATE_RANGE, GAP_EXTENSION_RANGE, 1, roadNetworkGraph, false);
//
//		// parallel processing
//		ForkJoinPool forkJoinPool = ForkJoinPool.commonPool();
//		ForkJoinTask<Stream<String>> matchedResultStream = forkJoinPool.submit(() -> inputTrajStream.parallel().map
//				(trajectory -> {
//					MatchResultWithUnmatchedTraj result = hmm.doMatching(trajectory._1());
//					if (result.getMatchResult().getCompleteMatchRouteAtRank(0).getRoadIDList().size() == 0 || !result.getUnmatchedTrajectoryList().isEmpty()) {
//						// matching result is empty or result contains breaks, waive the current trajectory
//						return null;
//					}
//					StringBuilder resultString = new StringBuilder();
//					resultString.append(trajectory._2()).append(",");
//					List<String> bestMatchWayList = result.getMatchResult().getCompleteMatchRouteAtRank(0).getRoadIDList();
//					for (int i = 0; i < bestMatchWayList.size() - 1; i++) {
//						String s = bestMatchWayList.get(i);
//						resultString.append(s).append("|");
//					}
//					resultString.append(bestMatchWayList.get(bestMatchWayList.size() - 1)).append("\n");
//					return resultString.toString();
//				}));
//
//		List<String> matchedResultList = matchedResultStream.get().collect(Collectors.toList());
//		for (String s : matchedResultList) {
//			if (s != null) {
//				bwRawTrajectory.write(s);
//				tripCount++;
//			}
//		}
//		LOG.debug("Ground-truth matching complete, total number of matched trajectories: " + tripCount + " start writing file");
//		bwRawTrajectory.close();
//		LOG.info("Ground-truth trajectory result generated.");
//	}
//
}