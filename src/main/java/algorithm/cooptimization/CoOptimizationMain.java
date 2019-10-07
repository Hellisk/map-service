package algorithm.cooptimization;

import evaluation.matchingevaluation.RouteMatchingEvaluation;
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
import util.object.structure.MultipleTrajectoryMatchResult;
import util.object.structure.Pair;
import util.settings.CoOptimizationProperty;
import util.settings.MapServiceLogger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

public class CoOptimizationMain {
	
	
	public static void main(String[] args) throws IOException, ExecutionException, InterruptedException {
		
		// initialize arguments
		CoOptimizationProperty property = new CoOptimizationProperty();
		property.loadPropertiesFromResourceFile("cooptimization.properties", args);
		long initTaskTime = System.currentTimeMillis();
		
		// setup java log
		String logPath = property.getPropertyString("algorithm.cooptimization.log.LogFolder");  // obtain the log folder from args
		String dataSet = property.getPropertyString("data.Dataset");
		// log file name
		String logFileName = dataSet + "_" + property.getPropertyString("algorithm.cooptimization.data.RoadRemovalPercentage") + "_"
				+ property.getPropertyString("algorithm.mapmatching.CandidateRange") + "_"
				+ property.getPropertyString("algorithm.cooptimization.GapExtensionDistance") + "_"
				+ property.getPropertyString("algorithm.mapmatching.hmm.RankLength") + "_"
				+ property.getPropertyString("algorithm.cooptimization.CorrectRoadPercentage") + "_" + initTaskTime;
		// initialize log file
		MapServiceLogger.logInit(logPath, logFileName);
		
		final Logger LOG = Logger.getLogger(CoOptimizationMain.class);
		LOG.info("Map-Trajectory Co-Optimization with arguments: " + property.toString());
		
		boolean isManualGTRequired = property.getPropertyBoolean("data.IsManualGTRequired");
		String inputTrajFolder = property.getPropertyString("path.InputTrajectoryFolder");
		String inputMapFolder = property.getPropertyString("path.InputMapFolder");
		String gtRouteMatchResultFolder;
		
		if (isManualGTRequired)
			gtRouteMatchResultFolder = property.getPropertyString("path.GroundTruthManualRouteMatchResultFolder");
		else
			gtRouteMatchResultFolder = property.getPropertyString("path.GroundTruthRouteMatchResultFolder");
		
		// read input partial map
		int percentage = property.getPropertyInteger("algorithm.cooptimization.data.RoadRemovalPercentage");
		DistanceFunction distanceFunction;
		if (dataSet.contains("Beijing"))
			distanceFunction = new GreatCircleDistanceFunction();
		else
			distanceFunction = new EuclideanDistanceFunction();
		
		RoadNetworkGraph initialMap = MapReader.readMap(inputMapFolder + percentage + ".txt", true, distanceFunction);
		List<RoadWay> removedWayList = MapReader.readWays(inputMapFolder + "remove_edges_" + percentage + ".txt", new HashMap<>(),
				distanceFunction);
		Stream<Trajectory> trajectoryStream = TrajectoryReader.readTrajectoriesToStream(inputTrajFolder, 1, distanceFunction);
//		List<Trajectory> trajectoryList = TrajectoryReader.readTrajectoriesToList(inputTrajFolder, 1, distanceFunction);
		
		Pair<RoadNetworkGraph, List<MultipleTrajectoryMatchResult>> coOptimizationResult = CoOptimization.coOptimisationProcess(trajectoryStream,
				initialMap, removedWayList, property);
		
		List<Pair<Integer, List<String>>> routeMatchResults = new ArrayList<>();
		for (MultipleTrajectoryMatchResult trajectoryMatchResult : coOptimizationResult._2()) {
			routeMatchResults.add(new Pair<>(Integer.parseInt(trajectoryMatchResult.getTrajID()),
					trajectoryMatchResult.getCompleteMatchRouteAtRank(0).getRoadIDList()));
		}
		// evaluation: map matching evaluation
		List<Pair<Integer, List<String>>> gtRouteMatchResult = MatchResultReader.readRouteMatchResults(gtRouteMatchResultFolder);
		Map<String, Double> id2RoadLength = new HashMap<>();
		for (RoadWay w : initialMap.getWays())
			id2RoadLength.put(w.getID(), w.getLength());
		RouteMatchingEvaluation.precisionRecallFScoreAccEvaluation(routeMatchResults, gtRouteMatchResult, id2RoadLength, removedWayList);
		
		LOG.info("Task finish, total time spent: " + (System.currentTimeMillis() - initTaskTime) / 1000 + " seconds");
	}
}