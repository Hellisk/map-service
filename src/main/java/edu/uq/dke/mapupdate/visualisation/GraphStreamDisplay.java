package edu.uq.dke.mapupdate.visualisation;

import org.graphstream.graph.EdgeRejectedException;
import org.graphstream.graph.Graph;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.graph.implementations.SingleNode;
import traminer.util.map.roadnetwork.RoadNetworkGraph;
import traminer.util.map.roadnetwork.RoadNode;
import traminer.util.map.roadnetwork.RoadWay;
import traminer.util.spatial.objects.Edges;
import traminer.util.spatial.objects.Point;
import traminer.util.trajectory.Trajectory;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by uqpchao on 21/06/2017.
 */
public class GraphStreamDisplay {

    private RoadNetworkGraph roadGraph = new RoadNetworkGraph();
    private List<Trajectory> rawTrajectories = new ArrayList<>();
    private List<Trajectory> matchedTrajectories = new ArrayList<>();

    public Graph generateGraph() {

        Graph graph = new SingleGraph("TrajectoryOnMap");
        graph.addAttribute("ui.stylesheet", "node {size: 1px;fill-color: #777;text-mode: hidden;z-index: 0;} node.rawtrajectory{size: 2px;fill-color: red;text-mode: hidden;z-index: 0;} node.matchtrajectory{size: 2px;fill-color: green;text-mode: hidden;z-index: 0;} edge {shape: line;fill-color: #222;arrow-size: 3px, 2px;} edge.rawtrajectory {shape: line;fill-color: red;arrow-size: 2px, 2px;} edge.matchtrajectory {shape: line;fill-color: green;arrow-size: 2px, 2px;}");

        // display the base map
        if (!roadGraph.isEmpty()) {
            // create one graph node per road network node.
            for (RoadNode node : roadGraph.getNodes()) {
                graph.addNode(node.getId() + "_MN")
                        .setAttribute("xy", node.lon(), node.lat());
            }
            // create one graph edge for every edge in the road ways
            for (RoadWay way : roadGraph.getWays()) {
                for (int i = 0; i < way.size() - 1; i++) {
                    RoadNode nodei = way.getNodes().get(i);
                    RoadNode nodej = way.getNodes().get(i + 1);
                    SingleNode newNode = null;
                    try {
                        // TODO avoid the exception in the future
                        graph.addEdge(way.getId() + "_ME" + i, nodei.getId() + "_MN", nodej.getId() + "_MN");
                    } catch (EdgeRejectedException e) {
                        System.out.println("fail");
                    }
                }
            }
        }

        // display the raw trajectories
        if (!rawTrajectories.isEmpty()) {
            for (Trajectory traj : rawTrajectories) {
                if (!traj.getCoordinates().isEmpty()) {
                    for (Point point : traj.getCoordinates()) {
                        graph.addNode(point.getId() + "_RN").setAttribute("xy", point.x(), point.y());
                        graph.getNode(point.getId() + "_RN").addAttribute("ui.class", "rawtrajectory");
                    }
                }
                if (!traj.getEdges().isEmpty()) {
                    for (Edges edge : traj.getEdges()) {
                        Point pointi = edge.getCoordinates().get(0);
                        Point pointj = edge.getCoordinates().get(1);
                        try {
                            // TODO avoid the exception in the future
                            graph.addEdge(edge.getId() + "_RE", pointi.getId() + "_RN", pointj.getId() + "_RN").addAttribute("ui.class", "rawtrajectory");
                        } catch (EdgeRejectedException e) {
                            System.out.println("fail");
                        }
                    }
                }
            }
        }
        return graph;
    }


    public void setRoadGraph(RoadNetworkGraph roadGraph) {
        this.roadGraph = roadGraph;
    }

    public void setRawTrajectories(List<Trajectory> rawTrajectories) {
        this.rawTrajectories = rawTrajectories;
    }

    public void setMatchedTrajectories(List<Trajectory> matchedTrajectories) {
        this.matchedTrajectories = matchedTrajectories;
    }
}
