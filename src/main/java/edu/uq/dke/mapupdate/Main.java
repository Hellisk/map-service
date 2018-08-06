package edu.uq.dke.mapupdate;

import edu.uq.dke.mapupdate.cooptimization.CoOptimizationFunc;
import edu.uq.dke.mapupdate.evaluation.TrajMatchingEvaluation;
import edu.uq.dke.mapupdate.mapinference.BiagioniKDE2012;
import edu.uq.dke.mapupdate.mapmatching.hmm.NewsonHMM2009;
import edu.uq.dke.mapupdate.mapmerge.MapMerge;
import edu.uq.dke.mapupdate.util.io.*;
import edu.uq.dke.mapupdate.util.object.datastructure.Pair;
import edu.uq.dke.mapupdate.util.object.datastructure.TrajectoryMatchingResult;
import edu.uq.dke.mapupdate.util.object.datastructure.Triplet;
import edu.uq.dke.mapupdate.util.object.roadnetwork.RoadNetworkGraph;
import edu.uq.dke.mapupdate.util.object.roadnetwork.RoadNode;
import edu.uq.dke.mapupdate.util.object.roadnetwork.RoadWay;
import edu.uq.dke.mapupdate.util.object.spatialobject.Trajectory;
import edu.uq.dke.mapupdate.visualisation.UnfoldingMapDisplay;
import org.apache.log4j.BasicConfigurator;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static edu.uq.dke.mapupdate.util.io.DataPreprocessing.dataPreprocessing;
import static edu.uq.dke.mapupdate.util.io.DataPreprocessing.rawMapInitialization;

public class Main {

    // global parameters
    public final static int PERCENTAGE = 0;         // percentage of removed road ways (max = 100)
    private final static int DATASET_OPTION = 0;     // 0 = beijing trajectory, 1 = global trajectory, -1 = map comparison
    private final static int WORKSPACE = 1; // 1 = home, 2 = school, 3 = server
    public final static boolean STATISTIC_MODE = false; // true = test and statistics mode, false = normal process
    public final static int TRAJECTORY_COUNT = -1; // total number of trajectories extracted. -1 = extract all
    public final static int MIN_TRAJ_POINT_COUNT = 5; // the minimal number of point required in a trajectory. -1 = no requirement
    public final static int MAX_TIME_INTERVAL = 120; // the maximum time interval within a trajectory -1 = no requirement

    // after the change of bounding box, the existing trajectories should be deleted manually
    // use the initial map, Beijing-L
//    public final static double[] BOUNDING_BOX = {};
//    // preset the map boundary, smaller san huan, Beijing-M
//    public final static double[] BOUNDING_BOX = {116.32, 116.459, 39.87, 39.95};
    // preset the map boundary, er huan
//    public final static double[] BOUNDING_BOX = {116.35, 116.44, 39.895, 39.95};
    // preset the map boundary, smaller er huan, Beijing-S
    public final static double[] BOUNDING_BOX = {116.4, 116.433773, 39.95, 39.98};

    /* parameters for KDE-based map inference */
    private final static double CELL_SIZE = 1;    // the size of each cell unit, default is 1
    private final static int GAUSSIAN_BLUR = 17;  // Gaussian blur filter, default is 17

    /* parameters for incremental trace merge */
    public final static double AHMED_EPSILON = 0.0020;    // in meter

    /* parameters for HMM-based map matching */
    public final static double SIGMA = 4.07;    // parameter for emission probability calculation, also used to filter unnecessary
    // trajectory points (points whose distance is closer than 2*SIGMA will be removed)
    public final static double BETA = 0.08;    // parameter for transition probability calculation
    // TODO u-turn penalty doesn't apply for global
//    public final static double U_TURN_PENALTY = -5;  // penalty for travelling through u-turn
    private final static int NUM_OF_THREADS = 8;    // number of parallel tasks for map-matching
    public final static int CANDIDATE_RANGE = 50;  // the radius of the candidate generation range in meter
    public final static int GAP_EXTENSION_RANGE = 15;  // the trajectory point will be extended as unmatched point if no candidate is
    // within the circle of this radius in meter
    public final static int RANK_LENGTH = 3;  // the number of top-ranked map-matching results to be stored

    // parameters for map merge
    public final static int MIN_ROAD_LENGTH = 50;     // the minimum length of a new road
    private final static int SUB_TRAJECTORY_MERGE_DISTANCE = 10;   // the maximum allowed distance to attach a end point to an intersection

    // parameters for co-optimization function
    public final static double SCORE_THRESHOLD = 50;   // the value that divide the high and low influence/confidence score

    private final static String BEIJING_HOME_PATH = "F:/data/beijingTrajectory/";         // the root folder of all data
    private final static String BEIJING_SCHOOL_PATH = "C:/data/beijingTrajectory/";       // the root folder of all data
    private final static String BEIJING_SERVER_PATH = "/media/dragon_data/uqpchao/MapUpdate/beijingTrajectory/";       // the root
    // folder of
    // all data
    private final static String GLOBAL_SCHOOL_PATH = "C:/data/evaluationTrajectory/";     // the root folder of all data
    private final static String GLOBAL_HOME_PATH = "F:/data/evaluationTrajectory/";       // the root folder of all data
    private final static String GLOBAL_SERVER_PATH = "/media/dragon_data/uqpchao/MapUpdate/evaluationTrajectory/";       // the root
    // folder of all data
    public final static String ROOT_PATH = DATASET_OPTION <= 0 ? (WORKSPACE == 1 ? BEIJING_HOME_PATH : (WORKSPACE == 2 ?
            BEIJING_SCHOOL_PATH : BEIJING_SERVER_PATH)) : (WORKSPACE == 1 ? GLOBAL_HOME_PATH : (WORKSPACE == 2 ? GLOBAL_SCHOOL_PATH : GLOBAL_SERVER_PATH));

    // different paths in Beijing dataset
    public final static String RAW_MAP = ROOT_PATH + "raw/map/";
    public final static String RAW_TRAJECTORY = ROOT_PATH + "raw/trajectory/";
    public final static String GT_MAP = ROOT_PATH + "groundTruth/map/";  // ground-truth road network
    public final static String GT_MATCHING_RESULT = ROOT_PATH + "groundTruth/matchingResult/TP" + MIN_TRAJ_POINT_COUNT + "_TI" +
            MAX_TIME_INTERVAL + "_TC" + TRAJECTORY_COUNT + "/";   // the map-matched trajectory dataset
    public final static String INPUT_MAP = ROOT_PATH + "input/map/"; // the map with removed roads
    public final static String INPUT_TRAJECTORY = ROOT_PATH + "input/trajectory/TP" + MIN_TRAJ_POINT_COUNT + "_TI" + MAX_TIME_INTERVAL +
            "_TC" + TRAJECTORY_COUNT + "/";    // input trajectory dataset
    public final static String OUTPUT_FOLDER = ROOT_PATH + "output/";
    public final static String CACHE_FOLDER = ROOT_PATH + "cache/";
    public final static String INFERENCE_FOLDER = ROOT_PATH + "mapInference/";

    public static void main(String[] args) throws IOException {

        System.out.println("Co-optimization process start.");
        BasicConfigurator.configure();
        long startTaskTime = System.currentTimeMillis();
        if (DATASET_OPTION == 0) {  // complete algorithm on beijing dataset

            System.out.println("Start working on the beijing dataset...");
            long prevTime = startTaskTime;

//            // process the raw data and convert them into standard format
//            System.out.println("Initializing the entire Beijing road map... This step is not required unless the raw data is changed.");
//            rawMapInitialization(false);
//            System.out.print("Initialization done. ");
//
//            // pre-processing the data so that the map and the trajectories are filtered
//            System.out.println("Start the data preprocessing step, including map resizing, trajectory filtering and map manipulation...");
//            dataPreprocessing(false);
//            System.out.println("Initialisation done in " + (System.currentTimeMillis() - prevTime) / 1000 + " seconds, start the " +
//                    "map-matching process.");
//            prevTime = System.currentTimeMillis();

            // initialization step, read the input map first
            CSVMapReader csvMapReader = new CSVMapReader(INPUT_MAP);
            RoadNetworkGraph initialMap = csvMapReader.readMap(PERCENTAGE, -1, false);
            List<RoadWay> removedWayList = csvMapReader.readRemovedEdges(PERCENTAGE, -1);

            // read input trajectories
            CSVTrajectoryReader csvTrajectoryReader = new CSVTrajectoryReader();
            List<Trajectory> rawTrajectoryList = csvTrajectoryReader.readTrajectoryFilesList(INPUT_TRAJECTORY);

            // step 0: map-matching process, start the initial map-matching
//            Stream<TrajectoryMatchingResult> trajMatchingResultStream = parallelMapMatchingBeijing();
            Triplet<List<TrajectoryMatchingResult>, RoadNetworkGraph, List<Trajectory>> initialMatchingResultTriplet = mapMatchingBeijing
                    (rawTrajectoryList, initialMap, 0, false);
            System.out.println("Initial map matching finished, time elapsed:" + (System.currentTimeMillis() - prevTime) / 1000 +
                    " seconds");
            prevTime = System.currentTimeMillis();

            // evaluation: initial map matching evaluation
            CSVTrajectoryReader groundTruthMatchingResultReader = new CSVTrajectoryReader();
            List<Pair<Integer, List<String>>> gtMatchingResult = groundTruthMatchingResultReader.readGroundTruthMatchingResult
                    (GT_MATCHING_RESULT);
            TrajMatchingEvaluation trajMatchingEvaluation = new TrajMatchingEvaluation();
//            List<TrajectoryMatchingResult> trajMatchingResultList = trajMatchingResultStream.collect(Collectors.toList());
//            List<TrajectoryMatchingResult> trajMatchResultList = groundTruthMatchingResultReader.readMatchedResult(OUTPUT_FOLDER);
            trajMatchingEvaluation.beijingPrecisionRecallCalc(initialMatchingResultTriplet._1(), gtMatchingResult, initialMap, removedWayList);

//            int iteration = 1;  // start the iteration
//            double iterationBenefitGain = 0;
//            while (iteration < 2) { // TODO replace it by cost function
//
//                System.out.println("Start the " + iteration + " round of iteration.");
//                long currIterationStartTime = System.currentTimeMillis();
//
//                // step 1: map inference
//                BiagioniKDE2012 mapInference = new BiagioniKDE2012(CELL_SIZE, GAUSSIAN_BLUR);
//                String localDir = System.getProperty("user.dir");
//                mapInference.KDEMapInferenceProcess(localDir + "/src/main/python/", CACHE_FOLDER + "unmatchedNextInput/TP" +
//                        MIN_TRAJ_POINT_COUNT + "_TI" + MAX_TIME_INTERVAL + "_TC" + TRAJECTORY_COUNT + "/" + (iteration - 1) + "/");
//                System.out.println("Map inference finished, time elapsed: " + (System.currentTimeMillis() - prevTime) / 1000 + " seconds");
//
//                // step 2: map merge
//                CSVMapReader inferenceEdgeReader = new CSVMapReader(INFERENCE_FOLDER);
//                List<RoadWay> inferredEdges = inferenceEdgeReader.readInferredEdges();
//                if (inferredEdges.size() == 0) {
//                    System.out.println("ERROR! Current iteration does not have new road inferred.");
//                    break;
//                }
//                RoadNetworkGraph prevMap;
//                if (iteration == 1) {
//                    CSVMapReader prevMapReader = new CSVMapReader(INPUT_MAP);
//                    prevMap = prevMapReader.readMap(PERCENTAGE, -1, false);
//                } else {
//                    CSVMapReader prevMapReader = new CSVMapReader(CACHE_FOLDER);
//                    prevMap = prevMapReader.readMap(PERCENTAGE, iteration - 1, false);
//                }
//                MapMerge spMapMerge = new MapMerge(prevMap, inferredEdges, removedWayList, CANDIDATE_RANGE, SUB_TRAJECTORY_MERGE_DISTANCE);
//                RoadNetworkGraph mergedMap = spMapMerge.nearestNeighbourMapMerge();
//                CSVMapWriter updatedMapWriter = new CSVMapWriter(mergedMap, CACHE_FOLDER);
//                updatedMapWriter.writeMap(PERCENTAGE, iteration, true);
//
//                // step 3: map-matching process on updated map
//                CSVMapReader updatedMapReader = new CSVMapReader(CACHE_FOLDER);
//                RoadNetworkGraph updatedMap = updatedMapReader.readMap(PERCENTAGE, iteration, true);
//
////                Stream<TrajectoryMatchingResult> trajMatchingResultStream = parallelMapMatchingBeijing();
//                Triplet<List<TrajectoryMatchingResult>, RoadNetworkGraph, List<Trajectory>> matchingResultTriplet = mapMatchingBeijing
//                        (rawTrajectoryList, updatedMap, iteration, false);
//                System.out.println("Map matching finished, time elapsed: " + (System.currentTimeMillis() - prevTime) / 1000 + " seconds");
//                prevTime = System.currentTimeMillis();
//
//                // step 4: co-optimization model
//                CoOptimizationFunc coOptimizationFunc = new CoOptimizationFunc();
//                Triplet<RoadNetworkGraph, List<Trajectory>, Double> refinementResult = coOptimizationFunc.costFunctionCal
//                        (matchingResultTriplet);
//                Triplet<List<TrajectoryMatchingResult>, RoadNetworkGraph, List<Trajectory>> refinedMatchingResult = mapMatchingBeijing
//                        (refinementResult._2(), refinementResult._1(), iteration, true);
//                // write refinement result
//                CSVMapWriter refinedMapWriter = new CSVMapWriter(refinementResult._1(), CACHE_FOLDER);
//                refinedMapWriter.writeMap(PERCENTAGE, iteration, false);
//                CSVTrajectoryWriter mergedMatchingResultWriter = new CSVTrajectoryWriter(CACHE_FOLDER);
//                mergedMatchingResultWriter.writeMergedMatchedTrajectory(matchingResultTriplet._1(), refinedMatchingResult._1(),
//                        RANK_LENGTH, iteration);
//                mergedMatchingResultWriter.writeMergedUnmatchedTrajectory(matchingResultTriplet._3(), refinedMatchingResult._3(), iteration);
//                iterationBenefitGain = refinementResult._3();
//                System.out.println("Result refinement finished, the benefit gain is " + iterationBenefitGain + ", time elapsed: " +
//                        (System.currentTimeMillis() - prevTime) / 1000 + " seconds.");
//                System.out.println("Finish the " + iteration + " round of iteration, total time elapsed: " + (System.currentTimeMillis()
//                        - currIterationStartTime) / 1000 + " seconds.");
//                prevTime = System.currentTimeMillis();
//                iteration++;
//            }
//
//            // finish the iterations and write the final output
//            CSVMapReader finalMapReader = new CSVMapReader(CACHE_FOLDER);
//            RoadNetworkGraph finalMap = finalMapReader.readMap(PERCENTAGE, iteration - 1, false);
//            CSVMapWriter finalMapWriter = new CSVMapWriter(finalMap, OUTPUT_FOLDER + "map/");
//            finalMapWriter.writeMap(PERCENTAGE, -1, false);
//            CSVTrajectoryReader finalMatchingResultReader = new CSVTrajectoryReader();
//            List<TrajectoryMatchingResult> finalMatchingResult = finalMatchingResultReader.readMatchedResult(CACHE_FOLDER, iteration - 1);
//            CSVTrajectoryWriter finalMatchingResultWriter = new CSVTrajectoryWriter(OUTPUT_FOLDER);
//            finalMatchingResultWriter.writeMatchedTrajectory(finalMatchingResult, RANK_LENGTH, -1);

            System.out.println("Iterative process done. Total running time: " + (System.currentTimeMillis() - startTaskTime) / 1000 + " " +
                    "seconds");
        } else if (DATASET_OPTION == 1) {    // map-matching evaluation dataset

            // use global dataset to evaluate the map-matching accuracy
            System.out.println("Start working on the global dataset");

            // map-matching process
            XMLTrajectoryReader reader = new XMLTrajectoryReader(ROOT_PATH + "input/");
            List<TrajectoryMatchingResult> trajectoryMatchingResults = mapMatchingGlobal(reader);

            System.out.println("Map matching finished, total time spent:" + (System.currentTimeMillis() - startTaskTime) / 1000 + "seconds");

            // evaluation: map matching evaluation
            List<Pair<Integer, List<String>>> groundTruthMatchingResult = new ArrayList<>();
            List<Map<String, String>> groundTruthTrajectoryInfo = new ArrayList<>();
            for (int i = 0; i < reader.getNumOfTrajectory(); i++) {
                List<String> matchResult = reader.readGroundTruthMatchResult(i);

                Pair<Integer, List<String>> currGroundTruthMatchResult = new Pair<>(i, matchResult);
                groundTruthMatchingResult.add(currGroundTruthMatchResult);
                groundTruthTrajectoryInfo.add(reader.findTrajectoryInfo(i));
            }
//            CSVTrajectoryReader groundTruthMatchingResultReader = new CSVTrajectoryReader();
//            List<TrajectoryMatchingResult> trajectoryMatchingResults = groundTruthMatchingResultReader.readMatchedResult(OUTPUT_FOLDER, -1);
            TrajMatchingEvaluation trajMatchingEvaluation = new TrajMatchingEvaluation();
            trajMatchingEvaluation.globalPrecisionRecallCalc(trajectoryMatchingResults, groundTruthMatchingResult, groundTruthTrajectoryInfo);

        } else {    // other test cases
            System.out.println("Start comparing different versions of Beijing map...");

            RawMapReader newMapReader = new RawMapReader(RAW_MAP, BOUNDING_BOX);
            System.out.println("Start reading the new and old Beijing maps.");
            RoadNetworkGraph newBeijingMap = newMapReader.readNewBeijingMap();
            CSVMapWriter beijingMapWriter = new CSVMapWriter(newBeijingMap, GT_MAP);
            beijingMapWriter.writeMap(0, -1, false);
            RawMapReader oldMapReader = new RawMapReader(RAW_MAP, BOUNDING_BOX);
            RoadNetworkGraph oldBeijingMap = oldMapReader.readOldBeijingMap();

            Set<String> nodeSet = new HashSet<>();
            int matchCount = 0;
            for (RoadNode n : newBeijingMap.getNodes()) {
                nodeSet.add(n.lon() + "_" + n.lat());
            }
            for (RoadNode n : oldBeijingMap.getNodes()) {
                if (nodeSet.contains(n.lon() + "_" + n.lat()))
                    matchCount++;
            }
            System.out.println("Total matched node percentage: " + matchCount / (double) newBeijingMap.getNodes().size());
            // visualization
            UnfoldingMapDisplay graphDisplay = new UnfoldingMapDisplay();
            graphDisplay.display();
        }

//        // evaluation: map inference evaluation
//        String removedEdgesPath = initialMap + CITY_NAME + "_removed_edges.txt";
//        CSVMapReader removedEdgeReader = new CSVMapReader("", removedEdgesPath);
//        RoadNetworkGraph removedGraph = removedEdgeReader.readMapEdges();
//        MapMatchingEvaluation mapMatchingEvaluation = new MapMatchingEvaluation(10);
//        mapMatchingEvaluation.precisionRecallEval(inferenceGraph, removedGraph, initialMap);
//
        System.out.println("Task finish, total time spent:" + (System.currentTimeMillis() - startTaskTime) / 1000 + " seconds");
    }

    /**
     * The main entry of map-matching algorithm for Beijing dataset
     *
     * @return map-matched trajectory result
     * @throws IOException file reading error
     */
    private static Triplet<List<TrajectoryMatchingResult>, RoadNetworkGraph, List<Trajectory>> mapMatchingBeijing
    (List<Trajectory> rawTrajectoryList, RoadNetworkGraph roadMap, int iteration, boolean refinementStep) throws IOException {

        // start matching process
        NewsonHMM2009 mapMatching = new NewsonHMM2009(CANDIDATE_RANGE, GAP_EXTENSION_RANGE, RANK_LENGTH);
        List<TrajectoryMatchingResult> currMatchingResultList = mapMatching.trajectoryListMatchingProcess(rawTrajectoryList, roadMap);
        List<Trajectory> unmatchedTraj;
        unmatchedTraj = mapMatching.getUnmatchedTraj();
        if (!refinementStep) {
            if (iteration != 0) {     // start processing the co-optimization model
                CSVTrajectoryReader csvMatchedTrajectoryReader = new CSVTrajectoryReader();
                List<TrajectoryMatchingResult> prevMatchingResult = csvMatchedTrajectoryReader.readMatchedResult(CACHE_FOLDER, iteration - 1);
                Map<String, TrajectoryMatchingResult> id2PrevMatchingResult = new HashMap<>();
                for (TrajectoryMatchingResult mr : prevMatchingResult) {
                    if (!id2PrevMatchingResult.containsKey(mr.getTrajID()))
                        id2PrevMatchingResult.put(mr.getTrajID(), mr);
                    else System.out.println("ERROR! The same trajectory matching result occurred twice: " + mr.getTrajID());
                }
                CoOptimizationFunc optimizationFunc = new CoOptimizationFunc();
                RoadNetworkGraph updatedMap = optimizationFunc.influenceScoreGen(currMatchingResultList, id2PrevMatchingResult, roadMap);
                return new Triplet<>(currMatchingResultList, updatedMap, unmatchedTraj);
            } else {
                // initial map-matching step, write output matching result
                CSVTrajectoryWriter matchingResultWriter = new CSVTrajectoryWriter(CACHE_FOLDER);
                matchingResultWriter.writeMatchedTrajectory(currMatchingResultList, RANK_LENGTH, iteration);
                CSVTrajectoryWriter unmatchedTrajWriter = new CSVTrajectoryWriter(CACHE_FOLDER);
                unmatchedTrajWriter.writeUnmatchedTrajectory(unmatchedTraj, iteration);
                return new Triplet<>(currMatchingResultList, roadMap, unmatchedTraj);
            }
        } else {
            return new Triplet<>(currMatchingResultList, roadMap, unmatchedTraj);
        }
    }

    /**
     * The main entry of map-matching algorithm for Beijing dataset
     *
     * @return map-matched trajectory result
     * @throws IOException file reading error
     */
    private static Stream<TrajectoryMatchingResult> parallelMapMatchingBeijing(int iteration) throws IOException, ExecutionException,
            InterruptedException {
        // read input map and trajectories
        CSVMapReader csvMapReader = new CSVMapReader(INPUT_MAP);
        RoadNetworkGraph initialMap = csvMapReader.readMap(PERCENTAGE, iteration, false);
        CSVTrajectoryReader csvTrajectoryReader = new CSVTrajectoryReader();
        Stream<Trajectory> initialTrajectoryList = csvTrajectoryReader.readTrajectoryFilesStream(INPUT_TRAJECTORY);

        // start matching process
        NewsonHMM2009 mapMatching = new NewsonHMM2009(CANDIDATE_RANGE, GAP_EXTENSION_RANGE, RANK_LENGTH);
        Stream<TrajectoryMatchingResult> trajMatchResults = mapMatching.trajectoryStreamMatchingProcess(initialTrajectoryList, initialMap,
                NUM_OF_THREADS);
        List<Trajectory> unmatchedTraj;
        unmatchedTraj = mapMatching.getUnmatchedTraj();

        // write output matching result
        CSVTrajectoryWriter matchingResultWriter = new CSVTrajectoryWriter(OUTPUT_FOLDER);
        List<TrajectoryMatchingResult> trajMatchResultList = trajMatchResults.collect(Collectors.toList());
        matchingResultWriter.writeMatchedTrajectory(trajMatchResultList, RANK_LENGTH, iteration);
        CSVTrajectoryWriter unmatchedTrajWriter = new CSVTrajectoryWriter(OUTPUT_FOLDER);
        unmatchedTrajWriter.writeUnmatchedTrajectory(unmatchedTraj, iteration);
        return trajMatchResults;
    }

    /**
     * The main entry of map-matching algorithm for global dataset
     *
     * @return map-matched trajectory result
     * @throws IOException file reading error
     */
    private static List<TrajectoryMatchingResult> mapMatchingGlobal(XMLTrajectoryReader reader) throws IOException {
        CSVRawMapReader mapReader = new CSVRawMapReader(ROOT_PATH + "input/");
        List<TrajectoryMatchingResult> results = new ArrayList<>();
        for (int i = 0; i < reader.getNumOfTrajectory(); i++) {
            Trajectory currTraj = reader.readInputTrajectory(i);
            RoadNetworkGraph currMap = mapReader.readRawMap(i);
            TrajectoryMatchingResult matchResult = startGlobalTrajectoryMatching(currTraj, currMap);
            results.add(matchResult);
        }
        CSVTrajectoryWriter matchingResultWriter = new CSVTrajectoryWriter(OUTPUT_FOLDER);
        matchingResultWriter.writeMatchedTrajectory(results, RANK_LENGTH, -1);
        return results;
    }

    /**
     * Perform trajectory map-matching and write the result individually, which is used in global dataset map-matching evaluation
     *
     * @param trajectory input trajectory
     * @param roadMap    underline road network
     * @return map-matching result for trajectory
     */
    private static TrajectoryMatchingResult startGlobalTrajectoryMatching(Trajectory trajectory, RoadNetworkGraph roadMap) {
        NewsonHMM2009 mapMatching = new NewsonHMM2009(CANDIDATE_RANGE, GAP_EXTENSION_RANGE, RANK_LENGTH);
        return mapMatching.trajectoryMatchingProcess(trajectory, roadMap);
    }
}