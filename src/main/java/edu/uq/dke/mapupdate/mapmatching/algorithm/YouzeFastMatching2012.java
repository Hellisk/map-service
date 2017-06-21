package edu.uq.dke.mapupdate.mapmatching.algorithm;

import edu.uq.dke.mapupdate.mapmatching.io.CSVMapReader;
import edu.uq.dke.mapupdate.mapmatching.io.CSVTrajectoryReader;
import edu.uq.dke.mapupdate.visualisation.GraphStreamDisplay;
import org.jdom2.JDOMException;
import traminer.util.map.roadnetwork.RoadNetworkGraph;
import traminer.util.trajectory.Trajectory;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by uqpchao on 23/05/2017.
 */
public class YouzeFastMatching2012 {
    public static void YouzeFastMatching(String cityName, String inputTrajectoryPath, String inputMapPath, String outputTrajectoryPath, int numThreads) throws JDOMException, IOException {

        // read ground truth map
        String inputVertexPath = inputMapPath + cityName + "_vertices_osm.txt";
        String inputEdgePath = inputMapPath + cityName + "_edges_osm.txt";
        CSVMapReader csvMapReader = new CSVMapReader(inputVertexPath, inputEdgePath);
        RoadNetworkGraph roadNetworkGraph = csvMapReader.readCSV();

        GraphStreamDisplay display = new GraphStreamDisplay();
        display.setRoadGraph(roadNetworkGraph);

        // unfolding map display, currently failed
//        UnfoldingMapDemo displayUnfolding = new UnfoldingMapDemo();
//        displayUnfolding.display(cityName,inputMapPath,"map");

        // basic display
//        roadNetworkGraph.display();
//        System.out.println(roadNetworkGraph.toString());

        // read input trajectories
        CSVTrajectoryReader csvTrajectoryReader = new CSVTrajectoryReader();
        Stream<Trajectory> trajectoryStream = csvTrajectoryReader.readTrajectoryFiles(inputTrajectoryPath);
        List<Trajectory> rawTraj = trajectoryStream.collect(Collectors.toList());
        display.setRawTrajectories(rawTraj);

//        System.out.println(trajectoryStream.toString());
//        List<RoadWay> result = new ArrayList<>();
//
//        // start the matching process sequentially
//        PointDistanceFunction distfunc = new EuclideanDistanceFunction();
//        PointBasedFastMatching matching = new PointBasedFastMatching(roadNetworkGraph, distfunc, 4);
//
//        Iterator<Trajectory> traj = trajectoryStream.iterator();
//        while (traj.hasNext()) {
//            RoadWay matchedTrajectory = new RoadWay();
//            Trajectory originTraj = traj.next();
//            matchedTrajectory = matching.doMatching(originTraj, roadNetworkGraph);
////            displayTrajMatch(originTraj,matchedTrajectory);
//            result.add(matchedTrajectory);
//        }


        display.generateGraph().display(false);
//        CSVTrajectoryWriter.TrajectoryWriter(result, outputTrajectoryPath);
    }

}
