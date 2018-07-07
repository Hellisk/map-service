package edu.uq.dke.mapupdate.util.io;

import edu.uq.dke.mapupdate.util.object.roadnetwork.RoadNetworkGraph;

import java.io.IOException;

import static edu.uq.dke.mapupdate.Main.*;

public class DataPreprocessing {
    /**
     * The data preprocessing step for Beijing dataset, including broken map generation and trajectory filter
     *
     * @throws IOException file read error
     */
    public static void dataPreprocessing() throws IOException {

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
        trajFilter.rawTrajectoryParser(roadNetworkGraph, RAW_TRAJECTORY, INPUT_TRAJECTORY, GT_MATCHING_RESULT, 2 * SIGMA);

        // pre-processing step 3: road map removal, remove road ways from ground truth map to generate an outdated map
        System.out.println("Start manipulating the map according to the given road removal percentage:" + PERCENTAGE);
//        CSVMapReader visitedMapReader = new CSVMapReader(INPUT_MAP);
//        RoadNetworkGraph visitedGraph = visitedMapReader.readMap(0);
        CSVMapWriter mapRemovalWriter = new CSVMapWriter(roadNetworkGraph, INPUT_MAP);
        mapRemovalWriter.popularityBasedRoadRemoval(PERCENTAGE);
    }

    /**
     * Initialize the entire Beijing road map, set the visit frequency of each edge.
     *
     * @throws IOException file read error
     */
    public static void rawMapInitialization() throws IOException {

        // pre-processing step 1: read raw map shape file and convert into csv file with default boundaries
        System.out.println("Start reading the raw road map from SHP file and extract the map enclosed by the bounding box");
        double[] boundingBox = new double[0];
        RawMapReader shpReader = new RawMapReader(RAW_MAP, boundingBox);
        RoadNetworkGraph roadNetworkGraph = shpReader.readNewBeijingMap();
        RawFileOperation trajFilter = new RawFileOperation(-1, -1, -1);
        trajFilter.trajectoryVisitAssignment(roadNetworkGraph, RAW_TRAJECTORY);
        // write the visited map to the ground truth folder
        CSVMapWriter rawGTMapWriter = new CSVMapWriter(roadNetworkGraph, GT_MAP);
        rawGTMapWriter.writeMap(0, -1, false);
    }
}
