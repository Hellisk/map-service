package edu.uq.dke.mapupdate.io;

import traminer.util.map.roadnetwork.RoadNode;
import traminer.util.map.roadnetwork.RoadWay;
import traminer.util.spatial.objects.st.STPoint;
import traminer.util.trajectory.Trajectory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Created by uqpchao on 23/05/2017.
 */
public class CSVTrajectoryReader {
    public CSVTrajectoryReader() {
    }

    private Trajectory readTrajectory(File trajectoryFile) throws IOException {
        BufferedReader brTrajectory = new BufferedReader(new FileReader(trajectoryFile));
        Trajectory newTrajectory = new Trajectory();
        String line;
        while ((line = brTrajectory.readLine()) != null) {
            String[] pointInfo = line.split(" ");
            STPoint newSTPoint = new STPoint(Double.parseDouble(pointInfo[0]), Double.parseDouble(pointInfo[1]), (long) Double.parseDouble(pointInfo[2]));
            newTrajectory.add(newSTPoint);
        }
        brTrajectory.close();
        return newTrajectory;
    }

    private RoadWay readMatchedTrajectory(File trajectoryFile) throws IOException {
        BufferedReader brTrajectory = new BufferedReader(new FileReader(trajectoryFile));
        RoadWay newMatchedTrajectory = new RoadWay();
        String line;
        while ((line = brTrajectory.readLine()) != null) {
            String[] pointInfo = line.split(" ");
            if (pointInfo.length == 4) {
                RoadNode newRoadNode = new RoadNode(pointInfo[3], Double.parseDouble(pointInfo[0]), Double.parseDouble(pointInfo[1]));
                newMatchedTrajectory.addNode(newRoadNode);
            }
        }
        brTrajectory.close();
        return newMatchedTrajectory;
    }

    public Stream<Trajectory> readTrajectoryFiles(String csvTrajectoryPath) throws IOException {
        File inputFile = new File(csvTrajectoryPath);
        List<Trajectory> trajectoryList = new ArrayList<>();
        if (inputFile.isDirectory()) {
            File[] trajectoryFiles = inputFile.listFiles();
            if (trajectoryFiles != null) {
                for (File trajectoryFile : trajectoryFiles) {
                    Trajectory newTrajectory = readTrajectory(trajectoryFile);
                    newTrajectory.setId(trajectoryFile.getName().substring(trajectoryFile.getName().indexOf('_') + 1, trajectoryFile.getName().indexOf('.')));
                    trajectoryList.add(newTrajectory);
                }
            }
        } else {
            trajectoryList.add(readTrajectory(inputFile));
        }
        int count = 0;
        for (Trajectory t : trajectoryList) {
            count += t.getCoordinates().size();
        }
        System.out.println("Total number of trajectory points:" + count);
        return trajectoryList.stream();
    }

    public List<Trajectory> readTrajectoryFilesList(String csvTrajectoryPath) throws IOException {
        File inputFile = new File(csvTrajectoryPath);
        List<Trajectory> trajectoryList = new ArrayList<>();
        if (inputFile.isDirectory()) {
            File[] trajectoryFiles = inputFile.listFiles();
            if (trajectoryFiles != null) {
                for (File trajectoryFile : trajectoryFiles) {
                    Trajectory newTrajectory = readTrajectory(trajectoryFile);
                    newTrajectory.setId(trajectoryFile.getName().substring(trajectoryFile.getName().indexOf('_') + 1, trajectoryFile.getName().indexOf('.')));
                    trajectoryList.add(newTrajectory);
                }
            }
        } else {
            trajectoryList.add(readTrajectory(inputFile));
        }
        int count = 0;
        for (Trajectory t : trajectoryList) {
            count += t.getCoordinates().size();
        }
        System.out.println("Total number of trajectory points:" + count);
        return trajectoryList;
    }

    public List<RoadWay> readMatchedTrajectoryFilesList(String csvMatchedTrajectoryPath) throws IOException {
        File inputFile = new File(csvMatchedTrajectoryPath);
        List<RoadWay> trajectoryList = new ArrayList<>();
        if (inputFile.isDirectory()) {
            File[] matchedTrajectoryFiles = inputFile.listFiles();
            if (matchedTrajectoryFiles != null) {
                for (File f : matchedTrajectoryFiles) {
                    RoadWay newTrajectory = readMatchedTrajectory(f);
                    newTrajectory.setId(f.getName().substring(f.getName().indexOf('_') + 1, f.getName().indexOf('.')));
                    trajectoryList.add(newTrajectory);
                }
            }
        } else {
            trajectoryList.add(readMatchedTrajectory(inputFile));
        }
        int count = 0;
        for (RoadWay t : trajectoryList) {
            count += t.getNodes().size();
        }
        System.out.println("Total number of matched trajectory points:" + count);
        return trajectoryList;
    }
}
