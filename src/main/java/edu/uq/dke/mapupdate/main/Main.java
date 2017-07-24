package edu.uq.dke.mapupdate.main;

import edu.uq.dke.mapupdate.evaluation.algorithm.MapMatchingEvaluation;
import edu.uq.dke.mapupdate.io.CSVMapReader;
import edu.uq.dke.mapupdate.visualisation.GraphStreamDisplay;
import org.graphstream.ui.view.View;
import org.graphstream.ui.view.Viewer;
import org.jdom2.JDOMException;
import traminer.util.map.roadnetwork.RoadNetworkGraph;

import java.io.IOException;

//import edu.uq.dke.mapupdate.mapmatching.algorithm.YouzeFastMatching2012;

public class Main {

    // global parameters
    private static String CITY_NAME = "beijing";
    private static String BASE_PATH = "C:/data/trajectorydata/raw/";
    private static String ROOT_PATH = "C:/data/trajectorydata/";
    private static String PURIFIED_BASE_PATH = "C:/data/trajectorydata/purified/";
    //    private static String BASE_PATH = "F:/data/trajectorydata/raw/";
//    private static String ROOT_PATH = "F:/data/trajectorydata/";
//    private static String PURIFIED_BASE_PATH = "F:/data/trajectorydata/purified/";
    //    private static String BASE_PATH = "/media/dragon_data/uqpchao/trajectory_map_data/";
    private static boolean IS_UPDATE = true;
    private static int PERCENTAGE = 5;     // percentage of removed road ways (max = 100)

    // all pair shortest path folder
    private static String GROUND_TRUTH_MAP_PATH = ROOT_PATH + "maps/map_" + CITY_NAME + "/";
    private static String INPUT_TRAJECTORY_PATH = ROOT_PATH + "tracks/" + CITY_NAME + "/trips/";
    private static String GROUND_TRUTH_TRAJECTORY_PATH = ROOT_PATH + "groundTruthTrajectories/" + CITY_NAME + "/trips/";

    private static String MANIPULATED_MAP_PATH = BASE_PATH + "manipulatedMaps/map_" + CITY_NAME + "/" + PERCENTAGE + "/";
    private static String OUTPUT_INFERRED_MAP_PATH = BASE_PATH + "inferredMaps/map_" + CITY_NAME + "/" + PERCENTAGE + "/";
    private static String OUTPUT_MANIPULATED_INFERRED_MAP_PATH = BASE_PATH + "manipulatedInferredMaps/map_" + CITY_NAME + "/" + PERCENTAGE + "/";
    private static String OUTPUT_MATCHED_TRAJECTORY_PATH = BASE_PATH + "outputTrajectories/" + CITY_NAME + "/trips/";
    private static String OUTPUT_MANIPULATED_MATCHED_TRAJECTORY_PATH = BASE_PATH + "manipulatedTrajectories/" + CITY_NAME + "/" + PERCENTAGE + "/trips/";
    private static String OUTPUT_UNMATCHED_PATH = BASE_PATH + "outputUnmatchedTrajectories/" + CITY_NAME + "/trips/";
    private static String OUTPUT_MANIPULATED_UNMATCHED_PATH = BASE_PATH + "manipulatedUnmatchedTrajectories/" + CITY_NAME + "/" + PERCENTAGE + "/trips/";
    private static String OUTPUT_MAP_PATH = BASE_PATH + "outputMaps/" + CITY_NAME + "/";
    private static String ALL_PAIR_PATH = BASE_PATH + "allPairSPFiles/" + CITY_NAME + "/";
    private static String MANIPULATED_ALL_PAIR_PATH = BASE_PATH + "manipulatedAllPairSPFiles/" + CITY_NAME + "/" + PERCENTAGE + "/";

    // purified paths
    private static String PURIFIED_GROUND_TRUTH_MAP_PATH = PURIFIED_BASE_PATH + "maps/map_" + CITY_NAME + "/";
    private static String PURIFIED_MANIPULATED_MAP_PATH = PURIFIED_BASE_PATH + "manipulatedMaps/map_" + CITY_NAME + "/" + PERCENTAGE + "/";
    private static String PURIFIED_OUTPUT_INFERRED_MAP_PATH = PURIFIED_BASE_PATH + "inferredMaps/map_" + CITY_NAME + "/" + PERCENTAGE + "/";
    private static String PURIFIED_OUTPUT_MANIPULATED_INFERRED_MAP_PATH = PURIFIED_BASE_PATH + "manipulatedInferredMaps/map_" + CITY_NAME + "/" + PERCENTAGE + "/";
    private static String PURIFIED_OUTPUT_MATCHED_TRAJECTORY_PATH = PURIFIED_BASE_PATH + "outputTrajectories/" + CITY_NAME + "/trips/";
    private static String PURIFIED_OUTPUT_MANIPULATED_MATCHED_TRAJECTORY_PATH = PURIFIED_BASE_PATH + "manipulatedTrajectories/" + CITY_NAME + "/" + PERCENTAGE + "/trips/";
    private static String PURIFIED_OUTPUT_UNMATCHED_PATH = PURIFIED_BASE_PATH + "outputUnmatchedTrajectories/" + CITY_NAME + "/trips/";
    private static String PURIFIED_OUTPUT_MANIPULATED_UNMATCHED_PATH = PURIFIED_BASE_PATH + "manipulatedUnmatchedTrajectories/" + CITY_NAME + "/" + PERCENTAGE + "/trips/";
    private static String PURIFIED_OUTPUT_MAP_PATH = PURIFIED_BASE_PATH + "outputMaps/" + CITY_NAME + "/";
    private static String PURIFIED_ALL_PAIR_PATH = PURIFIED_BASE_PATH + "allPairSPFiles/" + CITY_NAME + "/";
    private static String PURIFIED_MANIPULATED_ALL_PAIR_PATH = PURIFIED_BASE_PATH + "manipulatedAllPairSPFiles/" + CITY_NAME + "/" + PERCENTAGE + "/";

    private static String BEIJING_RAW_MAP_PATH = BASE_PATH + "rawMaps/beijing/";

    // log-related settings
    private static String LOG_PATH = BASE_PATH + "log/";

    public static void main(String[] args) throws JDOMException, IOException {

//        // logger handler
//        final Logger logger = Logger.getLogger("MapUpdate");
//        FileHandler handler;
//        try {
//            File logFile = new File(LOG_PATH + "MapUpdate.log");
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
        long startTime = System.currentTimeMillis();
        long endTime;

//        // preprocessing step 1: read raw map shape file and convert into csv file with default boundaries
//        SHPMapReader reader = new SHPMapReader(BEIJING_RAW_MAP_PATH + "Nbeijing_point.shp", BEIJING_RAW_MAP_PATH + "Rbeijing_polyline.shp");
//        RoadNetworkGraph roadNetworkGraph = reader.readSHP();
//        CSVMapWriter writer = new CSVMapWriter(roadNetworkGraph, GROUND_TRUTH_MAP_PATH + "beijing_vertices.txt", GROUND_TRUTH_MAP_PATH + "beijing_edges.txt");
//        writer.writeShapeCSV();

//        // preprocessing step 2: read and filter raw trajectories, filtered trajectories are guaranteed to be matched on given size of road map
//        RawFileOperation trajFilter = new RawFileOperation(10000);
//        trajFilter.RawTrajectoryParser(GROUND_TRUTH_MAP_PATH, ROOT_PATH, INPUT_TRAJECTORY_PATH, GROUND_TRUTH_TRAJECTORY_PATH);

//        // preprocessing step 3: further refine the ground truth map, remove all road ways that are not traveled by trajectories
//        CSVMapReader groundTruthMapReader = new CSVMapReader(GROUND_TRUTH_MAP_PATH + CITY_NAME + "_vertices.txt", GROUND_TRUTH_MAP_PATH + CITY_NAME + "_edges.txt");
//        RoadNetworkGraph groundTruthMap = groundTruthMapReader.readShapeCSV();
//        CSVTrajectoryReader groundTruthTrajReader = new CSVTrajectoryReader();
//        List<RoadWay> groundTruthTrajectories = groundTruthTrajReader.readMatchedTrajectoryFilesList(GROUND_TRUTH_TRAJECTORY_PATH);
//        RoadNetworkGraph purifiedMap = CSVMapWriter.purifyMap(groundTruthTrajectories, groundTruthMap);
//        CSVMapWriter mapWriter = new CSVMapWriter(purifiedMap, PURIFIED_GROUND_TRUTH_MAP_PATH + CITY_NAME + "_vertices.txt", PURIFIED_GROUND_TRUTH_MAP_PATH + CITY_NAME + "_edges.txt");
//        mapWriter.writeShapeCSV();

//        // preprocessing step 4: manipulate map, remove road ways from existing complete map
//        CSVMapReader mapReader = new CSVMapReader(PURIFIED_GROUND_TRUTH_MAP_PATH + CITY_NAME + "_vertices.txt", PURIFIED_GROUND_TRUTH_MAP_PATH + CITY_NAME + "_edges.txt");
//        RoadNetworkGraph graph = mapReader.readShapeCSV();
//        CSVMapWriter mapManipulateWriter = new CSVMapWriter(graph, PURIFIED_MANIPULATED_MAP_PATH + CITY_NAME + "_vertices.txt", PURIFIED_MANIPULATED_MAP_PATH + CITY_NAME + "_edges.txt", PURIFIED_MANIPULATED_MAP_PATH + CITY_NAME + "_removed_edges.txt");
//        mapManipulateWriter.areaBasedMapManipulation(PERCENTAGE);

//        // preprocessing step 5: all pair shortest path calculation
//        AllPairsShortestPathFile allPairSPGen = new AllPairsShortestPathFile(CITY_NAME, PURIFIED_GROUND_TRUTH_MAP_PATH, true);
//        allPairSPGen.writeShortestPathFiles(PURIFIED_ALL_PAIR_PATH);

//        // step 1: map matching:
//        List<Trajectory> unmatchedTrajList = YouzeFastMatching2012.YouzeFastMatching(CITY_NAME, INPUT_TRAJECTORY_PATH, PURIFIED_MANIPULATED_MAP_PATH, PURIFIED_OUTPUT_MANIPULATED_MATCHED_TRAJECTORY_PATH, PURIFIED_MANIPULATED_ALL_PAIR_PATH, IS_UPDATE);
//        CSVTrajectoryWriter.trajectoryWriter(unmatchedTrajList, PURIFIED_OUTPUT_MANIPULATED_UNMATCHED_PATH);
//        endTime = System.currentTimeMillis();
//        System.out.println("Map matching finished, total time spent:" + (endTime - startTime) / 1000 + "seconds");
//        startTime = endTime;

//
//        // step 2: map inference:
//        RoadNetworkGraph inferenceGraph = AhmedTraceMerge2012.AhmedTraceMerge(CITY_NAME, PURIFIED_OUTPUT_MANIPULATED_UNMATCHED_PATH);
//        CSVMapWriter inferenceMapWriter = new CSVMapWriter(inferenceGraph, PURIFIED_OUTPUT_MANIPULATED_INFERRED_MAP_PATH + CITY_NAME + "_vertices.txt", PURIFIED_OUTPUT_MANIPULATED_INFERRED_MAP_PATH + CITY_NAME + "_edges.txt");
//        inferenceMapWriter.writeShapeCSV();

//        // step 3: map merge
//
//        // evaluation: map matching evaluation
//        CSVTrajectoryReader reader = new CSVTrajectoryReader();
//        List<RoadWay> matchedTrajectories = reader.readMatchedTrajectoryFilesList(PURIFIED_OUTPUT_MANIPULATED_MATCHED_TRAJECTORY_PATH);
//        List<RoadWay> groundTruthTrajectories = reader.readMatchedTrajectoryFilesList(GROUND_TRUTH_TRAJECTORY_PATH);
//        TrajMatchingEvaluation trajMatchingEvaluation = new TrajMatchingEvaluation();
//        trajMatchingEvaluation.precisionRecallCalc(matchedTrajectories, groundTruthTrajectories);

        // evaluation: map inference evaluation
        String groundTruthVertexPath = PURIFIED_GROUND_TRUTH_MAP_PATH + CITY_NAME + "_vertices.txt";
        String groundTruthEdgePath = PURIFIED_GROUND_TRUTH_MAP_PATH + CITY_NAME + "_edges.txt";
        String removedEdgesPath = PURIFIED_MANIPULATED_MAP_PATH + CITY_NAME + "_removed_edges.txt";
        String inferredVertexPath = PURIFIED_OUTPUT_MANIPULATED_INFERRED_MAP_PATH + CITY_NAME + "_vertices.txt";
        String inferredEdgePath = PURIFIED_OUTPUT_MANIPULATED_INFERRED_MAP_PATH + CITY_NAME + "_edges.txt";
        CSVMapReader groundTruthMapReader = new CSVMapReader(groundTruthVertexPath, groundTruthEdgePath);
        RoadNetworkGraph groundTruthMap;
        if (CITY_NAME.equals("beijing")) {
            groundTruthMap = groundTruthMapReader.readShapeCSV();
        } else {
            groundTruthMap = groundTruthMapReader.readCSV();
        }
        CSVMapReader removedEdgeReader = new CSVMapReader("", removedEdgesPath);
        RoadNetworkGraph removedMap = removedEdgeReader.readRemovedEdgeCSV();
        CSVMapReader inferredMapReader = new CSVMapReader(inferredVertexPath, inferredEdgePath);
        RoadNetworkGraph inferredMap = inferredMapReader.readShapeCSV();
        MapMatchingEvaluation mapMatchingEvaluation = new MapMatchingEvaluation(10);
        mapMatchingEvaluation.precisionRecallEval(inferredMap, removedMap, groundTruthMap);

        // display
//        String groundTruthVertexPath = PURIFIED_GROUND_TRUTH_MAP_PATH + CITY_NAME + "_vertices.txt";
//        String groundTruthEdgePath = PURIFIED_GROUND_TRUTH_MAP_PATH + CITY_NAME + "_edges.txt";
//        CSVMapReader groundTruthMapReader = new CSVMapReader(groundTruthVertexPath, groundTruthEdgePath);
//        RoadNetworkGraph groundTruthMap;
//        if (CITY_NAME.equals("beijing")) {
//            groundTruthMap = groundTruthMapReader.readShapeCSV();
//        } else {
//            groundTruthMap = groundTruthMapReader.readCSV();
//        }
//        String removedEdgePath = PURIFIED_MANIPULATED_MAP_PATH + CITY_NAME + "_removed_edges.txt";
//        CSVMapReader removedMapReader = new CSVMapReader("", removedEdgePath);
//        RoadNetworkGraph removedMap = removedMapReader.readRemovedEdgeCSV();

//        String manipulatedMapVertexPath = PURIFIED_GROUND_TRUTH_MAP_PATH + CITY_NAME + "_vertices.txt";
//        String manipulatedMapEdgePath = PURIFIED_GROUND_TRUTH_MAP_PATH + CITY_NAME + "_edges.txt";
//        CSVMapReader manipulatedMapReader = new CSVMapReader(manipulatedMapVertexPath, manipulatedMapEdgePath);
//        RoadNetworkGraph manipulatedMap = manipulatedMapReader.readShapeCSV();

//        CSVMapReader manipulatedMapReader = new CSVMapReader(PURIFIED_OUTPUT_MANIPULATED_INFERRED_MAP_PATH + CITY_NAME + "_vertices.txt", PURIFIED_OUTPUT_MANIPULATED_INFERRED_MAP_PATH + CITY_NAME + "_edges.txt");
//        RoadNetworkGraph inferenceMap = manipulatedMapReader.readShapeCSV();

//        CSVTrajectoryReader rawTrajReader = new CSVTrajectoryReader();
//        List<Trajectory> rawTraj = rawTrajReader.readTrajectoryFilesList(INPUT_TRAJECTORY_PATH);
//        List<Trajectory> rawTrajOne = new ArrayList<>();
//        rawTrajOne.add(rawTraj.get(13));
//        CSVTrajectoryReader matchedTrajReader = new CSVTrajectoryReader();
//        List<RoadWay> matchedTraj = matchedTrajReader.readMatchedTrajectoryFilesList(PURIFIED_OUTPUT_MANIPULATED_MATCHED_TRAJECTORY_PATH);
//        List<RoadWay> matchedTrajOne = new ArrayList<>();
//        matchedTrajOne.add(matchedTraj.get(13));

//        CSVTrajectoryReader unmatchedManipulatedTrajReader = new CSVTrajectoryReader();
//        List<Trajectory> unmatchedManipulatedTraj = unmatchedManipulatedTrajReader.readTrajectoryFilesList(PURIFIED_OUTPUT_MANIPULATED_UNMATCHED_PATH);
//
        GraphStreamDisplay graphDisplay = new GraphStreamDisplay();
        graphDisplay.setGroundTruthGraph(groundTruthMap);
        graphDisplay.setRoadNetworkGraph(inferredMap);
//        graphDisplay.setMatchedTrajectories(matchedTrajOne);
//        graphDisplay.setCentralPoint(matchedTrajOne.get(0).getNode(100).toPoint());
        Viewer viewer = graphDisplay.generateGraph().display(false);
        if (graphDisplay.getCentralPoint() != null) {
            View view = viewer.getDefaultView();
            view.getCamera().setViewCenter(graphDisplay.getCentralPoint().x(), graphDisplay.getCentralPoint().y(), 0);
            view.getCamera().setViewPercent(0.35);
        }

        endTime = System.currentTimeMillis();
        System.out.println("Task finish, total time spent:" + (endTime - startTime) / 1000 + "seconds");
    }
}
