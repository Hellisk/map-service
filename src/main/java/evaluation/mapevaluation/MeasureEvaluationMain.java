package evaluation.mapevaluation;

import evaluation.mapevaluation.pathbaseddistance.benchmarkexperiments.PathBasedMapEvaluation;
import org.apache.log4j.Logger;
import preprocessing.MapGenerator;
import util.function.DistanceFunction;
import util.function.GreatCircleDistanceFunction;
import util.function.SpatialUtils;
import util.io.MapReader;
import util.object.roadnetwork.RoadNetworkGraph;
import util.settings.MapServiceLogger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Generate synthetic maps with various types of errors. Evaluate their performance on different measures.
 *
 * @author Hellisk
 * @since 3/06/2019
 */
public class MeasureEvaluationMain {
	
	public static void main(String[] args) throws IOException {
		String inputMapFolder = "F:/data/Beijing-S/input/map/";
//		String inputMapFolder = "C:/data/Chicago/input/map/";
		double percentage;    // percentage of roads to be changed
		double radius;    // maximum intersection drift distance in meter, used in geographical error
		double noiseBound;    // noise level in meter, used in road shape error
		
		String logFolder = "F:/data/Beijing-S/syntheticTest/log/";
		String cacheFolder = "F:/data/Beijing-S/syntheticTest/cache";
		String logFileName = "mapGenerator" + "_" + System.currentTimeMillis();
		MapServiceLogger.logInit(logFolder, logFileName);
		final Logger LOG = Logger.getLogger(MeasureEvaluationMain.class);
		DistanceFunction distFunc = new GreatCircleDistanceFunction();
//		DistanceFunction distFunc = new EuclideanDistanceFunction();
		System.out.println("Read input road map and generate error maps.");
		
		List<String> outputFrechetResultList = new ArrayList<>();
//		List<String> outputHausdorffResultList = new ArrayList<>();
		RoadNetworkGraph gtMap = MapReader.readMap(inputMapFolder + "0.txt", false, distFunc);
		SpatialUtils.convertMapGCJ2UTM(gtMap);
		RoadNetworkGraph inputMap;

//		outputFrechetResultList.add("start topo with complete random.");
//		outputHausdorffResultList.add("start topo with complete random.");
		LOG.info("start topo with complete random.");
		for (percentage = 10; percentage <= 60; percentage += 10) {
			inputMap = MapReader.readMap(inputMapFolder + "0.txt", false, distFunc);
			RoadNetworkGraph topoErrorMap = MapGenerator.topoErrorGenerator(inputMap, percentage, true);
			SpatialUtils.convertMapGCJ2UTM(topoErrorMap);
//			outputFrechetResultList.add(percentage + ", " + PathBasedMapEvaluation.pathBasedFrechetMapEval(topoErrorMap, gtMap, "LinkThree",
//					cacheFolder + "topo/"));
//			outputHausdorffResultList.add(percentage + ", " + PathBasedMapEvaluation.pathBasedHausdorffMapEval(topoErrorMap, gtMap, "LinkThree", cacheFolder + "topo/"));
		}
//		MapWriter.writeMap(topoErrorMap, inputMapFolder + "topo.txt");

//		outputFrechetResultList.add("start topo with weighted random.");
//		outputHausdorffResultList.add("start topo with weighted random.");
		LOG.info("start topo with weighted random.");
		for (percentage = 10; percentage <= 60; percentage += 10) {
			inputMap = MapReader.readMap(inputMapFolder + "0.txt", false, distFunc);
			RoadNetworkGraph topoErrorMap = MapGenerator.topoErrorGenerator(inputMap, percentage, false);
			SpatialUtils.convertMapGCJ2UTM(topoErrorMap);
//			outputFrechetResultList.add(percentage + ", " + PathBasedMapEvaluation.pathBasedFrechetMapEval(topoErrorMap, gtMap, "LinkThree",
//					cacheFolder + "topo/"));
//			outputHausdorffResultList.add(percentage + ", " + PathBasedMapEvaluation.pathBasedHausdorffMapEval(topoErrorMap, gtMap, "LinkThree", cacheFolder + "topo/"));
		}
//		MapWriter.writeMap(topoErrorMap, inputMapFolder + "topo.txt");

//		outputFrechetResultList.add("start road loss with complete random.");
//		outputHausdorffResultList.add("start road loss with complete random.");
		LOG.info("start road loss with complete random.");
		for (percentage = 10; percentage <= 60; percentage += 10) {
			inputMap = MapReader.readMap(inputMapFolder + "0.txt", false, distFunc);
			RoadNetworkGraph roadLossMap = MapGenerator.roadLossErrorMapGenerator(inputMap, percentage, true);
			SpatialUtils.convertMapGCJ2UTM(roadLossMap);
//			outputFrechetResultList.add(percentage + ", " + PathBasedMapEvaluation.pathBasedFrechetMapEval(roadLossMap, gtMap, "LinkThree",
//					cacheFolder + "roadLoss/"));
//			outputHausdorffResultList.add(percentage + ", " + PathBasedMapEvaluation.pathBasedHausdorffMapEval(roadLossMap, gtMap,
//					"LinkThree", cacheFolder + "roadLoss/"));
		}
//		MapWriter.writeMap(roadLossMap, inputMapFolder + "roadLoss.txt");

//		outputFrechetResultList.add("start road loss with weighted random.");
//		outputHausdorffResultList.add("start road loss with weighted random.");
		LOG.info("start road loss with weighted random.");
		for (percentage = 10; percentage <= 60; percentage += 10) {
			inputMap = MapReader.readMap(inputMapFolder + "0.txt", false, distFunc);
			RoadNetworkGraph roadLossMap = MapGenerator.roadLossErrorMapGenerator(inputMap, percentage, false);
			SpatialUtils.convertMapGCJ2UTM(roadLossMap);
//			outputFrechetResultList.add(percentage + ", " + PathBasedMapEvaluation.pathBasedFrechetMapEval(roadLossMap, gtMap, "LinkThree",
//					cacheFolder + "roadLoss/"));
//			outputHausdorffResultList.add(percentage + ", " + PathBasedMapEvaluation.pathBasedHausdorffMapEval(roadLossMap, gtMap,
//					"LinkThree", cacheFolder + "roadLoss/"));
		}
//		MapWriter.writeMap(roadLossMap, inputMapFolder + "roadLoss.txt");

//		outputFrechetResultList.add("start geo.");
//		outputHausdorffResultList.add("start geo.");
		LOG.info("start geo.");
		for (radius = 5; radius <= 40; radius += 5) {
			inputMap = MapReader.readMap(inputMapFolder + "0.txt", false, distFunc);
			RoadNetworkGraph geoErrorMap = MapGenerator.geoErrorMapGenerator(inputMap, 50, radius);
			SpatialUtils.convertMapGCJ2UTM(geoErrorMap);
//			outputFrechetResultList.add(radius + ", " + PathBasedMapEvaluation.pathBasedFrechetMapEval(geoErrorMap, gtMap, "LinkThree",
//					cacheFolder + "geo/"));
//			outputHausdorffResultList.add(radius + ", " + PathBasedMapEvaluation.pathBasedHausdorffMapEval(geoErrorMap, gtMap, "LinkThree", cacheFolder
//					+ "geo/"));
		}
//		MapWriter.writeMap(geoErrorMap, inputMapFolder + "geo.txt");
		
		outputFrechetResultList.add("start road shape.");
//		outputHausdorffResultList.add("start road shape.");
		LOG.info("start road shape.");
		for (noiseBound = 2; noiseBound <= 12; noiseBound += 2) {
			inputMap = MapReader.readMap(inputMapFolder + "0.txt", false, distFunc);
			RoadNetworkGraph roadShapeErrorMap = MapGenerator.roadShapeErrorMapGenerator(inputMap, 100, noiseBound);
			SpatialUtils.convertMapGCJ2UTM(roadShapeErrorMap);
			outputFrechetResultList.add(noiseBound + ", " + PathBasedMapEvaluation.pathBasedFrechetMapEval(roadShapeErrorMap, gtMap, "LinkThree",
					cacheFolder + "roadShape/"));
//			outputHausdorffResultList.add(noiseBound + ", " + PathBasedMapEvaluation.pathBasedHausdorffMapEval(roadShapeErrorMap, gtMap,
//					"LinkThree", cacheFolder + "roadShape/"));
		}
//		MapWriter.writeMap(roadShapeErrorMap, inputMapFolder + "roadShape.txt");

//		outputFrechetResultList.add("start intersection error.");
//		outputHausdorffResultList.add("start intersection error.");
		LOG.info("start intersection error.");
		for (percentage = 5; percentage <= 30; percentage += 5) {
			inputMap = MapReader.readMap(inputMapFolder + "0.txt", false, distFunc);
			RoadNetworkGraph intersectionErrorMap = MapGenerator.intersectionErrorMapGenerator(inputMap, percentage);
			SpatialUtils.convertMapGCJ2UTM(intersectionErrorMap);
//			outputFrechetResultList.add(percentage + ", " + PathBasedMapEvaluation.pathBasedFrechetMapEval(intersectionErrorMap, gtMap, "LinkThree",
//					cacheFolder + "intersection/"));
//			outputHausdorffResultList.add(percentage + ", " + PathBasedMapEvaluation.pathBasedHausdorffMapEval(intersectionErrorMap, gtMap,
//					"LinkThree", cacheFolder + "intersection/"));
		}
//		MapWriter.writeMap(intersectionErrorMap, inputMapFolder + "intersection.txt");

//		outputFrechetResultList.add("start spurious road.");
//		outputHausdorffResultList.add("start spurious road.");
		LOG.info("start spurious road.");
		for (percentage = 5; percentage <= 30; percentage += 5) {
			inputMap = MapReader.readMap(inputMapFolder + "0.txt", false, distFunc);
			RoadNetworkGraph spuriousRoadErrorMap = MapGenerator.spuriousRoadErrorMapGenerator(inputMap, percentage);
			SpatialUtils.convertMapGCJ2UTM(spuriousRoadErrorMap);
//			outputFrechetResultList.add(percentage + ", " + PathBasedMapEvaluation.pathBasedFrechetMapEval(spuriousRoadErrorMap, gtMap, "LinkThree",
//					cacheFolder + "spuriousRoad/"));
//			outputHausdorffResultList.add(percentage + ", " + PathBasedMapEvaluation.pathBasedHausdorffMapEval(spuriousRoadErrorMap, gtMap,
//					"LinkThree", cacheFolder + "spuriousRoad/"));
		}
//		MapWriter.writeMap(spuriousRoadErrorMap, inputMapFolder + "spuriousRoad.txt");
		for (String s : outputFrechetResultList) {
			LOG.info(s + "\n");
		}
//		for (String s : outputHausdorffResultList) {
//			LOG.info(s +"\n");
//		}
	}
}
