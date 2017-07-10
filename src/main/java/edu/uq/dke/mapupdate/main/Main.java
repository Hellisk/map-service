package edu.uq.dke.mapupdate.main;

import edu.uq.dke.mapupdate.evaluation.algorithm.TrajMatchingEvaluation;
import edu.uq.dke.mapupdate.io.CSVMapReader;
import edu.uq.dke.mapupdate.io.CSVMapWriter;
import edu.uq.dke.mapupdate.io.CSVTrajectoryReader;
import edu.uq.dke.mapupdate.io.CSVTrajectoryWriter;
import edu.uq.dke.mapupdate.mapmatching.algorithm.YouzeFastMatching2012;
import org.jdom2.JDOMException;
import traminer.util.map.roadnetwork.RoadNetworkGraph;
import traminer.util.map.roadnetwork.RoadWay;
import traminer.util.trajectory.Trajectory;

import java.io.IOException;
import java.util.List;

//import edu.uq.dke.mapupdate.mapmatching.algorithm.YouzeFastMatching2012;

public class Main {

    // global parameters
    private static String CITY_NAME = "beijing";
    //    private static String BASE_PATH = "C:/data/trajectorydata/";
    private static String BASE_PATH = "F:/data/trajectorydata/";
    //    private static String BASE_PATH = "/media/dragon_data/uqpchao/trajectory_map_data/";
    private static int PERCENTAGE = 40;     // percentage of removed road ways (max = 100)
    private static boolean IS_UPDATE = true;
    // all pair shortest path folder
    private static String GROUND_TRUTH_MAP_PATH = BASE_PATH + "maps/map_" + CITY_NAME + "/";
    private static String MANIPULATED_MAP_PATH = BASE_PATH + "manipulatedMaps/map_" + CITY_NAME + "/" + PERCENTAGE + "/";
    private static String GROUND_TRUTH_TRAJECTORY_PATH = BASE_PATH + "groundTruthTrajectories/" + CITY_NAME + "/trips/";
    private static String OUTPUT_SHORTEST_PATH_FOLDER = BASE_PATH + "shortestPath/" + CITY_NAME + "/";
    // map-matching parameters
    private static String INPUT_TRAJECTORY_PATH = BASE_PATH + "tracks/" + CITY_NAME + "/trips/";
    private static String OUTPUT_MATCHED_TRAJECTORY_PATH = BASE_PATH + "outputTrajectories/" + CITY_NAME + "/trips/";
    private static String OUTPUT_MANIPULATED_MATCHED_TRAJECTORY_PATH = BASE_PATH + "manipulatedTrajectories/" + CITY_NAME + "/" + PERCENTAGE + "/trips/";
    private static String OUTPUT_UNMATCHED_PATH = BASE_PATH + "outputUnmatchedTrajectories/" + CITY_NAME + "/trips/";
    private static String OUTPUT_MANIPULATED_UNMATCHED_PATH = BASE_PATH + "manipulatedUnmatchedTrajectories/" + CITY_NAME + "/" + PERCENTAGE + "/trips/";
    // map inference parameters
    private static String OUTPUT_MAP_PATH = BASE_PATH + "outputMaps/" + CITY_NAME + "/";

    private static String BEIJING_RAW_MAP_PATH = BASE_PATH + "rawMaps/beijing/";

    /* parameters for Ahmed 2012 */
    public static double AHMED_EPSILON = 150.0;
    // if input file has altitude information
    public static boolean HAS_ALTITUDE = false;
    // minimum altitude difference in meters between two streets
    public static double MIN_ALT_EPS = 4.0;

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

//        // preprocessing step 2: read and filter raw trajectories
//        RawFileOperation trajFilter = new RawFileOperation();
//        trajFilter.RawTrajectoryParser(GROUND_TRUTH_MAP_PATH,BASE_PATH,INPUT_TRAJECTORY_PATH,GROUND_TRUTH_TRAJECTORY_PATH);

        // preprocessing step 3: manipulate map
        CSVMapReader mapReader = new CSVMapReader(GROUND_TRUTH_MAP_PATH + CITY_NAME + "_vertices.txt", GROUND_TRUTH_MAP_PATH + CITY_NAME + "_edges.txt");
        RoadNetworkGraph graph = mapReader.readShapeCSV();
        CSVMapWriter mapWriter = new CSVMapWriter(graph, MANIPULATED_MAP_PATH + CITY_NAME + "_vertices.txt", MANIPULATED_MAP_PATH + CITY_NAME + "_edges.txt", MANIPULATED_MAP_PATH + CITY_NAME + "_removed_edges.txt");
        mapWriter.manipulateMap(PERCENTAGE);

        // step 1: map matching:
        List<Trajectory> unmatchedTrajList = YouzeFastMatching2012.YouzeFastMatching(CITY_NAME, INPUT_TRAJECTORY_PATH, MANIPULATED_MAP_PATH, OUTPUT_MANIPULATED_MATCHED_TRAJECTORY_PATH, IS_UPDATE);
        CSVTrajectoryWriter.trajectoryWriter(unmatchedTrajList, OUTPUT_MANIPULATED_UNMATCHED_PATH);
        // step 2: map inference:
//        AhmedTraceMerge2012.AhmedTraceMerge(CITY_NAME, OUTPUT_MANIPULATED_UNMATCHED_PATH, GROUND_TRUTH_MAP_PATH, OUTPUT_MAP_PATH, AHMED_EPSILON, HAS_ALTITUDE, MIN_ALT_EPS);

        // step 3: map merge

        // evaluation: trajectory matching evaluation
        CSVTrajectoryReader reader = new CSVTrajectoryReader();
        List<RoadWay> matchedTrajectories = reader.readMatchedTrajectoryFilesList(OUTPUT_MANIPULATED_MATCHED_TRAJECTORY_PATH);
        List<RoadWay> groundTruthTrajectories = reader.readMatchedTrajectoryFilesList(GROUND_TRUTH_TRAJECTORY_PATH);
        TrajMatchingEvaluation matchingEvaluation = new TrajMatchingEvaluation();
        matchingEvaluation.trajectoryMatchingEvaluation(matchedTrajectories, groundTruthTrajectories);

//        // display
//        GraphStreamDisplay display = new GraphStreamDisplay();
//        display.setGroundTruthGraph(roadNetworkGraph);
//        display.generateGraph().display(false);

        endTime = System.currentTimeMillis();
        System.out.println("Task finish, total time spent:" + (endTime - startTime) / 1000 + "seconds");
    }
}
