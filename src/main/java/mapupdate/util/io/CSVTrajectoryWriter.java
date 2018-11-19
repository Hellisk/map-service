package mapupdate.util.io;

import mapupdate.util.object.datastructure.PointMatch;
import mapupdate.util.object.datastructure.TrajectoryMatchingResult;
import mapupdate.util.object.datastructure.Triplet;
import mapupdate.util.object.spatialobject.Trajectory;
import mapupdate.util.object.spatialobject.TrajectoryPoint;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Stream;

import static mapupdate.Main.*;

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
            matchedResultFolder = new File(this.outputFolder + "matchedResult/TP" + MIN_TRAJ_TIME_SPAN + "_TI" +
                    MAX_TIME_INTERVAL + "_TC" + TRAJECTORY_COUNT + "/");
            roadIDListFolder = new File(this.outputFolder + "matchedRoadID/TP" + MIN_TRAJ_TIME_SPAN + "_TI" +
                    MAX_TIME_INTERVAL + "_TC" + TRAJECTORY_COUNT + "/");
        } else {
            matchedResultFolder = new File(this.outputFolder + "matchedResult/TP" + MIN_TRAJ_TIME_SPAN + "_TI" +
                    MAX_TIME_INTERVAL + "_TC" + TRAJECTORY_COUNT + "/" + iteration + "/");
            roadIDListFolder = new File(this.outputFolder + "matchedRoadID/TP" + MIN_TRAJ_TIME_SPAN + "_TI" +
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
            forkJoinPool.submit(() -> trajectoryMatchingStream.parallel().forEach(matchingResult -> writeMatchedTrajectoryRecord(matchedResultFolder, roadIDListFolder, matchingResult, rankLength)));
            while (Objects.requireNonNull(matchedResultFolder.list()).length != matchingList.size() && Objects.requireNonNull(roadIDListFolder.list())
                    .length != matchingList.size()) {
                try {
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } else LOGGER.severe("Matched trajectory output path is incorrect: " + this.outputFolder);
        LOGGER.info("Matched road ways written, total files: " + matchingList.size());
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
            for (int i = 0; i < matchingResult.getTrajSize(); i++) {

                bwMatchedTrajectory.write(df.format(matchingResult.getTrajPoint(i).x()) + " " + df.format(matchingResult.getTrajPoint(i).y()) + " " + matchingResult
                        .getTrajPoint(i).time() + " " + matchingResult.getTrajPoint(i).speed() + " " + matchingResult.getTrajPoint(i).heading());
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
     * Write unmatched trajectories to files
     *
     * @param trajectoryInfoList List of unmatched trajectories and their start and end anchor points.
     */
    public void writeUnmatchedTrajectory(List<Triplet<Trajectory, String, String>> trajectoryInfoList, int iteration) throws IOException {
        File outputTrajectoryFolder;
        File nextInputUnmatchedTrajectoryFolder;
        if (iteration == -1) {
            outputTrajectoryFolder = new File(this.outputFolder + "unmatchedTraj/");
            nextInputUnmatchedTrajectoryFolder = new File(this.outputFolder + "unmatchedNextInput/");
        } else {
            outputTrajectoryFolder = new File(this.outputFolder + "unmatchedTraj/TP" + MIN_TRAJ_TIME_SPAN + "_TI" +
                    MAX_TIME_INTERVAL + "_TC" + TRAJECTORY_COUNT + "/" + iteration + "/");
            nextInputUnmatchedTrajectoryFolder = new File(this.outputFolder + "unmatchedNextInput/TP" + MIN_TRAJ_TIME_SPAN + "_TI" +
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
            for (Triplet<Trajectory, String, String> info : trajectoryInfoList) {
                try {
                    BufferedWriter bwTrajectory;
                    BufferedWriter nextInputUnmatchedTrajectory;
                    Trajectory w = info._1();
                    if (!id2UnmatchedTrajCount.containsKey(w.getID())) {
                        bwTrajectory = new BufferedWriter(new FileWriter(outputTrajectoryFolder.toString() + "/trip_" + w.getID() + "_0.txt"));
                        nextInputUnmatchedTrajectory = new BufferedWriter(new FileWriter(nextInputUnmatchedTrajectoryFolder +
                                "/trip_" + w.getID() + "_0.txt"));
                        id2UnmatchedTrajCount.put(w.getID(), 1);
                    } else {
                        String additionalFileName = w.getID() + "_" + id2UnmatchedTrajCount.get(w.getID());
                        bwTrajectory = new BufferedWriter(new FileWriter(outputTrajectoryFolder.toString() + "/trip_" +
                                additionalFileName + ".txt"));
                        nextInputUnmatchedTrajectory = new BufferedWriter(new FileWriter(nextInputUnmatchedTrajectoryFolder +
                                "/trip_" + additionalFileName + ".txt"));
                        id2UnmatchedTrajCount.replace(w.getID(), id2UnmatchedTrajCount.get(w.getID()) + 1);
                    }
                    Iterator<TrajectoryPoint> iter = w.iterator();
                    int startPointCount = pointCount;
                    int matchingPointCount = w.getSTPoints().size();

                    // start writing the anchor road ID, then the trajectory points
                    if (info._2().equals("") || info._3().equals("") || info._2() == null || info._3() == null)
                        LOGGER.warning("WARNING! Anchor road is missing.");
                    bwTrajectory.write(info._2() + "," + info._3() + "\n");
                    while (iter.hasNext()) {
                        TrajectoryPoint p = iter.next();
                        bwTrajectory.write(p.x() + " " + p.y() + " " + p.time() + " " + p.speed() + " " + p.heading() + "\n");
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
        } else LOGGER.severe("Trajectory output path is incorrect:" + outputTrajectoryFolder.toString());
        LOGGER.info("Trajectories written, total files:" + tripCount + ", total trajectory points:" + (pointCount - 1));
    }

    /**
     * Merge the matching result generated before and after the map refinement and form the final map-matching result.
     *
     * @param rawMatches     The original map-matching result before map refinement.
     * @param refinedMatches The map-matching results affected by the map refinement.
     * @param rankLength     The rank k.
     * @param iteration      The current iteration number.
     * @return The merged map-matching result as the map-matching output of the current iteration.
     * @throws IOException File write error.
     */
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

    /**
     * Merge the unmatched trajectory sets generated before and after refinement and form the final unmatched trajectory set.
     *
     * @param rawUnmatchedTrajectoryList     Unmatched trajectory set before refinement.
     * @param rematchTrajList                   List of trajectories that has been re-matched.
     * @param refinedUnmatchedTrajectoryList Unmatched trajectory set after refinement.
     * @param iteration                      The current iteration number.
     * @throws IOException File write error.
     */
    public void writeMergedUnmatchedTrajectory(List<Triplet<Trajectory, String, String>> rawUnmatchedTrajectoryList, List<Trajectory> rematchTrajList, List<Triplet<Trajectory, String, String>>
            refinedUnmatchedTrajectoryList, int iteration) throws IOException {
        Set<String> refinedUnmatchedTrajSet = new HashSet<>();
        for (Trajectory mr : rematchTrajList)
            refinedUnmatchedTrajSet.add(mr.getID());
        rawUnmatchedTrajectoryList.removeIf(next -> refinedUnmatchedTrajSet.contains(next._1().getID()));
        rawUnmatchedTrajectoryList.addAll(refinedUnmatchedTrajectoryList);
        writeUnmatchedTrajectory(rawUnmatchedTrajectoryList, iteration);
    }

}
