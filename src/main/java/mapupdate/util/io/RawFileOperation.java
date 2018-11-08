package mapupdate.util.io;

import mapupdate.mapmatching.hmm.NewsonHMM2009;
import mapupdate.util.function.GreatCircleDistanceFunction;
import mapupdate.util.object.datastructure.Pair;
import mapupdate.util.object.datastructure.TrajectoryMatchingResult;
import mapupdate.util.object.roadnetwork.RoadNetworkGraph;
import mapupdate.util.object.roadnetwork.RoadWay;
import mapupdate.util.object.spatialobject.Trajectory;
import mapupdate.util.object.spatialobject.TrajectoryPoint;

import java.io.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static mapupdate.Main.*;

/**
 * Created by uqpchao on 5/07/2017.
 */
public class RawFileOperation {
    private int requiredRecordNum;
    private int minTrajTimeSpan;
    private int maxTimeInterval;
    private GreatCircleDistanceFunction distFunc = new GreatCircleDistanceFunction();

    RawFileOperation(int trajCount, int minTrajTimeSpan, int maxTimeInterval) {
        this.requiredRecordNum = trajCount;
        this.minTrajTimeSpan = minTrajTimeSpan;
        this.maxTimeInterval = maxTimeInterval;
    }

    /**
     * Read raw trajectories and assign visit count to the given map, each trajectory must be inside the map.
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

            // test whether the matching result is included in the map
            if (isMatchingResultNotEnclosed(id2RoadWayMapping, matchedTrajectory)) {
                continue;
            }

            // test whether the raw trajectory is within the map area
            boolean isInsideTrajectory = true;
            String[] firstTrajectoryPoint = rawTrajectory[0].split(":");
            double firstLon = Double.parseDouble(firstTrajectoryPoint[0]) / 100000;
            double firstLat = Double.parseDouble(firstTrajectoryPoint[1]) / 100000;
            if (isInside(firstLon, firstLat, rawMap)) {
                long prevTimeDiff = 0;
                for (int i = 1; i < rawTrajectory.length; i++) {
                    String[] currTrajectoryPoint = rawTrajectory[i].split(":");
                    double lon = firstLon + (Double.parseDouble(currTrajectoryPoint[0]) / 100000);
                    double lat = firstLat + (Double.parseDouble(currTrajectoryPoint[1]) / 100000);
                    long currTime = Long.parseLong(currTrajectoryPoint[3]);
                    long currTimeDiff = currTime - prevTimeDiff;
                    if (isInside(lon, lat, rawMap) && (maxTimeInterval == -1 || currTimeDiff <= maxTimeInterval)) {
                        prevTimeDiff = currTime;
                    } else {
                        isInsideTrajectory = false;
                        break;
                    }
                }
            } else {
                continue;   // the first point is outside the road map area, skip the current trajectory
            }

            if (isInsideTrajectory) {   // the current trajectory is selected
                for (String s : matchedTrajectory) {
                    int currCount = id2VisitCountMapping.get(s);
                    id2VisitCountMapping.replace(s, currCount + 1);
                }
                tripID++;
            }
        }

        DecimalFormat df = new DecimalFormat(".00000");
        int visitThreshold = 5;
        int totalHighVisitCount = 0;  // count the total number of edges whose visit is less than a given threshold
        int totalVisitCount = 0;  // count the total number of edges whose visit is less than a given threshold
        rawMap.setMaxVisitCount(0);
        for (RoadWay w : rawMap.getWays()) {
            int currCount = id2VisitCountMapping.get(w.getID());
            w.setVisitCount(currCount);
            rawMap.updateMaxVisitCount(currCount);
            if (currCount > 0) {
                totalVisitCount++;
                if (currCount > visitThreshold) {
                    totalHighVisitCount++;
                }
            }
        }
        LOGGER.info("Beijing map initialization is done. Total number of trajectories: " + tripID + ", max visit count: " +
                rawMap.getMaxVisitCount() + ", roads visited percentage: " + df.format(totalVisitCount / (double) rawMap.getWays().size() * 100) +
                "%, visit more than " + visitThreshold + " times :" + df.format(totalHighVisitCount / (double) rawMap.getWays().size() * 100) + "%");
    }

    /**
     * Read raw trajectories and filter them with a given size map, all trajectories that pass through the map area for a long period of
     * time are outputted
     *
     * @param roadGraph Input given map
     * @throws IOException IO exception
     */
    void rawTrajGTResultFilter(RoadNetworkGraph roadGraph) throws IOException {
        final Map<String, Integer> id2VisitCountMapping = new LinkedHashMap<>();   // a mapping between the road ID and the number of
        // trajectory visited
        final Map<String, RoadWay> id2RoadWayMapping = new LinkedHashMap<>();   // a mapping between the road ID and the road way

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
        int numOfCompleteTraj = 0;
        int numOfPartialTraj = 0;
        // reset the cursor to the start of the current file
        while ((line = brTrajectory.readLine()) != null && (requiredRecordNum == -1 || tripID < requiredRecordNum)) {
            String[] trajectoryInfo = line.split(",");
            String[] rawTrajectoryPointID = trajectoryInfo[28].split("\\|");
            String[] matchedRoadWayID = trajectoryInfo[4].split("\\|");

            // test whether the matching result pass through the area and continuous
            if (isMatchingResultNotContinuous(id2RoadWayMapping, matchedRoadWayID))
                continue;

            Trajectory newTraj = new Trajectory();

            String[] firstTrajectoryPoint = rawTrajectoryPointID[0].split(":");
            double firstLon = Double.parseDouble(firstTrajectoryPoint[0]) / 100000;
            double firstLat = Double.parseDouble(firstTrajectoryPoint[1]) / 100000;
            long firstTime = Long.parseLong(firstTrajectoryPoint[3]);
            int currIndex = 0;
            double lon = firstLon;
            double lat = firstLat;
            while (!isInside(lon, lat, roadGraph) && currIndex < rawTrajectoryPointID.length - 1) {
                currIndex++;
                String[] currTrajectoryPoint = rawTrajectoryPointID[currIndex].split(":");
                lon = firstLon + (Double.parseDouble(currTrajectoryPoint[0]) / 100000);
                lat = firstLat + (Double.parseDouble(currTrajectoryPoint[1]) / 100000);
            }
            if (currIndex == rawTrajectoryPointID.length - 1)  // the current trajectory is out of range
                continue;
            int startIndex = currIndex;
            String[] currTrajectoryPoint = rawTrajectoryPointID[currIndex].split(":");
            double currSpeed = Double.parseDouble(currTrajectoryPoint[2]);
            double currHeading = Double.parseDouble(currTrajectoryPoint[4]);
            long currTimeOffset = Long.parseLong(currTrajectoryPoint[3]);
            long time = startIndex == 0 ? firstTime : firstTime + currTimeOffset;
            newTraj.add(lon, lat, time, currSpeed, currHeading);
            long currMaxTimeDiff = 0;
            long currTotalTimeDiff = 0;
            long prevTimeOffset = time - firstTime;
            for (currIndex = currIndex + 1; currIndex < rawTrajectoryPointID.length; currIndex++) {
                currTrajectoryPoint = rawTrajectoryPointID[currIndex].split(":");
                lon = firstLon + (Double.parseDouble(currTrajectoryPoint[0]) / 100000);
                lat = firstLat + (Double.parseDouble(currTrajectoryPoint[1]) / 100000);
//                double distance = distFunc.pointToPointDistance(prevLon, prevLat, lon, lat);
//                if (distance < 2 * SIGMA)
//                    continue;
                currTimeOffset = Long.parseLong(currTrajectoryPoint[3]);
                long currTimeDiff = currTimeOffset - prevTimeOffset;
                time = firstTime + currTimeOffset;
                // the new point is inside the area and satisfies the time constraint
                if (isInside(lon, lat, roadGraph) && currTimeDiff <= (maxTimeInterval == -1 ? Long.MAX_VALUE : maxTimeInterval)) {
                    currMaxTimeDiff = currMaxTimeDiff > currTimeDiff ? currMaxTimeDiff : currTimeDiff;
                    currTotalTimeDiff += currTimeDiff;
                    currSpeed = Double.parseDouble(currTrajectoryPoint[2]);
                    currHeading = Double.parseDouble(currTrajectoryPoint[4]);
                    prevTimeOffset = currTimeOffset;
                    newTraj.add(lon, lat, time, currSpeed, currHeading);
//                        prevLon = lon;
//                        prevLat = lat;
                } else {
                    break;
                }
            }

            GreatCircleDistanceFunction distFunc = new GreatCircleDistanceFunction();
            if (newTraj.duration() >= minTrajTimeSpan && newTraj.length(distFunc) >= 3 * minTrajTimeSpan) {   // the minimum average
                // speed should be larger than 10.8km/h
                newTraj.setId(tripID + "");
                Pair<Integer, List<String>> newMatchingResult = new Pair<>(tripID, new ArrayList<>());
                if (startIndex == 0 && currIndex == rawTrajectoryPointID.length) {
                    for (String s : matchedRoadWayID) {
                        if (id2VisitCountMapping.containsKey(s)) {
                            int currCount = id2VisitCountMapping.get(s);
                            id2VisitCountMapping.replace(s, currCount + 1);
                            newMatchingResult._2().add(s);
                        }
                    }
                    numOfCompleteTraj++;
                } else {
//                    continue;
                    // only part of the trajectory is selected as the raw trajectory
                    for (String s : matchedRoadWayID) {
                        if (id2VisitCountMapping.containsKey(s)) {
                            int currCount = id2VisitCountMapping.get(s);
                            id2VisitCountMapping.replace(s, currCount + 1);
                            newMatchingResult._2().add(s);
                        }
                    }
                    numOfPartialTraj++;
                }

                resultTrajList.add(newTraj);
                gtResultRoadWayList.add(newMatchingResult);
                maxTimeDiff = maxTimeDiff > currMaxTimeDiff ? maxTimeDiff : currMaxTimeDiff;
                totalTimeDiff += currTotalTimeDiff;
                totalNumOfPoint += newTraj.size();
                tripID++;
            }
        }

        writeTrajectoryFile(resultTrajList, gtResultRoadWayList, createRawTrajFolder, createMatchedTrajFolder);

        roadGraph.setMaxVisitCount(0);
        for (RoadWay w : roadGraph.getWays()) {
            w.setVisitCount(id2VisitCountMapping.get(w.getID()));
        }
        LOGGER.info(tripID + " trajectories extracted, including " + numOfCompleteTraj + " complete trajectories and " + numOfPartialTraj +
                " partial ones. The average length is " + (int) (totalNumOfPoint / tripID));
        LOGGER.info("The maximum sampling interval is " + maxTimeDiff + "s, and the average time interval is " +
                totalTimeDiff / (totalNumOfPoint - tripID) + ".");
    }

    /**
     * Map-matching the trajectory to the ground-truth map so as to generate the ground-truth matching result. Used when the provided
     * ground-truth result is not reliable.
     *
     * @param roadGraph   The resized map
     * @param rawGrantMap The map cropped by the bounding box
     * @throws IOException
     * @throws InterruptedException
     * @throws ExecutionException
     */
    void rawTrajManualGTResultFilter(RoadNetworkGraph roadGraph, RoadNetworkGraph rawGrantMap) throws IOException, InterruptedException, ExecutionException {
        final Map<String, Integer> id2VisitCountSmallMapping = new LinkedHashMap<>();   // a mapping between the road ID and the number of
        // trajectory visited in current map
        final Map<String, Integer> id2VisitCountLargeMapping = new LinkedHashMap<>();   // a mapping between the road ID and the number of
        // trajectory visited in the original map
        final Map<String, RoadWay> id2RoadWayMapping = new LinkedHashMap<>();   // a mapping between the road ID and the road way

        initializeMapping(roadGraph, id2VisitCountSmallMapping, id2RoadWayMapping);

        BufferedReader brTrajectory = new BufferedReader(new FileReader(RAW_TRAJECTORY + "beijingTrajectory"));

        // create folders for further writing, if the folders exist and records appears, stop the process
        File createRawTrajFolder = new File(INPUT_TRAJECTORY);
        if (createRawTrajFolder.isDirectory() && Objects.requireNonNull(createRawTrajFolder.list()).length != 0) {
            LOGGER.info("Raw trajectories are already filtered, skip the process");
            return;
        } else
            cleanPath(createRawTrajFolder);

        File createMatchedTrajFolder = new File(GT_MATCHING_RESULT);
        cleanPath(createMatchedTrajFolder);

        List<Trajectory> tempTrajList = new ArrayList<>();
        HashMap<Integer, String[]> id2MatchingResult = new HashMap<>();
        List<Trajectory> resultTrajList = new ArrayList<>();
        List<Pair<Integer, List<String>>> gtResultRoadWayList = new ArrayList<>();
        String line;
        int tripID = 0;
        long totalNumOfPoint = 0;
        // reset the cursor to the start of the current file
        while ((line = brTrajectory.readLine()) != null && (requiredRecordNum == -1 || tempTrajList.size() < 1.5 * requiredRecordNum)) {
            String[] trajectoryInfo = line.split(",");
            String[] rawTrajectoryPointID = trajectoryInfo[28].split("\\|");
            String[] matchedRoadWayID = trajectoryInfo[4].split("\\|");

            // test whether the matching result pass through the area and continuous
            if (isMatchingResultNotContinuous(id2RoadWayMapping, matchedRoadWayID))
                continue;

            Trajectory newTraj = new Trajectory();

            String[] firstTrajectoryPoint = rawTrajectoryPointID[0].split(":");
            double firstLon = Double.parseDouble(firstTrajectoryPoint[0]) / 100000;
            double firstLat = Double.parseDouble(firstTrajectoryPoint[1]) / 100000;
            long firstTime = Long.parseLong(firstTrajectoryPoint[3]);
            int currIndex = 0;
            double lon = firstLon;
            double lat = firstLat;
            while (!isInside(lon, lat, roadGraph) && currIndex < rawTrajectoryPointID.length - 1) {
                currIndex++;
                String[] currTrajectoryPoint = rawTrajectoryPointID[currIndex].split(":");
                lon = firstLon + (Double.parseDouble(currTrajectoryPoint[0]) / 100000);
                lat = firstLat + (Double.parseDouble(currTrajectoryPoint[1]) / 100000);
            }
            if (currIndex == rawTrajectoryPointID.length - 1)  // the current trajectory is out of range
                continue;
            int startIndex = currIndex;
            String[] currTrajectoryPoint = rawTrajectoryPointID[currIndex].split(":");
            double currSpeed = Double.parseDouble(currTrajectoryPoint[2]);
            double currHeading = Double.parseDouble(currTrajectoryPoint[4]);
            long currTimeOffset = Long.parseLong(currTrajectoryPoint[3]);
            long time = startIndex == 0 ? firstTime : firstTime + currTimeOffset;
            newTraj.add(lon, lat, time, currSpeed, currHeading);
            long prevTimeOffset = time - firstTime;
            for (currIndex = currIndex + 1; currIndex < rawTrajectoryPointID.length; currIndex++) {
                currTrajectoryPoint = rawTrajectoryPointID[currIndex].split(":");
                lon = firstLon + (Double.parseDouble(currTrajectoryPoint[0]) / 100000);
                lat = firstLat + (Double.parseDouble(currTrajectoryPoint[1]) / 100000);
//                double distance = distFunc.pointToPointDistance(prevLon, prevLat, lon, lat);
//                if (distance < 2 * SIGMA)
//                    continue;
                currTimeOffset = Long.parseLong(currTrajectoryPoint[3]);
                long currTimeDiff = currTimeOffset - prevTimeOffset;
                time = firstTime + currTimeOffset;
                // the new point is inside the area and satisfies the time constraint
                if (isInside(lon, lat, roadGraph) && currTimeDiff <= (maxTimeInterval == -1 ? Long.MAX_VALUE : maxTimeInterval)) {
                    currSpeed = Double.parseDouble(currTrajectoryPoint[2]);
                    currHeading = Double.parseDouble(currTrajectoryPoint[4]);
                    prevTimeOffset = currTimeOffset;
                    newTraj.add(lon, lat, time, currSpeed, currHeading);
//                        prevLon = lon;
//                        prevLat = lat;
                } else {
                    break;
                }
            }

            GreatCircleDistanceFunction distFunc = new GreatCircleDistanceFunction();
            if (newTraj.duration() >= minTrajTimeSpan && newTraj.length(distFunc) >= 3 * minTrajTimeSpan) {   // the minimum average
                // speed should be larger than 10.8km/h
                newTraj.setId(tripID + "");
                tempTrajList.add(newTraj);
                tripID++;
            }
        }
        LOGGER.info("Trajectory filter finished, total number of candidates: " + tripID + ". Start the ground-truth map-matching.");

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
                    if (isMatchingResultNotContinuous(id2RoadWayMapping, bestMatchWayList))
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
                id2MatchingResult.put(matchedResult._1(), matchedResult._2());
            }
        }
        LOGGER.info("Ground-truth matching complete. Total number of valid matching result: " + matchedResultCount);

        tripID = 0;     // reset the trip ID for final trajectory id assignment
        for (Trajectory currTraj : tempTrajList) {
            if (id2MatchingResult.containsKey(Integer.parseInt(currTraj.getId()))) {
                String[] matchedRoadWayID = id2MatchingResult.get(Integer.parseInt(currTraj.getId()));
                // test whether the matching result pass through the area and continuous
                if (!isMatchingResultNotEnclosed(id2RoadWayMapping, matchedRoadWayID)) {
                    Pair<Integer, List<String>> newMatchingResult = new Pair<>(tripID, Arrays.asList(matchedRoadWayID));
                    currTraj.setId(tripID + "");
                    resultTrajList.add(currTraj);
                    gtResultRoadWayList.add(newMatchingResult);
                    for (String s : matchedRoadWayID) {
                        int currCount = id2VisitCountSmallMapping.get(s);
                        id2VisitCountSmallMapping.replace(s, currCount + 1);
                    }
                    totalNumOfPoint += currTraj.size();
                    tripID++;
                }
            }
        }
        if (requiredRecordNum != -1 && tempTrajList.size() == 1.5 * requiredRecordNum && tripID < requiredRecordNum)
            throw new IllegalArgumentException("ERROR! The cache for trajectory filter is too small. The final trajectory size is :" + tripID);
        LOGGER.info("Ground-truth trajectory result generated.");

        writeTrajectoryFile(resultTrajList, gtResultRoadWayList, createRawTrajFolder, createMatchedTrajFolder);

        // visit statistics
        int visitThreshold = 5;
        int totalHighVisitSmallCount = 0;
        int totalHighVisitLargeCount = 0;
        int totalVisitSmallCount = 0;
        int totalVisitLargeCount = 0;
        for (RoadWay w : rawGrantMap.getWays()) {
            id2VisitCountLargeMapping.put(w.getID(), w.getVisitCount());
        }
        roadGraph.setMaxVisitCount(0);
        for (RoadWay w : roadGraph.getWays()) {
            int visitSmallCount = id2VisitCountSmallMapping.get(w.getID());
            w.setVisitCount(visitSmallCount);
            if (visitSmallCount > 0) {
                totalVisitSmallCount++;
                if (visitSmallCount >= 5) {
                    totalHighVisitSmallCount++;
                }
            }
            if (id2VisitCountLargeMapping.containsKey(w.getID())) {
                int visitLargeCount = id2VisitCountLargeMapping.get(w.getID());
                if (visitLargeCount > 0) {
                    totalVisitLargeCount++;
                    if (visitLargeCount >= visitThreshold) {
                        totalHighVisitLargeCount++;
                    }
                }
            } else
                LOGGER.severe("ERROR! Road in new map doesn't exist in the original map");
        }
        DecimalFormat df = new DecimalFormat(".00000");
        LOGGER.info(tripID + " trajectories extracted, the average length: " + (int) (totalNumOfPoint / tripID) + ", max visit " +
                "count: " + roadGraph.getMaxVisitCount() + ".");
        LOGGER.info("Visit percentage: " + df.format((totalVisitSmallCount / (double) roadGraph.getWays().size()) * 100) + "%/" +
                df.format((totalVisitLargeCount / (double) roadGraph.getWays().size()) * 100) + "%, high visit(>=" + visitThreshold +
                "times): " + df.format((totalHighVisitSmallCount / (double) roadGraph.getWays().size()) * 100) + "%/" +
                df.format((totalHighVisitLargeCount / (double) roadGraph.getWays().size()) * 100) + "%.");
    }

    private void writeTrajectoryFile(List<Trajectory> resultTrajList, List<Pair<Integer, List<String>>> gtResultRoadWayList, File createRawTrajFolder, File createMatchedTrajFolder) {
        DecimalFormat df = new DecimalFormat("0.00000");
        if (resultTrajList == null || resultTrajList.isEmpty())
            throw new NullPointerException("ERROR! The output trajectory result list is empty.");
        if (gtResultRoadWayList == null || gtResultRoadWayList.isEmpty())
            throw new NullPointerException("ERROR! The output trajectory matching result list is empty.");
        if (resultTrajList.size() != gtResultRoadWayList.size())
            throw new IllegalArgumentException("ERROR! The counts of the output trajectories and their matching results are different.");
        Stream<Trajectory> resultTrajStream = resultTrajList.stream();
        Stream<Pair<Integer, List<String>>> gtResultRoadWayStream = gtResultRoadWayList.stream();

        // parallel processing
        ForkJoinPool forkJoinPool = ForkJoinPool.commonPool();
        forkJoinPool.submit(() -> resultTrajStream.parallel().forEach(trajectory -> {
            try {
                BufferedWriter bwRawTrajectory = new BufferedWriter(new FileWriter(createRawTrajFolder.getAbsolutePath() + "/trip_" +
                        trajectory.getId() + ".txt"));
                for (TrajectoryPoint p : trajectory) {
                    bwRawTrajectory.write(df.format(p.x()) + " " + df.format(p.y()) + " " + p.time() + " " + p.speed() + " " + p.heading() + "\n");
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
        while (Objects.requireNonNull(createRawTrajFolder.list()).length != resultTrajList.size() || Objects.requireNonNull
                (createMatchedTrajFolder.list()).length != gtResultRoadWayList.size()) {
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Check whether the ground-truth map-matching result satisfy the conditions that all roads must be included in the map area and the
     * map-matching result must be continuous.
     *
     * @param id2RoadWayMapping The road ID and the corresponding road way object.
     * @param matchedRoadWayID  The list of ground-truth map-matching result.
     * @return False if all map-matching result satisfy the requirement, otherwise true.
     */
    private boolean isMatchingResultNotEnclosed(Map<String, RoadWay> id2RoadWayMapping, String[] matchedRoadWayID) {
        RoadWay prevMatchRoad = null;
        for (String s : matchedRoadWayID) {
            if (!id2RoadWayMapping.containsKey(s)) { // current match road is not included in the map
                return true;
            } else if (prevMatchRoad != null) {    // check the connectivity of the match roadID
                RoadWay currRoad = id2RoadWayMapping.get(s);
                if (!prevMatchRoad.getToNode().getID().equals(currRoad.getFromNode().getID())) {  // break happens
//                    System.out.println("Matching result is not continuous.");
                    return true;
                } else
                    prevMatchRoad = id2RoadWayMapping.get(s);
            } else {
                prevMatchRoad = id2RoadWayMapping.get(s);
            }
        }
        return false;
    }

    /**
     * Check whether the ground-truth map-matching result satisfy the conditions that all roads must be included in the map area and the
     * map-matching result must be continuous.
     *
     * @param id2RoadWayMapping The road ID and the corresponding road way object.
     * @param matchedRoadWayID  The list of ground-truth map-matching result.
     * @return False if all map-matching result satisfy the requirement, otherwise true.
     */
    private boolean isMatchingResultNotContinuous(Map<String, RoadWay> id2RoadWayMapping,
                                                  String[] matchedRoadWayID) {
        RoadWay prevMatchRoad = null;
        boolean isNotTravelled = true;    // the current trajectory pass the map area
        for (String s : matchedRoadWayID) {
            if (id2RoadWayMapping.containsKey(s)) { // current match road is included in the map
                isNotTravelled = false;
                if (prevMatchRoad != null) {    // check the connectivity of the match roadID
                    RoadWay currRoad = id2RoadWayMapping.get(s);
                    if (!prevMatchRoad.getToNode().getID().equals(currRoad.getFromNode().getID())) {  // break happens
//                        System.out.println("Matching result is not continuous.");
                        return true;
                    } else
                        prevMatchRoad = id2RoadWayMapping.get(s);
                } else {
                    prevMatchRoad = id2RoadWayMapping.get(s);
                }
            } else {
                prevMatchRoad = null;
            }
        }
        return isNotTravelled;
    }

    private void initializeMapping(RoadNetworkGraph roadGraph, Map<String, Integer> id2VisitCountMapping, Map<String, RoadWay> id2RoadWayMapping) {
        for (RoadWay w : roadGraph.getWays())
            if (!id2VisitCountMapping.containsKey(w.getID())) {
                id2VisitCountMapping.put(w.getID(), 0);
                id2RoadWayMapping.put(w.getID(), w);
            } else LOGGER.severe("ERROR! The same road ID occurs twice: " + w.getID());
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
                LOGGER.info(trajectoryCount + " trajectory visited.");
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
                    LOGGER.severe("ERROR! Cannot find road" + s);
                if (id2WayLevel.containsKey(s))
                    levelCount[id2WayLevel.get(s)]++;
                else
                    LOGGER.severe("ERROR! Cannot find road" + s);
            }
            trajectoryCount++;
        }
        LOGGER.info("All trajectory visited. Total count: " + trajectoryCount);

        for (int i = 0; i < 10; i++)
            LOGGER.info("Way level " + i + " has " + levelCount[i] + " hits.");

        for (int i = 0; i < 25; i++)
            LOGGER.info("Way type " + i + " has " + typeCount[i] + " hits.");
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
        LOGGER.info("Generated ground-truth result required, start generating matching result.");
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
            String[] firstTrajectoryPoint = rawTrajectoryPointID[0].split(":");
            double firstLon = Double.parseDouble(firstTrajectoryPoint[0]) / 100000;
            double firstLat = Double.parseDouble(firstTrajectoryPoint[1]) / 100000;
            long firstTime = Long.parseLong(firstTrajectoryPoint[3]);
            double currSpeed = Double.parseDouble(firstTrajectoryPoint[2]);
            double currHeading = Double.parseDouble(firstTrajectoryPoint[4]);
            TrajectoryPoint currPoint = new TrajectoryPoint(firstLon, firstLat, firstTime, currSpeed, currHeading);
            traj.add(currPoint);
            double prevLon = firstLon;
            double prevLat = firstLat;
            for (int i = 1; i < rawTrajectoryPointID.length; i++) {
                String[] currTrajectoryPoint = rawTrajectoryPointID[i].split(":");
                double lon = firstLon + (Double.parseDouble(currTrajectoryPoint[0]) / 100000);
                double lat = firstLat + (Double.parseDouble(currTrajectoryPoint[1]) / 100000);
                long currTime = Long.parseLong(currTrajectoryPoint[3]);
                double distance = distFunc.pointToPointDistance(prevLon, prevLat, lon, lat);
                if (distance < minDist)
                    continue;
                long time = firstTime + currTime;
                currSpeed = Double.parseDouble(currTrajectoryPoint[2]);
                currHeading = Double.parseDouble(currTrajectoryPoint[4]);
                currPoint = new TrajectoryPoint(lon, lat, time, currSpeed, currHeading);
                traj.add(currPoint);
                prevLon = lon;
                prevLat = lat;
            }
            inputTrajList.add(new Pair<>(traj, rawTrajectoryPoints));
        }

        LOGGER.info("Start ground-truth generation, total number of input trajectory: " + inputTrajList.size());

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
        LOGGER.info("Ground-truth matching complete, total number of matched trajectories: " + tripCount + " start writing file");
        bwRawTrajectory.close();
        LOGGER.info("Ground-truth trajectory result generated.");
    }

}