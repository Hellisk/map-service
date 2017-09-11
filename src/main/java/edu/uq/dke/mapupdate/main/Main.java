package edu.uq.dke.mapupdate.main;

import edu.uq.dke.mapupdate.evaluation.algorithm.MapMatchingEvaluation;
import edu.uq.dke.mapupdate.io.AllPairsShortestPathFile;
import edu.uq.dke.mapupdate.io.CSVMapReader;
import edu.uq.dke.mapupdate.io.CSVMapWriter;
import edu.uq.dke.mapupdate.io.CSVTrajectoryReader;
import edu.uq.dke.mapupdate.mapinference.algorithm.AhmedTraceMerge2012;
import edu.uq.dke.mapupdate.visualisation.GraphStreamDisplay;
import org.graphstream.ui.view.View;
import org.graphstream.ui.view.Viewer;
import org.jdom2.JDOMException;
import traminer.util.map.roadnetwork.RoadNetworkGraph;
import traminer.util.trajectory.Trajectory;

import java.io.IOException;
import java.util.List;

//import edu.uq.dke.mapupdate.mapmatching.algorithm.YouzeFastMatching2012;

public class Main {

    // global parameters
    private static String CITY_NAME = "beijing";
    //        private static String BASE_PATH = "C:/data/trajectorydata/result/";
//    private static String ROOT_PATH = "C:/data/trajectorydata/";
    private static String BASE_PATH = "F:/data/trajectorydata/result/";
    private static String ROOT_PATH = "F:/data/trajectorydata/";
    //    private static String BASE_PATH = "/media/dragon_data/uqpchao/trajectory_map_data/";
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

//    // purified paths
//    private static String PURIFIED_GROUND_TRUTH_MAP_PATH = PURIFIED_BASE_PATH + "maps/map_" + CITY_NAME + "/";
//    private static String PURIFIED_MANIPULATED_MAP_PATH = PURIFIED_BASE_PATH + "manipulatedMaps/map_" + CITY_NAME + "/" + PERCENTAGE + "/";
//    private static String PURIFIED_OUTPUT_INFERRED_MAP_PATH = PURIFIED_BASE_PATH + "inferredMaps/map_" + CITY_NAME + "/" + PERCENTAGE + "/";
//    private static String PURIFIED_OUTPUT_MANIPULATED_INFERRED_MAP_PATH = PURIFIED_BASE_PATH + "manipulatedInferredMaps/map_" + CITY_NAME + "/" + PERCENTAGE + "/";
//    private static String PURIFIED_OUTPUT_MATCHED_TRAJECTORY_PATH = PURIFIED_BASE_PATH + "outputTrajectories/" + CITY_NAME + "/trips/";
//    private static String PURIFIED_OUTPUT_MANIPULATED_MATCHED_TRAJECTORY_PATH = PURIFIED_BASE_PATH + "manipulatedTrajectories/" + CITY_NAME + "/" + PERCENTAGE + "/trips/";
//    private static String PURIFIED_OUTPUT_UNMATCHED_PATH = PURIFIED_BASE_PATH + "outputUnmatchedTrajectories/" + CITY_NAME + "/trips/";
//    private static String PURIFIED_OUTPUT_MANIPULATED_UNMATCHED_PATH = PURIFIED_BASE_PATH + "manipulatedUnmatchedTrajectories/" + CITY_NAME + "/" + PERCENTAGE + "/trips/";
//    private static String PURIFIED_OUTPUT_MAP_PATH = PURIFIED_BASE_PATH + "outputMaps/" + CITY_NAME + "/";
//    private static String PURIFIED_ALL_PAIR_PATH = PURIFIED_BASE_PATH + "allPairSPFiles/" + CITY_NAME + "/";
//    private static String PURIFIED_MANIPULATED_ALL_PAIR_PATH = PURIFIED_BASE_PATH + "manipulatedAllPairSPFiles/" + CITY_NAME + "/" + PERCENTAGE + "/";

    private static String BEIJING_RAW_MAP_PATH = ROOT_PATH + "rawMaps/beijing/";

    // log-related settings
    private static String LOG_PATH = BASE_PATH + "log/";

    public static void main(String[] args) throws JDOMException, IOException {
        boolean IS_UPDATE = true;   // use iterative process
        boolean IS_MANIPULATED = true; // use broken map as input

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

        // set all path parameters according to the map type
        String groundTruthMap = "";
        String allPairSPFile = "";
        String inferredMap = "";
        String unmatchedTraj = "";
        String matchedResult = "";
        if (IS_MANIPULATED) {
            groundTruthMap = MANIPULATED_MAP_PATH;
            allPairSPFile = MANIPULATED_ALL_PAIR_PATH;
            inferredMap = OUTPUT_MANIPULATED_INFERRED_MAP_PATH;
            unmatchedTraj = OUTPUT_MANIPULATED_UNMATCHED_PATH;
            matchedResult = OUTPUT_MANIPULATED_MATCHED_TRAJECTORY_PATH;
        } else {
            groundTruthMap = GROUND_TRUTH_MAP_PATH;
            allPairSPFile = ALL_PAIR_PATH;
            inferredMap = OUTPUT_INFERRED_MAP_PATH;
            unmatchedTraj = OUTPUT_UNMATCHED_PATH;
            matchedResult = OUTPUT_MATCHED_TRAJECTORY_PATH;
        }

//        // preprocessing step 4: manipulate map, remove road ways from existing complete map
//        CSVMapReader mapReader = new CSVMapReader(GROUND_TRUTH_MAP_PATH + CITY_NAME + "_vertices.txt", GROUND_TRUTH_MAP_PATH + CITY_NAME + "_edges.txt");
//        RoadNetworkGraph graph = mapReader.readShapeCSV();
//        CSVMapWriter mapManipulateWriter = new CSVMapWriter(graph, MANIPULATED_MAP_PATH + CITY_NAME + "_vertices.txt", MANIPULATED_MAP_PATH + CITY_NAME + "_edges.txt", MANIPULATED_MAP_PATH + CITY_NAME + "_removed_edges.txt");
//        mapManipulateWriter.areaBasedMapManipulation(PERCENTAGE);

        // basic graph read
        RoadNetworkGraph originalRoadNetwork;
        String inputVertexPath = groundTruthMap + CITY_NAME + "_vertices.txt";
        String inputEdgePath = groundTruthMap + CITY_NAME + "_edges.txt";
        CSVMapReader csvMapReader = new CSVMapReader(inputVertexPath, inputEdgePath);
        if (CITY_NAME.equals("beijing")) {
            originalRoadNetwork = csvMapReader.readShapeCSV();
        } else {
            originalRoadNetwork = csvMapReader.readCSV();
        }

        // preprocess the data
        if (!dataPreprocessing(originalRoadNetwork, allPairSPFile)) {
            endTime = System.currentTimeMillis();
            System.out.println("Initialisation done, start the iteration:" + (endTime - startTime) / 1000 + "seconds");
            startTime = endTime;

            // iteration start
            RoadNetworkGraph mergedMap = originalRoadNetwork;
            AllPairsShortestPathFile currAllPairSP = new AllPairsShortestPathFile(originalRoadNetwork, 0);
            // read input trajectories
            CSVTrajectoryReader csvTrajectoryReader = new CSVTrajectoryReader();
            List<Trajectory> trajectoryList = csvTrajectoryReader.readTrajectoryFilesList(INPUT_TRAJECTORY_PATH);

            for (int i = 0; i < 1; i++) {
//                // step 0: all pair shortest path update
//                currAllPairSP.readShortestPathFiles(allPairSPFile);
//                endTime = System.currentTimeMillis();
//                System.out.println("Shortest path matrix initialisation done:" + (endTime - startTime) / 1000 + "seconds");
//                startTime = endTime;
//
//                // step 1: map matching:
//                List<Trajectory> unmatchedTrajSegmentList = YouzeFastMatching2012.YouzeFastMatching(trajectoryList, mergedMap, matchedResult + i + "/", currAllPairSP, IS_UPDATE);
//                CSVTrajectoryWriter.trajectoryWriter(unmatchedTrajSegmentList, unmatchedTraj + i + "/");
//                endTime = System.currentTimeMillis();
//                System.out.println("Map matching finished, total time spent:" + (endTime - startTime) / 1000 + "seconds");
//                startTime = endTime;
//
//                // evaluation: map matching evaluation
//                CSVTrajectoryReader reader = new CSVTrajectoryReader();
//                List<RoadWay> matchedTrajectories = reader.readMatchedTrajectoryFilesList(matchedResult + i + "/");
//                List<RoadWay> groundTruthTrajectories = reader.readMatchedTrajectoryFilesList(GROUND_TRUTH_TRAJECTORY_PATH);
//                TrajMatchingEvaluation trajMatchingEvaluation = new TrajMatchingEvaluation();
//                trajMatchingEvaluation.precisionRecallCalc(matchedTrajectories, groundTruthTrajectories);


                if (IS_MANIPULATED) {
                    // step 2: map inference:
                    RoadNetworkGraph inferenceGraph = AhmedTraceMerge2012.AhmedTraceMerge(CITY_NAME, unmatchedTraj + i + "/");
                    CSVMapWriter inferenceMapWriter = new CSVMapWriter(inferenceGraph, inferredMap + i + "/" + CITY_NAME + "_vertices.txt", inferredMap + i + "/" + CITY_NAME + "_edges.txt");
                    inferenceMapWriter.writeShapeCSV();
                    System.out.println("Map inference finished, total time spent:" + (endTime - startTime) / 1000 + "seconds");
                    startTime = endTime;

                    // evaluation: map inference evaluation
                    String removedEdgesPath = groundTruthMap + CITY_NAME + "_removed_edges.txt";
                    CSVMapReader removedEdgeReader = new CSVMapReader("", removedEdgesPath);
                    RoadNetworkGraph removedGraph = removedEdgeReader.readRemovedEdgeCSV();
                    MapMatchingEvaluation mapMatchingEvaluation = new MapMatchingEvaluation(10);
                    mapMatchingEvaluation.precisionRecallEval(inferenceGraph, removedGraph, originalRoadNetwork);
                }

//            // step 3: map merge
//            SPBasedRoadWayFiltering spMapMerge = new SPBasedRoadWayFiltering(mergedMap, inferenceGraph, removedGraph, 64);
//            mergedMap = spMapMerge.SPBasedMapMerge();

                // overall display
                GraphStreamDisplay graphDisplay = new GraphStreamDisplay();
//
                // inferred map reader
                CSVMapReader manipulatedMapReader = new CSVMapReader(inferredMap + i + "/" + CITY_NAME + "_vertices.txt", inferredMap + i + "/" + CITY_NAME + "_edges.txt");
                RoadNetworkGraph inferenceMap = manipulatedMapReader.readShapeCSV();
                graphDisplay.setRoadNetworkGraph(inferenceMap);   // inferred map
                String removedEdgesPath = groundTruthMap + CITY_NAME + "_removed_edges.txt";
                CSVMapReader removedEdgeReader = new CSVMapReader("", removedEdgesPath);
                RoadNetworkGraph removedGraph = removedEdgeReader.readRemovedEdgeCSV();
////
//                // set unmatched trajectory set as input
//                CSVTrajectoryReader unmatchedManipulatedTrajReader = new CSVTrajectoryReader();
//                List<Trajectory> unmatchedManipulatedTraj = unmatchedManipulatedTrajReader.readTrajectoryFilesList(unmatchedTraj + i + "/");
//
//                // trajectory reader
//                CSVTrajectoryReader rawTrajReader = new CSVTrajectoryReader();
//                List<Trajectory> rawTraj = rawTrajReader.readTrajectoryFilesList(INPUT_TRAJECTORY_PATH);
//                CSVTrajectoryReader matchedTrajReader = new CSVTrajectoryReader();
//                List<RoadWay> matchedTraj = matchedTrajReader.readMatchedTrajectoryFilesList(matchedResult + i + "/");
//                List<Trajectory> unmatchedTrajSegmentList = matchedTrajReader.readTrajectoryFilesList( unmatchedTraj + i + "/");
//
//                // set one trajectory as input
//                List<Trajectory> rawTrajOne = new ArrayList<>();
//                rawTrajOne.add(rawTraj.get(13));
//                List<RoadWay> matchedTrajOne = new ArrayList<>();
//                matchedTrajOne.add(matchedTraj.get(13));
//
//                // trajectory display
//                graphDisplay.setRawTrajectories(rawTrajOne);    // one trajectory as raw
//                graphDisplay.setMatchedTrajectories(matchedTrajOne);    // one trajectory as matched

                graphDisplay.setGroundTruthGraph(removedGraph);  // ground truth map
//                graphDisplay.setRawTrajectories(unmatchedTrajSegmentList);
                graphDisplay.setRoadNetworkGraph(inferenceMap);
////                graphDisplay.setCentralPoint(matchedTrajOne.get(0).getNode(100).toPoint());
                Viewer viewer = graphDisplay.generateGraph().display(false);
                if (graphDisplay.getCentralPoint() != null) {
                    View view = viewer.getDefaultView();
                    view.getCamera().setViewCenter(graphDisplay.getCentralPoint().x(), graphDisplay.getCentralPoint().y(), 0);
                    view.getCamera().setViewPercent(0.35);
                }
//                break;
            }
        }
        endTime = System.currentTimeMillis();
        System.out.println("Task finish, total time spent:" + (endTime - startTime) / 1000 + "seconds");
    }

    private static boolean dataPreprocessing(RoadNetworkGraph originalRoadNetwork, String allPairSPFile) throws JDOMException, IOException {
//        // preprocessing step 1: read raw map shape file and convert into csv file with default boundaries
//        SHPMapReader shpReader = new SHPMapReader(BEIJING_RAW_MAP_PATH + "Nbeijing_point.shp", BEIJING_RAW_MAP_PATH + "Rbeijing_polyline.shp");
//        RoadNetworkGraph roadNetworkGraph = shpReader.readSHP();
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

//        // preprocessing step 5: all pair shortest path calculation
//        AllPairsShortestPathFile allPairSPGen = new AllPairsShortestPathFile(originalRoadNetwork);
//        allPairSPGen.writeShortestPathFiles(allPairSPFile);
//        return true;

        return false;
    }
}