package edu.uq.dke.mapupdate.util.io;

import edu.uq.dke.mapupdate.util.object.datastructure.Pair;
import edu.uq.dke.mapupdate.util.object.datastructure.PointMatch;
import edu.uq.dke.mapupdate.util.object.datastructure.TrajectoryMatchResult;
import edu.uq.dke.mapupdate.util.object.spatialobject.Point;
import edu.uq.dke.mapupdate.util.object.spatialobject.STPoint;
import edu.uq.dke.mapupdate.util.object.spatialobject.Segment;
import edu.uq.dke.mapupdate.util.object.spatialobject.Trajectory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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

    public List<TrajectoryMatchResult> readMatchedResult(String trajectoryFilePath, int rankLength) throws IOException {
        File matchingPointFileFolder = new File(trajectoryFilePath + "matchedResult/");
        File roadIDFileFolder = new File(trajectoryFilePath + "matchedRoadID/");
        List<TrajectoryMatchResult> gtResult = new ArrayList<>();
        if (matchingPointFileFolder.isDirectory() && roadIDFileFolder.isDirectory()) {
            File[] matchingPointFileList = matchingPointFileFolder.listFiles();
            File[] roadIDFileList = roadIDFileFolder.listFiles();
            if (matchingPointFileList != null && roadIDFileList != null) {
                for (int i = 0; i < roadIDFileList.length; i++) {
                    File matchingPointFile = matchingPointFileList[i];
                    File roadIDFile = roadIDFileList[i];
                    Trajectory rawTraj = new Trajectory();
                    List<List<PointMatch>> matchingPointSet = new ArrayList<>(rankLength);
                    List<List<String>> roadWayIDList = new ArrayList<>(rankLength);
                    for (int j = 0; j < rankLength; j++) {
                        matchingPointSet.add(new ArrayList<>());
                        roadWayIDList.add(new ArrayList<>());
                    }
                    double[] probabilities = new double[rankLength];
                    BufferedReader brMatchingTrajectory = new BufferedReader(new FileReader(matchingPointFile));
                    BufferedReader brRoadIDTrajectory = new BufferedReader(new FileReader(roadIDFile));
                    String line;
                    while ((line = brMatchingTrajectory.readLine()) != null) {
                        String[] matchInfo = line.split("\\|");
                        if (matchInfo.length == rankLength + 1) {
                            String[] pointInfo = matchInfo[0].split(" ");
                            STPoint currPoint = new STPoint(Double.parseDouble(pointInfo[0]), Double.parseDouble(pointInfo[1]), Long
                                    .parseLong(pointInfo[2]));
                            rawTraj.add(currPoint);
                            for (int j = 0; j < rankLength; j++) {
                                if (matchInfo[j + 1].equals("null")) {
                                    matchingPointSet.get(j).add(new PointMatch());
                                } else {
                                    String[] matchPointInfo = matchInfo[j + 1].split(",");
                                    if (matchPointInfo.length == 7) {
                                        Point matchPoint = new Point(Double.parseDouble(matchPointInfo[0]), Double.parseDouble(matchPointInfo[1]));
                                        Segment matchSegment = new Segment(Double.parseDouble(matchPointInfo[2]), Double.parseDouble
                                                (matchPointInfo[3]), Double.parseDouble(matchPointInfo[4]), Double.parseDouble
                                                (matchPointInfo[5]));
                                        PointMatch currMatchPoint = new PointMatch(matchPoint, matchSegment, matchPointInfo[6]);
                                        matchingPointSet.get(j).add(currMatchPoint);
                                    } else System.out.println("ERROR! Incorrect match result length.");
                                }
                            }
                        } else System.out.println("ERROR! Inconsistent rank length during trajectory reading.");
                    }
                    int rowCount = 0;
                    while ((line = brRoadIDTrajectory.readLine()) != null && rowCount < rankLength) {
                        String[] roadWayInfo = line.split("\\|");
                        if (roadWayInfo.length == 2) {
                            String[] matchWayResult = roadWayInfo[0].split(",");
                            roadWayIDList.set(rowCount, Arrays.asList(matchWayResult));
                            probabilities[rowCount] = Double.parseDouble(roadWayInfo[1]);
                            rowCount++;
                        } else System.out.println("ERROR! Wrong length of the road way info.");
                    }
                    brMatchingTrajectory.close();
                    brRoadIDTrajectory.close();
                    int fileNum = Integer.parseInt(matchingPointFile.getName().substring(matchingPointFile.getName().indexOf('_') + 1, matchingPointFile.getName().indexOf('.')));
                    rawTraj.setId(fileNum + "");
                    TrajectoryMatchResult currMatchResult = new TrajectoryMatchResult(rawTraj, rankLength);
                    for (int j = 0; j < rankLength; j++) {
                        currMatchResult.setMatchingResult(matchingPointSet.get(j), j);
                    }
                    currMatchResult.setMatchWayLists(roadWayIDList);
                    currMatchResult.setProbabilities(probabilities);
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

    /**
     * Read and parse the input CSV trajectory files to a Stream
     * of trajectories.
     *
     * @param csvTrajectoryPath the trajectory input path
     */
    public Stream<Trajectory> readTrajectoryFilesStream(String csvTrajectoryPath) {
        // read input data
        Stream<File> dataFiles =
                IOService.getFiles(csvTrajectoryPath);
        return dataFiles.parallel().map(
                file -> {
                    try {
                        Trajectory newTrajectory = readTrajectory(file);
                        newTrajectory.setId(file.getName().substring(file.getName().indexOf('_') + 1, file
                                .getName().indexOf('.')));
                        return newTrajectory;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return null;
                });
    }

}
