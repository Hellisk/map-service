package edu.uq.dke.mapupdate.io;

import org.jdom2.JDOMException;
import traminer.util.map.roadnetwork.RoadNetworkGraph;
import traminer.util.map.roadnetwork.RoadNode;
import traminer.util.map.roadnetwork.RoadWay;
import traminer.util.spatial.distance.EuclideanDistanceFunction;

import java.io.*;
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
    private EuclideanDistanceFunction distanceFunction = new EuclideanDistanceFunction();
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
        }
        System.out.println("Initialisation is done");
    }

    public void writeShortestPathFiles(String outputFilePath) throws IOException {

        // Calculate all pairs shortest paths
        floydWarshallUpdate();

        // shortest path output
        System.out.println("Start writing shortest path files.");
        File outputFolder = new File(outputFilePath);
        if (!outputFolder.exists()) {
            outputFolder.mkdirs();
        }
        BufferedWriter shortestPathWriter = new BufferedWriter(new FileWriter(outputFilePath + "shortestPaths.txt"));
        BufferedWriter shortestDistanceWriter = new BufferedWriter(new FileWriter(outputFilePath + "shortestDistances.txt"));
        int infinityCount = 0;
        for (int i = 0; i < matrixSize; i++) {
            for (int j = 0; j < matrixSize; j++) {
                if (distanceMatrix[i][j] == Double.POSITIVE_INFINITY) {
                    infinityCount++;
                }
                shortestDistanceWriter.write(i + "," + j + "," + distanceMatrix[i][j] + "\n");
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
                if (elements[2].equals("Infinity")) {
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
            if (!vertexIDMap.containsKey(node.lon() + "_" + node.lat())) {
                vertexIDMap.put(node.lon() + "_" + node.lat(), idCount);
                idCount++;
            } else System.out.println("Duplicated roadNode:" + node.getId());
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
            for (int i = 0; i < way.size() - 1; i++) {
                distance += distanceFunction.distance(way.getNode(i).toPoint(), way.getNode(i + 1).toPoint());
            }
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
            } else System.out.println("RoadNode doesn't exist");
        }
        System.out.println("Navigate map created");
    }

    public double getShortestDistance(String startCoordinate, String endCoordinate) {
        if (vertexIDMap.containsKey(startCoordinate) && vertexIDMap.containsKey(endCoordinate)) {
            return distanceMatrix[vertexIDMap.get(startCoordinate)][vertexIDMap.get(endCoordinate)];
        } else {
            System.out.println("No such points:" + startCoordinate + "," + endCoordinate);
            return Double.POSITIVE_INFINITY;
        }
    }

    public RoadWay getShortestPath(String startCoordinate, String endCoordinate) {
        if (getShortestDistance(startCoordinate, endCoordinate) == Double.POSITIVE_INFINITY) {
            System.out.println("No shortest path avaliable:" + startCoordinate + "," + endCoordinate);
            return null;
        }
        int startPoint = vertexIDMap.get(startCoordinate);
        int endPoint = vertexIDMap.get(endCoordinate);
        Stack<Integer> path = new Stack<>();
        path.push(endPoint);
        int[] test = shortestPathMatrix[startPoint][endPoint];
        for (int[] e = shortestPathMatrix[startPoint][endPoint]; e[0] != startPoint; e = shortestPathMatrix[startPoint][e[0]]) {
            path.push(e[0]);
        }
        if (startPoint != endPoint) {
            path.push(startPoint);
        }
        RoadWay result = new RoadWay("");
        int size = path.size();
        int prevPoint = -1;
        int currPoint = -1;
        for (int i = 0; i < size - 1; i++) {
            if (prevPoint == -1) {
                prevPoint = path.pop();
                currPoint = path.pop();
            } else {
                currPoint = path.pop();
            }
            if (idPairRoadMap.containsKey(prevPoint + "_" + currPoint)) {
                RoadWay currRoadWay = idPairRoadMap.get(prevPoint + "_" + currPoint);
                List<RoadNode> roadNodes = currRoadWay.getNodes();
                if (i != size - 2) {
                    // remove the point that overlapping the next road way
                    roadNodes.remove(currRoadWay.size() - 1);
                }
                result.addNodes(roadNodes);
            } else {
                System.out.println("No edge between point pair:" + prevPoint + "," + currPoint);
            }
            prevPoint = currPoint;
        }
        return result;
    }

}