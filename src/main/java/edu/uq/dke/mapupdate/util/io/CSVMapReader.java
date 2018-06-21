package edu.uq.dke.mapupdate.util.io;

import edu.uq.dke.mapupdate.util.object.SpatialInterface;
import edu.uq.dke.mapupdate.util.object.roadnetwork.RoadNetworkGraph;
import edu.uq.dke.mapupdate.util.object.roadnetwork.RoadNode;
import edu.uq.dke.mapupdate.util.object.roadnetwork.RoadWay;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Created by uqpchao on 22/05/2017.
 */
public class CSVMapReader implements SpatialInterface {
    private RoadNetworkGraph roadGraph;
    private String csvMapPath;

    public CSVMapReader(String csvPath) {
        this.roadGraph = new RoadNetworkGraph();
        this.csvMapPath = csvPath;
    }

    /**
     * Read and parse the CSV file.
     *
     * @return A road network graph containing the road nodes and road ways in the CSV file.
     * @throws IOException file not found
     */
    public RoadNetworkGraph readMap(int percentage) throws IOException {

        List<RoadNode> nodes = new ArrayList<>();
        List<RoadWay> ways = new ArrayList<>();
        Map<String, RoadNode> index2Node = new HashMap<>();       // maintain a mapping of road location to node index
        // read road nodes
        String line;
        BufferedReader brVertices = new BufferedReader(new FileReader(this.csvMapPath + "vertices_" + percentage + ".txt"));
        while ((line = brVertices.readLine()) != null) {
            RoadNode newNode = RoadNode.parseRoadNode(line);
            nodes.add(newNode);
            index2Node.put(newNode.getId(), newNode);
        }
        brVertices.close();

        // read road ways
        BufferedReader brEdges = new BufferedReader(new FileReader(this.csvMapPath + "edges_" + percentage + ".txt"));
        while ((line = brEdges.readLine()) != null) {
            RoadWay newWay = RoadWay.parseRoadWay(line, index2Node);
            ways.add(newWay);
        }
        brEdges.close();

        roadGraph.addNodes(nodes);
        roadGraph.addWays(ways);
        int removedNodeCount = roadGraph.isolatedNodeRemoval();
        System.out.println("Read " + percentage + "% road map, isolate nodes:" + removedNodeCount + ", total nodes:" + nodes.size() + ", total roads:" +
                ways.size());
        return roadGraph;
    }

    /**
     * Read the ground truth map and extract the sub graph enclosed by the bounding box.
     *
     * @param boundingBox The specified bounding box, same as readMap(0) when the bounding box is empty
     * @return The road network graph enclosed by the given bounding box
     * @throws IOException file not found
     */
    public RoadNetworkGraph extractMapWithBoundary(double[] boundingBox) throws IOException {

        if (boundingBox.length != 4)
            return this.readMap(0);
        this.roadGraph.setBoundingBox(boundingBox[0], boundingBox[1], boundingBox[2], boundingBox[3]);

        List<RoadNode> nodeList = new ArrayList<>();
        List<RoadWay> wayList = new ArrayList<>();
        Map<String, RoadNode> index2Node = new HashMap<>();       // maintain a mapping of road location to node index
        // read road nodes
        String line;
        BufferedReader brVertices = new BufferedReader(new FileReader(this.csvMapPath + "vertices_0.txt"));
        while ((line = brVertices.readLine()) != null) {
            RoadNode currNode = RoadNode.parseRoadNode(line);
            if (isInside(currNode.lon(), currNode.lat(), roadGraph)) {
                nodeList.add(currNode);
                index2Node.put(currNode.getId(), currNode);
            }
        }
        brVertices.close();

        // read road ways
        BufferedReader brEdges = new BufferedReader(new FileReader(this.csvMapPath + "edges_0.txt"));
        while ((line = brEdges.readLine()) != null) {
            RoadWay newWay = RoadWay.parseRoadWay(line, index2Node);
            if (!newWay.isEmpty()) {
                wayList.add(newWay);
            }
        }
        brEdges.close();

        roadGraph.addNodes(nodeList);
        roadGraph.addWays(wayList);
        int removedNodeCount = roadGraph.isolatedNodeRemoval();
        System.out.println("Extract road map complete, isolate nodes:" + removedNodeCount + ", total nodes:" + nodeList.size() + ", total" +
                " roads:" + wayList.size());
        return roadGraph;
    }

    public List<RoadWay> readMapEdgeByLevel(int percentage, int level) throws IOException {
        List<RoadWay> wayList = new ArrayList<>();
        // read road ways
        BufferedReader brEdges = new BufferedReader(new FileReader(this.csvMapPath + "edges_" + percentage + ".txt"));
        String line;
        while ((line = brEdges.readLine()) != null) {
            RoadWay newWay = RoadWay.parseRoadWay(line, new HashMap<>());
            if (newWay.getRoadWayLevel() == level)
                wayList.add(newWay);
        }
        brEdges.close();
        return wayList;
    }

    public List<RoadWay> readMapEdgeByType(int percentage, int typeIndex) throws IOException {
        List<RoadWay> wayList = new ArrayList<>();
        // read road ways
        BufferedReader brEdges = new BufferedReader(new FileReader(this.csvMapPath + "edges_" + percentage + ".txt"));
        String line;
        while ((line = brEdges.readLine()) != null) {
            RoadWay newWay = RoadWay.parseRoadWay(line, new HashMap<>());
            if (newWay.getRoadWayType().get(typeIndex))
                wayList.add(newWay);
        }
        brEdges.close();
        return wayList;
    }

    public List<RoadWay> readMapEdgeByTypeSet(int percentage, BitSet typeIndex) throws IOException {
        if (typeIndex.size() != 25)
            throw new IllegalArgumentException("ERROR! Failed to read edges through type set: Incorrect type set length." + typeIndex
                    .size());
        List<RoadWay> wayList = new ArrayList<>();
        // read road ways
        BufferedReader brEdges = new BufferedReader(new FileReader(this.csvMapPath + "edges_" + percentage + ".txt"));
        String line;
        while ((line = brEdges.readLine()) != null) {
            RoadWay newWay = RoadWay.parseRoadWay(line, new HashMap<>());
            boolean isContained = true;
            for (int i = 0; i < typeIndex.size(); i++) {
                if (typeIndex.get(i))
                    if (!newWay.getRoadWayType().get(i))
                        isContained = false;
            }
            if (isContained)
                wayList.add(newWay);
        }
        brEdges.close();
        return wayList;
    }

    public List<RoadWay> readRemovedEdges(int percentage) throws IOException {
        if (percentage == 0)    // no removed edge when percentage is 0
            return new ArrayList<>();
        List<RoadWay> removedRoads = new ArrayList<>();
        String line;
        // read removed road ways
        BufferedReader brEdges = new BufferedReader(new FileReader(this.csvMapPath + "removedEdges_" + percentage + ".txt"));
        while ((line = brEdges.readLine()) != null) {
            RoadWay newWay = RoadWay.parseRoadWay(line, new HashMap<>());
            removedRoads.add(newWay);
        }
        return removedRoads;
    }

    public List<RoadWay> readInferredEdges() throws IOException {
        List<RoadWay> inferredRoads = new ArrayList<>();
        String line;
        // read inferred road ways
        BufferedReader brEdges = new BufferedReader(new FileReader(this.csvMapPath + "final_map.txt"));
        while ((line = brEdges.readLine()) != null) {
            RoadWay newWay = new RoadWay();
            List<RoadNode> miniNode = new ArrayList<>();
            String[] edgeInfo = line.split("\\|");
            if (!edgeInfo[0].equals("null"))
                newWay.setId(edgeInfo[0]);
            if (edgeInfo[1].contains(",")) {  // confidence score is set
                newWay.setConfidenceScore(Double.parseDouble(edgeInfo[1].split(",")[1]));
            }
            for (int i = 2; i < edgeInfo.length; i++) {
                String[] roadWayPoint = edgeInfo[i].split(",");
                if (roadWayPoint.length == 2) { // inferred edges do not have id
                    RoadNode newNode = new RoadNode(null, Double.parseDouble(roadWayPoint[0]), Double.parseDouble(roadWayPoint[1]));
                    miniNode.add(newNode);
                } else if (roadWayPoint.length == 3) {
                    RoadNode newNode = new RoadNode(roadWayPoint[0], Double.parseDouble(roadWayPoint[1]), Double.parseDouble
                            (roadWayPoint[2]));
                    miniNode.add(newNode);
                } else throw new InvalidPropertiesFormatException("Wrong road way node in inferred map:" + roadWayPoint.length);
            }
            newWay.setNodes(miniNode);
            inferredRoads.add(newWay);
        }
        return inferredRoads;
    }

    private boolean isInside(double pointX, double pointY, RoadNetworkGraph roadGraph) {
        if (roadGraph.hasBoundary())
            return pointX >= roadGraph.getMinLon() && pointX <= roadGraph.getMaxLon() && pointY >= roadGraph.getMinLat() && pointY <=
                    roadGraph.getMaxLat();
        else return true;
    }
}