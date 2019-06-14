package algorithm.mapinference;

import algorithm.mapinference.kde.KDEMapInference;
import algorithm.mapinference.lineclustering.LineClusteringMapInference;
import algorithm.mapinference.roadrunner.RoadRunnerMapInference;
import algorithm.mapinference.tracemerge.TraceMergeMapInference;
import org.apache.log4j.Logger;
import util.function.DistanceFunction;
import util.function.EuclideanDistanceFunction;
import util.function.GreatCircleDistanceFunction;
import util.function.SpatialUtils;
import util.io.MapReader;
import util.io.MapWriter;
import util.io.TrajectoryReader;
import util.io.TrajectoryWriter;
import util.object.roadnetwork.RoadNetworkGraph;
import util.object.spatialobject.Rect;
import util.object.spatialobject.Trajectory;
import util.object.structure.Pair;
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
		String cacheFolder = property.getPropertyString("algorithm.mapinference.path.CacheFolder");    // used to store temporary files
		String dataSet = property.getPropertyString("data.Dataset");
		String pythonRootFolder = property.getPropertyString("data.PythonCodeRootPath");
		String inputTrajFolder = property.getPropertyString("path.InputTrajectoryFolder");
		String gtMapFolder = property.getPropertyString("path.GroundTruthMapFolder");
		String inferenceMethod = property.getPropertyString("algorithm.mapinference.InferenceMethod");
		String outputMapFolder = property.getPropertyString("path.OutputMapFolder");
		String dataSpec = property.getPropertyString("data.DataSpec");
		// log file name
		String logFileName = "";
		DistanceFunction distFunc;
		
		if (dataSet.contains("Beijing"))
			distFunc = new GreatCircleDistanceFunction();
		else
			distFunc = new EuclideanDistanceFunction();
		
		switch (inferenceMethod) {
			case "LC":
				logFileName = "inference_" + dataSet + "_" + inferenceMethod + "_" + dataSpec + "_"
						+ property.getPropertyString("algorithm.mapinference.lineclustering.MaximumClusteringDistance") + "_"
						+ property.getPropertyString("algorithm.mapinference.lineclustering.DPEpsilon") + "_"
						+ property.getPropertyString("algorithm.mapinference.lineclustering.MaximumAngleChangeDegree") + "_" + initTaskTime;
				break;
			case "KDE":
				logFileName = "inference_" + dataSet + "_" + inferenceMethod + "_" + dataSpec + "_"
						+ property.getPropertyString("algorithm.mapinference.kde.CellSize") + "_"
						+ property.getPropertyString("algorithm.mapinference.kde.GaussianBlur") + "_" + initTaskTime;
				break;
			case "TM":
				logFileName = "inference_" + dataSet + "_" + inferenceMethod + "_" + dataSpec + "_"
						+ property.getPropertyString("algorithm.mapinference.tracemerge.Epsilon") + "_" + initTaskTime;
				break;
			case "RR":
				logFileName = "inference_" + dataSet + "_" + inferenceMethod + "_" + dataSpec + "_"
						+ property.getPropertyString("algorithm.mapinference.roadrunner.HistoryLength") + "_"
						+ property.getPropertyString("algorithm.mapinference.roadrunner.NumberOfDeferredBranch") + "_"
						+ property.getPropertyString("algorithm.mapinference.roadrunner.MinNumberOfTrajectory") + "_" + initTaskTime;
				break;
		}
		// initialize log file
		MapServiceLogger.logInit(logFolder, logFileName);
		
		// use global dataset to evaluate the map-matching accuracy
		final Logger LOG = Logger.getLogger(MapInferenceMain.class);
		
		LOG.info("Map inference on the " + dataSet + " dataset with input from: " + inputTrajFolder);
		
		// start the process
		RoadNetworkGraph gtMap = MapReader.readMap(gtMapFolder + "0.txt", false, distFunc);
		List<Trajectory> inputTrajList = TrajectoryReader.readTrajectoriesToList(inputTrajFolder, distFunc);
		
		if (inferenceMethod.equals("KDE") || inferenceMethod.equals("RR")) {        // KDE and RoadRunner uses great circle
			// (Haversine) distance
			if (dataSet.contains("Chicago")) {
				LOG.info("Convert the input trajectory into WGS84.");
				for (Trajectory traj : inputTrajList) {    // convert the coordinate to UTM
					SpatialUtils.convertTrajUTM2WGS(traj, 16, 'T');
				}
			} else if (dataSet.contains("Berlin")) {
				LOG.info("Convert the input trajectory into WGS84.");
				for (Trajectory traj : inputTrajList) {    // convert the coordinate to UTM
					SpatialUtils.convertTrajUTM2WGS(traj, 33, 'U');
				}
			}
		} else {        // other methods use Euclidean distance
			if (dataSet.contains("Beijing")) {
				LOG.info("Convert the input trajectory and map into UTM");
				for (Trajectory traj : inputTrajList) {    // convert the coordinate to UTM
					SpatialUtils.convertTrajGCJ2UTM(traj);
				}
			}
		}
		
		// inference step
		LOG.info("Input data prepared. Start the map inference process.");
		long startTaskTime = System.currentTimeMillis();    // the start of the map-matching process
		RoadNetworkGraph outputMap;
		switch (inferenceMethod) {
			case "LC":
				LineClusteringMapInference mapInference = new LineClusteringMapInference();
				outputMap = mapInference.mapInferenceProcess(inputTrajList, property);
				if (!(outputMap.getDistanceFunction() instanceof EuclideanDistanceFunction)) {
					SpatialUtils.convertMapGCJ2UTM(outputMap);
				}
				MapWriter.writeMap(outputMap, outputMapFolder + "LC_" + dataSpec + ".txt");
				break;
			case "KDE":
				KDEMapInference kdeMapInference = new KDEMapInference(property);
				if (dataSet.equals("Berlin") || dataSet.equals("Chicago")) {
					inputTrajFolder = cacheFolder + "kdeTraj/";
					TrajectoryWriter.writeTrajectories(inputTrajList, inputTrajFolder);
				}
				outputMap = kdeMapInference.mapInferenceProcess(pythonRootFolder + "kde/", inputTrajFolder, cacheFolder + "kde/");
				SpatialUtils.convertMapWGS2UTM(outputMap);
				MapWriter.writeMap(outputMap, outputMapFolder + "KDE_" + dataSpec + ".txt");
				break;
			case "TM":
				TraceMergeMapInference traceMergeMapInference = new TraceMergeMapInference();
				// TODO trajectory points whose pairwise point distance <2m are to be removed.
				outputMap = traceMergeMapInference.mapInferenceProcess(inputTrajList, property);
				MapWriter.writeMap(outputMap, outputMapFolder + "TM_" + dataSpec + ".txt");
				break;
			case "RR":
				Rect boundary;
				if (dataSet.equals("Chicago") || dataSet.equals("Berlin")) {
					boundary = gtMap.getBoundary();
					if (dataSet.equals("Chicago")) {
						Pair<Double, Double> minLonLat = SpatialUtils.convertUTM2WGS(boundary.minX(), boundary.minY(), 16, 'T');
						Pair<Double, Double> maxLonLat = SpatialUtils.convertUTM2WGS(boundary.maxX(), boundary.maxY(), 16, 'T');
						boundary = new Rect(minLonLat._1(), minLonLat._2(), maxLonLat._1(), maxLonLat._2(), new GreatCircleDistanceFunction());
						LOG.info("Convert the Chicago map bounding box from UTM to WGS: " + minLonLat._1() + "," + maxLonLat._1()
								+ "," + minLonLat._2() + "," + maxLonLat._2());
					} else {    // Berlin
						Pair<Double, Double> minLonLat = SpatialUtils.convertUTM2WGS(boundary.minX(), boundary.minY(), 33, 'U');
						Pair<Double, Double> maxLonLat = SpatialUtils.convertUTM2WGS(boundary.maxX(), boundary.maxY(), 33, 'U');
						boundary = new Rect(minLonLat._1(), minLonLat._2(), maxLonLat._1(), maxLonLat._2(), new GreatCircleDistanceFunction());
						LOG.info("Convert the Berlin map bounding box from UTM to WGS: " + minLonLat._1() + "," + maxLonLat._1()
								+ "," + minLonLat._2() + "," + maxLonLat._2());
					}
				} else {
					boundary = gtMap.getBoundary();
				}
				if (dataSet.equals("Berlin") || dataSet.equals("Chicago")) {
					inputTrajFolder = cacheFolder + "roadRunnerTraj/";
					TrajectoryWriter.writeTrajectories(inputTrajList, inputTrajFolder);
				}
				RoadRunnerMapInference roadRunnerMapInference = new RoadRunnerMapInference(property, boundary);
				outputMap = roadRunnerMapInference.mapInferenceProcess(pythonRootFolder + "roadrunner/", inputTrajFolder,
						cacheFolder + "roadRunner/");
				SpatialUtils.convertMapWGS2UTM(outputMap);
				MapWriter.writeMap(outputMap, outputMapFolder + "RR_" + dataSpec + ".txt");
				break;
			default:     // TODO continue
				LOG.error("The inference method " + inferenceMethod + "does not exist.");
				outputMap = gtMap;
				break;
		}
		
		// note that all output map should be under the UTM coordination system
		LOG.info("Map inference finished. Total number of road/node: " + outputMap.getNodes().size() + "/" + outputMap.getWays().size()
				+ ", Total inference time: " + (System.currentTimeMillis() - startTaskTime) / 1000);
	}
}
