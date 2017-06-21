package edu.uq.dke.mapupdate.mapinference.algorithm;


import edu.uq.dke.mapupdate.lib.SpatialFunc;
import edu.uq.dke.mapupdate.mapinference.io.TrajectoryLoader;
import edu.uq.dke.mapupdate.mapinference.io.TrajectoryLoader.GPSPoint;
import edu.uq.dke.mapupdate.mapinference.io.TrajectoryLoader.Trajectory;
import javafx.util.Pair;

import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Hellisk on 4/9/2017.
 */
public class DaviesKDE2006 {

    //required parameters
    private static int cellSize = 2; // meters
    private static int maskThreshold = 100; //turns grayscale into binary
    private static int gaussianBlur = 17;
    private static int voronoiSamplingInterval = 10; //sample one point every so many pixels along the outline
    private static int minDirCount = 10;
    private static float shaveUntil = 0.9999f;
    private static int trajMax = 0;
    private static String pathBase = "F:/OneDrive/data/";

    public static void DavieKDE(String[] args) {

        // read trajectory files, each file corresponds to a trajectory
        List<Trajectory> trajList = new ArrayList<>();
        File tripDir = new File(pathBase + "trips");
        TrajectoryLoader trajLoader = new TrajectoryLoader();
        trajList = trajLoader.getAllTraj(tripDir);
        trajMax = trajList.size();

        // initialise globals
        List<GPSPoint> pointList = new ArrayList<>();   // the global list for storing all points
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
        // find geographical boarder of the data
        minLat += -0.003;
        maxLat += 0.003;
        minLon += -0.005;
        maxLon += 0.005;
        double diffLat = maxLat - minLat;
        double diffLon = maxLon - minLon;
        //System.out.println("The margin of bounding box are:" + maxLat + "," + minLat + "," + maxLon + "," + minLon);
        int width = (int) (diffLon * SpatialFunc.METERS_PER_DEGREE_LONGITUDE / cellSize);   // how many cells in total horizontally
        int height = (int) (diffLat * SpatialFunc.METERS_PER_DEGREE_LATITUDE / cellSize);   // how many cells in total vertically
        double yScale = height / diffLat;   // how many cells per degree of latitude
        double xScale = width / diffLon;    // how many cells per degree of longitude

        // aggregate intensity map for all traces
        short[][] denseIntMap = new short[height][width];
        BufferedImage intMapImg = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
//        graphTest(width, height);

        // aggregate intensity map for all traces, split by sector heading
        short[][][] sectorMaps = new short[8][height][width];
        // build an aggregate intensity map from all the edges
        String fileName = pathBase + "cache/n" + trajMax + "_c" + cellSize + ".csv";
        if (new File(fileName).exists()) {
            System.out.println("Found cached intensity map" + "n" + trajMax + "_c" + cellSize + ".csv" + ", loading");
            denseIntMap = readIntMap(fileName, height, width);
            for (int i = 0; i < 8; i++) {
                sectorMaps[i] = readIntMap(pathBase + "cache/n" + trajMax + "_c" + cellSize + "_s" + i + ".csv", height, width);
            }
        } else {
            System.out.println("Making new intensity map:" + fileName);
//            byte[][][] sectorTraMap = new byte[8][height][width];
            short[][][] sectorLineMap = new short[8][height][width];

            for (int i = 0; i < trajMax; i++) {

                // pick up every line segment of a trajectory
                for (Pair<GPSPoint, GPSPoint> trajPair : pairWise(trajList.get(i).getPointList())) {

                    // get the cell locations of original/destination points.
                    int oy = height - (int) (yScale * (trajPair.getKey().getLatitude() - minLat));
                    int ox = (int) (xScale * (trajPair.getKey().getLongitude() - minLon));
                    int dy = height - (int) (yScale * (trajPair.getValue().getLatitude() - minLat));
                    int dx = (int) (xScale * (trajPair.getValue().getLongitude() - minLon));

                    // add line into the global line map and the sector line map.
                    denseIntMap = addLine(ox, oy, dx, dy, denseIntMap);
                    int sector = getSector(ox, oy, dx, dy);
                    sectorLineMap[sector] = addLine(ox, oy, dx, dy, sectorLineMap[sector]);

                }
                System.out.println("New trajectory added");
            }
            saveIntMap(fileName, denseIntMap);
            for (int i = 0; i < 8; i++) {
                saveIntMap(pathBase + "cache/n" + trajMax + "_c" + cellSize + "_s" + i + ".csv", sectorLineMap[i]);
            }
        }
    }

//    private static void graphTest(int width, int height) {
//
//
//        intMapImg.getGraphics().drawLine(12,12,20,20);
//        try {
//            ImageIO.write(intMapImg,"JPG",new File("F:/OneDrive/data/test.jpg"));
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

    private static void saveIntMap(String fileName, short[][] intMap) {
        File intMapFile = new File(fileName);
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(intMapFile));
            for (short[] intMapLine : intMap) {
                StringBuilder line = new StringBuilder();
                for (int i = 0; i < intMapLine.length - 1; i++) {
                    line.append(intMapLine[i]).append(",");
                }
                line.append(intMapLine[intMapLine.length - 1]).append("\n");
                bw.write(line.toString());
            }
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // draw a new line into the existing map matrix
    private static short[][] addLine(int ox, int oy, int dx, int dy, short[][] lineMap) {
        if (dy == oy && dx == ox) {
            System.out.println("Error: Line between two same points");
        } else if (dy == oy) {
            for (int j = dx > ox ? ox : dx; j <= (dx > ox ? dx : ox); j++) {
                lineMap[dy][j] += 1;
            }
        } else if (dx == ox) {
            for (int i = dy > oy ? oy : dy; i <= (dy > oy ? dy : oy); i++) {
                lineMap[i][dx] += 1;
            }
        } else {
            for (int i = 0; i < lineMap.length; i++) {
                for (int j = 0; j < lineMap[i].length; j++) {
                    if ((i - oy) / (dy - oy) == (j - ox) / (dx - ox)) { // the point falls on that line
                        lineMap[i][j] += 1;
                    }
                }
            }
        }
        return lineMap;
    }

    private static short[][] readIntMap(String fileName, int height, int width) {
        File intMapFile = new File(fileName);
        short[][] resultMap = new short[height][width];
        try {
            BufferedReader br = new BufferedReader(new FileReader(intMapFile));
            String line;
            int lineCount = 0;
            while ((line = br.readLine()) != null) {
                String[] record = line.split(",");
                if (record.length == width) {
                    for (int i = 0; i < width; i++) {
                        resultMap[lineCount][i] = Byte.parseByte(record[i]);
                    }
                } else System.out.println("Incorrect input file, record mismatched");
                lineCount++;
            }
            if (lineCount != height) System.out.println("Incorrect input file, record gross is wrong");
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return resultMap;

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
        return sector >= 0 ? sector : sector + 8;
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
