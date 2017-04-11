package edu.uq.dke.mapupd.mapinference;

import edu.uq.dke.mapupd.io.TrajectoryLoader;
import edu.uq.dke.mapupd.io.TrajectoryLoader.GPSPoint;
import edu.uq.dke.mapupd.io.TrajectoryLoader.Trajectory;
import edu.uq.dke.mapupd.lib.SpatialFunc;
import javafx.util.Pair;
import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_imgproc;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Hellisk on 4/9/2017.
 */
public class DaviesKDE {

    //required parameters
    private static int cellSize = 2; // meters
    private static int maskThreshold = 100; //turns grayscale into binary
    private static int gaussianBlur = 17;
    private static int voronoiSamplingInterval = 10; //sample one point every so many pixels along the outline
    private static int minDirCount = 10;
    private static float shaveUntil = 0.9999f;
    private static int trajMax = 0;

    public static void DavieKDE(String[] args) {
        List<Trajectory> trajList = new ArrayList<>();
        File newFile = new File("F:/OneDrive/data/trips");
        TrajectoryLoader trajLoader = new TrajectoryLoader();
        trajList = trajLoader.getAllTraj(newFile);
        trajMax = trajList.size();

        List<String> doubleOptsList = new ArrayList<String>();

        // initialise globals
        List<GPSPoint> pointList = new ArrayList<>();
        for (Trajectory traj : trajList) {
            pointList.addAll(traj.getPointList());
        }
        List<Double> latitudeList = new ArrayList<>();
        List<Double> longitudeList = new ArrayList<>();
        double maxLat = -Double.MAX_VALUE;
        double maxLon = -Double.MAX_VALUE;
        double minLat = Double.MAX_VALUE;
        double minLon = Double.MAX_VALUE;

        for (GPSPoint point : pointList) {
            latitudeList.add(point.getLatitude());
            longitudeList.add(point.getLongitude());
            if (point.getLatitude() > maxLat) {
                maxLat = point.getLatitude();
            }
            if (point.getLatitude() < minLat) {
                minLat = point.getLatitude();
            }
            if (point.getLongitude() > maxLon) {
                maxLon = point.getLongitude();
            }
            if (point.getLongitude() < minLon) {
                minLon = point.getLongitude();
            }
        }
        // find bounding box for data
        minLat += -0.003;
        maxLat += 0.003;
        minLon += -0.005;
        maxLon += 0.005;
        double diffLat = maxLat - minLat;
        double diffLon = maxLon - minLat;
        //System.out.println("The margin of bounding box are:" + maxLat + "," + minLat + "," + maxLon + "," + minLon);
        int width = (int) (diffLon * SpatialFunc.METERS_PER_DEGREE_LONGITUDE / cellSize);
        int height = (int) (diffLat * SpatialFunc.METERS_PER_DEGREE_LATITUDE / cellSize);
        double yScale = height / diffLat;
        double xScale = width / diffLon;

        //aggregate intensity map for all traces
        Mat theMap = new Mat(height, width, opencv_core.CV_16UC1, 0);

        //aggregate intensity map for all traces, split by sector heading
        Mat[] sectorMaps = new Mat[8];

        //Build an aggregate intensity map from all the edges
        String fileName = "cache/n" + trajMax + "_c" + cellSize + ".xml";
/*        if(new File(fileName).exists()){
            System.out.println("Found cached intensity map" + fileName + ", loading");
            theMap = opencv_core.;
        }*/
        System.out.println("Making new intensity map:" + fileName);
        Mat[] sectorTemp8 = new Mat[8];
        Mat[] sectorTemp16 = new Mat[8];
        for (int i = 0; i < 8; i++) {
            sectorMaps[i] = new Mat(height, width, opencv_core.CV_16UC1, 0);
            sectorTemp8[i] = new Mat(height, width, opencv_core.CV_8UC1, 0);
            sectorTemp16[i] = new Mat(height, width, opencv_core.CV_16UC1, 0);
        }

        for (int i = 0; i < trajMax; i++) {
            Mat temp8 = new Mat(height, width, opencv_core.CV_8UC1, 0);
            Mat temp16 = new Mat(height, width, opencv_core.CV_16UC1, 0);

            // pairwise the GPS points in one trajectory
            for (Pair<GPSPoint, GPSPoint> trajPair : pairWise(trajList.get(i).getPointList())) {
                int oy = height - (int) (yScale * (trajPair.getKey().getLatitude() - minLat));
                int ox = (int) (xScale * (trajPair.getKey().getLongitude() - minLon));
                int dy = height - (int) (yScale * (trajPair.getValue().getLatitude() - minLat));
                int dx = (int) (xScale * (trajPair.getValue().getLongitude() - minLon));
                opencv_core.Point oriPoint = new opencv_core.Point(ox, oy);
                opencv_core.Point desPoint = new opencv_core.Point(dx, dy);
                opencv_imgproc.line(temp8, oriPoint, desPoint, new opencv_core.Scalar(32), 1, opencv_imgproc.CV_AA, 0);
                int sector = getSector(ox, oy, dx, dy);
                opencv_imgproc.line(sectorTemp8[sector], oriPoint, desPoint, new opencv_core.Scalar(32), 1, opencv_imgproc.CV_AA, 0);

                //add trajectories into theMap
                opencv_core.convertScaleAbs(temp8, temp16, 1, 0);
                opencv_core.add(theMap, temp16, theMap);

                for (i = 0; i < 8; i++) {
                    opencv_core.convertScaleAbs(sectorTemp8[i], sectorTemp16[i], 1, 0);
                    opencv_core.add(sectorMaps[i], sectorTemp16[i], sectorMaps[i]);
                    sectorTemp8[i] = new Mat(height, width, opencv_core.CV_8UC1, 0);
                }
            }
            opencv_core.cvSave(fileName, theMap);
            for (i = 0; i < 8; i++) {
                opencv_core.cvSave("cache/n" + trajMax + "_c" + cellSize + "_s" + sectorMaps[i] + ".xml", sectorMaps[i]);
            }
            Mat lines = new Mat(height, width, opencv_core.CV_8U, 0);
            for (Trajectory traj : trajList) {
                for (Pair<GPSPoint, GPSPoint> trajPair : pairWise(traj.getPointList())) {
                    int oy = height - (int) (yScale * (trajPair.getKey().getLatitude() - minLat));
                    int ox = (int) (xScale * (trajPair.getKey().getLongitude() - minLon));
                    int dy = height - (int) (yScale * (trajPair.getValue().getLatitude() - minLat));
                    int dx = (int) (xScale * (trajPair.getValue().getLongitude() - minLon));
                    opencv_core.Point oriPoint = new opencv_core.Point(ox, oy);
                    opencv_core.Point desPoint = new opencv_core.Point(dx, dy);
                    opencv_imgproc.line(temp8, oriPoint, desPoint, new opencv_core.Scalar(255), 1, opencv_imgproc.CV_AA, 0);
                }
                opencv_core.cvSave("lines.png", lines);
            }
        }
    }

    private void parseArgs(String[] args) {
        // parse parameters
        List<String> argsList = new ArrayList<String>();
        Map<String, List<String>> params = new HashMap<>();
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                if (args[i].charAt(0) == '-') {
                    if (args[i].length() != 2) {
                        System.err.println("Error at argument " + args[i]);
                        return;
                    } else {
                        switch (args[i].charAt(1)) {
                            case 'c':
                                cellSize = Integer.parseInt(args[i + 1]);
                                break;
                            case 't':
                                maskThreshold = Integer.parseInt(args[i + 1]);
                                break;
                            case 'b':
                                gaussianBlur = Integer.parseInt(args[i + 1]);
                                break;
                            case 's':
                                voronoiSamplingInterval = Integer.parseInt(args[i + 1]);
                                break;
                            case 'd':
                                shaveUntil = Float.parseFloat(args[i + 1]);
                                break;
                            case 'n':
                                trajMax = Integer.parseInt(args[i + 1]);
                                break;
                            case 'h':
                                System.out.println("Usage: daviesKDE.jar [-c <cell_size>] [-t <mask_threshold>] [-b <gaussian_blur_size>] [-s <voronoi_sampling_interval>] [-d <shave_until_fraction>] [-n <max_trips>] [-h]\n");
                                break;
                            default:
                                break;
                        }
                        i++;
                    }

                } else {
                    System.err.println("Illegal parameter usage");
                    return;
                }
            }
        }
    }

    // 0 = North, 2 = East, 4 = South, 6 = West
    private static int getSector(int fromX, int fromY, int toX, int toY) {
        double angle = Math.atan2(toY - fromY, toX - fromX);
        int sector = (int) (-angle / (Math.PI / 4) + 2) % 8;
        // subtract pi/8 to align better with cardinal directions
        return sector > 0 ? sector : sector + 8;
    }

    private static List<Pair<GPSPoint, GPSPoint>> pairWise(List<GPSPoint> pointList) {
        List<Pair<GPSPoint, GPSPoint>> pointPairs = new ArrayList<>();
        for (int i = 0; i < pointList.size() - 1; i++) {
            Pair<GPSPoint, GPSPoint> pair = new Pair<>(pointList.get(i), pointList.get(i + 1));
            pointPairs.add(pair);
        }
        return pointPairs;
    }
}
