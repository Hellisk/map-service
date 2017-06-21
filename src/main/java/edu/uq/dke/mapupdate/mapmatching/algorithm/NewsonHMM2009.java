package edu.uq.dke.mapupdate.mapmatching.algorithm;

import edu.uq.dke.mapupdate.mapmatching.io.CSVMapReader;
import edu.uq.dke.mapupdate.mapmatching.io.CSVTrajectoryReader;
import edu.uq.dke.mapupdate.mapmatching.io.CSVTrajectoryWriter;
import org.jdom2.JDOMException;
import traminer.util.map.matching.MapMatchingMethod;
import traminer.util.map.matching.hmm.HMMMatching;
import traminer.util.map.matching.nearest.PointToEdgeMatching;
import traminer.util.map.roadnetwork.RoadNetworkGraph;
import traminer.util.map.roadnetwork.RoadWay;
import traminer.util.spatial.distance.EuclideanDistanceFunction;
import traminer.util.spatial.distance.PointDistanceFunction;
import traminer.util.trajectory.Trajectory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by uqpchao on 22/05/2017.
 */
public class NewsonHMM2009 {

    public static void NewsonHMM(String cityName, String inputTrajectoryPath, String inputMapPath, String outputTrajectoryPath, int numThreads) throws JDOMException, IOException {
        String inputVertexPath = inputMapPath + cityName + "_vertices_osm.txt";
        String inputEdgePath = inputMapPath + cityName + "_edges_osm.txt";

        CSVMapReader csvMapReader = new CSVMapReader(inputVertexPath, inputEdgePath);
        RoadNetworkGraph roadNetworkGraph = new RoadNetworkGraph();
        roadNetworkGraph = csvMapReader.readCSV();
//        System.out.println(roadNetworkGraph.toString());
        CSVTrajectoryReader csvTrajectoryReader = new CSVTrajectoryReader();
        Stream<Trajectory> trajectoryStream = csvTrajectoryReader.readTrajectoryFiles(inputTrajectoryPath);
//        System.out.println(trajectoryStream.toString());
        PointDistanceFunction distfunc = new EuclideanDistanceFunction();
        MapMatchingMethod hmm = new HMMMatching(4.17, distfunc);
        MapMatchingMethod nearest = new PointToEdgeMatching();
        // sequential test
        Trajectory test = trajectoryStream.collect(Collectors.toList()).get(5);
        test.display();
        List<RoadWay> result = new ArrayList<>();
        Iterator<Trajectory> traj = trajectoryStream.iterator();
        while (traj.hasNext()) {
            RoadWay newRoadWay = nearest.doMatching(traj.next(), roadNetworkGraph);
//            RoadWay newRoadWay = hmm.doMatching(traj.next(),roadNetworkGraph);
            result.add(newRoadWay);
        }
//        ParallelMapMatching paraHMM = new ParallelMapMatching(hmm);
//        Stream<RoadWay> result = paraHMM.doMatching(trajectoryStream, roadNetworkGraph,numThreads);
//        System.out.println(result.toString());
        CSVTrajectoryWriter.TrajectoryWriter(result, outputTrajectoryPath);
    }
}
