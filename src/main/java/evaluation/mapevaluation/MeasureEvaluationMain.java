package evaluation.mapevaluation;

import evaluation.mapevaluation.graphmatching.GraphMatchingMapEvaluation;
import evaluation.mapevaluation.graphsampling.GraphSamplingMapEvaluation;
import evaluation.mapevaluation.pathbaseddistance.benchmarkexperiments.PathBasedMapEvaluation;
import org.apache.log4j.Logger;
import preprocessing.MapGenerator;
import util.function.DistanceFunction;
import util.function.GreatCircleDistanceFunction;
import util.function.SpatialUtils;
import util.io.MapReader;
import util.object.roadnetwork.RoadNetworkGraph;
import util.settings.MapServiceLogger;

import java.util.ArrayList;
import java.util.List;

/**
 * Generate synthetic maps with various types of errors. Evaluate their performance on different measures.
 *
 * @author Hellisk
 * @since 3/06/2019
 */
public class MeasureEvaluationMain {
	
	public static void main(String[] args) {
		String inputMapFolder = "C:/data/Beijing-S/input/map/";
//		String inputMapFolder = "C:/data/Chicago/input/map/";
		double percentage;    // percentage of roads to be changed
		double radius;    // maximum intersection drift distance in meter, used in geographical error
		double noiseBound;    // noise level in meter, used in road shape error
		
		String logFolder = "C:/data/Beijing-S/syntheticTest/log/";
		String cacheFolder = "C:/data/Beijing-S/syntheticTest/cache";
		String logFileName = "mapGenerator" + "_" + System.currentTimeMillis();
		MapServiceLogger.logInit(logFolder, logFileName);
		final Logger LOG = Logger.getLogger(MeasureEvaluationMain.class);
		DistanceFunction distFunc = new GreatCircleDistanceFunction();
//		DistanceFunction distFunc = new EuclideanDistanceFunction();
		System.out.println("Read input road map and generate error maps.");
		
		List<String> outputGraphMatchingResultList = new ArrayList<>();
		List<String> outputGraphSamplingResultList = new ArrayList<>();
		List<String> outputFrechetResultList = new ArrayList<>();
//		List<String> outputHausdorffResultList = new ArrayList<>();
		
		double gmMaxDist = 20;
		double gsHopDist = 50;
		double gsRadius = 2000;
		double gsMatchDist = 20;
		int gsNumOfRoots = 200;
		
		RoadNetworkGraph gtMap = MapReader.readMap(inputMapFolder + "0.txt", false, distFunc);
		SpatialUtils.convertMapGCJ2UTM(gtMap);
		RoadNetworkGraph inputMap;
		
		outputGraphMatchingResultList.add("Start topo with complete random.");
		outputGraphSamplingResultList.add("Start topo with complete random.");
		outputFrechetResultList.add("Start topo with complete random.");
//		outputHausdorffResultList.add("Start topo with complete random.");
		LOG.info("Start topo with complete random.");
		for (percentage = 10; percentage <= 60; percentage += 10) {
			inputMap = MapReader.readMap(inputMapFolder + "0.txt", false, distFunc);
			RoadNetworkGraph topoErrorMap = MapGenerator.topoErrorGenerator(inputMap, percentage, true);
			SpatialUtils.convertMapGCJ2UTM(topoErrorMap);
			outputGraphMatchingResultList.add(percentage + ", " + GraphMatchingMapEvaluation.precisionRecallGraphMatchingMapEval(topoErrorMap,
					gtMap, gmMaxDist));
			outputGraphSamplingResultList.add(percentage + ", " + GraphSamplingMapEvaluation.precisionRecallGraphSamplingMapEval(topoErrorMap,
					gtMap, gsHopDist, gsRadius, gsMatchDist, gsNumOfRoots));
			outputFrechetResultList.add(percentage + ", " + PathBasedMapEvaluation.pathBasedFrechetMapEval(topoErrorMap, gtMap, "LinkThree",
					cacheFolder + "topo/"));
//			outputHausdorffResultList.add(percentage + ", " + PathBasedMapEvaluation.pathBasedHausdorffMapEval(topoErrorMap, gtMap, "LinkThree", cacheFolder + "topo/"));
		}
//		MapWriter.writeMap(topoErrorMap, inputMapFolder + "topo.txt");
		
		outputGraphMatchingResultList.add("Start topo with weighted random.");
		outputGraphSamplingResultList.add("Start topo with weighted random.");
		outputFrechetResultList.add("Start topo with weighted random.");
//		outputHausdorffResultList.add("Start topo with weighted random.");
		LOG.info("Start topo with weighted random.");
		for (percentage = 10; percentage <= 60; percentage += 10) {
			inputMap = MapReader.readMap(inputMapFolder + "0.txt", false, distFunc);
			RoadNetworkGraph topoErrorMap = MapGenerator.topoErrorGenerator(inputMap, percentage, false);
			SpatialUtils.convertMapGCJ2UTM(topoErrorMap);
			outputGraphMatchingResultList.add(percentage + ", " + GraphMatchingMapEvaluation.precisionRecallGraphMatchingMapEval(topoErrorMap,
					gtMap, gmMaxDist));
			outputGraphSamplingResultList.add(percentage + ", " + GraphSamplingMapEvaluation.precisionRecallGraphSamplingMapEval(topoErrorMap,
					gtMap, gsHopDist, gsRadius, gsMatchDist, gsNumOfRoots));
			outputFrechetResultList.add(percentage + ", " + PathBasedMapEvaluation.pathBasedFrechetMapEval(topoErrorMap, gtMap, "LinkThree",
					cacheFolder + "topo/"));
//			outputHausdorffResultList.add(percentage + ", " + PathBasedMapEvaluation.pathBasedHausdorffMapEval(topoErrorMap, gtMap, "LinkThree", cacheFolder + "topo/"));
		}
//		MapWriter.writeMap(topoErrorMap, inputMapFolder + "topo.txt");
		
		outputGraphMatchingResultList.add("Start road loss with complete random.");
		outputGraphSamplingResultList.add("Start road loss with complete random.");
		outputFrechetResultList.add("Start road loss with complete random.");
//		outputHausdorffResultList.add("Start road loss with complete random.");
		LOG.info("Start road loss with complete random.");
		for (percentage = 10; percentage <= 60; percentage += 10) {
			inputMap = MapReader.readMap(inputMapFolder + "0.txt", false, distFunc);
			RoadNetworkGraph roadLossMap = MapGenerator.roadLossErrorMapGenerator(inputMap, percentage, true);
			SpatialUtils.convertMapGCJ2UTM(roadLossMap);
			outputGraphMatchingResultList.add(percentage + ", " + GraphMatchingMapEvaluation.precisionRecallGraphMatchingMapEval(roadLossMap,
					gtMap, gmMaxDist));
			outputGraphSamplingResultList.add(percentage + ", " + GraphSamplingMapEvaluation.precisionRecallGraphSamplingMapEval(roadLossMap,
					gtMap, gsHopDist, gsRadius, gsMatchDist, gsNumOfRoots));
			outputFrechetResultList.add(percentage + ", " + PathBasedMapEvaluation.pathBasedFrechetMapEval(roadLossMap, gtMap, "LinkThree",
					cacheFolder + "roadLoss/"));
//			outputHausdorffResultList.add(percentage + ", " + PathBasedMapEvaluation.pathBasedHausdorffMapEval(roadLossMap, gtMap,
//					"LinkThree", cacheFolder + "roadLoss/"));
		}
//		MapWriter.writeMap(roadLossMap, inputMapFolder + "roadLoss.txt");
		
		outputGraphMatchingResultList.add("Start road loss with weighted random.");
		outputGraphSamplingResultList.add("Start road loss with weighted random.");
		outputFrechetResultList.add("Start road loss with weighted random.");
//		outputHausdorffResultList.add("Start road loss with weighted random.");
		LOG.info("Start road loss with weighted random.");
		for (percentage = 10; percentage <= 60; percentage += 10) {
			inputMap = MapReader.readMap(inputMapFolder + "0.txt", false, distFunc);
			RoadNetworkGraph roadLossMap = MapGenerator.roadLossErrorMapGenerator(inputMap, percentage, false);
			SpatialUtils.convertMapGCJ2UTM(roadLossMap);
			outputGraphMatchingResultList.add(percentage + ", " + GraphMatchingMapEvaluation.precisionRecallGraphMatchingMapEval(roadLossMap,
					gtMap, gmMaxDist));
			outputGraphSamplingResultList.add(percentage + ", " + GraphSamplingMapEvaluation.precisionRecallGraphSamplingMapEval(roadLossMap,
					gtMap, gsHopDist, gsRadius, gsMatchDist, gsNumOfRoots));
			outputFrechetResultList.add(percentage + ", " + PathBasedMapEvaluation.pathBasedFrechetMapEval(roadLossMap, gtMap, "LinkThree",
					cacheFolder + "roadLoss/"));
//			outputHausdorffResultList.add(percentage + ", " + PathBasedMapEvaluation.pathBasedHausdorffMapEval(roadLossMap, gtMap,
//					"LinkThree", cacheFolder + "roadLoss/"));
		}
//		MapWriter.writeMap(roadLossMap, inputMapFolder + "roadLoss.txt");
		
		outputGraphMatchingResultList.add("Start geo.");
		outputGraphSamplingResultList.add("Start geo.");
		outputFrechetResultList.add("Start geo.");
//		outputHausdorffResultList.add("Start geo.");
		LOG.info("Start geo.");
		for (radius = 5; radius <= 40; radius += 5) {
			inputMap = MapReader.readMap(inputMapFolder + "0.txt", false, distFunc);
			RoadNetworkGraph geoErrorMap = MapGenerator.geoErrorMapGenerator(inputMap, 50, radius);
			SpatialUtils.convertMapGCJ2UTM(geoErrorMap);
			outputGraphMatchingResultList.add(radius + ", " + GraphMatchingMapEvaluation.precisionRecallGraphMatchingMapEval(geoErrorMap,
					gtMap, gmMaxDist));
			outputGraphSamplingResultList.add(radius + ", " + GraphSamplingMapEvaluation.precisionRecallGraphSamplingMapEval(geoErrorMap,
					gtMap, gsHopDist, gsRadius, gsMatchDist, gsNumOfRoots));
			outputFrechetResultList.add(radius + ", " + PathBasedMapEvaluation.pathBasedFrechetMapEval(geoErrorMap, gtMap, "LinkThree",
					cacheFolder + "geo/"));
//			outputHausdorffResultList.add(radius + ", " + PathBasedMapEvaluation.pathBasedHausdorffMapEval(geoErrorMap, gtMap, "LinkThree", cacheFolder
//					+ "geo/"));
		}
//		MapWriter.writeMap(geoErrorMap, inputMapFolder + "geo.txt");
		
		outputGraphMatchingResultList.add("Start road shape.");
		outputGraphSamplingResultList.add("Start road shape.");
		outputFrechetResultList.add("Start road shape.");
//		outputHausdorffResultList.add("Start road shape.");
		LOG.info("Start road shape.");
		for (noiseBound = 2; noiseBound <= 12; noiseBound += 2) {
			inputMap = MapReader.readMap(inputMapFolder + "0.txt", false, distFunc);
			RoadNetworkGraph roadShapeErrorMap = MapGenerator.roadShapeErrorMapGenerator(inputMap, 100, noiseBound);
			SpatialUtils.convertMapGCJ2UTM(roadShapeErrorMap);
			outputGraphMatchingResultList.add(noiseBound + ", " + GraphMatchingMapEvaluation.precisionRecallGraphMatchingMapEval(roadShapeErrorMap,
					gtMap, gmMaxDist));
			outputGraphSamplingResultList.add(noiseBound + ", " + GraphSamplingMapEvaluation.precisionRecallGraphSamplingMapEval(roadShapeErrorMap,
					gtMap, gsHopDist, gsRadius, gsMatchDist, gsNumOfRoots));
			outputFrechetResultList.add(noiseBound + ", " + PathBasedMapEvaluation.pathBasedFrechetMapEval(roadShapeErrorMap, gtMap, "LinkThree",
					cacheFolder + "roadShape/"));
//			outputHausdorffResultList.add(noiseBound + ", " + PathBasedMapEvaluation.pathBasedHausdorffMapEval(roadShapeErrorMap, gtMap,
//					"LinkThree", cacheFolder + "roadShape/"));
		}
//		MapWriter.writeMap(roadShapeErrorMap, inputMapFolder + "roadShape.txt");
		
		outputGraphMatchingResultList.add("Start intersection error.");
		outputGraphSamplingResultList.add("Start intersection error.");
		outputFrechetResultList.add("start intersection error.");
//		outputHausdorffResultList.add("start intersection error.");
		LOG.info("Start intersection error.");
		for (percentage = 5; percentage <= 30; percentage += 5) {
			inputMap = MapReader.readMap(inputMapFolder + "0.txt", false, distFunc);
			RoadNetworkGraph intersectionErrorMap = MapGenerator.intersectionErrorMapGenerator(inputMap, percentage);
			SpatialUtils.convertMapGCJ2UTM(intersectionErrorMap);
			outputGraphMatchingResultList.add(percentage + ", " + GraphMatchingMapEvaluation.precisionRecallGraphMatchingMapEval(intersectionErrorMap,
					gtMap, gmMaxDist));
			outputGraphSamplingResultList.add(percentage + ", " + GraphSamplingMapEvaluation.precisionRecallGraphSamplingMapEval(intersectionErrorMap,
					gtMap, gsHopDist, gsRadius, gsMatchDist, gsNumOfRoots));
			outputFrechetResultList.add(percentage + ", " + PathBasedMapEvaluation.pathBasedFrechetMapEval(intersectionErrorMap, gtMap, "LinkThree",
					cacheFolder + "intersection/"));
//			outputHausdorffResultList.add(percentage + ", " + PathBasedMapEvaluation.pathBasedHausdorffMapEval(intersectionErrorMap, gtMap,
//					"LinkThree", cacheFolder + "intersection/"));
		}
//		MapWriter.writeMap(intersectionErrorMap, inputMapFolder + "intersection.txt");
		
		outputGraphMatchingResultList.add("Start spurious road.");
		outputGraphSamplingResultList.add("Start spurious road.");
		outputFrechetResultList.add("Start spurious road.");
//		outputHausdorffResultList.add("Start spurious road.");
		LOG.info("Start spurious road.");
		for (percentage = 5; percentage <= 30; percentage += 5) {
			inputMap = MapReader.readMap(inputMapFolder + "0.txt", false, distFunc);
			RoadNetworkGraph spuriousRoadErrorMap = MapGenerator.spuriousRoadErrorMapGenerator(inputMap, percentage);
			SpatialUtils.convertMapGCJ2UTM(spuriousRoadErrorMap);
			outputGraphMatchingResultList.add(percentage + ", " + GraphMatchingMapEvaluation.precisionRecallGraphMatchingMapEval(spuriousRoadErrorMap,
					gtMap, gmMaxDist));
			outputGraphSamplingResultList.add(percentage + ", " + GraphSamplingMapEvaluation.precisionRecallGraphSamplingMapEval(spuriousRoadErrorMap,
					gtMap, gsHopDist, gsRadius, gsMatchDist, gsNumOfRoots));
			outputFrechetResultList.add(percentage + ", " + PathBasedMapEvaluation.pathBasedFrechetMapEval(spuriousRoadErrorMap, gtMap, "LinkThree",
					cacheFolder + "spuriousRoad/"));
//			outputHausdorffResultList.add(percentage + ", " + PathBasedMapEvaluation.pathBasedHausdorffMapEval(spuriousRoadErrorMap, gtMap,
//					"LinkThree", cacheFolder + "spuriousRoad/"));
		}
//		MapWriter.writeMap(spuriousRoadErrorMap, inputMapFolder + "spuriousRoad.txt");
		
		LOG.info("Graph item matching results:");
		for (String s : outputGraphMatchingResultList) {
			LOG.info(s + "\n");
		}
		LOG.info("Graph sampling results:");
		for (String s : outputGraphSamplingResultList) {
			LOG.info(s + "\n");
		}
		for (String s : outputFrechetResultList) {
			LOG.info(s + "\n");
		}
//		for (String s : outputHausdorffResultList) {
//			LOG.info(s +"\n");
//		}
	}
}
