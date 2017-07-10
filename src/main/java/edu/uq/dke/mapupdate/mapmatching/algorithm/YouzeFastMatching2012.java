package edu.uq.dke.mapupdate.mapmatching.algorithm;

import edu.uq.dke.mapupdate.io.CSVMapReader;
import edu.uq.dke.mapupdate.io.CSVTrajectoryReader;
import edu.uq.dke.mapupdate.io.CSVTrajectoryWriter;
import org.jdom2.JDOMException;
import traminer.util.Pair;
import traminer.util.map.roadnetwork.RoadNetworkGraph;
import traminer.util.map.roadnetwork.RoadWay;
import traminer.util.spatial.distance.GPSDistanceFunction;
import traminer.util.trajectory.Trajectory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by uqpchao on 23/05/2017.
 */
public class YouzeFastMatching2012 {
    public static List<Trajectory> YouzeFastMatching(String cityName, String inputTrajPath, String groundTruthMapPath, String outputTrajPath, boolean isUpdate) throws JDOMException, IOException {

        // read ground truth map
        String inputVertexPath = groundTruthMapPath + cityName + "_vertices.txt";
        String inputEdgePath = groundTruthMapPath + cityName + "_edges.txt";
        CSVMapReader csvMapReader = new CSVMapReader(inputVertexPath, inputEdgePath);
        RoadNetworkGraph roadNetworkGraph;
        if (cityName.equals("beijing")) {
            roadNetworkGraph = csvMapReader.readShapeCSV();
        } else {
            roadNetworkGraph = csvMapReader.readCSV();
        }

//        AllPairsShortestPathFile shortestPathFile = new AllPairsShortestPathFile(roadNetworkGraph);
//        System.out.println("Shortest Path generation done!");


        // read input trajectories
        CSVTrajectoryReader csvTrajectoryReader = new CSVTrajectoryReader();
        List<Trajectory> trajectoryList = csvTrajectoryReader.readTrajectoryFilesList(inputTrajPath);
        List<Trajectory> originalTrajList = new ArrayList<>();
        List<RoadWay> matchedResultList = new ArrayList<>();
        List<Trajectory> unmatchedTrajList = new ArrayList<>();

//        // read evaluation trajectories
//        List<RoadWay> matchedTrajectoryList;
//        matchedTrajectoryList = csvTrajectoryReader. readMatchedTrajectoryFilesList(groundTruthMatchingPath);
//
//        // test whether the roadways can be found in the map
//        Map<String, Boolean> segmentLookup = new HashMap<>();
//        for (RoadWay r : roadNetworkGraph.getWays()) {
//            segmentLookup.put(r.getId(), false);
//        }
//        int roadMatchCount = 0;
//        int unmatchedCount = 0;
//
//        List<String> unmatchedRoadwayList = new ArrayList<>();
//
//        for (RoadWay r : matchedTrajectoryList) {
//            for (RoadNode n : r.getNodes()) {
//                String id = n.getId();
//                if (segmentLookup.containsKey(id)) {
//                    roadMatchCount++;
//                } else {
//                    unmatchedCount++;
//                    if (!unmatchedRoadwayList.contains(r.getId())) {
//                        unmatchedRoadwayList.add(r.getId());
//                    }
//                }
//            }
//            matchedResultList.add(r);
//        }
//
//        System.out.println("unmatchedRoadwayList.size() = " + unmatchedRoadwayList.size());
//        System.out.println("Road match:" + roadMatchCount + ", unmatched count:" + unmatchedCount);

//        // remove the trajectories that matched to outer roads
//        File trajFolder = new File(inputTrajPath);
//        File[] fileList = trajFolder.listFiles();
//        int newID = 0;
//        if (fileList != null) {
//            for (File f : fileList) {
//                String fileID = f.getName().substring(f.getName().indexOf('_') + 1, f.getName().indexOf('.'));
//                File matchedTraj = new File(groundTruthMatchingPath + "realtrip_" + fileID + ".txt");
//                if (unmatchedRoadwayList.contains(fileID)) {
//                    f.delete();
//                    matchedTraj.delete();
//                    newID++;
//                }
//            }
//        }
//        System.out.println("newID = " + newID);

//        // knn test
//        trajectoryList = csvTrajectoryReader.readTrajectoryFilesList(inputTrajPath);
//        GPSDistanceFunction dist = new GPSDistanceFunction();
//        PointBasedFastMatching testMatching = new PointBasedFastMatching(roadNetworkGraph, dist, 4, inputShortestPathFile,isUpdate);
//        testMatching.knnSearchTest(trajectoryList.get(7), roadNetworkGraph);

        // start the matching process sequentially
        GPSDistanceFunction distFunc = new GPSDistanceFunction();
        PointBasedFastMatching matching = new PointBasedFastMatching(roadNetworkGraph, distFunc, 64, isUpdate);

//        // stream read trajectories
//        Stream<Trajectory> trajectoryStream = csvTrajectoryReader.readTrajectoryFiles(inputTrajPath);
//        Iterator<Trajectory> traj = trajectoryStream.iterator();
//        while (traj.hasNext()) {
//            RoadWay matchedTraj = new RoadWay();
//            Trajectory originTraj = traj.next();
//            originTraj = traj.next();
//            originTraj = traj.next();
//            originTraj = traj.next();
//
//            matchedTraj = matching.doMatching(originTraj, roadNetworkGraph);
//            originalTrajList.add(originTraj);
//            matchedResultList.add(matchedTraj);
//        }

        // list read trajectories
//        Trajectory t = trajectoryList.get(26);
        for (Trajectory t : trajectoryList) {
            originalTrajList.add(t);
            Pair<RoadWay, List<Trajectory>> trajMatchResult = matching.doMatching(t);
            matchedResultList.add(trajMatchResult._1());
            unmatchedTrajList.addAll(trajMatchResult._2());
            if (matchedResultList.size() % (trajectoryList.size() / 10) == 0) {
                System.out.println(matchedResultList.size() / (trajectoryList.size() / 10) + "/10 trajectories matched");
            }
        }

//        // graph stream display
//        GraphStreamDisplay display = new GraphStreamDisplay();
//        display.setGroundTruthGraph(roadNetworkGraph);
//        display.setRawTrajectories(originalTrajList);
//        display.setMatchedTrajectories(matchedResultList);
//        display.setCentralPoint(originalTrajList.get(0).getPoints().get(0));
//        Viewer viewer = display.generateGraph().display(false);
//        if (display.getCentralPoint() != null) {
//            View view = viewer.getDefaultView();
//            view.getCamera().setViewCenter(display.getCentralPoint().x(), display.getCentralPoint().y(), 0);
//            view.getCamera().setViewPercent(1);
//        }
        CSVTrajectoryWriter.matchedTrajectoryWriter(matchedResultList, outputTrajPath);
        return unmatchedTrajList;
    }
}
