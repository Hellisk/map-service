package mapupdate.util.io;

import mapupdate.util.index.rtree.STRTree;
import mapupdate.util.object.datastructure.*;
import mapupdate.util.object.spatialobject.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;

import static mapupdate.Main.*;

/**
 * Created by uqpchao on 23/05/2017.
 */
public class CSVTrajectoryReader {
    private int indexType;
    private List<XYObject<Point>> indexPointList = null;

    public CSVTrajectoryReader(int indexType) {
        this.indexType = indexType;
    }

    public Trajectory readTrajectory(File trajectoryFile, String trajID) throws IOException {
        BufferedReader brTrajectory = new BufferedReader(new FileReader(trajectoryFile));
        Trajectory newTrajectory = new Trajectory();
        String line;
        while ((line = brTrajectory.readLine()) != null) {
            String[] pointInfo = line.split(" ");
            if (pointInfo.length != 5)
                continue;   // the anchor road ID when reading unmatched trajectories
            TrajectoryPoint newTrajectoryPoint = new TrajectoryPoint(Double.parseDouble(pointInfo[0]), Double.parseDouble(pointInfo[1]),
                    Long.parseLong(pointInfo[2]), Double.parseDouble(pointInfo[3]), Double.parseDouble(pointInfo[4]));
            newTrajectory.add(newTrajectoryPoint);
            if (indexType != 0 && indexPointList != null) {
                Point currPoint = new Point(Double.parseDouble(pointInfo[0]), Double.parseDouble(pointInfo[1]));
                currPoint.setID(trajID);
                XYObject<Point> indexItem = new XYObject<>(Double.parseDouble(pointInfo[0]), Double.parseDouble(pointInfo[1]), currPoint);
                indexPointList.add(indexItem);
            }
        }
        brTrajectory.close();
        return newTrajectory;
    }

    public List<TrajectoryMatchingResult> readMatchedResult(String trajectoryFilePath, int iteration) throws IOException {
        File matchingPointFileFolder;
        File roadIDFileFolder;
        if (iteration == -1) {
            matchingPointFileFolder = new File(trajectoryFilePath + "matchedResult/TP" + MIN_TRAJ_TIME_SPAN + "_TI" +
                    MAX_TIME_INTERVAL + "_TC" + TRAJECTORY_COUNT + "/");
            roadIDFileFolder = new File(trajectoryFilePath + "matchedRoadID/TP" + MIN_TRAJ_TIME_SPAN + "_TI" +
                    MAX_TIME_INTERVAL + "_TC" + TRAJECTORY_COUNT + "/");
        } else {
            matchingPointFileFolder = new File(trajectoryFilePath + "matchedResult/TP" + MIN_TRAJ_TIME_SPAN + "_TI" +
                    MAX_TIME_INTERVAL + "_TC" + TRAJECTORY_COUNT + "/" + iteration + "/");
            roadIDFileFolder = new File(trajectoryFilePath + "matchedRoadID/TP" + MIN_TRAJ_TIME_SPAN + "_TI" +
                    MAX_TIME_INTERVAL + "_TC" + TRAJECTORY_COUNT + "/" + iteration + "/");
        }
        List<TrajectoryMatchingResult> gtResult = new ArrayList<>();
        if (matchingPointFileFolder.isDirectory() && roadIDFileFolder.isDirectory()) {
            File[] matchingPointFileList = matchingPointFileFolder.listFiles();
            File[] roadIDFileList = roadIDFileFolder.listFiles();
            if (matchingPointFileList != null && roadIDFileList != null) {
                for (int i = 0; i < roadIDFileList.length; i++) {
                    File matchingPointFile = matchingPointFileList[i];
                    File roadIDFile = roadIDFileList[i];
                    Trajectory rawTraj = new Trajectory();
                    List<List<PointMatch>> matchingPointSet = new ArrayList<>(RANK_LENGTH);
                    List<List<String>> roadWayIDList = new ArrayList<>(RANK_LENGTH);
                    for (int j = 0; j < RANK_LENGTH; j++) {
                        matchingPointSet.add(new ArrayList<>());
                        roadWayIDList.add(new ArrayList<>());
                    }
                    double[] probabilities = new double[RANK_LENGTH];
                    BufferedReader brMatchingTrajectory = new BufferedReader(new FileReader(matchingPointFile));
                    BufferedReader brRoadIDTrajectory = new BufferedReader(new FileReader(roadIDFile));
                    String line;
                    while ((line = brMatchingTrajectory.readLine()) != null) {
                        String[] matchInfo = line.split("\\|");
                        if (matchInfo.length == RANK_LENGTH + 1) {
                            String[] pointInfo = matchInfo[0].split(" ");
                            TrajectoryPoint currPoint = new TrajectoryPoint(Double.parseDouble(pointInfo[0]), Double.parseDouble(pointInfo[1]), Long
                                    .parseLong(pointInfo[2]), Double.parseDouble(pointInfo[3]), Double.parseDouble(pointInfo[4]));
                            rawTraj.add(currPoint);
                            for (int j = 0; j < RANK_LENGTH; j++) {
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
                                    } else LOGGER.severe("ERROR! Incorrect match result length." + roadIDFileList[i].getName());
                                }
                            }
                        } else LOGGER.severe("ERROR! Inconsistent rank length during trajectory reading: " + matchInfo.length);
                    }
                    int rowCount = 0;
                    while ((line = brRoadIDTrajectory.readLine()) != null && rowCount < RANK_LENGTH) {
                        String[] roadWayInfo = line.split("\\|");
                        if (roadWayInfo.length == 2) {
                            String[] matchWayResult = roadWayInfo[0].split(",");
                            List<String> matchWayList = new ArrayList<>();
                            for (String aMatchWayResult : matchWayResult) {
                                if (!aMatchWayResult.equals("null"))
                                    matchWayList.add(aMatchWayResult);
                            }
                            roadWayIDList.set(rowCount, matchWayList);
                            probabilities[rowCount] = Double.parseDouble(roadWayInfo[1]);
                            rowCount++;
                        } else LOGGER.severe("ERROR! Wrong length of the road way info.");
                    }
                    brMatchingTrajectory.close();
                    brRoadIDTrajectory.close();
                    int fileNum = Integer.parseInt(matchingPointFile.getName().substring(matchingPointFile.getName().indexOf('_') + 1, matchingPointFile.getName().indexOf('.')));
                    rawTraj.setID(fileNum + "");
                    TrajectoryMatchingResult currMatchResult = new TrajectoryMatchingResult(rawTraj, RANK_LENGTH);
                    for (int j = 0; j < RANK_LENGTH; j++) {
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
        if (!inputFile.exists())
            LOGGER.severe("ERROR! The input trajectory path doesn't exist: " + csvTrajectoryPath);
        if (inputFile.isDirectory()) {
            File[] trajectoryFiles = inputFile.listFiles();
            if (trajectoryFiles != null) {
                if (indexType != 0) {
                    indexPointList = new ArrayList<>();
                }
                for (File trajectoryFile : trajectoryFiles) {
                    String trajID = trajectoryFile.getName().substring(trajectoryFile.getName().indexOf('_') + 1,
                            trajectoryFile.getName().indexOf('.'));
                    Trajectory newTrajectory = readTrajectory(trajectoryFile, trajID);
                    newTrajectory.setID(trajID);
                    trajectoryList.add(newTrajectory);
                }
            } else LOGGER.severe("ERROR! The input trajectory dictionary is empty: " + csvTrajectoryPath);
        } else {
            trajectoryList.add(readTrajectory(inputFile, 0 + ""));
        }
        int count = 0;
        for (Trajectory t : trajectoryList) {
            count += t.getCoordinates().size();
        }

        LOGGER.info("Trajectory reading finished, total number of trajectories:" + trajectoryList.size() + ", trajectory points:" + count);
        return trajectoryList;
    }

    public List<Triplet<Trajectory, String, String>> readUnmatchedTrajectoryFilesList(String csvUnmatchedTrajectoryPath) throws IOException {
        File inputFile = new File(csvUnmatchedTrajectoryPath);
        List<Triplet<Trajectory, String, String>> trajectoryList = new ArrayList<>();
        if (!inputFile.exists())
            LOGGER.severe("ERROR! The input trajectory path doesn't exist: " + csvUnmatchedTrajectoryPath);
        if (inputFile.isDirectory()) {
            File[] trajectoryFiles = inputFile.listFiles();
            if (trajectoryFiles != null) {
                for (File trajectoryFile : trajectoryFiles) {
                    BufferedReader brTrajectory = new BufferedReader(new FileReader(trajectoryFile));
                    String[] line = brTrajectory.readLine().split(",");
                    if (line.length != 2)
                        throw new IllegalStateException("ERROR! The unmatched trajectory format is incorrect:" + Arrays.toString(line));
                    brTrajectory.close();
                    String unmatchedTrajID = trajectoryFile.getName().substring(trajectoryFile.getName().indexOf('_') + 1,
                            trajectoryFile.getName().indexOf('.'));
                    Trajectory newTrajectory = readTrajectory(trajectoryFile, unmatchedTrajID);
                    newTrajectory.setID(unmatchedTrajID);
                    trajectoryList.add(new Triplet<>(newTrajectory, line[0], line[1]));
                }
            } else LOGGER.severe("ERROR! The input trajectory dictionary is empty: " + csvUnmatchedTrajectoryPath);
        } else {
            BufferedReader brTrajectory = new BufferedReader(new FileReader(inputFile));
            String[] line = brTrajectory.readLine().split(",");
            trajectoryList.add(new Triplet<>(readTrajectory(inputFile, 0 + ""), line[0], line[1]));
        }
        int count = 0;
        for (Triplet<Trajectory, String, String> unmatchedTrajInfo : trajectoryList) {
            count += unmatchedTrajInfo._1().size();
        }
        LOGGER.info("Trajectory read finished, total number of trajectories:" + trajectoryList.size() + ", trajectory points:" + count);
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
        File inputFile = new File(csvTrajectoryPath);
        if (!inputFile.exists())
            LOGGER.severe("ERROR! The input trajectory path doesn't exist: " + csvTrajectoryPath);
        Stream<File> dataFiles = IOService.getFiles(csvTrajectoryPath);
        if (indexType != 0)
            indexPointList = Collections.synchronizedList(new ArrayList<>());
        return dataFiles.parallel().map(
                file -> {
                    try {
                        String trajID = file.getName().substring(file.getName().indexOf('_') + 1, file.getName().indexOf('.'));
                        Trajectory newTrajectory = readTrajectory(file, trajID);
                        newTrajectory.setID(trajID);
                        return newTrajectory;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return null;
                });
    }

    /**
     * Read and parse the input CSV trajectory files to a Stream of trajectories given a list of trajectory.
     *
     * @param csvTrajectoryPath the trajectory input path
     * @param trajIDSet         The set of trajectory ID to be read
     */
    public Stream<Trajectory> readPartialTrajectoryFilesStream(String csvTrajectoryPath, Set<String> trajIDSet) {
        // read input data
        File inputFile = new File(csvTrajectoryPath);
        if (!inputFile.exists())
            LOGGER.severe("ERROR! The input trajectory path doesn't exist: " + csvTrajectoryPath);
        Stream<File> dataFiles = IOService.getFilesWithIDs(csvTrajectoryPath, trajIDSet);
        if (indexType == 0)
            LOGGER.severe("ERROR! Partial trajectory read should not be called in non-index mode.");
        return dataFiles.parallel().map(
                file -> {
                    try {
                        String trajID = file.getName().substring(file.getName().indexOf('_') + 1, file.getName().indexOf('.'));
                        Trajectory newTrajectory = readTrajectory(file, trajID);
                        newTrajectory.setID(trajID);
                        return newTrajectory;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return null;
                });
    }

    public Pair<STRTree<Point>, Integer> createIndex() {
        if (indexType != 0 && indexPointList.size() != 0) {
            STRTree<Point> index = new STRTree<>(indexPointList, indexPointList.size() / 10000);
            return new Pair<>(index, indexPointList.size());
        } else
            throw new IllegalArgumentException("ERROR! Index should not be built for non-index-based matching.");
    }
}
