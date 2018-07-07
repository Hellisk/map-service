package edu.uq.dke.mapupdate.util.io;

import edu.uq.dke.mapupdate.util.function.GreatCircleDistanceFunction;
import edu.uq.dke.mapupdate.util.object.roadnetwork.RoadNetworkGraph;
import edu.uq.dke.mapupdate.util.object.roadnetwork.RoadWay;
import edu.uq.dke.mapupdate.util.object.spatialobject.Trajectory;

import java.io.*;
import java.text.DecimalFormat;
import java.util.*;

import static edu.uq.dke.mapupdate.Main.*;

/**
 * Created by uqpchao on 5/07/2017.
 */
public class RawFileOperation {
    private int requiredRecordNum;
    private int minTrajPointNum;
    private int maxTimeInterval;
    private GreatCircleDistanceFunction distFunc = new GreatCircleDistanceFunction();

    public RawFileOperation(int trajCount, int minTrajPointCount, int maxTimeInterval) {
        this.requiredRecordNum = trajCount;
        this.minTrajPointNum = minTrajPointCount;
        this.maxTimeInterval = maxTimeInterval;
    }

    /**
     * read raw trajectories and assign visit count to the given map, each trajectory must be inside the map
     *
     * @param rawMap          input map
     * @param rawTrajectories input path for raw trajectories
     * @throws IOException IO exception
     */
    public void trajectoryVisitAssignment(RoadNetworkGraph rawMap, String rawTrajectories) throws IOException {
        Map<String, Integer> id2VisitCountMapping = new LinkedHashMap<>();   // a mapping between the road ID and the number of trajectory
        // visited

        for (RoadWay w : rawMap.getWays())
            if (!id2VisitCountMapping.containsKey(w.getId()))
                id2VisitCountMapping.put(w.getId(), 0);
            else System.out.println("ERROR! The same road ID occurs twice: " + w.getId());

        BufferedReader brTrajectory = new BufferedReader(new FileReader(rawTrajectories + "beijingTrajectory"));

        // create folders for further writing
        String line;
        int tripID = 0;
        while ((line = brTrajectory.readLine()) != null) {
            String[] trajectoryInfo = line.split(",");
            String[] rawTrajectory = trajectoryInfo[28].split("\\|");
            String[] matchedTrajectory = trajectoryInfo[4].split("\\|");

            if (minTrajPointNum == -1 || rawTrajectory.length > minTrajPointNum) {

                // test whether the matching result is included in the map
                boolean isMatchResultInside = true;
                for (String s : matchedTrajectory) {
                    if (!id2VisitCountMapping.containsKey(s)) {
                        isMatchResultInside = false;
                        break;
                    }
                }
                if (!isMatchResultInside)
                    continue;

                // test whether the raw trajectory is within the map area
                boolean isInsideTrajectory = true;
                double firstLon = Double.parseDouble(rawTrajectory[0].split(":")[0]) / 100000;
                double firstLat = Double.parseDouble(rawTrajectory[0].split(":")[1]) / 100000;
                if (isInside(firstLon, firstLat, rawMap)) {
                    long prevTimeDiff = 0;
                    for (int i = 1; i < rawTrajectory.length; i++) {
                        double lon = firstLon + (Double.parseDouble(rawTrajectory[i].split(":")[0]) / 100000);
                        double lat = firstLat + (Double.parseDouble(rawTrajectory[i].split(":")[1]) / 100000);
                        long currTime = Long.parseLong(rawTrajectory[i].split(":")[3]);
                        long currTimeDiff = currTime - prevTimeDiff;
                        if (isInside(lon, lat, rawMap) && (maxTimeInterval == -1 || currTimeDiff <= maxTimeInterval)) {
                            prevTimeDiff = currTime;
                        } else {
                            isInsideTrajectory = false;
                            break;
                        }
                    }
                } else {
                    continue;   // the point is outside the road map area, skip the current trajectory
                }

                if (isInsideTrajectory) {   // the current trajectory is selected
                    for (String s : matchedTrajectory) {
                        int currCount = id2VisitCountMapping.get(s);
                        id2VisitCountMapping.replace(s, currCount + 1);
                    }
                    tripID++;
                }
            }
        }

        for (RoadWay w : rawMap.getWays()) {
            int currCount = id2VisitCountMapping.get(w.getId());
            w.setVisitCount(currCount);
            rawMap.updateMaxVisitCount(currCount);
        }
        System.out.println("Beijing map initialization is done. Total number of trajectory scanned: " + tripID + ". The max visit count " +
                "is " + "" + rawMap.getMaxVisitCount() + ".");
    }

    /**
     * read raw trajectories and filter them with a given size map, all trajectories that are completely inside the map bounds are outputted
     *
     * @param roadGraph                          Input given map
     * @param rawTrajectories                    Input path for raw trajectorie
     * @param initialTrajectories                Folder for output trajectories
     * @param outputGroundTruthMatchResultFolder Folder for all corresponding ground truth trajectory match result
     * @param minDist                            The minimum distance the two consecutive trajectory point should have
     * @throws IOException IO exception
     */
    public void rawTrajectoryParser(RoadNetworkGraph roadGraph, String rawTrajectories, String initialTrajectories, String
            outputGroundTruthMatchResultFolder, double minDist) throws IOException {
        Map<String, Integer> id2VisitCountMapping = new LinkedHashMap<>();   // a mapping between the road ID and the number of trajectory
        // visited

        for (RoadWay w : roadGraph.getWays())
            if (!id2VisitCountMapping.containsKey(w.getId()))
                id2VisitCountMapping.put(w.getId(), 0);
            else System.out.println("ERROR! The same road ID occurs twice: " + w.getId());

        BufferedReader brTrajectory = new BufferedReader(new FileReader(rawTrajectories + "beijingTrajectory"));

        // create folders for further writing
        File createRawTrajFolder = new File(initialTrajectories);
        cleanPath(createRawTrajFolder);
        File createMatchedTrajFolder = new File(outputGroundTruthMatchResultFolder);
        if (createRawTrajFolder.exists() && Objects.requireNonNull(createRawTrajFolder.list()).length > 0)
            return;
        cleanPath(createMatchedTrajFolder);
        DecimalFormat df = new DecimalFormat("0.00000");
        String line;
        int tripID = 0;
        long maxTimeDiff = 0;   // the maximum time difference
        long totalTimeDiff = 0;  // total time difference
        long totalNumOfPoint = 0;
        // reset the cursor to the start of the current file
        while ((line = brTrajectory.readLine()) != null && (requiredRecordNum == -1 || tripID < requiredRecordNum)) {
            String[] trajectoryInfo = line.split(",");
            String[] rawTrajectory = trajectoryInfo[28].split("\\|");
            String[] matchedTrajectory = trajectoryInfo[4].split("\\|");

            if (minTrajPointNum == -1 || rawTrajectory.length > minTrajPointNum) {

                // test whether the matching result is included in the map
                boolean isMatchResultInside = true;
                for (String s : matchedTrajectory) {
                    if (!id2VisitCountMapping.containsKey(s)) {
                        isMatchResultInside = false;
                        break;
                    }
                }
                if (!isMatchResultInside)
                    continue;

                // test whether the raw trajectory is within the map area
                String trajectoryFile = "";
                boolean isValidTrajectory = true;
                double firstLon = Double.parseDouble(rawTrajectory[0].split(":")[0]) / 100000;
                double firstLat = Double.parseDouble(rawTrajectory[0].split(":")[1]) / 100000;
                int pointCount = 1;
                long tempMaxTime = 0;
                long tempTotalTime = 0;
                if (isInside(firstLon, firstLat, roadGraph)) {
                    long firstTime = Long.parseLong(rawTrajectory[0].split((":"))[3]);
                    trajectoryFile += firstLon + " " + firstLat + " " + firstTime + "\n";
                    double prevLon = firstLon;
                    double prevLat = firstLat;
                    long prevTimeDiff = 0;
                    for (int i = 1; i < rawTrajectory.length; i++) {
                        double lon = firstLon + (Double.parseDouble(rawTrajectory[i].split(":")[0]) / 100000);
                        double lat = firstLat + (Double.parseDouble(rawTrajectory[i].split(":")[1]) / 100000);

                        double distance = distFunc.pointToPointDistance(prevLon, prevLat, lon, lat);
                        if (distance < minDist)
                            continue;
                        long currTime = Long.parseLong(rawTrajectory[i].split(":")[3]);
                        long currTimeDiff = currTime - prevTimeDiff;
                        long time = firstTime + currTime;
                        // the new point is inside the area and satisfies the time constraint
                        if (isInside(lon, lat, roadGraph) && currTimeDiff <= maxTimeInterval) {
                            tempMaxTime = tempMaxTime > currTimeDiff ? tempMaxTime : currTimeDiff;
                            tempTotalTime += currTimeDiff;
                            prevTimeDiff = currTime;
                            trajectoryFile += df.format(lon) + " " + df.format(lat) + " " + time + "\n";
                            prevLon = lon;
                            prevLat = lat;
                            pointCount++;
                        } else {
                            isValidTrajectory = false;
                            break;
                        }
                    }
                } else {
                    continue;   // the point is outside the road map area, skip the current trajectory
                }

                if (isValidTrajectory && pointCount > minTrajPointNum) {   // the current trajectory is selected
                    BufferedWriter bwRawTrajectory = new BufferedWriter(new FileWriter(createRawTrajFolder.getAbsolutePath() +
                            "/trip_" + tripID + ".txt"));
                    bwRawTrajectory.write(trajectoryFile);
                    BufferedWriter bwMatchedTrajectory = new BufferedWriter(new FileWriter(createMatchedTrajFolder.getAbsolutePath()
                            + "/realtrip_" + tripID + ".txt"));

                    for (String s : matchedTrajectory) {
                        int currCount = id2VisitCountMapping.get(s);
                        id2VisitCountMapping.replace(s, currCount + 1);
                        bwMatchedTrajectory.write(s + "\n");
                    }

                    bwRawTrajectory.close();
                    bwMatchedTrajectory.close();
                    maxTimeDiff = maxTimeDiff > tempMaxTime ? maxTimeDiff : tempMaxTime;
                    totalTimeDiff += tempTotalTime;
                    totalNumOfPoint += rawTrajectory.length;
                    tripID++;

                }
            }
        }

        for (RoadWay w : roadGraph.getWays()) {
            w.setVisitCount(id2VisitCountMapping.get(w.getId()));
        }
        System.out.println(tripID + " trajectories extracted, the average length is " + (int) (totalNumOfPoint / tripID));
        System.out.println("The maximum sampling interval is " + maxTimeDiff + "s, and the average time interval is " +
                totalTimeDiff / (totalNumOfPoint - tripID));
    }

    public void groundTruthMatchResultStatistics(RoadNetworkGraph roadGraph, String rawTrajectories) throws IOException {
        Map<String, BitSet> id2WayType = new HashMap<>();
        Map<String, Short> id2WayLevel = new HashMap<>();
        for (RoadWay w : roadGraph.getWays()) {
            id2WayType.put(w.getId(), w.getRoadWayType());
            id2WayLevel.put(w.getId(), w.getRoadWayLevel());
        }
        BufferedReader brTrajectory = new BufferedReader(new FileReader(rawTrajectories + "beijingTrajectory"));
        int[] typeCount = new int[25];
        int[] levelCount = new int[10];
        int trajectoryCount = 0;
        String line;
        while ((line = brTrajectory.readLine()) != null) {
            if (trajectoryCount % 50000 == 0)
                System.out.println(trajectoryCount + " trajectory visited.");
            String[] trajectoryInfo = line.split(",");
            String[] matchedTrajectory = trajectoryInfo[4].split("\\|");
            for (String s : matchedTrajectory) {
                if (id2WayType.containsKey(s)) {
                    BitSet currWayType = id2WayType.get(s);
                    for (int i = 0; i < 25; i++) {
                        if (currWayType.get(i))
                            typeCount[i]++;
                    }
                } else
                    System.out.println("ERROR! Cannot find road" + s);
                if (id2WayLevel.containsKey(s))
                    levelCount[id2WayLevel.get(s)]++;
                else
                    System.out.println("ERROR! Cannot find road" + s);
            }
            trajectoryCount++;
        }
        System.out.println("All trajectory visited. Total count: " + trajectoryCount);

        for (int i = 0; i < 10; i++)
            System.out.println("Way level " + i + " has " + levelCount[i] + " hits.");

        for (int i = 0; i < 25; i++)
            System.out.println("Way type " + i + " has " + typeCount[i] + " hits.");
    }

    private void cleanPath(File currFolder) throws IOException {
        if (!currFolder.exists()) {
            if (!currFolder.mkdirs()) throw new IOException("ERROR! Failed to create directory " + currFolder.getAbsolutePath());
        } else {
            File[] fileList = currFolder.listFiles();
            if (fileList != null) {
                for (File f : fileList) {
                    if (!f.delete()) throw new IOException("ERROR! Unable to delete file " + f.getAbsolutePath());
                }
            }
        }
    }

    private boolean isInside(double pointX, double pointY, RoadNetworkGraph roadGraph) {
        boolean inside = false;
        if (pointX >= roadGraph.getMinLon() && pointX <= roadGraph.getMaxLon())
            if (pointY >= roadGraph.getMinLat() && pointY <= roadGraph.getMaxLat())
                inside = true;
        return inside;
    }
}