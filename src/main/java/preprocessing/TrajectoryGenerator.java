package preprocessing;

import algorithm.mapmatching.stmatching.FeatureSTMapMatching;
import org.apache.log4j.Logger;
import util.function.DistanceFunction;
import util.function.GreatCircleDistanceFunction;
import util.io.*;
import util.object.roadnetwork.RoadNetworkGraph;
import util.object.roadnetwork.RoadNode;
import util.object.roadnetwork.RoadWay;
import util.object.spatialobject.Rect;
import util.object.spatialobject.Segment;
import util.object.spatialobject.Trajectory;
import util.object.spatialobject.TrajectoryPoint;
import util.object.structure.Pair;
import util.object.structure.PointMatch;
import util.object.structure.Triplet;
import util.settings.MapInferenceProperty;
import util.settings.MapMatchingProperty;
import util.settings.MapServiceLogger;

import java.io.File;
import java.util.*;

/**
 * Generate synthetic trajectories according to the given parameters.
 *
 * @author uqpchao
 * Created 3/06/2019
 */
public class TrajectoryGenerator {
	
	private static Logger LOG;
	
	public static void main(String[] args) {
		
		// setup java log
//		MapInferenceProperty property = new MapInferenceProperty();
//		property.loadPropertiesFromResourceFile("mapinference.properties", args);
//		String logFolder = property.getPropertyString("algorithm.mapinference.log.LogFolder");  // obtain the log folder from args
		MapMatchingProperty property = new MapMatchingProperty();
		property.loadPropertiesFromResourceFile("mapmatching.properties", args);
		String logFolder = property.getPropertyString("algorithm.mapmatching.log.LogFolder");  // obtain the log folder from args
		String dataSet = property.getPropertyString("data.Dataset");
		String dataSpec = property.getPropertyString("data.DataSpec");
		long initTaskTime = System.currentTimeMillis();
		
		// log file name
		String logFileName = "syntheticTrajGeneration_" + dataSet + "_" + dataSpec + "_" + initTaskTime;
		// initialize log file
		MapServiceLogger.logInit(logFolder, logFileName);
		
		// use global dataset to evaluate the map-matching accuracy
		LOG = Logger.getLogger(TrajectoryGenerator.class);
		
		startMapMatchingTrajectoryGen(property);
//		startMapInferenceTrajectoryGen(property);
	}
	
	private static void startMapInferenceTrajectoryGen(MapInferenceProperty property) {
		DistanceFunction distFunc = new GreatCircleDistanceFunction();    // only perform on Beijing dataset
		String inputTrajFolder = property.getPropertyString("path.InputOriginalTrajectoryFolder");
		String gtMapFolder = property.getPropertyString("path.GroundTruthMapFolder");
		String gtMatchResultFolder = property.getPropertyString("path.GroundTruthOriginalRouteMatchResultFolder");
		int sigma;    // parameter for trajectory noise level
		int samplingInterval;    // trajectory sampling interval minimum 1s
		int coverage;    // road coverage among the map region
		String syntheticSpec;
		String outputTrajFolderName;
		File outputTrajFolder;
		
		LOG.info("Start the synthetic trajectory generation with various sigma.");
		RoadNetworkGraph gtMap = MapReader.readMap(gtMapFolder + "0.txt", false, distFunc);
		samplingInterval = 5;
		coverage = -1;
		int[] sigmaValues = {0, 5, 10, 20, 35, 50};
		for (int sigmaValue : sigmaValues) {
			LOG.info("Start the generation on sigma=" + sigmaValue);
			syntheticSpec = "_S" + sigmaValue + "_R" + samplingInterval + "_C" + coverage;
			outputTrajFolderName = inputTrajFolder.substring(0, inputTrajFolder.length() - 1) + syntheticSpec + "/";        // remove the
			// last "/"
			outputTrajFolder = new File(outputTrajFolderName);
			if (outputTrajFolder.exists() && Objects.requireNonNull(outputTrajFolder.listFiles()).length > 0) {
				// the folder already exist, read the folder
				LOG.info("The synthetic dataset " + syntheticSpec + " has already been generated, total count: "
						+ Objects.requireNonNull(outputTrajFolder.listFiles()).length);
			} else {
				List<Trajectory> inputTrajList = TrajectoryReader.readTrajectoriesToList(inputTrajFolder, 1, distFunc);
				List<Pair<Integer, List<String>>> gtMatchResultList = MatchResultReader.readRouteMatchResults(gtMatchResultFolder);
				Map<Integer, Long> id2timeDiffMap = new HashMap<>();
				for (Trajectory traj : inputTrajList) {
					long currTimeDiff = traj.get(traj.size() - 1).time() - traj.get(0).time();
					id2timeDiffMap.put(Integer.parseInt(traj.getID()), currTimeDiff);
				}
				List<Trajectory> resultTraj = rawTrajGenerator(gtMatchResultList, id2timeDiffMap, gtMap, sigmaValue, samplingInterval)._1();
				TrajectoryWriter.writeTrajectories(resultTraj, outputTrajFolderName);
				LOG.info("Trajectory written for sigma=" + sigmaValue + " is done");
			}
		}
		int[] samplingValues = {1, 10, 30, 60, 120, 180};
		sigma = 0;
		for (int samplingValue : samplingValues) {
			samplingInterval = samplingValue;
			LOG.info("Start the generation on sampling rate=" + samplingInterval);
			syntheticSpec = "_S" + sigma + "_R" + samplingInterval + "_C" + coverage;
			outputTrajFolderName = inputTrajFolder.substring(0, inputTrajFolder.length() - 1) + syntheticSpec + "/";        // remove the
			// last "/"
			outputTrajFolder = new File(outputTrajFolderName);
			if (outputTrajFolder.exists() && Objects.requireNonNull(outputTrajFolder.listFiles()).length > 0) {
				// the folder already exist, read the folder
				LOG.info("The synthetic dataset " + syntheticSpec + " has already been generated, total count: "
						+ Objects.requireNonNull(outputTrajFolder.listFiles()).length);
			} else {
				List<Trajectory> inputTrajList = TrajectoryReader.readTrajectoriesToList(inputTrajFolder, 1, distFunc);
				List<Pair<Integer, List<String>>> gtMatchResultList = MatchResultReader.readRouteMatchResults(gtMatchResultFolder);
				Map<Integer, Long> id2timeDiffMap = new HashMap<>();
				for (Trajectory traj : inputTrajList) {
					long currTimeDiff = traj.get(traj.size() - 1).time() - traj.get(0).time();
					id2timeDiffMap.put(Integer.parseInt(traj.getID()), currTimeDiff);
				}
				List<Trajectory> resultTraj = rawTrajGenerator(gtMatchResultList, id2timeDiffMap, gtMap, sigma, samplingInterval)._1();
				TrajectoryWriter.writeTrajectories(resultTraj, outputTrajFolderName);
				LOG.info("Trajectory written for sampling rate=" + samplingInterval + " is done");
			}
		}
		sigma = 0;
		samplingInterval = 1;
		for (coverage = 10; coverage <= 60; coverage += 10) {
			LOG.info("Start the generation on coverage=" + coverage);
			syntheticSpec = "_S" + sigma + "_R" + samplingInterval + "_C" + coverage;
			outputTrajFolderName = inputTrajFolder.substring(0, inputTrajFolder.length() - 1) + syntheticSpec + "/";        // remove the
			// last "/"
			outputTrajFolder = new File(outputTrajFolderName);
			if (outputTrajFolder.exists() && Objects.requireNonNull(outputTrajFolder.listFiles()).length > 0) {
				// the folder already exist, read the folder
				LOG.info("The synthetic dataset " + syntheticSpec + " has already been generated, total count: "
						+ Objects.requireNonNull(outputTrajFolder.listFiles()).length);
			} else {
				List<Trajectory> inputTrajList = TrajectoryReader.readTrajectoriesToList(inputTrajFolder, 1, distFunc);
				List<Pair<Integer, List<String>>> gtMatchResultList = MatchResultReader.readRouteMatchResults(gtMatchResultFolder);
				Map<Integer, Long> id2timeDiffMap = new HashMap<>();
				for (Trajectory traj : inputTrajList) {
					long currTimeDiff = traj.get(traj.size() - 1).time() - traj.get(0).time();
					id2timeDiffMap.put(Integer.parseInt(traj.getID()), currTimeDiff);
				}
				List<Trajectory> resultTraj = rawTrajWithCoverageGenerator(gtMatchResultList, id2timeDiffMap, gtMap, sigma,
						samplingInterval, coverage);
				TrajectoryWriter.writeTrajectories(resultTraj, outputTrajFolderName);
				LOG.info("Trajectory written for coverage=" + coverage + " is done");
			}
		}
	}
	
	private static void startMapMatchingTrajectoryGen(MapMatchingProperty property) {
		DistanceFunction distFunc = new GreatCircleDistanceFunction();    // only perform on Beijing dataset
		String inputTrajFolder = property.getPropertyString("path.InputOriginalTrajectoryFolder");
		String gtMapFolder = property.getPropertyString("path.GroundTruthMapFolder");
		String gtRouteMatchOriginalResultFolder = property.getPropertyString("path.GroundTruthOriginalRouteMatchResultFolder");
		String gtRouteMatchResultFolder = property.getPropertyString("path.GroundTruthSyntheticRouteMatchBaseFolder");
		String gtPointMatchResultFolder = property.getPropertyString("path.GroundTruthSyntheticPointMatchBaseFolder");
		int sigma;    // parameter for trajectory noise level
		int samplingInterval;    // trajectory sampling interval minimum 1s
		int outlierPercentage;    // road coverage among the map region
		String syntheticSpec;
		String outputTrajFolderName;
		File outputTrajFolder;
		
		LOG.info("Start the synthetic trajectory generation with various sigma.");
		RoadNetworkGraph gtMap = MapReader.readMap(gtMapFolder + "0.txt", false, distFunc);
		samplingInterval = 5;
		outlierPercentage = 0;
		int[] sigmaValues = {0, 5, 10, 20, 35, 50};
		for (int sigmaValue : sigmaValues) {
			LOG.info("Start the generation on sigma=" + sigmaValue);
			syntheticSpec = "_S" + sigmaValue + "_R" + samplingInterval + "_O" + outlierPercentage;
			outputTrajFolderName = inputTrajFolder.substring(0, inputTrajFolder.length() - 1) + syntheticSpec + "/";        // remove the
			// last "/"
			outputTrajFolder = new File(outputTrajFolderName);
			if (outputTrajFolder.exists() && Objects.requireNonNull(outputTrajFolder.listFiles()).length > 0) {
				// the folder already exist, read the folder
				LOG.info("The synthetic dataset " + syntheticSpec + " has already been generated, total count: "
						+ Objects.requireNonNull(outputTrajFolder.listFiles()).length);
			} else {
				List<Trajectory> inputTrajList = TrajectoryReader.readTrajectoriesToList(inputTrajFolder, 1, distFunc);
				List<Pair<Integer, List<String>>> gtMatchResultList = MatchResultReader.readRouteMatchResults(gtRouteMatchOriginalResultFolder);
				Map<Integer, Long> id2timeDiffMap = new HashMap<>();
				for (Trajectory traj : inputTrajList) {
					long currTimeDiff = traj.get(traj.size() - 1).time() - traj.get(0).time();
					id2timeDiffMap.put(Integer.parseInt(traj.getID()), currTimeDiff);
				}
				Pair<List<Trajectory>, List<Triplet<Integer, List<String>, List<PointMatch>>>> resultTraj = rawTrajGenerator(gtMatchResultList,
						id2timeDiffMap, gtMap, sigmaValue, samplingInterval);
				List<Pair<Integer, List<String>>> routeMatchPairList = new ArrayList<>();
				List<Pair<Integer, List<PointMatch>>> pointMatchPairList = new ArrayList<>();
				for (Triplet<Integer, List<String>, List<PointMatch>> triplet : resultTraj._2()) {
					routeMatchPairList.add(new Pair<>(triplet._1(), triplet._2()));
					pointMatchPairList.add(new Pair<>(triplet._1(), triplet._3()));
				}
				TrajectoryWriter.writeTrajectories(resultTraj._1(), outputTrajFolderName);
				MatchResultWriter.writeRouteMatchResults(routeMatchPairList, gtRouteMatchResultFolder + syntheticSpec + "/");
				MatchResultWriter.writePointMatchResults(pointMatchPairList, gtPointMatchResultFolder + syntheticSpec + "/");
				LOG.info("Trajectory written for sigma=" + sigmaValue + " is done");
			}
		}
		int[] samplingValues = {1, 10, 20, 30, 60, 90, 120, 180};
		sigma = 0;
		for (int samplingValue : samplingValues) {
			samplingInterval = samplingValue;
			LOG.info("Start the generation on sampling rate=" + samplingInterval);
			syntheticSpec = "_S" + sigma + "_R" + samplingInterval + "_O" + outlierPercentage;
			outputTrajFolderName = inputTrajFolder.substring(0, inputTrajFolder.length() - 1) + syntheticSpec + "/";        // remove the
			// last "/"
			outputTrajFolder = new File(outputTrajFolderName);
			if (outputTrajFolder.exists() && Objects.requireNonNull(outputTrajFolder.listFiles()).length > 0) {
				// the folder already exist, read the folder
				LOG.info("The synthetic dataset " + syntheticSpec + " has already been generated, total count: "
						+ Objects.requireNonNull(outputTrajFolder.listFiles()).length);
			} else {
				List<Trajectory> inputTrajList = TrajectoryReader.readTrajectoriesToList(inputTrajFolder, 1, distFunc);
				List<Pair<Integer, List<String>>> gtMatchResultList = MatchResultReader.readRouteMatchResults(gtRouteMatchOriginalResultFolder);
				Map<Integer, Long> id2timeDiffMap = new HashMap<>();
				for (Trajectory traj : inputTrajList) {
					long currTimeDiff = traj.get(traj.size() - 1).time() - traj.get(0).time();
					id2timeDiffMap.put(Integer.parseInt(traj.getID()), currTimeDiff);
				}
				Pair<List<Trajectory>, List<Triplet<Integer, List<String>, List<PointMatch>>>> resultTraj = rawTrajGenerator(gtMatchResultList,
						id2timeDiffMap, gtMap, sigma, samplingInterval);
				List<Pair<Integer, List<String>>> routeMatchPairList = new ArrayList<>();
				List<Pair<Integer, List<PointMatch>>> pointMatchPairList = new ArrayList<>();
				for (Triplet<Integer, List<String>, List<PointMatch>> triplet : resultTraj._2()) {
					routeMatchPairList.add(new Pair<>(triplet._1(), triplet._2()));
					pointMatchPairList.add(new Pair<>(triplet._1(), triplet._3()));
				}
				TrajectoryWriter.writeTrajectories(resultTraj._1(), outputTrajFolderName);
				MatchResultWriter.writeRouteMatchResults(routeMatchPairList, gtRouteMatchResultFolder + syntheticSpec + "/");
				MatchResultWriter.writePointMatchResults(pointMatchPairList, gtPointMatchResultFolder + syntheticSpec + "/");
				LOG.info("Trajectory written for sampling rate=" + samplingInterval + " is done");
			}
		}
		sigma = 5;
		samplingInterval = 5;
		for (outlierPercentage = 2; outlierPercentage <= 20; outlierPercentage += 2) {
			LOG.info("Start the generation on outlier percentage=" + outlierPercentage);
			syntheticSpec = "_S" + sigma + "_R" + samplingInterval + "_O" + outlierPercentage;
			outputTrajFolderName = inputTrajFolder.substring(0, inputTrajFolder.length() - 1) + syntheticSpec + "/";        // remove the
			// last "/"
			outputTrajFolder = new File(outputTrajFolderName);
			if (outputTrajFolder.exists() && Objects.requireNonNull(outputTrajFolder.listFiles()).length > 0) {
				// the folder already exist, read the folder
				LOG.info("The synthetic dataset " + syntheticSpec + " has already been generated, total count: "
						+ Objects.requireNonNull(outputTrajFolder.listFiles()).length);
			} else {
				List<Trajectory> inputTrajList = TrajectoryReader.readTrajectoriesToList(inputTrajFolder, 1, distFunc);
				List<Pair<Integer, List<String>>> gtMatchResultList = MatchResultReader.readRouteMatchResults(gtRouteMatchOriginalResultFolder);
				Map<Integer, Long> id2timeDiffMap = new HashMap<>();
				for (Trajectory traj : inputTrajList) {
					long currTimeDiff = traj.get(traj.size() - 1).time() - traj.get(0).time();
					id2timeDiffMap.put(Integer.parseInt(traj.getID()), currTimeDiff);
				}
				Pair<List<Trajectory>, List<Triplet<Integer, List<String>, List<PointMatch>>>> resultTraj = rawTrajGenerator(gtMatchResultList,
						id2timeDiffMap, gtMap, sigma, samplingInterval);
				for (Trajectory currTraj : resultTraj._1()) {
					List<TrajectoryPoint> outlierPoint = new ArrayList<>();
					int requiredOutlierCount = Math.max(currTraj.size() * outlierPercentage / 100, 1);
					Random random = new Random(10);
					Set<Integer> outlierIndex = new HashSet<>();
					while (requiredOutlierCount > 0) {
						int index = random.nextInt(currTraj.size());
						if (!outlierIndex.contains(index)) {
							outlierPoint.add(currTraj.get(index));
							outlierIndex.add(index);
							requiredOutlierCount--;
						}
					}
					fixedTrajPointShift(outlierPoint, sigma * 10, gtMap.getBoundary(), distFunc);
				}
				List<Pair<Integer, List<String>>> routeMatchPairList = new ArrayList<>();
				List<Pair<Integer, List<PointMatch>>> pointMatchPairList = new ArrayList<>();
				for (Triplet<Integer, List<String>, List<PointMatch>> triplet : resultTraj._2()) {
					routeMatchPairList.add(new Pair<>(triplet._1(), triplet._2()));
					pointMatchPairList.add(new Pair<>(triplet._1(), triplet._3()));
				}
				TrajectoryWriter.writeTrajectories(resultTraj._1(), outputTrajFolderName);
				MatchResultWriter.writeRouteMatchResults(routeMatchPairList, gtRouteMatchResultFolder + syntheticSpec + "/");
				MatchResultWriter.writePointMatchResults(pointMatchPairList, gtPointMatchResultFolder + syntheticSpec + "/");
				LOG.info("Trajectory written for outlier percentage=" + outlierPercentage + "% is done");
			}
		}
	}
	
	/**
	 * Generate a list of synthetic trajectories that follows the given distribution and sampling rate.
	 *
	 * @param gtRouteList      The input list of routes and trajectory IDs, the input route may not be continuous on the map.
	 * @param id2timeDiffMap   The trajectory ID and its time span of the travel
	 * @param map              The underlying map.
	 * @param sigma            The Gaussian function parameter. Pr(x\in[x-sigma,x+sigma])=0.6526, Pr(x\in[x-2*sigma,x+2*sigma])=0.9544
	 * @param samplingInterval The number of seconds per point.
	 * @return The generated trajectories, ground-truth route and point matching result.
	 */
	private static Pair<List<Trajectory>, List<Triplet<Integer, List<String>, List<PointMatch>>>> rawTrajGenerator(List<Pair<Integer,
			List<String>>> gtRouteList, Map<Integer, Long> id2timeDiffMap, RoadNetworkGraph map, double sigma, int samplingInterval) {
		DistanceFunction distFunc = map.getDistanceFunction();
		Map<String, RoadWay> id2WayMap = new HashMap<>();
		for (RoadWay way : map.getWays()) {
			id2WayMap.put(way.getID(), way);
		}
		List<Trajectory> resultTrajList = new ArrayList<>();
		List<Triplet<Integer, List<String>, List<PointMatch>>> gtMatchResult = new ArrayList<>();
		for (Pair<Integer, List<String>> integerListPair : gtRouteList) {
			List<RoadWay> currRoute = new ArrayList<>();
			double length = 0;
			boolean isContinuous = true;
			for (String s : integerListPair._2()) {
				RoadWay currWay = id2WayMap.get(s);
				if (currRoute.size() != 0) {
					if (!currRoute.get(currRoute.size() - 1).getToNode().equals(currWay.getFromNode())) {
						LOG.warn("The input routes contain disconnected roads.");
						isContinuous = false;
						break;
					}
				}
				currRoute.add(currWay);
				length += currWay.getLength();
			}
			if (!isContinuous)    // the current route is omitted
				continue;
			List<TrajectoryPoint> trajPointList = new ArrayList<>();
			if (!id2timeDiffMap.containsKey(integerListPair._1()))
				throw new IllegalArgumentException("The ground-truth route id cannot be found in time difference list: " + integerListPair._1());
			double interval = length / id2timeDiffMap.get(integerListPair._1()) * samplingInterval;        // the distance per point
			double remainLength = 0;    // used when the previous road way has left-over distance
			RoadNode startNode = currRoute.get(0).getFromNode();
			
			// add start point
			TrajectoryPoint currPoint = new TrajectoryPoint(startNode.lon(), startNode.lat(), trajPointList.size() + 1, distFunc);
			trajPointList.add(currPoint);
			
			for (RoadWay roadWay : currRoute) {
				for (Segment edge : roadWay.getEdges()) {
					remainLength += edge.length();
					while (remainLength > interval) {    // insert new node
						remainLength -= interval;
						double ratio = remainLength / edge.length();
						double currLon = edge.x2() - (edge.x2() - edge.x1()) * ratio;
						double currLat = edge.y2() - (edge.y2() - edge.y1()) * ratio;
						trajPointList.add(new TrajectoryPoint(currLon, currLat, trajPointList.size() + 1, distFunc));
					}
				}
			}
			RoadNode endNode = currRoute.get(currRoute.size() - 1).getToNode();
			
			// add start point
			trajPointList.add(new TrajectoryPoint(endNode.lon(), endNode.lat(), trajPointList.size() + 1, distFunc));
			trajPointShift(trajPointList, sigma, map.getBoundary(), distFunc);
			Trajectory currTraj = new Trajectory(integerListPair._1() + "", trajPointList);
			List<PointMatch> currPointMatchList = FeatureSTMapMatching.findPointMatch(currTraj, integerListPair._2(), map);
			resultTrajList.add(currTraj);
			gtMatchResult.add(new Triplet<>(integerListPair._1(), integerListPair._2(), currPointMatchList));
		}
		return new Pair<>(resultTrajList, gtMatchResult);
	}
	
	/**
	 * Generate a list of synthetic trajectories that follows the given distribution with sampling rate 1pts/sec and road coverage
	 * requirement.
	 *
	 * @param gtRouteList      The input list of routes and trajectory IDs, the input route may not be continuous on the map.
	 * @param id2timeDiffMap   The mapping between trajectory id and its time span of the travel.
	 * @param map              The underlying map.
	 * @param sigma            The Gaussian function parameter. Pr(x\in[x-sigma,x+sigma])=0.6526, Pr(x\in[x-2*sigma,x+2*sigma])=0.9544
	 * @param samplingInterval The number of seconds per point.
	 * @param percentage       The percentage of roads to be covered.
	 * @return The generated trajectories, which has the same size as the input route list.
	 */
	private static List<Trajectory> rawTrajWithCoverageGenerator(List<Pair<Integer, List<String>>> gtRouteList,
																 Map<Integer, Long> id2timeDiffMap, RoadNetworkGraph map, double sigma,
																 int samplingInterval, double percentage) {
		Set<String> coveredWaySet = new HashSet<>();
		boolean isNewRoadOccurred;
		double mapWaySize = map.getWays().size();
		List<Pair<Integer, List<String>>> tempGTRouteList = new ArrayList<>();
		for (Pair<Integer, List<String>> integerListPair : gtRouteList) {
			isNewRoadOccurred = false;
			for (String s : integerListPair._2()) {
				if (!coveredWaySet.contains(s)) {
					isNewRoadOccurred = true;
					break;
				}
			}
			if (isNewRoadOccurred) {
				coveredWaySet.addAll(integerListPair._2());
				tempGTRouteList.add(integerListPair);
				if (coveredWaySet.size() >= mapWaySize / 100 * percentage)
					break;
			}
		}
		if (coveredWaySet.size() < mapWaySize / 100 * percentage)
			LOG.warn("Cannot achieve required road coverage, the actual coverage is: " + (double) coveredWaySet.size() / mapWaySize * 100 +
					"%");
		return rawTrajGenerator(tempGTRouteList, id2timeDiffMap, map, sigma, samplingInterval)._1();
	}
	
	/**
	 * Randomly shift every point in the trajectories into a region whose distance to its original position is less than
	 * <tt>errorRadius</tt>. The randomness follows the Gaussian distribution.
	 *
	 * @param trajPointList The input trajectory point list.
	 * @param sigma         The Gaussian parameter.
	 * @param boundary      The boundary of the map region.
	 * @param distFunc      The distance function.
	 */
	private static void trajPointShift(List<TrajectoryPoint> trajPointList, double sigma, Rect boundary, DistanceFunction distFunc) {
		Random random = new Random(10);
		if (sigma == 0) {
			return;        // no shift required
		}
		for (TrajectoryPoint point : trajPointList) {
			double newLon, newLat;
			do {
				newLon = point.x() + distFunc.getCoordinateOffsetX(random.nextGaussian() * sigma, point.y());
				newLat = point.y() + distFunc.getCoordinateOffsetY(random.nextGaussian() * sigma, point.x());
			} while (!boundary.contains(newLon, newLat));
			point.setPoint(newLon, newLat, distFunc);
		}
	}
	
	/**
	 * Shift every point in the trajectories into a region whose distance to its original position is fixed distance.
	 *
	 * @param trajPointList The input trajectory point list.
	 * @param distance      The fixed distance.
	 * @param boundary      The boundary of the map region.
	 * @param distFunc      The distance function.
	 */
	private static void fixedTrajPointShift(List<TrajectoryPoint> trajPointList, double distance, Rect boundary, DistanceFunction distFunc) {
		Random random = new Random(10);
		if (distance == 0) {
			return;        // no shift required
		}
		for (TrajectoryPoint point : trajPointList) {
			double newLon, newLat, lonDiff, latDiff;
			do {
				lonDiff = random.nextDouble();
				latDiff = Math.sqrt(1 - Math.pow(lonDiff, 2));
				newLon = point.x() + distFunc.getCoordinateOffsetX(lonDiff * distance, point.y());
				newLat = point.y() + distFunc.getCoordinateOffsetY(latDiff * distance, point.x());
			} while (!boundary.contains(newLon, newLat));
			point.setPoint(newLon, newLat, distFunc);
		}
	}
}
