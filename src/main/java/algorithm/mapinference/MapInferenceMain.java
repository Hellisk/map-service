package algorithm.mapinference;

import algorithm.mapinference.trajectoryclustering.TrajectoryClusteringMapInference;
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
	private static final Logger LOG = Logger.getLogger(MapInferenceMain.class);
	
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
					+ property.getPropertyString("algorithm.mapinference.traceclustering.MaximumClusteringDistance") + "_"
					+ property.getPropertyString("algorithm.mapinference.traceclustering.DPEpsilon") + "_"
					+ property.getPropertyString("algorithm.mapinference.traceclustering.MaximumAngleChangeDegree") + "_" + initTaskTime;
		} else if (inferenceMethod.equals("KDE")) {
			logFileName = dataSet + "_" + inferenceMethod + "_"
					+ property.getPropertyString("algorithm.mapinference.kde.CellSize") + "_"
					+ property.getPropertyString("algorithm.mapinference.kde.GaussianBlur") + "_" + initTaskTime;
		}
		// initialize log file
		MapServiceLogger.logInit(logFolder, logFileName);
		
		// use global dataset to evaluate the map-matching accuracy
		LOG.info("Map inference on the " + dataSet + " dataset with argument:" + property.toString());
		
		long startTaskTime = System.currentTimeMillis();    // the start of the map-matching process
		// map inference process
		if (inferenceMethod.equals("TC")) {
			List<Trajectory> inputTrajList = TrajectoryReader.readTrajectoriesToList(inputTrajFolder, distFunc);
			TrajectoryClusteringMapInference mapInference = new TrajectoryClusteringMapInference();
			RoadNetworkGraph inferenceResult = mapInference.mapInferenceProcess(inputTrajList, property, distFunc);
			MapWriter.writeMap(inferenceResult, outputMapFolder + "0.txt");
			LOG.info("Map inference finished. Total number of road/node: " + inferenceResult.getNodes().size() + "/" + inferenceResult.getWays().size()
					+ ", Total running time: " + (System.currentTimeMillis() - initTaskTime));
		} else if (dataSet.equals("Beijing")) {
		
		}
		
		
	}
}
