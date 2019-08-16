package evaluation.matchingevaluation;

import org.apache.log4j.Logger;
import util.function.DistanceFunction;
import util.function.EuclideanDistanceFunction;
import util.function.GreatCircleDistanceFunction;
import util.io.GlobalMapLoader;
import util.io.GlobalTrajectoryLoader;
import util.io.MapReader;
import util.io.MatchResultReader;
import util.object.roadnetwork.RoadNetworkGraph;
import util.object.structure.Pair;
import util.settings.MapInferenceProperty;
import util.settings.MapServiceLogger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author uqpchao
 * Created 11/06/2019
 */
public class MatchingEvaluationMain {
	
	public static void main(String[] args) throws IOException {
		
		// initialize arguments
		MapInferenceProperty property = new MapInferenceProperty();
		property.loadPropertiesFromResourceFile("mapmatching.properties", args);
		long initTaskTime = System.currentTimeMillis();
		
		// setup java log
		String logFolder = property.getPropertyString("algorithm.mapmatching.log.LogFolder");  // obtain the log folder from args
		String cacheFolder = property.getPropertyString("algorithm.mapmatching.path.CacheFolder");    // used to store temporary files
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
		final Logger LOG = Logger.getLogger(MatchingEvaluationMain.class);
		
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
			LOG.info("Graph item matching finish, total time cost: " + (System.currentTimeMillis() - startTaskTime));
			LOG.info("Evaluation results for " + matchingMethod + "_" + dataSet + "_" + dataSpec);
			LOG.info(precisionRecall);
		} else {
			// evaluation step, read the output and ground-truth dataset
			RoadNetworkGraph inputMap = MapReader.readMap(inputMapFolder + "0.txt", false, distFunc);
			List<Pair<Integer, List<String>>> routeMatchResult = MatchResultReader.readRouteMatchResults(outputMatchResultFolder);
			List<Pair<Integer, List<String>>> gtRouteMatchResult = MatchResultReader.readRouteMatchResults(gtRouteMatchResultFolder);
			long startTaskTime = System.currentTimeMillis();    // the start of the map-matching process
			
			LOG.info("Precision-recall map-matching evaluation of the " + matchingMethod + " method on " + dataSet + " dataset with input: " + dataSpec);
			
			String precisionRecall = "Precision recall: " + RouteMatchingEvaluation.precisionRecallEvaluation(routeMatchResult,
					gtRouteMatchResult, inputMap, null);
			LOG.info("Graph item matching finish, total time cost: " + (System.currentTimeMillis() - startTaskTime));
			LOG.info("Evaluation results for " + matchingMethod + "_" + dataSet + "_" + dataSpec);
			LOG.info(precisionRecall);
		}
	}
}
