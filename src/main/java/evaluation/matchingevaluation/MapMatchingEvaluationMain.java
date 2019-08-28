package evaluation.matchingevaluation;

import org.apache.log4j.Logger;
import util.function.DistanceFunction;
import util.function.EuclideanDistanceFunction;
import util.function.GreatCircleDistanceFunction;
import util.io.*;
import util.object.roadnetwork.RoadNetworkGraph;
import util.object.spatialobject.Trajectory;
import util.object.structure.Pair;
import util.object.structure.PointMatch;
import util.object.structure.SimpleTrajectoryMatchResult;
import util.settings.MapMatchingProperty;
import util.settings.MapServiceLogger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author uqpchao
 * Created 11/06/2019
 */
public class MapMatchingEvaluationMain {
	
	public static void main(String[] args) throws IOException {
		
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
		String gtRouteMatchResultFolder = property.getPropertyString("path.GroundTruthRouteMatchResultFolder");
		String gtPointMatchResultFolder = property.getPropertyString("path.GroundTruthPointMatchResultFolder");
		String matchingMethod = property.getPropertyString("algorithm.mapmatching.MatchingMethod");
		String dataSpec = property.getPropertyString("data.DataSpec");
		// log file name
		String logFileName = "evaluation_" + dataSet + "_" + matchingMethod + "_" + dataSpec + "_" + initTaskTime;
		DistanceFunction distFunc;
		
		if (dataSet.contains("Beijing"))
			distFunc = new GreatCircleDistanceFunction();
		else
			distFunc = new EuclideanDistanceFunction();
		
		// initialize log file
		MapServiceLogger.logInit(logFolder, logFileName);
		
		// use global dataset to evaluate the map-matching accuracy
		final Logger LOG = Logger.getLogger(MapMatchingEvaluationMain.class);
		
		if (dataSet.equals("Global")) {
			List<RoadNetworkGraph> mapList = new ArrayList<>();
			String rawDataFolder = property.getPropertyString("path.RawDataFolder");
			GlobalTrajectoryLoader trajReader = new GlobalTrajectoryLoader(rawDataFolder);
			GlobalMapLoader mapReader = new GlobalMapLoader(rawDataFolder);
			List<Pair<Integer, List<String>>> outputRouteMatchResult = MatchResultReader.readRouteMatchResults(outputMatchResultFolder);
			List<Pair<Integer, List<String>>> gtRouteMatchResult = new ArrayList<>();
			for (int i = 0; i < trajReader.getNumOfTrajectory(); i++) {
				List<String> matchResult = trajReader.readGTRouteMatchResult(i);
				Pair<Integer, List<String>> currGroundTruthMatchResult = new Pair<>(i, matchResult);
				gtRouteMatchResult.add(currGroundTruthMatchResult);
				mapList.add(mapReader.readRawMap(i));
			}
			long startTaskTime = System.currentTimeMillis();    // the start of the map-matching process
			LOG.info("Precision-recall map-matching evaluation of the " + matchingMethod + " method on " + dataSet + " dataset with input: " + dataSpec);
			
			String precisionRecall = "Precision recall: " + RouteMatchingEvaluation.globalPrecisionRecallEvaluation(outputRouteMatchResult,
					gtRouteMatchResult, mapList);
			LOG.info("Precision-recall map-matching finished, total time cost: " + (System.currentTimeMillis() - startTaskTime));
			LOG.info("Evaluation results for " + matchingMethod + "_" + dataSet + "_" + dataSpec);
			LOG.info(precisionRecall);
		} else {
			// evaluation step, read the output and ground-truth dataset
			RoadNetworkGraph inputMap = MapReader.readMap(inputMapFolder + "0.txt", false, distFunc);
			List<Trajectory> inputTraj = TrajectoryReader.readTrajectoriesToList(inputTrajFolder, distFunc);
			List<SimpleTrajectoryMatchResult> outputMatchResult = MatchResultReader.readSimpleMatchResultsToList(outputMatchResultFolder,
					distFunc);
			List<Pair<Integer, List<String>>> gtRouteMatchResult = MatchResultReader.readRouteMatchResults(gtRouteMatchResultFolder);
			List<Pair<Integer, List<PointMatch>>> gtPointMatchResult = MatchResultReader.readPointMatchResults(gtPointMatchResultFolder,
					distFunc);
			long startTaskTime = System.currentTimeMillis();    // the start of the map-matching process
			
			if (outputMatchResult.size() == 0)
				throw new IllegalArgumentException("The output match result set is empty.");
			List<String> evaluationResultList = new ArrayList<>();
			if (!outputMatchResult.get(0).getRouteMatchResultList().isEmpty()) {
				List<Pair<Integer, List<String>>> routeMatchResult = new ArrayList<>();
				LOG.info("Start route match result evaluation for " + matchingMethod + " method on " + dataSet + " dataset with input: " + dataSpec);
				
				// collect all route match results
				for (SimpleTrajectoryMatchResult matchResult : outputMatchResult) {
					routeMatchResult.add(new Pair<>(Integer.parseInt(matchResult.getTrajID()), matchResult.getRouteMatchResultList()));
				}
				long evaluationTime = System.currentTimeMillis();
				String precisionRecallFScoreAcc =
						"Precision/recall/f-score/acc: " + RouteMatchingEvaluation.precisionRecallFScoreAccEvaluation(routeMatchResult,
								gtRouteMatchResult, inputMap, null);
				LOG.info("Precision/recall/f-score/acc evaluation finish, total time cost: " + (System.currentTimeMillis() - evaluationTime));
				evaluationResultList.add(precisionRecallFScoreAcc);
				evaluationTime = System.currentTimeMillis();
				String rmf = "RMF: " + RouteMatchingEvaluation.rmfEvaluation(routeMatchResult,
						gtRouteMatchResult, inputMap);
				LOG.info("Route Match Fraction (RMF) evaluation finish, total time cost: " + (System.currentTimeMillis() - evaluationTime));
				evaluationResultList.add(rmf);
				evaluationTime = System.currentTimeMillis();
				
				String nonGT = "Measure without GT: " + RouteMatchingEvaluation.nonGTEvaluation(routeMatchResult, inputTraj, inputMap);
				LOG.info("Measure without GT evaluation finish, total time cost: " + (System.currentTimeMillis() - evaluationTime));
				evaluationResultList.add(nonGT);
				
			} else if (!outputMatchResult.get(0).getPointMatchResultList().isEmpty()) {
				List<Pair<Integer, List<PointMatch>>> pointMatchResult = new ArrayList<>();
				LOG.info("Start point match result evaluation for " + matchingMethod + " method on " + dataSet + " dataset with input: " + dataSpec);
				
				// collect all point match results
				for (SimpleTrajectoryMatchResult matchResult : outputMatchResult) {
					pointMatchResult.add(new Pair<>(Integer.parseInt(matchResult.getTrajID()), matchResult.getPointMatchResultList()));
				}
				long evaluationTime = System.currentTimeMillis();
				
				String accuracy =
						"Accuracy: " + PointMatchingEvaluation.accuracyEvaluation(pointMatchResult, gtPointMatchResult);
				LOG.info("Accuracy evaluation finish, total time cost: " + (System.currentTimeMillis() - evaluationTime));
				evaluationResultList.add(accuracy);
				
				evaluationTime = System.currentTimeMillis();
				String rmse = "Root Mean Square Error: " + PointMatchingEvaluation.rootMeanSquareErrorEvaluation(pointMatchResult,
						gtPointMatchResult);
				LOG.info("Root Mean Square Error (RMSE) evaluation finish, total time cost: " + (System.currentTimeMillis() - evaluationTime));
				evaluationResultList.add(rmse);
				
			}
			
			LOG.info("Evaluation finish, total time cost: " + (System.currentTimeMillis() - startTaskTime));
			LOG.info("Evaluation results for " + matchingMethod + "_" + dataSet + "_" + dataSpec);
			for (String s : evaluationResultList) {
				LOG.info(s);
			}
		}
	}
}
