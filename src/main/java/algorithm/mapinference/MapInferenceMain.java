package algorithm.mapinference;

import algorithm.mapinference.lineclustering.LineClusteringMapInference;
import algorithm.mapinference.tracemerge.TraceMergeMapInference;
import org.apache.log4j.Logger;
import util.function.DistanceFunction;
import util.function.EuclideanDistanceFunction;
import util.function.GreatCircleDistanceFunction;
import util.io.MapWriter;
import util.io.TrajectoryReader;
import util.object.roadnetwork.RoadNetworkGraph;
import util.object.spatialobject.Trajectory;
import util.settings.MapInferenceProperty;
import util.settings.MapServiceLogger;

import java.io.IOException;
import java.util.List;

/**
 * @author uqpchao
 * Created 24/04/2019
 */
public class MapInferenceMain {
	
	public static void main(String[] args) throws IOException, InterruptedException {
		
		// initialize arguments
		MapInferenceProperty property = new MapInferenceProperty();
		property.loadPropertiesFromResourceFile("mapinference.properties", args);
		long initTaskTime = System.currentTimeMillis();
		
		// setup java log
		String logFolder = property.getPropertyString("algorithm.mapinference.log.LogFolder");  // obtain the log folder from args
		String dataSet = property.getPropertyString("data.Dataset");
		String inputTrajFolder = property.getPropertyString("path.InputTrajectoryFolder");
		String inferenceMethod = property.getPropertyString("algorithm.mapinference.InferenceMethod");
		String outputMapFolder = property.getPropertyString("path.OutputMapFolder");
		// log file name
		String logFileName = "";
		DistanceFunction distFunc;
		if (dataSet.contains("Beijing"))
			distFunc = new GreatCircleDistanceFunction();
		else
			distFunc = new EuclideanDistanceFunction();
		
		if (inferenceMethod.equals("TC")) {
			logFileName = dataSet + "_" + inferenceMethod + "_"
					+ property.getPropertyString("algorithm.mapinference.lineclustering.MaximumClusteringDistance") + "_"
					+ property.getPropertyString("algorithm.mapinference.lineclustering.DPEpsilon") + "_"
					+ property.getPropertyString("algorithm.mapinference.lineclustering.MaximumAngleChangeDegree") + "_" + initTaskTime;
		} else if (inferenceMethod.equals("KDE")) {
			logFileName = dataSet + "_" + inferenceMethod + "_"
					+ property.getPropertyString("algorithm.mapinference.kde.CellSize") + "_"
					+ property.getPropertyString("algorithm.mapinference.kde.GaussianBlur") + "_" + initTaskTime;
		}
		// initialize log file
		MapServiceLogger.logInit(logFolder, logFileName);
		
		// use global dataset to evaluate the map-matching accuracy
		final Logger LOG = Logger.getLogger(MapInferenceMain.class);
		LOG.info("Map inference on the " + dataSet + " dataset with argument:" + property.toString());
		
		long startTaskTime = System.currentTimeMillis();    // the start of the map-matching process
		// map inference process
		List<Trajectory> inputTrajList = TrajectoryReader.readTrajectoriesToList(inputTrajFolder, distFunc);
		if (inferenceMethod.equals("TC")) {
			LineClusteringMapInference mapInference = new LineClusteringMapInference();
			RoadNetworkGraph inferenceResult = mapInference.mapInferenceProcess(inputTrajList, property, distFunc);
			MapWriter.writeMap(inferenceResult, outputMapFolder + "0.txt");
			LOG.info("Map inference finished. Total number of road/node: " + inferenceResult.getNodes().size() + "/" + inferenceResult.getWays().size()
					+ ", Total running time: " + (System.currentTimeMillis() - initTaskTime));
		} else if (inferenceMethod.equals("TM")) {
			TraceMergeMapInference traceMergeMapInference = new TraceMergeMapInference();
			
			// epsilon; see the paper for detail
			double eps = property.getPropertyDouble("algorithm.mapinference.tracemerge.Epsilon");
			// TODO trajectory points whose pairwise point distance <2m are to be removed.
			RoadNetworkGraph resultMap = traceMergeMapInference.mapInferenceProcess(inputTrajList, eps, distFunc);
			MapWriter.writeMap(resultMap, outputMapFolder + "0.txt");
		}
		LOG.info("Map inference finished. Total running time is: " + (System.currentTimeMillis() - startTaskTime / 1000));
	}
}
