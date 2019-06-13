package preprocessing;

import org.apache.log4j.Logger;
import util.function.DistanceFunction;
import util.function.GreatCircleDistanceFunction;
import util.io.MapReader;
import util.io.MatchResultReader;
import util.io.TrajectoryReader;
import util.io.TrajectoryWriter;
import util.object.roadnetwork.RoadNetworkGraph;
import util.object.roadnetwork.RoadNode;
import util.object.roadnetwork.RoadWay;
import util.object.spatialobject.Segment;
import util.object.spatialobject.Trajectory;
import util.object.spatialobject.TrajectoryPoint;
import util.object.structure.Pair;
import util.settings.MapInferenceProperty;
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
		
		MapInferenceProperty property = new MapInferenceProperty();
		property.loadPropertiesFromResourceFile("mapinference.properties", args);
		long initTaskTime = System.currentTimeMillis();
		
		// setup java log
		String logFolder = property.getPropertyString("algorithm.mapinference.log.LogFolder");  // obtain the log folder from args
		String dataSet = property.getPropertyString("data.Dataset");
		String inputTrajFolder = property.getPropertyString("path.InputOriginalTrajectoryFolder");
		String gtMapFolder = property.getPropertyString("path.GroundTruthMapFolder");
		String gtMatchResultFolder = property.getPropertyString("path.GroundTruthOriginalMatchResultFolder");
		String dataSpec = property.getPropertyString("data.DataSpec");
		
		
		// log file name
		DistanceFunction distFunc = new GreatCircleDistanceFunction();    // only perform on Beijing dataset
		String logFileName = "syntheticTrajGeneration_" + dataSet + "_" + dataSpec + "_" + initTaskTime;
		// initialize log file
		MapServiceLogger.logInit(logFolder, logFileName);
		
		// use global dataset to evaluate the map-matching accuracy
		LOG = Logger.getLogger(TrajectoryGenerator.class);
		
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
		for (sigma = 0; sigma < 40; sigma += 5) {
			LOG.info("Start the generation on sigma=" + sigma);
			syntheticSpec = "_S" + sigma + "_R" + samplingInterval + "_C" + coverage;
			outputTrajFolderName = inputTrajFolder.substring(0, inputTrajFolder.length() - 1) + syntheticSpec + "/";        // remove the
			// last "/"
			outputTrajFolder = new File(outputTrajFolderName);
			if (outputTrajFolder.exists() && Objects.requireNonNull(outputTrajFolder.listFiles()).length > 0) {
				// the folder already exist, read the folder
				LOG.info("The synthetic dataset " + syntheticSpec + " has already been generated, total count: "
						+ Objects.requireNonNull(outputTrajFolder.listFiles()).length);
			} else {
				List<Trajectory> inputTrajList = TrajectoryReader.readTrajectoriesToList(inputTrajFolder, distFunc);
				List<Pair<Integer, List<String>>> gtMatchResultList = MatchResultReader.readRouteMatchResults(gtMatchResultFolder);
				List<Long> timeDiffList = new ArrayList<>();
				for (int i = 0; i < inputTrajList.size(); i++) {
					Trajectory traj = inputTrajList.get(i);
					if (Integer.parseInt(traj.getID()) != gtMatchResultList.get(i)._1()) {
						LOG.warn("trajectory ID inconsistency: " + traj.getID() + "," + gtMatchResultList.get(i)._1());
						timeDiffList.add(0L);
					} else {
						long currTimeDiff = traj.get(traj.size() - 1).time() - traj.get(0).time();
						timeDiffList.add(currTimeDiff);
					}
				}
				List<Trajectory> resultTraj = rawTrajGenerator(gtMatchResultList, timeDiffList, gtMap, sigma, samplingInterval);
				TrajectoryWriter.writeTrajectories(resultTraj, outputTrajFolderName);
				LOG.info("Trajectory written for sigma=" + sigma + " is done");
			}
		}
		int[] samplingValues = {1, 10, 20, 30, 45, 60, 90, 120, 180};
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
				List<Trajectory> inputTrajList = TrajectoryReader.readTrajectoriesToList(inputTrajFolder, distFunc);
				List<Pair<Integer, List<String>>> gtMatchResultList = MatchResultReader.readRouteMatchResults(gtMatchResultFolder);
				List<Long> timeDiffList = new ArrayList<>();
				for (int i = 0; i < inputTrajList.size(); i++) {
					Trajectory traj = inputTrajList.get(i);
					if (Integer.parseInt(traj.getID()) != gtMatchResultList.get(i)._1()) {
						LOG.warn("trajectory ID inconsistency: " + traj.getID() + "," + gtMatchResultList.get(i)._1());
						timeDiffList.add(0L);
					} else {
						long currTimeDiff = traj.get(traj.size() - 1).time() - traj.get(0).time();
						timeDiffList.add(currTimeDiff);
					}
				}
				List<Trajectory> resultTraj = rawTrajGenerator(gtMatchResultList, timeDiffList, gtMap, sigma, samplingInterval);
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
				List<Trajectory> inputTrajList = TrajectoryReader.readTrajectoriesToList(inputTrajFolder, distFunc);
				List<Pair<Integer, List<String>>> gtMatchResultList = MatchResultReader.readRouteMatchResults(gtMatchResultFolder);
				List<Long> timeDiffList = new ArrayList<>();
				for (int i = 0; i < inputTrajList.size(); i++) {
					Trajectory traj = inputTrajList.get(i);
					if (Integer.parseInt(traj.getID()) != gtMatchResultList.get(i)._1()) {
						LOG.warn("trajectory ID inconsistency: " + traj.getID() + "," + gtMatchResultList.get(i)._1());
						timeDiffList.add(0L);
					} else {
						long currTimeDiff = traj.get(traj.size() - 1).time() - traj.get(0).time();
						timeDiffList.add(currTimeDiff);
					}
				}
				List<Trajectory> resultTraj = rawTrajWithCoverageGenerator(gtMatchResultList, timeDiffList, gtMap, sigma,
						samplingInterval, coverage);
				TrajectoryWriter.writeTrajectories(resultTraj, outputTrajFolderName);
				LOG.info("Trajectory written for coverage=" + coverage + " is done");
			}
		}
	}
	
	/**
	 * Generate a list of synthetic trajectories that follows the given distribution and sampling rate.
	 *
	 * @param gtRouteList      The input list of routes and trajectory IDs, the input route may not be continuous on the map.
	 * @param timeDiffList     The time span of each route travel.
	 * @param map              The underlying map.
	 * @param sigma            The Gaussian function parameter. Pr(x\in[x-sigma,x+sigma])=0.6526, Pr(x\in[x-2*sigma,x+2*sigma])=0.9544
	 * @param samplingInterval The number of seconds per point.
	 * @return The generated trajectories, which has the same size as the input route list.
	 */
	private static List<Trajectory> rawTrajGenerator(List<Pair<Integer, List<String>>> gtRouteList, List<Long> timeDiffList,
													 RoadNetworkGraph map, double sigma, int samplingInterval) {
		DistanceFunction distFunc = map.getDistanceFunction();
		if (gtRouteList.size() != timeDiffList.size())
			throw new IllegalArgumentException("The size of the input route list and time different list is inconsistent.");
		Map<String, RoadWay> id2WayMap = new HashMap<>();
		for (RoadWay way : map.getWays()) {
			id2WayMap.put(way.getID(), way);
		}
		List<Trajectory> resultTrajList = new ArrayList<>();
		for (int i = 0; i < gtRouteList.size(); i++) {
			List<RoadWay> currRoute = new ArrayList<>();
			double length = 0;
			boolean isContinuous = true;
			for (String s : gtRouteList.get(i)._2()) {
				RoadWay currWay = id2WayMap.get(s);
				if (currRoute.size() != 0) {
					if (!currRoute.get(currRoute.size() - 1).getToNode().equals(currWay.getFromNode())) {
						LOG.warn("The input routes contains disconnected roads.");
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
			double interval = length / timeDiffList.get(i) * samplingInterval;        // the distance per point
			double remainLength = 0;    // used when the previous road way has left-over distance
			RoadNode startNode = currRoute.get(0).getFromNode();
			
			// add start point
			trajPointList.add(new TrajectoryPoint(startNode.lon(), startNode.lat(), trajPointList.size() + 1, distFunc));
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
			trajPointShift(trajPointList, sigma, distFunc);
			Trajectory currTraj = new Trajectory(gtRouteList.get(i)._1() + "", trajPointList);
			resultTrajList.add(currTraj);
		}
		return resultTrajList;
	}
	
	/**
	 * Generate a list of synthetic trajectories that follows the given distribution with sampling rate 1pts/sec and road coverage
	 * requirement.
	 *
	 * @param gtRouteList      The input list of routes and trajectory IDs, the input route may not be continuous on the map.
	 * @param timeDiffList     The time span of each route travel.
	 * @param map              The underlying map.
	 * @param sigma            The Gaussian function parameter. Pr(x\in[x-sigma,x+sigma])=0.6526, Pr(x\in[x-2*sigma,x+2*sigma])=0.9544
	 * @param samplingInterval The number of seconds per point.
	 * @param percentage       The percentage of roads to be covered.
	 * @return The generated trajectories, which has the same size as the input route list.
	 */
	private static List<Trajectory> rawTrajWithCoverageGenerator(List<Pair<Integer, List<String>>> gtRouteList, List<Long> timeDiffList,
																 RoadNetworkGraph map, double sigma, int samplingInterval, double percentage) {
		Set<String> coveredWaySet = new HashSet<>();
		boolean isNewRoadOccurred;
		double mapWaySize = map.getWays().size();
		List<Pair<Integer, List<String>>> tempGTRouteList = new ArrayList<>();
		List<Long> tempTimeDiffList = new ArrayList<>();
		for (int i = 0; i < gtRouteList.size(); i++) {
			isNewRoadOccurred = false;
			for (String s : gtRouteList.get(i)._2()) {
				if (!coveredWaySet.contains(s)) {
					isNewRoadOccurred = true;
					break;
				}
			}
			if (isNewRoadOccurred) {
				coveredWaySet.addAll(gtRouteList.get(i)._2());
				tempGTRouteList.add(gtRouteList.get(i));
				tempTimeDiffList.add(timeDiffList.get(i));
				if (coveredWaySet.size() >= mapWaySize / 100 * percentage)
					break;
			}
		}
		if (coveredWaySet.size() < mapWaySize / 100 * percentage)
			LOG.warn("Cannot achieve required road coverage, the actual coverage is: " + (double) coveredWaySet.size() / mapWaySize * 100 +
					"%");
		return rawTrajGenerator(tempGTRouteList, tempTimeDiffList, map, sigma, samplingInterval);
	}
	
	/**
	 * Randomly shift every point in the trajectories into a region whose distance to its original position is less than
	 * <tt>errorRadius</tt>. The randomness follows the Gaussian distribution.
	 *
	 * @param trajPointList The input trajectory point list.
	 * @param sigma         The Gaussian parameter.
	 * @param distFunc      The distance function.
	 */
	private static void trajPointShift(List<TrajectoryPoint> trajPointList, double sigma, DistanceFunction distFunc) {
		Random random = new Random(10);
		if (sigma == 0) {
			return;        // no shift required
		}
		for (TrajectoryPoint point : trajPointList) {
			double newLon = point.x() + distFunc.getCoordinateOffsetX(random.nextGaussian() * sigma, point.y());
			double newLat = point.y() + distFunc.getCoordinateOffsetY(random.nextGaussian() * sigma, point.x());
			point.setPoint(newLon, newLat, distFunc);
		}
	}
}
