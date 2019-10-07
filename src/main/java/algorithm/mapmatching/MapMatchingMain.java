package algorithm.mapmatching;

import algorithm.mapmatching.simpleHMM.SimpleHMMMatching;
import algorithm.mapmatching.stmatching.FeatureSTMapMatching;
import algorithm.mapmatching.weightBased.WeightBasedMapMatching;
import org.apache.log4j.Logger;
import util.function.DistanceFunction;
import util.function.GreatCircleDistanceFunction;
import util.io.*;
import util.object.roadnetwork.RoadNetworkGraph;
import util.object.spatialobject.Trajectory;
import util.object.spatialobject.TrajectoryPoint;
import util.object.structure.SimpleTrajectoryMatchResult;
import util.settings.BaseProperty;
import util.settings.MapMatchingProperty;
import util.settings.MapServiceLogger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

/**
 * Entry for running map-matching algorithms and evaluation.
 *
 * @author Hellisk
 * @since 30/03/2019
 */
public class MapMatchingMain {

    public static void main(String[] args) throws IOException, ExecutionException, InterruptedException {

        // initialize arguments
        MapMatchingProperty property = new MapMatchingProperty();
        property.loadPropertiesFromResourceFile("mapmatching.properties", args);
        long initTaskTime = System.currentTimeMillis();

        // setup settings
        String logFolder = property.getPropertyString("algorithm.mapmatching.log.LogFolder");  // obtain the log folder from args
        String dataSet = property.getPropertyString("data.Dataset");
        String inputTrajFolder = property.getPropertyString("path.InputTrajectoryFolder");
        String inputMapFolder = property.getPropertyString("path.InputMapFolder");
        String outputMatchResultFolder = property.getPropertyString("path.OutputMatchResultFolder");
        String matchingMethod = property.getPropertyString("algorithm.mapmatching.MatchingMethod");
        String dataSpec = property.getPropertyString("data.DataSpec");
        int numOfThreads = property.getPropertyInteger("algorithm.mapmatching.NumOfThreads");
        boolean isOnline = matchingMethod.substring(0, 2).equals("ON");        // check if the current matching is online matching
        DistanceFunction distFunc;
        String logFileName;
        String parameters = "";
        // log file name
        switch (matchingMethod.substring(3)) {
            case "HMM":
                parameters = property.getPropertyString("algorithm.mapmatching.hmm.Beta");
                break;
            case "HMM-goh":
                parameters = property.getPropertyString("algorithm.mapmatching.WindowSize");
                break;
            case "HMM-eddy":
                parameters = property.getPropertyString("algorithm.mapmatching.hmm.Eddy.Gamma");
                break;
            case "HMM-fixed":
                parameters = property.getPropertyString("algorithm.mapmatching.WindowSize");
                break;
            case "FST":
                parameters = property.getPropertyString("algorithm.mapmatching.fst.Omega");
                break;
            case "WGT":
                parameters = property.getPropertyString("algorithm.mapmatching.CandidateRange") + "_"
                        + property.getPropertyString("algorithm.mapmatching.Sigma");
                break;
            default:
                parameters = "null";
                break;
        }

        // initialize log file
        logFileName = "matching_" + dataSet + "_" + matchingMethod + "_" + dataSpec + "_" + initTaskTime + "_" + parameters;
        MapServiceLogger.logInit(logFolder, logFileName);

        final Logger LOG = Logger.getLogger(MapMatchingMain.class);
        // use global dataset to evaluate the map-matching accuracy
        LOG.info("Map-matching on the " + dataSet + " dataset using method " + matchingMethod + ".");

        long startTaskTime = System.currentTimeMillis();    // the start of the map-matching process
        // map-matching process
        List<SimpleTrajectoryMatchResult> matchResultList = new ArrayList<>();
        if (dataSet.equals("Global")) {
            String rawDataFolder = property.getPropertyString("path.RawDataFolder");
            int samplingInterval = property.getPropertyInteger("data.global.SamplingInterval");
            GlobalTrajectoryLoader reader = new GlobalTrajectoryLoader(rawDataFolder);
            GlobalMapLoader mapReader = new GlobalMapLoader(rawDataFolder);
            int trajPointCount = 0;
            for (int i = 0; i < reader.getNumOfTrajectory(); i++) {
                Trajectory currTraj;
                if (samplingInterval != -1) {
                    currTraj = reader.readInputTrajectory(i).subSample(samplingInterval);
                } else {
                    currTraj = reader.readInputTrajectory(i);
                }
                Iterator<TrajectoryPoint> iterator = currTraj.getSTPoints().iterator();
                TrajectoryPoint prevPoint = iterator.next();
                while (iterator.hasNext()) {
                    TrajectoryPoint currPoint = iterator.next();
                    if (currPoint.time() <= prevPoint.time()) {
                        LOG.warn("The input time is not ordered: " + currPoint.time() + "," + prevPoint.time());
                        currTraj.remove(currPoint);
                    } else prevPoint = currPoint;
                }
                trajPointCount += currTraj.size();
                RoadNetworkGraph currMap = mapReader.readRawMap(i);
                MapMatchingMethod mapMatching = chooseMatchMethod(matchingMethod, currMap, property);
                SimpleTrajectoryMatchResult matchResult;
                if (isOnline)
                    matchResult = mapMatching.onlineMatching(currTraj)._2();
                else
                    matchResult = mapMatching.offlineMatching(currTraj);

                matchResultList.add(matchResult);
            }
            LOG.info("Map matching finished, total time spent:" + (System.currentTimeMillis() - startTaskTime) / 1000 + "seconds");
            MatchResultWriter.writeMatchResults(matchResultList, outputMatchResultFolder);

            System.out.println("Total number of trajectory points is " + trajPointCount);
        } else if (dataSet.contains("Beijing")) {
            distFunc = new GreatCircleDistanceFunction();
            RoadNetworkGraph roadMap = MapReader.readMap(inputMapFolder + "0.txt", false, distFunc);
            Stream<Trajectory> inputTrajStream = TrajectoryReader.readTrajectoriesToStream(inputTrajFolder, distFunc);
//            List<Trajectory> inputTrajList = TrajectoryReader.readTrajectoriesToList(inputTrajFolder, distFunc);
            MapMatchingMethod mapMatching = chooseMatchMethod(matchingMethod, roadMap, property);
            long loadingTime = System.currentTimeMillis();
            LOG.info("Loading complete, loading time: " + (loadingTime - startTaskTime) / 1000.0 + "s.");
//            matchResultList = mapMatching.sequentialMatching(inputTrajList, isOnline);
            matchResultList = mapMatching.parallelMatching(inputTrajStream, numOfThreads, isOnline);
            MatchResultWriter.writeMatchResults(matchResultList, outputMatchResultFolder);
            LOG.info("Matching complete, matching time: " + (System.currentTimeMillis() - loadingTime) / 1000.0 + "s, total time:" +
                    (System.currentTimeMillis() - startTaskTime) / 1000.0 + "s.");
        }
    }

    public static MapMatchingMethod chooseMatchMethod(String matchingMethod, RoadNetworkGraph roadMap, BaseProperty property) {
        switch (matchingMethod.substring(3, 6)) {
            case "HMM":
                return new SimpleHMMMatching(roadMap, property);
            case "FST":
                return new FeatureSTMapMatching(roadMap, property);
            case "WGT":
                return new WeightBasedMapMatching(roadMap, property);
            default:
                throw new IllegalArgumentException("The matching method is not found: " + matchingMethod);
        }
    }
}
