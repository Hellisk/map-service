package edu.uq.dke.mapupdate.io;

import edu.uq.dke.mapupdate.datatype.MatchingPoint;
import edu.uq.dke.mapupdate.datatype.MatchingResult;
import traminer.util.Pair;
import traminer.util.map.matching.PointNodePair;
import traminer.util.spatial.objects.Point;
import traminer.util.spatial.objects.st.STPoint;
import traminer.util.trajectory.Trajectory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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

    public List<MatchingResult> readMatchingResult(String trajectoryFilePath) throws IOException {
        File f = new File(trajectoryFilePath + "matchedResult/");
        List<MatchingResult> gtResult = new ArrayList<>();
        if (f.isDirectory()) {
            File[] fileList = f.listFiles();
            if (fileList != null) {
                for (File file : fileList) {
                    List<PointNodePair> matchingPointSet = new ArrayList<>();
                    BufferedReader brTrajectory = new BufferedReader(new FileReader(file));
                    String line;
                    while ((line = brTrajectory.readLine()) != null) {
                        String[] pointInfo = line.split(" ");
                        if (pointInfo.length == 4) {
                            Point currPoint = new Point(Double.parseDouble(pointInfo[0]), Double.parseDouble(pointInfo[1]));
                            MatchingPoint currMatchPoint = new MatchingPoint(currPoint, null, pointInfo[3]);
                            PointNodePair result = new PointNodePair(null, currMatchPoint);
                            matchingPointSet.add(result);
                        }
                    }
                    brTrajectory.close();
                    int fileNum = Integer.parseInt(file.getName().substring(file.getName().indexOf('_') + 1, file.getName().indexOf('.')));
                    MatchingResult currMatchResult = new MatchingResult(fileNum + "");
                    currMatchResult.setMatchingResult(matchingPointSet);
                    gtResult.add(currMatchResult);
                }
            }
        }
        return gtResult;
    }

    public List<Pair<Integer, List<String>>> readGroundTruthMatchingResult(String matchingResultPath) throws IOException {
        File f = new File(matchingResultPath);
        List<Pair<Integer, List<String>>> gtResult = new ArrayList<>();
        if (f.isDirectory()) {
            File[] fileList = f.listFiles();
            if (fileList != null) {
                for (File file : fileList) {
                    BufferedReader brTrajectory = new BufferedReader(new FileReader(file));
                    List<String> matchResult = new ArrayList<>();
                    String line;
                    while ((line = brTrajectory.readLine()) != null) {
                        matchResult.add(line);
                    }
                    brTrajectory.close();
                    int fileNum = Integer.parseInt(file.getName().substring(file.getName().indexOf('_') + 1, file.getName().indexOf('.')));
                    gtResult.add(new Pair<>(fileNum, matchResult));
                }
            }
        }
        return gtResult;
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
        System.out.println("Trajectory read finished, total number of trajectories:" + trajectoryList.size() + ", trajectory points:" + count);
        return trajectoryList;
    }
}
