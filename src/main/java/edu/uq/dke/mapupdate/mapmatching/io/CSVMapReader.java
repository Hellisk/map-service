package edu.uq.dke.mapupdate.mapmatching.io;

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
}
