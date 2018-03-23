package edu.uq.dke.mapupdate.io;

import edu.uq.dke.mapupdate.datatype.TrajectoryMatchResult;
import traminer.util.map.matching.PointNodePair;
import traminer.util.spatial.objects.st.STPoint;
import traminer.util.trajectory.Trajectory;

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
    String outputFolder;

    public CSVTrajectoryWriter(String outputPath) {
        this.outputFolder = outputPath;
    }

    /**
     * writer for writing matching result and the input of the inference step
     *
     * @param matchingList matching result
     */
    public void matchedTrajectoryWriter(List<TrajectoryMatchResult> matchingList) {
        File matchedResultFolder = new File(this.outputFolder + "matchedResult/");
        File roadIDListFolder = new File(this.outputFolder + "matchedRoadID/");
        int tripCount = 0;
        int pointCount = 1;
        if (!matchedResultFolder.exists()) {
            matchedResultFolder.mkdirs();
        }
        if (!roadIDListFolder.exists()) {
            roadIDListFolder.mkdirs();
        }
        if (matchedResultFolder.isDirectory() && roadIDListFolder.isDirectory()) {
            if (matchedResultFolder.listFiles() != null) {
                for (File f : Objects.requireNonNull(matchedResultFolder.listFiles())) {
                    f.delete();
                }
            }
            if (roadIDListFolder.listFiles() != null) {
                for (File f : Objects.requireNonNull(roadIDListFolder.listFiles())) {
                    f.delete();
                }
            }
            DecimalFormat df = new DecimalFormat(".00000");
            for (TrajectoryMatchResult w : matchingList) {
                try {
                    BufferedWriter bwMatchedTrajectory = new BufferedWriter(new FileWriter(this.outputFolder + "matchedResult/matchedtrip_" + w.getTrajID() + ".txt"));
                    BufferedWriter roadIDFromTrajectory = new BufferedWriter(new FileWriter(this.outputFolder + "matchedRoadID/matchedtripID_" + w.getTrajID() + ".txt"));
                    for (PointNodePair p : w.getMatchingResult()) {
                        if (p.getMatchingPoint() != null) {
                            bwMatchedTrajectory.write(df.format(p.getMatchingPoint().lon()) + " " + df.format(p.getMatchingPoint().lat()) + " " + p.getPoint().time() + " " + p.getMatchingPoint().getRoadID() + "\n");
                        } else {
                            // no point matched, use original point instead
                            bwMatchedTrajectory.write(df.format(p.getPoint().x()) + " " + df.format(p.getPoint().y()) + " " + p.getPoint().time() + " " + 0 + "\n");
                        }
                        pointCount++;
                    }
                    for (String s : w.getMatchWayList()) {
                        roadIDFromTrajectory.write(s + "\n");
                    }
                    tripCount += 1;
                    bwMatchedTrajectory.close();
                    roadIDFromTrajectory.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else System.err.println("Matched trajectory output path is incorrect:" + this.outputFolder);
        System.out.println("Matched road ways written, total files:" + tripCount + ", total trajectory points:" + (pointCount - 1));
    }

    /**
     * writer for writing raw trajectories
     *
     * @param trajectoryList output trajectories
     */
    public void trajectoryWriter(List<Trajectory> trajectoryList) {
        File outputTrajectoryFolder = new File(this.outputFolder + "unmatchedTraj/");
        File nextInputUnmatchedTrajectoryFolder = new File(this.outputFolder + "unmatchedNextInput/");
        int tripCount = 0;
        int pointCount = 1;
        if (!outputTrajectoryFolder.exists()) {
            outputTrajectoryFolder.mkdirs();
        }
        if (!nextInputUnmatchedTrajectoryFolder.exists()) {
            nextInputUnmatchedTrajectoryFolder.mkdirs();
        }
        if (outputTrajectoryFolder.isDirectory() && nextInputUnmatchedTrajectoryFolder.isDirectory()) {
            if (outputTrajectoryFolder.listFiles() != null) {
                for (File f : outputTrajectoryFolder.listFiles()) {
                    f.delete();
                }
            }
            if (nextInputUnmatchedTrajectoryFolder.listFiles() != null) {
                for (File f : nextInputUnmatchedTrajectoryFolder.listFiles()) {
                    f.delete();
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
