package edu.uq.dke.mapupdate.io;

import org.jdom2.JDOMException;
import traminer.util.map.roadnetwork.RoadNetworkGraph;
import traminer.util.map.roadnetwork.RoadNode;
import traminer.util.map.roadnetwork.RoadWay;
import traminer.util.spatial.SpatialInterface;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
     * Read and parse the OSM file.
     *
     * @return A Road Network Graph containing the
     * Nodes, Ways and Relations in the OSM file.
     * @throws JDOMException
     * @throws IOException
     */

    public RoadNetworkGraph readMap(int percentage) throws IOException {

        double maxLat = -Double.MAX_VALUE, maxLon = -Double.MAX_VALUE;
        double minLat = Double.MAX_VALUE, minLon = Double.MAX_VALUE;        // boarder of the map
        List<RoadNode> nodes = new ArrayList<RoadNode>();
        List<RoadWay> ways = new ArrayList<RoadWay>();
        Map<String, Integer> nodeIndexMapping = new HashMap<>();       // maintain a mapping of road location to node index
        // read road nodes
        String line;
        BufferedReader brVertices = new BufferedReader(new FileReader(this.csvMapPath + "vertices_" + percentage + ".txt"));
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

            RoadNode newNode = new RoadNode(nodeInfo[0], lon, lat, false);
            nodes.add(newNode);
            nodeIndexMapping.put(newNode.getId(), nodes.indexOf(newNode));
        }
        brVertices.close();

        // read road ways
        BufferedReader brEdges = new BufferedReader(new FileReader(this.csvMapPath + "edges_" + percentage + ".txt"));
        while ((line = brEdges.readLine()) != null) {
            RoadWay newWay = new RoadWay();
            List<RoadNode> miniNode = new ArrayList<>();
            String[] edgeInfo = line.split("\\|");
            if (!edgeInfo[0].contains(",")) {
                boolean isCompleteRoad = true;
                for (int i = 1; i < edgeInfo.length; i++) {
                    String[] roadWayPoint = edgeInfo[i].split(",");
                    if (roadWayPoint.length == 3) {
                        RoadNode newNode = new RoadNode(roadWayPoint[0], Double.parseDouble(roadWayPoint[1]), Double.parseDouble(roadWayPoint[2]), true);

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
                    newWay.setNodes(miniNode);
                    newWay.getNode(0).setToNonMiniNode();
                    newWay.getNode(newWay.size() - 1).setToNonMiniNode();
                    if (nodeIndexMapping.containsKey(newWay.getNode(0).getId())) {
                        nodes.get(nodeIndexMapping.get(newWay.getNode(0).getId())).addOutgoingAdjacency(newWay);
                    } else {
                        System.out.println("ERROR");
                    }
                    nodes.get(nodeIndexMapping.get(newWay.getNode(newWay.size() - 1).getId())).addIncomingAdjacency(newWay);
                    ways.add(newWay);
                }
            } else {
                System.out.println("Wrong road id info:" + edgeInfo[0]);
            }
        }
        brEdges.close();

        List<RoadNode> removedRoadNodeList = new ArrayList<>();
        for (RoadNode n : nodes) {
            if (n.getDegree() == 0) {
                removedRoadNodeList.add(n);
            }
        }
        nodes.removeAll(removedRoadNodeList);
        System.out.println("Read " + percentage + "% road map, isolate nodes:" + removedRoadNodeList.size() + ", total nodes:" + nodes.size() + ", total roads:" + ways.size());
        roadGraph.addNodes(nodes);
        roadGraph.addWays(ways);
        roadGraph.setMaxLat(maxLat);
        roadGraph.setMinLat(minLat);
        roadGraph.setMaxLon(maxLon);
        roadGraph.setMinLon(minLon);
        if (!checkCompleteness(roadGraph)) {
            System.out.println("Map contains problem");
        }
        return roadGraph;
    }

    private boolean checkCompleteness(RoadNetworkGraph roadGraph) {
        for (RoadNode n : roadGraph.getNodes()) {
            if (!n.checkNodeCompleteness()) {
                return false;
            }
        }
        for (RoadWay w : roadGraph.getWays()) {
            for (RoadNode n : w.getNodes()) {
                if (!n.checkNodeCompleteness()) {
//                    return false;
                }
            }
        }
        return true;
    }

    public RoadNetworkGraph readMapEdges(int percentage) throws IOException {

        double maxLat = -Double.MAX_VALUE, maxLon = -Double.MAX_VALUE;
        double minLat = Double.MAX_VALUE, minLon = Double.MAX_VALUE;        // boarder of the map
        // read road ways only
        String line;
        List<RoadNode> nodes = new ArrayList<>();
        List<RoadWay> ways = new ArrayList<>();
        Map<String, Integer> locIndexMapping = new HashMap<>();       // maintain a mapping of road location to node index
        BufferedReader brEdges = new BufferedReader(new FileReader(csvMapPath + "edges_" + percentage + ".txt"));
        while ((line = brEdges.readLine()) != null) {
            String[] edgeInfo = line.split("\\|");
            List<RoadNode> intermediateNodes = new ArrayList<>();
            if (!edgeInfo[0].contains(",")) {
                RoadWay newWay = new RoadWay();
                boolean isCompleteRoad = true;
                for (int i = 1; i < edgeInfo.length; i++) {
                    String[] roadWayPoint = edgeInfo[i].split(",");
                    if (roadWayPoint.length == 3) {
                        RoadNode newNode = new RoadNode(roadWayPoint[0], Double.parseDouble(roadWayPoint[1]), Double.parseDouble(roadWayPoint[2]), true);

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
                        if (i == 1 || i == edgeInfo.length - 1) {
                            newNode.setToNonMiniNode();
                            if (!locIndexMapping.containsKey(newNode.lon() + "_" + newNode.lat())) {
                                nodes.add(newNode);
                                locIndexMapping.put(newNode.lon() + "_" + newNode.lat(), nodes.indexOf(newNode));
                            }
                        }
                        intermediateNodes.add(newNode);
                    } else {
                        System.err.println("Wrong road node:" + roadWayPoint.length);
                        isCompleteRoad = false;
                        break;
                    }
                }
                if (isCompleteRoad) {
                    newWay.setId(edgeInfo[0]);
                    newWay.setNodes(intermediateNodes);
                    if (nodeUpdate(nodes, newWay, locIndexMapping)) {
                        ways.add(newWay);
                    }
                }
            } else {
                System.out.println("Wrong road id info:" + edgeInfo[0]);
            }
        }

        brEdges.close();

        List<RoadNode> removedRoadNodeList = new ArrayList<>();
        for (RoadNode n : nodes) {
            if (n.getDegree() == 0) {
                removedRoadNodeList.add(n);
            }
        }
        nodes.removeAll(removedRoadNodeList);
        System.out.println("Total removed nodes:" + removedRoadNodeList.size() + ", total nodes:" + nodes.size());
        roadGraph.addNodes(nodes);
        roadGraph.addWays(ways);
        roadGraph.setMaxLat(maxLat);
        roadGraph.setMinLat(minLat);
        roadGraph.setMaxLon(maxLon);
        roadGraph.setMinLon(minLon);
        if (!checkCompleteness(roadGraph)) {
            System.out.println("Map contains problem");
        }
        return roadGraph;
    }

    // abandoned method since we use nodeID to control the connections
    private boolean nodeUpdate(List<RoadNode> nodeList, RoadWay roadWay, Map<String, Integer> mapping) {
        // find the corresponding end points
        RoadNode startPoint = roadWay.getNode(0);
        RoadNode endPoint = roadWay.getNode(roadWay.size() - 1);
        startPoint.setToNonMiniNode();
        endPoint.setToNonMiniNode();

        for (RoadNode n : roadWay.getNodes()) {
            if (n.isMiniNode() && mapping.containsKey(n.lon() + "_" + n.lat())) {
                System.out.println("Error! mini node is an intersection as well:" + mapping.get(n.lon() + "_" + n.lat()) + "," + n.lon() + "," + n.lat());
                return false;
            }
        }
        RoadNode startNode = nodeList.get(mapping.get(startPoint.lon() + "_" + startPoint.lat()));
        RoadNode endNode = nodeList.get(mapping.get(endPoint.lon() + "_" + endPoint.lat()));
        if (startNode.lon() == startPoint.lon() && startNode.lat() == startPoint.lat() && endNode.lon() == endNode.lon() && endNode.lat() == endNode.lat()) {
            startNode.addOutgoingAdjacency(roadWay);
            endNode.addIncomingAdjacency(roadWay);
        } else {
            System.out.println("Wrong node match");
        }

        return true;
    }
}