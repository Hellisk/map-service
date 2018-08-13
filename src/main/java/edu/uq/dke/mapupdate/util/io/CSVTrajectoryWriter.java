package edu.uq.dke.mapupdate.util.io;

import edu.uq.dke.mapupdate.util.object.datastructure.Pair;
import edu.uq.dke.mapupdate.util.object.datastructure.PointMatch;
import edu.uq.dke.mapupdate.util.object.datastructure.TrajectoryMatchingResult;
import edu.uq.dke.mapupdate.util.object.spatialobject.STPoint;
import edu.uq.dke.mapupdate.util.object.spatialobject.Trajectory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.stream.Stream;

import static edu.uq.dke.mapupdate.Main.*;

/**
 * Created by uqpchao on 23/05/2017.
 */
public class CSVTrajectoryWriter {
    private String outputFolder;

    public CSVTrajectoryWriter(String outputPath) {
        this.outputFolder = outputPath;
    }

    /**
     * Writer for writing matching result and the input of the inference step
     * Matching result format: raw point lon lat time|match point lon,lat,match segment lon1,lat1,lon2,lat2,time
     *
     * @param matchingList matching result
     */
    public void writeMatchedTrajectory(List<TrajectoryMatchingResult> matchingList, int rankLength, int iteration) throws IOException {
        File matchedResultFolder;
        File roadIDListFolder;
        if (iteration == -1) {
            matchedResultFolder = new File(this.outputFolder + "matchedResult/TP" + MIN_TRAJ_POINT_COUNT + "_TI" +
                    MAX_TIME_INTERVAL + "_TC" + TRAJECTORY_COUNT + "/");
            roadIDListFolder = new File(this.outputFolder + "matchedRoadID/TP" + MIN_TRAJ_POINT_COUNT + "_TI" +
                    MAX_TIME_INTERVAL + "_TC" + TRAJECTORY_COUNT + "/");
        } else {
            matchedResultFolder = new File(this.outputFolder + "matchedResult/TP" + MIN_TRAJ_POINT_COUNT + "_TI" +
                    MAX_TIME_INTERVAL + "_TC" + TRAJECTORY_COUNT + "/" + iteration + "/");
            roadIDListFolder = new File(this.outputFolder + "matchedRoadID/TP" + MIN_TRAJ_POINT_COUNT + "_TI" +
                    MAX_TIME_INTERVAL + "_TC" + TRAJECTORY_COUNT + "/" + iteration + "/");
        }
        if (!matchedResultFolder.exists()) {
            if (!matchedResultFolder.mkdirs()) throw new IOException("ERROR! Failed to create folder.");
        }
        if (!roadIDListFolder.exists()) {
            if (!roadIDListFolder.mkdirs()) throw new IOException("ERROR! Failed to create folder.");
        }
        if (matchedResultFolder.isDirectory() && roadIDListFolder.isDirectory()) {
            if (matchedResultFolder.listFiles() != null) {
                for (File f : Objects.requireNonNull(matchedResultFolder.listFiles())) {
                    if (!f.delete()) throw new IOException("ERROR! Failed to delete file.");
                }
            }
            if (roadIDListFolder.listFiles() != null) {
                for (File f : Objects.requireNonNull(roadIDListFolder.listFiles())) {
                    if (!f.delete()) throw new IOException("ERROR! Failed to delete file.");
                }
            }
            Stream<TrajectoryMatchingResult> trajectoryMatchingStream = matchingList.stream();

            if (trajectoryMatchingStream == null)
                throw new NullPointerException("ERROR! The input matching result list is empty.");

            // parallel processing
            ForkJoinPool forkJoinPool = ForkJoinPool.commonPool();
            forkJoinPool.submit(() -> trajectoryMatchingStream.parallel().forEach(matchingResult -> {
                writeMatchedTrajectoryRecord(matchedResultFolder, roadIDListFolder, matchingResult, rankLength);
            }));
            while (Objects.requireNonNull(matchedResultFolder.list()).length != matchingList.size() && Objects.requireNonNull(roadIDListFolder.list())
                    .length != matchingList.size()) {
                try {
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } else System.err.println("Matched trajectory output path is incorrect: " + this.outputFolder);
        System.out.println("Matched road ways written, total files: " + matchingList.size());
    }

    private void writeMatchedTrajectoryRecord(File matchedResultFolder, File roadIDListFolder, TrajectoryMatchingResult matchingResult,
                                              int rankLength) {
        DecimalFormat df = new DecimalFormat("0.00000");
        try {
            BufferedWriter bwMatchedTrajectory = new BufferedWriter(new FileWriter(matchedResultFolder.toString() +
                    "/matchedtrip_" + matchingResult.getTrajID() + ".txt"));
            BufferedWriter roadIDFromTrajectory = new BufferedWriter(new FileWriter(roadIDListFolder.toString() + "/matchedtripID_"
                    + matchingResult.getTrajID() + ".txt"));

            // write point matching result, format ((raw trajectory) lon,lat,time|(matching result rank 1)lon,lat,roadID|lon,lat,
            // roadID|...)
            for (int i = 0; i < matchingResult.getTrajLength(); i++) {

                bwMatchedTrajectory.write(df.format(matchingResult.getTrajPoint(i).x()) + " " + df.format(matchingResult.getTrajPoint(i).y()) + " " + matchingResult
                        .getTrajPoint(i).time());
                int maxRank = matchingResult.getNumOfPositiveRank(); // matching results whose ranks are larger than maxRank are definitely empty
                for (int j = 0; j < rankLength; j++) {
                    if (j < maxRank && matchingResult.getMatchingResult(j).size() > i) {
                        PointMatch currMatch = matchingResult.getMatchingResult(j).get(i);
                        if (!currMatch.getRoadID().equals(""))
                            // write the information of trajectory matching result, including match point, match segment and road id
                            bwMatchedTrajectory.write("|" + df.format(currMatch.lon()) + "," + df.format(currMatch.lat()) + "," +
                                    df.format(currMatch.getMatchedSegment().x1()) + "," + df.format(currMatch.getMatchedSegment()
                                    .y1()) + "," + df.format(currMatch.getMatchedSegment().x2()) + "," + df.format(currMatch
                                    .getMatchedSegment().y2()) + "," + currMatch.getRoadID());
                        else bwMatchedTrajectory.write("|null");
                    } else {
                        // no point matched, use null instead
                        bwMatchedTrajectory.write("|null");
                    }
                }
                bwMatchedTrajectory.write("\n");
            }

            // start writing road way list, each line refers to one rank, format(roadID,roadID,...|probability)
            for (int i = 0; i < rankLength; i++) {
                List<String> matchWayList = matchingResult.getMatchWayList(i);
                for (int j = 0; j < matchWayList.size() - 1; j++) {
                    roadIDFromTrajectory.write(matchWayList.get(j) + ",");
                }
                if (matchWayList.size() != 0) {
                    roadIDFromTrajectory.write(matchWayList.get(matchWayList.size() - 1) + "|");
                } else
                    roadIDFromTrajectory.write("null|");

                roadIDFromTrajectory.write(matchingResult.getProbability(i) + "" + "\n");
            }
            bwMatchedTrajectory.close();
            roadIDFromTrajectory.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * writer for writing raw trajectories
     *
     * @param trajectoryList output trajectories
     */
    public void writeUnmatchedTrajectory(List<Trajectory> trajectoryList, int iteration) throws IOException {
        File outputTrajectoryFolder;
        File nextInputUnmatchedTrajectoryFolder;
        if (iteration == -1) {
            outputTrajectoryFolder = new File(this.outputFolder + "unmatchedTraj/");
            nextInputUnmatchedTrajectoryFolder = new File(this.outputFolder + "unmatchedNextInput/");
        } else {
            outputTrajectoryFolder = new File(this.outputFolder + "unmatchedTraj/TP" + MIN_TRAJ_POINT_COUNT + "_TI" +
                    MAX_TIME_INTERVAL + "_TC" + TRAJECTORY_COUNT + "/" + iteration + "/");
            nextInputUnmatchedTrajectoryFolder = new File(this.outputFolder + "unmatchedNextInput/TP" + MIN_TRAJ_POINT_COUNT + "_TI" +
                    MAX_TIME_INTERVAL + "_TC" + TRAJECTORY_COUNT + "/" + iteration + "/");
        }
        int tripCount = 0;
        int pointCount = 1;
        if (!outputTrajectoryFolder.exists()) {
            if (!outputTrajectoryFolder.mkdirs()) throw new IOException("ERROR! Failed to create folder.");
        }
        if (!nextInputUnmatchedTrajectoryFolder.exists()) {
            if (!nextInputUnmatchedTrajectoryFolder.mkdirs()) throw new IOException("ERROR! Failed to create folder.");
        }
        if (outputTrajectoryFolder.isDirectory() && nextInputUnmatchedTrajectoryFolder.isDirectory()) {
            if (outputTrajectoryFolder.listFiles() != null) {
                for (File f : Objects.requireNonNull(outputTrajectoryFolder.listFiles())) {
                    if (!f.delete()) throw new IOException("ERROR! Failed to delete file.");
                }
            }
            if (nextInputUnmatchedTrajectoryFolder.listFiles() != null) {
                for (File f : Objects.requireNonNull(nextInputUnmatchedTrajectoryFolder.listFiles())) {
                    if (!f.delete()) throw new IOException("ERROR! Failed to delete file.");
                }
            }
            Map<String, Integer> id2UnmatchedTrajCount = new HashMap<>();
            for (Trajectory w : trajectoryList) {
                try {
                    BufferedWriter bwTrajectory;
                    BufferedWriter nextInputUnmatchedTrajectory;
                    if (!id2UnmatchedTrajCount.containsKey(w.getId())) {
                        bwTrajectory = new BufferedWriter(new FileWriter(outputTrajectoryFolder.toString() + "/trip_" + w
                                .getId() + "_0.txt"));
                        nextInputUnmatchedTrajectory = new BufferedWriter(new FileWriter(nextInputUnmatchedTrajectoryFolder +
                                "/trip_" + w.getId() + "_0.txt"));
                    } else {
                        String additionalFileName = w.getId() + "_" + id2UnmatchedTrajCount.get(w.getId());
                        bwTrajectory = new BufferedWriter(new FileWriter(outputTrajectoryFolder.toString() + "/trip_" +
                                additionalFileName + ".txt"));
                        nextInputUnmatchedTrajectory = new BufferedWriter(new FileWriter(nextInputUnmatchedTrajectoryFolder +
                                "/trip_" + additionalFileName + ".txt"));
                        id2UnmatchedTrajCount.replace(w.getId(), id2UnmatchedTrajCount.get(w.getId()) + 1);
                    }
                    Iterator<STPoint> iter = w.iterator();
                    int startPointCount = pointCount;
                    int matchingPointCount = w.getSTPoints().size();
                    while (iter.hasNext()) {
                        STPoint p = iter.next();
                        bwTrajectory.write(p.x() + " " + p.y() + " " + p.time() + "\n");
                        nextInputUnmatchedTrajectory.write(pointCount + "," + p.y() + "," + p.x() + "," + p.time() + "," + (pointCount == startPointCount ? "None" : pointCount - 1) + "," + (pointCount != startPointCount + matchingPointCount - 1 ? pointCount + 1 : "None") + "\n");
                        pointCount++;
                    }
                    tripCount++;
                    bwTrajectory.close();
                    nextInputUnmatchedTrajectory.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else System.err.println("Trajectory output path is incorrect:" + outputTrajectoryFolder.toString());
        System.out.println("Trajectories written, total files:" + tripCount + ", total trajectory points:" + (pointCount - 1));
    }

    public List<TrajectoryMatchingResult> writeMergedMatchedTrajectory(List<TrajectoryMatchingResult> rawMatches,
                                                                       List<TrajectoryMatchingResult> refinedMatches, int rankLength, int iteration) throws IOException {
        Set<String> refinedMatchingResultSet = new HashSet<>();
        for (TrajectoryMatchingResult mr : refinedMatches)
            refinedMatchingResultSet.add(mr.getTrajID());
        rawMatches.removeIf(next -> refinedMatchingResultSet.contains(next.getTrajID()));
        rawMatches.addAll(refinedMatches);
        writeMatchedTrajectory(rawMatches, rankLength, iteration);
        return rawMatches;
    }

    public void writeMergedUnmatchedTrajectory(List<Trajectory> rawUnmatchedTrajectoryList, List<Trajectory>
            refinedUnmatchedTrajectoryList, int iteration) throws IOException {
        Set<String> refinedUnmatchedTrajSet = new HashSet<>();
        for (Trajectory mr : refinedUnmatchedTrajectoryList)
            refinedUnmatchedTrajSet.add(mr.getId());
        rawUnmatchedTrajectoryList.removeIf(next -> refinedUnmatchedTrajSet.contains(next.getId()));
        rawUnmatchedTrajectoryList.addAll(refinedUnmatchedTrajectoryList);
        writeUnmatchedTrajectory(rawUnmatchedTrajectoryList, iteration);

    }
}
