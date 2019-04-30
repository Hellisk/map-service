package algorithm.mapmatching;

import algorithm.mapmatching.hmm.HMMMapMatching;
import evaluation.matchingevaluation.precisionRecallMatchingEvaluation;
import org.apache.log4j.Logger;
import util.function.GreatCircleDistanceFunction;
import util.io.GlobalMapLoader;
import util.io.GlobalTrajectoryLoader;
import util.io.MatchResultReader;
import util.io.MatchResultWriter;
import util.object.roadnetwork.RoadNetworkGraph;
import util.object.spatialobject.Trajectory;
import util.object.spatialobject.TrajectoryPoint;
import util.object.structure.Pair;
import util.object.structure.TrajectoryMatchResult;
import util.settings.MapMatchingProperty;
import util.settings.MapServiceLogger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Entry for running map-matching algorithms and evaluation.
 *
 * @author Hellisk
 * @since 30/03/2019
 */
public class MapMatchingMain {
	
	public static void main(String[] args) throws IOException {
		
		// initialize arguments
		MapMatchingProperty property = new MapMatchingProperty();
		property.loadPropertiesFromResourceFile("mapmatching.properties", args);
		long initTaskTime = System.currentTimeMillis();
		
		// setup java log
		String logFolder = property.getPropertyString("algorithm.mapmatching.log.LogFolder");  // obtain the log folder from args
		String dataSet = property.getPropertyString("data.Dataset");
		String rawDataFolder = property.getPropertyString("path.RawDataFolder");
		String outputMatchResultFolder = property.getPropertyString("path.OutputMatchResultFolder");
		String logFileName = "";
		if (dataSet.equals("Global")) {
			// log file name
			logFileName = dataSet + "_" + property.getPropertyString("algorithm.mapmatching.hmm.CandidateRange") + "_"
					+ property.getPropertyString("algorithm.mapmatching.hmm.Sigma") + "_"
					+ property.getPropertyString("algorithm.mapmatching.hmm.Beta") + "_" + initTaskTime;
		} else if (dataSet.equals("Beijing")) {
			logFileName = dataSet + "_" + property.getPropertyString("algorithm.mapmatching.hmm.CandidateRange") + "_"
					+ property.getPropertyString("algorithm.mapmatching.hmm.Sigma") + "_"
					+ property.getPropertyString("algorithm.mapmatching.hmm.Beta") + "_"
					+ property.getPropertyString("algorithm.mapmatching.hmm.RankLength") + "_" + initTaskTime;
		}
		// initialize log file
		MapServiceLogger.logInit(logFolder, logFileName);
		
		final Logger LOG = Logger.getLogger(MapMatchingMain.class);
		// use global dataset to evaluate the map-matching accuracy
		LOG.info("Map-matching on the " + dataSet + " dataset with argument:" + property.toString());
		
		long startTaskTime = System.currentTimeMillis();    // the start of the map-matching process
		// map-matching process
		List<TrajectoryMatchResult> results = new ArrayList<>();
		List<Pair<Integer, List<String>>> gtRouteMatchResult = new ArrayList<>();
		if (dataSet.equals("Global")) {
			GlobalTrajectoryLoader reader = new GlobalTrajectoryLoader(rawDataFolder);
			GlobalMapLoader mapReader = new GlobalMapLoader(rawDataFolder);
			int trajPointCount = 0;
			for (int i = 0; i < reader.getNumOfTrajectory(); i++) {
				Trajectory currTraj = reader.readInputTrajectory(i);
				Iterator<TrajectoryPoint> iterator = currTraj.getSTPoints().iterator();
				TrajectoryPoint prevPoint = iterator.next();
				while (iterator.hasNext()) {
					TrajectoryPoint currPoint = iterator.next();
					if (currPoint.time() <= prevPoint.time()) {
						LOG.warn("The input time is not ordered: " + currPoint.time() + "," + prevPoint.time());
						currTraj.remove(currPoint);
					} else prevPoint = currPoint;
				}
				trajPointCount += currTraj.size();
				RoadNetworkGraph currMap = mapReader.readRawMap(i);
				HMMMapMatching mapMatching = new HMMMapMatching(currMap, property);
				TrajectoryMatchResult matchResult = mapMatching.trajectorySingleMatchingProcess(currTraj);
				results.add(matchResult);
			}
			LOG.info("Map matching finished, total time spent:" + (System.currentTimeMillis() - startTaskTime) / 1000 + "seconds");
			for (int i = 0; i < reader.getNumOfTrajectory(); i++) {
				List<String> matchResult = reader.readGTRouteMatchResult(i);
				Pair<Integer, List<String>> currGroundTruthMatchResult = new Pair<>(i, matchResult);
				gtRouteMatchResult.add(currGroundTruthMatchResult);
			}
			MatchResultWriter.writeMatchResults(results, outputMatchResultFolder);
			
			// evaluation: map matching evaluation
			results = MatchResultReader.readMatchResultsToList(outputMatchResultFolder, new GreatCircleDistanceFunction());    // used for
			// evaluation only
			precisionRecallMatchingEvaluation.globalPrecisionRecallMapMatchingEval(results, gtRouteMatchResult, rawDataFolder);
			System.out.println("Total number of trajectory points is " + trajPointCount);
		} else if (dataSet.equals("Beijing")) {
		
		}
	}
}
