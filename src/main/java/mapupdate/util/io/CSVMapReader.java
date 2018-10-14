package mapupdate.util.io;

import mapupdate.util.object.SpatialInterface;
import mapupdate.util.object.roadnetwork.RoadNetworkGraph;
import mapupdate.util.object.roadnetwork.RoadNode;
import mapupdate.util.object.roadnetwork.RoadWay;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Created by uqpchao on 22/05/2017.
 */
public class CSVMapReader implements SpatialInterface {
    private String csvMapPath;

    public CSVMapReader(String csvPath) {
        this.csvMapPath = csvPath;
    }

    /**
     * Read and parse the CSV file.
     *
     * @return A road network graph containing the road nodes and road ways in the CSV file.
     * @throws IOException file not found
     */
    public RoadNetworkGraph readMap(int percentage, int iteration, boolean isTempMap) throws IOException {

        RoadNetworkGraph roadGraph = new RoadNetworkGraph();
        List<RoadNode> nodes = new ArrayList<>();
        List<RoadWay> ways = new ArrayList<>();
        Map<String, RoadNode> index2Node = new HashMap<>();       // maintain a mapping of road location to node index
        // read road nodes
        String line;
        String inputPath;
        if (iteration != -1)
            inputPath = this.csvMapPath + "map/" + iteration + "/";
        else inputPath = this.csvMapPath;

        BufferedReader brVertices;
        if (isTempMap)
            brVertices = new BufferedReader(new FileReader(inputPath + "temp_vertices_" + percentage + ".txt"));
        else brVertices = new BufferedReader(new FileReader(inputPath + "vertices_" + percentage + ".txt"));
        while ((line = brVertices.readLine()) != null) {
            RoadNode newNode = RoadNode.parseRoadNode(line);
            nodes.add(newNode);
            index2Node.put(newNode.getID(), newNode);
        }
        brVertices.close();

        // read road ways
        BufferedReader brEdges;
        if (isTempMap)
            brEdges = new BufferedReader(new FileReader(inputPath + "temp_edges_" + percentage + ".txt"));
        else brEdges = new BufferedReader(new FileReader(inputPath + "edges_" + percentage + ".txt"));
        while ((line = brEdges.readLine()) != null) {
            RoadWay newWay = RoadWay.parseRoadWay(line, index2Node);
            ways.add(newWay);
        }
        brEdges.close();

        roadGraph.addNodes(nodes);
        roadGraph.addWays(ways);
        int removedNodeCount = roadGraph.isolatedNodeRemoval();
//        System.out.println("Read " + percentage + "% road map, isolate nodes:" + removedNodeCount + ", total nodes:" + nodes.size() + ", total roads:" +
//                ways.size());
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

        RoadNetworkGraph roadGraph = new RoadNetworkGraph();
        if (boundingBox.length != 4)
            return this.readMap(0, -1, false);
        roadGraph.setBoundingBox(boundingBox[0], boundingBox[1], boundingBox[2], boundingBox[3]);

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
                index2Node.put(currNode.getID(), currNode);
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
//        System.out.println("Extract road map complete, isolate nodes:" + removedNodeCount + ", total nodes:" + roadGraph.getNodes().size() +
//                ", total" + " roads:" + roadGraph.getWays().size());
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

    public List<RoadWay> readNewMapEdge(int percentage, int iteration, boolean isTempMap) throws IOException {
        List<RoadWay> wayList = new ArrayList<>();
        String inputPath;
        if (iteration != -1)
            inputPath = this.csvMapPath + "map/" + iteration + "/";
        else inputPath = this.csvMapPath;
        // read road ways
        BufferedReader brEdges;
        if (isTempMap)
            brEdges = new BufferedReader(new FileReader(inputPath + "temp_edges_" + percentage + ".txt"));
        else brEdges = new BufferedReader(new FileReader(inputPath + "edges_" + percentage + ".txt"));
        String line;
        while ((line = brEdges.readLine()) != null) {
            RoadWay newWay = RoadWay.parseRoadWay(line, new HashMap<>());
            if (newWay.isNewRoad())
                wayList.add(newWay);
        }
        brEdges.close();
        return wayList;
    }

    public List<RoadWay> readRemovedEdges(int percentage, int iteration) throws IOException {
        HashSet<String> removedRoadIdSet = new HashSet<>();
        if (percentage == 0)    // no removed edge when percentage is 0
            return new ArrayList<>();
        List<RoadWay> removedRoads = new ArrayList<>();
        String line;
        // read removed road ways
        if (iteration != -1)
            System.out.println("ERROR! Removed road ways is only read outside the iteration.");
        BufferedReader brEdges = new BufferedReader(new FileReader(this.csvMapPath + "removedEdges_" + percentage + ".txt"));
        while ((line = brEdges.readLine()) != null) {
            RoadWay newWay = RoadWay.parseRoadWay(line, new HashMap<>());
            if (!removedRoadIdSet.contains(newWay.getID())) {
                removedRoadIdSet.add(newWay.getID());
                removedRoads.add(newWay);
            } else
                System.out.println("ERROR! Duplicated removed road.");
        }
        return removedRoads;
    }

    public List<RoadWay> readInferredEdges() throws IOException {
        List<RoadWay> inferredRoads = new ArrayList<>();
        // read inferred road ways
        File inferenceFile = new File(this.csvMapPath + "inferred_edges.txt");
        if (!inferenceFile.exists())
            return inferredRoads;
        BufferedReader brEdges = new BufferedReader(new FileReader(this.csvMapPath + "inferred_edges.txt"));
        String line;
        while ((line = brEdges.readLine()) != null) {
            RoadWay newWay = RoadWay.parseRoadWay(line, new HashMap<>());
            newWay.setId("temp_" + newWay.getID());
            newWay.setNewRoad(true);
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