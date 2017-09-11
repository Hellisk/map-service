package edu.uq.dke.mapupdate.mapmatching.algorithm;

import edu.uq.dke.mapupdate.io.AllPairsShortestPathFile;
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
    public static List<Trajectory> YouzeFastMatching(List<Trajectory> trajectoryList, RoadNetworkGraph originalMap, String outputTrajPath, AllPairsShortestPathFile allPairSP, boolean isUpdate) throws JDOMException, IOException {
        // read input trajectories
        List<RoadWay> matchedResultList = new ArrayList<>();
        List<Trajectory> unmatchedTrajList = new ArrayList<>();

//        // knn test
//        trajectoryList = csvTrajectoryReader.readTrajectoryFilesList(inputTrajPath);
//        GPSDistanceFunction dist = new GPSDistanceFunction();
//        PointBasedFastMatching testMatching = new PointBasedFastMatching(cleansedMap, dist, 4, inputShortestPathFile,isUpdate);
//        testMatching.knnSearchTest(trajectoryList.get(7), roadNetworkGraph);

        // start the matching process sequentially
        GPSDistanceFunction distFunc = new GPSDistanceFunction();
        PointBasedFastMatching matching = new PointBasedFastMatching(originalMap, distFunc, 128, allPairSP, isUpdate);

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
//            matchedTraj = matching.doMatching(originTraj, cleansedMap);
//            originalTrajList.add(originTraj);
//            matchedResultList.add(matchedTraj);
//        }

        // list read trajectories
//        Trajectory t = trajectoryList.get(15);
        for (Trajectory t : trajectoryList) {
            Pair<RoadWay, List<Trajectory>> trajMatchResult = matching.doMatching(t);
            matchedResultList.add(trajMatchResult._1());
            unmatchedTrajList.addAll(trajMatchResult._2());
            if (matchedResultList.size() % (trajectoryList.size() / 10) == 0) {
                System.out.println(matchedResultList.size() / (trajectoryList.size() / 10) + "/10 trajectories matched");
            }
        }

//        // graph stream display
//        List<Trajectory> originalTrajList = new ArrayList<>();
//        originalTrajList.add(t);
//        GraphStreamDisplay display = new GraphStreamDisplay();
//        display.setGroundTruthGraph(originalMap);
//        display.setRawTrajectories(originalTrajList);
//        display.setMatchedTrajectories(matchedResultList);
//        display.setCentralPoint(originalTrajList.get(0).getPoints().get(10));
//        Viewer viewer = display.generateGraph().display(false);
//        if (display.getCentralPoint() != null) {
//            View view = viewer.getDefaultView();
//            view.getCamera().setViewCenter(display.getCentralPoint().x(), display.getCentralPoint().y(), 0);
//            view.getCamera().setViewPercent(0.45);
//        }
        CSVTrajectoryWriter.matchedTrajectoryWriter(matchedResultList, outputTrajPath);
        return unmatchedTrajList;
    }
}
