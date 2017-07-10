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
    private final RoadNetworkGraph roadGraph;
    private final String csvVerticesPath;
    private final String csvEdgesPath;

    public CSVMapReader(final String csvVertexPath, final String csvEdgePath) {
        this.roadGraph = new RoadNetworkGraph();
        this.csvVerticesPath = csvVertexPath;
        this.csvEdgesPath = csvEdgePath;
    }

    /**
     * Read and parse the OSM file.
     *
     * @return A Road Network Graph containing the
     * Nodes, Ways and Relations in the OSM file.
     * @throws JDOMException
     * @throws IOException
     */
    public RoadNetworkGraph readCSV() throws JDOMException, IOException {

        double maxLat = -Double.MAX_VALUE, maxLon = -Double.MAX_VALUE;
        double minLat = Double.MAX_VALUE, minLon = Double.MAX_VALUE;        // boarder of the map
        List<RoadNode> nodes = new ArrayList<RoadNode>();
        List<RoadWay> ways = new ArrayList<RoadWay>();
        Map<String, RoadNode> nodeList = new HashMap<>();       // maintain a mapping of road id and node list
//        Map<String, Integer> nodeDegree = new HashMap<>();      // degree of each node.

        /**
         * Read vertex file, store the vertices into node list.
         */
        String line;
        BufferedReader brVertices = new BufferedReader(new FileReader(csvVerticesPath));
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
            nodeList.put(nodeInfo[0], newNode);
//            nodeDegree.put(nodeInfo[0],0);
        }
        brVertices.close();

//        int[] degree = new int[nodes.size()];

        /**
         * read edge file, make sure the corresponding vertices exist before adding.
         */
        BufferedReader bwEdges = new BufferedReader(new FileReader(csvEdgesPath));
        while ((line = bwEdges.readLine()) != null) {
            if (line.trim().equals(""))
                continue;
            String[] edgeInfo = line.split(",");

            if (nodeList.containsKey(edgeInfo[1]) && nodeList.containsKey(edgeInfo[2])) {
                RoadWay newWay = new RoadWay(edgeInfo[0], nodeList.get(edgeInfo[1]), nodeList.get(edgeInfo[2]));
                ways.add(newWay);
            }
//            nodeDegree.put(edgeInfo[1],nodeDegree.get(edgeInfo[1])+1);
//            nodeDegree.put(edgeInfo[2],nodeDegree.get(edgeInfo[2])+1);
        }


        bwEdges.close();

        roadGraph.addNodes(nodes);
        roadGraph.addWays(ways);
        roadGraph.setMaxLat(maxLat);
        roadGraph.setMinLat(minLat);
        roadGraph.setMaxLon(maxLon);
        roadGraph.setMinLon(minLon);

        return roadGraph;
    }

    public RoadNetworkGraph readShapeCSV() throws JDOMException, IOException {

        double maxLat = -Double.MAX_VALUE, maxLon = -Double.MAX_VALUE;
        double minLat = Double.MAX_VALUE, minLon = Double.MAX_VALUE;        // boarder of the map
        List<RoadNode> nodes = new ArrayList<RoadNode>();
        List<RoadWay> ways = new ArrayList<RoadWay>();
        // read road nodes
        String line;
        BufferedReader brVertices = new BufferedReader(new FileReader(csvVerticesPath));
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
        }
        brVertices.close();

        // read road ways
        BufferedReader brEdges = new BufferedReader(new FileReader(csvEdgesPath));
        while ((line = brEdges.readLine()) != null) {
            RoadWay newWay = new RoadWay();
            List<RoadNode> intermediateNodes = new ArrayList<>();
            String[] edgeInfo = line.split("\\|");
            if (!edgeInfo[0].contains(",")) {
                boolean isCompleteRoad = true;
                for (int i = 1; i < edgeInfo.length; i++) {
                    String[] roadWayPoint = edgeInfo[i].split(",");
                    if (roadWayPoint.length == 3) {
                        RoadNode newNode = new RoadNode(roadWayPoint[0], Double.parseDouble(roadWayPoint[1]), Double.parseDouble(roadWayPoint[2]));

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
                    ways.add(newWay);
                }
            } else {
                System.out.println("Wrong road id info:" + edgeInfo[0]);
            }
        }
        brEdges.close();

        roadGraph.addNodes(nodes);
        roadGraph.addWays(ways);
        roadGraph.setMaxLat(maxLat);
        roadGraph.setMinLat(minLat);
        roadGraph.setMaxLon(maxLon);
        roadGraph.setMinLon(minLon);

        return roadGraph;
    }


}
