package algorithm.mapmatching;

import algorithm.mapmatching.hmm.HMMMapMatching;
import algorithm.mapmatching.hmm.HMMProbabilities;
import algorithm.mapmatching.simpleHMM.Matcher;
import algorithm.mapmatching.simpleHMM.StateCandidate;
import evaluation.matchingevaluation.RouteMatchingEvaluation;
import org.apache.log4j.Logger;
import util.function.DistanceFunction;
import util.function.GreatCircleDistanceFunction;
import util.io.*;
import util.object.roadnetwork.RoadNetworkGraph;
import util.object.spatialobject.Trajectory;
import util.object.spatialobject.TrajectoryPoint;
import util.object.structure.MultipleTrajectoryMatchResult;
import util.object.structure.Pair;
import util.object.structure.PointMatch;
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
        String inputTrajFolder = property.getPropertyString("path.InputTrajectoryFolder");
        String inputMapFolder = property.getPropertyString("path.InputMapFolder");
        String outputMatchResultFolder = property.getPropertyString("path.OutputMatchResultFolder");
        String groundTruthRouteMatchResultFolder = property.getPropertyString("path.GroundTruthRouteMatchResultFolder");
        String groundTruthPointMatchResultFolder = property.getPropertyString("path.GroundTruthPointMatchResultFolder");
        String matchingMethod = property.getPropertyString("algorithm.mapmatching.MatchingMethod");
        String dataSpec = property.getPropertyString("data.DataSpec");
        DistanceFunction distFunc;
        String logFileName = "";
        // log file name
        switch (matchingMethod) {
            case "HMM":
                logFileName = "matching_" + dataSet + "_" + matchingMethod + "_" + dataSpec + "_"
                        + property.getPropertyString("algorithm.mapmatching.CandidateRange") + "_"
                        + property.getPropertyString("algorithm.mapmatching.hmm.Sigma") + "_"
                        + property.getPropertyString("algorithm.mapmatching.hmm.Beta") + "_" + initTaskTime;
                break;
            default:
                logFileName = "matching_" + dataSet + "_" + matchingMethod + "_" + dataSpec + "_" + initTaskTime;
                break;
        }
        // initialize log file
        MapServiceLogger.logInit(logFolder, logFileName);

        final Logger LOG = Logger.getLogger(MapMatchingMain.class);
        // use global dataset to evaluate the map-matching accuracy
        LOG.info("Map-matching on the " + dataSet + " dataset with argument:" + property.toString());

        long startTaskTime = System.currentTimeMillis();    // the start of the map-matching process
        // map-matching process
        List<Pair<Integer, List<PointMatch>>> pointMatchResult = new ArrayList<>();
        List<Pair<Integer, List<String>>> routeMatchResult = new ArrayList<>();
        if (dataSet.equals("Global")) {
            String rawDataFolder = property.getPropertyString("path.RawDataFolder");
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
                MultipleTrajectoryMatchResult matchResult = mapMatching.trajectorySingleMatchingProcess(currTraj);
                routeMatchResult.add(new Pair<>(i, matchResult.getCompleteMatchRouteAtRank(0).getRoadIDList()));
            }
            LOG.info("Map matching finished, total time spent:" + (System.currentTimeMillis() - startTaskTime) / 1000 + "seconds");
            MatchResultWriter.writeRouteMatchResults(routeMatchResult, outputMatchResultFolder);

            System.out.println("Total number of trajectory points is " + trajPointCount);
        } else {
            distFunc = new GreatCircleDistanceFunction();
            RoadNetworkGraph roadMap = MapReader.readMap(inputMapFolder + "0.txt", false, distFunc);
//			Stream<Trajectory> inputTrajStream = TrajectoryReader.readTrajectoriesToStream(inputTrajFolder, distFunc);
//			HMMMapMatching mapMatching = new HMMMapMatching(roadMap, property);
//			Stream<MatchResultWithUnmatchedTraj> currCombinedMatchResultStream = mapMatching.trajectoryStreamMatchingProcess(inputTrajStream);
//			List<MatchResultWithUnmatchedTraj> currCombinedMatchResultList = currCombinedMatchResultStream.collect(Collectors.toList());
//			for (MatchResultWithUnmatchedTraj currPair : currCombinedMatchResultList) {
//				routeMatchResult.add(new Pair<>(Integer.parseInt(currPair.getTrajID()),
//						currPair.getMatchResult().getCompleteMatchRouteAtRank(0).getRoadIDList()));
//			}
//			MatchResultWriter.writeRouteMatchResults(routeMatchResult, outputMatchResultFolder);
//			LOG.info("Matching complete, total time spent:" + (System.currentTimeMillis() - startTaskTime) / 1000 + "seconds.");

            List<Trajectory> inputTrajList = TrajectoryReader.readTrajectoriesToList(inputTrajFolder, distFunc);
//			WeightBasedMM weightBasedMM = new WeightBasedMM(roadMap, property, 1000, 12, 21, 32, 35);
//
//			for (Trajectory trajectory : inputTrajList) {
//				weightBasedMM.doMatching(trajectory);
//			}
//
//			List<Pair<Integer, List<PointMatch>>> matchedPointSequence = weightBasedMM.getOutputPointMatchResult();
//			List<Pair<Integer, List<String>>> matchedWaySequence = weightBasedMM.getOutputRouteMatchResult();
//
//			MatchResultWriter.writeRouteMatchResults(matchedWaySequence, outputMatchResultFolder + "/route");
//			MatchResultWriter.writePointMatchResults(matchedPointSequence, outputMatchResultFolder + "/point");
            // evaluation test
//            List<Pair<Integer, List<String>>> gtRouteMatchResult = MatchResultReader.readRouteMatchResults(groundTruthRouteMatchResultFolder);
//			List<Pair<Integer, List<PointMatch>>> gtPointMatchResult = MatchResultReader.readPointMatchResults(groundTruthPointMatchResultFolder, distFunc);

//			RouteMatchingEvaluation.precisionRecallFScoreAccEvaluation(matchedWaySequence, gtRouteMatchResult, roadMap, null);
//			PointMatchingEvaluation.rootMeanSquareErrorEvaluation(pointMatchResult, gtPointMatchResult);
//			PointMatchingEvaluation.accuracyEvaluation(pointMatchResult, gtPointMatchResult);


//            /* Simple HMM test*/
            double sigma = property.getPropertyDouble("algorithm.mapmatching.hmm.Sigma");
            double beta = property.getPropertyDouble("algorithm.mapmatching.hmm.Beta");
            HMMProbabilities hmmProbabilities = new HMMProbabilities(sigma, beta);

            Matcher simpleHMM = new Matcher(roadMap, property, hmmProbabilities, 50, 1000);
            for (Trajectory trajectory : inputTrajList) {
                List<StateCandidate> sequence = simpleHMM.mmatch(trajectory);
                simpleHMM.pullResult(sequence, Integer.parseInt(trajectory.getID()));
            }
            List<Pair<Integer, List<String>>> matchedWaySequence = simpleHMM.getOutputRouteMatchResult();
            List<Pair<Integer, List<String>>> gtRouteMatchResult = MatchResultReader.readRouteMatchResults(groundTruthRouteMatchResultFolder);
            RouteMatchingEvaluation.precisionRecallFScoreAccEvaluation(matchedWaySequence, gtRouteMatchResult, roadMap, null);
        }
    }
}
