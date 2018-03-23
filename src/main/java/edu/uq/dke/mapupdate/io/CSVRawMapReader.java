package edu.uq.dke.mapupdate.io;

import traminer.util.map.roadnetwork.RoadNetworkGraph;
import traminer.util.map.roadnetwork.RoadNode;
import traminer.util.map.roadnetwork.RoadWay;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static edu.uq.dke.mapupdate.io.XMLTrajectoryReader.stringFormatter;

public class CSVRawMapReader {
    private String path;

    public CSVRawMapReader(String mapFolder) {
        this.path = mapFolder;
    }

    public RoadNetworkGraph readRawMap(int trajNum) throws IOException {
        double maxLat = -Double.MAX_VALUE, maxLon = -Double.MAX_VALUE;
        double minLat = Double.MAX_VALUE, minLon = Double.MAX_VALUE;        // boarder of the map
        RoadNetworkGraph roadGraph = new RoadNetworkGraph();
        List<RoadNode> nodes = new ArrayList<RoadNode>();
        Map<String, RoadNode> index2Node = new HashMap<>();
        List<RoadWay> ways = new ArrayList<RoadWay>();
        // read road nodes
        String line;
        int lineCount = 0;
        BufferedReader brVertices = new BufferedReader(new FileReader(this.path + stringFormatter
                (trajNum) + File.separator + stringFormatter
                (trajNum) + ".nodes"));
        while ((line = brVertices.readLine()) != null) {
            String[] nodeInfo = line.split("\t");
            double lon = Double.parseDouble(nodeInfo[0]);
            double lat = Double.parseDouble(nodeInfo[1]);

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

            RoadNode newNode = new RoadNode(lineCount + "", lon, lat, false);
            index2Node.put(lineCount + "", newNode);
            nodes.add(newNode);
            lineCount++;
        }
        brVertices.close();

        // read road ways
        int roadCount = 0;
        BufferedReader brEdges = new BufferedReader(new FileReader(this.path + stringFormatter
                (trajNum) + File.separator + stringFormatter
                (trajNum) + ".arcs"));
        while ((line = brEdges.readLine()) != null) {
            RoadWay newWay = new RoadWay();
            List<RoadNode> miniNode = new ArrayList<>();
            String[] edgeInfo = line.split("\t");
            miniNode.add(index2Node.get(edgeInfo[0]));
            miniNode.add(index2Node.get(edgeInfo[1]));
            newWay.setId(roadCount + "");
            newWay.setNodes(miniNode);
            newWay.getNode(0).setToNonMiniNode();
            newWay.getNode(newWay.size() - 1).setToNonMiniNode();
            if (index2Node.containsKey(newWay.getNode(0).getId())) {
                index2Node.get(newWay.getNode(0).getId()).addOutgoingAdjacency(newWay);
            } else {
                System.out.println("ERROR");
            }
            if (index2Node.containsKey(newWay.getNode(newWay.size() - 1).getId())) {
                index2Node.get(newWay.getNode(newWay.size() - 1).getId()).addIncomingAdjacency(newWay);
            } else {
                System.out.println("ERROR");
            }
            ways.add(newWay);
            roadCount++;
        }
        brEdges.close();

        List<RoadNode> removedRoadNodeList = new ArrayList<>();
        for (RoadNode n : nodes) {
            if (n.getDegree() == 0) {
                removedRoadNodeList.add(n);
            }
        }
        nodes.removeAll(removedRoadNodeList);
        System.out.println("Read " + trajNum + " road map, isolate nodes:" + removedRoadNodeList.size() + ", total nodes:" + nodes.size() + ", total roads:" + ways.size());
        roadGraph.addNodes(nodes);
        roadGraph.addWays(ways);
        roadGraph.setMaxLat(maxLat);
        roadGraph.setMinLat(minLat);
        roadGraph.setMaxLon(maxLon);
        roadGraph.setMinLon(minLon);
        return roadGraph;
    }
}
