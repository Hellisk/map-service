package edu.uq.dke.mapupdate.util.io;

import edu.uq.dke.mapupdate.util.object.roadnetwork.RoadNetworkGraph;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import static edu.uq.dke.mapupdate.Main.*;

public class DataPreprocessing {
    /**
     * The data preprocessing step for Beijing dataset, including broken map generation and trajectory filter
     *
     * @throws IOException file read error
     */
    public static void dataPreprocessing(boolean isGeneratedMatchingResult) throws IOException {

        // pre-processing step 1: read entire ground truth map from csv file and select the bounded area
        System.out.println("Start extracting the map from the ground-truth and resizing it by the bounding box");
        CSVMapReader rawMapReader = new CSVMapReader(GT_MAP);
        RoadNetworkGraph roadNetworkGraph = rawMapReader.extractMapWithBoundary(BOUNDING_BOX);
        CSVMapWriter rawGTMapWriter = new CSVMapWriter(roadNetworkGraph, INPUT_MAP);
        rawGTMapWriter.writeMap(0, -1, false);
//        RawFileOperation trajFilter = new RawFileOperation(TRAJECTORY_COUNT, MIN_TRAJ_POINT_COUNT, MAX_TIME_INTERVAL);
//        trajFilter.groundTruthMatchResultStatistics(roadNetworkGraph, RAW_TRAJECTORY);

        // pre-processing step 2: read and filter raw trajectories, filtered trajectories are guaranteed to be matched on given size of
        // road map
        System.out.println("Start the trajectory filtering.");
        RawFileOperation trajFilter = new RawFileOperation(TRAJECTORY_COUNT, MIN_TRAJ_POINT_COUNT, MAX_TIME_INTERVAL);
        trajFilter.rawTrajectoryParser(roadNetworkGraph, RAW_TRAJECTORY, INPUT_TRAJECTORY, GT_MATCHING_RESULT, 2 * SIGMA,
                isGeneratedMatchingResult);

        // pre-processing step 3: road map removal, remove road ways from ground truth map to generate an outdated map
        System.out.println("Start manipulating the map according to the given road removal percentage:" + PERCENTAGE);
        CSVMapWriter mapRemovalWriter = new CSVMapWriter(roadNetworkGraph, INPUT_MAP);
        mapRemovalWriter.popularityBasedRoadRemoval(PERCENTAGE, CANDIDATE_RANGE / 2);
    }

    /**
     * Initialize the entire Beijing road map, set the visit frequency of each edge.
     *
     * @throws IOException file read error
     */
    public static void rawMapInitialization(boolean isGeneratedMatchingResult) throws IOException, ExecutionException, InterruptedException {

        // pre-processing step 1: read raw map shape file and convert into csv file with default boundaries
        System.out.println("Start reading the raw road map from SHP file.");
        double[] boundingBox = new double[0];
        RawMapReader shpReader = new RawMapReader(RAW_MAP, boundingBox);
        RoadNetworkGraph roadNetworkGraph = shpReader.readNewBeijingMap();
        RawFileOperation trajFilter = new RawFileOperation(-1, -1, -1);
        if (isGeneratedMatchingResult) {
            trajFilter.generateGTMatchingResult(roadNetworkGraph, RAW_TRAJECTORY, 2 * SIGMA);
        }
        trajFilter.trajectoryVisitAssignment(roadNetworkGraph, RAW_TRAJECTORY, isGeneratedMatchingResult);
        // write the visited map to the ground truth folder
        CSVMapWriter rawGTMapWriter = new CSVMapWriter(roadNetworkGraph, GT_MAP);
        rawGTMapWriter.writeMap(0, -1, false);
    }
}
