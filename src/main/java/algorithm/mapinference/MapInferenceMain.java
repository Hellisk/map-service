package algorithm.mapinference;

import algorithm.mapinference.kde.KDEMapInference;
import algorithm.mapinference.lineclustering.LineClusteringMapInference;
import algorithm.mapinference.pointclustering.KharitaMapInference;
import algorithm.mapinference.roadrunner.RoadRunnerMapInference;
import algorithm.mapinference.topicmodel.CRIFMapInference;
import algorithm.mapinference.tracemerge.TraceMergeMapInference;
import org.apache.log4j.Logger;
import util.function.DistanceFunction;
import util.function.EuclideanDistanceFunction;
import util.function.GreatCircleDistanceFunction;
import util.function.SpatialUtils;
import util.io.*;
import util.object.roadnetwork.RoadNetworkGraph;
import util.object.spatialobject.Rect;
import util.object.spatialobject.Trajectory;
import util.object.spatialobject.TrajectoryPoint;
import util.object.structure.Pair;
import util.settings.MapInferenceProperty;
import util.settings.MapServiceLogger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
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
			case "CRIF":
				logFileName = "inference_" + dataSet + "_" + inferenceMethod + "_" + dataSpec + "_"
						+ property.getPropertyString("algorithm.mapinference.crif.CellWidth") + "_" + initTaskTime;
				break;
			case "KHA":
				logFileName = "inference_" + dataSet + "_" + inferenceMethod + "_" + dataSpec + "_"
						+ property.getPropertyString("algorithm.mapinference.pointclustering.Radius") + "_"
						+ property.getPropertyString("algorithm.mapinference.pointclustering.DensityDistance") + "_"
						+ property.getPropertyString("algorithm.mapinference.pointclustering.AngleTolerance") + "_" + initTaskTime;
				break;
			default:
				logFileName = "Failure";
				break;
		}
		// initialize log file
		MapServiceLogger.logInit(logFolder, logFileName);
		
		// use global dataset to evaluate the map-matching accuracy
		final Logger LOG = Logger.getLogger(MapInferenceMain.class);
		
		LOG.info("Map inference on the " + dataSet + " dataset with input from: " + inputTrajFolder);
		
		// start the process
		RoadNetworkGraph gtMap = MapReader.readMap(gtMapFolder + "0.txt", false, distFunc);
		List<Trajectory> inputTrajList = TrajectoryReader.readTrajectoriesToList(inputTrajFolder, 1, distFunc);
		
		// KDE, RoadRunner and Kharita uses great circle (Haversine) distance
		if (inferenceMethod.equals("KDE") || inferenceMethod.equals("RR") || inferenceMethod.equals("KHA")) {
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
					IOService.cleanFolder(cacheFolder + "kdeTraj/");
					TrajectoryWriter.writeTrajectories(inputTrajList, inputTrajFolder);
				}
				outputMap = kdeMapInference.mapInferenceProcess(pythonRootFolder + "kde/", inputTrajFolder, cacheFolder + "kde/");
				SpatialUtils.convertMapWGS2UTM(outputMap);
				MapWriter.writeMap(outputMap, outputMapFolder + "KDE_" + dataSpec + ".txt");
				break;
			case "CRIF":
				Rect crifBoundary;
				if (dataSet.contains("Beijing")) {
					crifBoundary = gtMap.getBoundary();
					Pair<Double, Double> minLonLat = SpatialUtils.convertGCJ2UTM(crifBoundary.minX(), crifBoundary.minY());
					Pair<Double, Double> maxLonLat = SpatialUtils.convertGCJ2UTM(crifBoundary.maxX(), crifBoundary.maxY());
					crifBoundary = new Rect(minLonLat._1(), minLonLat._2(), maxLonLat._1(), maxLonLat._2(), new EuclideanDistanceFunction());
					LOG.info("Convert the " + dataSet + "map bounding box from WGS84 to UTM: " + minLonLat._1() + "," + maxLonLat._1()
							+ "," + minLonLat._2() + "," + maxLonLat._2());
				} else {
					crifBoundary = gtMap.getBoundary();
				}
				inputTrajFolder = cacheFolder + "crifTraj/";
				// convert to a pickle file
				IOService.createFolder(inputTrajFolder);
				IOService.cleanFolder(inputTrajFolder);
				BufferedWriter crifBW = new BufferedWriter(new FileWriter(new File(inputTrajFolder + dataSet + ".pickle")));
				for (int index = 0; index < inputTrajList.size(); index++) {
					Trajectory traj = inputTrajList.get(index);
					StringBuilder currTrajString = new StringBuilder();
					for (int i = 0; i < traj.size(); i++) {
						TrajectoryPoint trajPoint = traj.get(i);
						currTrajString.append(i).append(",").append(trajPoint.x()).append(",").append(trajPoint.y())
								.append(",").append(trajPoint.time()).append(",").append(traj.getID()).append("\n");
					}
					if (index == inputTrajList.size() - 1) {    // last line should remove "\n"
						crifBW.write(currTrajString.toString().substring(0, currTrajString.length() - 1));
					} else {
						crifBW.write(currTrajString.toString());
					}
				}
				crifBW.flush();
				crifBW.close();
				CRIFMapInference crifMapInference = new CRIFMapInference(property, crifBoundary);
				outputMap = crifMapInference.mapInferenceProcess(pythonRootFolder + "crif/", inputTrajFolder, cacheFolder + "crif/");
				MapWriter.writeMap(outputMap, outputMapFolder + "CRIF_" + dataSpec + ".txt");
				break;
			case "KHA":
				inputTrajFolder = cacheFolder + "khaTraj/";
				// convert to a csv file
				IOService.createFolder(inputTrajFolder);
				IOService.cleanFolder(inputTrajFolder);
//				DateFormat dateFormat = new SimpleDateFormat("yyyy-mm-dd hh:mm:ss+03");
				BufferedWriter bw = new BufferedWriter(new FileWriter(new File(inputTrajFolder + dataSet + ".txt")));
				for (int index = 0; index < inputTrajList.size(); index++) {
					Trajectory traj = inputTrajList.get(index);
					StringBuilder currTrajString = new StringBuilder();
					for (int i = 0; i < traj.size(); i++) {
						TrajectoryPoint trajPoint = traj.get(i);
//						Date date = new Date(trajPoint.time());
//						String time = dateFormat.format(date);
						currTrajString.append(traj.getID()).append(",").append(trajPoint.time()).append(",").append(trajPoint.y())
								.append(",").append(trajPoint.x()).append(",").append(trajPoint.speed()).append(",")
								.append(trajPoint.heading()).append("\n");
					}
					if (index == inputTrajList.size() - 1) {    // last line should remove "\n"
						bw.write(currTrajString.toString().substring(0, currTrajString.length() - 1));
					} else {
						bw.write(currTrajString.toString());
					}
				}
				bw.flush();
				bw.close();
				KharitaMapInference kharitaMapInference = new KharitaMapInference(property);
				outputMap = kharitaMapInference.mapInferenceProcess(pythonRootFolder + "kharita/", inputTrajFolder,
						cacheFolder + "kharita/");
				SpatialUtils.convertMapGCJ2UTM(outputMap);
				MapWriter.writeMap(outputMap, outputMapFolder + "KHA_" + dataSpec + ".txt");
				break;
			case "TM":
				TraceMergeMapInference traceMergeMapInference = new TraceMergeMapInference();
				// TODO trajectory points whose pairwise point distance <2m are to be removed.
				outputMap = traceMergeMapInference.mapInferenceProcess(inputTrajList, property);
				MapWriter.writeMap(outputMap, outputMapFolder + "TM_" + dataSpec + ".txt");
				break;
			case "RR":
				Thread.sleep(30000);    // wait to close the last daemon thread.
				Rect rrBoundary;
				if (dataSet.equals("Chicago") || dataSet.equals("Berlin")) {
					rrBoundary = gtMap.getBoundary();
					if (dataSet.equals("Chicago")) {
						Pair<Double, Double> minLonLat = SpatialUtils.convertUTM2WGS(rrBoundary.minX(), rrBoundary.minY(), 16, 'T');
						Pair<Double, Double> maxLonLat = SpatialUtils.convertUTM2WGS(rrBoundary.maxX(), rrBoundary.maxY(), 16, 'T');
						rrBoundary = new Rect(minLonLat._1(), minLonLat._2(), maxLonLat._1(), maxLonLat._2(), new GreatCircleDistanceFunction());
						LOG.info("Convert the Chicago map bounding box from UTM to WGS: " + minLonLat._1() + "," + maxLonLat._1()
								+ "," + minLonLat._2() + "," + maxLonLat._2());
					} else {    // Berlin
						Pair<Double, Double> minLonLat = SpatialUtils.convertUTM2WGS(rrBoundary.minX(), rrBoundary.minY(), 33, 'U');
						Pair<Double, Double> maxLonLat = SpatialUtils.convertUTM2WGS(rrBoundary.maxX(), rrBoundary.maxY(), 33, 'U');
						rrBoundary = new Rect(minLonLat._1(), minLonLat._2(), maxLonLat._1(), maxLonLat._2(), new GreatCircleDistanceFunction());
						LOG.info("Convert the Berlin map bounding box from UTM to WGS: " + minLonLat._1() + "," + maxLonLat._1()
								+ "," + minLonLat._2() + "," + maxLonLat._2());
					}
					inputTrajFolder = cacheFolder + "roadRunnerTraj/";
					TrajectoryWriter.writeTrajectories(inputTrajList, inputTrajFolder);
				} else {
					rrBoundary = gtMap.getBoundary();
				}
				RoadRunnerMapInference roadRunnerMapInference = new RoadRunnerMapInference(property, rrBoundary);
				outputMap = roadRunnerMapInference.mapInferenceProcess(pythonRootFolder + "roadrunner/", inputTrajFolder,
						cacheFolder + "roadRunner/", inputTrajList);
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
