package edu.uq.dke.mapupdate.mapmatching.io;

import traminer.util.map.roadnetwork.RoadNode;
import traminer.util.map.roadnetwork.RoadWay;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * Created by uqpchao on 23/05/2017.
 */
public class CSVTrajectoryWriter {
    public static void TrajectoryWriter(List<RoadWay> roadWayList, String outputTrajectoryFolder) {
        File outputFolder = new File(outputTrajectoryFolder);
        int tripCount = 0;
        if (!outputFolder.exists()) {
            outputFolder.mkdirs();
        }
        if (outputFolder.isDirectory()) {

            for (RoadWay outputTrajectory : roadWayList) {
                try {
                    BufferedWriter bwTrajectory = new BufferedWriter(new FileWriter(outputTrajectoryFolder + "trip_" + tripCount + ".txt"));
                    for (RoadNode p : outputTrajectory.getNodes()) {
                        bwTrajectory.write(p.lon() + " " + p.lat() + " " + 0 + "\n");
                    }
                    tripCount += 1;
                    bwTrajectory.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else System.err.println("Trajectory out path is incorrect:" + outputTrajectoryFolder);
    }
}
