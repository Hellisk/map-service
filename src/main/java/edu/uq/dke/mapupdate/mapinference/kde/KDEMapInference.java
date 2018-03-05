package edu.uq.dke.mapupdate.mapinference.kde;

import edu.uq.dke.mapupdate.util.SpatialFunc;
import traminer.util.map.roadnetwork.RoadNetworkGraph;
import traminer.util.trajectory.Trajectory;

import java.util.List;

import static org.bytedeco.javacpp.opencv_core.*;
import static org.bytedeco.javacpp.opencv_imgproc.*;

public class KDEMapInference {
    private double cellSize;    // meter
    private int gaussianBlur;

    public KDEMapInference(double cellSize, int gaussianBlur) {
        this.cellSize = cellSize;
        this.gaussianBlur = gaussianBlur;
    }

    public KDEMapInference() {
        this.cellSize = 1;
        this.gaussianBlur = 17;
    }

    public void createKDEWithTrajectories(List<Trajectory> trajList, RoadNetworkGraph roadNetworkGraph, String outputMapPath) {

        // flag to save images
        boolean saveImages = true;

        System.out.println("Finding bounding box...");

//        try {
//            Loader.load(opencv_core.class);
//        } catch (UnsatisfiedLinkError e) {
//            String path = Loader.cacheResource(opencv_core.class, "windows-x86_64/jniopencv_core.dll").getPath();
//            new ProcessBuilder("C:\\data\\depends22_x64\\depends.exe", path).start().waitFor();
//        }

        double minLat = roadNetworkGraph.getMinLat();
        double maxLat = roadNetworkGraph.getMaxLat();
        double minLon = roadNetworkGraph.getMinLon();
        double maxLon = roadNetworkGraph.getMaxLon();

        // find bounding box for data
        minLat -= 0.003;
        maxLat += 0.003;
        minLon -= 0.005;
        maxLon += 0.005;

        double diffLat = maxLat - minLat;
        double diffLon = maxLon - minLon;

        // discretize the map
        int width = (int) (diffLon * SpatialFunc.METERS_PER_DEGREE_LONGITUDE / cellSize);
        int height = (int) (diffLat * SpatialFunc.METERS_PER_DEGREE_LATITUDE / cellSize);
        double yScale = height / diffLat;  // pixels per lat
        double xScale = width / diffLon;   // pixels per lon

        // aggregate intensity map for all traces
        Mat theMap = new Mat(height, width, CV_16UC1);
        theMap.zero();      // not sure whether it works or not

        // build an aggregate intensity map from all the edges
        int tripCount = 1;

        for (Trajectory traj : trajList) {
            if (tripCount % 10 == 0 || tripCount == trajList.size()) {
                System.out.println("Creating histogram (trip " + tripCount + "/" + trajList.size() + ")");
            }
            tripCount++;

            Mat tempMat = new Mat(height, width, CV_8UC1);
            tempMat.zero();
            Mat tempMat16 = new Mat(height, width, CV_16UC1);
            tempMat16.zero();
            System.out.println("test");

            for (int i = 0; i < traj.size() - 1; i++) {
                int oy = height - (int) (yScale * traj.get(i).y() - minLat);
                int ox = (int) (xScale * (traj.get(i).x() - minLon));
                int dy = height - (int) (yScale * (traj.get(i + 1).y() - minLon));
                int dx = (int) (xScale * (traj.get(i + 1).x() - minLon));
                Point op = new Point(ox, oy);
                Point dp = new Point(dx, dy);
                Scalar sc = new Scalar(32);
                line(tempMat, op, dp, sc, 1, CV_AA, 0);
            }

            convertScaleAbs(tempMat, tempMat16, 1, 0);
            add(theMap, tempMat, theMap);
        }

        Mat lines = new Mat(height, width, CV_8U);
        lines.zero();

        System.out.println("done.");

        tripCount = 1;

        for (Trajectory traj : trajList) {
            if (tripCount % 10 == 0 || tripCount == trajList.size()) {
                System.out.println("Creating drawing (trip " + tripCount + "/" + trajList.size() + ")");
            }
            tripCount++;

            for (int i = 0; i < traj.size() - 1; i++) {
                int oy = height - (int) (yScale * traj.get(i).y() - minLat);
                int ox = (int) (xScale * (traj.get(i).x() - minLon));
                int dy = height - (int) (yScale * (traj.get(i + 1).y() - minLon));
                int dx = (int) (xScale * (traj.get(i + 1).x() - minLon));
                Point op = new Point(ox, oy);
                Point dp = new Point(dx, dy);
                Scalar sc = new Scalar(255);
                line(lines, op, dp, sc, 1, CV_AA, 0);
            }

        }
        cvSave(outputMapPath + "raw_data.png", lines);

        System.out.println("done.");
        System.out.println("Smoothing...");
        CvArr theMapArr = new CvArr(theMap);
        cvSmooth(theMapArr, theMapArr, CV_GAUSSIAN, gaussianBlur, gaussianBlur, 0, 0);
        cvSave(outputMapPath + "kde.png", theMapArr);

        System.out.println("done.");
        System.out.println("KDE generation complete.");
    }

}
