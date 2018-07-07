package edu.uq.dke.mapupdate.mapmerge;

import edu.uq.dke.mapupdate.util.dijkstra.RoutingGraph;
import edu.uq.dke.mapupdate.util.function.GreatCircleDistanceFunction;
import edu.uq.dke.mapupdate.util.index.grid.Grid;
import edu.uq.dke.mapupdate.util.io.CSVMapWriter;
import edu.uq.dke.mapupdate.util.object.datastructure.XYObject;
import edu.uq.dke.mapupdate.util.object.roadnetwork.RoadNetworkGraph;
import edu.uq.dke.mapupdate.util.object.roadnetwork.RoadNode;
import edu.uq.dke.mapupdate.util.object.roadnetwork.RoadWay;
import edu.uq.dke.mapupdate.util.object.spatialobject.Point;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.sql.SQLOutput;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static edu.uq.dke.mapupdate.Main.MIN_ROAD_LENGTH;

public class MapMerge {
    private RoadNetworkGraph rawMap;
    private List<RoadWay> inferredWayList;
    private Map<String, String> loc2RemovedWayID = new HashMap<>();
    private Map<String, RoadWay> loc2RoadWayMapping = new HashMap<>();
    private Map<Point, RoadNode> point2RoadNodeMapping = new HashMap<>();
    private GreatCircleDistanceFunction distFunc = new GreatCircleDistanceFunction();
    private Grid<Point> grid;
    private int avgNodePerGrid = 64;
    private int directMergeDistance = 50;
    private int subTrajectoryMergeDistance = 10;
    private long maxAbsRoadWayID;
    private long maxMiniNodeID;
    private long maxRoadNodeID;
    public List<RoadWay> isolatedEdges = new ArrayList<>();    // list of roads that are not close to any of the existing intersections

    public MapMerge(RoadNetworkGraph rawMap, List<RoadWay> inferredWayList, List<RoadWay> removedWayList, int avgNodePerGrid, int
            directMergeDistance, int subTrajectoryMergeDistance) {
        this.rawMap = rawMap;
        for (RoadWay w : rawMap.getWays())
            w.setNewRoad(false);
        this.inferredWayList = inferredWayList;
        for (RoadWay w : removedWayList)
            this.loc2RemovedWayID.put(w.getFromNode().lon() + "_" + w.getFromNode().lat() + "," + w.getToNode().lon() + "_" + w.getToNode()
                    .lat(), w.getId());
        if (avgNodePerGrid > 0)
            this.avgNodePerGrid = avgNodePerGrid;
        if (directMergeDistance > 0)
            this.directMergeDistance = directMergeDistance;
        if (subTrajectoryMergeDistance > 0)
            this.subTrajectoryMergeDistance = subTrajectoryMergeDistance;
        for (RoadWay w : rawMap.getWays())
            loc2RoadWayMapping.put(w.getFromNode().lon() + "_" + w.getFromNode().lat() + "," + w.getToNode().lon() + "_" + w.getToNode()
                    .lat(), w);
        this.maxAbsRoadWayID = rawMap.getMaxAbsWayID();
        this.maxMiniNodeID = rawMap.getMaxMiniNodeID();
        this.maxRoadNodeID = rawMap.getMaxRoadNodeID();
    }

    public RoadNetworkGraph NearestNeighbourMapMerge() {
        buildGridIndex();
        double prevMaxWayID = rawMap.getMaxAbsWayID();

        for (RoadWay w : inferredWayList) {
            Point startPoint = this.grid.nearestNeighborSearch(w.getFromNode().lon(), w.getFromNode().lat(), distFunc).getSpatialObject();
            Point endPoint = this.grid.nearestNeighborSearch(w.getToNode().lon(), w.getToNode().lat(), distFunc).getSpatialObject();

            if (distFunc.distance(startPoint, w.getFromNode().toPoint()) < directMergeDistance && distFunc.distance(endPoint, w
                    .getToNode().toPoint()) < directMergeDistance) {
                RoadWay newWay = roadMapConnection(w, startPoint, endPoint);
                if (newWay != null) {
                    if (loc2RemovedWayID.containsKey(newWay.getFromNode().lon() + "_" + newWay.getFromNode().lat() + "," + newWay.getToNode
                            ().lon() + "_" + newWay.getToNode().lat())) {   // for a double-direction removed road, both direction should
                        // be removed
                        String currID = loc2RemovedWayID.get(newWay.getFromNode().lon() + "_" + newWay.getFromNode().lat() + "," + newWay
                                .getToNode().lon() + "_" + newWay.getToNode().lat());
                        newWay.setId(currID);
                        for (int i = 1; i < newWay.getNodes().size() - 1; i++) {
                            RoadNode n = newWay.getNode(i);
                            maxMiniNodeID++;
                            n.setId(maxMiniNodeID + "-");
                        }
                        newWay.setNewRoad(true);
                        newWay.setConfidenceScore(w.getConfidenceScore());
                        rawMap.addWay(newWay);
                        RoadWay reverseRoad = new RoadWay(currID.contains("-") ? currID.substring(currID.indexOf("-")) : "-" + currID);
                        reverseRoad.addNode(newWay.getToNode());
                        for (int i = newWay.getNodes().size() - 2; i > 0; i--) {
                            maxMiniNodeID++;
                            RoadNode reverseNode = new RoadNode(maxMiniNodeID + "-", newWay.getNode(i).lon(), newWay.getNode(i).lat());
                            reverseRoad.addNode(reverseNode);
                        }
                        reverseRoad.addNode(newWay.getFromNode());
                        reverseRoad.setNewRoad(true);
                        reverseRoad.setConfidenceScore(w.getConfidenceScore());
                        rawMap.addWay(reverseRoad);
                    } else {    // no removed edge is found matching the current road
                        maxAbsRoadWayID++;
                        newWay.setId(maxAbsRoadWayID + "");
                        List<RoadNode> nodes = newWay.getNodes();
                        for (int i = 1, nodesSize = nodes.size() - 1; i < nodesSize; i++) {
                            RoadNode n = nodes.get(i);
                            maxMiniNodeID++;
                            n.setId(maxMiniNodeID + "-");
                        }
                        newWay.setNewRoad(true);
                        newWay.setConfidenceScore(w.getConfidenceScore());
                        rawMap.addWay(newWay);
                        RoadWay reverseRoad = new RoadWay("-" + maxAbsRoadWayID);
                        reverseRoad.addNode(newWay.getToNode());
                        for (int i = newWay.getNodes().size() - 2; i > 0; i--) {
                            maxMiniNodeID++;
                            RoadNode reverseNode = new RoadNode(maxMiniNodeID + "-", newWay.getNode(i).lon(), newWay.getNode(i).lat());
                            reverseRoad.addNode(reverseNode);
                        }
                        reverseRoad.addNode(newWay.getFromNode());
                        reverseRoad.setNewRoad(true);
                        reverseRoad.setConfidenceScore(w.getConfidenceScore());
                        rawMap.addWay(reverseRoad);
                    }
                }
            } else {
                findSubRoadConnection(w);   // find sub-trajectories that can be connected to the existing road ways
            }
        }
        System.out.println("Nearest neighbour map merge completed. Total number of road way added:" + (rawMap.getMaxAbsWayID()
                - prevMaxWayID) * 2);
        return rawMap;
    }

    private void findSubRoadConnection(RoadWay roadWay) {
        RoadNode startNode = roadWay.getNode(0);
        boolean foundConnection = false;
        List<RoadNode> currNodeList = new ArrayList<>();
        currNodeList.add(startNode);
        for (int i = 0; i < roadWay.getNodes().size() - 1; i++) {
            List<Point> intermediatePoint = edgeSegmentation(roadWay.getNode(i), roadWay.getNode(i + 1));
            for (Point p : intermediatePoint) {
                Point nearestPoint = this.grid.nearestNeighborSearch(p.x(), p.y(), distFunc).getSpatialObject();
                if (distFunc.distance(p, nearestPoint) < subTrajectoryMergeDistance && !point2RoadNodeMapping.get(nearestPoint).equals
                        (startNode)) {
                    RoadNode linkedNode = point2RoadNodeMapping.get(nearestPoint);
                    currNodeList.add(linkedNode);
                    if (foundConnection) {
                        // both endpoints are connected to an existing intersection
                        if (!loc2RoadWayMapping.containsKey(currNodeList.get(0).lon() + "_" + currNodeList.get(0).lat() + "," +
                                currNodeList.get(currNodeList.size() - 1).lon() + "_" + currNodeList.get(currNodeList.size() - 1).lat())) {
                            // if the current road way does not exist
                            if (loc2RemovedWayID.containsKey(currNodeList.get(0).lon() + "_" + currNodeList.get(0).lat() + "," +
                                    currNodeList.get(currNodeList.size() - 1).lon() + "_" + currNodeList.get(currNodeList.size() - 1).lat())) {
                                // if the current road way is equivalent to an removed road, copy its road id
                                String roadID = loc2RemovedWayID.get(currNodeList.get(0).lon() + "_" + currNodeList.get(0).lat() + "," +
                                        currNodeList.get(currNodeList.size() - 1).lon() + "_" + currNodeList.get(currNodeList.size() - 1)
                                        .lat());
                                for (int j = 1; j < currNodeList.size() - 1; j++) {
                                    RoadNode n = currNodeList.get(j);
                                    maxMiniNodeID++;
                                    n.setId(maxMiniNodeID + "-");
                                }
                                RoadWay newWay = new RoadWay(roadID, currNodeList);
                                newWay.setNewRoad(true);
                                newWay.setConfidenceScore(roadWay.getConfidenceScore());
                                rawMap.addWay(newWay);
                                System.out.println("Found removed edge: " + roadID);
                                RoadWay reverseRoad = new RoadWay(roadID.contains("-") ? roadID.substring(roadID.indexOf("-")) : "-" +
                                        roadID);
                                reverseRoad.addNode(newWay.getToNode());
                                for (int j = newWay.getNodes().size() - 2; j > 0; j--) {
                                    maxMiniNodeID++;
                                    RoadNode reverseNode = new RoadNode(maxMiniNodeID + "-", newWay.getNode(j).lon(), newWay.getNode(j)
                                            .lat());
                                    reverseRoad.addNode(reverseNode);
                                }
                                reverseRoad.addNode(newWay.getFromNode());
                                reverseRoad.setNewRoad(true);
                                reverseRoad.setConfidenceScore(roadWay.getConfidenceScore());
                                rawMap.addWay(reverseRoad);
                                currNodeList = new ArrayList<>();
                                startNode = linkedNode;
                                currNodeList.add(linkedNode);
                            } else {
                                // it is a new road connecting two existing intersections
                                maxAbsRoadWayID++;
                                String roadID = maxAbsRoadWayID + "";
                                for (int j = 1; j < currNodeList.size() - 1; j++) {
                                    RoadNode n = currNodeList.get(j);
                                    maxMiniNodeID++;
                                    n.setId(maxMiniNodeID + "-");
                                }
                                RoadWay newWay = new RoadWay(roadID, currNodeList);
                                newWay.setNewRoad(true);
                                newWay.setConfidenceScore(roadWay.getConfidenceScore());
                                rawMap.addWay(newWay);
                                System.out.println("Found removed edge: " + roadID);
                                RoadWay reverseRoad = new RoadWay("-" + roadID);
                                reverseRoad.addNode(newWay.getToNode());
                                for (int j = newWay.getNodes().size() - 2; j > 0; j--) {
                                    maxMiniNodeID++;
                                    RoadNode reverseNode = new RoadNode(maxMiniNodeID + "-", newWay.getNode(j).lon(), newWay.getNode(j)
                                            .lat());
                                    reverseRoad.addNode(reverseNode);
                                }
                                reverseRoad.addNode(newWay.getFromNode());
                                reverseRoad.setNewRoad(true);
                                reverseRoad.setConfidenceScore(roadWay.getConfidenceScore());
                                rawMap.addWay(reverseRoad);
                                currNodeList = new ArrayList<>();
                                startNode = linkedNode;
                                currNodeList.add(linkedNode);
                            }
                        } else {
                            // the new road overlaps with an existing road, restart a new road from the current intersection
                            currNodeList.clear();
                            startNode = linkedNode;
                            currNodeList.add(linkedNode);

                        }
                    } else {
                        if (distFunc.distance(roadWay.getNode(0).toPoint(), linkedNode.toPoint()) < MIN_ROAD_LENGTH) {
                            // the starting piece of the road is shorter than the threshold, remove the starting piece
                            foundConnection = true;
                            currNodeList.clear();
                            startNode = linkedNode;
                            currNodeList.add(linkedNode);
                        } else {
                            // we create a new intersection for the starting piece of the road
                            maxRoadNodeID++;
                            startNode.setId(maxRoadNodeID + "");
                            rawMap.addNode(startNode);
                            // TODO: The new intersection should be included in the intersection search
                            maxAbsRoadWayID++;
                            String roadID = maxAbsRoadWayID + "";
                            for (int j = 1; j < currNodeList.size() - 1; j++) {
                                RoadNode n = currNodeList.get(j);
                                maxMiniNodeID++;
                                n.setId(maxMiniNodeID + "-");
                            }
                            RoadWay newWay = new RoadWay(roadID, currNodeList);
                            newWay.setNewRoad(true);
                            newWay.setConfidenceScore(roadWay.getConfidenceScore());
                            rawMap.addWay(newWay);
                            RoadWay reverseRoad = new RoadWay("-" + maxRoadNodeID);
                            reverseRoad.addNode(newWay.getToNode());
                            for (int j = newWay.getNodes().size() - 2; j > 0; j--) {
                                maxMiniNodeID++;
                                RoadNode reverseNode = new RoadNode(maxMiniNodeID + "-", newWay.getNode(j).lon(), newWay.getNode(j)
                                        .lat());
                                reverseRoad.addNode(reverseNode);
                            }
                            reverseRoad.addNode(newWay.getFromNode());
                            reverseRoad.setNewRoad(true);
                            reverseRoad.setConfidenceScore(roadWay.getConfidenceScore());
                            rawMap.addWay(reverseRoad);
                            currNodeList = new ArrayList<>();
                            startNode = linkedNode;
                            currNodeList.add(linkedNode);
                        }
                    }

                }
            }
            currNodeList.add(roadWay.getNode(i + 1));
        }
        // deal with the remaining road piece
        if (!foundConnection) {
            // the entire road does not pass any existing intersection
            System.out.println("Isolated road found: " + roadWay.getFromNode() + "," + roadWay.getToNode());
            isolatedEdges.add(roadWay);
        } else if (distFunc.distance(startNode.toPoint(), roadWay.getToNode().toPoint()) > MIN_ROAD_LENGTH) {
            // the last piece of road is valid
            maxRoadNodeID++;
            roadWay.getToNode().setId(maxRoadNodeID + "");
            rawMap.addNode(roadWay.getToNode());
            maxAbsRoadWayID++;
            String roadID = maxAbsRoadWayID + "";
            for (int j = 1; j < currNodeList.size() - 1; j++) {
                RoadNode n = currNodeList.get(j);
                maxMiniNodeID++;
                n.setId(maxMiniNodeID + "-");
            }
            RoadWay newWay = new RoadWay(roadID, currNodeList);
            newWay.setNewRoad(true);
            newWay.setConfidenceScore(roadWay.getConfidenceScore());
            rawMap.addWay(newWay);
            RoadWay reverseRoad = new RoadWay("-" + maxRoadNodeID);
            reverseRoad.addNode(newWay.getToNode());
            for (int j = newWay.getNodes().size() - 2; j > 0; j--) {
                maxMiniNodeID++;
                RoadNode reverseNode = new RoadNode(maxMiniNodeID + "-", newWay.getNode(j).lon(), newWay.getNode(j).lat());
                reverseRoad.addNode(reverseNode);
            }
            reverseRoad.addNode(newWay.getFromNode());
            reverseRoad.setNewRoad(true);
            reverseRoad.setConfidenceScore(roadWay.getConfidenceScore());
            rawMap.addWay(reverseRoad);
        }
    }

    private List<Point> edgeSegmentation(RoadNode firstNode, RoadNode secondNode) {
        DecimalFormat df = new DecimalFormat("0.00000");
        double distance = distFunc.distance(firstNode.toPoint(), secondNode.toPoint());
        List<Point> nodeList = new ArrayList<>();
        if (distance > subTrajectoryMergeDistance * 2) {
            int tempPointCount = (int) Math.ceil(distance / (subTrajectoryMergeDistance * 2)) - 1;
            double lonDiff = (secondNode.toPoint().x() - firstNode.toPoint().x()) / tempPointCount;
            double latDiff = (secondNode.toPoint().y() - firstNode.toPoint().y()) / tempPointCount;
            for (int i = 1; i <= tempPointCount; i++) {
                Point newPoint = new Point(Double.parseDouble(df.format(firstNode.toPoint().x() + lonDiff * i)), Double.parseDouble(df.format
                        (firstNode.toPoint().y() + latDiff * i)));
                nodeList.add(newPoint);
            }
        }
        return nodeList;
    }


    private RoadWay roadMapConnection(RoadWay candidateRoadWay, Point startPoint, Point endPoint) {
        // there must not be an existing road directly connecting the given points, otherwise, the inferred road is useless
        if (!loc2RoadWayMapping.containsKey(startPoint.x() + "_" + startPoint.y() + "," + endPoint.x() + "_" + endPoint.y())) {
            if (point2RoadNodeMapping.containsKey(startPoint) && point2RoadNodeMapping.containsKey(endPoint)) {
                List<RoadNode> refinedWay = new ArrayList<>();
                refinedWay.add(point2RoadNodeMapping.get(startPoint));
                refinedWay.addAll(candidateRoadWay.getNodes());
                refinedWay.add(point2RoadNodeMapping.get(endPoint));
                return new RoadWay(candidateRoadWay.getId(), refinedWay);
            } else
                System.out.println("ERROR! At least one of the end points of the inferred road is not found in the raw map.");
        } else
            System.out.println("ERROR! The inferred road exists: " + loc2RoadWayMapping.get(startPoint.x() + "_" + startPoint.y() + "," +
                    endPoint.x() + "_" + endPoint.y()).toString());
        return null;
    }


    private void buildGridIndex() {
        // calculate the grid settings
        int cellNum;    // total number of cells
        int rowNum;     // number of rows and columns
        int nodeCount = rawMap.getNodes().size();
//        Set<String> nodeLocationList = new HashSet<>();
//        for (RoadWay w : rawMap.getWays()) {
//            for (RoadNode n : w.getNodes()) {
//                if (!nodeLocationList.contains(n.lon() + "_" + n.lat())) {
//                    nodeLocationList.add(n.lon() + "_" + n.lat());
//                    nodeCount++;
//                } else {
//                    System.out.println("Duplicated road nodes in nearest neighbour network index");
//                }
//            }
//        }
        cellNum = nodeCount / avgNodePerGrid;
        rowNum = (int) Math.ceil(Math.sqrt(cellNum));
        this.grid = new Grid<>(rowNum, rowNum, rawMap.getMinLon(), rawMap.getMinLat(), rawMap.getMaxLon(), rawMap.getMaxLat());

        for (RoadNode n : rawMap.getNodes()) {
            Point nodeIndex = new Point(n.lon(), n.lat());
            nodeIndex.setId(n.getId());
            XYObject<Point> nodeIndexObject = new XYObject<>(nodeIndex.x(), nodeIndex.y(), nodeIndex);
            this.grid.insert(nodeIndexObject);
            this.point2RoadNodeMapping.put(nodeIndex, n);
        }

        System.out.println("Total number of nodes in grid index:" + rawMap.getNodes().size());
        System.out.println("The grid contains " + rowNum + " rows and columns");
    }
}