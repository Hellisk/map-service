package mapupdate.util.io;

import mapupdate.util.object.roadnetwork.RoadNetworkGraph;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import static mapupdate.Main.*;

public class DataPreprocessing {
    /**
     * The data preprocessing step for Beijing dataset, including broken map generation and trajectory filter
     *
     * @throws IOException file read error
     */
    public static void dataPreprocessing(boolean isManualGTRequired) throws IOException, ExecutionException, InterruptedException {

        LOGGER.info("Start the data preprocessing step, including map resizing, trajectory filtering and map manipulation...");

        // pre-processing step 1: read entire ground truth map from csv file and select the bounded area
        LOGGER.info("Start extracting the map from the ground-truth and resizing it by the bounding box.");
        CSVMapReader rawMapReader = new CSVMapReader(GT_MAP);
        RoadNetworkGraph roadNetworkGraph = rawMapReader.extractMapWithBoundary(BOUNDING_BOX);
        CSVMapWriter rawGTMapWriter = new CSVMapWriter(roadNetworkGraph, INPUT_MAP);
        rawGTMapWriter.writeMap(0, -1, false);
        CSVMapReader resizedMapReader = new CSVMapReader(INPUT_MAP);
        RoadNetworkGraph resizedMap = resizedMapReader.readMap(0, -1, false);

        // pre-processing step 2: read and filter raw trajectories, filtered trajectories are guaranteed to be matched on given size of
        // road map
//        if (isManualGTRequired)
//            LOGGER.info("Start the trajectory filtering and ground-truth result generation.");
//        else LOGGER.info("Start the trajectory filtering.");
        RawFileOperation trajFilter = new RawFileOperation(TRAJECTORY_COUNT, MIN_TRAJ_POINT_COUNT, MAX_TIME_INTERVAL);
        if (isManualGTRequired) {
            double[] emptyBoundingBox = {};
            RoadNetworkGraph rawGrantMap = rawMapReader.extractMapWithBoundary(emptyBoundingBox);
            trajFilter.rawTrajManualGTResultFilter(resizedMap, rawGrantMap);
        } else trajFilter.rawTrajGTResultFilter(resizedMap);

        // pre-processing step 3: road map removal, remove road ways from ground truth map to generate an outdated map
        LOGGER.info("Start manipulating the map according to the given road removal percentage: " + PERCENTAGE);
        CSVMapWriter mapRemovalWriter = new CSVMapWriter(roadNetworkGraph, INPUT_MAP);
        mapRemovalWriter.popularityBasedRoadRemoval(PERCENTAGE, CANDIDATE_RANGE / 2);

        LOGGER.info("Initialisation done. start the map-matching process.");
    }

    /**
     * Initialize the entire Beijing road map, set the visit frequency of each edge.
     *
     * @throws IOException file read error
     */
    public static void rawMapInitialization() throws IOException, ExecutionException, InterruptedException {

        LOGGER.info("Initializing the entire Beijing road map... This step is not required unless the raw data is changed.");

        // pre-processing step 1: read raw map shape file and convert into csv file with default boundaries
        LOGGER.info("Start reading the raw road map from SHP file.");
        double[] boundingBox = new double[0];
        RawMapReader shpReader = new RawMapReader(RAW_MAP, boundingBox);
        RoadNetworkGraph roadNetworkGraph = shpReader.readNewBeijingMap();
        RawFileOperation trajFilter = new RawFileOperation(-1, -1, -1);
        trajFilter.trajectoryVisitAssignment(roadNetworkGraph, RAW_TRAJECTORY);
        // write the visited map to the ground truth folder
        CSVMapWriter rawGTMapWriter = new CSVMapWriter(roadNetworkGraph, GT_MAP);
        rawGTMapWriter.writeMap(0, -1, false);

        LOGGER.info("Initialization done.");
    }
}
