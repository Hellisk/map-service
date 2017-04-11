package edu.uq.dke.mapupd.mapinference;

import edu.uq.dke.mapupd.io.TrajectoryLoader;
import edu.uq.dke.mapupd.lib.SpatialFunc;

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
    private static int tripMax = 0;

    public static void DavieKDE(String[] args) {
        List<TrajectoryLoader.Trajectory> trajList = new ArrayList<>();
        File newFile = new File("F:/OneDrive/data/trips");
        TrajectoryLoader trajLoader = new TrajectoryLoader();
        trajList = trajLoader.getAllTraj(newFile);
        tripMax = trajList.size();

        List<String> doubleOptsList = new ArrayList<String>();

        // initialise globals
        List<TrajectoryLoader.GPSPoint> pointList = new ArrayList<>();
        for (TrajectoryLoader.Trajectory traj : trajList) {
            pointList.addAll(traj.getPointList());
        }
        List<Double> latitudeList = new ArrayList<>();
        List<Double> longitudeList = new ArrayList<>();
        double maxLat = -Double.MAX_VALUE;
        double maxLon = -Double.MAX_VALUE;
        double minLat = Double.MAX_VALUE;
        double minLon = Double.MAX_VALUE;

        for (TrajectoryLoader.GPSPoint point : pointList) {
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
                                tripMax = Integer.parseInt(args[i + 1]);
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
                    System.err.println("Illegal parameter usage:" + args.toString());
                    return;
                }
            }
        }
    }

    // 0 = North, 2 = East, 4 = South, 6 = West
    private int getSector(int fromX, int fromY, int toX, int toY) {
        double angle = Math.atan2(toY - fromY, toX - fromX);

        // subtract pi/8 to align better with cardinal directions
        return (int) (-angle / (Math.PI / 4) + 2) % 8;
    }

    /*    private HashMap<Object,Object> pairwise(Iterator it) {
            HashMap<Object,Object> pairs = new HashMap<>();

            while(it.hasNext()) {
                pairs.put(it.next(),it.);
            }
        }*/

}
