package edu.uq.dke.mapupdate.io;

import org.jdom2.JDOMException;
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


    public AllPairsShortestPathFile(String cityName, String inputMapPath, boolean isShpFile) throws JDOMException, IOException {
        RoadNetworkGraph roadNetworkGraph;
        String inputVertexPath = inputMapPath + cityName + "_vertices.txt";
        String inputEdgePath = inputMapPath + cityName + "_edges.txt";
        CSVMapReader csvMapReader = new CSVMapReader(inputVertexPath, inputEdgePath);
        if (isShpFile) {
            roadNetworkGraph = csvMapReader.readShapeCSV();
        } else {
            roadNetworkGraph = csvMapReader.readCSV();
        }
        matrixSize = roadNodeRegistration(roadNetworkGraph);
        System.out.println("Matrix size is:" + matrixSize);
        shortestPathMatrix = new int[matrixSize][matrixSize][2];
        distanceMatrix = new double[matrixSize][matrixSize];

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
        // register initial edges
        roadWayRegistration(roadNetworkGraph);
        System.out.println("Initialisation is done");
    }

    public AllPairsShortestPathFile(RoadNetworkGraph roadNetworkGraph) {
        matrixSize = roadNodeRegistration(roadNetworkGraph);
        System.out.println("Matrix size is:" + matrixSize);
        shortestPathMatrix = new int[matrixSize][matrixSize][2];
        distanceMatrix = new double[matrixSize][matrixSize];

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

        // maintain ID pair and road way mapping
        for (RoadWay way : roadNetworkGraph.getWays()) {
            if (vertexIDMap.containsKey(way.getNode(0).lon() + "_" + way.getNode(0).lat()) && vertexIDMap.containsKey(way.getNode(way.size() - 1).lon() + "_" + way.getNode(way.size() - 1).lat())) {
                int endPointA = vertexIDMap.get(way.getNode(0).lon() + "_" + way.getNode(0).lat());
                int endPointB = vertexIDMap.get(way.getNode(way.size() - 1).lon() + "_" + way.getNode(way.size() - 1).lat());
                if (endPointA == endPointB) {
                    System.out.println("Self loop edge occurred.");
                } else {
                    idPairRoadMap.put(endPointA + "_" + endPointB, way);
                }
            } else System.out.println("RoadNode doesn't exist");
            for (Segment s : way.getEdges()) {
                if (!segmentRoadMap.containsKey(s.p1().x() + "_" + s.p1().y() + "," + s.p2().x() + "_" + s.p2().y())) {
                    segmentRoadMap.put(s.p1().x() + "_" + s.p1().y() + "," + s.p2().x() + "_" + s.p2().y(), way);
                } else {
                    System.out.println("Same segment on different road ways");
                }
            }
        }
        System.out.println("Initialisation is done");
    }

    public void writeShortestPathFiles(String outputFilePath) throws IOException {

        // Calculate all pairs shortest paths
        floydWarshallUpdate();

        // check whether there is an isolated point inside
        for (int i = 0; i < matrixSize; i++) {
            boolean isIsolated = true;
            for (int j = 0; j < matrixSize; j++) {
                if (distanceMatrix[i][j] != 0) {
                    isIsolated = false;
                    break;
                }
            }
            if (isIsolated) {
                System.out.println("Isolated point found:" + i);
            }
        }
        // shortest path output
        System.out.println("Start writing shortest path files.");
        File outputFolder = new File(outputFilePath);
        if (!outputFolder.exists()) {
            outputFolder.mkdirs();
        }
        BufferedWriter shortestPathWriter = new BufferedWriter(new FileWriter(outputFilePath + "shortestPaths.txt"));
        BufferedWriter shortestDistanceWriter = new BufferedWriter(new FileWriter(outputFilePath + "shortestDistances.txt"));
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
        System.out.println("Number of infinity value:" + infinityCount);
        shortestPathWriter.close();
        shortestDistanceWriter.close();
    }

    public void readShortestPathFiles(String inputFilePath) throws IOException {
        // read shortest path file
        System.out.println("Start reading shortest path files");
        BufferedReader shortestPathReader = new BufferedReader(new FileReader(inputFilePath + "shortestPaths.txt"));
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
        System.out.println("Shortest path files imported");

        // read shortest distance file
        BufferedReader shortestDistanceReader = new BufferedReader(new FileReader(inputFilePath + "shortestDistances.txt"));
        line = shortestDistanceReader.readLine();
        while (line != null) {
            String[] elements = line.split(",");
            if (elements.length == 3) {
                if (elements[2].equals("∞")) {
                    distanceMatrix[Integer.parseInt(elements[0])][Integer.parseInt(elements[1])] = Double.POSITIVE_INFINITY;
                } else
                    distanceMatrix[Integer.parseInt(elements[0])][Integer.parseInt(elements[1])] = Double.parseDouble(elements[2]);
            } else {
                System.out.println("Error line with shortest distance file with" + elements.length + "elements:" + line);
            }
            line = shortestDistanceReader.readLine();
        }
    }

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

    private int roadNodeRegistration(RoadNetworkGraph inputMap) {
        int idCount = 0;
        // create one graph node per road network node.
        for (RoadNode node : inputMap.getNodes()) {
            if (node.getDegree() != 0) {
                if (!vertexIDMap.containsKey(node.lon() + "_" + node.lat())) {
                    vertexIDMap.put(node.lon() + "_" + node.lat(), idCount);
                    idCount++;
                } else System.out.println("Duplicated roadNode:" + node.getId());
            }
        }
        return idCount;
    }

    private int roadWayNodeRegistration(RoadNetworkGraph inputMap, int idCount) {

        // create one graph node per road network node.
        for (RoadWay way : inputMap.getWays()) {
            for (int i = 0; i < way.size() - 1; i++) {
                if (!vertexIDMap.containsKey(way.getNode(i).lon() + "_" + way.getNode(i).lat())) {
                    vertexIDMap.put(way.getNode(i).lon() + "_" + way.getNode(i).lat(), idCount);
                    idCount++;
                } else {
                    System.out.println("Duplicated roadWayNode:" + way.getNode(i).getId());
                }
            }
        }
        return idCount;
    }

    private void roadWayRegistration(RoadNetworkGraph inputMap) {
        // add vertex distance for every edge in the road ways
        for (RoadWay way : inputMap.getWays()) {
            double distance = 0;
            if (way.getDistance() == 0) {
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

                    // undirected graph
                    distanceMatrix[endPointB][endPointA] = distance;
                    shortestPathMatrix[endPointB][endPointA][0] = endPointB;
                    shortestPathMatrix[endPointB][endPointA][1] = endPointA;
                    idPairRoadMap.put(endPointA + "_" + endPointB, way);
                }
            } else System.out.println("RoadNode doesn't exist:" + way.getNode(0).lon() + "_" + way.getNode(0).lat());
        }
        System.out.println("Navigate map created");
    }

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
                if (prevPointLoc.equals("")) {
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
//                        System.out.println("No edge between point pair:" + prevPoint + "," + currPoint);
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
                    System.out.println("Add intermediate node to shortest path error: last point appears first");
                break;
            } else if (startTracking) {
                intermediateNodeList.add(new RoadNode(currRoadWay.getId(), n.lon(), n.lat()));
            }
        }
        return intermediateNodeList;
    }
}