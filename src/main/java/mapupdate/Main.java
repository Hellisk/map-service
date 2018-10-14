package mapupdate;

import mapupdate.cooptimization.CoOptimizationFunc;
import mapupdate.evaluation.ResultEvaluation;
import mapupdate.mapinference.BiagioniKDE2012;
import mapupdate.mapmatching.hmm.NewsonHMM2009;
import mapupdate.mapmerge.MapMerge;
import mapupdate.util.io.*;
import mapupdate.util.object.datastructure.Pair;
import mapupdate.util.object.datastructure.TrajectoryMatchingResult;
import mapupdate.util.object.datastructure.Triplet;
import mapupdate.util.object.roadnetwork.RoadNetworkGraph;
import mapupdate.util.object.roadnetwork.RoadNode;
import mapupdate.util.object.roadnetwork.RoadWay;
import mapupdate.util.object.spatialobject.Trajectory;
import mapupdate.visualisation.UnfoldingMapDisplay;
import org.apache.log4j.BasicConfigurator;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static mapupdate.util.io.DataPreprocessing.dataPreprocessing;
import static mapupdate.util.io.DataPreprocessing.rawMapInitialization;

public class Main {

    /* environmental settings */
    private final static int DATASET_OPTION = 0;     // 0 = beijing trajectory, 1 = global trajectory, -1 = map comparison
    private final static int BB_OPTION = 1;          // 1 = Beijing-S, 2 = Beijing-M, 3 = Beijing-L
    public final static int WORKSPACE = 1; // 1 = home, 2 = school, 3 = server
    public final static int MIN_TRAJ_POINT_COUNT = 5; // the minimal number of point required in a trajectory. -1 = no requirement
    public final static int MAX_TIME_INTERVAL = 120; // the maximum time interval within a trajectory -1 = no requirement

    /* tunable parameters */
    public final static int PERCENTAGE = 2;         // percentage of removed road ways (max = 100)
    public final static int GAP_EXTENSION_RANGE = 20;  // the trajectory point will be extended as unmatched point if no candidate is
    public final static int RANK_LENGTH = 3;  // the number of top-ranked map-matching results to be stored
    public final static double SCORE_THRESHOLD = 20;   // the value that divide the high and low influence/confidence score
    public final static int TRAJECTORY_COUNT = -1; // total number of trajectories extracted. -1 = extract all

    /* fixed settings */
    /* bounding boxes */
    private final static double[] BB_BEIJING_S = {116.4, 116.435, 39.95, 39.98};    // preset the map boundary, N-E outside 2 ring, Beijing-S
    private final static double[] BB_BEIJING_M = {116.34, 116.44, 39.89, 39.95};    // preset the map boundary, er huan, Beijing-M
    private final static double[] BB_BEIJING_L = {};    // use the initial map, Beijing-L
    public final static double[] BOUNDING_BOX = BB_OPTION == 1 ? BB_BEIJING_S : (BB_OPTION == 2 ? BB_BEIJING_M : BB_BEIJING_L);
    /* parameters for KDE-based map inference */
    private final static int CELL_SIZE = 1;    // the size of each cell unit, default is 1
    private final static int GAUSSIAN_BLUR = 17;  // Gaussian blur filter, default is 17
    /* parameters for HMM-based map matching */
    public final static int CANDIDATE_RANGE = 60;  // the radius of the candidate generation range in meter
    public final static double SIGMA = 2;    // parameter for emission probability calculation, also used to filter unnecessary
    // trajectory points (points whose distance is closer than 2*SIGMA will be removed)
    public final static double BETA = 0.3;    // parameter for transition probability calculation
    /* parameters for map merge */
    public final static int MIN_ROAD_LENGTH = 30;     // the minimum length of a new road
    private final static int SUB_TRAJECTORY_MERGE_DISTANCE = 15;   // the maximum allowed distance to attach a end point to an intersection

    /* settings of folders */
    private final static String BEIJING_HOME_PATH = "F:/data/beijingTrajectory/";         // the root folder of all data
    private final static String BEIJING_SCHOOL_PATH = "C:/data/beijingTrajectory/";       // the root folder of all data
    private final static String BEIJING_SERVER_PATH = "/media/dragon_data/uqpchao/MapUpdate/beijingTrajectory/";       // the root
    // folder of all data
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
    public final static String GT_MATCHING_RESULT =
            ROOT_PATH + "groundTruth/matchingResult/M" + BB_OPTION + "_TP" + MIN_TRAJ_POINT_COUNT + "_TI" + MAX_TIME_INTERVAL + "_TC" + TRAJECTORY_COUNT + "/";   // the map-matched trajectory dataset
    public final static String INPUT_MAP = ROOT_PATH + "input/map/"; // the map with removed roads
    public final static String INPUT_TRAJECTORY =
            ROOT_PATH + "input/trajectory/M" + BB_OPTION + "_TP" + MIN_TRAJ_POINT_COUNT + "_TI" + MAX_TIME_INTERVAL + "_TC" + TRAJECTORY_COUNT + "/";    // input trajectory dataset
    public final static String OUTPUT_FOLDER = ROOT_PATH + "output/";
    public final static String CACHE_FOLDER = ROOT_PATH + "cache/";
    public final static String INFERENCE_FOLDER = WORKSPACE == 3 ? "/home/uqpchao/data/mapInference/" : ROOT_PATH + "mapInference/";

    public static void main(String[] args) throws IOException, ExecutionException, InterruptedException {

        BasicConfigurator.configure();
        long startTaskTime = System.currentTimeMillis();
        if (DATASET_OPTION == 0) {  // complete algorithm on beijing dataset

//            System.out.println("Start working on the beijing dataset...");
            long prevTime = startTaskTime;

//            // process the raw data and convert them into standard format
//            System.out.println("Initializing the entire Beijing road map... This step is not required unless the raw data is changed.");
            rawMapInitialization();
//            System.out.print("Initialization done.");

            // pre-processing the data so that the map and the trajectories are filtered
//            System.out.println("Start the data preprocessing step, including map resizing, trajectory filtering and map manipulation...");
            dataPreprocessing(false);
//            System.out.println("Initialisation done in " + (System.currentTimeMillis() - prevTime) / 1000 + " seconds, start the " +
//                    "map-matching process.");
            prevTime = System.currentTimeMillis();

            // initialization step, read the input first
            CSVMapReader csvMapReader = new CSVMapReader(INPUT_MAP);
            RoadNetworkGraph initialMap = csvMapReader.readMap(PERCENTAGE, -1, false);
            List<RoadWay> removedWayList = csvMapReader.readRemovedEdges(PERCENTAGE, -1);
            CSVTrajectoryReader csvTrajectoryReader = new CSVTrajectoryReader();    // read input trajectories
            Stream<Trajectory> initialTrajectoryStream = csvTrajectoryReader.readTrajectoryFilesStream(INPUT_TRAJECTORY);
//            List<Trajectory> rawTrajectoryList = csvTrajectoryReader.readTrajectoryFilesList(INPUT_TRAJECTORY);
            CSVTrajectoryReader groundTruthMatchingResultReader = new CSVTrajectoryReader();    // read ground-truth map-matching result
            List<Pair<Integer, List<String>>> gtMatchingResult = groundTruthMatchingResultReader.readGroundTruthMatchingResult(GT_MATCHING_RESULT);

            // step 0: map-matching process, start the initial map-matching
            Triplet<List<TrajectoryMatchingResult>, RoadNetworkGraph, List<Trajectory>> initialMatchingResultTriplet =
                    parallelMapMatchingBeijing(initialTrajectoryStream, initialMap, 0, false);
//            Triplet<List<TrajectoryMatchingResult>, RoadNetworkGraph, List<Trajectory>> initialMatchingResultTriplet = mapMatchingBeijing
//                    (rawTrajectoryList, initialMap, 0, false);
            System.out.println("Initial map matching finished, time elapsed:" + (System.currentTimeMillis() - prevTime) / 1000 +
                    " seconds");
            prevTime = System.currentTimeMillis();
            startTaskTime = prevTime;
            int matchingTime = 0;
            int updateTime = 0;

            // evaluation: initial map matching evaluation
            ResultEvaluation resultEvaluation = new ResultEvaluation();
            resultEvaluation.beijingMapMatchingEval(initialMatchingResultTriplet._1(), gtMatchingResult, initialMap, removedWayList);

            if (PERCENTAGE != 0) {
                int iteration = 1;  // start the iteration
                double costFunc = 0;
                while (costFunc >= 0) {

                    System.out.println("Start the " + iteration + " round of iteration.");
                    long currIterationStartTime = System.currentTimeMillis();

                    // step 1: map inference
                    BiagioniKDE2012 mapInference = new BiagioniKDE2012(CELL_SIZE, GAUSSIAN_BLUR);
                    String localDir = System.getProperty("user.dir");
                    mapInference.KDEMapInferenceProcess(localDir + "/src/main/python/", CACHE_FOLDER + "unmatchedNextInput/TP" +
                            MIN_TRAJ_POINT_COUNT + "_TI" + MAX_TIME_INTERVAL + "_TC" + TRAJECTORY_COUNT + "/" + (iteration - 1) + "/");
                    System.out.println("Map inference finished, time elapsed: " + (System.currentTimeMillis() - prevTime) / 1000 + " seconds");
                    updateTime += (System.currentTimeMillis() - prevTime) / 1000;
                    prevTime = System.currentTimeMillis();

                    // step 2: map merge
                    CSVMapReader inferenceEdgeReader = new CSVMapReader(INFERENCE_FOLDER);
                    List<RoadWay> inferredEdges = inferenceEdgeReader.readInferredEdges();
                    if (inferredEdges.size() == 0) {
                        System.out.println("Current iteration does not have new road inferred. Finish the iteration.");
                        break;
                    }
                    RoadNetworkGraph prevMap;
                    if (iteration == 1) {
                        CSVMapReader prevMapReader = new CSVMapReader(INPUT_MAP);
                        prevMap = prevMapReader.readMap(PERCENTAGE, -1, false);
                    } else {
                        CSVMapReader prevMapReader = new CSVMapReader(CACHE_FOLDER);
                        prevMap = prevMapReader.readMap(PERCENTAGE, iteration - 1, false);
                    }
                    MapMerge spMapMerge = new MapMerge(prevMap, inferredEdges, removedWayList, CANDIDATE_RANGE, SUB_TRAJECTORY_MERGE_DISTANCE);
                    Pair<RoadNetworkGraph, Boolean> mergedMapResult = spMapMerge.nearestNeighbourMapMerge();
                    if (mergedMapResult._2()) {
                        System.out.println("Current iteration does not have new road added. Finish the iteration.");
                        break;
                    }
                    CSVMapWriter updatedMapWriter = new CSVMapWriter(mergedMapResult._1(), CACHE_FOLDER);
                    updatedMapWriter.writeMap(PERCENTAGE, iteration, true);

                    // step 3: map-matching process on updated map
                    CSVMapReader updatedMapReader = new CSVMapReader(CACHE_FOLDER);
                    RoadNetworkGraph updatedMap = updatedMapReader.readMap(PERCENTAGE, iteration, true);
                    resultEvaluation.beijingMapUpdateEval(updatedMap, removedWayList, initialMap);

                    Stream<Trajectory> inputTrajectoryStream = csvTrajectoryReader.readTrajectoryFilesStream(INPUT_TRAJECTORY);
                    Triplet<List<TrajectoryMatchingResult>, RoadNetworkGraph, List<Trajectory>> matchingResultTriplet =
                            parallelMapMatchingBeijing(inputTrajectoryStream, updatedMap, iteration, false);
                    if (iteration == 1)
                        resultEvaluation.beijingMapMatchingEval(matchingResultTriplet._1(), gtMatchingResult, updatedMap,
                                removedWayList);

//                Triplet<List<TrajectoryMatchingResult>, RoadNetworkGraph, List<Trajectory>> matchingResultTriplet = mapMatchingBeijing
//                        (rawTrajectoryList, updatedMap, iteration, false);
                    System.out.println("Map matching finished, time elapsed: " + (System.currentTimeMillis() - prevTime) / 1000 + " seconds");
                    matchingTime += (System.currentTimeMillis() - prevTime) / 1000;
                    prevTime = System.currentTimeMillis();

                    // step 4: co-optimization model
                    CoOptimizationFunc coOptimizationFunc = new CoOptimizationFunc();
                    Triplet<RoadNetworkGraph, List<Trajectory>, Double> refinementResult =
                            coOptimizationFunc.costFunctionCal(matchingResultTriplet, removedWayList, costFunc);
                    Stream<Trajectory> refinedTrajectory = refinementResult._2().stream();
                    Triplet<List<TrajectoryMatchingResult>, RoadNetworkGraph, List<Trajectory>> refinedMatchingResult = parallelMapMatchingBeijing
                            (refinedTrajectory, refinementResult._1(), iteration, true);
                    // step 5: write refinement result
                    CSVMapWriter refinedMapWriter = new CSVMapWriter(refinementResult._1(), CACHE_FOLDER);
                    refinedMapWriter.writeMap(PERCENTAGE, iteration, false);
                    CSVTrajectoryWriter mergedMatchingResultWriter = new CSVTrajectoryWriter(CACHE_FOLDER);
                    List<TrajectoryMatchingResult> iterationFinalMatchingResult =
                            mergedMatchingResultWriter.writeMergedMatchedTrajectory(matchingResultTriplet._1(), refinedMatchingResult._1(), RANK_LENGTH, iteration);

                    mergedMatchingResultWriter.writeMergedUnmatchedTrajectory(matchingResultTriplet._3(), refinedMatchingResult._3(), iteration);
                    costFunc = refinementResult._3();
                    System.out.println("Result refinement finished, the cost function: " + costFunc + ", time elapsed: " +
                            (System.currentTimeMillis() - prevTime) / 1000 + " seconds.");
                    System.out.println("Finish the " + iteration + " round of iteration, total time elapsed: " + (System.currentTimeMillis()
                            - currIterationStartTime) / 1000 + " seconds.");

                    // evaluation: map-matching evaluation
                    resultEvaluation.beijingMapMatchingEval(iterationFinalMatchingResult, gtMatchingResult, refinementResult._1(), removedWayList);
                    // evaluation: map update evaluation
                    System.out.println("Evaluate the map update result and compare the map accuracy before and after refinement.");
                    resultEvaluation.beijingMapUpdateEval(refinementResult._1(), removedWayList, initialMap);

                    prevTime = System.currentTimeMillis();
                    iteration++;
                }

                // finish the iterations and write the final output
                CSVMapReader finalMapReader = new CSVMapReader(CACHE_FOLDER);
                RoadNetworkGraph finalMap = finalMapReader.readMap(PERCENTAGE, iteration - 1, false);
                CSVMapWriter finalMapWriter = new CSVMapWriter(finalMap, OUTPUT_FOLDER + "map/");
                finalMapWriter.writeMap(PERCENTAGE, -1, false);
                CSVTrajectoryReader finalMatchingResultReader = new CSVTrajectoryReader();
                List<TrajectoryMatchingResult> finalMatchingResult = finalMatchingResultReader.readMatchedResult(CACHE_FOLDER, iteration - 1);
                CSVTrajectoryWriter finalMatchingResultWriter = new CSVTrajectoryWriter(OUTPUT_FOLDER);
                finalMatchingResultWriter.writeMatchedTrajectory(finalMatchingResult, RANK_LENGTH, -1);

                System.out.println("Map matching precision/recall/f-measure:");
                int count = 0;
                for (String s : resultEvaluation.getMapMatchingResult()) {
                    System.out.println(count + "," + s);
                    count++;
                }
                System.out.println("Map update precision/recall/f-measure:");
                count = 0;
                for (String s : resultEvaluation.getMapUpdateResult()) {
                    System.out.println(count + "," + s);
                    count++;
                }
                System.out.println("Co-optimization finish. Total running time: " + (System.currentTimeMillis() - startTaskTime) / 1000 +
                        " seconds, matching time: " + matchingTime + ", update time: " + updateTime + ", refinement time: " +
                        ((System.currentTimeMillis() - startTaskTime) / 1000 - matchingTime - updateTime) + ", total number of " +
                        "iterations: " + (iteration - 1));
            }
        } else if (DATASET_OPTION == 1) {    // map-matching evaluation dataset

            // use global dataset to evaluate the map-matching accuracy
            System.out.println("Start working on the global dataset");

            // map-matching process
            XMLTrajectoryReader reader = new XMLTrajectoryReader(ROOT_PATH + "input/");
            List<TrajectoryMatchingResult> trajectoryMatchingResults = mapMatchingGlobal(reader);

            System.out.println("Map matching finished, total time spent:" + (System.currentTimeMillis() - startTaskTime) / 1000 + "seconds");

            // evaluation: map matching evaluation
            List<Pair<Integer, List<String>>> groundTruthMatchingResult = new ArrayList<>();
            for (int i = 0; i < reader.getNumOfTrajectory(); i++) {
                List<String> matchResult = reader.readGroundTruthMatchResult(i);

                Pair<Integer, List<String>> currGroundTruthMatchResult = new Pair<>(i, matchResult);
                groundTruthMatchingResult.add(currGroundTruthMatchResult);
            }
//            CSVTrajectoryReader groundTruthMatchingResultReader = new CSVTrajectoryReader();
//            List<TrajectoryMatchingResult> trajectoryMatchingResults = groundTruthMatchingResultReader.readMatchedResult(OUTPUT_FOLDER, -1);
            ResultEvaluation resultEvaluation = new ResultEvaluation();
            resultEvaluation.globalPrecisionRecallCalc(trajectoryMatchingResults, groundTruthMatchingResult);

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
        NewsonHMM2009 mapMatching = new NewsonHMM2009(CANDIDATE_RANGE, GAP_EXTENSION_RANGE, RANK_LENGTH, roadMap);
        List<TrajectoryMatchingResult> currMatchingResultList = mapMatching.trajectoryListMatchingProcess(rawTrajectoryList);
        List<Trajectory> unmatchedTraj;
        unmatchedTraj = mapMatching.getUnmatchedTraj();
        return matchedResultPostProcess(roadMap, iteration, refinementStep, currMatchingResultList, unmatchedTraj);
    }

    /**
     * The main entry of map-matching algorithm for Beijing dataset
     *
     * @return map-matched trajectory result
     * @throws IOException file reading error
     */
    private static Triplet<List<TrajectoryMatchingResult>, RoadNetworkGraph, List<Trajectory>> parallelMapMatchingBeijing
    (Stream<Trajectory> rawTrajectoryList, RoadNetworkGraph roadMap, int iteration, boolean refinementStep) throws IOException,
            ExecutionException, InterruptedException {

        // start matching process
        NewsonHMM2009 mapMatching = new NewsonHMM2009(CANDIDATE_RANGE, GAP_EXTENSION_RANGE, RANK_LENGTH, roadMap);
        Stream<Pair<TrajectoryMatchingResult, List<Trajectory>>> currCombinedMatchingResultStream = mapMatching
                .trajectoryStreamMatchingProcess(rawTrajectoryList);
        List<Pair<TrajectoryMatchingResult, List<Trajectory>>> currCombinedMatchingResultList = currCombinedMatchingResultStream.collect(Collectors.toList());
        List<TrajectoryMatchingResult> currMatchingResultList = new ArrayList<>();
        List<Trajectory> unmatchedTraj = new ArrayList<>();
        int brokenTrajCount = 0;
        for (Pair<TrajectoryMatchingResult, List<Trajectory>> currPair : currCombinedMatchingResultList) {
            currMatchingResultList.add(currPair._1());
            if (!currPair._2().isEmpty()) {
                brokenTrajCount++;
                unmatchedTraj.addAll(currPair._2());
            }
        }
        System.out.println("Matching complete, total number of broken trajectories: " + brokenTrajCount);
        return matchedResultPostProcess(roadMap, iteration, refinementStep, currMatchingResultList, unmatchedTraj);
    }

    private static Triplet<List<TrajectoryMatchingResult>, RoadNetworkGraph, List<Trajectory>> matchedResultPostProcess(RoadNetworkGraph roadMap, int iteration, boolean refinementStep, List<TrajectoryMatchingResult> currMatchingResultList, List<Trajectory> unmatchedTraj) throws IOException {
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
        NewsonHMM2009 mapMatching = new NewsonHMM2009(CANDIDATE_RANGE, GAP_EXTENSION_RANGE, RANK_LENGTH, roadMap);
        return mapMatching.trajectorySingleMatchingProcess(trajectory);
    }
}