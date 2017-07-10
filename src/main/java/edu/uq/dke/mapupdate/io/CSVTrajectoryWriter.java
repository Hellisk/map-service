package edu.uq.dke.mapupdate.io;

import traminer.util.map.roadnetwork.RoadNode;
import traminer.util.map.roadnetwork.RoadWay;
import traminer.util.spatial.objects.st.STPoint;
import traminer.util.trajectory.Trajectory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;

/**
 * Created by uqpchao on 23/05/2017.
 */
public class CSVTrajectoryWriter {
    public static void matchedTrajectoryWriter(List<RoadWay> roadWayList, String outputTrajectoryFolder) {
        File outputFolder = new File(outputTrajectoryFolder);
        int tripCount = 0;
        if (!outputFolder.exists()) {
            outputFolder.mkdirs();
        }
        if (outputFolder.isDirectory()) {
            if (outputFolder.listFiles() != null) {
                for (File f : outputFolder.listFiles()) {
                    f.delete();
                }
            }
            DecimalFormat df = new DecimalFormat(".00000");
            for (RoadWay w : roadWayList) {
                try {
                    BufferedWriter bwMatchedTrajectory = new BufferedWriter(new FileWriter(outputTrajectoryFolder + "matchedtrip_" + w.getId() + ".txt"));
                    for (RoadNode p : w.getNodes()) {
                        bwMatchedTrajectory.write(df.format(p.lon()) + " " + df.format(p.lat()) + " " + 0 + " " + p.getId() + "\n");
                    }
                    tripCount += 1;
                    bwMatchedTrajectory.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else System.err.println("Trajectory out path is incorrect:" + outputTrajectoryFolder);
        System.out.println("Matched road ways written, total files:" + tripCount);
    }

    public static void trajectoryWriter(List<Trajectory> trajectoryList, String outputTrajectoryFolder) {
        File outputFolder = new File(outputTrajectoryFolder);
        int tripCount = 0;
        if (!outputFolder.exists()) {
            outputFolder.mkdirs();
        }
        if (outputFolder.isDirectory()) {
            if (outputFolder.listFiles() != null) {
                for (File f : outputFolder.listFiles()) {
                    f.delete();
                }
            }

            for (Trajectory w : trajectoryList) {
                try {
                    BufferedWriter bwTrajectory = new BufferedWriter(new FileWriter(outputTrajectoryFolder + "trip_" + w.getId().substring(w.getId().indexOf("T") + 1) + ".txt"));
                    for (STPoint p : w.getPoints()) {
                        bwTrajectory.write(p.x() + " " + p.y() + " " + p.time() + "\n");
                    }
                    tripCount += 1;
                    bwTrajectory.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else System.err.println("Trajectory out path is incorrect:" + outputTrajectoryFolder);
        System.out.println("Road ways written, total files:" + tripCount);
    }
}
