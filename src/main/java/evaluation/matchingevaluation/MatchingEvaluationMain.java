//package evaluation.matchingevaluation;
//
//import evaluation.mapevaluation.graphmatching.GraphMatchingMapEvaluation;
//import evaluation.mapevaluation.graphsampling.GraphSamplingMapEvaluation;
//import org.apache.log4j.Logger;
//import util.function.DistanceFunction;
//import util.function.EuclideanDistanceFunction;
//import util.function.GreatCircleDistanceFunction;
//import util.function.SpatialUtils;
//import util.io.MapReader;
//import util.object.roadnetwork.RoadNetworkGraph;
//import util.object.structure.Route;
//import util.settings.MapInferenceProperty;
//import util.settings.MapServiceLogger;
//
//import java.util.List;
//
///**
// * @author uqpchao
// * Created 11/06/2019
// */
//public class MatchingEvaluationMain {
//
//	public static void main(String[] args) {
//
//		// initialize arguments
//		MapInferenceProperty property = new MapInferenceProperty();
//		property.loadPropertiesFromResourceFile("mapmatching.properties", args);
//		long initTaskTime = System.currentTimeMillis();
//
//		// setup java log
//		String logFolder = property.getPropertyString("algorithm.mapmatching.log.LogFolder");  // obtain the log folder from args
//		String cacheFolder = property.getPropertyString("algorithm.mapmatching.path.CacheFolder");    // used to store temporary files
//		String dataSet = property.getPropertyString("data.Dataset");
//		String inputTrajFolder = property.getPropertyString("path.InputTrajectoryFolder");
//		String inputMapFolder = property.getPropertyString("path.InputMapFolder");
//		String outputMatchResultFolder = property.getPropertyString("path.OutputMatchResultFolder");
//		String gtRouteMatchResultFolder = property.getPropertyString("path.GroundTruthRouteMatchResultFolder");
//		String gtPointMatchResultFolder = property.getPropertyString("path.GroundTruthPointMatchResultFolder");
//		String matchingMethod = property.getPropertyString("algorithm.mapmatching.MatchingMethod");
//		String dataSpec = property.getPropertyString("data.DataSpec");
//		// log file name
//		String logFileName = "evaluation_" + dataSet + "_" + matchingMethod + "_" + dataSpec + "_" + initTaskTime;
//		DistanceFunction distFunc;
//
//		if (dataSet.contains("Beijing"))
//			distFunc = new GreatCircleDistanceFunction();
//		else
//			distFunc = new EuclideanDistanceFunction();
//
//		// initialize log file
//		MapServiceLogger.logInit(logFolder, logFileName);
//
//		// use global dataset to evaluate the map-matching accuracy
//		final Logger LOG = Logger.getLogger(MatchingEvaluationMain.class);
//
//		// evaluation step, read the output and ground-truth dataset
//		RoadNetworkGraph inputMap = MapReader.readMap(inputMapFolder + "0.txt", false, distFunc);
//		long startTaskTime = System.currentTimeMillis();    // the start of the map-matching process
//
//		LOG.info("Map-matching evaluation of the " + matchingMethod + " method on " + dataSet + " dataset with input: " + dataSpec);
//
//		String graphMatchingResult = "Precision recall: " + precisionRecallMatchingEvaluation.precisionRecallMapMatchingEval(results, gtRouteMatchResult, roadMap, null);;
//		LOG.info("Graph item matching finish, total time cost: " + (System.currentTimeMillis() - startTaskTime));
//		startTaskTime = System.currentTimeMillis();
//
//		String graphSamplingResult = "GS: " + GraphSamplingMapEvaluation.precisionRecallGraphSamplingMapEval(outputMap, inputMap, 1, 100,
//				50, 1000);    // radius,seeds follows stanojevic2018
//		LOG.info("Graph sampling finish, total time cost: " + (System.currentTimeMillis() - startTaskTime));
//		startTaskTime = System.currentTimeMillis();
//
////		String pathBasedFrechetResult = "FrechetPBD: " + PathBasedMapEvaluation.pathBasedFrechetMapEval(outputMap, inputMap, "LinkThree",
////				cacheFolder);
////		String pathBasedHausdorffResult = "HausdorffPBD: " + PathBasedMapEvaluation.pathBasedHausdorffMapEval(outputMap, inputMap, "LinkThree",
////				cacheFolder);
//
//		LOG.info("Evaluation results for " + inferenceMethod + "_" + dataSet + "_" + dataSpec);
//		LOG.info(graphMatchingResult);
//		LOG.info(graphSamplingResult);
////		LOG.info(pathBasedFrechetResult);
////		LOG.info(pathBasedHausdorffResult);
//	}
//}
