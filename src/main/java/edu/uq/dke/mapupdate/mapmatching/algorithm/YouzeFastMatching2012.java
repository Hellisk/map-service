package edu.uq.dke.mapupdate.mapmatching.algorithm;

import edu.uq.dke.mapupdate.io.CSVMapReader;
import edu.uq.dke.mapupdate.io.CSVTrajectoryReader;
import edu.uq.dke.mapupdate.visualisation.GraphStreamDisplay;
import org.graphstream.ui.view.View;
import org.graphstream.ui.view.Viewer;
import org.jdom2.JDOMException;
import traminer.util.map.roadnetwork.RoadNetworkGraph;
import traminer.util.map.roadnetwork.RoadWay;
import traminer.util.trajectory.Trajectory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by uqpchao on 23/05/2017.
 */
public class YouzeFastMatching2012 {
    public static void YouzeFastMatching(String cityName, String inputTrajectoryPath, String inputMapPath, String outputTrajectoryPath, String inputShortestPathFile, String groundTruthMatchingPath, boolean isUpdate) throws JDOMException, IOException {

        // read ground truth map
        String inputVertexPath = inputMapPath + cityName + "_vertices.txt";
        String inputEdgePath = inputMapPath + cityName + "_edges.txt";
        CSVMapReader csvMapReader = new CSVMapReader(inputVertexPath, inputEdgePath);
        RoadNetworkGraph roadNetworkGraph;
        if (cityName.equals("beijing")) {
            roadNetworkGraph = csvMapReader.readShapeCSV();
        } else {
            roadNetworkGraph = csvMapReader.readCSV();
        }

        // unfolding map display, currently failed
//        UnfoldingMapDemo displayUnfolding = new UnfoldingMapDemo();
//        displayUnfolding.display(cityName,inputMapPath,"map");

//        AllPairsShortestPathFile shortestPathFile = new AllPairsShortestPathFile(roadNetworkGraph);
//        System.out.println("Shortest Path generation done!");


        // read input trajectories
        CSVTrajectoryReader csvTrajectoryReader = new CSVTrajectoryReader();
        List<Trajectory> trajectoryList;
        List<Trajectory> originalTrajList = new ArrayList<>();
        trajectoryList = csvTrajectoryReader.readTrajectoryFilesList(inputTrajectoryPath);

        // read evaluation trajectories
        List<RoadWay> matchedTrajectoryList;
        matchedTrajectoryList = csvTrajectoryReader.readMatchedTrajectoryFilesList(groundTruthMatchingPath);
        List<RoadWay> resultList = new ArrayList<>();

//        // test whether the roadways can be found in the map
//        Map<Integer, Boolean> segmentLookup = new HashMap<>();
//        for(RoadNode n : roadNetworkGraph.getNodes()){
//            String test = n.getId().substring(1);
//            segmentLookup.put(Integer.parseInt(n.getId().substring(1)),true);
//        }
//        for (RoadWay r : roadNetworkGraph.getWays()) {
//            String shit = r.getId().substring(1);
//            segmentLookup.put(Integer.parseInt(r.getId().substring(1)), true);
//            for (RoadNode n : r.getNodes()) {
//                String test = n.getId().substring(n.getId().indexOf('N') + 1);
//                segmentLookup.put(Integer.parseInt(n.getId().substring(n.getId().indexOf('N') + 1)), false);
//            }
//        }
//        int roadMatchCount = 0;
//        int subpointMatchCount = 0;
//        int unmatchCount = 0;
//
//        for (RoadWay r : matchedTrajectoryList) {
//            for (RoadNode n : r.getNodes()) {
//                int id = Math.abs(Integer.parseInt(n.getId()));
//                if (segmentLookup.containsKey(id)) {
//                    if (segmentLookup.get(id))
//                        roadMatchCount++;
//                    else subpointMatchCount++;
//                } else unmatchCount++;
//            }
//        }
//        System.out.println("Road match count:"+ roadMatchCount + ", subroad match:" + subpointMatchCount+", unmatched count:"+unmatchCount);

        // knn test
//        trajectoryList = csvTrajectoryReader.readTrajectoryFilesList(inputTrajectoryPath);
//        GPSDistanceFunction dist = new GPSDistanceFunction();
//        PointBasedFastMatching testMatching = new PointBasedFastMatching(roadNetworkGraph, dist, 4, inputShortestPathFile);
//        testMatching.knnSearchTest(trajectoryList.get(8), roadNetworkGraph);

//        Stream<Trajectory> trajectoryStream = csvTrajectoryReader.readTrajectoryFiles(inputTrajectoryPath);
        Trajectory originTraj = trajectoryList.get(13);
        RoadWay matchedTraj = matchedTrajectoryList.get(13);
        // start the matching process sequentially
//        GPSDistanceFunction distfunc = new GPSDistanceFunction();
//        PointBasedFastMatching matching = new PointBasedFastMatching(roadNetworkGraph, distfunc, 64, inputShortestPathFile, isUpdate);

//        Iterator<Trajectory> traj = trajectoryStream.iterator();
//        while (traj.hasNext()) {
//        RoadWay matchedTrajectory = new RoadWay();
//        Trajectory originTraj = traj.next();
//        originTraj = traj.next();
//        originTraj = traj.next();
//        originTraj = traj.next();
//
//        matchedTraj = matching.doMatching(originTraj, roadNetworkGraph);
//        displayTrajMatch(originTraj, matchedTrajectory);
        originalTrajList.add(originTraj);
        resultList.add(matchedTraj);
//        }

        // graph stream display
        GraphStreamDisplay display = new GraphStreamDisplay();
        display.setGroundTruthGraph(roadNetworkGraph);
        display.setRawTrajectories(originalTrajList);
        display.setMatchedTrajectories(resultList);
        display.setCentralPoint(originalTrajList.get(0).getPoints().get(0));
        Viewer viewer = display.generateGraph().display(false);
        if (display.getCentralPoint() != null) {
            View view = viewer.getDefaultView();
            view.getCamera().setViewCenter(display.getCentralPoint().x(), display.getCentralPoint().y(), 0);
            view.getCamera().setViewPercent(0.05);
        }
//        CSVTrajectoryWriter.matchedTrajectoryWriter(result, outputTrajectoryPath);
    }

}
