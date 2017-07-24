package edu.uq.dke.mapupdate.mapmatching.algorithm;

import edu.uq.dke.mapupdate.io.CSVMapReader;
import edu.uq.dke.mapupdate.io.CSVTrajectoryReader;
import edu.uq.dke.mapupdate.io.CSVTrajectoryWriter;
import edu.uq.dke.mapupdate.visualisation.GraphStreamDisplay;
import org.graphstream.ui.view.View;
import org.graphstream.ui.view.Viewer;
import org.jdom2.JDOMException;
import traminer.util.Pair;
import traminer.util.map.roadnetwork.RoadNetworkGraph;
import traminer.util.map.roadnetwork.RoadNode;
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
    public static List<Trajectory> YouzeFastMatching(String cityName, String inputTrajPath, String groundTruthMapPath, String outputTrajPath, String allPairSPFilePath, boolean isUpdate) throws JDOMException, IOException {

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
        int maxDegree = 0;
        int nodeRemoveCount = 0;

        // eliminate the road nodes that have no edges
        List<RoadNode> removedRoadNodeList = new ArrayList<>();
        for (RoadNode n : roadNetworkGraph.getNodes()) {
            if (n.getDegree() == 0) {
                removedRoadNodeList.add(n);
                nodeRemoveCount++;
            }
            if (maxDegree < n.getDegree())
                maxDegree = n.getDegree();
        }
        for (RoadNode n : removedRoadNodeList) {
            roadNetworkGraph.getNodes().remove(n);
        }

        System.out.println("nodeRemoveCount = " + nodeRemoveCount);
        System.out.println("maxDegree = " + maxDegree);

//        AllPairsShortestPathFile shortestPathFile = new AllPairsShortestPathFile(roadNetworkGraph);
//        System.out.println("Shortest Path generation done!");


        // read input trajectories
        CSVTrajectoryReader csvTrajectoryReader = new CSVTrajectoryReader();
        List<Trajectory> trajectoryList = csvTrajectoryReader.readTrajectoryFilesList(inputTrajPath);
        List<RoadWay> matchedResultList = new ArrayList<>();
        List<Trajectory> unmatchedTrajList = new ArrayList<>();

//        // knn test
//        trajectoryList = csvTrajectoryReader.readTrajectoryFilesList(inputTrajPath);
//        GPSDistanceFunction dist = new GPSDistanceFunction();
//        PointBasedFastMatching testMatching = new PointBasedFastMatching(roadNetworkGraph, dist, 4, inputShortestPathFile,isUpdate);
//        testMatching.knnSearchTest(trajectoryList.get(7), roadNetworkGraph);

        // start the matching process sequentially
        GPSDistanceFunction distFunc = new GPSDistanceFunction();
        PointBasedFastMatching matching = new PointBasedFastMatching(roadNetworkGraph, distFunc, 128, allPairSPFilePath, isUpdate);

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
        Trajectory t = trajectoryList.get(13);
//        for (Trajectory t : trajectoryList) {
        Pair<RoadWay, List<Trajectory>> trajMatchResult = matching.doMatching(t);
        matchedResultList.add(trajMatchResult._1());
        unmatchedTrajList.addAll(trajMatchResult._2());
        if (matchedResultList.size() % (trajectoryList.size() / 10) == 0) {
            System.out.println(matchedResultList.size() / (trajectoryList.size() / 10) + "/10 trajectories matched");
        }
//        }

        // graph stream display
        List<Trajectory> originalTrajList = new ArrayList<>();
        originalTrajList.add(t);
        GraphStreamDisplay display = new GraphStreamDisplay();
        display.setGroundTruthGraph(roadNetworkGraph);
        display.setRawTrajectories(originalTrajList);
        display.setMatchedTrajectories(matchedResultList);
        display.setCentralPoint(originalTrajList.get(0).getPoints().get(0));
        Viewer viewer = display.generateGraph().display(false);
        if (display.getCentralPoint() != null) {
            View view = viewer.getDefaultView();
            view.getCamera().setViewCenter(display.getCentralPoint().x(), display.getCentralPoint().y(), 0);
            view.getCamera().setViewPercent(1);
        }
        CSVTrajectoryWriter.matchedTrajectoryWriter(matchedResultList, outputTrajPath);
        return unmatchedTrajList;
    }
}
