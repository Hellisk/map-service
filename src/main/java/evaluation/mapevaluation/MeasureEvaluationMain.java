package evaluation.mapevaluation;

import preprocessing.MapGenerator;
import util.function.DistanceFunction;
import util.function.GreatCircleDistanceFunction;
import util.io.MapReader;
import util.io.MapWriter;
import util.object.roadnetwork.RoadNetworkGraph;

import java.io.IOException;

/**
 * Generate synthetic maps with various types of errors. Evaluate their performance on different measures.
 *
 * @author Hellisk
 * @since 3/06/2019
 */
public class MeasureEvaluationMain {
	
	public static void main(String[] args) throws IOException {
		String inputMapFolder = "C:/data/Beijing-S/input/map/";
		double percentage = 20;    // percentage of roads to be changed
		double radius = 25;    // maximum intersection drift distance in meter, used in geographical error
		double noiseBound = 3;    // noise lever in meter, used in road shape error
		boolean isCompleteRandom = true;
		
		DistanceFunction distFunc = new GreatCircleDistanceFunction();
		System.out.println("Read input road map and generate error maps.");
		
		RoadNetworkGraph inputMap = MapReader.readMap(inputMapFolder + "0.txt", false, distFunc);
		RoadNetworkGraph topoErrorMap = MapGenerator.topoErrorGenerator(inputMap, percentage, isCompleteRandom);
		MapWriter.writeMap(topoErrorMap, inputMapFolder + "topo.txt");
		inputMap = MapReader.readMap(inputMapFolder + "0.txt", false, distFunc);
		RoadNetworkGraph geoErrorMap = MapGenerator.geoErrorMapGenerator(inputMap, percentage, radius);
		MapWriter.writeMap(geoErrorMap, inputMapFolder + "geo.txt");
		inputMap = MapReader.readMap(inputMapFolder + "0.txt", false, distFunc);
		RoadNetworkGraph roadLossMap = MapGenerator.roadLossErrorMapGenerator(inputMap, percentage, isCompleteRandom);
		MapWriter.writeMap(roadLossMap, inputMapFolder + "roadLoss.txt");
		inputMap = MapReader.readMap(inputMapFolder + "0.txt", false, distFunc);
		RoadNetworkGraph spuriousRoadErrorMap = MapGenerator.spuriousRoadErrorMapGenerator(inputMap, percentage);
		MapWriter.writeMap(spuriousRoadErrorMap, inputMapFolder + "spuriousRoad.txt");
		inputMap = MapReader.readMap(inputMapFolder + "0.txt", false, distFunc);
		RoadNetworkGraph roadShapeErrorMap = MapGenerator.roadShapeErrorMapGenerator(inputMap, percentage, noiseBound, isCompleteRandom);
		MapWriter.writeMap(roadShapeErrorMap, inputMapFolder + "roadShape.txt");
		inputMap = MapReader.readMap(inputMapFolder + "0.txt", false, distFunc);
		RoadNetworkGraph intersectionErrorMap = MapGenerator.intersectionErrorMapGenerator(inputMap, percentage);
		MapWriter.writeMap(intersectionErrorMap, inputMapFolder + "intersection.txt");
	}
}
