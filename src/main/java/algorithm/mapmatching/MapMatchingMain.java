package algorithm.mapmatching;

import algorithm.mapmatching.hmm.HMMMapMatching;
import evaluation.matchingevaluation.precisionRecallMatchingEvaluation;
import org.apache.log4j.Logger;
import util.function.DistanceFunction;
import util.function.GreatCircleDistanceFunction;
import util.io.*;
import util.object.roadnetwork.RoadNetworkGraph;
import util.object.spatialobject.Trajectory;
import util.object.spatialobject.TrajectoryPoint;
import util.object.structure.MatchResultWithUnmatchedTraj;
import util.object.structure.MultipleTrajectoryMatchResult;
import util.object.structure.Pair;
import util.settings.MapMatchingProperty;
import util.settings.MapServiceLogger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
		
		// setup java log
		String logFolder = property.getPropertyString("algorithm.mapmatching.log.LogFolder");  // obtain the log folder from args
		String dataSet = property.getPropertyString("data.Dataset");
		String inputTrajFolder = property.getPropertyString("path.InputTrajectoryFolder");
		String inputMapFolder = property.getPropertyString("path.InputMapFolder");
		String outputMatchResultFolder = property.getPropertyString("path.OutputMatchResultFolder");
		String groundTruthRouteMatchResultFolder = property.getPropertyString("path.GroundTruthRouteMatchResultFolder");
		String groundTruthPointMatchResultFolder = property.getPropertyString("path.GroundTruthPointMatchResultFolder");
		String matchingMethod = property.getPropertyString("algorithm.mapmatching.MatchingMethod");
		String dataSpec = property.getPropertyString("data.DataSpec");
		DistanceFunction distFunc;
		String logFileName = "";
		// log file name
		switch (matchingMethod) {
			case "HMM":
				logFileName = "matching_" + dataSet + "_" + matchingMethod + "_" + dataSpec + "_"
						+ property.getPropertyString("algorithm.mapmatching.hmm.CandidateRange") + "_"
						+ property.getPropertyString("algorithm.mapmatching.hmm.Sigma") + "_"
						+ property.getPropertyString("algorithm.mapmatching.hmm.Beta") + "_" + initTaskTime;
				break;
			default:
				logFileName = "matching_" + dataSet + "_" + matchingMethod + "_" + dataSpec + "_" + initTaskTime;
				break;
		}
		// initialize log file
		MapServiceLogger.logInit(logFolder, logFileName);
		
		final Logger LOG = Logger.getLogger(MapMatchingMain.class);
		// use global dataset to evaluate the map-matching accuracy
		LOG.info("Map-matching on the " + dataSet + " dataset with argument:" + property.toString());
		
		long startTaskTime = System.currentTimeMillis();    // the start of the map-matching process
		// map-matching process
		List<MultipleTrajectoryMatchResult> results = new ArrayList<>();
		List<Pair<Integer, List<String>>> gtRouteMatchResult = new ArrayList<>();
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
				HMMMapMatching mapMatching = new HMMMapMatching(currMap, property);
				MultipleTrajectoryMatchResult matchResult = mapMatching.trajectorySingleMatchingProcess(currTraj);
				results.add(matchResult);
			}
			LOG.info("Map matching finished, total time spent:" + (System.currentTimeMillis() - startTaskTime) / 1000 + "seconds");
			for (int i = 0; i < reader.getNumOfTrajectory(); i++) {
				List<String> matchResult = reader.readGTRouteMatchResult(i);
				Pair<Integer, List<String>> currGroundTruthMatchResult = new Pair<>(i, matchResult);
				gtRouteMatchResult.add(currGroundTruthMatchResult);
			}
			MatchResultWriter.writeMatchResults(results, outputMatchResultFolder);
			
			// evaluation: map matching evaluation
			results = MatchResultReader.readMatchResultsToList(outputMatchResultFolder, new GreatCircleDistanceFunction());    // used for
			// evaluation only
			precisionRecallMatchingEvaluation.globalPrecisionRecallMapMatchingEval(results, gtRouteMatchResult, rawDataFolder);
			System.out.println("Total number of trajectory points is " + trajPointCount);
		} else {
			distFunc = new GreatCircleDistanceFunction();
			RoadNetworkGraph roadMap = MapReader.readMap(inputMapFolder + "0.txt", false, distFunc);
			Stream<Trajectory> inputTrajStream = TrajectoryReader.readTrajectoriesToStream(inputTrajFolder, distFunc);
			HMMMapMatching mapMatching = new HMMMapMatching(roadMap, property);
			Stream<MatchResultWithUnmatchedTraj> currCombinedMatchResultStream = mapMatching.trajectoryStreamMatchingProcess(inputTrajStream);
			List<MatchResultWithUnmatchedTraj> currCombinedMatchResultList = currCombinedMatchResultStream.collect(Collectors.toList());
			List<Pair<Integer, List<String>>> routeMatchResult = new ArrayList<>();
			for (MatchResultWithUnmatchedTraj currPair : currCombinedMatchResultList) {
				results.add(currPair.getMatchResult());
				routeMatchResult.add(new Pair<>(Integer.parseInt(currPair.getTrajID()),
						currPair.getMatchResult().getCompleteMatchRouteAtRank(0).getRoadIDList()));
			}
			MatchResultWriter.writeRouteMatchResults(routeMatchResult, outputMatchResultFolder);
			LOG.info("Matching complete.");
			
			gtRouteMatchResult = MatchResultReader.readRouteMatchResults(groundTruthRouteMatchResultFolder);
			precisionRecallMatchingEvaluation.precisionRecallMapMatchingEval(results, gtRouteMatchResult, roadMap, null);
		}
	}
}
