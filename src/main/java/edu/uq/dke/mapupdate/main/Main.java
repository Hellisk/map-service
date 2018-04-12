package edu.uq.dke.mapupdate.main;

import edu.uq.dke.mapupdate.datatype.TrajectoryMatchResult;
import edu.uq.dke.mapupdate.evaluation.TrajMatchingEvaluation;
import edu.uq.dke.mapupdate.io.*;
import edu.uq.dke.mapupdate.mapmatching.hmm.NewsonHMM2009;
import traminer.util.Pair;
import traminer.util.map.roadnetwork.RoadNetworkGraph;
import traminer.util.trajectory.Trajectory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Main {

    // global parameters
    public final static int PERCENTAGE = 0;         // percentage of removed road ways (max = 100)
    private final static int datasetOption = 0;     // 0 = beijing trajectory, 1 = global trajectory
    private final static boolean workspace = true; // true = home, false = school

    // parameters for KDE-based map inference
    private final static double CELL_SIZE = 1;    // the size of each cell unit, default is 1
    private final static int GAUSSIAN_BLUR = 17;  // Gaussian blur filter, default is 17

    // parameters for HMM-based map matching
    private final static int CANDIDATE_RANGE = 50;
    private final static int GAP_EXTENSION_RANGE = 20;

    private final static String beijingSchoolPath = "C:/data/beijingTrajectory/";       // the root folder of all data
    private final static String beijingHomePath = "F:/data/beijingTrajectory/";         // the root folder of all data
    private final static String globalSchoolPath = "C:/data/evaluationTrajectory/";     // the root folder of all data
    private final static String globalHomePath = "F:/data/evaluationTrajectory/";       // the root folder of all data
    public final static String ROOT_PATH = datasetOption == 0 ? (workspace ? beijingHomePath : beijingSchoolPath) : (workspace ?
            globalHomePath : globalSchoolPath);
    private final static String pythonSchoolPath = "C:/Users/uqpchao/OneDrive/code/github/MapUpdate/src/main/python/";
    private final static String pythonHomePath = "F:/OneDrive/code/github/MapUpdate/src/main/python/";
    private final static String PYTHON_CODE_ROOT_PATH = workspace ? pythonHomePath : pythonSchoolPath;

    // different paths in Beijing dataset
    private final static String RAW_MAP = ROOT_PATH + "raw/map/";
    private final static String RAW_TRAJECTORY = ROOT_PATH + "raw/trajectory/";
    private final static String GT_MAP = ROOT_PATH + "groundTruth/map/";  // ground-truth road network
    private final static String GT_MATCHING_RESULT = ROOT_PATH + "groundTruth/matchingResult/";   // the map-matched trajectory dataset
    private final static String INPUT_MAP = ROOT_PATH + "input/map/"; // the map with removed roads
    private final static String INPUT_TRAJECTORY = ROOT_PATH + "input/trajectory/";    // input trajectory dataset
    private final static String OUTPUT_FOLDER = ROOT_PATH + "output/";

    public static void main(String[] args) throws IOException {

        System.out.println("Co-optimization process start.");
        long startTaskTime = System.currentTimeMillis();
        if (datasetOption == 0) {

            System.out.println("Start working on the beijing dataset...");
            long startTime, endTime;
            startTime = System.currentTimeMillis();

            // preprocessing the data
            System.out.println("Data preprocessing step required. Start the data preprocessing step...");
            dataPreparation();

            endTime = System.currentTimeMillis();
            System.out.println("Initialisation done in " + (endTime - startTime) / 1000 + "seconds" + ", start the map-matching process.");
            startTime = endTime;

            // map-matching process
            List<TrajectoryMatchResult> initialTrajectoryMatchResults = mapMatchingBeijing();

            endTime = System.currentTimeMillis();
            System.out.println("Map matching finished, total time spent:" + (endTime - startTime) / 1000 + "seconds");
            startTime = endTime;

//            // step 1: map inference
//            BiagioniKDE2012 mapInference = new BiagioniKDE2012(CELL_SIZE, GAUSSIAN_BLUR);
//            mapInference.KDEMapInferenceProcess(PYTHON_CODE_ROOT_PATH);
//            System.out.println("Map inference finished, total time spent:" + (endTime - startTime) / 1000 + "seconds");
//            startTime = endTime;

            // evaluation: map matching evaluation
            CSVTrajectoryReader groundTruthMatchingResultReader = new CSVTrajectoryReader();
//            List<TrajectoryMatchResult> initialTrajectoryMatchResults = groundTruthMatchingResultReader.readMatchedResult(OUTPUT_FOLDER);
            List<Pair<Integer, List<String>>> gtMatchingResult = groundTruthMatchingResultReader
                    .readGroundTruthMatchingResult(GT_MATCHING_RESULT);
            TrajMatchingEvaluation trajMatchingEvaluation = new TrajMatchingEvaluation();
            trajMatchingEvaluation.beijingPrecisionRecallCalc(initialTrajectoryMatchResults, gtMatchingResult);

            endTime = System.currentTimeMillis();
            System.out.println("Task finish, total time spent:" + (endTime - startTaskTime) / 1000 + " seconds");

        } else {

            // use global dataset to evaluate the map-matching accuracy
            System.out.println("Start working on the global dataset");
            long startTime = System.currentTimeMillis();

            // map-matching process
            XMLTrajectoryReader reader = new XMLTrajectoryReader(ROOT_PATH + "input/");
            List<TrajectoryMatchResult> trajectoryMatchResults = mapMatchingGlobal(reader);

            long endTime = System.currentTimeMillis();
            System.out.println("Map matching finished, total time spent:" + (endTime - startTime) / 1000 + "seconds");
            startTime = endTime;

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

            endTime = System.currentTimeMillis();
            System.out.println("Task finish, total time spent:" + (endTime - startTime) / 1000 + " seconds");
        }

//        // visualization
//        UnfoldingGraphDisplay graphDisplay = new UnfoldingGraphDisplay();
//        graphDisplay.display();


//
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

        return startTrajectoryListMatching(initialTrajectoryList, initialMap);
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

    private static List<TrajectoryMatchResult> startTrajectoryListMatching(List<Trajectory> initialTrajectoryList, RoadNetworkGraph initialMap) {
        List<Trajectory> unmatchedTraj;
        NewsonHMM2009 mapMatching = new NewsonHMM2009(CANDIDATE_RANGE, GAP_EXTENSION_RANGE);
        List<TrajectoryMatchResult> initialTrajectoryMatchResults = mapMatching.trajectoryListMatchingProcess(initialTrajectoryList, initialMap);
        unmatchedTraj = mapMatching.getUnmatchedTraj();
        CSVTrajectoryWriter matchingResultWriter = new CSVTrajectoryWriter(OUTPUT_FOLDER);
        matchingResultWriter.matchedTrajectoryWriter(initialTrajectoryMatchResults);
        CSVTrajectoryWriter unmatchedTrajWriter = new CSVTrajectoryWriter(OUTPUT_FOLDER);
        unmatchedTrajWriter.trajectoryWriter(unmatchedTraj);
        return initialTrajectoryMatchResults;
    }

    private static TrajectoryMatchResult startTrajectoryMatching(Trajectory trajectory, RoadNetworkGraph roadMap) {
        List<Trajectory> unmatchedTraj;
        NewsonHMM2009 mapMatching = new NewsonHMM2009(CANDIDATE_RANGE, GAP_EXTENSION_RANGE);
        TrajectoryMatchResult matchResult = mapMatching.trajectoryMatchingProcess(trajectory, roadMap);
        List<TrajectoryMatchResult> results = new ArrayList<>();
        results.add(matchResult);
        unmatchedTraj = mapMatching.getUnmatchedTraj();
        CSVTrajectoryWriter matchingResultWriter = new CSVTrajectoryWriter(OUTPUT_FOLDER);
        matchingResultWriter.matchedTrajectoryWriter(results);
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

        // preprocessing step 1: read raw map shape file and convert into csv file with default boundaries
        SHPMapReader shpReader = new SHPMapReader(RAW_MAP + "Nbeijing_point.shp", RAW_MAP + "Rbeijing_polyline.shp");
        RoadNetworkGraph roadNetworkGraph = shpReader.readSHP();
        CSVMapWriter rawGTMapWriter = new CSVMapWriter(roadNetworkGraph, GT_MAP);
        rawGTMapWriter.writeMap(0);

        // preprocessing step 2: read and filter raw trajectories, filtered trajectories are guaranteed to be matched on given size of road map
        RawFileOperation trajFilter = new RawFileOperation(500, 20);
        trajFilter.RawTrajectoryParser(GT_MAP, RAW_TRAJECTORY, INPUT_TRAJECTORY, GT_MATCHING_RESULT);

        // preprocessing step 3: road map removal, remove road ways from ground truth map to generate an outdated map
        CSVMapReader groundTruthMapReader = new CSVMapReader(GT_MAP);
        RoadNetworkGraph graph = groundTruthMapReader.readMap(0);
        CSVMapWriter mapRemovalWriter = new CSVMapWriter(graph, INPUT_MAP);
        mapRemovalWriter.randomBasedRoadRemoval(PERCENTAGE);
    }
}