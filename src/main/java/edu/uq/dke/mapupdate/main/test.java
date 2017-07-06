package edu.uq.dke.mapupdate.main;

import org.jdom2.JDOMException;

import java.io.IOException;

/**
 * Created by uqpchao on 4/07/2017.
 * Raw trajectory file format: longitude latitude time
 * Ground truth matched result format: longitude latitude time matchedLineNo
 */
public class test {
    // global parameters
    private static String CITY_NAME = "beijing";
    private static String BASE_PATH = "C:/Users/uqpchao/OneDrive/data/";
    //    private static String BASE_PATH = "F:/OneDrive/data/";
    // all pair shortest path folder
    private static String GROUND_TRUTH_MAP_PATH = BASE_PATH + "maps/map_" + CITY_NAME + "/";
    private static String OUTPUT_SHORTEST_PATH_FOLDER = BASE_PATH + "shortestPath/" + CITY_NAME + "/";
    // map-matching parameters
    private static String INPUT_TRAJECTORY_PATH = BASE_PATH + "tracks/" + CITY_NAME + "/trips/";
    private static String OUTPUT_MATCHED_TRAJECTORY_PATH = BASE_PATH + "outputTrajectorys/" + CITY_NAME + "/trips/";
    private static String OUTPUT_GROUND_TRUTH_TRAJECTORY_PATH = BASE_PATH + "groundTruthTrajectorys/" + CITY_NAME + "/trips/";
    // map inference parameters
    private static String OUTPUT_MAP_PATH = BASE_PATH + "outputMaps/" + CITY_NAME + "/";

    public static void main(String[] arg) throws IOException, JDOMException {

    }
}
