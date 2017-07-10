package edu.uq.dke.mapupdate.visualisation;

import edu.uq.dke.mapupdate.mapmatching.io.PointWithSegment;
import org.graphstream.graph.EdgeRejectedException;
import org.graphstream.graph.ElementNotFoundException;
import org.graphstream.graph.Graph;
import org.graphstream.graph.implementations.SingleGraph;
import traminer.util.map.roadnetwork.RoadNetworkGraph;
import traminer.util.map.roadnetwork.RoadNode;
import traminer.util.map.roadnetwork.RoadWay;
import traminer.util.spatial.objects.Point;
import traminer.util.trajectory.Trajectory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by uqpchao on 21/06/2017.
 */
public class GraphStreamDisplay {

    private RoadNetworkGraph groundTruthGraph = new RoadNetworkGraph();
    private RoadNetworkGraph roadNetworkGraph = new RoadNetworkGraph();
    private List<Trajectory> rawTrajectories = new ArrayList<>();
    private List<RoadWay> matchedTrajectories = new ArrayList<>();
    private List<PointWithSegment> indexNode = new ArrayList<>();
    private Point selectPoint = null;
    private List<PointWithSegment> candidatePoints = new ArrayList<>();
    private Point CentralPoint = null;

    public Graph generateGraph() {

        Graph graph = new SingleGraph("TrajectoryOnMap");
        graph.addAttribute("ui.stylesheet", "node {size: 1px;fill-color: #777;text-mode: hidden;z-index: 0;} node.rawtrajectory{size: 6px;fill-color: red;text-mode: hidden;z-index: 0;} node.matchtrajectory{size: 6px;fill-color: green;text-mode: hidden;z-index: 0;} edge {shape: line;fill-color: #222;arrow-size: 4px, 4px;} edge.rawtrajectory {shape: line;fill-color: red;arrow-size: 2px, 2px;} edge.matchtrajectory {shape: line;fill-color: green;arrow-size: 2px, 2px;}");

        // display the base map
        if (!groundTruthGraph.isEmpty()) {
            Map<String, String> nodeIDMap = new HashMap<>();
            // create one graph node per road network node.
            for (RoadNode node : groundTruthGraph.getNodes()) {
                graph.addNode(node.getId() + "_MN")
                        .setAttribute("xy", node.lon(), node.lat());
                nodeIDMap.put(node.lon() + "_" + node.lat(), node.getId());
            }
            // create one graph edge for every edge in the road ways
            for (RoadWay way : groundTruthGraph.getWays()) {
                for (int i = 0; i < way.size() - 1; i++) {
                    RoadNode nodeI = way.getNode(i);
                    RoadNode nodeJ = way.getNode(i + 1);
                    String idI = nodeI.getId();
                    String idJ = nodeJ.getId();
                    try {
                        if (nodeIDMap.containsKey(nodeI.lon() + "_" + nodeI.lat())) {
                            if (!nodeIDMap.get(nodeI.lon() + "_" + nodeI.lat()).equals(idI)) {
//                                System.out.println("Same pointID with different coordinates:"+nodeIDMap.get(nodeI.lon() + "_" + nodeI.lat())+","+ idI);
//                                continue;
                                idI = nodeIDMap.get(nodeI.lon() + "_" + nodeI.lat());
                            }
                        } else {
                            graph.addNode(idI + "_MN").setAttribute("xy", nodeI.lon(), nodeI.lat());
                            nodeIDMap.put(nodeI.lon() + "_" + nodeI.lat(), idI);
                        }

                        if (nodeIDMap.containsKey(nodeJ.lon() + "_" + nodeJ.lat())) {
                            if (!nodeIDMap.get(nodeJ.lon() + "_" + nodeJ.lat()).equals(idJ)) {
//                                System.out.println("Same pointID with different coordinates:"+nodeIDMap.get(nodeI.lon() + "_" + nodeI.lat())+","+ idI);
//                                continue;
                                idJ = nodeIDMap.get(nodeJ.lon() + "_" + nodeJ.lat());
                            }
                        } else {
                            graph.addNode(idJ + "_MN").setAttribute("xy", nodeJ.lon(), nodeJ.lat());
                            nodeIDMap.put(nodeJ.lon() + "_" + nodeJ.lat(), idJ);
                        }


                        if (graph.getEdge(way.getId() + "_ME" + i) == null) {
                            // TODO avoid the exception in the future
                            graph.addEdge(way.getId() + "_ME" + i, idI + "_MN", idJ + "_MN");
                        } else System.out.println(way.getId());
                    } catch (EdgeRejectedException e) {
//                        System.out.println("fail");
                    } catch (ElementNotFoundException f) {
                        System.out.println("missing");
                    }
                }
            }
        }

        if (!roadNetworkGraph.isEmpty()) {
            Map<String, String> nodeIDMap = new HashMap<>();
            // create one graph node per road network node.
            for (RoadNode node : roadNetworkGraph.getNodes()) {
                graph.addNode(node.getId() + "_RNN")
                        .setAttribute("xy", node.lon(), node.lat());
                graph.getNode(node.getId() + "_RNN").addAttribute("ui.class", "rawtrajectory");
                nodeIDMap.put(node.lon() + "_" + node.lat(), node.getId());
            }
            // create one graph edge for every edge in the road ways
            for (RoadWay way : groundTruthGraph.getWays()) {
                for (int i = 0; i < way.size() - 1; i++) {
                    RoadNode nodeI = way.getNode(i);
                    RoadNode nodeJ = way.getNode(i + 1);
                    String idI = nodeI.getId();
                    String idJ = nodeJ.getId();
                    try {
                        if (nodeIDMap.containsKey(nodeI.lon() + "_" + nodeI.lat())) {
                            if (!nodeIDMap.get(nodeI.lon() + "_" + nodeI.lat()).equals(idI)) {
//                                System.out.println("Same pointID with different coordinates");
//                                continue;
                                idI = nodeIDMap.get(nodeI.lon() + "_" + nodeI.lat());
                            }
                        } else {
                            graph.addNode(idI + "_RNN").setAttribute("xy", nodeI.lon(), nodeI.lat());
                            nodeIDMap.put(nodeI.lon() + "_" + nodeI.lat(), idI);
                        }

                        if (nodeIDMap.containsKey(nodeJ.lon() + "_" + nodeJ.lat())) {
                            if (!nodeIDMap.get(nodeJ.lon() + "_" + nodeJ.lat()).equals(idJ)) {
//                                System.out.println("Same pointID with different coordinates");
//                                continue;
                                idI = nodeIDMap.get(nodeJ.lon() + "_" + nodeJ.lat());
                            }
                        } else {
                            graph.addNode(idJ + "_RNN").setAttribute("xy", nodeJ.lon(), nodeJ.lat());
                            nodeIDMap.put(nodeJ.lon() + "_" + nodeJ.lat(), idJ);
                        }
                        if (graph.getEdge(way.getId() + "_ME" + i) == null) {
                            // TODO avoid the exception in the future
                            graph.addEdge(way.getId() + "_RNE" + i, idI + "_RNN", idJ + "_RNN").addAttribute("ui.class", "rawtrajectory");
                        }
                    } catch (EdgeRejectedException e) {
//                        System.out.println("fail");
                    } catch (ElementNotFoundException f) {
                        System.out.println("missing");
                    }
                }
            }
        }

        // display the raw trajectories
        if (!rawTrajectories.isEmpty()) {
            int trajID = 0;
            for (Trajectory traj : rawTrajectories) {
                if (!traj.getCoordinates().isEmpty() && !traj.getEdges().isEmpty()) {
                    int pointID = 0;
                    for (Point point : traj.getCoordinates()) {
                        graph.addNode(trajID + "_" + pointID + "_RN").setAttribute("xy", point.x(), point.y());
                        graph.getNode(trajID + "_" + pointID + "_RN").addAttribute("ui.class", "rawtrajectory");
                        if (pointID != 0) {
                            try {
                                // TODO avoid the exception in the future
                                graph.addEdge(trajID + "_" + (pointID - 1) + "_RE", trajID + "_" + (pointID - 1) + "_RN", trajID + "_" + pointID + "_RN").addAttribute("ui.class", "rawtrajectory");
                            } catch (EdgeRejectedException e) {
                                System.out.println("fail");
                            }

                        }
                        pointID++;
                    }
                }
                trajID++;
            }
        }

        // display the raw trajectories
        if (!matchedTrajectories.isEmpty()) {
            int trajID = 0;
            for (RoadWay traj : matchedTrajectories) {
                if (!traj.getNodes().isEmpty() && !traj.getEdges().isEmpty()) {
                    int pointID = 0;
                    for (RoadNode point : traj.getNodes()) {
                        graph.addNode(trajID + "_" + pointID + "_MAN").setAttribute("xy", point.lon(), point.lat());
                        graph.getNode(trajID + "_" + pointID + "_MAN").addAttribute("ui.class", "matchtrajectory");
                        if (pointID != 0) {
                            try {
                                // TODO avoid the exception in the future
                                graph.addEdge(trajID + "_" + (pointID - 1) + "_MAE", trajID + "_" + (pointID - 1) + "_MAN", trajID + "_" + pointID + "_MAN").addAttribute("ui.class", "matchtrajectory");
                            } catch (EdgeRejectedException e) {
//                                System.out.println("fail");
                            }

                        }
                        pointID++;
                    }
                }
                trajID++;
            }
        }

        if (!indexNode.isEmpty()) {
            // create one graph node per road network node.
            int i = 0;
            for (PointWithSegment node : indexNode) {
                graph.addNode(i + "_INDEXN")
                        .setAttribute("xy", node.x(), node.y());
                i++;
            }
        }

        if (selectPoint != null) {
            graph.addNode("0_SELN")
                    .setAttribute("xy", selectPoint.x(), selectPoint.y());
            graph.getNode("0_SELN").addAttribute("ui.class", "rawtrajectory");
        }

        if (!candidatePoints.isEmpty()) {
            int i = 0;
            // create one graph node per road network node.
            for (PointWithSegment node : candidatePoints) {
                graph.addNode(i + "_CANN")
                        .setAttribute("xy", node.x(), node.y());
                graph.getNode(i + "_CANN").addAttribute("ui.class", "matchtrajectory");
                i++;
            }
        }

        return graph;
    }

    public void setGroundTruthGraph(RoadNetworkGraph groundTruthGraph) {
        this.groundTruthGraph = groundTruthGraph;
    }

    public void setRoadNetworkGraph(RoadNetworkGraph roadNetworkGraph) {
        this.roadNetworkGraph = roadNetworkGraph;
    }

    public void setRawTrajectories(List<Trajectory> rawTrajectories) {
        this.rawTrajectories = rawTrajectories;
    }

    public void setMatchedTrajectories(List<RoadWay> matchedTrajectories) {
        this.matchedTrajectories = matchedTrajectories;
    }

    public void setIndexNodes(List<PointWithSegment> indexNode) {
        this.indexNode = indexNode;
    }

    public void setCandidatePoints(List<PointWithSegment> candidatePoints) {
        this.candidatePoints = candidatePoints;
    }

    public void addCandidatePoints(List<PointWithSegment> candidatePoints) {
        this.candidatePoints.addAll(candidatePoints);
    }

    public void setSelectPoint(Point selectPoint) {
        this.selectPoint = selectPoint;
    }

    public Point getCentralPoint() {
        return CentralPoint;
    }

    public void setCentralPoint(Point centralPoint) {
        CentralPoint = centralPoint;
    }
}
