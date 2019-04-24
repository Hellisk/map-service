package algorithm.cooptimization;

import evaluation.ResultEvaluation;
import org.apache.log4j.Logger;
import util.function.DistanceFunction;
import util.function.EuclideanDistanceFunction;
import util.function.GreatCircleDistanceFunction;
import util.io.MapReader;
import util.io.MatchResultReader;
import util.io.TrajectoryReader;
import util.object.roadnetwork.RoadNetworkGraph;
import util.object.roadnetwork.RoadWay;
import util.object.spatialobject.Trajectory;
import util.object.structure.Pair;
import util.object.structure.TrajectoryMatchResult;
import util.settings.CoOptimizationProperty;
import util.settings.MapServiceLogger;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

public class CoOptimizationMain {
	
	private static final Logger LOG = Logger.getLogger(CoOptimizationMain.class);
	
	public static void main(String[] args) throws IOException, ExecutionException, InterruptedException {
		
		// initialize arguments
		CoOptimizationProperty property = new CoOptimizationProperty();
		property.loadPropertiesFromResourceFile("algorithm.cooptimization.properties", args);
		long initTaskTime = System.currentTimeMillis();
		
		// setup java log
		String logPath = property.getPropertyString("algorithm.cooptimization.log.LogFolder");  // obtain the log folder from args
		String dataSet = property.getPropertyString("algorithm.cooptimization.data.Dataset");
		// log file name
		String logFileName = dataSet + "_" + property.getPropertyString("algorithm.cooptimization.data.RoadRemovalPercentage") + "_"
				+ property.getPropertyString("algorithm.mapmatching.hmm.CandidateRange") + "_"
				+ property.getPropertyString("algorithm.mapmatching.hmm.GapExtensionDistance") + "_"
				+ property.getPropertyString("algorithm.mapmatching.RankLength") + "_"
				+ property.getPropertyString("algorithm.cooptimization.CorrectRoadPercentage") + "_" + initTaskTime;
		// initialize log file
		MapServiceLogger.logInit(logPath, logFileName);
		
		LOG.info("Map-Trajectory Co-Optimization with arguments: " + property.toString());
		
		boolean isManualGTRequired = property.getPropertyBoolean("data.IsManualGTRequired");
		String inputTrajFolder = property.getPropertyString("path.InputTrajectoryFolder");
		String inputMapFolder = property.getPropertyString("path.InputMapFolder");
		String gtMatchResultFolder;
		
		if (isManualGTRequired)
			gtMatchResultFolder = property.getPropertyString("path.GroundTruthManualMatchResultFolder");
		else
			gtMatchResultFolder = property.getPropertyString("path.GroundTruthMatchResultFolder");
		
		// read input partial map
		int percentage = property.getPropertyInteger("algorithm.cooptimization.data.RoadRemovalPercentage");
		DistanceFunction distanceFunction;
		if (dataSet.contains("Beijing"))
			distanceFunction = new GreatCircleDistanceFunction();
		else
			distanceFunction = new EuclideanDistanceFunction();
		
		RoadNetworkGraph initialMap = MapReader.readMap(inputMapFolder + percentage + ".txt", true, distanceFunction);
		List<RoadWay> removedWayList = MapReader.readWays(inputMapFolder + "remove_" + percentage + ".txt", new HashMap<>(), distanceFunction);
		Stream<Trajectory> trajectoryStream = TrajectoryReader.readTrajectoriesToStream(inputTrajFolder, distanceFunction);
//		List<Trajectory> trajectoryList = TrajectoryReader.readTrajectoriesToList(inputTrajFolder, distanceFunction);
		
		Pair<RoadNetworkGraph, List<TrajectoryMatchResult>> coOptimizationResult = CoOptimization.coOptimisationProcess(trajectoryStream,
				initialMap, removedWayList, property);
		
		// evaluation: map matching evaluation
		List<Pair<Integer, List<String>>> gtRouteMatchResult = MatchResultReader.readRouteMatchResults(gtMatchResultFolder);
		ResultEvaluation resultEvaluation = new ResultEvaluation();
		resultEvaluation.beijingMapMatchingEval(coOptimizationResult._2(), gtRouteMatchResult, initialMap, removedWayList);
		
		LOG.info("Task finish, total time spent: " + (System.currentTimeMillis() - initTaskTime) / 1000 + " seconds");
	}
}