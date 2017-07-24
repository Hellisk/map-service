package edu.uq.dke.mapupdate.io;

/**
 * Created by uqpchao on 4/07/2017.
 */

import edu.uq.dke.mapupdate.mapmatching.io.PointWithSegment;
import traminer.util.map.roadnetwork.RoadNetworkGraph;
import traminer.util.map.roadnetwork.RoadNode;
import traminer.util.map.roadnetwork.RoadWay;
import traminer.util.spatial.SpatialInterface;
import traminer.util.spatial.objects.Point;
import traminer.util.spatial.objects.Segment;
import traminer.util.spatial.objects.XYObject;
import traminer.util.spatial.structures.grid.Grid;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;

/**
 * Created by uqpchao on 22/05/2017.
 */
public class CSVMapWriter implements SpatialInterface {
    private final RoadNetworkGraph roadGraph;
    private final String csvVerticesPath;
    private final String csvEdgesPath;
    private final String csvRemovedEdgesPath;


    public CSVMapWriter(RoadNetworkGraph roadNetworkGraph, final String csvVertexPath, final String csvEdgePath) {
        this.roadGraph = roadNetworkGraph;
        this.csvVerticesPath = csvVertexPath;
        this.csvEdgesPath = csvEdgePath;
        this.csvRemovedEdgesPath = "";
    }

    public CSVMapWriter(RoadNetworkGraph roadNetworkGraph, final String csvVertexPath, final String csvEdgePath, final String csvRemovedEdgesPath) {
        this.roadGraph = roadNetworkGraph;
        this.csvVerticesPath = csvVertexPath;
        this.csvEdgesPath = csvEdgePath;
        this.csvRemovedEdgesPath = csvRemovedEdgesPath;
    }

    public void writeShapeCSV() throws IOException {

        DecimalFormat df = new DecimalFormat(".00000");

        // create directories before writing
        File file = new File(csvVerticesPath.substring(0, csvVerticesPath.lastIndexOf('/')));
        if (!file.exists()) {
            file.mkdirs();
        }
        // write vertex file
        BufferedWriter bwVertices = new BufferedWriter(new FileWriter(csvVerticesPath));
        for (RoadNode n : roadGraph.getNodes()) {
            bwVertices.write(n.getId() + "," + df.format(n.lon()) + "," + df.format(n.lat()) + "\n");
        }
        bwVertices.close();
        // write road way file
        BufferedWriter bwEdges = new BufferedWriter(new FileWriter(csvEdgesPath));
        for (RoadWay w : roadGraph.getWays()) {
            bwEdges.write(w.getId());
            for (RoadNode n : w.getNodes()) {
                bwEdges.write("|" + n.getId() + "," + df.format(n.lon()) + "," + df.format(n.lat()));
            }
            bwEdges.write("\n");
        }

        bwEdges.close();
    }

    public void areaBasedMapManipulation(int percentage) throws IOException {

        // build grid index for point and segment search
        int cellNum;    // total number of cells
        int rowNum;     // number of rows and columns
        int avgNodePerGrid = 128;    // average road nodes in one grid cell, default 64

        int nodeCount = this.roadGraph.getNodes().size();
        Set<String> nodeLocationList = new HashSet<>();
        for (RoadWay w : this.roadGraph.getWays()) {
            for (RoadNode n : w.getNodes()) {
                if (!nodeLocationList.contains(n.lon() + "_" + n.lat())) {
                    nodeLocationList.add(n.lon() + "_" + n.lat());
                    nodeCount++;
                }
            }
        }
        cellNum = nodeCount / avgNodePerGrid;
        rowNum = (int) Math.ceil(Math.sqrt(cellNum));
        Grid<PointWithSegment> mapGridIndex = new Grid<>(rowNum, rowNum, this.roadGraph.getMinLon(), this.roadGraph.getMinLat(), this.roadGraph.getMaxLon(), this.roadGraph.getMaxLat());

        System.out.println("Total number of nodes in manipulation map grid index:" + this.roadGraph.getNodes().size());
        System.out.println("The grid contains " + rowNum + "rows and columns");

        // add all map nodes and edges into a hash map, both incoming and outgoing segments are included
        Map<String, List<Segment>> adjacentList = new HashMap<>();  // for every node (both road node and intermediate node), all segments that connect it
        Map<String, Integer> roadWayNodeCount = new HashMap<>();    // for every road way, the number of intermediate nodes

        for (RoadNode p : this.roadGraph.getNodes()) {
            adjacentList.put(p.lon() + "_" + p.lat(), new ArrayList<>());
        }

        for (RoadWay t : this.roadGraph.getWays()) {
            roadWayNodeCount.put(t.getId(), t.getNodes().size() - 2);
            for (int i = 0; i < t.getNodes().size() - 1; i++) {
                if (!adjacentList.containsKey(t.getNode(i).lon() + "_" + t.getNode(i).lat())) {
                    adjacentList.put(t.getNode(i).lon() + "_" + t.getNode(i).lat(), new ArrayList<>());
                } else {
                    // double direction road way will have overlapped nodes
                    Segment newSegment = t.getEdges().get(i);
                    newSegment.setId(t.getId());
                    adjacentList.get(t.getNode(i).lon() + "_" + t.getNode(i).lat()).add(newSegment);
                }
                if (!adjacentList.containsKey(t.getNode(i + 1).lon() + "_" + t.getNode(i + 1).lat())) {
                    adjacentList.put(t.getNode(i + 1).lon() + "_" + t.getNode(i + 1).lat(), new ArrayList<>());
                } else {
                    // double direction road way will have overlapped nodes
                    Segment newSegment = t.getEdges().get(i);
                    newSegment.setId(t.getId());
                    adjacentList.get(t.getNode(i + 1).lon() + "_" + t.getNode(i + 1).lat()).add(newSegment);
                }
            }
        }

        int pointCount = 0;
        List<Point> pointList = new ArrayList<>();

        for (Map.Entry<String, List<Segment>> entry : adjacentList.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                String[] coordinate = entry.getKey().split("_");
                PointWithSegment newPoint = new PointWithSegment(Double.parseDouble(coordinate[0]), Double.parseDouble(coordinate[1]), entry.getValue());
                pointList.add(newPoint.toPoint());
                XYObject<PointWithSegment> point = new XYObject<>(newPoint.x(), newPoint.y(), newPoint);
                mapGridIndex.insert(point);
                pointCount++;
            }
        }
        System.out.println("Grid index build successfully, total number of points:" + pointCount);

        // generate removed road way list
        int removedPointCount = 0;
        Set<String> removedRoadIDList = new HashSet<>();
        Set<String> removedPointList = new HashSet<>();
        Random random = new Random(1);
        while (removedPointCount < (pointCount * ((double) percentage / 100))) {
            int nextPointPosition = random.nextInt(pointCount);
            Point candidatePoint = pointList.get(nextPointPosition);
            if (!removedPointList.contains(candidatePoint.x() + "_" + candidatePoint.y())) {
                List<XYObject<PointWithSegment>> nearbyPoints = mapGridIndex.partitionSearch(candidatePoint.x(), candidatePoint.y()).getObjectsList();
                for (XYObject<PointWithSegment> point : nearbyPoints) {
                    for (Segment s : point.getSpatialObject().getAdjacentSegments()) {
                        if (!removedRoadIDList.contains(s.getId())) {
                            removedRoadIDList.add(s.getId());
                            removedPointCount += roadWayNodeCount.get(s.getId());
                        }
                    }
                }
                removedPointList.add(candidatePoint.x() + "_" + candidatePoint.y());
            }
        }

        // start writing new file
        // create directories before writing
        File file = new File(csvVerticesPath.substring(0, csvVerticesPath.lastIndexOf('/')));
        if (!file.exists()) {
            file.mkdirs();
        }
        // write vertex file
        BufferedWriter bwVertices = new BufferedWriter(new FileWriter(csvVerticesPath));
        for (RoadNode n : roadGraph.getNodes()) {
            bwVertices.write(n.getId() + "," + n.lon() + "," + n.lat() + "\n");
        }
        bwVertices.close();
        // write road way file
        BufferedWriter bwEdges = new BufferedWriter(new FileWriter(csvEdgesPath));
        BufferedWriter bwRemovedEdges = new BufferedWriter(new FileWriter(csvRemovedEdgesPath));
        for (RoadWay w : roadGraph.getWays()) {
            if (!removedRoadIDList.contains(w.getId())) {
                bwEdges.write(w.getId());
                for (RoadNode n : w.getNodes()) {
                    bwEdges.write("|" + n.getId() + "," + n.lon() + "," + n.lat());
                }
                bwEdges.write("\n");
            } else {
                bwRemovedEdges.write(w.getId());
                for (RoadNode n : w.getNodes()) {
                    bwRemovedEdges.write("|" + n.getId() + "," + n.lon() + "," + n.lat());
                }
                bwRemovedEdges.write("\n");
            }
        }
        bwEdges.close();
        bwRemovedEdges.close();

        System.out.println("Total removed road ways:" + removedRoadIDList.size());
        System.out.println("Total road ways:" + roadWayNodeCount.size());
    }

    public void randomBasedMapManipulation(int percentage) throws IOException {
        // create directories before writing
        File file = new File(csvVerticesPath.substring(0, csvVerticesPath.lastIndexOf('/')));
        if (!file.exists()) {
            file.mkdirs();
        }
        // write vertex file
        BufferedWriter bwVertices = new BufferedWriter(new FileWriter(csvVerticesPath));
        for (RoadNode n : roadGraph.getNodes()) {
            bwVertices.write(n.getId() + "," + n.lon() + "," + n.lat() + "\n");
        }
        bwVertices.close();
        // write road way file
        BufferedWriter bwEdges = new BufferedWriter(new FileWriter(csvEdgesPath));
        BufferedWriter bwRemovedEdges = new BufferedWriter(new FileWriter(csvRemovedEdgesPath));
        Random random = new Random(1);
        for (RoadWay w : roadGraph.getWays()) {
            if (random.nextInt(100) >= percentage) {
                bwEdges.write(w.getId());
                for (RoadNode n : w.getNodes()) {
                    bwEdges.write("|" + n.getId() + "," + n.lon() + "," + n.lat());
                }
                bwEdges.write("\n");
            } else {
                bwRemovedEdges.write(w.getId());
                for (RoadNode n : w.getNodes()) {
                    bwRemovedEdges.write("|" + n.getId() + "," + n.lon() + "," + n.lat());
                }
                bwRemovedEdges.write("\n");
            }
        }
        bwEdges.close();
        bwRemovedEdges.close();
    }

    public static RoadNetworkGraph purifyMap(List<RoadWay> inputMatchedTrajList, RoadNetworkGraph inputRoadMap) {
        Map<String, Boolean> roadIDCheckList = new HashMap<>();
        Map<String, Integer> nodeDegreeList = new HashMap<>();
        Map<String, List<RoadNode>> roadIDEndPointMapping = new HashMap<>();

        System.out.println("Current road node:" + inputRoadMap.getNodes().size() + ", road way:" + inputRoadMap.getWays().size());

        // update the road node degree for future node removal
        for (RoadNode n : inputRoadMap.getNodes()) {
            nodeDegreeList.put(n.lon() + "_" + n.lat(), 0);
        }
        for (RoadWay w : inputRoadMap.getWays()) {
            roadIDCheckList.put(w.getId(), false);
            List<RoadNode> endPointList = new ArrayList<>();
            RoadNode startNode = w.getNode(0);
            RoadNode endNode = w.getNode(w.getNodes().size() - 1);
            endPointList.add(startNode);
            endPointList.add(endNode);
            roadIDEndPointMapping.put(w.getId(), endPointList);
            int startNodeDegree = nodeDegreeList.get(startNode.lon() + "_" + startNode.lat());
            int endNodeDegree = nodeDegreeList.get(endNode.lon() + "_" + endNode.lat());
            nodeDegreeList.replace(startNode.lon() + "_" + startNode.lat(), startNodeDegree + 1);
            nodeDegreeList.replace(endNode.lon() + "_" + endNode.lat(), endNodeDegree + 1);
        }

        // scan the match result and update the coverage
        for (RoadWay w : inputMatchedTrajList) {
            for (RoadNode n : w.getNodes()) {
                if (!roadIDCheckList.get(n.getId())) {
                    roadIDCheckList.replace(n.getId(), true);
                }
            }
        }
        Set<String> removedRoadWayList = new HashSet<>();
        Set<String> removedRoadNodeList = new HashSet<>();

        for (Map.Entry<String, Boolean> e : roadIDCheckList.entrySet()) {
            if (!e.getValue()) {
                String roadID = e.getKey();
                removedRoadWayList.add(roadID);
                List<RoadNode> endPointList = roadIDEndPointMapping.get(roadID);
                int startNodeDegree = nodeDegreeList.get(endPointList.get(0).lon() + "_" + endPointList.get(0).lat());
                int endNodeDegree = nodeDegreeList.get(endPointList.get(1).lon() + "_" + endPointList.get(1).lat());
                if (startNodeDegree - 1 <= 0) {
                    nodeDegreeList.remove(endPointList.get(0).lon() + "_" + endPointList.get(0).lat());
                    removedRoadNodeList.add(endPointList.get(0).lon() + "_" + endPointList.get(0).lat());
                } else {
                    nodeDegreeList.replace(endPointList.get(0).lon() + "_" + endPointList.get(0).lat(), startNodeDegree - 1);
                }
                if (endNodeDegree - 1 <= 0) {
                    nodeDegreeList.remove(endPointList.get(1).lon() + "_" + endPointList.get(1).lat());
                    removedRoadNodeList.add(endPointList.get(1).lon() + "_" + endPointList.get(1).lat());
                } else {
                    nodeDegreeList.replace(endPointList.get(1).lon() + "_" + endPointList.get(1).lat(), endNodeDegree - 1);
                }
            }
        }
//        // remove all untouched nodes
//        for(String s: removedRoadNodeList){
//            roadIDCheckList.remove(s);
//        }

        System.out.println("Removed road node:" + removedRoadNodeList.size() + ", road way:" + removedRoadWayList.size());

        // eliminate the road nodes that have no edges
        int nodeRemoveCount = 0;
        List<RoadNode> zeroDegreeRoadNodeList = new ArrayList<>();
        for (RoadNode n : inputRoadMap.getNodes()) {
            if (n.getDegree() == 0) {
                zeroDegreeRoadNodeList.add(n);
                nodeRemoveCount++;
            }
        }
        for (RoadNode n : zeroDegreeRoadNodeList) {
            inputRoadMap.getNodes().remove(n);
        }

        System.out.println("nodeRemoveCount = " + nodeRemoveCount);

        // modify the current road map
        List<RoadNode> newRoadNode = new ArrayList<>();
        List<RoadWay> newRoadWay = new ArrayList<>();

        for (RoadNode n : inputRoadMap.getNodes()) {
            if (!removedRoadNodeList.contains(n.lon() + "_" + n.lat())) {
                newRoadNode.add(n);
            }
        }
        for (RoadWay w : inputRoadMap.getWays()) {
            if (!removedRoadWayList.contains(w.getId())) {
                newRoadWay.add(w);
            }
        }
        RoadNetworkGraph filteredNetwork = new RoadNetworkGraph();
        filteredNetwork.setMaxLon(inputRoadMap.getMaxLon());
        filteredNetwork.setMinLon(inputRoadMap.getMinLon());
        filteredNetwork.setMaxLat(inputRoadMap.getMaxLat());
        filteredNetwork.setMinLat(inputRoadMap.getMinLat());
        filteredNetwork.addNodes(newRoadNode);
        filteredNetwork.addWays(newRoadWay);

        return filteredNetwork;
    }
}

