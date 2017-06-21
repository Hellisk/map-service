package edu.uq.dke.mapupdate.main;

import edu.uq.dke.mapupdate.mapmatching.algorithm.YouzeFastMatching2012;
import org.jdom2.JDOMException;

import java.io.File;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

//import edu.uq.dke.mapupdate.mapmatching.algorithm.YouzeFastMatching2012;

public class Main {

    // path to the folder that contains input tracks.
    public static String BASE_PATH = "C:/Users/uqpchao/OneDrive/data/";
    //    public static String BASE_PATH = "F:/OneDrive/data/";
    public static String CITY_NAME = "chicago";
    public static String INPUT_PATH = BASE_PATH + "Pfoser/tracks/" + CITY_NAME + "/trips/";
    public static String OUTPUT_MAP_PATH = BASE_PATH + "Pfoser/outputMaps/" + CITY_NAME + "/";
    public static String OUTPUT_TRAJECTORY_PATH = BASE_PATH + "Pfoser/outputTrajectorys/" + CITY_NAME + "/trips/";

    // parameters for map evaluation

    public static String GROUND_TRUTH_MAP_PATH = BASE_PATH + "Pfoser/maps/map_" + CITY_NAME + "/";
    public static String EVALUATION_RESULT_PATH = BASE_PATH + "Pfoser/evaluation/result/" + CITY_NAME + "/";
    public static String PATH_GENERATION_PATH = BASE_PATH + "Pfoser/evaluation/paths/" + CITY_NAME + "/";
    public static boolean IS_DIRECTED = false;
    public static String BOUNDARY = "()";
    public static String LINK_LENGTH = "LinkThree";

    /* parameters for Ahmed 2012 */
    public static double AHMED_EPSILON = 150.0;
    // if input file has altitude information
    public static boolean HAS_ALTITUDE = false;
    // minimum altitude difference in meters between two streets
    public static double MIN_ALT_EPS = 4.0;

    // log-related settings
    public static String LOG_PATH = BASE_PATH + "log/";

    public static void main(String[] args) throws JDOMException, IOException {

        // logger handler
        final Logger logger = Logger.getLogger("MapUpdate");
        FileHandler handler;
        try {
            File logFile = new File(LOG_PATH + "MapUpdate.log");
            if (!logFile.exists()) {
                logFile.createNewFile();
            }
            handler = new FileHandler(LOG_PATH + "MapUpdate.log", true);
            logger.addHandler(handler);
            SimpleFormatter formatter = new SimpleFormatter();
            handler.setFormatter(formatter);
        } catch (IOException e) {
            e.printStackTrace();
        }

        logger.info("Map Update Algorithm v 0.1.0");
        long startTime = System.currentTimeMillis();
        long endTime;
//        int mapInference = 0;
//        int mapEvaluation = 0;
//
//        // pure test map inference
//        AhmedTraceMerge2012.AhmedTraceMerge(OUTPUT_TRAJECTORY_PATH, OUTPUT_MAP_PATH, AHMED_EPSILON, HAS_ALTITUDE, MIN_ALT_EPS);
//        endTime = System.currentTimeMillis();
//        logger.info("Total map inference time:" + (endTime - startTime) / 1000 + "s");
//        AhmedFrechetEvaluation2013.AhmedFrechetEvaluation(CITY_NAME, OUTPUT_MAP_PATH, GROUND_TRUTH_MAP_PATH, IS_DIRECTED, BOUNDARY, EVALUATION_RESULT_PATH, PATH_GENERATION_PATH, LINK_LENGTH);
//
//        // pure test map matching
        YouzeFastMatching2012.YouzeFastMatching(CITY_NAME, INPUT_PATH, GROUND_TRUTH_MAP_PATH, OUTPUT_TRAJECTORY_PATH, 1);

//        NewsonHMM2009.NewsonHMM(CITY_NAME, INPUT_PATH, GROUND_TRUTH_MAP_PATH, OUTPUT_TRAJECTORY_PATH, 1);
//        if (args.length == 2) {
//            mapInference = Integer.parseInt(args[0]);
//            mapEvaluation = Integer.parseInt(args[1]);
//
//
//            switch (mapInference) {
//                case 1: {
//                    System.out.println("Start the Ahmed Trace Merge 2012.");
//                    logger.info("Start the Ahmed Trace Merge 2012.");
//                    AhmedTraceMerge2012.AhmedTraceMerge(INPUT_PATH, OUTPUT_MAP_PATH, AHMED_EPSILON, HAS_ALTITUDE, MIN_ALT_EPS);
//                    break;
//                }
//                case 2: {
//                    System.out.println("Start the Davies KDE 2006.");
//                    logger.info("Start the Davies KDE 2006.");
//                    DaviesKDE2006.DavieKDE(args);
//                    break;
//                }
//            }
//            endTime = cal.getTimeInMillis();
//            logger.info("Map inference finished, time cost:" + (endTime - startTime) / 1000 + "s");
//        }

    }
}
