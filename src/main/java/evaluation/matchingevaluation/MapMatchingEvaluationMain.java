package evaluation.matchingevaluation;

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
import util.object.structure.PointMatch;
import util.object.structure.SimpleTrajectoryMatchResult;
import util.settings.MapMatchingProperty;
import util.settings.MapServiceLogger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author uqpchao
 * Created 11/06/2019
 */
public class MapMatchingEvaluationMain {

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
        String gtRouteMatchResultFolder = property.getPropertyString("path.GroundTruthRouteMatchResultFolder");
        String gtPointMatchResultFolder = property.getPropertyString("path.GroundTruthPointMatchResultFolder");
        String matchingMethod = property.getPropertyString("algorithm.mapmatching.MatchingMethod");
        String dataSpec = property.getPropertyString("data.DataSpec");
        double candidateRange = property.getPropertyDouble("algorithm.mapmatching.CandidateRange");
        // log file name
        String parameters = "";
        switch (matchingMethod.substring(3)) {
            case "HMM-goh":
                parameters = property.getPropertyString("algorithm.mapmatching.WindowSize");
                break;
            case "HMM-eddy":
                parameters = property.getPropertyString("algorithm.mapmatching.hmm.Eddy.Gamma");
                break;
            case "HMM-fixed":
                parameters = property.getPropertyString("algorithm.mapmatching.WindowSize");
                break;
            case "FST":
                parameters = property.getPropertyString("algorithm.mapmatching.CandidateRange") + "_"
                        + property.getPropertyString("algorithm.mapmatching.Sigma") + "_"
                        + property.getPropertyString("algorithm.mapmatching.fst.CandidateSize") + "_"
                        + property.getPropertyString("algorithm.mapmatching.fst.Omega");
                break;
            case "WGT":
                parameters = property.getPropertyString("algorithm.mapmatching.CandidateRange") + "_"
                        + property.getPropertyString("algorithm.mapmatching.Sigma");
                break;
            default:
                parameters = "null";
                break;
        }
        String logFileName = "evaluation_" + dataSet + "_" + matchingMethod + "_" + dataSpec + "_" + parameters + "_" +
                initTaskTime;
        DistanceFunction distFunc;

        if (dataSet.contains("Beijing"))
            distFunc = new GreatCircleDistanceFunction();
        else
            distFunc = new EuclideanDistanceFunction();

        // initialize log file
        MapServiceLogger.logInit(logFolder, logFileName);

        // use global dataset to evaluate the map-matching accuracy
        final Logger LOG = Logger.getLogger(MapMatchingEvaluationMain.class);

        if (dataSet.equals("Global")) {
            long startTaskTime = System.currentTimeMillis();    // the start of the map-matching process
            int samplingInterval = property.getPropertyInteger("data.global.SamplingInterval");
            LOG.info("Precision-recall map-matching evaluation of the " + matchingMethod + " method on " + dataSet + " dataset with input" +
                    " sampling interval: " + samplingInterval);
            String rawDataFolder = property.getPropertyString("path.RawDataFolder");
            List<SimpleTrajectoryMatchResult> outputMatchResult =
                    MatchResultReader.readSimpleMatchResultsToList(outputMatchResultFolder.substring(0,
                            outputMatchResultFolder.length() - 1), distFunc);
            Map<Integer, List<String>> id2OutputRouteMatchMapping = new HashMap<>();
            for (SimpleTrajectoryMatchResult matchResult : outputMatchResult) {
                id2OutputRouteMatchMapping.put(Integer.parseInt(matchResult.getTrajID()), matchResult.getRouteMatchResultList());
            }
            String precisionRecall =
                    "Precision/recall/f-score: " + RouteMatchingEvaluation.globalPrecisionRecallEvaluation(id2OutputRouteMatchMapping,
                            rawDataFolder);

            LOG.info("Precision-recall map-matching finished, total time cost: " + (System.currentTimeMillis() - startTaskTime));
            LOG.info("Evaluation results for " + matchingMethod + "_" + dataSet);
            LOG.info(precisionRecall);
        } else {
            // evaluation step, read the output and ground-truth dataset
            RoadNetworkGraph inputMap = MapReader.readMap(inputMapFolder + "0.txt", false, distFunc);
            List<Trajectory> inputTraj = TrajectoryReader.readTrajectoriesToList(inputTrajFolder, distFunc);
            List<SimpleTrajectoryMatchResult> outputMatchResult = MatchResultReader.readSimpleMatchResultsToList(outputMatchResultFolder,
                    distFunc);
            List<Pair<Integer, List<String>>> gtRouteMatchResult = MatchResultReader.readRouteMatchResults(gtRouteMatchResultFolder);
            List<Pair<Integer, List<PointMatch>>> gtPointMatchResult = MatchResultReader.readPointMatchResults(gtPointMatchResultFolder,
                    distFunc);
            long startTaskTime = System.currentTimeMillis();    // the start of the map-matching process

            if (outputMatchResult.size() == 0)
                throw new IllegalArgumentException("The output match result set is empty.");
            List<String> evaluationResultList = new ArrayList<>();
            if (!outputMatchResult.get(0).getRouteMatchResultList().isEmpty()) {
                List<Pair<Integer, List<String>>> routeMatchResult = new ArrayList<>();
                Map<String, Double> id2RoadLength = new HashMap<>();
                for (RoadWay w : inputMap.getWays())
                    id2RoadLength.put(w.getID(), w.getLength());

                LOG.info("Start route match result evaluation for " + matchingMethod + " method on " + dataSet + " dataset with input: " + dataSpec);

                // collect all route match results
                for (SimpleTrajectoryMatchResult matchResult : outputMatchResult) {
                    routeMatchResult.add(new Pair<>(Integer.parseInt(matchResult.getTrajID()), matchResult.getRouteMatchResultList()));
                }
                long evaluationTime = System.currentTimeMillis();
                String precisionRecallFScoreAcc =
                        "Precision/recall/f-score/acc: " + RouteMatchingEvaluation.precisionRecallFScoreAccEvaluation(routeMatchResult,
                                gtRouteMatchResult, id2RoadLength, null);
                LOG.info("Precision/recall/f-score/acc evaluation finish, total time cost: " + (System.currentTimeMillis() - evaluationTime) / 1000.0 + "s.");
                evaluationResultList.add(precisionRecallFScoreAcc);
                evaluationTime = System.currentTimeMillis();
                String rmf = "RMF: " + RouteMatchingEvaluation.rmfEvaluation(routeMatchResult, gtRouteMatchResult, id2RoadLength);
                LOG.info("Route Match Fraction (RMF) evaluation finish, total time cost: " + (System.currentTimeMillis() - evaluationTime) / 1000.0 + "s.");
                evaluationResultList.add(rmf);
                evaluationTime = System.currentTimeMillis();

//                String nonGT = "Measure without GT: " + RouteMatchingEvaluation.nonGTEvaluation(routeMatchResult, inputTraj, id2RoadLength);
//                LOG.info("Measure without GT evaluation finish, total time cost: " + (System.currentTimeMillis() - evaluationTime) / 1000.0 + "s.");
//                evaluationResultList.add(nonGT);
            }

            if (!outputMatchResult.get(0).getPointMatchResultList().isEmpty()) {
                List<Pair<Integer, List<PointMatch>>> pointMatchResult = new ArrayList<>();
                LOG.info("Start point match result evaluation for " + matchingMethod + " method on " + dataSet + " dataset with input: " + dataSpec);

                // collect all point match results
                for (SimpleTrajectoryMatchResult matchResult : outputMatchResult) {
                    pointMatchResult.add(new Pair<>(Integer.parseInt(matchResult.getTrajID()), matchResult.getPointMatchResultList()));
                }
                long evaluationTime = System.currentTimeMillis();

                String accuracy = "Accuracy: " + PointMatchingEvaluation.accuracyEvaluation(pointMatchResult, gtPointMatchResult);
                LOG.info("Accuracy evaluation finish, total time cost: " + (System.currentTimeMillis() - evaluationTime) / 1000.0 + "s.");
                evaluationResultList.add(accuracy);

                evaluationTime = System.currentTimeMillis();
                String rmse = "Root Mean Square Error: " + PointMatchingEvaluation.rootMeanSquareErrorEvaluation(pointMatchResult,
                        gtPointMatchResult, candidateRange * 2);
                LOG.info("Root Mean Square Error (RMSE) evaluation finish, total time cost: " + (System.currentTimeMillis() - evaluationTime) / 1000.0 + "s.");
                evaluationResultList.add(rmse);
            }

            LOG.info("Evaluation finish, total time cost: " + (System.currentTimeMillis() - startTaskTime) / 1000.0 + "s.");
            LOG.info("Evaluation results for " + matchingMethod + "_" + dataSet + "_" + dataSpec);
            for (String s : evaluationResultList) {
                LOG.info(s);
            }
        }
    }
}
