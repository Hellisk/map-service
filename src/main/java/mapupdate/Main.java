package mapupdate;

import mapupdate.cooptimization.CoOptimizationFunc;
import mapupdate.evaluation.ResultEvaluation;
import mapupdate.mapinference.KDEMapInference;
import mapupdate.mapinference.trajectoryclustering.TrajectoryClusteringMapInference;
import mapupdate.mapmatching.hmm.NewsonHMM2009;
import mapupdate.mapmerge.MapMerge;
import mapupdate.util.function.GreatCircleDistanceFunction;
import mapupdate.util.index.rtree.STRNode;
import mapupdate.util.index.rtree.STRTree;
import mapupdate.util.io.*;
import mapupdate.util.object.datastructure.*;
import mapupdate.util.object.roadnetwork.RoadNetworkGraph;
import mapupdate.util.object.roadnetwork.RoadNode;
import mapupdate.util.object.roadnetwork.RoadWay;
import mapupdate.util.object.spatialobject.Point;
import mapupdate.util.object.spatialobject.Rect;
import mapupdate.util.object.spatialobject.Trajectory;
import mapupdate.util.object.spatialobject.TrajectoryPoint;
import mapupdate.visualisation.UnfoldingMapDisplay;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static mapupdate.util.io.DataPreprocessing.dataPreprocessing;

public class Main {

    /* environmental settings */
    private final static int DATASET_OPTION = 0;     // 0 = beijing trajectory, 1 = global trajectory, -1 = map comparison
    private static int BB_OPTION = 1;          // 1 = Beijing-S, 2 = Beijing-M, 3 = Beijing-L, -d
    public final static int WORKSPACE = 1; // 1 = home, 2 = school, 3 = server
    /* tunable parameters */
    public static int PERCENTAGE = 1;         // percentage of removed road ways (max = 100), -p
    public final static int MIN_TRAJ_TIME_SPAN = 180; // the minimal time span required for a trajectory. -1 = no requirement
    public final static int MAX_TIME_INTERVAL = 120; // the maximum time interval within a trajectory -1 = no requirement
    public static int RANK_LENGTH = 3;  // the number of top-ranked map-matching results to be stored, -r
    private final static boolean IS_TC_MAP_INFERENCE = true;    // true = trace clustering map inference, false =  KDE map inference
    private static int INDEX_TYPE = 0;    // 0 = no index, 1 = DMA, 2 = DMA + PM, -i
    private static double SCORE_THRESHOLD = 20;   // the value that divide the high and low influence/confidence score, -s
    public final static int TRAJECTORY_COUNT = -1; // total number of trajectories extracted. -1 = extract all
    /* bounding boxes */
    private final static double[] BB_BEIJING_S = {116.4, 116.435, 39.95, 39.98};    // preset the map boundary, N-E outside 2 ring, Beijing-S
    private final static double[] BB_BEIJING_M = {116.34, 116.44, 39.89, 39.95};    // preset the map boundary, er huan, Beijing-M
    private final static double[] BB_BEIJING_L = {};    // use the initial map, Beijing-L
    public static double[] BOUNDING_BOX = BB_OPTION == 1 ? BB_BEIJING_S : (BB_OPTION == 2 ? BB_BEIJING_M : BB_BEIJING_L);

    /* parameters */
    /* parameters for HMM-based map matching */
    public final static double BETA = 0.08;    // parameter for transition probability calculation
    public final static double SIGMA = 4;    // parameter for emission probability calculation, also used to filter unnecessary
    // trajectory points (points whose distance is closer than 2*SIGMA will be removed)
    public static int CANDIDATE_RANGE = 50;  // the radius of the candidate generation range in meter, -c
    public static int GAP_EXTENSION_RANGE = 15;  // the trajectory point will be extended as unmatched point if no candidate is
    // found within range, -g·
    public static double U_TURN_PENALTY = 50;
    private static double BB_FACTOR = 1;   // the factor of the index-based bounding box.

    /* parameters for KDE-based map inference */
    private final static int CELL_SIZE = 1;    // size of each cell unit, default is 1
    private final static int GAUSSIAN_BLUR = 17;  // Gaussian blur filter, default is 17
    private final static double SCORE_LAMBDA = 0.5;   // the argument for combining confidence and influence score

    /* parameters for Trace clustering map inference */
    private final static double MAX_ANGLE_CHANGE = 15.0;    // maximum allowable angle change within a road segment, used for
    public final static double DP_EPSILON = 10.0;      // epsilon for Douglas-Peucker filter
    private final static int MAX_TRAJECTORY_DISTANCE = CANDIDATE_RANGE;    // maximum distance between two unmatched trajectories when merging clusters
    // trajectory segmentation

    /* parameters for map merge */
    public final static int MIN_ROAD_LENGTH = 30;     // the minimum length of a new road
    private final static int SUB_TRAJECTORY_MERGE_DISTANCE = 15;   // the maximum allowed distance to attach a end point to an intersection
    public final static double BACKWARDS_FACTOR = 0.2;  // used when trajectory point move backwards due to GPS noise

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
    private final static String LOG_FOLDER = ROOT_PATH + "log/";
    public final static Logger LOGGER = Logger.getLogger("MapUpdateLog");

    // different paths in Beijing dataset
    public final static String RAW_MAP = ROOT_PATH + "raw/map/";
    public final static String RAW_TRAJECTORY = ROOT_PATH + "raw/trajectory/";
    public final static String GT_MAP = ROOT_PATH + "groundTruth/map/";  // ground-truth road network
    public final static String GT_MATCHING_RESULT =
            ROOT_PATH + "groundTruth/matchingResult/M" + BB_OPTION + "_TP" + MIN_TRAJ_TIME_SPAN + "_TI" + MAX_TIME_INTERVAL + "_TC" + TRAJECTORY_COUNT + "/";   // the map-matched trajectory dataset
    public final static String INPUT_MAP = ROOT_PATH + "input/map/"; // the map with removed roads
    public final static String INPUT_TRAJECTORY =
            ROOT_PATH + "input/trajectory/M" + BB_OPTION + "_TP" + MIN_TRAJ_TIME_SPAN + "_TI" + MAX_TIME_INTERVAL + "_TC" + TRAJECTORY_COUNT + "/";    // input trajectory dataset
    public final static String OUTPUT_FOLDER = ROOT_PATH + "output/";
    public final static String CACHE_FOLDER = ROOT_PATH + "cache/";
    public final static String INFERENCE_FOLDER = WORKSPACE == 3 ? "/home/uqpchao/data/mapInference/" : ROOT_PATH + "mapInference/";

    public static void main(String[] args) throws IOException, ExecutionException, InterruptedException {

        Properties prop = System.getProperties();
        long initTaskTime = System.currentTimeMillis();

        for (String arg : args) {
            if (arg.charAt(0) == '-') {
                if (arg.length() < 3)
                    throw new IllegalArgumentException("Not a valid argument: " + arg);
                switch (arg.charAt(1)) {
                    case 'd':
                        BB_OPTION = Integer.parseInt(arg.substring(2));
                        break;
                    case 'p':
                        PERCENTAGE = Integer.parseInt(arg.substring(2));
                        break;
                    case 'r':
                        RANK_LENGTH = Integer.parseInt(arg.substring(2));
                        break;
                    case 's':
                        SCORE_THRESHOLD = Integer.parseInt(arg.substring(2));
                        break;
                    case 'c':
                        CANDIDATE_RANGE = Integer.parseInt(arg.substring(2));
                        break;
                    case 'g':
                        GAP_EXTENSION_RANGE = Integer.parseInt(arg.substring(2));
                        break;
                    case 'i':
                        INDEX_TYPE = Integer.parseInt(arg.substring(2));
                    case 'X':
                        break;
                    default:
                        throw new IllegalArgumentException("Not a valid argument: " + arg);
                }
            } else {
                throw new IllegalArgumentException("Not a valid argument: " + arg);
            }
        }

        // setup java log
        logInit();

        if (DATASET_OPTION == 0) {  // complete algorithm on beijing dataset

            LOGGER.info("Start working on the beijing dataset...");

            // process the raw data and convert them into standard format
//            rawMapInitialization();

            // pre-processing the data so that the map and the trajectories are filtered
//            if (PERCENTAGE != 6)
            dataPreprocessing(true);

            long startTaskTime = System.currentTimeMillis();
            long prevTime = System.currentTimeMillis();

            // initialization step, read the input first
            CSVMapReader csvMapReader = new CSVMapReader(INPUT_MAP);
            RoadNetworkGraph initialMap = csvMapReader.readMap(PERCENTAGE, -1, false);
            List<RoadWay> removedWayList = csvMapReader.readRemovedEdges(PERCENTAGE, -1);
            STRTree<Point> trajectoryPointIndex = null;   // index for location-based trajectory search
            CSVTrajectoryReader rawTrajectoryReader = new CSVTrajectoryReader(INDEX_TYPE);    // read input trajectories
            Stream<Trajectory> initialTrajectoryStream = rawTrajectoryReader.readTrajectoryFilesStream(INPUT_TRAJECTORY);
//            List<Trajectory> rawTrajectoryList = rawTrajectoryReader.readTrajectoryFilesList(INPUT_TRAJECTORY);
            CSVTrajectoryReader groundTruthMatchingResultReader = new CSVTrajectoryReader(0);    // read ground-truth map-matching
            // result
            List<Pair<Integer, List<String>>> gtMatchingResult = groundTruthMatchingResultReader.readGroundTruthMatchingResult(GT_MATCHING_RESULT);

            File cacheFolder = new File(CACHE_FOLDER);
            if (cacheFolder.exists())
                FileUtils.deleteDirectory(cacheFolder);
            File inferenceFolder = new File(INFERENCE_FOLDER);
            if (inferenceFolder.exists())
                FileUtils.deleteDirectory(inferenceFolder);

            // step 0: map-matching process, start the initial map-matching
            CoOptimizationFunc initialOptimizationFunc = new CoOptimizationFunc();  // not useful, only for filling the arguments
            Pair<List<TrajectoryMatchingResult>, List<Triplet<Trajectory, String, String>>> initialMatchingResultPair =
                    parallelMapMatchingBeijing(initialTrajectoryStream, initialMap, 0, "normal", initialOptimizationFunc);
            LOGGER.info("Initial map matching finished, time elapsed:" + (System.currentTimeMillis() - prevTime) / 1000 + " seconds");
            prevTime = System.currentTimeMillis();
            if (INDEX_TYPE != 0) {
                Pair<STRTree<Point>, Integer> indexInfoPair = rawTrajectoryReader.createIndex();
                trajectoryPointIndex = indexInfoPair._1();
                LOGGER.info("Trajectory index is built for subsequent queries. Total number of points in index: " + indexInfoPair._2() +
                        ", time elapsed:" + (System.currentTimeMillis() - prevTime) / 1000 + " seconds");
            }
            int matchingTime = 0;
            int updateTime = 0;
            int refineMatchingTime = 0;
            int refinementTime = 0;

            // evaluation: initial map matching evaluation
            ResultEvaluation resultEvaluation = new ResultEvaluation();
            resultEvaluation.beijingMapMatchingEval(initialMatchingResultPair._1(), gtMatchingResult, initialMap, removedWayList);

            Pair<List<TrajectoryMatchingResult>, List<Triplet<Trajectory, String, String>>> prevMatchingResultPair = initialMatchingResultPair;
            if (PERCENTAGE != 0) {
                long totalIterationStartTime = System.currentTimeMillis();
                int iteration = 1;  // start the iteration
                double costFunc = 0;
                while (iteration <= 8) {
//                while (costFunc >= 0) {

                    LOGGER.info("Start the " + iteration + " round of iteration.");
                    long currIterationStartTime = System.currentTimeMillis();

                    HashMap<String, Pair<HashSet<String>, HashSet<String>>> newRoadID2AnchorPoints = new HashMap<>();
                    List<RoadWay> inferenceResult;
                    if (IS_TC_MAP_INFERENCE) {
                        // step 1: Trace clustering map inference
//                        CSVTrajectoryReader unmatchedTrajReader = new CSVTrajectoryReader(0);
                        List<Triplet<Trajectory, String, String>> unmatchedTrajList = prevMatchingResultPair._2();
//                        List<Triplet<Trajectory, String, String>> unmatchedTrajList =
//                                unmatchedTrajReader.readUnmatchedTrajectoryFilesList(CACHE_FOLDER + "unmatchedTraj/TP" +
//                        MIN_TRAJ_TIME_SPAN + "_TI" + MAX_TIME_INTERVAL + "_TC" + TRAJECTORY_COUNT + "/" + (iteration - 1) + "/");
                        TrajectoryClusteringMapInference mapInference = new TrajectoryClusteringMapInference();
                        inferenceResult = mapInference.startMapInferenceProcess(unmatchedTrajList, newRoadID2AnchorPoints,
                                MAX_ANGLE_CHANGE, MAX_TRAJECTORY_DISTANCE);
                        CSVMapWriter.writeInferredRoadWayList(inferenceResult, INFERENCE_FOLDER, iteration);
                    } else {
                        // step 1-old: KDE map inference
                        KDEMapInference mapInference = new KDEMapInference(CELL_SIZE, GAUSSIAN_BLUR);
                        String localDir = System.getProperty("user.dir");
                        mapInference.startMapInference(localDir + "/src/main/python/", CACHE_FOLDER + "unmatchedNextInput/TP" +
                                MIN_TRAJ_TIME_SPAN + "_TI" + MAX_TIME_INTERVAL + "_TC" + TRAJECTORY_COUNT + "/" + (iteration - 1) + "/");
                        CSVMapReader inferenceEdgeReader = new CSVMapReader(INFERENCE_FOLDER);
                        inferenceResult = inferenceEdgeReader.readInferredEdges(true);
                    }
                    LOGGER.info("Map inference finished, " + inferenceResult.size() + " new roads inferred, time elapsed: " +
                            (System.currentTimeMillis() - currIterationStartTime) / 1000 + " " + "seconds");

                    // step 2: map merge
                    if (inferenceResult.size() == 0) {
                        LOGGER.info("Current iteration does not have new road inferred. Finish the iteration.");
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
                    MapMerge spMapMerge = new MapMerge(prevMap, removedWayList, CANDIDATE_RANGE, SUB_TRAJECTORY_MERGE_DISTANCE);
                    List<RoadWay> newWayList;
                    newWayList = spMapMerge.nearestNeighbourMapMerge(inferenceResult, newRoadID2AnchorPoints);
                    if (newWayList.size() == 0) {
                        LOGGER.info("Current iteration does not have new road added. Finish the iteration.");
                        break;
                    }

                    updateTime += (System.currentTimeMillis() - currIterationStartTime) / 1000;
                    prevTime = System.currentTimeMillis();

                    if (INDEX_TYPE != 2) {  // no index or index on one-pass co-optimisation
                        LOGGER.info("One-pass map merge finished, time elapsed: " + (System.currentTimeMillis() - prevTime) / 1000 + " seconds");
                        CoOptimizationFunc coOptimizationFunc = new CoOptimizationFunc();
                        prevMap.addWays(newWayList);
                        resultEvaluation.beijingMapUpdateEval(prevMap, removedWayList, initialMap);
//                        CSVMapWriter updatedMapWriter = new CSVMapWriter(prevMap, CACHE_FOLDER);
//                        updatedMapWriter.writeMap(PERCENTAGE, iteration, true);

                        // map update evaluation

                        // step 3: map-matching process on updated map
                        Pair<List<TrajectoryMatchingResult>, List<Triplet<Trajectory, String, String>>> matchingResultPair;
                        if (INDEX_TYPE == 1) {
                            HashSet<String> trajIDSet = trajectoryIDSearch(newWayList, trajectoryPointIndex);
                            Stream<Trajectory> inputTrajectoryStream = rawTrajectoryReader.readPartialTrajectoryFilesStream(INPUT_TRAJECTORY,
                                    trajIDSet);
                            LOGGER.info("One-pass trajectory filtering finished, " + trajIDSet.size() + " trajectories are " +
                                    "selected, time elapsed: " + (System.currentTimeMillis() - prevTime) / 1000 + " seconds");
                            matchingResultPair = parallelMapMatchingBeijing(inputTrajectoryStream, prevMap, iteration, "partial",
                                    coOptimizationFunc);
                            List<TrajectoryMatchingResult> completeMatchingResultList = matchingResultMerge(matchingResultPair._1(),
                                    prevMatchingResultPair._1());
                            resultEvaluation.beijingMapMatchingEval(completeMatchingResultList, gtMatchingResult, prevMap, removedWayList);
                            LOGGER.info("Map matching finished, time elapsed: " + (System.currentTimeMillis() - prevTime) / 1000 + " seconds");
                        } else {
                            Stream<Trajectory> inputTrajectoryStream = rawTrajectoryReader.readTrajectoryFilesStream(INPUT_TRAJECTORY);
                            matchingResultPair = parallelMapMatchingBeijing(inputTrajectoryStream, prevMap, iteration, "normal",
                                    coOptimizationFunc);
                            resultEvaluation.beijingMapMatchingEval(matchingResultPair._1(), gtMatchingResult, prevMap, removedWayList);
                            LOGGER.info("Map matching finished, time elapsed: " + (System.currentTimeMillis() - prevTime) / 1000 + " seconds");
                        }

                        matchingTime += (System.currentTimeMillis() - prevTime) / 1000;
                        prevTime = System.currentTimeMillis();

                        // step 4: co-optimization model
//                        Triplet<RoadNetworkGraph, List<Trajectory>, Double> refinementResult = coOptimizationFunc.percentageBasedCostFunction
//                                (matchingResultPair, removedWayList, SCORE_THRESHOLD, costFunc);
                        Triplet<RoadNetworkGraph, List<Trajectory>, Double> refinementResult = coOptimizationFunc.combinedScoreCostFunction
                                (matchingResultPair, removedWayList, prevMap, SCORE_THRESHOLD, SCORE_LAMBDA, costFunc);
                        Stream<Trajectory> refinedTrajectory = refinementResult._2().stream();
                        Pair<List<TrajectoryMatchingResult>, List<Triplet<Trajectory, String, String>>> refinedMatchingResult = parallelMapMatchingBeijing
                                (refinedTrajectory, refinementResult._1(), iteration, "refinement", coOptimizationFunc);

                        // step 5: write refinement result
                        CSVMapWriter refinedMapWriter = new CSVMapWriter(refinementResult._1(), CACHE_FOLDER);
                        refinedMapWriter.writeMap(PERCENTAGE, iteration, false);
                        refinementTime += (System.currentTimeMillis() - prevTime) / 1000;
                        prevTime = System.currentTimeMillis();

                        CSVTrajectoryWriter mergedMatchingResultWriter = new CSVTrajectoryWriter(CACHE_FOLDER);
                        List<TrajectoryMatchingResult> iterationFinalMatchingResult =
                                mergedMatchingResultWriter.writeMergedMatchedTrajectory(matchingResultPair._1(), refinedMatchingResult._1(), RANK_LENGTH, iteration);

                        List<Triplet<Trajectory, String, String>> iterationFinalUnmatchedResult =
                                mergedMatchingResultWriter.writeMergedUnmatchedTrajectory(matchingResultPair._2(), refinementResult._2(),
                                        refinedMatchingResult._2(), iteration);
                        costFunc = refinementResult._3();
                        refineMatchingTime += (System.currentTimeMillis() - prevTime) / 1000;

                        // evaluation: map-matching evaluation
                        resultEvaluation.beijingMapMatchingEval(iterationFinalMatchingResult, gtMatchingResult, refinementResult._1(), removedWayList);
                        if (matchingResultPair._2().size() == 0)  // no unmatched trajectory, iteration terminates
                            costFunc = -1;
                        // evaluation: map update evaluation
                        LOGGER.info("Evaluate the map update result and compare the map accuracy before and after refinement.");
                        resultEvaluation.beijingMapUpdateEval(refinementResult._1(), removedWayList, initialMap);
                        prevMatchingResultPair = new Pair<>(iterationFinalMatchingResult, iterationFinalUnmatchedResult);
                    } else {    // index-based parallel map update
                        CoOptimizationFunc coOptimizationFunc = new CoOptimizationFunc(prevMap, newWayList);
                        // read the previous map-matching result
                        HashMap<String, List<Pair<String, MatchingResultItem>>> trajID2MatchingResultUpdate = new LinkedHashMap<>();
//                        CSVTrajectoryReader csvMatchedTrajectoryReader = new CSVTrajectoryReader(0);
//                        List<TrajectoryMatchingResult> prevMatchingResultList = csvMatchedTrajectoryReader.readMatchedResult(CACHE_FOLDER, iteration - 1);
                        for (TrajectoryMatchingResult mr : prevMatchingResultPair._1()) {
                            if (!trajID2MatchingResultUpdate.containsKey(mr.getTrajID())) {
                                List<Pair<String, MatchingResultItem>> matchingResultList = new ArrayList<>();
                                MatchingResultItem prevMatchingResultItem = new MatchingResultItem(mr, new ArrayList<>());
                                matchingResultList.add(new Pair<>("", prevMatchingResultItem));
                                trajID2MatchingResultUpdate.put(mr.getTrajID(), matchingResultList);
                            } else LOGGER.severe("ERROR! The same trajectory matching result occurred twice: " + mr.getTrajID());
                        }

                        prevMap.addWays(newWayList);
                        resultEvaluation.beijingMapUpdateEval(prevMap, removedWayList, initialMap);
                        NewsonHMM2009 mapMatching = new NewsonHMM2009(CANDIDATE_RANGE, GAP_EXTENSION_RANGE, RANK_LENGTH, prevMap, true);
                        HashMap<String, List<RoadWay>> id2DDWayList = new LinkedHashMap<>();

                        // combine the double-directed road to one entry.
                        for (RoadWay w : newWayList) {
                            String id = w.getID().replace("-", "");
                            if (id2DDWayList.containsKey(id))
                                id2DDWayList.get(id).add(w);
                            else {
                                List<RoadWay> wayList = new ArrayList<>();
                                wayList.add(w);
                                id2DDWayList.put(id, wayList);
                            }
                        }
                        int totalTrajCount = 0;
                        LinkedHashMap<String, Integer> trajectoryMatchCount = new LinkedHashMap<>();
                        for (Map.Entry<String, List<RoadWay>> entry : id2DDWayList.entrySet()) {
                            if (entry.getValue().size() > 2)
                                LOGGER.warning("WARNING! More than two roads have the same id");
                            List<RoadWay> oneRoadList = new ArrayList<>();
                            oneRoadList.add(entry.getValue().get(0));
                            LinkedHashSet<String> trajIDSet = trajectoryIDSearch(oneRoadList, trajectoryPointIndex);
                            totalTrajCount += trajIDSet.size();
                            for (String trajID : trajIDSet) {
                                if (!trajectoryMatchCount.containsKey(trajID)) {
                                    trajectoryMatchCount.put(trajID, 1);
                                } else
                                    trajectoryMatchCount.put(trajID, trajectoryMatchCount.get(trajID) + 1);
                            }
                            List<String> roadIDList = new ArrayList<>();
                            for (RoadWay w : entry.getValue()) {
                                roadIDList.add(w.getID());
                            }
                            singleDDRoadMapMatchingBeijing(trajIDSet, rawTrajectoryReader, mapMatching, roadIDList,
                                    trajID2MatchingResultUpdate, coOptimizationFunc);

                        }

                        int maxMatchCount = 0;
                        for (Map.Entry<String, Integer> entry : trajectoryMatchCount.entrySet()) {
                            maxMatchCount = maxMatchCount > entry.getValue() ? maxMatchCount : entry.getValue();
                        }

                        LOGGER.info("Map matching finished, " + totalTrajCount + " trajectories involved, " + trajectoryMatchCount.size() +
                                " unique trajectories, max duplicated matching count: " + maxMatchCount + ", time elapsed: " + (System.currentTimeMillis() - prevTime) / 1000 + "seconds");
                        matchingTime += (System.currentTimeMillis() - prevTime) / 1000;
                        prevTime = System.currentTimeMillis();

                        Triplet<RoadNetworkGraph, Set<String>, Double> refinementResult = coOptimizationFunc.indexedCombinedScoreCostFunction
                                (trajID2MatchingResultUpdate, removedWayList, prevMap, SCORE_THRESHOLD, SCORE_LAMBDA, costFunc);
                        Triplet<List<TrajectoryMatchingResult>, List<Triplet<Trajectory, String, String>>, Integer> refinedMatchingResult =
                                refineMatchingResult(trajID2MatchingResultUpdate, refinementResult._2());

                        LOGGER.info("Map refinement finished, total road removed: " + refinementResult._2().size() + ", trajectory affected: " +
                                refinedMatchingResult._3());

                        // step 5: write refinement result
                        CSVMapWriter refinedMapWriter = new CSVMapWriter(refinementResult._1(), CACHE_FOLDER);
                        refinedMapWriter.writeMap(PERCENTAGE, iteration, false);
                        refinementTime += (System.currentTimeMillis() - prevTime) / 1000;
                        prevTime = System.currentTimeMillis();

                        CSVTrajectoryWriter matchingResultWriter = new CSVTrajectoryWriter(CACHE_FOLDER);
                        matchingResultWriter.writeMatchedTrajectory(refinedMatchingResult._1(), RANK_LENGTH, iteration);
                        matchingResultWriter.writeUnmatchedTrajectory(refinedMatchingResult._2(), iteration);
                        costFunc = refinementResult._3();
                        refineMatchingTime += (System.currentTimeMillis() - prevTime) / 1000;

                        // evaluation: map-matching evaluation
                        resultEvaluation.beijingMapMatchingEval(refinedMatchingResult._1(), gtMatchingResult, refinementResult._1(), removedWayList);
                        if (refinedMatchingResult._2().size() == 0)  // no unmatched trajectory, iteration terminates
                            costFunc = -1;
                        // evaluation: map update evaluation
                        LOGGER.info("Evaluate the map update result and compare the map accuracy before and after refinement.");
                        resultEvaluation.beijingMapUpdateEval(refinementResult._1(), removedWayList, initialMap);
                        prevMatchingResultPair = new Pair<>(refinedMatchingResult._1(), refinedMatchingResult._2());
                    }

                    LOGGER.info("Result refinement finished, the cost function: " + costFunc + ", time elapsed: " +
                            (System.currentTimeMillis() - prevTime) / 1000 + " seconds.");

                    LOGGER.info("Finish the " + iteration + " round of iteration, total time elapsed: " + (System.currentTimeMillis()
                            - currIterationStartTime) / 1000 + " seconds.");
                    iteration++;
                }

                // finish the iterations and write the final output
                CSVMapReader finalMapReader = new CSVMapReader(CACHE_FOLDER);
                RoadNetworkGraph finalMap = finalMapReader.readMap(PERCENTAGE, iteration - 1, false);
                CSVMapWriter finalMapWriter = new CSVMapWriter(finalMap, OUTPUT_FOLDER + "map/");
                finalMapWriter.writeMap(PERCENTAGE, -1, false);
                CSVTrajectoryReader finalMatchingResultReader = new CSVTrajectoryReader(0);
                List<TrajectoryMatchingResult> finalMatchingResult = finalMatchingResultReader.readMatchedResult(CACHE_FOLDER, iteration - 1);
                CSVTrajectoryWriter finalMatchingResultWriter = new CSVTrajectoryWriter(OUTPUT_FOLDER);
                finalMatchingResultWriter.writeMatchedTrajectory(finalMatchingResult, RANK_LENGTH, -1);

                LOGGER.info("Map matching length-based precision/recall/f-measure:");
                int count = 0;
                for (String s : resultEvaluation.getMapMatchingLengthResult()) {
                    LOGGER.info(count + "," + s);
                    count++;
                }
//                LOGGER.info("Map matching count-based precision/recall/f-measure:");
//                count = 0;
//                for (String s : resultEvaluation.getMapMatchingCountResult()) {
//                    LOGGER.info(count + "," + s);
//                    count++;
//                }
                LOGGER.info("Map update length-based precision/recall/f-measure:");
                count = 0;
                for (String s : resultEvaluation.getMapUpdateLengthResult()) {
                    LOGGER.info(count + "," + s);
                    count++;
                }
//                LOGGER.info("Map update count-based precision/recall/f-measure:");
//                count = 0;
//                for (String s : resultEvaluation.getMapUpdateCountResult()) {
//                    LOGGER.info(count + "," + s);
//                    count++;
//                }
                LOGGER.info("Co-optimization finish. Total running time: " + (System.currentTimeMillis() - startTaskTime) / 1000 +
                        " seconds, matching time: " + matchingTime + ", update time: " + updateTime + ", refinement time: " +
                        refinementTime + ", refinement matching time: " + refineMatchingTime + ", average time per " +
                        "iteration: " + (System.currentTimeMillis() - totalIterationStartTime) / (iteration - 1) / 1000 + ", total number of " +
                        "iterations: " + (iteration - 1));
            }
        } else if (DATASET_OPTION == 1) {    // map-matching evaluation dataset

            // use global dataset to evaluate the map-matching accuracy
            LOGGER.info("Start working on the global dataset");

            long startTaskTime = System.currentTimeMillis();
            // map-matching process
            XMLTrajectoryReader reader = new XMLTrajectoryReader(ROOT_PATH + "input/");
            List<TrajectoryMatchingResult> trajectoryMatchingResults = mapMatchingGlobal(reader);

            LOGGER.info("Map matching finished, total time spent:" + (System.currentTimeMillis() - startTaskTime) / 1000 + "seconds");

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
            LOGGER.info("Start comparing different versions of Beijing map...");

            long startTaskTime = System.currentTimeMillis();
            RawMapReader newMapReader = new RawMapReader(RAW_MAP, BOUNDING_BOX);
            LOGGER.info("Start reading the new and old Beijing maps.");
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
            LOGGER.info("Total matched node percentage: " + matchCount / (double) newBeijingMap.getNodes().size());
            // visualization
            UnfoldingMapDisplay graphDisplay = new UnfoldingMapDisplay();
            graphDisplay.display();
        }

        LOGGER.info("Task finish, total time spent: " + (System.currentTimeMillis() - initTaskTime) / 1000 + " seconds");
    }

    private static List<TrajectoryMatchingResult> matchingResultMerge(List<TrajectoryMatchingResult> currPartialMatchingResult, List<TrajectoryMatchingResult> prevMatchingResult) {
        HashSet<String> matchingResultSet = new HashSet<>(currPartialMatchingResult.size());
        for (TrajectoryMatchingResult mr : currPartialMatchingResult) {
            matchingResultSet.add(mr.getTrajID());
        }
        List<TrajectoryMatchingResult> mergedMatchingResult = new ArrayList<>(prevMatchingResult);
        mergedMatchingResult.removeIf(next -> matchingResultSet.contains(next.getTrajID()));
        mergedMatchingResult.addAll(currPartialMatchingResult);
        return mergedMatchingResult;
    }

    private static Triplet<List<TrajectoryMatchingResult>, List<Triplet<Trajectory, String, String>>, Integer> refineMatchingResult(HashMap<String, List<Pair<String, MatchingResultItem>>> trajID2MatchingResultUpdate, Set<String> removedRoadIDSet) {
        int affectedTrajCount = 0;
        int multipleMatchTraj = 0;  // number of trajectories that affected by multiple new roads
        List<TrajectoryMatchingResult> matchingResultList = new ArrayList<>();
        List<Triplet<Trajectory, String, String>> unmatchedTrajList = new ArrayList<>();
        for (Map.Entry<String, List<Pair<String, MatchingResultItem>>> entry : trajID2MatchingResultUpdate.entrySet()) {
            List<Pair<String, MatchingResultItem>> matchingResult = entry.getValue();
            if (matchingResult.size() == 1) { // map-matching not changed
                matchingResultList.add(matchingResult.get(0)._2().getMatchingResult());
                if (matchingResult.get(0)._2().getUnmatchedTrajectoryList().size() != 0)
                    unmatchedTrajList.addAll(matchingResult.get(0)._2().getUnmatchedTrajectoryList());
            } else if (matchingResult.size() > 1) { //map-matching result changed
                MatchingResultItem finalResult = matchingResult.get(0)._2();
                double maxProbability = finalResult.getMatchingResult().getBestProbability();
                boolean isAffectedByRemoval = false;
                if (matchingResult.size() > 2)
                    multipleMatchTraj++;
                for (int i = 1; i < matchingResult.size(); i++) {
                    String roadID = matchingResult.get(i)._1();
                    MatchingResultItem currMatchingResult = matchingResult.get(i)._2();
                    if (!removedRoadIDSet.contains(roadID)) {
                        if (currMatchingResult.getMatchingResult().getBestProbability() > maxProbability) {
                            finalResult = currMatchingResult;
                            maxProbability = currMatchingResult.getMatchingResult().getBestProbability();
                        }
                    } else isAffectedByRemoval = true;
                }
                if (isAffectedByRemoval)
                    affectedTrajCount++;

                matchingResultList.add(finalResult.getMatchingResult());
                if (finalResult.getUnmatchedTrajectoryList().size() != 0)
                    unmatchedTrajList.addAll(finalResult.getUnmatchedTrajectoryList());
            }
        }

        LOGGER.info("Total number of trajectories that can be matched to multiple new roads: " + multipleMatchTraj);
        return new Triplet<>(matchingResultList, unmatchedTrajList, affectedTrajCount);
    }

    private static void logInit() {
//        BasicConfigurator.configure();
        FileHandler fh;
        try {
            File logFolder = new File(LOG_FOLDER);
            if (!logFolder.exists())
                if (!logFolder.mkdirs()) throw new IOException("ERROR! Failed to create log folder.");
            // This block configure the logger with handler and formatter
            String logType = DATASET_OPTION + "_" + BB_OPTION + "_" + PERCENTAGE + "_" + GAP_EXTENSION_RANGE + "_" + RANK_LENGTH + "_"
                    + SCORE_THRESHOLD + "_" + CANDIDATE_RANGE + "_" + INDEX_TYPE + "_" + BB_FACTOR;
            fh = new FileHandler(LOG_FOLDER + "/" + logType + "_" + System.currentTimeMillis() + ".log");
            LOGGER.addHandler(fh);
//            SimpleFormatter formatter = new SimpleFormatter();
//            fh.setFormatter(formatter);
            fh.setFormatter(new MyFormatter());

            // start the log by listing all arguments
            LOGGER.info("Map-Trajectory Co-Optimization with arguments: " + DATASET_OPTION + ", " + BB_OPTION + ", " + MIN_TRAJ_TIME_SPAN
                    + ", " + MAX_TIME_INTERVAL + ", pct=" + PERCENTAGE + "%, gapDist=" + GAP_EXTENSION_RANGE + ", k=" + RANK_LENGTH + ", " +
                    "scoreThresh=" + SCORE_THRESHOLD + ", canDist=" + CANDIDATE_RANGE + ", index type = " + INDEX_TYPE + ", bounding box " +
                    "factor = " + BB_FACTOR);

        } catch (SecurityException | IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * The main entry of map-matching algorithm for Beijing dataset
     *
     * @return map-matched trajectory result
     * @throws IOException file reading error
     */
    private static Pair<List<TrajectoryMatchingResult>, List<Triplet<Trajectory, String, String>>> mapMatchingBeijing
    (List<Trajectory> rawTrajectoryList, RoadNetworkGraph roadMap, int iteration, String matchType,
     CoOptimizationFunc coOptimizationFunc) throws IOException {

        // start matching process
        NewsonHMM2009 mapMatching = new NewsonHMM2009(CANDIDATE_RANGE, GAP_EXTENSION_RANGE, RANK_LENGTH, roadMap, false);
        List<MatchingResultItem> currMatchingResultList = mapMatching.trajectoryListMatchingProcess(rawTrajectoryList);
        List<TrajectoryMatchingResult> matchingResultList = new ArrayList<>();
        List<Triplet<Trajectory, String, String>> unmatchedTrajList = new ArrayList<>();
        for (MatchingResultItem mr : currMatchingResultList) {
            matchingResultList.add(mr.getMatchingResult());
            unmatchedTrajList.addAll(mr.getUnmatchedTrajectoryList());
        }
        return matchedResultPostProcess(roadMap, iteration, matchType, matchingResultList, unmatchedTrajList, coOptimizationFunc);
    }

    /**
     * Map-matching and influence score generation on map that contains only one new road.
     *
     * @param trajIDSet                   ID of Trajectories to be matched.
     * @param mapMatching                 Map-matching class which contains routing graph.
     * @param roadIDList                  List of new road IDs.
     * @param trajID2MatchingResultUpdate Mapping between trajectory and its previous + new matching result.
     * @param coOptimizationFunc          Co-optimization function.
     * @throws ExecutionException   Parallel error.
     * @throws InterruptedException Parallel error.
     */
    private static void singleDDRoadMapMatchingBeijing(Set<String> trajIDSet, CSVTrajectoryReader rawTrajectoryReader,
                                                       NewsonHMM2009 mapMatching, List<String> roadIDList, HashMap<String,
            List<Pair<String, MatchingResultItem>>> trajID2MatchingResultUpdate, CoOptimizationFunc coOptimizationFunc) throws ExecutionException, InterruptedException {
        for (String roadID : roadIDList) {
            List<XYObject<SegmentIndexItem>> indexEntry = mapMatching.insertRoadWayIntoMap(roadID);
            // start matching process
//            List<MatchingResultItem> currCombinedMatchingResultList =
//                    mapMatching.trajectoryListMatchingProcess(rawTrajectoryList.collect(Collectors.toList()));
            Stream<Trajectory> inputTrajectoryStream = rawTrajectoryReader.readPartialTrajectoryFilesStream(INPUT_TRAJECTORY,
                    trajIDSet);
            Stream<MatchingResultItem> currCombinedMatchingResultStream = mapMatching.trajectoryStreamMatchingProcess(inputTrajectoryStream);
            List<MatchingResultItem> currCombinedMatchingResultList = currCombinedMatchingResultStream.collect(Collectors.toList());

//            Set<String> currMatchingIDSet = new HashSet<>();
//            // check duplicated matching results
//            for (MatchingResultItem mr : currCombinedMatchingResultList) {
//                if (!currMatchingIDSet.contains(mr.getTrajID())) {
//                    currMatchingIDSet.add(mr.getTrajID());
//                } else LOGGER.severe("ERROR! The current trajectory is matched twice: " + mr.getTrajID());
//            }
            coOptimizationFunc.singleRoadInfluenceScoreGen(currCombinedMatchingResultList, trajID2MatchingResultUpdate, roadID);
            mapMatching.removeRoadWayFromMap(roadID, indexEntry);
        }
    }

    /**
     * The main entry of map-matching algorithm for Beijing dataset
     *
     * @return map-matched trajectory result
     * @throws IOException file reading error
     */
    private static Pair<List<TrajectoryMatchingResult>, List<Triplet<Trajectory, String, String>>> parallelMapMatchingBeijing
    (Stream<Trajectory> rawTrajectoryList, RoadNetworkGraph roadMap, int iteration, String matchType,
     CoOptimizationFunc coOptimizationFunc) throws IOException, ExecutionException, InterruptedException {

        // start matching process
        NewsonHMM2009 mapMatching = new NewsonHMM2009(CANDIDATE_RANGE, GAP_EXTENSION_RANGE, RANK_LENGTH, roadMap, false);
        Stream<MatchingResultItem> currCombinedMatchingResultStream = mapMatching.trajectoryStreamMatchingProcess(rawTrajectoryList);
        List<MatchingResultItem> currCombinedMatchingResultList = currCombinedMatchingResultStream.collect(Collectors.toList());
        List<TrajectoryMatchingResult> currMatchingResultList = new ArrayList<>();
        List<Triplet<Trajectory, String, String>> unmatchedTrajInfo = new ArrayList<>();
        int brokenTrajCount = 0;
        for (MatchingResultItem currPair : currCombinedMatchingResultList) {
            currMatchingResultList.add(currPair.getMatchingResult());
            if (!currPair.getUnmatchedTrajectoryList().isEmpty()) {
                brokenTrajCount++;
                unmatchedTrajInfo.addAll(currPair.getUnmatchedTrajectoryList());
            }
        }
        LOGGER.info("Matching complete, total number of broken trajectories: " + brokenTrajCount);
        return matchedResultPostProcess(roadMap, iteration, matchType, currMatchingResultList, unmatchedTrajInfo, coOptimizationFunc);
    }

    private static Pair<List<TrajectoryMatchingResult>, List<Triplet<Trajectory, String, String>>> matchedResultPostProcess
            (RoadNetworkGraph roadMap, int iteration, String matchType, List<TrajectoryMatchingResult> currMatchingResultList,
             List<Triplet<Trajectory, String, String>> unmatchedTrajInfo, CoOptimizationFunc coOptimizationFunc) throws IOException {
        switch (matchType) {
            case "normal":  // traditional iterative map-matching
                if (iteration != 0) {     // start processing the co-optimization model
                    CSVTrajectoryReader csvMatchedTrajectoryReader = new CSVTrajectoryReader(0);
                    List<TrajectoryMatchingResult> prevMatchingResult = csvMatchedTrajectoryReader.readMatchedResult(CACHE_FOLDER, iteration - 1);
                    Map<String, TrajectoryMatchingResult> id2PrevMatchingResult = new HashMap<>();
                    for (TrajectoryMatchingResult mr : prevMatchingResult) {
                        if (!id2PrevMatchingResult.containsKey(mr.getTrajID()))
                            id2PrevMatchingResult.put(mr.getTrajID(), mr);
                        else LOGGER.severe("ERROR! The same trajectory matching result occurred twice: " + mr.getTrajID());
                    }
                    coOptimizationFunc.influenceScoreGen(currMatchingResultList, id2PrevMatchingResult, roadMap);
                    return new Pair<>(currMatchingResultList, unmatchedTrajInfo);
                } else {
                    // initial map-matching step, write output matching result
                    CSVTrajectoryWriter matchingResultWriter = new CSVTrajectoryWriter(CACHE_FOLDER);
                    matchingResultWriter.writeMatchedTrajectory(currMatchingResultList, RANK_LENGTH, iteration);
                    CSVTrajectoryWriter unmatchedTrajWriter = new CSVTrajectoryWriter(CACHE_FOLDER);
                    unmatchedTrajWriter.writeUnmatchedTrajectory(unmatchedTrajInfo, iteration);
                    return new Pair<>(currMatchingResultList, unmatchedTrajInfo);
                }
            case "partial": // index-based iterative map-matching
                if (iteration != 0) {     // start processing the co-optimization model
                    CSVTrajectoryReader csvMatchedTrajectoryReader = new CSVTrajectoryReader(0);
                    Set<String> currMatchingIDSet = new HashSet<>();
                    List<TrajectoryMatchingResult> unchangedResultList = new ArrayList<>();
                    for (TrajectoryMatchingResult mr : currMatchingResultList) {
                        if (!currMatchingIDSet.contains(mr.getTrajID()))
                            currMatchingIDSet.add(mr.getTrajID());
                        else LOGGER.severe("ERROR! The current trajectory is matched twice: " + mr.getTrajID());
                    }
                    List<TrajectoryMatchingResult> prevMatchingResult = csvMatchedTrajectoryReader.readMatchedResult(CACHE_FOLDER, iteration - 1);
                    Map<String, TrajectoryMatchingResult> id2PrevMatchingResult = new HashMap<>();
                    for (TrajectoryMatchingResult mr : prevMatchingResult) {
                        if (currMatchingIDSet.contains(mr.getTrajID())) {
                            if (!id2PrevMatchingResult.containsKey(mr.getTrajID()))
                                id2PrevMatchingResult.put(mr.getTrajID(), mr);
                            else LOGGER.severe("ERROR! The same trajectory matching result occurred twice: " + mr.getTrajID());
                        } else
                            unchangedResultList.add(mr);
                    }
                    if (id2PrevMatchingResult.size() != currMatchingIDSet.size())
                        LOGGER.severe("ERROR! The new matching result cannot match to the old ones: " + currMatchingIDSet.size() + "," + id2PrevMatchingResult.size());
                    coOptimizationFunc.influenceScoreGen(currMatchingResultList, id2PrevMatchingResult, roadMap);
                    currMatchingResultList.addAll(unchangedResultList);
                    return new Pair<>(currMatchingResultList, unmatchedTrajInfo);
                } else {
                    LOGGER.severe("ERROR! Partial map-matching should not happen in the initialization step.");
                    return new Pair<>(currMatchingResultList, unmatchedTrajInfo);
                }
            case "refinement":  // rematch after map refinement
                return new Pair<>(currMatchingResultList, unmatchedTrajInfo);
            default:
                LOGGER.severe("ERROR! The match type is unknown: " + matchType);
                return new Pair<>(currMatchingResultList, unmatchedTrajInfo);
        }
    }

    /**
     * The main entry of map-matching algorithm for global dataset.
     *
     * @return map-matched trajectory result
     * @throws IOException file reading error
     */
    private static List<TrajectoryMatchingResult> mapMatchingGlobal(XMLTrajectoryReader reader) throws IOException {
        CSVRawMapReader mapReader = new CSVRawMapReader(ROOT_PATH + "input/");
        List<TrajectoryMatchingResult> results = new ArrayList<>();
        int trajPointCount = 0;
        for (int i = 0; i < reader.getNumOfTrajectory(); i++) {
            Trajectory currTraj = reader.readInputTrajectory(i);
            Iterator<TrajectoryPoint> iterator = currTraj.getSTPoints().iterator();
            TrajectoryPoint prevPoint = iterator.next();
            while (iterator.hasNext()) {
                TrajectoryPoint currPoint = iterator.next();
                if (currPoint.time() <= prevPoint.time())
                    currTraj.remove(currPoint);
                else prevPoint = currPoint;
            }
            trajPointCount += currTraj.size();
            RoadNetworkGraph currMap = mapReader.readRawMap(i);
            TrajectoryMatchingResult matchResult = startGlobalTrajectoryMatching(currTraj, currMap);
            results.add(matchResult);
        }
        System.out.println("Total number of trajectory points is " + trajPointCount);
        CSVTrajectoryWriter matchingResultWriter = new CSVTrajectoryWriter(OUTPUT_FOLDER);
        matchingResultWriter.writeMatchedTrajectory(results, 1, -1);
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
        NewsonHMM2009 mapMatching = new NewsonHMM2009(CANDIDATE_RANGE, GAP_EXTENSION_RANGE, 1, roadMap, false);
        return mapMatching.trajectorySingleMatchingProcess(trajectory);
    }

    /**
     * For each new road in the current map, we find all trajectories which is close to the new road.
     *
     * @param newWayList           List of new roads
     * @param trajectoryPointIndex Trajectory point R-Tree index.
     * @return A set of trajectory IDs.
     */
    private static LinkedHashSet<String> trajectoryIDSearch(List<RoadWay> newWayList, STRTree<Point> trajectoryPointIndex) {
        LinkedHashSet<String> trajIDSet = new LinkedHashSet<>();
        int roadCount = 0;
        for (RoadWay w : newWayList) {
            Rect boundingBox = findBoundingBox(w, BB_FACTOR);
            List<STRNode<Point>> resultPartitionList = trajectoryPointIndex.rangePartitionSearch(boundingBox);
            if (resultPartitionList != null) {
                roadCount++;
                for (STRNode<Point> n : resultPartitionList) {
                    for (XYObject<Point> p : n.getObjectsList()) {
                        if (boundingBox.contains(p.x(), p.y()))
                            trajIDSet.add(p.getSpatialObject().getID());
                    }
                }
            }
        }
//        System.out.println("Adjacent trajectory search finished, Total number of successful road search: " + roadCount);
        return trajIDSet;
    }

    /**
     * Find the minimum bounding box for a given road way. The bounding box can be extended according to the given factor for broader
     * search range.
     *
     * @param way    The query road way.
     * @param factor The enlarge factor. 1= normal size.
     * @return The MBB rectangle.
     */
    private static Rect findBoundingBox(RoadWay way, double factor) {
        GreatCircleDistanceFunction distFunc = new GreatCircleDistanceFunction();
        double minLon = way.getNode(0).lon();
        double maxLon = way.getNode(0).lon();
        double minLat = way.getNode(0).lat();
        double maxLat = way.getNode(0).lat();
        for (int i = 1; i < way.getNodes().size(); i++) {
            RoadNode roadNode = way.getNodes().get(i);
            if (roadNode.lon() < minLon)
                minLon = roadNode.lon();
            if (roadNode.lon() > maxLon)
                maxLon = roadNode.lon();
            if (roadNode.lat() < minLat)
                minLat = roadNode.lat();
            if (roadNode.lat() > maxLat)
                maxLat = roadNode.lat();
        }
        if (minLon == maxLon && minLat == maxLat)
            throw new IllegalArgumentException("ERROR! The bounding box for road: \n" + way.toString() + "\n is illegal: " + minLon +
                    "," + maxLat);
        minLon -= distFunc.coordinateOffset(CANDIDATE_RANGE) * factor;
        maxLon += distFunc.coordinateOffset(CANDIDATE_RANGE) * factor;
        minLat -= distFunc.coordinateOffset(CANDIDATE_RANGE) * factor;
        maxLat += distFunc.coordinateOffset(CANDIDATE_RANGE) * factor;
        return new Rect(minLon, minLat, maxLon, maxLat);
    }
}