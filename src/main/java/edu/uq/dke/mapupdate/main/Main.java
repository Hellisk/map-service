package edu.uq.dke.mapupdate.main;

import edu.uq.dke.mapupdate.mapmatching.algorithm.YouzeFastMatching2012;
import org.jdom2.JDOMException;
import traminer.util.trajectory.Trajectory;

import java.io.IOException;
import java.util.List;

//import edu.uq.dke.mapupdate.mapmatching.algorithm.YouzeFastMatching2012;

public class Main {

    // global parameters
    private static String CITY_NAME = "beijing";
    private static String BASE_PATH = "C:/data/trajectorydata/";
    //    private static String BASE_PATH = "F:/data/trajectorydata/";
    //    private static String BASE_PATH = "/media/dragon_data/uqpchao/trajectory_map_data/";
    // all pair shortest path folder
    private static String GROUND_TRUTH_MAP_PATH = BASE_PATH + "maps/map_" + CITY_NAME + "/";
    private static String GROUND_TRUTH_MATCHING_PATH = BASE_PATH + "groundTruthTrajectories/" + CITY_NAME + "/trips/";
    private static String OUTPUT_SHORTEST_PATH_FOLDER = BASE_PATH + "shortestPath/" + CITY_NAME + "/";
    // map-matching parameters
    private static String INPUT_TRAJECTORY_PATH = BASE_PATH + "tracks/" + CITY_NAME + "/trips/";
    private static String OUTPUT_MATCHED_TRAJECTORY_PATH = BASE_PATH + "outputTrajectories/" + CITY_NAME + "/trips/";
    private static String OUTPUT_UNMATCHED_PATH = BASE_PATH + "outputUnmatchedTrajectories/" + CITY_NAME + "/trips/";
    // map inference parameters
    private static String OUTPUT_MAP_PATH = BASE_PATH + "outputMaps/" + CITY_NAME + "/";

    private static String BEIJING_RAW_MAP_PATH = BASE_PATH + "rawMaps/beijing/";

    // parameters for map evaluation
//    private static String EVALUATION_RESULT_PATH = BASE_PATH + "evaluation/result/" + CITY_NAME + "/";
//    private static String PATH_GENERATION_PATH = BASE_PATH + "evaluation/paths/" + CITY_NAME + "/";
//    private static boolean IS_DIRECTED = false;
//    private static String BOUNDARY = "()";
//    private static String LINK_LENGTH = "LinkThree";

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
//        trajFilter.RawTrajectoryParser(GROUND_TRUTH_MAP_PATH,BASE_PATH,INPUT_TRAJECTORY_PATH,GROUND_TRUTH_MATCHING_PATH);

//        // preprocessing step 3: generate all pair shortest path file
//        AllPairsShortestPathFile shortestPathFile = new AllPairsShortestPathFile(CITY_NAME, GROUND_TRUTH_MAP_PATH, true);
//        shortestPathFile.writeShortestPathFiles(OUTPUT_SHORTEST_PATH_FOLDER);

        // step 1: map matching:
        List<Trajectory> unmatchedTrajList = YouzeFastMatching2012.YouzeFastMatching(CITY_NAME, INPUT_TRAJECTORY_PATH, GROUND_TRUTH_MAP_PATH, OUTPUT_MATCHED_TRAJECTORY_PATH, OUTPUT_SHORTEST_PATH_FOLDER, GROUND_TRUTH_MATCHING_PATH, OUTPUT_UNMATCHED_PATH, true);
        // pure test map inference
//        AhmedTraceMerge2012.AhmedTraceMerge(CITY_NAME, INPUT_TRAJECTORY_PATH, GROUND_TRUTH_MAP_PATH, OUTPUT_MAP_PATH, AHMED_EPSILON, HAS_ALTITUDE, MIN_ALT_EPS);
//        endTime = System.currentTimeMillis();
//        logger.info("Total map inference time:" + (endTime - startTime) / 1000 + "s");
//        AhmedFrechetEvaluation2013.AhmedFrechetEvaluation(CITY_NAME, OUTPUT_MAP_PATH, GROUND_TRUTH_MAP_PATH, IS_DIRECTED, BOUNDARY, EVALUATION_RESULT_PATH, PATH_GENERATION_PATH, LINK_LENGTH);

//        // display
//        GraphStreamDisplay display = new GraphStreamDisplay();
//        display.setGroundTruthGraph(roadNetworkGraph);
//        display.generateGraph().display(false);

        endTime = System.currentTimeMillis();
        System.out.println("Task finish, total time spent:" + (endTime - startTime) / 1000 + "seconds");
    }
}
