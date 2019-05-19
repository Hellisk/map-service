package preprocessing;

import org.apache.log4j.Logger;
import util.function.DistanceFunction;
import util.function.GreatCircleDistanceFunction;
import util.io.*;
import util.object.roadnetwork.RoadNetworkGraph;
import util.object.spatialobject.Trajectory;
import util.settings.MapServiceLogger;
import util.settings.PreprocessingProperty;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class PreprocessingMain {
	
	
	public static void main(String[] args) throws IOException, ExecutionException, InterruptedException {
		
		// initialize arguments
		PreprocessingProperty property = new PreprocessingProperty();
		property.loadPropertiesFromResourceFile("preprocessing.properties", args);
		long initTaskTime = System.currentTimeMillis();
		
		// setup java log
		String logPath = property.getPropertyString("algorithm.preprocessing.log.LogFolder");  // obtain the log folder from args
		String dataSet = property.getPropertyString("data.Dataset");
		String inputTrajFolder = property.getPropertyString("path.InputTrajectoryFolder");
		String inputMapFolder = property.getPropertyString("path.InputMapFolder");
		String gtMapFolder = property.getPropertyString("path.GroundTruthMapFolder");
		String gtMatchResultFolder = property.getPropertyString("path.GroundTruthMatchResultFolder");
		int numOfTraj = property.getPropertyInteger("data.NumberOfTrajectory");
		int trajMinLengthSec = property.getPropertyInteger("data.TrajectoryMinimalLengthSec");
		int sampleMaxIntervalSec = property.getPropertyInteger("data.SampleMaximalIntervalSec");
		// log file name
//		String logFileName =
//				"preprocessing_" + dataSet + "_" + property.getPropertyString("algorithm.cooptimization.data.RoadRemovalPercentage") + "_" + initTaskTime;
		String logFileName = "preprocessing_" + dataSet + "_" + initTaskTime;
		// initialize log file
		MapServiceLogger.logInit(logPath, logFileName);
		
		final Logger LOG = Logger.getLogger(PreprocessingMain.class);
		LOG.info("Map-Trajectory Co-Optimization with arguments: " + property.toString());
		
		if (dataSet.contains("Beijing")) {
			DistanceFunction distFunc = new GreatCircleDistanceFunction();
			String rawDataFolder = property.getPropertyString("path.RawDataFolder");
//			if (property.getPropertyBoolean("data.IsRawInitRequired")) {
			LOG.info("Initializing the entire Beijing road map... This step is not required unless the raw data is changed.");
			// initialization: read raw map shape file and convert into csv file with default boundaries
			LOG.info("Start reading the raw road map from SHP file.");
			BeijingMapLoader shpReader = new BeijingMapLoader(rawDataFolder + "map/");
			RoadNetworkGraph initialMap = shpReader.loadRawMap();
			// write the visited map to the ground truth folder
			MapWriter.writeMap(initialMap, gtMapFolder + "raw.txt");
			
			LOG.info("Raw file initialization done.");
//			}
			
			LOG.info("Start the data preprocessing step, including map resizing, trajectory filtering and map manipulation...");
			
			// pre-processing step 1: read entire ground truth map from csv file and select the bounded area
			LOG.info("Start extracting the original map and resizing it by the bounding box.");
			String[] boundingBoxInfo = property.getPropertyString("data.BoundingBox").split(",");
			double[] boundingBox;
			if (boundingBoxInfo.length != 4)
				boundingBox = new double[0];
			else {
				boundingBox = new double[boundingBoxInfo.length];
				for (int i = 0; i < boundingBoxInfo.length; i++) {
					boundingBox[i] = Double.parseDouble(boundingBoxInfo[i]);
				}
			}
			RoadNetworkGraph roadNetworkGraph = MapReader.extractMapWithBoundary(gtMapFolder + "raw.txt", false, boundingBox, distFunc);
			MapWriter.writeMap(roadNetworkGraph, gtMapFolder + "0.txt");
			
			// pre-processing step 2: read and filter raw trajectories, remaining trajectories are guaranteed to be matched on given road map
			// area
//			boolean isManualGTRequired = property.getPropertyBoolean("data.IsManualGTRequired");
//			String gtManualMatchResultFolder = property.getPropertyString("path.GroundTruthManualMatchResultFolder");

//			if (isManualGTRequired)
//				LOG.info("Start the trajectory filtering and ground-truth result generation.");
//			else
			LOG.info("Start the trajectory filtering.");
			BeijingTrajectoryLoader trajFilter = new BeijingTrajectoryLoader(numOfTraj, trajMinLengthSec, sampleMaxIntervalSec);
//			if (isManualGTRequired) {
//				RoadNetworkGraph rawCompleteMap = MapReader.readMap(gtMapFolder + "raw.txt", false, distFunc);
//				trajFilter.readTrajAndGenerateGTRouteMatchResult(roadNetworkGraph, rawCompleteMap, rawDataFolder + "trajectory" +
//						"/beijingTrajectory-50000", inputTrajFolder, gtManualMatchResultFolder, property);
//			} else
			trajFilter.readTrajWithGTRouteMatchResult(roadNetworkGraph, rawDataFolder + "trajectory/beijingTrajectory",
					inputTrajFolder, gtMatchResultFolder);

//			// pre-processing step 3: road map removal, remove road ways from ground truth map to generate an outdated map
//			int percentage = property.getPropertyInteger("algorithm.cooptimization.data.RoadRemovalPercentage");
//			int candidateRange = property.getPropertyInteger("algorithm.mapmatching.hmm.CandidateRange");
//			int minRoadLength = property.getPropertyInteger("algorithm.mapmerge.MinimumRoadLength");
//			LOG.info("Start manipulating the map according to the given road removal percentage: " + percentage);
//			if (percentage == 0)
			MapWriter.writeMap(roadNetworkGraph, inputMapFolder + "0.txt");        // no removal happens, directly copy the map to input
//				// folder
//			else
//				MapPreprocessing.popularityBasedRoadRemoval(roadNetworkGraph, percentage, candidateRange / 2, minRoadLength, inputMapFolder);
		} else if (dataSet.contains("Chicago") || dataSet.contains("Berlin") || dataSet.contains("Athens")) {
			String rawDataFolder = property.getPropertyString("path.RawDataFolder");
			LOG.info("Initializing the road map for " + dataSet + ".");
			OSMMapLoader mapLoader = new OSMMapLoader(rawDataFolder + "map/");
			RoadNetworkGraph currMap = mapLoader.loadRawMap(dataSet);
			MapWriter.writeMap(currMap, gtMapFolder + "0.txt");
			LOG.info("Map initialized, total number of nodes: " + currMap.getNodes().size() + ", number of edges: " + currMap.getWays().size()
					+ ", total number of mini nodes: " + (currMap.getAllTypeOfNodes().size() - currMap.getNodes().size()) + ".");
			LOG.info("Start initializing the trajectories.");
			OSMTrajectoryLoader trajLoader = new OSMTrajectoryLoader(numOfTraj, trajMinLengthSec, sampleMaxIntervalSec);
			List<Trajectory> trajList = trajLoader.loadTrajectories(currMap, rawDataFolder + "trajectory/", inputTrajFolder);
			trajLoader.trajectoryVisitAssignmentWithMapMatching(currMap, trajList, property);
			MapWriter.writeMap(currMap, inputMapFolder + "0.txt");
		} else
			throw new IllegalArgumentException("Wrong dataset specified: " + dataSet);
		
		LOG.info("Data preprocessing finish, total time spent: " + (System.currentTimeMillis() - initTaskTime) / 1000 + " seconds");
	}
}