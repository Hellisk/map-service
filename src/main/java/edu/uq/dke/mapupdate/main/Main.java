package edu.uq.dke.mapupdate.main;

import edu.uq.dke.mapupdate.datatype.MatchingResult;
import edu.uq.dke.mapupdate.io.*;
import edu.uq.dke.mapupdate.mapmatching.hmm.NewsonHMM2009;
import edu.uq.dke.mapupdate.visualisation.UnfoldingGraphDisplay;
import traminer.util.map.roadnetwork.RoadNetworkGraph;
import traminer.util.trajectory.Trajectory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

//import edu.uq.dke.mapupdate.mapmatching.hmm.NewsonHMM2009;


public class Main {

    // global parameters
//    private static String ROOT_PATH = "C:/data/trajectorydata/";    // the root folder of all data
    private final static String ROOT_PATH = "F:/data/trajectorydata/";
    private final static int PERCENTAGE = 0;     // percentage of removed road ways (max = 100)
    private final static boolean IS_BROKEN_MAP = true; // use broken map as input

    // paths for different datasets
    private final static String RAW_MAP = ROOT_PATH + "raw/map/";
    private final static String RAW_TRAJECTORY = ROOT_PATH + "raw/trajectory/";
    private final static String GT_MAP = ROOT_PATH + "groundTruth/map/";  // ground-truth road network
    private final static String GT_MATCHING_RESULT = ROOT_PATH + "groundTruth/matchingResult/";   // the map-matched trajectory dataset
    private final static String INPUT_MAP = ROOT_PATH + "input/map/"; // the map with removed roads
    private final static String INPUT_TRAJECTORY = ROOT_PATH + "input/trajectory/";    // input trajectory dataset
    private final static String OUTPUT_FOLDER = ROOT_PATH + "output/";
    private final static String OUTPUT_MAP = ROOT_PATH + "output/map/";

//    // log-related settings
//    private static String LOG_PATH = ROOT_PATH + "log/";

    public static void main(String[] args) {

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
        String inputMap = "";
        String outputMap = OUTPUT_MAP;
        String matchingResult = OUTPUT_FOLDER;
        if (IS_BROKEN_MAP) {
            inputMap = INPUT_MAP;
        } else {
            inputMap = GT_MAP;
        }

        // preprocess the data
//        dataProcessing();
//        endTime = System.currentTimeMillis();
//        System.out.println("Initialisation done, start the iteration:" + (endTime - startTime) / 1000 + "seconds");
//        startTime = endTime;
//
//        // read input map and trajectories
//        CSVMapReader csvMapReader = new CSVMapReader(inputMap);
//        RoadNetworkGraph initialMap = csvMapReader.readMap(PERCENTAGE);
//        CSVTrajectoryReader csvTrajectoryReader = new CSVTrajectoryReader();
//        List<Trajectory> initialTrajectoryList = csvTrajectoryReader.readTrajectoryFilesList(INPUT_TRAJECTORY);
//
////        double costFunction = 2;
//
//        // iteration 0 calculate the first matching result
//        List<MatchingResult> initialMatchingResults = startMapmatching(initialTrajectoryList, initialMap);
//        endTime = System.currentTimeMillis();
//        System.out.println("Map matching finished, total time spent:" + (endTime - startTime) / 1000 + "seconds");
//        startTime = endTime;

        // iteration start
//        while (costFunction > 0) {
//
//            if (IS_BROKEN_MAP) {
//                // step 1: map inference:
//                BiagioniKDE2012 mapInference = new BiagioniKDE2012();
//                mapInference.KDEMapInferenceProcess(initialTrajectoryList, initialMap, OUTPUT_MAP);
//                System.out.println("Map inference finished, total time spent:" + (endTime - startTime) / 1000 + "seconds");
//                startTime = endTime;

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
//            List<MatchingResult> matchingResults = NewsonHMM2009.mapMatchingProcess(initialTrajectoryList, initialMap, unmatchedTraj);
//            endTime = System.currentTimeMillis();
//            System.out.println("Map matching finished, total time spent:" + (endTime - startTime) / 1000 + "seconds");
//            startTime = endTime;
//

//            // overall display
//            GraphStreamDisplay graphDisplay = new GraphStreamDisplay();
////
//            // inferred map reader
//            CSVMapReader manipulatedMapReader = new CSVMapReader(outputMap + i + "/" + CITY_NAME + "_vertices.txt", outputMap + i + "/" + CITY_NAME + "_edges.txt");
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
////                // trajectory display
////                graphDisplay.setRawTrajectories(rawTrajOne);    // one trajectory as raw
////                graphDisplay.setMatchedTrajectories(matchedTrajOne);    // one trajectory as matched
//
//            graphDisplay.setGroundTruthGraph(removedGraph);  // ground truth map
////                graphDisplay.setRawTrajectories(unmatchedSegmentList);
//            graphDisplay.setRoadNetworkGraph(inferenceMap);
//////                graphDisplay.setCentralPoint(matchedTrajOne.get(0).getNode(100).toPoint());
//            Viewer viewer = graphDisplay.generateGraph().display(false);
//            if (graphDisplay.getCentralPoint() != null) {
//                View view = viewer.getDefaultView();
//                view.getCamera().setViewCenter(graphDisplay.getCentralPoint().x(), graphDisplay.getCentralPoint().y(), 0);
//                view.getCamera().setViewPercent(0.35);
//            }
////                break;
//        }

        // evaluation
//        // evaluation: map matching evaluation
//        CSVTrajectoryReader groundTruthMatchingResultReader = new CSVTrajectoryReader();
////        List<MatchingResult> initialMatchingResults = groundTruthMatchingResultReader.readMatchingResult(OUTPUT_FOLDER);
//        List<Pair<Integer, List<String>>> gtMatchingResult = groundTruthMatchingResultReader.readGroundTruthMatchingResult(GT_MATCHING_RESULT);
//        TrajMatchingEvaluation trajMatchingEvaluation = new TrajMatchingEvaluation();
//        trajMatchingEvaluation.precisionRecallCalc(initialMatchingResults, gtMatchingResult);

        UnfoldingGraphDisplay graphDisplay = new UnfoldingGraphDisplay();
        graphDisplay.display();
//        // visualization
//        GraphStreamDisplay graphDisplay = new GraphStreamDisplay();
//        graphDisplay.setGroundTruthGraph(initialMap);  // ground truth map
//
//        // raw trajectory
//        List<Trajectory> displayTraj = new ArrayList<>();
//        displayTraj.add(initialTrajectoryList.get(0));
//        graphDisplay.setRawTrajectories(displayTraj);
//
//        // match result
//        List<List<PointNodePair>> matchResult = new ArrayList<>();
//        matchResult.add(initialMatchingResults.get(0).getMatchingResult());
//        graphDisplay.setMatchedTrajectories(matchResult);
//
//        graphDisplay.setCentralPoint(initialTrajectoryList.get(0).get(5));
//        Viewer viewer = graphDisplay.generateGraph().display(false);
//        if (graphDisplay.getCentralPoint() != null) {
//            View view = viewer.getDefaultView();
//            view.getCamera().setViewCenter(graphDisplay.getCentralPoint().x(), graphDisplay.getCentralPoint().y(), 0);
//            view.getCamera().setViewPercent(0.35);
//        }

        endTime = System.currentTimeMillis();
        System.out.println("Task finish, total time spent:" + (endTime - startTime) / 1000 + " seconds");
    }

    private static List<MatchingResult> startMapmatching(List<Trajectory> initialTrajectoryList, RoadNetworkGraph initialMap) {
        List<Trajectory> unmatchedTraj = new ArrayList<>();
        NewsonHMM2009 mapMatching = new NewsonHMM2009();
        List<MatchingResult> initialMatchingResults = mapMatching.mapMatchingProcess(initialTrajectoryList, initialMap);
        unmatchedTraj = mapMatching.getUnmatchedTraj();
        CSVTrajectoryWriter matchingResultWriter = new CSVTrajectoryWriter(OUTPUT_FOLDER);
        matchingResultWriter.matchedTrajectoryWriter(initialMatchingResults);
        CSVTrajectoryWriter unmatchedTrajWriter = new CSVTrajectoryWriter(OUTPUT_FOLDER);
        unmatchedTrajWriter.trajectoryWriter(unmatchedTraj);
        return initialMatchingResults;
    }

    private static void dataProcessing() throws IOException {

        // preprocessing step 1: read raw map shape file and convert into csv file with default boundaries
        SHPMapReader shpReader = new SHPMapReader(RAW_MAP + "Nbeijing_point.shp", RAW_MAP + "Rbeijing_polyline.shp");
        RoadNetworkGraph roadNetworkGraph = shpReader.readSHP();
        CSVMapWriter rawGTMapWriter = new CSVMapWriter(roadNetworkGraph, GT_MAP);
        rawGTMapWriter.writeMap(0);
        CSVMapReader reader = new CSVMapReader(GT_MAP);
        RoadNetworkGraph cleanedGTMap = reader.readMap(0);
        CSVMapWriter cleanedGTMapWriter = new CSVMapWriter(cleanedGTMap, GT_MAP);
        cleanedGTMapWriter.writeMap(0);

        // preprocessing step 2: read and filter raw trajectories, filtered trajectories are guaranteed to be matched on given size of road map
        RawFileOperation trajFilter = new RawFileOperation(100);
        trajFilter.RawTrajectoryParser(GT_MAP, RAW_TRAJECTORY, INPUT_TRAJECTORY, GT_MATCHING_RESULT);

        // preprocessing step 3: road map removal, remove road ways from ground truth map to generate an outdated map
        CSVMapReader groundTruthMapReader = new CSVMapReader(GT_MAP);
        RoadNetworkGraph graph = groundTruthMapReader.readMap(0);
        CSVMapWriter mapRemovalWriter = new CSVMapWriter(graph, INPUT_MAP);
        mapRemovalWriter.randomBasedRoadRemoval(PERCENTAGE);
    }
}