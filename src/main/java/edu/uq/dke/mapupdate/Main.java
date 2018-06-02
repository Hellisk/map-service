package edu.uq.dke.mapupdate;

import edu.uq.dke.mapupdate.evaluation.TrajMatchingEvaluation;
import edu.uq.dke.mapupdate.mapmatching.hmm.NewsonHMM2009;
import edu.uq.dke.mapupdate.util.io.*;
import edu.uq.dke.mapupdate.util.object.datastructure.Pair;
import edu.uq.dke.mapupdate.util.object.datastructure.TrajectoryMatchResult;
import edu.uq.dke.mapupdate.util.object.roadnetwork.RoadNetworkGraph;
import edu.uq.dke.mapupdate.util.object.roadnetwork.RoadNode;
import edu.uq.dke.mapupdate.util.object.spatialobject.Trajectory;
import edu.uq.dke.mapupdate.visualisation.UnfoldingGraphDisplay;
import org.apache.log4j.BasicConfigurator;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Main {

    // global parameters
    public final static int PERCENTAGE = 0;         // percentage of removed road ways (max = 100)
    private final static int DATASET_OPTION = 0;     // 0 = beijing trajectory, 1 = global trajectory, -1 = map comparison
    private final static boolean WORKSPACE = false; // true = home, false = school
    private final static boolean STATISTIC_MODE = false; // true = test and statistics mode, false = normal process
    public final static int TRAJECTORY_COUNT = 100; // total number of trajectories extracted. -1 = extract all
    public final static int MIN_TRAJ_POINT_COUNT = 20; // the minimal number of point required in a trajectory. -1 = no requirement
    public final static int MAX_TIME_INTERVAL = 30; // the maximum time interval within a trajectory -1 = no requirement

    //    public final static double[] BOUNDING_BOX = {};
//    // preset the map boundary, si huan
//    public final static double[] BOUNDING_BOX = {116.20, 116.57, 39.76, 40.03};
    // preset the map boundary, er huan
    public final static double[] BOUNDING_BOX = {116.35, 116.44, 39.895, 39.95};
//    // preset the map boundary, smaller er huan
//    public final static double[] BOUNDING_BOX = {116.400000, 116.433773, 39.950000, 39.980000};

    // parameters for KDE-based map inference
    private final static double CELL_SIZE = 1;    // the size of each cell unit, default is 1
    private final static int GAUSSIAN_BLUR = 17;  // Gaussian blur filter, default is 17

    // parameters for HMM-based map matching
    private final static int NUM_OF_THREADS = 8;    // number of parallel tasks for map-matching
    private final static int CANDIDATE_RANGE = 50;  // the radius of the candidate generation range in meter
    private final static int GAP_EXTENSION_RANGE = 20;  // the trajectory point will be extended as unmatched point if no candidate is
    // within the circle of this radius in meter
    public final static int RANK_LENGTH = 3;  // the number of top-ranked map-matching results to be stored

    private final static String BEIJING_SCHOOL_PATH = "C:/data/beijingTrajectory/";       // the root folder of all data
    private final static String BEIJING_HOME_PATH = "F:/data/beijingTrajectory/";         // the root folder of all data
    private final static String GLOBAL_SCHOOL_PATH = "C:/data/evaluationTrajectory/";     // the root folder of all data
    private final static String GLOBAL_HOME_PATH = "F:/data/evaluationTrajectory/";       // the root folder of all data
    public final static String ROOT_PATH = DATASET_OPTION <= 0 ? (WORKSPACE ? BEIJING_HOME_PATH : BEIJING_SCHOOL_PATH) : (WORKSPACE ?
            GLOBAL_HOME_PATH : GLOBAL_SCHOOL_PATH);
    private final static String PYTHON_SCHOOL_PATH = "C:/Users/uqpchao/OneDrive/code/github/MapUpdate/src/main/python/";
    private final static String PYTHON_HOME_PATH = "F:/OneDrive/code/github/MapUpdate/src/main/python/";
    private final static String PYTHON_CODE_ROOT_PATH = WORKSPACE ? PYTHON_HOME_PATH : PYTHON_SCHOOL_PATH;

    // different paths in Beijing dataset
    public final static String RAW_MAP = ROOT_PATH + "raw/map/";
    public final static String RAW_TRAJECTORY = ROOT_PATH + "raw/trajectory/";
    public final static String GT_MAP = ROOT_PATH + "groundTruth/map/";  // ground-truth road network
    public final static String GT_MATCHING_RESULT = ROOT_PATH + "groundTruth/matchingResult/";   // the map-matched trajectory dataset
    private final static String INPUT_MAP = ROOT_PATH + "input/map/"; // the map with removed roads
    public final static String INPUT_TRAJECTORY = ROOT_PATH + "input/trajectory/";    // input trajectory dataset
    private final static String OUTPUT_FOLDER = ROOT_PATH + "output/";

    public static void main(String[] args) throws IOException, ExecutionException, InterruptedException {

        System.out.println("Co-optimization process start.");
        BasicConfigurator.configure();
        long startTaskTime = System.currentTimeMillis();
        if (DATASET_OPTION == 0) {  // complete algorithm on beijing dataset

            System.out.println("Start working on the beijing dataset...");
            long prevTime = startTaskTime;

            // pre-processing the data
            System.out.println("Data preprocessing step required. Start the data preprocessing step...");
            dataPreparation();
            System.out.println("Initialisation done in " + (System.currentTimeMillis() - prevTime) / 1000 + "seconds" + ", start the " +
                    "map-matching process.");
            prevTime = System.currentTimeMillis();

            // map-matching process
//            Stream<TrajectoryMatchResult> trajMatchingResultStream = parallelMapMatchingBeijing();
            List<TrajectoryMatchResult> trajMatchingResultList = mapMatchingBeijing();
            System.out.println("Map matching finished, total time spent:" + (System.currentTimeMillis() - prevTime) / 1000 + "seconds");
            prevTime = System.currentTimeMillis();
//
//            // step 1: map inference
//            BiagioniKDE2012 mapInference = new BiagioniKDE2012(CELL_SIZE, GAUSSIAN_BLUR);
//            mapInference.KDEMapInferenceProcess(PYTHON_CODE_ROOT_PATH);
//            System.out.println("Map inference finished, total time spent:" + (System.currentTimeMillis() - prevTime) / 1000 + "seconds");
//
            // evaluation: map matching evaluation
            CSVTrajectoryReader groundTruthMatchingResultReader = new CSVTrajectoryReader();
            List<Pair<Integer, List<String>>> gtMatchingResult = groundTruthMatchingResultReader
                    .readGroundTruthMatchingResult(GT_MATCHING_RESULT);
            TrajMatchingEvaluation trajMatchingEvaluation = new TrajMatchingEvaluation();
//            List<TrajectoryMatchResult> trajMatchingResultList = trajMatchingResultStream.collect(Collectors.toList());
//            List<TrajectoryMatchResult> trajMatchResultList = groundTruthMatchingResultReader.readMatchedResult(OUTPUT_FOLDER);
            trajMatchingEvaluation.beijingPrecisionRecallCalc(trajMatchingResultList, gtMatchingResult);
        } else if (DATASET_OPTION == 1) {    // map-matching evaluation dataset

            // use global dataset to evaluate the map-matching accuracy
            System.out.println("Start working on the global dataset");
            long prevTime = startTaskTime;

            // map-matching process
            XMLTrajectoryReader reader = new XMLTrajectoryReader(ROOT_PATH + "input/");
            List<TrajectoryMatchResult> trajectoryMatchResults = mapMatchingGlobal(reader);

            System.out.println("Map matching finished, total time spent:" + (System.currentTimeMillis() - prevTime) / 1000 + "seconds");
            prevTime = System.currentTimeMillis();

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
//            List<TrajectoryMatchResult> trajectoryMatchResults = groundTruthMatchingResultReader.readMatchedResult(OUTPUT_FOLDER);
            TrajMatchingEvaluation trajMatchingEvaluation = new TrajMatchingEvaluation();
            trajMatchingEvaluation.globalPrecisionRecallCalc(trajectoryMatchResults, groundTruthMatchingResult, groundTruthTrajectoryInfo);
        } else {    // other test cases
            System.out.println("Start comparing different versions of Beijing map...");
            long prevTime = startTaskTime;

            RawMapReader newMapReader = new RawMapReader(RAW_MAP, BOUNDING_BOX);
            System.out.println("Start reading the new and old Beijing maps.");
            RoadNetworkGraph newBeijingMap = newMapReader.readNewBeijingMap();
            CSVMapWriter beijingMapWriter = new CSVMapWriter(newBeijingMap, GT_MAP);
            beijingMapWriter.writeMap(0);
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
        }

        // visualization
        UnfoldingGraphDisplay graphDisplay = new UnfoldingGraphDisplay();
        graphDisplay.display();

//        // evaluation: map inference evaluation
//        String removedEdgesPath = initialMap + CITY_NAME + "_removed_edges.txt";
//        CSVMapReader removedEdgeReader = new CSVMapReader("", removedEdgesPath);
//        RoadNetworkGraph removedGraph = removedEdgeReader.readMapEdges();
//        MapMatchingEvaluation mapMatchingEvaluation = new MapMatchingEvaluation(10);
//        mapMatchingEvaluation.precisionRecallEval(inferenceGraph, removedGraph, initialMap);
//
//        // step 3: map merge
//        SPBasedRoadWayFiltering spMapMerge = new SPBasedRoadWayFiltering(mergedMap, inferenceGraph, removedGraph, 64);
//        mergedMap = spMapMerge.SPBasedMapMerge();
//
//        // step 2: map matching:
//        List<Trajectory> unmatchedTraj = new ArrayList<>();
//        List<TrajectoryMatchResult> matchingResults = NewsonHMM2009.trajectoryMatchingProcess(initialTrajectoryList, initialMap, unmatchedTraj);

        System.out.println("Task finish, total time spent:" + (System.currentTimeMillis() - startTaskTime) / 1000 + " seconds");
    }

    /**
     * The main entry of map-matching algorithm for Beijing dataset
     *
     * @return map-matched trajectory result
     * @throws IOException file reading error
     */
    private static List<TrajectoryMatchResult> mapMatchingBeijing() throws IOException {

        // read input map and trajectories
        CSVMapReader csvMapReader;
        if (PERCENTAGE != 0) {
            csvMapReader = new CSVMapReader(INPUT_MAP);
        } else {
            csvMapReader = new CSVMapReader(GT_MAP);
        }
        RoadNetworkGraph initialMap = csvMapReader.readMap(PERCENTAGE);
        CSVTrajectoryReader csvTrajectoryReader = new CSVTrajectoryReader();
        List<Trajectory> initialTrajectoryList = csvTrajectoryReader.readTrajectoryFilesList(INPUT_TRAJECTORY);

        // start matching process
        NewsonHMM2009 mapMatching = new NewsonHMM2009(CANDIDATE_RANGE, GAP_EXTENSION_RANGE, RANK_LENGTH);
        List<TrajectoryMatchResult> trajMatchResults = mapMatching.trajectoryListMatchingProcess(initialTrajectoryList, initialMap);
        List<Trajectory> unmatchedTraj;
        unmatchedTraj = mapMatching.getUnmatchedTraj();

        // write output matching result
        CSVTrajectoryWriter matchingResultWriter = new CSVTrajectoryWriter(OUTPUT_FOLDER);
        matchingResultWriter.matchedTrajectoryWriter(trajMatchResults, RANK_LENGTH);
        CSVTrajectoryWriter unmatchedTrajWriter = new CSVTrajectoryWriter(OUTPUT_FOLDER);
        unmatchedTrajWriter.trajectoryWriter(unmatchedTraj);
        return trajMatchResults;
    }

    /**
     * The main entry of map-matching algorithm for Beijing dataset
     *
     * @return map-matched trajectory result
     * @throws IOException file reading error
     */
    private static Stream<TrajectoryMatchResult> parallelMapMatchingBeijing() throws IOException, ExecutionException, InterruptedException {
        // read input map and trajectories
        CSVMapReader csvMapReader;
        if (PERCENTAGE != 0) {
            csvMapReader = new CSVMapReader(INPUT_MAP);
        } else {
            csvMapReader = new CSVMapReader(GT_MAP);
        }
        RoadNetworkGraph initialMap = csvMapReader.readMap(PERCENTAGE);
        CSVTrajectoryReader csvTrajectoryReader = new CSVTrajectoryReader();
        Stream<Trajectory> initialTrajectoryList = csvTrajectoryReader.readTrajectoryFilesStream(INPUT_TRAJECTORY);

        // start matching process
        NewsonHMM2009 mapMatching = new NewsonHMM2009(CANDIDATE_RANGE, GAP_EXTENSION_RANGE, RANK_LENGTH);
        Stream<TrajectoryMatchResult> trajMatchResults = mapMatching.trajectoryStreamMatchingProcess(initialTrajectoryList, initialMap,
                NUM_OF_THREADS);
        List<Trajectory> unmatchedTraj;
        unmatchedTraj = mapMatching.getUnmatchedTraj();

        // write output matching result
        CSVTrajectoryWriter matchingResultWriter = new CSVTrajectoryWriter(OUTPUT_FOLDER);
        List<TrajectoryMatchResult> trajMatchResultList = trajMatchResults.collect(Collectors.toList());
        matchingResultWriter.matchedTrajectoryWriter(trajMatchResultList, RANK_LENGTH);
        CSVTrajectoryWriter unmatchedTrajWriter = new CSVTrajectoryWriter(OUTPUT_FOLDER);
        unmatchedTrajWriter.trajectoryWriter(unmatchedTraj);
        return trajMatchResults;
    }

    /**
     * The main entry of map-matching algorithm for global dataset
     *
     * @return map-matched trajectory result
     * @throws IOException file reading error
     */
    private static List<TrajectoryMatchResult> mapMatchingGlobal(XMLTrajectoryReader reader) throws IOException {
        CSVRawMapReader mapReader = new CSVRawMapReader(ROOT_PATH + "input/");
        List<TrajectoryMatchResult> results = new ArrayList<>();
        for (int i = 0; i < reader.getNumOfTrajectory(); i++) {
            Trajectory currTraj = reader.readInputTrajectory(i);
            RoadNetworkGraph currMap = mapReader.readRawMap(i);
            TrajectoryMatchResult matchResult = startTrajectoryMatching(currTraj, currMap);
            results.add(matchResult);
        }
        return results;
    }

    /**
     * Perform trajectory map-matching and write the result individually, which is used in global dataset map-matching evaluation
     *
     * @param trajectory input trajectory
     * @param roadMap    underline road network
     * @return map-matching result for trajectory
     */
    private static TrajectoryMatchResult startTrajectoryMatching(Trajectory trajectory, RoadNetworkGraph roadMap) throws IOException {
        List<Trajectory> unmatchedTraj;
        NewsonHMM2009 mapMatching = new NewsonHMM2009(CANDIDATE_RANGE, GAP_EXTENSION_RANGE, RANK_LENGTH);
        TrajectoryMatchResult matchResult = mapMatching.trajectoryMatchingProcess(trajectory, roadMap);
        List<TrajectoryMatchResult> results = new ArrayList<>();
        results.add(matchResult);
        unmatchedTraj = mapMatching.getUnmatchedTraj();
        CSVTrajectoryWriter matchingResultWriter = new CSVTrajectoryWriter(OUTPUT_FOLDER);
        matchingResultWriter.matchedTrajectoryWriter(results, RANK_LENGTH);
        CSVTrajectoryWriter unmatchedTrajWriter = new CSVTrajectoryWriter(OUTPUT_FOLDER);
        unmatchedTrajWriter.trajectoryWriter(unmatchedTraj);
        return matchResult;
    }

    /**
     * The data preprocessing step for Beijing dataset, including broken map generation and trajectory filter
     *
     * @throws IOException file read error
     */
    private static void dataPreparation() throws IOException {

        // pre-processing step 1: read raw map shape file and convert into csv file with default boundaries
        System.out.println("Start reading the raw road map from SHP file.");
        RawMapReader shpReader = new RawMapReader(RAW_MAP, BOUNDING_BOX);
        RoadNetworkGraph roadNetworkGraph = shpReader.readNewBeijingMap();
        CSVMapWriter rawGTMapWriter = new CSVMapWriter(roadNetworkGraph, GT_MAP);
        rawGTMapWriter.writeMap(0);

        // pre-processing step 2: read and filter raw trajectories, filtered trajectories are guaranteed to be matched on given size of
        // road map
        System.out.println("Start the trajectory filtering.");
        RawFileOperation trajFilter = new RawFileOperation(TRAJECTORY_COUNT, MIN_TRAJ_POINT_COUNT, STATISTIC_MODE, MAX_TIME_INTERVAL);
        trajFilter.RawTrajectoryParser(GT_MAP, RAW_TRAJECTORY, INPUT_TRAJECTORY, GT_MATCHING_RESULT);

        // pre-processing step 3: road map removal, remove road ways from ground truth map to generate an outdated map
        System.out.println("Start manipulating the map according to the given road removal percentage:" + PERCENTAGE);
        CSVMapReader groundTruthMapReader = new CSVMapReader(GT_MAP);
        RoadNetworkGraph graph = groundTruthMapReader.readMap(0);
        CSVMapWriter mapRemovalWriter = new CSVMapWriter(graph, INPUT_MAP);
        mapRemovalWriter.randomBasedRoadRemoval(PERCENTAGE);
    }
}