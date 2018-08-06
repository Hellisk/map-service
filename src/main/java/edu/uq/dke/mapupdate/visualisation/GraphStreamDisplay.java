package edu.uq.dke.mapupdate.visualisation;

import edu.uq.dke.mapupdate.util.object.datastructure.PointNodePair;
import edu.uq.dke.mapupdate.util.object.roadnetwork.RoadNetworkGraph;
import edu.uq.dke.mapupdate.util.object.roadnetwork.RoadNode;
import edu.uq.dke.mapupdate.util.object.roadnetwork.RoadWay;
import edu.uq.dke.mapupdate.util.object.spatialobject.Point;
import edu.uq.dke.mapupdate.util.object.spatialobject.Trajectory;
import org.graphstream.graph.Graph;
import org.graphstream.graph.implementations.MultiGraph;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Created by uqpchao on 21/06/2017.
 */
public class GraphStreamDisplay {

    private RoadNetworkGraph groundTruthGraph = new RoadNetworkGraph();
    private RoadNetworkGraph roadNetworkGraph = new RoadNetworkGraph();
    private List<Trajectory> rawTrajectories = new ArrayList<>();
    private List<List<PointNodePair>> matchedTrajectories = new ArrayList<>();
    private List<Point> indexNode = new ArrayList<>();
    private Point selectPoint = null;
    private List<Point> candidatePoints = new ArrayList<>();
    private Point CentralPoint = null;

    public Graph generateGraph() {

        Graph graph = new MultiGraph("TrajectoryOnMap");
        graph.addAttribute("ui.stylesheet", "node {size: 1px;fill-color: #777;text-mode: hidden;z-index: 0;} node.rawtrajectory{size: 6px;fill-color: red;text-mode: hidden;z-index: 0;} node.matchtrajectory{size: 6px;fill-color: green;text-mode: hidden;z-index: 0;} edge {shape: line;fill-color: #222;arrow-size: 4px, 4px;} edge.rawtrajectory {shape: line;fill-color: red;arrow-size: 2px, 2px;} edge.matchtrajectory {shape: line;fill-color: green;arrow-size: 2px, 2px;}");

        // display the base map
        if (!groundTruthGraph.isEmpty()) {
            HashSet<String> nodeIDMap = new HashSet<>();
            // create one graph node per road network node.
            for (RoadNode node : groundTruthGraph.getNodes()) {
                String id = node.lon() + "_" + node.lat();
                graph.addNode(id + "_G")
                        .setAttribute("xy", node.lon(), node.lat());
                nodeIDMap.add(id);
            }
            // create one graph edge for every edge in the road ways
            for (RoadWay way : groundTruthGraph.getWays()) {
                for (int i = 0; i < way.size() - 1; i++) {
                    RoadNode nodeI = way.getNode(i);
                    RoadNode nodeJ = way.getNode(i + 1);
                    String idI = nodeI.lon() + "_" + nodeI.lat();
                    String idJ = nodeJ.lon() + "_" + nodeJ.lat();
                    if (!nodeIDMap.contains(idI)) {
                        graph.addNode(idI + "_G").setAttribute("xy", nodeI.lon(), nodeI.lat());
                        nodeIDMap.add(idI);
                    }
                    if (!nodeIDMap.contains(idJ)) {
                        graph.addNode(idJ + "_G").setAttribute("xy", nodeJ.lon(), nodeJ.lat());
                        nodeIDMap.add(idJ);
                    }

                    if (graph.getEdge(way.getID() + "_GE" + i) == null) {
                        graph.addEdge(way.getID() + "_GE" + i, idI + "_G", idJ + "_G");
                    } else System.out.println(way.getID());
                }
            }
        }

        if (!roadNetworkGraph.isEmpty()) {
            HashSet<String> nodeIDSet = new HashSet<>();
            // create one graph node per road network node.
            for (RoadNode node : roadNetworkGraph.getNodes()) {
                String id = node.lon() + "_" + node.lat();
                graph.addNode(id + "_R")
                        .setAttribute("xy", node.lon(), node.lat());
                graph.getNode(id + "_R").addAttribute("ui.class", "rawtrajectory");
                nodeIDSet.add(id);
            }
            // create one graph edge for every edge in the road ways
            for (RoadWay way : groundTruthGraph.getWays()) {
                for (int i = 0; i < way.size() - 1; i++) {
                    RoadNode nodeI = way.getNode(i);
                    RoadNode nodeJ = way.getNode(i + 1);
                    String idI = nodeI.lon() + "_" + nodeI.lat();
                    String idJ = nodeJ.lon() + "_" + nodeJ.lat();
                    if (!nodeIDSet.contains(idI)) {
                        graph.addNode(idI + "_R").setAttribute("xy", nodeI.lon(), nodeI.lat());
                        graph.getNode(idI + "_R").addAttribute("ui.class", "rawtrajectory");
                        nodeIDSet.add(idI);
                    }
                    if (!nodeIDSet.contains(idJ)) {
                        graph.addNode(idJ + "_R").setAttribute("xy", nodeJ.lon(), nodeJ.lat());
                        graph.getNode(idJ + "_R").addAttribute("ui.class", "rawtrajectory");
                        nodeIDSet.add(idJ);
                    }

                    if (graph.getEdge(way.getID() + "_RE" + i) == null) {
                        graph.addEdge(way.getID() + "_RE" + i, idI + "_R", idJ + "_R").addAttribute("ui.class", "rawtrajectory");
                    } else System.out.println(way.getID());
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
                        graph.addNode(trajID + "_" + pointID + "_RT").setAttribute("xy", point.x(), point.y());
                        graph.getNode(trajID + "_" + pointID + "_RT").addAttribute("ui.class", "rawtrajectory");
                        if (pointID != 0) {
                            graph.addEdge(trajID + "_" + (pointID - 1) + "_RTE", trajID + "_" + (pointID - 1) + "_RT", trajID + "_" + pointID + "_RT").addAttribute("ui.class", "rawtrajectory");
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
            for (List<PointNodePair> traj : matchedTrajectories) {
                if (!traj.isEmpty()) {
                    int pointID = 0;
                    for (PointNodePair point : traj) {
                        graph.addNode(trajID + "_" + pointID + "_MTL").setAttribute("xy", point.getMatchingPoint().getMatchedSegment().x1(), point.getMatchingPoint().getMatchedSegment().y1());
                        graph.getNode(trajID + "_" + pointID + "_MTL").addAttribute("ui.class", "matchtrajectory");
                        graph.addNode(trajID + "_" + pointID + "_MTR").setAttribute("xy", point.getMatchingPoint().getMatchedSegment().x2(), point.getMatchingPoint().getMatchedSegment().y2());
                        graph.getNode(trajID + "_" + pointID + "_MTR").addAttribute("ui.class", "matchtrajectory");
                        if (pointID != 0) {
                            graph.addEdge(trajID + "_" + pointID + "_MTE1", trajID + "_" + (pointID - 1) + "_MTR", trajID + "_" + pointID + "_MTL").addAttribute("ui.class", "matchtrajectory");
                            graph.addEdge(trajID + "_" + pointID + "_MTE2", trajID + "_" + pointID + "_MTL", trajID + "_" + pointID + "_MTR").addAttribute("ui.class", "matchtrajectory");
                        } else {
                            graph.addEdge(trajID + "_" + pointID + "_MTE2", trajID + "_" + pointID + "_MTL", trajID + "_" + pointID + "_MTR").addAttribute("ui.class", "matchtrajectory");
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
            for (Point node : indexNode) {
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
            for (Point node : candidatePoints) {
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

    public void setMatchedTrajectories(List<List<PointNodePair>> matchedTrajectories) {
        this.matchedTrajectories = matchedTrajectories;
    }

    public void setIndexNodes(List<Point> indexNode) {
        this.indexNode = indexNode;
    }

    public void setCandidatePoints(List<Point> candidatePoints) {
        this.candidatePoints = candidatePoints;
    }

    public void addCandidatePoints(List<Point> candidatePoints) {
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