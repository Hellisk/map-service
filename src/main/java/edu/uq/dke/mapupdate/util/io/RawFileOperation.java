package edu.uq.dke.mapupdate.util.io;

import edu.uq.dke.mapupdate.mapmatching.hmm.NewsonHMM2009;
import edu.uq.dke.mapupdate.util.function.GreatCircleDistanceFunction;
import edu.uq.dke.mapupdate.util.object.datastructure.Pair;
import edu.uq.dke.mapupdate.util.object.datastructure.TrajectoryMatchingResult;
import edu.uq.dke.mapupdate.util.object.roadnetwork.RoadNetworkGraph;
import edu.uq.dke.mapupdate.util.object.roadnetwork.RoadWay;
import edu.uq.dke.mapupdate.util.object.spatialobject.STPoint;
import edu.uq.dke.mapupdate.util.object.spatialobject.Trajectory;

import java.io.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
     * @param rawMap          Input map
     * @param rawTrajectories Input path for raw trajectories
     * @throws IOException IO exception
     */
    public void trajectoryVisitAssignment(RoadNetworkGraph rawMap, String rawTrajectories) throws
            IOException {
        Map<String, Integer> id2VisitCountMapping = new LinkedHashMap<>();   // a mapping between the road ID and the number of trajectory
        // visited
        Map<String, RoadWay> id2RoadWayMapping = new LinkedHashMap<>();   // a mapping between the road ID and the road way

        initializeMapping(rawMap, id2VisitCountMapping, id2RoadWayMapping);

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
                if (isInvalidMatchingResult(id2VisitCountMapping, id2RoadWayMapping, matchedTrajectory))
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

        int visitThreshold = 5;
        int lowVisitCount = 0;  // count the total number of edges whose visit is less than a given threshold
        rawMap.setMaxVisitCount(0);
        for (RoadWay w : rawMap.getWays()) {
            int currCount = id2VisitCountMapping.get(w.getID());
            w.setVisitCount(currCount);
            rawMap.updateMaxVisitCount(currCount);
            if (currCount <= visitThreshold)
                lowVisitCount++;
        }
        System.out.println("Beijing map initialization is done. Total number of trajectory scanned: " + tripID + ". The max visit count " +
                "is " + "" + rawMap.getMaxVisitCount() + ", the percentage of roads visited by less than " + visitThreshold + " times is " +
                lowVisitCount / (double) rawMap.getWays().size());
    }

    /**
     * read raw trajectories and filter them with a given size map, all trajectories that are completely inside the map bounds are outputted
     *
     * @param roadGraph Input given map
     * @throws IOException IO exception
     */
    void rawTrajGTResultFilter(RoadNetworkGraph roadGraph) throws IOException {
        Map<String, Integer> id2VisitCountMapping = new LinkedHashMap<>();   // a mapping between the road ID and the number of trajectory
        // visited
        Map<String, RoadWay> id2RoadWayMapping = new LinkedHashMap<>();   // a mapping between the road ID and the road way

        initializeMapping(roadGraph, id2VisitCountMapping, id2RoadWayMapping);

        BufferedReader brTrajectory = new BufferedReader(new FileReader(RAW_TRAJECTORY + "beijingTrajectory"));

        // create folders for further writing
        File createRawTrajFolder = new File(INPUT_TRAJECTORY);
        cleanPath(createRawTrajFolder);
        File createMatchedTrajFolder = new File(GT_MATCHING_RESULT);
        cleanPath(createMatchedTrajFolder);

        List<Trajectory> resultTrajList = new ArrayList<>();
        List<Pair<Integer, List<String>>> gtResultRoadWayList = new ArrayList<>();
        String line;
        int tripID = 0;
        long maxTimeDiff = 0;   // the maximum time difference
        long totalTimeDiff = 0;  // total time difference
        long totalNumOfPoint = 0;
        // reset the cursor to the start of the current file
        while ((line = brTrajectory.readLine()) != null && (requiredRecordNum == -1 || tripID < requiredRecordNum)) {
            String[] trajectoryInfo = line.split(",");
            String[] rawTrajectoryPointID = trajectoryInfo[28].split("\\|");
            String[] matchedRoadWayID = trajectoryInfo[4].split("\\|");

            if (minTrajPointNum == -1 || rawTrajectoryPointID.length > minTrajPointNum) {

                // test whether the matching result is included in the map
                if (isInvalidMatchingResult(id2VisitCountMapping, id2RoadWayMapping, matchedRoadWayID))
                    continue;

                Trajectory newTraj = new Trajectory();
                // test whether the raw trajectory is within the map area
//                String trajectoryFile = "";
                boolean isValidTrajectory = true;
                double firstLon = Double.parseDouble(rawTrajectoryPointID[0].split(":")[0]) / 100000;
                double firstLat = Double.parseDouble(rawTrajectoryPointID[0].split(":")[1]) / 100000;
                long tempMaxTime = 0;
                long tempTotalTime = 0;
                if (isInside(firstLon, firstLat, roadGraph)) {
                    long firstTime = Long.parseLong(rawTrajectoryPointID[0].split((":"))[3]);
                    newTraj.add(firstLon, firstLat, firstTime);
//                    trajectoryFile += firstLon + " " + firstLat + " " + firstTime + "\n";
                    double prevLon = firstLon;
                    double prevLat = firstLat;
                    long prevTimeDiff = 0;
                    for (int i = 1; i < rawTrajectoryPointID.length; i++) {
                        double lon = firstLon + (Double.parseDouble(rawTrajectoryPointID[i].split(":")[0]) / 100000);
                        double lat = firstLat + (Double.parseDouble(rawTrajectoryPointID[i].split(":")[1]) / 100000);

                        double distance = distFunc.pointToPointDistance(prevLon, prevLat, lon, lat);
                        if (distance < 2 * SIGMA)
                            continue;
                        long currTime = Long.parseLong(rawTrajectoryPointID[i].split(":")[3]);
                        long currTimeDiff = currTime - prevTimeDiff;
                        long time = firstTime + currTime;
                        // the new point is inside the area and satisfies the time constraint
                        if (isInside(lon, lat, roadGraph) && currTimeDiff <= (maxTimeInterval == -1 ? Long.MAX_VALUE : maxTimeInterval)) {
                            tempMaxTime = tempMaxTime > currTimeDiff ? tempMaxTime : currTimeDiff;
                            tempTotalTime += currTimeDiff;
                            prevTimeDiff = currTime;
                            newTraj.add(lon, lat, time);
//                            trajectoryFile += df.format(lon) + " " + df.format(lat) + " " + time + "\n";
                            prevLon = lon;
                            prevLat = lat;
                        } else {
                            isValidTrajectory = false;
                            break;
                        }
                    }
                } else {
                    continue;   // the point is outside the road map area, skip the current trajectory
                }

                if (isValidTrajectory && newTraj.size() > minTrajPointNum) {   // the current trajectory is selected
//                    BufferedWriter bwRawTrajectory = new BufferedWriter(new FileWriter(createRawTrajFolder.getAbsolutePath() +
//                            "/trip_" + tripID + ".txt"));
//                    bwRawTrajectory.write(trajectoryFile);
//                    BufferedWriter bwMatchedTrajectory = new BufferedWriter(new FileWriter(createMatchedTrajFolder.getAbsolutePath()
//                            + "/realtrip_" + tripID + ".txt"));
                    newTraj.setId(tripID + "");
                    Pair<Integer, List<String>> newMatchingResult = new Pair<>(tripID, new ArrayList<>());

                    for (String s : matchedRoadWayID) {
                        int currCount = id2VisitCountMapping.get(s);
                        id2VisitCountMapping.replace(s, currCount + 1);
                        newMatchingResult._2().add(s);
//                        bwMatchedTrajectory.write(s + "\n");
                    }

                    resultTrajList.add(newTraj);
                    gtResultRoadWayList.add(newMatchingResult);
//                    bwRawTrajectory.close();
//                    bwMatchedTrajectory.close();
                    maxTimeDiff = maxTimeDiff > tempMaxTime ? maxTimeDiff : tempMaxTime;
                    totalTimeDiff += tempTotalTime;
                    totalNumOfPoint += newTraj.size();
                    tripID++;
                }
            }
        }

        writeTrajectoryFilterResult(resultTrajList, gtResultRoadWayList, createRawTrajFolder, createMatchedTrajFolder);

        roadGraph.setMaxVisitCount(0);
        for (RoadWay w : roadGraph.getWays()) {
            w.setVisitCount(id2VisitCountMapping.get(w.getID()));
        }
        System.out.println(tripID + " trajectories extracted, the average length is " + (int) (totalNumOfPoint / tripID));
        System.out.println("The maximum sampling interval is " + maxTimeDiff + "s, and the average time interval is " +
                totalTimeDiff / (totalNumOfPoint - tripID));
    }

    void rawTrajManualGTResultFilter(RoadNetworkGraph roadGraph, RoadNetworkGraph rawGrantMap) throws IOException, InterruptedException, ExecutionException {
        final Map<String, Integer> id2VisitCountMapping = new LinkedHashMap<>();   // a mapping between the road ID and the number of
        // trajectory visited
        final Map<String, RoadWay> id2RoadWayMapping = new LinkedHashMap<>();   // a mapping between the road ID and the road way

        initializeMapping(roadGraph, id2VisitCountMapping, id2RoadWayMapping);

        BufferedReader brTrajectory = new BufferedReader(new FileReader(RAW_TRAJECTORY + "beijingTrajectory"));

        // create folders for further writing, if the folders exist and records appears, stop the process
        File createRawTrajFolder = new File(INPUT_TRAJECTORY);
        if (createRawTrajFolder.isDirectory() && Objects.requireNonNull(createRawTrajFolder.list()).length != 0)
            return;
        else cleanPath(createRawTrajFolder);

        File createMatchedTrajFolder = new File(GT_MATCHING_RESULT);
        if (createMatchedTrajFolder.exists() && Objects.requireNonNull(createMatchedTrajFolder.list()).length > 0)
            return;
        else cleanPath(createMatchedTrajFolder);

        List<Trajectory> tempTrajList = new ArrayList<>();
        HashMap<Integer, String[]> id2MatchResult = new HashMap<>();
        List<Trajectory> resultTrajList = new ArrayList<>();
        List<Pair<Integer, List<String>>> gtResultRoadWayList = new ArrayList<>();
        String line;
        int tripID = 0;
        long totalNumOfPoint = 0;
        // reset the cursor to the start of the current file
        while ((line = brTrajectory.readLine()) != null && (requiredRecordNum == -1 || tempTrajList.size() < 2 * requiredRecordNum)) {
            String[] trajectoryInfo = line.split(",");
            String[] rawTrajectoryPointID = trajectoryInfo[28].split("\\|");

            if (minTrajPointNum == -1 || rawTrajectoryPointID.length > minTrajPointNum) {

                Trajectory newTraj = new Trajectory();
                // test whether the raw trajectory is within the map area
//                String trajectoryFile = "";
                boolean isValidTrajectory = true;
                double firstLon = Double.parseDouble(rawTrajectoryPointID[0].split(":")[0]) / 100000;
                double firstLat = Double.parseDouble(rawTrajectoryPointID[0].split(":")[1]) / 100000;
                if (isInside(firstLon, firstLat, roadGraph)) {
                    long firstTime = Long.parseLong(rawTrajectoryPointID[0].split((":"))[3]);
                    newTraj.add(firstLon, firstLat, firstTime);
//                    trajectoryFile += firstLon + " " + firstLat + " " + firstTime + "\n";
                    double prevLon = firstLon;
                    double prevLat = firstLat;
                    long prevTimeDiff = 0;
                    for (int i = 1; i < rawTrajectoryPointID.length; i++) {
                        double lon = firstLon + (Double.parseDouble(rawTrajectoryPointID[i].split(":")[0]) / 100000);
                        double lat = firstLat + (Double.parseDouble(rawTrajectoryPointID[i].split(":")[1]) / 100000);

                        double distance = distFunc.pointToPointDistance(prevLon, prevLat, lon, lat);
                        if (distance < 2 * SIGMA)
                            continue;
                        long currTime = Long.parseLong(rawTrajectoryPointID[i].split(":")[3]);
                        long currTimeDiff = currTime - prevTimeDiff;
                        long time = firstTime + currTime;
                        // the new point is inside the area and satisfies the time constraint
                        if (isInside(lon, lat, roadGraph) && currTimeDiff <= (maxTimeInterval == -1 ? Long.MAX_VALUE : maxTimeInterval)) {
                            prevTimeDiff = currTime;
                            newTraj.add(lon, lat, time);
//                            trajectoryFile += df.format(lon) + " " + df.format(lat) + " " + time + "\n";
                            prevLon = lon;
                            prevLat = lat;
                        } else {
                            isValidTrajectory = false;
                            break;
                        }
                    }
                } else {
                    continue;   // the point is outside the road map area, skip the current trajectory
                }

                if (isValidTrajectory && newTraj.size() > minTrajPointNum) {   // the current trajectory is selected
                    newTraj.setId(tripID + "");
                    tempTrajList.add(newTraj);
                    tripID++;
                }
            }
        }
        System.out.println("Trajectory filter finished, total number of candidates: " + tripID + ". Start the ground-truth map-matching.");

        // start the generation of ground-truth map-matching result
        NewsonHMM2009 hmm = new NewsonHMM2009(CANDIDATE_RANGE, GAP_EXTENSION_RANGE, 1, rawGrantMap);
        Stream<Trajectory> tempTrajStream = tempTrajList.stream();
        // parallel processing
        ForkJoinPool forkJoinPool = ForkJoinPool.commonPool();
        ForkJoinTask<Stream<Pair<Integer, String[]>>> matchedResultStream = forkJoinPool.submit(() -> tempTrajStream.parallel().map
                (trajectory -> {
                    Pair<TrajectoryMatchingResult, List<Trajectory>> result = hmm.doMatching(trajectory);
                    // matching result is empty or result contains breaks, waive the current trajectory
                    if (result._1().getBestMatchWayList().size() == 0 || !result._2().isEmpty())
                        return null;
                    String[] bestMatchWayList = result._1().getBestMatchWayList().toArray(new String[0]);
                    // test whether the matching result is included in the map
                    if (isInvalidMatchingResult(id2VisitCountMapping, id2RoadWayMapping, bestMatchWayList))
                        return null;
                    return new Pair<>(Integer.parseInt(trajectory.getId()), bestMatchWayList);
                }));
        while (!matchedResultStream.isDone()) {
            Thread.sleep(5);
        }
        int matchedResultCount = 0;
        List<Pair<Integer, String[]>> matchedResultList = matchedResultStream.get().collect(Collectors.toList());
        for (Pair<Integer, String[]> matchedResult : matchedResultList) {
            if (matchedResult != null) {
                matchedResultCount++;
                id2MatchResult.put(matchedResult._1(), matchedResult._2());
            }
        }
        System.out.println("Ground-truth matching complete. Total number of valid matching result: " + matchedResultCount);

        tripID = 0;     // reset the trip ID for final trajectory id assignment
        for (Trajectory currTraj : tempTrajList) {
            if (id2MatchResult.containsKey(Integer.parseInt(currTraj.getId()))) {

                Pair<Integer, List<String>> newMatchingResult = new Pair<>(tripID, new ArrayList<>());

                String[] matchedRoadWayID = id2MatchResult.get(Integer.parseInt(currTraj.getId()));
                for (String s : matchedRoadWayID) {
                    int currCount = id2VisitCountMapping.get(s);
                    id2VisitCountMapping.replace(s, currCount + 1);
                    newMatchingResult._2().add(s);
                }
                currTraj.setId(tripID + "");
                resultTrajList.add(currTraj);
                gtResultRoadWayList.add(newMatchingResult);
                totalNumOfPoint += currTraj.size();
                tripID++;
            }
        }
        if (requiredRecordNum != -1 && tempTrajList.size() == 2 * requiredRecordNum && tripID < requiredRecordNum)
            throw new IllegalArgumentException("ERROR! The cache for trajectory filter is too small. The final trajectory size is :" + tripID);
        System.out.println("Ground-truth trajectory result generated.");

        writeTrajectoryFilterResult(resultTrajList, gtResultRoadWayList, createRawTrajFolder, createMatchedTrajFolder);

        // visit statistics
        int visitThreshold = 5;
        int totalHighVisitCount = 0;
        int totalVisitCount = 0;
        roadGraph.setMaxVisitCount(0);
        for (RoadWay w : roadGraph.getWays()) {
            int visitCount = id2VisitCountMapping.get(w.getID());
            w.setVisitCount(visitCount);
            if (visitCount > 0) {
                totalVisitCount++;
                if (visitCount >= 5) {
                    totalHighVisitCount++;
                }
            }
        }
        System.out.println(tripID + " trajectories extracted, the average length: " + (int) (totalNumOfPoint / tripID) + ", max visit count: " +
                +roadGraph.getMaxVisitCount() + ", visit percentage: " + totalVisitCount / (double) roadGraph.getWays().size() + ", high " +
                "visit(>=" + visitThreshold + "times): " + totalHighVisitCount / (double) roadGraph.getWays().size());
    }

    private void writeTrajectoryFilterResult(List<Trajectory> resultTrajList, List<Pair<Integer, List<String>>> gtResultRoadWayList, File createRawTrajFolder, File createMatchedTrajFolder) {
        DecimalFormat df = new DecimalFormat("0.00000");
        if (resultTrajList == null || resultTrajList.isEmpty())
            throw new NullPointerException("ERROR! The filtered trajectory result list is empty.");
        if (gtResultRoadWayList == null || gtResultRoadWayList.isEmpty())
            throw new NullPointerException("ERROR! The filtered trajectory matching result list is empty.");

        Stream<Trajectory> resultTrajStream = resultTrajList.stream();
        Stream<Pair<Integer, List<String>>> gtResultRoadWayStream = gtResultRoadWayList.stream();

        // parallel processing
        ForkJoinPool forkJoinPool = ForkJoinPool.commonPool();
        forkJoinPool.submit(() -> resultTrajStream.parallel().forEach(trajectory -> {
            try {
                BufferedWriter bwRawTrajectory = new BufferedWriter(new FileWriter(createRawTrajFolder.getAbsolutePath() + "/trip_" +
                        trajectory.getId() + ".txt"));
                for (STPoint p : trajectory) {
                    bwRawTrajectory.write(df.format(p.x()) + " " + df.format(p.y()) + " " + p.time() + "\n");
                }
                bwRawTrajectory.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }));
        forkJoinPool.submit(() -> gtResultRoadWayStream.parallel().forEach(matchResultRoadWay -> {
            try {
                BufferedWriter bwMatchedTrajectory = new BufferedWriter(new FileWriter(createMatchedTrajFolder.getAbsolutePath() +
                        "/realtrip_" + matchResultRoadWay._1() + ".txt"));
                for (String s : matchResultRoadWay._2()) {
                    bwMatchedTrajectory.write(s + "\n");
                }
                bwMatchedTrajectory.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }));
        while (Objects.requireNonNull(createRawTrajFolder.list()).length != resultTrajList.size() && Objects.requireNonNull
                (createMatchedTrajFolder.list()).length != gtResultRoadWayList.size()) {
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Check whether the ground-truth map-matching result satisfy the condition, all roads must be included in the map area and the
     * map-matching result must be continuous.
     *
     * @param id2VisitCountMapping The road ID and the corresponding visit count.
     * @param id2RoadWayMapping    The road ID and the corresponding road way object.
     * @param matchedRoadWayID     The list of ground-truth map-matching result.
     * @return False if all map-matching result satisfy the requirement, otherwise true.
     */
    private boolean isInvalidMatchingResult(Map<String, Integer> id2VisitCountMapping, Map<String, RoadWay> id2RoadWayMapping, String[] matchedRoadWayID) {
        RoadWay prevMatchRoad = null;
        for (String s : matchedRoadWayID) {
            if (!id2VisitCountMapping.containsKey(s)) {
                return true;
            } else if (prevMatchRoad != null) {    // check the connectivity of the match roadID
                RoadWay currRoad = id2RoadWayMapping.get(s);
                if (!prevMatchRoad.getToNode().getID().equals(currRoad.getFromNode().getID())) {  // break happens
                    return true;
                } else
                    prevMatchRoad = id2RoadWayMapping.get(s);
            } else {
                prevMatchRoad = id2RoadWayMapping.get(s);
            }
        }
        return false;
    }

    private void initializeMapping(RoadNetworkGraph roadGraph, Map<String, Integer> id2VisitCountMapping, Map<String, RoadWay> id2RoadWayMapping) {
        for (RoadWay w : roadGraph.getWays())
            if (!id2VisitCountMapping.containsKey(w.getID())) {
                id2VisitCountMapping.put(w.getID(), 0);
                id2RoadWayMapping.put(w.getID(), w);
            } else System.out.println("ERROR! The same road ID occurs twice: " + w.getID());
    }

    public void groundTruthMatchResultStatistics(RoadNetworkGraph roadGraph, String rawTrajectories) throws IOException {
        Map<String, BitSet> id2WayType = new HashMap<>();
        Map<String, Short> id2WayLevel = new HashMap<>();
        for (RoadWay w : roadGraph.getWays()) {
            id2WayType.put(w.getID(), w.getRoadWayType());
            id2WayLevel.put(w.getID(), w.getRoadWayLevel());
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

    public void generateGTMatchingResult(RoadNetworkGraph roadNetworkGraph, String rawTrajectories, double minDist) throws IOException, ExecutionException, InterruptedException {
        System.out.println("Generated ground-truth result required, start generating matching result.");
        BufferedReader brTrajectory = new BufferedReader(new FileReader(rawTrajectories + "beijingTrajectory"));
        BufferedWriter bwRawTrajectory = new BufferedWriter(new FileWriter(rawTrajectories + "beijingTrajectoryNew"));

//        NewsonHMM2009 mapMatching = new NewsonHMM2009(CANDIDATE_RANGE, GAP_EXTENSION_RANGE, RANK_LENGTH, roadNetworkGraph);
        String line;
        int tripCount = 0;
        List<Pair<Trajectory, String>> inputTrajList = new ArrayList<>();

        // reset the cursor to the start of the current file
        while ((line = brTrajectory.readLine()) != null) {
            String[] trajectoryInfo = line.split(",");
            String rawTrajectoryPoints = trajectoryInfo[28];
            String[] rawTrajectoryPointID = rawTrajectoryPoints.split("\\|");

            // generate trajectory object
            Trajectory traj = new Trajectory();
            double firstLon = Double.parseDouble(rawTrajectoryPointID[0].split(":")[0]) / 100000;
            double firstLat = Double.parseDouble(rawTrajectoryPointID[0].split(":")[1]) / 100000;
            long firstTime = Long.parseLong(rawTrajectoryPointID[0].split((":"))[3]);
            STPoint currPoint = new STPoint(firstLon, firstLat, firstTime);
            traj.add(currPoint);
            double prevLon = firstLon;
            double prevLat = firstLat;
            for (int i = 1; i < rawTrajectoryPointID.length; i++) {
                double lon = firstLon + (Double.parseDouble(rawTrajectoryPointID[i].split(":")[0]) / 100000);
                double lat = firstLat + (Double.parseDouble(rawTrajectoryPointID[i].split(":")[1]) / 100000);
                long currTime = Long.parseLong(rawTrajectoryPointID[i].split(":")[3]);
                double distance = distFunc.pointToPointDistance(prevLon, prevLat, lon, lat);
                if (distance < minDist)
                    continue;
                long time = firstTime + currTime;
                currPoint = new STPoint(lon, lat, time);
                traj.add(currPoint);
                prevLon = lon;
                prevLat = lat;
            }
            inputTrajList.add(new Pair<>(traj, rawTrajectoryPoints));
        }

        System.out.println("Start ground-truth generation, total number of input trajectory: " + inputTrajList.size());

        Stream<Pair<Trajectory, String>> inputTrajStream = inputTrajList.stream();
        NewsonHMM2009 hmm = new NewsonHMM2009(CANDIDATE_RANGE, GAP_EXTENSION_RANGE, 1, roadNetworkGraph);

        // parallel processing
        ForkJoinPool forkJoinPool = ForkJoinPool.commonPool();
        ForkJoinTask<Stream<String>> matchedResultStream = forkJoinPool.submit(() -> inputTrajStream.parallel().map
                (trajectory -> {
                    Pair<TrajectoryMatchingResult, List<Trajectory>> result = hmm.doMatching(trajectory._1());
                    if (result._1().getBestMatchWayList().size() == 0 || !result._2().isEmpty()) {
                        // matching result is empty or result contains breaks, waive the current trajectory
                        return null;
                    }
                    StringBuilder resultString = new StringBuilder();
                    resultString.append(trajectory._2()).append(",");
                    List<String> bestMatchWayList = result._1().getBestMatchWayList();
                    for (int i = 0; i < bestMatchWayList.size() - 1; i++) {
                        String s = bestMatchWayList.get(i);
                        resultString.append(s).append("|");
                    }
                    resultString.append(bestMatchWayList.get(bestMatchWayList.size() - 1)).append("\n");
                    return resultString.toString();
                }));

        List<String> matchedResultList = matchedResultStream.get().collect(Collectors.toList());
        for (String s : matchedResultList) {
            if (s != null) {
                bwRawTrajectory.write(s);
                tripCount++;
            }
        }
        System.out.println("Ground-truth matching complete, total number of matched trajectories: " + tripCount + " start writing file");
        bwRawTrajectory.close();
        System.out.println("Ground-truth trajectory result generated.");
    }

}