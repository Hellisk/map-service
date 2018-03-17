package edu.uq.dke.mapupdate.main;

import edu.uq.dke.mapupdate.datatype.TrajectoryMatchResult;
import edu.uq.dke.mapupdate.evaluation.TrajMatchingEvaluation;
import edu.uq.dke.mapupdate.io.*;
import edu.uq.dke.mapupdate.mapmatching.hmm.NewsonHMM2009;
import edu.uq.dke.mapupdate.visualisation.UnfoldingGraphDisplay;
import traminer.util.Pair;
import traminer.util.map.roadnetwork.RoadNetworkGraph;
import traminer.util.trajectory.Trajectory;

import java.io.IOException;
import java.util.List;

//import edu.uq.dke.mapupdate.mapmatching.hmm.NewsonHMM2009;


public class Main {

    private final static int PERCENTAGE = 0;     // percentage of removed road ways (max = 100)
    private final static boolean OLD_GT_ABANDONED = true; // use broken map as input
    // global parameters
    private static String ROOT_PATH = "C:/data/trajectorydata/";    // the root folder of all data
    //    private final static String ROOT_PATH = "F:/data/trajectorydata/";
    private static String PYTHON_CODE_ROOT_PATH = "C:/Users/uqpchao//OneDrive/code/github/MapUpdate/src/main/python/";
//    private static String PYTHON_CODE_ROOT_PATH = "F:/OneDrive/code/github/MapUpdate/src/main/python/";


    // paths for different datasets
    private final static String RAW_MAP = ROOT_PATH + "raw/map/";
    private final static String RAW_TRAJECTORY = ROOT_PATH + "raw/trajectory/";
    private final static String GT_MAP = ROOT_PATH + "groundTruth/map/";  // ground-truth road network
    private final static String GT_MATCHING_RESULT = ROOT_PATH + "groundTruth/matchingResult/";   // the map-matched trajectory dataset
    private final static String GT_GENERATED_RESULT = ROOT_PATH + "groundTruth/generatedResult/";   // the map-matched trajectory dataset
    private final static String INPUT_MAP = ROOT_PATH + "input/map/"; // the map with removed roads
    private final static String INPUT_TRAJECTORY = ROOT_PATH + "input/trajectory/";    // input trajectory dataset
    private final static String OUTPUT_FOLDER = ROOT_PATH + "output/";
//    private final static String OUTPUT_MAP = ROOT_PATH + "output/map/";

//    // log-related settings
//    private static String LOG_PATH = ROOT_PATH + "log/";

    public static void main(String[] args) throws IOException {

        long startTime = System.currentTimeMillis();
        long endTime;

//        // logger handler
//        final Logger logger = Logger.getLogger("MapUpdate");
//        FileHandler handler;
//        try {
//            File logFile = new File(LOG_PATH + startTime + "MapUpdate.log");
//            if (!logFile.exists()) {
//                logFile.createNewFile();
//            }
//            handler = new FileHandler(LOG_PATH + "MapUpdate.log", true);
//            logger.addHandler(handler);
//            SimpleFormatter formatter = new SimpleFormatter();
//            handler.setFormatter(formatter);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        logger.info("Map Update Algorithm v 0.1.0");

        // set all path parameters according to the map type

        // preprocess the data
//        dataProcessing();
        endTime = System.currentTimeMillis();
        System.out.println("Initialisation done, start the iteration:" + (endTime - startTime) / 1000 + "seconds");
        startTime = endTime;

//        // read input map and trajectories
//        CSVMapReader csvMapReader;
//        if (PERCENTAGE != 0) {
//            csvMapReader = new CSVMapReader(INPUT_MAP);
//        } else {
//            csvMapReader = new CSVMapReader(GT_MAP);
//        }
//        RoadNetworkGraph initialMap = csvMapReader.readMap(PERCENTAGE);
//        CSVTrajectoryReader csvTrajectoryReader = new CSVTrajectoryReader();
//        List<Trajectory> initialTrajectoryList = csvTrajectoryReader.readTrajectoryFilesList(INPUT_TRAJECTORY);
//
////        double costFunction = 2;
//
//        // iteration 0 calculate the first matching result
//        List<TrajectoryMatchResult> initialTrajectoryMatchResults = startMapmatching(initialTrajectoryList, initialMap);
//        endTime = System.currentTimeMillis();
//        System.out.println("Map matching finished, total time spent:" + (endTime - startTime) / 1000 + "seconds");
//        startTime = endTime;

        // iteration start
//        while (costFunction > 0) {
//
//        // step 1: map inference:
//        BiagioniKDE2012 mapInference = new BiagioniKDE2012();
//        mapInference.KDEMapInferenceProcess(PYTHON_CODE_ROOT_PATH);
//        System.out.println("Map inference finished, total time spent:" + (endTime - startTime) / 1000 + "seconds");
//        startTime = endTime;

//                // evaluation: map inference evaluation
//                String removedEdgesPath = initialMap + CITY_NAME + "_removed_edges.txt";
//                CSVMapReader removedEdgeReader = new CSVMapReader("", removedEdgesPath);
//                RoadNetworkGraph removedGraph = removedEdgeReader.readMapEdges();
//                MapMatchingEvaluation mapMatchingEvaluation = new MapMatchingEvaluation(10);
//                mapMatchingEvaluation.precisionRecallEval(inferenceGraph, removedGraph, initialMap);
//            }
//            // step 3: map merge
//            SPBasedRoadWayFiltering spMapMerge = new SPBasedRoadWayFiltering(mergedMap, inferenceGraph, removedGraph, 64);
//            mergedMap = spMapMerge.SPBasedMapMerge();

//            // step 2: map matching:
//            List<Trajectory> unmatchedTraj = new ArrayList<>();
//            List<TrajectoryMatchResult> matchingResults = NewsonHMM2009.mapMatchingProcess(initialTrajectoryList, initialMap, unmatchedTraj);
//            endTime = System.currentTimeMillis();
//            System.out.println("Map matching finished, total time spent:" + (endTime - startTime) / 1000 + "seconds");
//            startTime = endTime;
//

//            // overall display
//            GraphStreamDisplay graphDisplay = new GraphStreamDisplay();
////
//            // inferred map reader
//            CSVMapReader manipulatedMapReader = new CSVMapReader(OUTPUT_MAP + i + "/" + CITY_NAME + "_vertices.txt", OUTPUT_MAP + i + "/" + CITY_NAME + "_edges.txt");
//            RoadNetworkGraph inferenceMap = manipulatedMapReader.readMap();
//            graphDisplay.setRoadNetworkGraph(inferenceMap);   // inferred map
//            String removedEdgesPath = initialMap + CITY_NAME + "_removed_edges.txt";
//            CSVMapReader removedEdgeReader = new CSVMapReader("", removedEdgesPath);
//            RoadNetworkGraph removedGraph = removedEdgeReader.readMapEdges();
////
////                // set unmatched trajectory set as input
////                CSVTrajectoryReader unmatchedManipulatedTrajReader = new CSVTrajectoryReader();
////                List<Trajectory> unmatchedManipulatedTraj = unmatchedManipulatedTrajReader.readTrajectoryFilesList(unmatchedTraj + i + "/");
////
////                // trajectory reader
////                CSVTrajectoryReader rawTrajReader = new CSVTrajectoryReader();
////                List<Trajectory> rawTraj = rawTrajReader.readTrajectoryFilesList(INPUT_TRAJECTORY);
////                CSVTrajectoryReader matchedTrajReader = new CSVTrajectoryReader();
////                List<RoadWay> matchedTraj = matchedTrajReader.readMatchedTrajectoryFilesList(matchingResult + i + "/");
////                List<Trajectory> unmatchedSegmentList = matchedTrajReader.readTrajectoryFilesList( unmatchedTraj + i + "/");
////
////                // set one trajectory as input
////                List<Trajectory> rawTrajOne = new ArrayList<>();
////                rawTrajOne.add(rawTraj.get(13));
////                List<RoadWay> matchedTrajOne = new ArrayList<>();
////                matchedTrajOne.add(matchedTraj.get(13));
////

        // evaluation
        // evaluation: map matching evaluation
        CSVTrajectoryReader groundTruthMatchingResultReader = new CSVTrajectoryReader();
        List<TrajectoryMatchResult> initialTrajectoryMatchResults = groundTruthMatchingResultReader.readMatchedResult(OUTPUT_FOLDER);
        List<Pair<Integer, List<String>>> gtMatchingResult = groundTruthMatchingResultReader.readGroundTruthMatchingResult(GT_MATCHING_RESULT);
        TrajMatchingEvaluation trajMatchingEvaluation = new TrajMatchingEvaluation();
        trajMatchingEvaluation.precisionRecallCalc(initialTrajectoryMatchResults, gtMatchingResult);

        // visualization
        UnfoldingGraphDisplay graphDisplay = new UnfoldingGraphDisplay();
        graphDisplay.display();

        endTime = System.currentTimeMillis();
        System.out.println("Task finish, total time spent:" + (endTime - startTime) / 1000 + " seconds");
    }

    private static List<TrajectoryMatchResult> startMapmatching(List<Trajectory> initialTrajectoryList, RoadNetworkGraph initialMap) {
        List<Trajectory> unmatchedTraj;
        NewsonHMM2009 mapMatching = new NewsonHMM2009();
        List<TrajectoryMatchResult> initialTrajectoryMatchResults = mapMatching.mapMatchingProcess(initialTrajectoryList, initialMap);
        unmatchedTraj = mapMatching.getUnmatchedTraj();
        CSVTrajectoryWriter matchingResultWriter = new CSVTrajectoryWriter(OUTPUT_FOLDER);
        matchingResultWriter.matchedTrajectoryWriter(initialTrajectoryMatchResults);
        CSVTrajectoryWriter unmatchedTrajWriter = new CSVTrajectoryWriter(OUTPUT_FOLDER);
        unmatchedTrajWriter.trajectoryWriter(unmatchedTraj);
        return initialTrajectoryMatchResults;
    }

    private static void dataProcessing() throws IOException {

        // preprocessing step 1: read raw map shape file and convert into csv file with default boundaries
        SHPMapReader shpReader = new SHPMapReader(RAW_MAP + "Nbeijing_point.shp", RAW_MAP + "Rbeijing_polyline.shp");
        RoadNetworkGraph roadNetworkGraph = shpReader.readSHP();
        CSVMapWriter rawGTMapWriter = new CSVMapWriter(roadNetworkGraph, GT_MAP);
        rawGTMapWriter.writeMap(0);

        // preprocessing step 2: read and filter raw trajectories, filtered trajectories are guaranteed to be matched on given size of road map
        RawFileOperation trajFilter = new RawFileOperation(200);
        trajFilter.RawTrajectoryParser(GT_MAP, RAW_TRAJECTORY, INPUT_TRAJECTORY, GT_MATCHING_RESULT);

        // preprocessing step 3: road map removal, remove road ways from ground truth map to generate an outdated map
        CSVMapReader groundTruthMapReader = new CSVMapReader(GT_MAP);
        RoadNetworkGraph graph = groundTruthMapReader.readMap(0);
        CSVMapWriter mapRemovalWriter = new CSVMapWriter(graph, INPUT_MAP);
        mapRemovalWriter.randomBasedRoadRemoval(PERCENTAGE);
    }
}