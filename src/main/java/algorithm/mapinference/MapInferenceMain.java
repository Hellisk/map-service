package algorithm.mapinference;

import evaluation.mapevaluation.pathbaseddistance.benchmarkexperiments.PathBasedMapEvaluation;
import org.apache.log4j.Logger;
import util.function.DistanceFunction;
import util.function.EuclideanDistanceFunction;
import util.function.GreatCircleDistanceFunction;
import util.io.MapReader;
import util.object.roadnetwork.RoadNetworkGraph;
import util.settings.MapInferenceProperty;
import util.settings.MapServiceLogger;

import java.io.IOException;

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
		String inputTrajFolder = property.getPropertyString("path.InputTrajectoryFolder");
		String gtMapFolder = property.getPropertyString("path.GroundTruthMapFolder");
		String inferenceMethod = property.getPropertyString("algorithm.mapinference.InferenceMethod");
		String outputMapFolder = property.getPropertyString("path.OutputMapFolder");
		// log file name
		String logFileName = "";
		DistanceFunction distFunc;
		if (dataSet.contains("Beijing"))
			distFunc = new GreatCircleDistanceFunction();
		else
			distFunc = new EuclideanDistanceFunction();
		
		switch (inferenceMethod) {
			case "TC":
				logFileName = dataSet + "_" + inferenceMethod + "_"
						+ property.getPropertyString("algorithm.mapinference.lineclustering.MaximumClusteringDistance") + "_"
						+ property.getPropertyString("algorithm.mapinference.lineclustering.DPEpsilon") + "_"
						+ property.getPropertyString("algorithm.mapinference.lineclustering.MaximumAngleChangeDegree") + "_" + initTaskTime;
				break;
			case "KDE":
				logFileName = dataSet + "_" + inferenceMethod + "_"
						+ property.getPropertyString("algorithm.mapinference.kde.CellSize") + "_"
						+ property.getPropertyString("algorithm.mapinference.kde.GaussianBlur") + "_" + initTaskTime;
				break;
			case "TM":
				logFileName = dataSet + "_" + inferenceMethod + "_"
						+ property.getPropertyString("algorithm.mapinference.tracemerge.Epsilon") + "_" + initTaskTime;
				break;
		}
		// initialize log file
		MapServiceLogger.logInit(logFolder, logFileName);
		
		// use global dataset to evaluate the map-matching accuracy
		final Logger LOG = Logger.getLogger(MapInferenceMain.class);
		
		LOG.info("Map inference on the " + dataSet + " dataset with argument:" + property.toString());
		
		// preprocessing step
		RoadNetworkGraph gtMap = MapReader.readMap(gtMapFolder + "0.txt", false, distFunc);
//		List<Trajectory> inputTrajList;
//		if (dataSet.contains("Beijing") && property.getPropertyBoolean("data.IsSyntheticTrajectory")) {
//			double sigma = 5;    // parameter for trajectory noise level
//			int samplingInterval = 15;    // trajectory sampling interval minimum 1s
//			LOG.info("Generate synthetic dataset with sigma=" + sigma + " and sampling interval=" + samplingInterval + ".");
//			// generate synthetic dataset according to the existing dataset provided
//			String inputSyntheticTrajFolder = property.getPropertyString("path.InputSyntheticTrajectoryFolder");
//			String gtMatchResultFolder = property.getPropertyString("path.GroundTruthMatchResultFolder");
//			List<Trajectory> originalInputTrajList = TrajectoryReader.readTrajectoriesToList(inputTrajFolder, distFunc);
//			for (Trajectory traj : originalInputTrajList) {    // convert the coordinate to UTM
//				SpatialUtils.convertTrajGCJ2UTM(traj);
//			}
//			SpatialUtils.convertMapGCJ2UTM(gtMap);
//			List<Pair<Integer, List<String>>> originalGTRouteList = MatchResultReader.readRouteMatchResults(gtMatchResultFolder);
//			Map<Integer, List<String>> id2GTRouteMap = new HashMap<>();
//			for (Pair<Integer, List<String>> routePair : originalGTRouteList) {
//				if (!id2GTRouteMap.containsKey(routePair._1())) {
//					id2GTRouteMap.put(routePair._1(), routePair._2());
//				} else
//					throw new IllegalArgumentException("Two trajectories has the same id:" + routePair._1());
//			}
//			List<Long> timeDiffList = new ArrayList<>();
//			List<Pair<String, List<String>>> gtRouteList = new ArrayList<>();
//			for (Trajectory traj : originalInputTrajList) {
//				if (id2GTRouteMap.containsKey(Integer.parseInt(traj.getID()))) {
//					long timeDiff = traj.getSTPoints().get(traj.size() - 1).time() - traj.getSTPoints().get(0).time();
//					timeDiffList.add(timeDiff);
//					gtRouteList.add(new Pair<>(traj.getID(), id2GTRouteMap.get(Integer.parseInt(traj.getID()))));
//				} else
//					throw new IllegalArgumentException("Cannot find the corresponding ground-truth for trajectory: " + traj.getID());
//			}
//			inputTrajList = TrajectoryGenerator.rawTrajGenerator(gtRouteList, timeDiffList, gtMap, sigma, samplingInterval);
//			TrajectoryWriter.writeTrajectories(inputTrajList, inputSyntheticTrajFolder);
//		} else {
//			inputTrajList = TrajectoryReader.readTrajectoriesToList(inputTrajFolder, distFunc);
//			if (dataSet.contains("Beijing")) {
//				for (Trajectory traj : inputTrajList) {    // convert the coordinate to UTM
//					SpatialUtils.convertTrajGCJ2UTM(traj);
//				}
//				SpatialUtils.convertMapGCJ2UTM(gtMap);
//			}
//		}
//
//		// inference step
//		long startTaskTime = System.currentTimeMillis();    // the start of the map-matching process
//		RoadNetworkGraph outputMap;
//		if (inferenceMethod.equals("TC")) {
//			LineClusteringMapInference mapInference = new LineClusteringMapInference();
//			outputMap = mapInference. mapInferenceProcess(inputTrajList, property, distFunc);
//			MapWriter.writeMap(outputMap, outputMapFolder + "0.txt");
//		} else if (inferenceMethod.equals("TM")) {
//			TraceMergeMapInference traceMergeMapInference = new TraceMergeMapInference();
//
//			// epsilon, see the paper for detail
//			double eps = property.getPropertyDouble("algorithm.mapinference.tracemerge.Epsilon");
//			// TODO trajectory points whose pairwise point distance <2m are to be removed.
//			outputMap = traceMergeMapInference.mapInferenceProcess(inputTrajList, eps, distFunc);
//			MapWriter.writeMap(outputMap, outputMapFolder + "0.txt");
//		} else {	// TODO continue
//			outputMap = gtMap;
//		}
//
//		LOG.info("Map inference finished. Total number of road/node: " + outputMap.getNodes().size() + "/" + outputMap.getWays().size()
//				+ ", Total running time: " + (System.currentTimeMillis() - initTaskTime) / 1000);
		
		// evaluation step
		RoadNetworkGraph outputMap = MapReader.readMap(outputMapFolder + "0.txt", false, new EuclideanDistanceFunction());
		String pathBasedFrechetResult = PathBasedMapEvaluation.pathBasedFrechetMapEval(outputMap, gtMap, "LinkThree", cacheFolder);
		String pathBasedHausdorffResult = PathBasedMapEvaluation.pathBasedHausdorffMapEval(outputMap, gtMap, "LinkThree", cacheFolder);
	}
}
