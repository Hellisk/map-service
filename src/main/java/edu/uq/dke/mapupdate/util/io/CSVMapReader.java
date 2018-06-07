package edu.uq.dke.mapupdate.util.io;

import edu.uq.dke.mapupdate.util.object.SpatialInterface;
import edu.uq.dke.mapupdate.util.object.roadnetwork.RoadNetworkGraph;
import edu.uq.dke.mapupdate.util.object.roadnetwork.RoadNode;
import edu.uq.dke.mapupdate.util.object.roadnetwork.RoadWay;

import java.io.BufferedReader;
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

        double maxLat = Double.NEGATIVE_INFINITY, maxLon = Double.NEGATIVE_INFINITY;
        double minLat = Double.POSITIVE_INFINITY, minLon = Double.POSITIVE_INFINITY;        // boarder of the map
        List<RoadNode> nodes = new ArrayList<>();
        List<RoadWay> ways = new ArrayList<>();
        Map<String, Integer> node2Index = new HashMap<>();       // maintain a mapping of road location to node index
        // read road nodes
        String line;
        BufferedReader brVertices = new BufferedReader(new FileReader(this.csvMapPath + "vertices_" + percentage + ".txt"));
        int nodeIndex = 0;
        while ((line = brVertices.readLine()) != null) {
            String[] nodeInfo = line.split(",");
            double lon = Double.parseDouble(nodeInfo[1]);
            double lat = Double.parseDouble(nodeInfo[2]);

            // update the map boarder
            if (maxLon < lon) {
                maxLon = lon;
            }
            if (minLon > lon) {
                minLon = lon;
            }
            if (maxLat < lat) {
                maxLat = lat;
            }
            if (minLat > lat) {
                minLat = lat;
            }

            RoadNode newNode = new RoadNode(nodeInfo[0], lon, lat);
            nodes.add(newNode);
            node2Index.put(newNode.getId(), nodeIndex);
            nodeIndex++;
        }
        brVertices.close();

        // read road ways
        BufferedReader brEdges = new BufferedReader(new FileReader(this.csvMapPath + "edges_" + percentage + ".txt"));
        while ((line = brEdges.readLine()) != null) {
            RoadWay newWay = new RoadWay();
            List<RoadNode> miniNode = new ArrayList<>();
            String[] edgeInfo = line.split("\\|");
            // the road way record is complete and the endpoints exist
            if (!edgeInfo[0].contains(",") && node2Index.containsKey(edgeInfo[2].split(",")[0]) && node2Index.containsKey
                    (edgeInfo[edgeInfo.length - 1].split(",")[0])) {
                boolean isCompleteRoad = true;
                for (int i = 2; i < edgeInfo.length; i++) {
                    String[] roadWayPoint = edgeInfo[i].split(",");
                    if (roadWayPoint.length == 3) {
                        RoadNode newNode;
                        if (i == 2) {
                            newNode = nodes.get(node2Index.get(roadWayPoint[0]));
                        } else if (i == edgeInfo.length - 1) {
                            newNode = nodes.get(node2Index.get(roadWayPoint[0]));
                        } else
                            newNode = new RoadNode(roadWayPoint[0], Double.parseDouble(roadWayPoint[1]), Double.parseDouble(roadWayPoint[2]));

                        // update the map boarder
                        if (maxLon < Double.parseDouble(roadWayPoint[1])) {
                            maxLon = Double.parseDouble(roadWayPoint[1]);
                        }
                        if (minLon > Double.parseDouble(roadWayPoint[1])) {
                            minLon = Double.parseDouble(roadWayPoint[1]);
                        }
                        if (maxLat < Double.parseDouble(roadWayPoint[2])) {
                            maxLat = Double.parseDouble(roadWayPoint[2]);
                        }
                        if (minLat > Double.parseDouble(roadWayPoint[2])) {
                            minLat = Double.parseDouble(roadWayPoint[2]);
                        }

                        miniNode.add(newNode);
                    } else {
                        System.err.println("Wrong road node:" + roadWayPoint.length);
                        isCompleteRoad = false;
                        break;
                    }

                }

                if (isCompleteRoad) {
                    newWay.setId(edgeInfo[0]);
                    if (edgeInfo[1].contains(",")) {
                        String[] scoreInfo = edgeInfo[1].split(",");
                        newWay.setNewRoad(true);
                        newWay.setConfidenceScore(Double.parseDouble(scoreInfo[0]));
                        newWay.setInfluenceScore(Double.parseDouble(scoreInfo[1]));
                    }
                    newWay.setNodes(miniNode);
                    ways.add(newWay);
                }
            } else {
                System.out.println("Road way record is broken or endpoints not found: " + edgeInfo[0]);
            }
        }
        brEdges.close();

        roadGraph.addNodes(nodes);
        roadGraph.addWays(ways);
        roadGraph.setBoundingBox(minLon, maxLon, minLat, maxLat);
        List<RoadNode> removedRoadNodeList = new ArrayList<>();
        for (RoadNode n : nodes) {
            if (n.getDegree() == 0) {
                removedRoadNodeList.add(n);
            }
        }
        roadGraph.getNodes().removeAll(removedRoadNodeList);
        System.out.println("Read " + percentage + "% road map, isolate nodes:" + removedRoadNodeList.size() + ", total nodes:" + nodes.size() + ", total roads:" + ways.size());
        return roadGraph;
    }

    public List<RoadWay> readRemovedEdges(int percentage) throws IOException {
        List<RoadWay> removedRoads = new ArrayList<>();
        String line;
        // read removed road ways
        BufferedReader brEdges = new BufferedReader(new FileReader(this.csvMapPath + "removedEdges_" + percentage + ".txt"));
        while ((line = brEdges.readLine()) != null) {
            RoadWay newWay = new RoadWay();
            List<RoadNode> miniNode = new ArrayList<>();
            String[] edgeInfo = line.split("\\|");
            if (!edgeInfo[0].contains(",")) {
                for (int i = 2; i < edgeInfo.length; i++) {
                    String[] roadWayPoint = edgeInfo[i].split(",");
                    if (roadWayPoint.length == 3) {
                        RoadNode newNode = new RoadNode(roadWayPoint[0], Double.parseDouble(roadWayPoint[1]), Double.parseDouble(roadWayPoint[2]));

                        miniNode.add(newNode);
                    } else {
                        System.err.println("Wrong road node:" + roadWayPoint.length);
                        break;
                    }
                }
                newWay.setId(edgeInfo[0]);
                newWay.setNodes(miniNode);
                removedRoads.add(newWay);
            } else {
                System.out.println("Wrong road id info:" + edgeInfo[0]);
            }
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
}