package edu.uq.dke.mapupdate.util.io;

import edu.uq.dke.mapupdate.util.object.roadnetwork.RoadNetworkGraph;
import edu.uq.dke.mapupdate.util.object.spatialobject.Trajectory;

import java.io.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by uqpchao on 5/07/2017.
 */
public class RawFileOperation {
    private int requiredRecordNum;
    private int minTrajPointNum;
    private int maxTimeInterval;
    private boolean statisticsMode;

    public RawFileOperation(int trajCount, int minTrajPointCount, boolean statisticsMode, int maxTimeInterval) {
        this.requiredRecordNum = trajCount;
        this.minTrajPointNum = minTrajPointCount;
        this.statisticsMode = statisticsMode;
        this.maxTimeInterval = maxTimeInterval;
    }

    /**
     * read raw trajectories and filter them with a given size map, all trajectories that are completely inside the map bounds are outputted
     *
     * @param groundTruthMap                     input path for given map
     * @param rawTrajectories                    input path for raw trajectorie
     * @param initialTrajectories                folder for output trajectories
     * @param outputGroundTruthMatchResultFolder folder for all corresponding ground truth trajectory match result
     * @return the trajectory list
     * @throws IOException IO exception
     */
    public List<Trajectory> RawTrajectoryParser(String groundTruthMap, String rawTrajectories, String initialTrajectories, String
            outputGroundTruthMatchResultFolder) throws IOException {
        CSVMapReader map = new CSVMapReader(groundTruthMap);
        RoadNetworkGraph roadGraph = map.readMap(0);
        BufferedReader brTrajectory = new BufferedReader(new FileReader(rawTrajectories + "beijingTrajectory"));
        List<Trajectory> trajectoryList = new ArrayList<>();
//        Set<String> segmentLookup = new HashSet<>();

//        // maintain a road way list of given map area for filtering the match result
//        for (RoadWay r : roadGraph.getWays()) {
//            segmentLookup.add(r.getId());
//        }

        // create folders for further writing
        File createFolder = new File(initialTrajectories);
        cleanPath(createFolder);
        createFolder = new File(outputGroundTruthMatchResultFolder);
        cleanPath(createFolder);
        DecimalFormat df = new DecimalFormat(".00000");
        String line;
        int tripID = 0;
        long maxTimeDiff = 0;   // the maximum time difference
        long totalTimeDiff = 0;  // total time difference
        long totalNumOfPoint = 0;
        int currCursor = 0;
        while ((line = brTrajectory.readLine()) != null && (requiredRecordNum == -1 || tripID < requiredRecordNum)) {
            if (tripID != 0 && tripID % 5000 == 0)
                if (tripID != currCursor) {
                    System.out.println(tripID + " trajectories processed. ");
                    currCursor = tripID;
                }
            if (statisticsMode) {   // for statistics purpose
                String[] trajectoryInfo = line.split(",");
                String[] rawTrajectory = trajectoryInfo[28].split("\\|");
                if (rawTrajectory.length > minTrajPointNum || minTrajPointNum == -1) {
                    long prevTimeDiff = 0;
                    for (int i = 1; i < rawTrajectory.length; i++) {
                        long currTime = Long.parseLong(rawTrajectory[i].split(":")[3]);
                        long currTimeDiff = currTime - prevTimeDiff;
                        if (currTimeDiff > maxTimeInterval) {
                            totalNumOfPoint -= rawTrajectory.length;
                            tripID--;
                            break;
                        }
                        maxTimeDiff = maxTimeDiff > currTimeDiff ? maxTimeDiff : currTimeDiff;
                        totalTimeDiff += currTimeDiff;
                        prevTimeDiff = currTime;
                    }
                    totalNumOfPoint += rawTrajectory.length;
                    tripID++;
                }
            } else {
                String[] trajectoryInfo = line.split(",");
                String[] rawTrajectory = trajectoryInfo[28].split("\\|");
                if (rawTrajectory.length > minTrajPointNum || minTrajPointNum == -1) {
                    BufferedWriter bwRawTrajectory = new BufferedWriter(new FileWriter(initialTrajectories + "trip_" + tripID + ".txt"));
                    Trajectory currTraj = new Trajectory(tripID + "");
                    boolean isInsideTrajectory = true;
                    double firstLon = Double.parseDouble(rawTrajectory[0].split(":")[0]) / 100000;
                    double firstLat = Double.parseDouble(rawTrajectory[0].split(":")[1]) / 100000;
                    long tempMaxTime = 0;
                    long tempTotalTime = 0;
                    if (isInside(firstLon, firstLat, roadGraph)) {
                        long firstTime = Long.parseLong(rawTrajectory[0].split((":"))[3]);
                        bwRawTrajectory.write(firstLon + " " + firstLat + " " + firstTime + "\n");
                        currTraj.add(firstLon, firstLat, firstTime);

                        long prevTimeDiff = 0;
                        for (int i = 1; i < rawTrajectory.length; i++) {
                            double lon = firstLon + (Double.parseDouble(rawTrajectory[i].split(":")[0]) / 100000);
                            double lat = firstLat + (Double.parseDouble(rawTrajectory[i].split(":")[1]) / 100000);
                            long currTime = Long.parseLong(rawTrajectory[i].split(":")[3]);
                            long currTimeDiff = currTime - prevTimeDiff;
                            long time = firstTime + currTime;
                            if (isInside(lon, lat, roadGraph) && currTimeDiff <= maxTimeInterval) {
                                tempMaxTime = tempMaxTime > currTimeDiff ? tempMaxTime : currTimeDiff;
                                tempTotalTime += currTimeDiff;
                                prevTimeDiff = currTime;
                                bwRawTrajectory.write(df.format(lon) + " " + df.format(lat) + " " + time + "\n");
                                currTraj.add(Double.parseDouble(df.format(lon)), Double.parseDouble(df.format(lat)), time);
                            } else {
                                isInsideTrajectory = false;
                                break;
                            }
                        }
                    } else {
                        isInsideTrajectory = false;
                    }
                    bwRawTrajectory.close();

                    if (isInsideTrajectory) {
                        BufferedWriter bwMatchedTrajectory = new BufferedWriter(new FileWriter(outputGroundTruthMatchResultFolder + "realtrip_" + tripID + ".txt"));
                        String[] matchLines = trajectoryInfo[4].split("\\|");

                        for (String l : matchLines)
                            bwMatchedTrajectory.write(l + "\n");

                        bwMatchedTrajectory.close();
                        trajectoryList.add(currTraj);
                        maxTimeDiff = maxTimeDiff > tempMaxTime ? maxTimeDiff : tempMaxTime;
                        totalTimeDiff += tempTotalTime;
                        totalNumOfPoint += rawTrajectory.length;
                        tripID++;

                        //                boolean isInsideMatchedTrajectory = true;
                        //                for (String l : matchLines) {
                        //                    String[] matchPointInfo = l.split(":");
                        //                    if (!segmentLookup.contains(matchPointInfo[0])) {
                        //                        isInsideMatchedTrajectory = false;
                        //                        break;
                        //                    } else {
                        //                        bwMatchedTrajectory.write(matchPointInfo[3] + " " + matchPointInfo[4] + " " + matchPointInfo[5] + " " + matchPointInfo[0] + "\n");
                        //                    }
                        //                }
                        //                bwMatchedTrajectory.close();
                        //                if (isInsideMatchedTrajectory) {
                        //                    tripID++;
                        //                } else {
                        //                    File currTrajFile = new File(initialTrajectories + "trip_" + tripID + ".txt");
                        //                    File currMatchedTrajFile = new File(outputGroundTruthMatchResultFolder + "realtrip_" + tripID + ".txt");
                        //                    currTrajFile.delete();
                        //                    currMatchedTrajFile.delete();
                        //                }

                    } else {
                        File currTrajFile = new File(initialTrajectories + "trip_" + tripID + ".txt");
                        currTrajFile.delete();
                    }
                }
            }
        }
        System.out.println(tripID + " trajectories extracted, the average length is " + (int) (totalNumOfPoint / tripID));
        System.out.println("The maximum sampling interval is " + maxTimeDiff + "s, and the average time interval is " +
                totalTimeDiff / (totalNumOfPoint - tripID));
        return trajectoryList;
    }

    private void cleanPath(File currFolder) {
        if (!currFolder.exists()) {
            currFolder.mkdirs();
        } else {
            File[] fileList = currFolder.listFiles();
            if (fileList != null) {
                for (File f : fileList) {
                    f.delete();
                }
            }
        }
    }

    private boolean isInside(double pointX, double pointY, RoadNetworkGraph roadGraph) {
        boolean inside = false;
        if (pointX > roadGraph.getMinLon() && pointX < roadGraph.getMaxLon())
            if (pointY > roadGraph.getMinLat() && pointY < roadGraph.getMaxLat())
                inside = true;
        return inside;
    }
}