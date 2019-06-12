package evaluation.mapevaluation;

import evaluation.mapevaluation.graphmatching.GraphMatchingMapEvaluation;
import evaluation.mapevaluation.graphsampling.GraphSamplingMapEvaluation;
import org.apache.log4j.Logger;
import util.function.DistanceFunction;
import util.function.EuclideanDistanceFunction;
import util.function.GreatCircleDistanceFunction;
import util.function.SpatialUtils;
import util.io.MapReader;
import util.object.roadnetwork.RoadNetworkGraph;
import util.settings.MapInferenceProperty;
import util.settings.MapServiceLogger;

/**
 * @author uqpchao
 * Created 11/06/2019
 */
public class MapEvaluationMain {
	
	public static void main(String[] args) {
		
		// initialize arguments
		MapInferenceProperty property = new MapInferenceProperty();
		property.loadPropertiesFromResourceFile("mapinference.properties", args);
		long initTaskTime = System.currentTimeMillis();
		
		// setup java log
		String logFolder = property.getPropertyString("algorithm.mapinference.log.LogFolder");  // obtain the log folder from args
		String cacheFolder = property.getPropertyString("algorithm.mapinference.path.CacheFolder");    // used to store temporary files
		String dataSet = property.getPropertyString("data.Dataset");
		String inputTrajFolder = property.getPropertyString("path.InputTrajectoryFolder");
		String gtMapFolder = property.getPropertyString("path.GroundTruthMapFolder");
		String inferenceMethod = property.getPropertyString("algorithm.mapinference.InferenceMethod");
		String outputMapFolder = property.getPropertyString("path.OutputMapFolder");
		String dataSpec = property.getPropertyString("data.DataSpec");
		// log file name
		String logFileName = "evaluation_" + dataSet + "_" + inferenceMethod + "_" + dataSpec + "_" + initTaskTime;
		DistanceFunction distFunc;
		
		if (dataSet.contains("Beijing"))
			distFunc = new GreatCircleDistanceFunction();
		else
			distFunc = new EuclideanDistanceFunction();
		
		// initialize log file
		MapServiceLogger.logInit(logFolder, logFileName);
		
		// use global dataset to evaluate the map-matching accuracy
		final Logger LOG = Logger.getLogger(MapEvaluationMain.class);
		
		// evaluation step, read the output and ground-truth datasets
		RoadNetworkGraph gtMap = MapReader.readMap(gtMapFolder + "0.txt", false, distFunc);
		if (dataSet.contains("Beijing")) {
			LOG.info("Convert the ground-truth map into UTM before path-based evaluation");
			SpatialUtils.convertMapGCJ2UTM(gtMap);
		}
		RoadNetworkGraph outputMap = MapReader.readMap(outputMapFolder + inferenceMethod + "_" + dataSpec + ".txt", false, new
				EuclideanDistanceFunction());
		
		long startTaskTime = System.currentTimeMillis();    // the start of the map-matching process
		LOG.info("Map evaluation of the " + inferenceMethod + " method on " + dataSet + " dataset with input: " + dataSpec);
		
		String graphMatchingResult = "GM: " + GraphMatchingMapEvaluation.precisionRecallGraphMatchingMapEval(outputMap, gtMap, 50);
		LOG.info("Graph item matching finish, total time cost: " + (System.currentTimeMillis() - startTaskTime));
		startTaskTime = System.currentTimeMillis();
		
		String graphSamplingResult = "GS: " + GraphSamplingMapEvaluation.precisionRecallGraphSamplingMapEval(outputMap, gtMap, 50, 2000, 20,
				200);    // radius,seeds follows stanojevic2018
		LOG.info("Graph sampling finish, total time cost: " + (System.currentTimeMillis() - startTaskTime));
		startTaskTime = System.currentTimeMillis();

//		String pathBasedFrechetResult = "FrechetPBD: " + PathBasedMapEvaluation.pathBasedFrechetMapEval(outputMap, gtMap, "LinkThree",
//				cacheFolder);
//		String pathBasedHausdorffResult = "HausdorffPBD: " + PathBasedMapEvaluation.pathBasedHausdorffMapEval(outputMap, gtMap, "LinkThree",
//				cacheFolder);
		
		LOG.info("Evaluation results for " + inferenceMethod + "_" + dataSet + "_" + dataSpec);
		LOG.info(graphMatchingResult);
		LOG.info(graphSamplingResult);
//		LOG.info(pathBasedFrechetResult);
//		LOG.info(pathBasedHausdorffResult);
	}
}
