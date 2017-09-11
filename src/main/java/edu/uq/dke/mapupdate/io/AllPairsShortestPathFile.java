package edu.uq.dke.mapupdate.io;

import traminer.util.map.roadnetwork.RoadNetworkGraph;
import traminer.util.map.roadnetwork.RoadNode;
import traminer.util.map.roadnetwork.RoadWay;
import traminer.util.spatial.distance.GPSDistanceFunction;
import traminer.util.spatial.objects.Segment;

import java.io.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

/**
 * shortest path file reader and writer
 * <p>
 * Created by uqpchao on 28/06/2017.
 */
public class AllPairsShortestPathFile {
    private HashMap<String, Integer> vertexIDMap = new HashMap<>();
    //    private HashMap<Integer, RoadNode> idNodeMap = new HashMap<>();
    private HashMap<String, RoadWay> idPairRoadMap = new HashMap<>();
    private HashMap<String, RoadWay> segmentRoadMap = new HashMap<>();
    private GPSDistanceFunction distanceFunction = new GPSDistanceFunction();
    private int matrixSize = 0;
    private int[][][] shortestPathMatrix;
    private double[][] distanceMatrix;
    private int iteration = 0;
    private RoadNetworkGraph roadNetworkGraph;

    public AllPairsShortestPathFile(RoadNetworkGraph roadNetworkGraph) {
        this.roadNetworkGraph = isolatedNodeRemoval(roadNetworkGraph);
        matrixSize = roadNodeRegistration();
        System.out.println("Matrix size is:" + matrixSize);
        shortestPathMatrix = new int[matrixSize][matrixSize][2];
        distanceMatrix = new double[matrixSize][matrixSize];
        System.out.println("Matrices space created");
    }

    /**
     * input a road network and generate an all pair file from it
     *
     * @param roadNetworkGraph the current road network file
     * @param iteration        the number of current iteration, used in creating the folder
     */
    public AllPairsShortestPathFile(RoadNetworkGraph roadNetworkGraph, int iteration) {
        this.roadNetworkGraph = isolatedNodeRemoval(roadNetworkGraph);
        this.iteration = iteration;
        matrixSize = roadNodeRegistration();
        System.out.println("Matrix size is:" + matrixSize);
        shortestPathMatrix = new int[matrixSize][matrixSize][2];
        distanceMatrix = new double[matrixSize][matrixSize];
        System.out.println("Matrices space created");
    }

    /**
     * calculate all-pair shortest paths and store them into files
     *
     * @param outputFilePath folder of output shortest path file and shortest distance file
     * @throws IOException BufferedWriter exception
     */
    public void writeShortestPathFiles(String outputFilePath) throws IOException {
        // initialize shortest path matrices
        matrixInit();

        // Calculate all pairs shortest paths
        floydWarshallUpdate();

        // shortest path output
        System.out.println("Start writing shortest path files.");
        File outputFolder = new File(outputFilePath);
        if (!outputFolder.exists()) {
            outputFolder.mkdirs();
        }
        BufferedWriter shortestPathWriter;
        BufferedWriter shortestDistanceWriter;
        shortestPathWriter = new BufferedWriter(new FileWriter(outputFilePath + this.iteration + "_shortestPaths.txt"));
        shortestDistanceWriter = new BufferedWriter(new FileWriter(outputFilePath + this.iteration + "_shortestDistances.txt"));
        int infinityCount = 0;
        DecimalFormat df = new DecimalFormat("0.0000");
        for (int i = 0; i < matrixSize; i++) {
            for (int j = 0; j < matrixSize; j++) {
                if (distanceMatrix[i][j] == Double.POSITIVE_INFINITY) {
                    infinityCount++;
                }
                shortestDistanceWriter.write(i + "," + j + "," + df.format(distanceMatrix[i][j]) + "\n");
                shortestPathWriter.write(i + "," + j + "," + shortestPathMatrix[i][j][0] + "," + shortestPathMatrix[i][j][1] + "\n");
            }
        }
        System.out.println("Number of infinity value in iteration " + this.iteration + ":" + infinityCount);
        shortestPathWriter.close();
        shortestDistanceWriter.close();
    }

    /**
     * read shortest path information from existing shortest path files
     *
     * @param inputFilePath folder of shortest path files
     * @throws IOException BufferedReader exception
     */
    public void readShortestPathFiles(String inputFilePath) throws IOException {
        // register the road ways into the hash maps
        roadWayRegistration();

        // read shortest path file
        System.out.println("Start reading shortest path files");
        BufferedReader shortestPathReader;
        BufferedReader shortestDistanceReader;
        shortestPathReader = new BufferedReader(new FileReader(inputFilePath + this.iteration + "_shortestPaths.txt"));
        shortestDistanceReader = new BufferedReader(new FileReader(inputFilePath + this.iteration + "_shortestDistances.txt"));
        String line = shortestPathReader.readLine();
        while (line != null) {
            String[] elements = line.split(",");
            // every shortest path should have at least four items: start point, end point, start point node ID, end point node ID
            if (elements.length == 4) {
                shortestPathMatrix[Integer.parseInt(elements[0])][Integer.parseInt(elements[1])][0] = Integer.parseInt(elements[2]);
                shortestPathMatrix[Integer.parseInt(elements[0])][Integer.parseInt(elements[1])][1] = Integer.parseInt(elements[3]);
            } else {
                System.out.println("Error line in shortest path file with " + elements.length + "elements: " + line);
            }
            line = shortestPathReader.readLine();
        }
        shortestPathReader.close();

        // read shortest distance file
        line = shortestDistanceReader.readLine();
        int infiniteCount = 0;
        while (line != null) {
            String[] elements = line.split(",");
            if (elements.length == 3) {
                if (elements[2].equals("∞")) {
                    distanceMatrix[Integer.parseInt(elements[0])][Integer.parseInt(elements[1])] = Double.POSITIVE_INFINITY;
                    infiniteCount++;
                } else
                    distanceMatrix[Integer.parseInt(elements[0])][Integer.parseInt(elements[1])] = Double.parseDouble(elements[2]);
            } else {
                System.out.println("Error line with shortest distance file with" + elements.length + "elements:" + line);
            }
            line = shortestDistanceReader.readLine();
        }
        System.out.println("Shortest path files imported, infinite value count:" + infiniteCount);
    }

    /**
     * initialize the shortest path matrix and shortest distance matrix
     */
    private void matrixInit() {
        // initialize distances to infinity, and shortest path lists
        for (int v = 0; v < matrixSize; v++) {
            for (int w = 0; w < matrixSize; w++) {
                if (v == w) {
                    shortestPathMatrix[v][w][0] = v;
                    shortestPathMatrix[v][w][1] = w;
                    distanceMatrix[v][w] = 0;

                } else {
                    shortestPathMatrix[v][w][0] = -1;
                    shortestPathMatrix[v][w][1] = -1;
                    distanceMatrix[v][w] = Double.POSITIVE_INFINITY;
                }
            }
        }
        System.out.println("Matrix initialisation is done");

        // add vertex distance for every edge to the road ways
        for (RoadWay way : this.roadNetworkGraph.getWays()) {
            double distance = 0;
            if (way.getDistance() == 0) {
                System.out.println("The road way doesn't contain distance.");
                for (int i = 0; i < way.size() - 1; i++) {
                    distance += distanceFunction.distance(way.getNode(i).toPoint(), way.getNode(i + 1).toPoint());
                }
            } else distance = way.getDistance();
            if (vertexIDMap.containsKey(way.getNode(0).lon() + "_" + way.getNode(0).lat()) && vertexIDMap.containsKey(way.getNode(way.size() - 1).lon() + "_" + way.getNode(way.size() - 1).lat())) {
                int endPointA = vertexIDMap.get(way.getNode(0).lon() + "_" + way.getNode(0).lat());
                int endPointB = vertexIDMap.get(way.getNode(way.size() - 1).lon() + "_" + way.getNode(way.size() - 1).lat());
                if (endPointA == endPointB) {
                    System.out.println("Self loop edge occurred.");
                } else {
                    distanceMatrix[endPointA][endPointB] = distance;
                    shortestPathMatrix[endPointA][endPointB][0] = endPointA;
                    shortestPathMatrix[endPointA][endPointB][1] = endPointB;

//                    // TODO test undirected part
//                    distanceMatrix[endPointB][endPointA] = distance;
//                    shortestPathMatrix[endPointB][endPointA][0] = endPointB;
//                    shortestPathMatrix[endPointB][endPointA][1] = endPointA;
                }
            } else
                System.out.println("Road node pair doesn't exist:" + way.getNode(0).lon() + "_" + way.getNode(0).lat() + "," + way.getNode(way.size() - 1).lon() + "_" + way.getNode(way.size() - 1).lat());
        }
        System.out.println("The existing road ways are imported");
    }

    /**
     * remove all road nodes that have no edge connected
     *
     * @param roadNetworkGraph input road network
     * @return road network after removing all isolated nodes
     */
    private RoadNetworkGraph isolatedNodeRemoval(RoadNetworkGraph roadNetworkGraph) {
        int maxDegree = 0;
        int nodeRemoveCount = 0;

        // eliminate the road nodes that have no edges
        List<RoadNode> removedRoadNodeList = new ArrayList<>();
        for (RoadNode n : roadNetworkGraph.getNodes()) {
            if (n.getDegree() == 0) {
                removedRoadNodeList.add(n);
                nodeRemoveCount++;
            }
            if (maxDegree < n.getDegree())
                maxDegree = n.getDegree();
        }
        for (RoadNode n : removedRoadNodeList) {
            roadNetworkGraph.getNodes().remove(n);
        }

        System.out.println("All pair file: Number of isolated road nodes = " + nodeRemoveCount);
        System.out.println("All pair file: max degree = " + maxDegree);
        return roadNetworkGraph;
    }

    /**
     * calculate shortest path between every vertex pair using Floyd-Warshall algorithm. The complexity is O(v^3)
     */
    private void floydWarshallUpdate() {
        // Floyd-Warshall updates
        for (int i = 0; i < matrixSize; i++) {
            // compute shortest paths using only 0, 1, ..., i as intermediate vertices
            for (int v = 0; v < matrixSize; v++) {
                if (shortestPathMatrix[v][i][0] == -1 || shortestPathMatrix[v][i][1] == -1)
                    continue;  // optimization
                for (int w = 0; w < matrixSize; w++) {
                    if (distanceMatrix[v][w] > distanceMatrix[v][i] + distanceMatrix[i][w]) {
                        distanceMatrix[v][w] = distanceMatrix[v][i] + distanceMatrix[i][w];

                        // derive the actual path
                        shortestPathMatrix[v][w] = shortestPathMatrix[i][w];
                    }
                }
                // check for negative cycle
                if (distanceMatrix[v][v] < 0.0) {
                    System.out.println("Negative cycle exists.");
                    return;
                }
            }
            if (i % (matrixSize / 100) == 0) {
                System.out.println(i / (matrixSize / 100) + "/100 milestone reached");
            }
        }
    }

    /**
     * give every road node an index id, which will be used to find the position
     *
     * @return road node count
     */
    private int roadNodeRegistration() {
        int idCount = 0;
        // create one graph node per road network node.
        for (RoadNode node : this.roadNetworkGraph.getNodes()) {
            if (node.getDegree() != 0) {
                if (!vertexIDMap.containsKey(node.lon() + "_" + node.lat())) {
                    vertexIDMap.put(node.lon() + "_" + node.lat(), idCount);
                    idCount++;
                } else
                    System.out.println("Duplicated roadNode:" + node.getId() + ", the road network should guarantee the uniqueness of every road node");
            }
        }
        int totalNodeCount = idCount;
        for (RoadWay w : this.roadNetworkGraph.getWays()) {
            for (RoadNode n : w.getNodes()) {
                totalNodeCount++;
            }
        }
        System.out.println("totalNodeCount = " + totalNodeCount);
        return idCount;
    }

    /**
     * register all existing connections to hash maps
     */
    private void roadWayRegistration() {
        for (RoadWay way : this.roadNetworkGraph.getWays()) {
            if (vertexIDMap.containsKey(way.getNode(0).lon() + "_" + way.getNode(0).lat()) && vertexIDMap.containsKey(way.getNode(way.size() - 1).lon() + "_" + way.getNode(way.size() - 1).lat())) {
                int endPointA = vertexIDMap.get(way.getNode(0).lon() + "_" + way.getNode(0).lat());
                int endPointB = vertexIDMap.get(way.getNode(way.size() - 1).lon() + "_" + way.getNode(way.size() - 1).lat());
                idPairRoadMap.put(endPointA + "_" + endPointB, way);
            } else
                System.out.println("Road node pair doesn't exist:" + way.getNode(0).lon() + "_" + way.getNode(0).lat() + "," + way.getNode(way.size() - 1).lon() + "_" + way.getNode(way.size() - 1).lat());
            for (Segment s : way.getEdges()) {
                if (!this.segmentRoadMap.containsKey(s.p1().x() + "_" + s.p1().y() + "," + s.p2().x() + "_" + s.p2().y())) {
                    this.segmentRoadMap.put(s.p1().x() + "_" + s.p1().y() + "," + s.p2().x() + "_" + s.p2().y(), way);
                } else {
                    System.out.println("Same segment on different road ways");
                }
            }
        }
    }


//    public void roadMapRegistration() {
//        // maintain ID pair and road way mapping
//        for (RoadWay way : this.roadNetworkGraph.getWays()) {
//            if (this.vertexIDMap.containsKey(way.getNode(0).lon() + "_" + way.getNode(0).lat()) && this.vertexIDMap.containsKey(way.getNode(way.size() - 1).lon() + "_" + way.getNode(way.size() - 1).lat())) {
//                int endPointA = this.vertexIDMap.get(way.getNode(0).lon() + "_" + way.getNode(0).lat());
//                int endPointB = this.vertexIDMap.get(way.getNode(way.size() - 1).lon() + "_" + way.getNode(way.size() - 1).lat());
//                if (endPointA == endPointB) {
//                    System.out.println("Self loop edge occurred.");
//                } else {
//                    this.idPairRoadMap.put(endPointA + "_" + endPointB, way);
//                }
//            } else System.out.println("Road node doesn't exist");
//            for (Segment s : way.getEdges()) {
//                if (!this.segmentRoadMap.containsKey(s.p1().x() + "_" + s.p1().y() + "," + s.p2().x() + "_" + s.p2().y())) {
//                    this.segmentRoadMap.put(s.p1().x() + "_" + s.p1().y() + "," + s.p2().x() + "_" + s.p2().y(), way);
//                } else {
//                    System.out.println("Same segment on different road ways");
//                }
//            }
//        }
//    }

    public double getShortestDistance(Segment startSegment, Segment endSegment) {
        if (vertexIDMap.containsKey(startSegment.p2().x() + "_" + startSegment.p2().y()) && vertexIDMap.containsKey(endSegment.p1().x() + "_" + endSegment.p1().y())) {
            return distanceMatrix[vertexIDMap.get(startSegment.p2().x() + "_" + startSegment.p2().y())][vertexIDMap.get(endSegment.p1().x() + "_" + endSegment.p1().y())];
        } else {
            double distance = 0;
            RoadWay startRoad = segmentRoadMap.get(startSegment.p1().x() + "_" + startSegment.p1().y() + "," + startSegment.p2().x() + "_" + startSegment.p2().y());
            RoadWay endRoad = segmentRoadMap.get(endSegment.p1().x() + "_" + endSegment.p1().y() + "," + endSegment.p2().x() + "_" + endSegment.p2().y());
            String startSegmentEndPoint = startSegment.p2().x() + "_" + startSegment.p2().y();
            String endSegmentStartPoint = endSegment.p1().x() + "_" + endSegment.p1().y();
            if (startRoad.getId().equals(endRoad.getId())) {
                distance += distanceWithinRoadWay(startSegmentEndPoint, endSegmentStartPoint, startRoad);
                return distance;
            } else {
                String startRoadEndCoordinate = startRoad.getNode(startRoad.getNodes().size() - 1).lon() + "_" + startRoad.getNode(startRoad.getNodes().size() - 1).lat();
                String endRoadStartCoordinate = endRoad.getNode(0).lon() + "_" + endRoad.getNode(0).lat();
                if (vertexIDMap.containsKey(startRoadEndCoordinate) && vertexIDMap.containsKey(endRoadStartCoordinate)) {
                    distance += distanceWithinRoadWay(startSegmentEndPoint, startRoadEndCoordinate, startRoad);
                    distance += distanceMatrix[vertexIDMap.get(startRoadEndCoordinate)][vertexIDMap.get(endRoadStartCoordinate)];
                    distance += distanceWithinRoadWay(endRoadStartCoordinate, endSegmentStartPoint, endRoad);
                    return distance;
                } else {
                    System.out.println("No such point pair:" + startSegmentEndPoint + "," + endSegmentStartPoint);
                    return Double.POSITIVE_INFINITY;
                }
            }
        }
    }

    // check whether the start point is in front of the end point on the road way
    private boolean isOrdered(String startCoordinate, String endCoordinate, RoadWay currRoadWay) {
        boolean isOrdered = false;
        for (RoadNode n : currRoadWay.getNodes()) {
            String currLoc = n.lon() + "_" + n.lat();
            if (currLoc.equals(startCoordinate)) {
                isOrdered = true;
            } else if (currLoc.equals(endCoordinate)) {
                return isOrdered;
            }
        }
        if (isOrdered) {
            System.out.println("current road way doesn't contain given end point");
        } else {
            System.out.println("Both start and end points are not found in this road way");
        }
        return false;
    }

    private double distanceWithinRoadWay(String startCoordinate, String endCoordinate, RoadWay currRoadWay) {
        double distance = 0;
        boolean startTracking = false;
        String prevPointLoc = "";
        for (RoadNode n : currRoadWay.getNodes()) {
            String currPointLoc = n.lon() + "_" + n.lat();
            if (currPointLoc.equals(startCoordinate)) {
                if (currPointLoc.equals(endCoordinate)) {
                    break;
                }
                startTracking = true;
                prevPointLoc = currPointLoc;
            } else if (currPointLoc.equals(endCoordinate)) {
                if (!startTracking) {
                    distance = Double.POSITIVE_INFINITY;
                    break;
                } else {
                    String[] prevPoint = prevPointLoc.split("_");
                    String[] currPoint = currPointLoc.split("_");
                    distance += distanceFunction.pointToPointDistance(Double.parseDouble(prevPoint[0]), Double.parseDouble(prevPoint[1]), Double.parseDouble(currPoint[0]), Double.parseDouble(currPoint[1]));
                    break;
                }
            } else if (startTracking) {
                String[] prevPoint = prevPointLoc.split("_");
                String[] currPoint = currPointLoc.split("_");
                distance += distanceFunction.pointToPointDistance(Double.parseDouble(prevPoint[0]), Double.parseDouble(prevPoint[1]), Double.parseDouble(currPoint[0]), Double.parseDouble(currPoint[1]));
                prevPointLoc = currPointLoc;
            }
        }
        return distance;
    }

    // add all points that along the shortest path between the start and the end segment. Both end points are excluded
    public List<RoadNode> getShortestPath(Segment startSegment, Segment endSegment) {
        List<RoadNode> result = new ArrayList<>();
        double distance = getShortestDistance(startSegment, endSegment);
        if (distance == Double.POSITIVE_INFINITY) {
            System.out.println("No shortest path available:" + startSegment + "," + endSegment);
            return null;
        }
        RoadWay startRoad = segmentRoadMap.get(startSegment.p1().x() + "_" + startSegment.p1().y() + "," + startSegment.p2().x() + "_" + startSegment.p2().y());
        RoadWay endRoad = segmentRoadMap.get(endSegment.p1().x() + "_" + endSegment.p1().y() + "," + endSegment.p2().x() + "_" + endSegment.p2().y());
        String startSegmentEndPoint = startSegment.p2().x() + "_" + startSegment.p2().y();
        String endSegmentStartPoint = endSegment.p1().x() + "_" + endSegment.p1().y();
        if (startRoad.getId().equals(endRoad.getId())) {
            result.addAll(addIntermediateNodes(startSegmentEndPoint, endSegmentStartPoint, startRoad));
            return result;
        } else {
            RoadNode startRoadEndPoint = startRoad.getNode(startRoad.getNodes().size() - 1);
            RoadNode endRoadStartPoint = endRoad.getNode(0);
            // add the intermediate points between the start point and the end point of the corresponding road way
            result.addAll(addIntermediateNodes(startSegmentEndPoint, startRoadEndPoint.lon() + "_" + startRoadEndPoint.lat(), startRoad));
            result.add(new RoadNode(startRoad.getId(), startRoadEndPoint.lon(), startRoadEndPoint.lat()));
            if (startRoadEndPoint.lon() != endRoadStartPoint.lon() || startRoadEndPoint.lat() != endRoadStartPoint.lat()) {
                // the start road way and the end road way are not connected
                int startPoint = vertexIDMap.get(startRoadEndPoint.lon() + "_" + startRoadEndPoint.lat());
                int endPoint = vertexIDMap.get(endRoadStartPoint.lon() + "_" + endRoadStartPoint.lat());
                Stack<Integer> path = new Stack<>();
                path.push(endPoint);
                for (int[] e = shortestPathMatrix[startPoint][endPoint]; e[0] != startPoint; e = shortestPathMatrix[startPoint][e[0]]) {
                    if (e[0] == -1) {
                        System.out.println("test");
                    }
                    path.push(e[0]);
                }
                if (startPoint != endPoint) {
                    path.push(startPoint);
                }
                int size = path.size();
                int prevPoint = -1;
                int currPoint;
                for (int i = 0; i < size - 1; i++) {
                    if (prevPoint == -1) {
                        prevPoint = path.pop();
                        currPoint = path.pop();
                    } else {
                        currPoint = path.pop();
                    }
                    if (idPairRoadMap.containsKey(prevPoint + "_" + currPoint)) {
                        RoadWay currRoadWay = idPairRoadMap.get(prevPoint + "_" + currPoint);
                        if (currRoadWay.getNodes() == null) {
                            System.out.println("test");
                        }
                        List<RoadNode> roadNodes = currRoadWay.getNodes();
                        for (int j = 1; j < roadNodes.size(); j++) {
                            RoadNode n = roadNodes.get(j);
                            result.add(new RoadNode(currRoadWay.getId(), n.lon(), n.lat()));
                        }
                    } else {
                        // TODO find the reason why it happens
                        System.out.println("No edge between point pair:" + prevPoint + "," + currPoint);
                    }
                    prevPoint = currPoint;
                }
            }
            // add the intermediate points between the last road way and the end point
            result.addAll(addIntermediateNodes(endRoadStartPoint.lon() + "_" + endRoadStartPoint.lat(), endSegmentStartPoint, endRoad));
            return result;
        }
    }

    //
    private List<RoadNode> addIntermediateNodes(String startPoint, String endPoint, RoadWay currRoadWay) {
        List<RoadNode> intermediateNodeList = new ArrayList<>();
        boolean startTracking = false;
        for (RoadNode n : currRoadWay.getNodes()) {
            String currPointLoc = n.lon() + "_" + n.lat();
            if (currPointLoc.equals(startPoint)) {
                if (currPointLoc.equals(endPoint)) {
                    break;
                }
                startTracking = true;
            } else if (currPointLoc.equals(endPoint)) {
                if (!startTracking)
                    // TODO find the reason why it happens
//                    System.out.println("Add intermediate node to shortest path error: last point appears first");
                break;
            } else if (startTracking) {
                intermediateNodeList.add(new RoadNode(currRoadWay.getId(), n.lon(), n.lat()));
            }
        }
        return intermediateNodeList;
    }
}