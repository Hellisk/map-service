package edu.uq.dke.mapupdate.util.io;

import edu.uq.dke.mapupdate.util.object.datastructure.TrajectoryMatchResult;
import edu.uq.dke.mapupdate.util.object.spatialobject.STPoint;
import edu.uq.dke.mapupdate.util.object.spatialobject.Trajectory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * Created by uqpchao on 23/05/2017.
 */
public class CSVTrajectoryWriter {
    private String outputFolder;

    public CSVTrajectoryWriter(String outputPath) {
        this.outputFolder = outputPath;
    }

    /**
     * writer for writing matching result and the input of the inference step
     *
     * @param matchingList matching result
     */
    public void matchedTrajectoryWriter(List<TrajectoryMatchResult> matchingList, int rankLength) throws IOException {
        File matchedResultFolder = new File(this.outputFolder + "matchedResult/");
        File roadIDListFolder = new File(this.outputFolder + "matchedRoadID/");
        int tripCount = 0;
        int pointCount = 0;
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
            DecimalFormat df = new DecimalFormat(".00000");
            for (TrajectoryMatchResult w : matchingList) {
                try {
                    BufferedWriter bwMatchedTrajectory = new BufferedWriter(new FileWriter(this.outputFolder + "matchedResult/matchedtrip_" + w.getTrajID() + ".txt"));
                    BufferedWriter roadIDFromTrajectory = new BufferedWriter(new FileWriter(this.outputFolder + "matchedRoadID/matchedtripID_" + w.getTrajID() + ".txt"));

                    // write point matching result, format ((raw trajectory) lon,lat,time|(matching result rank 1)lon,lat,roadID|lon,lat,
                    // roadID|...)
                    for (int i = 0; i < w.getTrajLength(); i++) {

                        bwMatchedTrajectory.write(df.format(w.getTrajPoint(i).x()) + " " + df.format(w.getTrajPoint(i).y()) + w
                                .getTrajPoint(i).time());
                        int maxRank = w.getNumOfPositiveRank(); // matching results whose ranks are larger than maxRank are definitely empty
                        for (int j = 0; j < rankLength; j++) {
                            if (j < maxRank && w.getMatchingResult(j).size() > i) {
                                bwMatchedTrajectory.write("|" + df.format(w.getMatchingResult(j).get(i).lon()) + "," + df.format(w
                                        .getMatchingResult(j).get(i).lat()) + "," + w.getMatchingResult(j).get(i).getRoadID());
                            } else {
                                // no point matched, use null instead
                                bwMatchedTrajectory.write("|null");
                            }
                        }
                        bwMatchedTrajectory.write("\n");

                        pointCount++;
                    }

                    // start writing road way list, each line refers to one rank, format(roadID,roadID,...|probability)
                    for (int i = 0; i < rankLength; i++) {
                        List<String> matchWayList = w.getMatchWayList(i);
                        for (int j = 0; j < matchWayList.size() - 1; j++) {
                            roadIDFromTrajectory.write(matchWayList.get(j) + ",");
                        }
                        roadIDFromTrajectory.write(matchWayList.get(matchWayList.size() - 1) + "|");
                        roadIDFromTrajectory.write(w.getProbability(i) + "");
                    }
                    tripCount += 1;
                    bwMatchedTrajectory.close();
                    roadIDFromTrajectory.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else System.err.println("Matched trajectory output path is incorrect:" + this.outputFolder);
        System.out.println("Matched road ways written, total files:" + tripCount + ", total trajectory points:" + pointCount);
    }

    /**
     * writer for writing raw trajectories
     *
     * @param trajectoryList output trajectories
     */
    public void trajectoryWriter(List<Trajectory> trajectoryList) throws IOException {
        File outputTrajectoryFolder = new File(this.outputFolder + "unmatchedTraj/");
        File nextInputUnmatchedTrajectoryFolder = new File(this.outputFolder + "unmatchedNextInput/");
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
            for (Trajectory w : trajectoryList) {
                try {
                    BufferedWriter bwTrajectory = new BufferedWriter(new FileWriter(this.outputFolder + "unmatchedTraj/trip_" + w.getId() + ".txt"));
                    BufferedWriter nextInputUnmatchedTrajectory = new BufferedWriter(new FileWriter(this.outputFolder + "unmatchedNextInput/trip_" + w.getId() + ".txt"));
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
        } else System.err.println("Trajectory output path is incorrect:" + this.outputFolder);
        System.out.println("Trajectories written, total files:" + tripCount + ", total trajectory points:" + (pointCount - 1));
    }
}
