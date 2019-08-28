package algorithm.mapmatching;

import algorithm.mapmatching.simpleHMM.SimpleHMMMatching;
import algorithm.mapmatching.stmatching.FeatureSTMapMatching;
import algorithm.mapmatching.weightBased.WeightBasedMM;
import org.apache.log4j.Logger;
import util.function.DistanceFunction;
import util.function.GreatCircleDistanceFunction;
import util.io.*;
import util.object.roadnetwork.RoadNetworkGraph;
import util.object.spatialobject.Trajectory;
import util.object.spatialobject.TrajectoryPoint;
import util.object.structure.SimpleTrajectoryMatchResult;
import util.settings.BaseProperty;
import util.settings.MapMatchingProperty;
import util.settings.MapServiceLogger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Entry for running map-matching algorithms and evaluation.
 *
 * @author Hellisk
 * @since 30/03/2019
 */
public class MapMatchingMain {
	
	public static void main(String[] args) throws IOException, ExecutionException, InterruptedException {
		
		// initialize arguments
		MapMatchingProperty property = new MapMatchingProperty();
		property.loadPropertiesFromResourceFile("mapmatching.properties", args);
		long initTaskTime = System.currentTimeMillis();
		
		// setup settings
		String logFolder = property.getPropertyString("algorithm.mapmatching.log.LogFolder");  // obtain the log folder from args
		String dataSet = property.getPropertyString("data.Dataset");
		String inputTrajFolder = property.getPropertyString("path.InputTrajectoryFolder");
		String inputMapFolder = property.getPropertyString("path.InputMapFolder");
		String outputMatchResultFolder = property.getPropertyString("path.OutputMatchResultFolder");
		String matchingMethod = property.getPropertyString("algorithm.mapmatching.MatchingMethod");
		String dataSpec = property.getPropertyString("data.DataSpec");
		int numOfThreads = property.getPropertyInteger("algorithm.mapmatching.NumOfThreads");
		boolean isOnline = matchingMethod.substring(0, 2).equals("ON");        // check if the current matching is online matching
		DistanceFunction distFunc;
		String logFileName;
		String parameters = "";
		// log file name
		switch (matchingMethod) {
			case "HMM":
				parameters = property.getPropertyString("algorithm.mapmatching.CandidateRange") + "_"
						+ property.getPropertyString("algorithm.mapmatching.Sigma") + "_"
						+ property.getPropertyString("algorithm.mapmatching.hmm.Beta");
				break;
			case "FST":
				parameters = property.getPropertyString("algorithm.mapmatching.CandidateRange") + "_"
						+ property.getPropertyString("algorithm.mapmatching.Sigma") + "_"
						+ property.getPropertyString("algorithm.mapmatching.fst.CandidateSize") + "_"
						+ property.getPropertyString("algorithm.mapmatching.fst.Omega");
				break;
			case "WGT":
				parameters = property.getPropertyString("algorithm.mapmatching.CandidateRange") + "_"
						+ property.getPropertyString("algorithm.mapmatching.Sigma");
				break;
			default:
				parameters = "null";
				break;
		}
		
		// initialize log file
		logFileName = "matching_" + dataSet + "_" + matchingMethod + "_" + dataSpec + "_" + parameters + "_" + initTaskTime;
		MapServiceLogger.logInit(logFolder, logFileName);
		
		final Logger LOG = Logger.getLogger(MapMatchingMain.class);
		// use global dataset to evaluate the map-matching accuracy
		LOG.info("Map-matching on the " + dataSet + " dataset using method " + matchingMethod + ".");
		
		long startTaskTime = System.currentTimeMillis();    // the start of the map-matching process
		// map-matching process
		List<SimpleTrajectoryMatchResult> matchResultList = new ArrayList<>();
		if (dataSet.equals("Global")) {
			String rawDataFolder = property.getPropertyString("path.RawDataFolder");
			GlobalTrajectoryLoader reader = new GlobalTrajectoryLoader(rawDataFolder);
			GlobalMapLoader mapReader = new GlobalMapLoader(rawDataFolder);
			int trajPointCount = 0;
			for (int i = 0; i < reader.getNumOfTrajectory(); i++) {
				Trajectory currTraj = reader.readInputTrajectory(i);
				Iterator<TrajectoryPoint> iterator = currTraj.getSTPoints().iterator();
				TrajectoryPoint prevPoint = iterator.next();
				while (iterator.hasNext()) {
					TrajectoryPoint currPoint = iterator.next();
					if (currPoint.time() <= prevPoint.time()) {
						LOG.warn("The input time is not ordered: " + currPoint.time() + "," + prevPoint.time());
						currTraj.remove(currPoint);
					} else prevPoint = currPoint;
				}
				trajPointCount += currTraj.size();
				RoadNetworkGraph currMap = mapReader.readRawMap(i);
				MapMatchingMethod mapMatching = chooseMatchMethod(matchingMethod, currMap, property);
				SimpleTrajectoryMatchResult matchResult;
				if (isOnline)
					matchResult = mapMatching.onlineMatching(currTraj);
				else
					matchResult = mapMatching.offlineMatching(currTraj);
				
				matchResultList.add(matchResult);
			}
			LOG.info("Map matching finished, total time spent:" + (System.currentTimeMillis() - startTaskTime) / 1000 + "seconds");
			MatchResultWriter.writeMatchResults(matchResultList, outputMatchResultFolder);
			
			System.out.println("Total number of trajectory points is " + trajPointCount);
		} else if (dataSet.contains("Beijing")) {
			distFunc = new GreatCircleDistanceFunction();
			RoadNetworkGraph roadMap = MapReader.readMap(inputMapFolder + "0.txt", false, distFunc);
//			Stream<Trajectory> inputTrajStream = TrajectoryReader.readTrajectoriesToStream(inputTrajFolder, distFunc);
//			matchResultList = mapMatching.parallelMatching(inputTrajStream, numOfThreads, isOnline);
			List<Trajectory> inputTrajList = TrajectoryReader.readTrajectoriesToList(inputTrajFolder, distFunc);
			MapMatchingMethod mapMatching = chooseMatchMethod(matchingMethod, roadMap, property);
			matchResultList = mapMatching.sequentialMatching(inputTrajList, isOnline);
			MatchResultWriter.writeMatchResults(matchResultList, outputMatchResultFolder);
			LOG.info("Matching complete, total time spent:" + (System.currentTimeMillis() - startTaskTime) / 1000 + " seconds.");
//			/* Simple HMM test*/
//			double sigma = property.getPropertyDouble("algorithm.mapmatching.hmm.Sigma");
//			double beta = property.getPropertyDouble("algorithm.mapmatching.hmm.Beta");
//			HMMProbabilities hmmProbabilities = new HMMProbabilities(sigma, beta);
//
//			Matcher simpleHMM = new Matcher(roadMap, property, hmmProbabilities, 50, 1000);
//			for (Trajectory trajectory : inputTrajList) {
//				List<StateCandidate> sequence = simpleHMM.mmatch(trajectory);
//				simpleHMM.pullResult(sequence, Integer.parseInt(trajectory.getID()));
//			}
//			List<Pair<Integer, List<String>>> matchedWaySequence = simpleHMM.getOutputRouteMatchResult();
//			List<Pair<Integer, List<String>>> gtRouteMatchResult = MatchResultReader.readRouteMatchResults(groundTruthRouteMatchResultFolder);
//			RouteMatchingEvaluation.precisionRecallFScoreAccEvaluation(matchedWaySequence, gtRouteMatchResult, roadMap, null);
		}
	}
	
	private static MapMatchingMethod chooseMatchMethod(String matchingMethod, RoadNetworkGraph roadMap, BaseProperty property) {
		switch (matchingMethod.substring(3)) {
			case "HMM":
                return new SimpleHMMMatching(roadMap, property);
			case "FST":
				return new FeatureSTMapMatching(roadMap, property);
			case "WGT":
				return new WeightBasedMM(roadMap, property);
			default:
				throw new IllegalArgumentException("The matching method is not found: " + matchingMethod);
		}
	}
}
