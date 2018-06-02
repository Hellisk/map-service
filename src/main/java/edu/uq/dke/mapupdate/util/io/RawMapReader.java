package edu.uq.dke.mapupdate.util.io;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import edu.uq.dke.mapupdate.util.object.roadnetwork.RoadNetworkGraph;
import edu.uq.dke.mapupdate.util.object.roadnetwork.RoadNode;
import edu.uq.dke.mapupdate.util.object.roadnetwork.RoadWay;
import org.geotools.data.FeatureSource;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;

/**
 * Created by uqpchao on 3/07/2017.
 */
public class RawMapReader {
    private final RoadNetworkGraph roadGraph;
    private final String generalPath;   // general path for old Beijing road map

    public RawMapReader(final String generalPath, final double[] boundingBox) {
        this.roadGraph = new RoadNetworkGraph();
        this.generalPath = generalPath;
        if (boundingBox.length == 4)
            this.roadGraph.setBoundingBox(boundingBox[0], boundingBox[1], boundingBox[2], boundingBox[3]);
    }

    /**
     * Read and parse the shape file.
     *
     * @return A Road Network Graph containing the
     * Nodes, Ways and Relations in the shape file.
     * @throws IOException File read failure
     */
    public RoadNetworkGraph readNewBeijingMap() throws IOException {
//        HashMap<String, String> coNodeMapping = new HashMap<>();  // map the co-node in an intersection to its main node

        // read vertices
        File vertexFile = new File(generalPath + "Nbeijing_point.shp");
        FileDataStore dataStoreVertex = FileDataStoreFinder.getDataStore(vertexFile);
        String typeName = dataStoreVertex.getTypeNames()[0];
        FeatureSource<SimpleFeatureType, SimpleFeature> vertexSource = dataStoreVertex
                .getFeatureSource(typeName);
        Filter filter = Filter.INCLUDE; // ECQL.toFilter("BBOX(THE_GEOM, 10,20,30,40)")
        FeatureCollection<SimpleFeatureType, SimpleFeature> vertexCollection = vertexSource.getFeatures(filter);

        // set boundary
        if (this.roadGraph.getMinLat() == -Double.MAX_VALUE && this.roadGraph.getMaxLat() == Double.MAX_VALUE && this.roadGraph.getMinLon()
                == -Double.MAX_VALUE && this.roadGraph.getMaxLon() == Double.MAX_VALUE) {
            ReferencedEnvelope bounds = vertexCollection.getBounds();
            this.roadGraph.setMinLon(bounds.getMinX());
            this.roadGraph.setMinLat(bounds.getMinY());
            this.roadGraph.setMaxLon(bounds.getMaxX());
            this.roadGraph.setMaxLat(bounds.getMaxY());
        }

        List<RoadNode> roadNodeList = new ArrayList<>();
        Map<String, RoadNode> id2Node = new HashMap<>();
        int roadNodeCount = 0;
        try (FeatureIterator<SimpleFeature> features = vertexCollection.features()) {
            while (features.hasNext()) {
                SimpleFeature feature = features.next();
                MultiPoint point = (MultiPoint) feature.getAttribute(0);
                // check whether the road node is inside the given rectangle
                if (isInside(point.getCoordinate().x, point.getCoordinate().y, roadGraph)) {
                    String pointID = feature.getAttribute(2).toString();
//                    if (feature.getAttribute(5).equals("1")) {
//                        coNodeMapping.put(pointID, (String) feature.getAttribute(8));
//                    }
                    RoadNode newRoadNode = new RoadNode(pointID, point.getCoordinate().x, point.getCoordinate().y);
                    roadNodeList.add(newRoadNode);
                    id2Node.put(newRoadNode.getId(), newRoadNode);
                    roadNodeCount++;
                }
            }
        }

        this.roadGraph.addNodes(roadNodeList);
        dataStoreVertex.dispose();

        // read edges
        File edgeFile = new File(generalPath + "Rbeijing_polyline.shp");
        FileDataStore dataStoreEdge = FileDataStoreFinder.getDataStore(edgeFile);
        String edgeTypeName = dataStoreEdge.getTypeNames()[0];
        FeatureSource<SimpleFeatureType, SimpleFeature> edgeSource = dataStoreEdge
                .getFeatureSource(edgeTypeName);
        FeatureCollection<SimpleFeatureType, SimpleFeature> edgeCollection = edgeSource.getFeatures(filter);

        List<RoadWay> roadWayList = new ArrayList<>();
        int roadWayPointID = 0;
        try (FeatureIterator<SimpleFeature> features = edgeCollection.features()) {
            while (features.hasNext()) {
                SimpleFeature feature = features.next();
                MultiLineString edges = (MultiLineString) feature.getAttribute(0);
                String edgeID = feature.getAttribute(2).toString();
                RoadWay newRoadWay = new RoadWay(edgeID);
                List<RoadNode> miniNode = new ArrayList<>();
                Coordinate[] coordinates = edges.getCoordinates();
                // the endpoints are not included in the current map
                if (!isInside(coordinates[0].x, coordinates[0].y, roadGraph) || !isInside(coordinates[coordinates.length - 1].x, coordinates[coordinates.length - 1].y, roadGraph))
                    continue;
                if (!id2Node.containsKey(feature.getAttribute(10).toString()) || !id2Node.containsKey(feature.getAttribute(11).toString())) {
                    System.out.println("ERROR! The endpoints of the input road way is not in the road node list!");
                    continue;
                }
                for (int i = 0; i < coordinates.length; i++) {
                    if (i == 0) {
                        miniNode.add(id2Node.get(feature.getAttribute(10).toString()));
                    } else if (i == coordinates.length - 1) {
                        miniNode.add(id2Node.get(feature.getAttribute(11).toString()));
                    } else {
                        miniNode.add(new RoadNode(roadWayPointID + "-", coordinates[i].x, coordinates[i].y));
                        roadWayPointID++;
                    }
                }
                switch (feature.getAttribute(6).toString()) {
                    case "0":
                    case "1":
                        newRoadWay.addNodes(miniNode);
                        roadWayList.add(newRoadWay);
                        RoadWay reverseRoad = new RoadWay("-" + edgeID);

                        reverseRoad.addNode(newRoadWay.getToNode());
                        for (int i = miniNode.size() - 2; i > 0; i--) {
                            RoadNode reverseNode = new RoadNode(roadWayPointID + "-", miniNode.get(i).lon(), miniNode.get(i).lat());
                            roadWayPointID++;
                            reverseRoad.addNode(reverseNode);
                        }
                        reverseRoad.addNode(newRoadWay.getFromNode());
                        roadWayList.add(reverseRoad);
                        break;
                    case "2":
                        newRoadWay.addNodes(miniNode);
                        roadWayList.add(newRoadWay);
                        break;
                    case "3":
                        for (int i = miniNode.size() - 1; i >= 0; i--) {
                            newRoadWay.addNode(miniNode.get(i));
                        }
                        roadWayList.add(newRoadWay);
                        break;
                    default:
                        System.out.println("Error direction number:" + feature.getAttribute(6).toString());
                        break;
                }
            }
        }
        this.roadGraph.addWays(roadWayList);
        System.out.println("Raw map read finish, total intersections:" + roadNodeCount + ", total road node points:" + roadWayPointID +
                ", total road ways: " + roadWayList.size());
        dataStoreEdge.dispose();

        return this.roadGraph;
    }

    public RoadNetworkGraph readOldBeijingMap() throws IOException {
        List<RoadNode> nodes = new ArrayList<>();
        List<RoadWay> ways = new ArrayList<>();
        Map<String, String> nodeID2Coordination = new HashMap<>();       // node ID and its coordination, format: lon_lat
        // read road nodes
        BufferedReader mapFileReader = new BufferedReader(new FileReader(generalPath + "Oldbeijing_map.txt"));
        int nodeSize = Integer.parseInt(mapFileReader.readLine());      // the total number of nodes in file
        DecimalFormat df = new DecimalFormat(".00000");
        for (int i = 0; i < nodeSize; i++) {
            String[] nodeInfo = mapFileReader.readLine().split("\t");
            if (nodeInfo.length == 3) {
                if (isInside(Double.parseDouble(nodeInfo[2]), Double.parseDouble(nodeInfo[1]), roadGraph)) {
                    RoadNode newNode = new RoadNode(nodeInfo[0], Double.parseDouble(df.format(Double.parseDouble(nodeInfo[2]))), Double
                            .parseDouble
                                    (df.format(Double.parseDouble(nodeInfo[1]))));
                    nodeID2Coordination.put(nodeInfo[0], df.format(Double.parseDouble(nodeInfo[2])) + "_" + df.format(Double.parseDouble
                            (nodeInfo[1])));
                    nodes.add(newNode);
                }
            } else
                throw new IOException("ERROR! read old beijing node failed: " + nodeInfo[0]);
        }
        String line = mapFileReader.readLine();
        int edgeSize;       // the total number of edges in file
        int miniNodeCount = 0;  // unique number for each intermediate node
        if (line.split("\t").length == 1)
            edgeSize = Integer.parseInt(line);
        else
            throw new IOException("ERROR! read old beijing edge failed: " + line);
        for (int i = 0; i < edgeSize; i++) {
            String lineOne = mapFileReader.readLine();
            String lineTwo = mapFileReader.readLine();
            String[] nodeInfo = lineOne.split("\t");
            if (nodeID2Coordination.containsKey(nodeInfo[0]) && nodeID2Coordination.containsKey(nodeInfo[1])) {
                List<RoadNode> nodeList = new ArrayList<>();
                String[] startPoint = nodeID2Coordination.get(nodeInfo[0]).split("_");
                String[] endPoint = nodeID2Coordination.get(nodeInfo[1]).split("_");
                nodeList.add(new RoadNode(nodeInfo[0], Double.parseDouble(startPoint[0]), Double.parseDouble(startPoint[1])));
                String[] intermediateNodes = lineTwo.split(",");
                for (String p : intermediateNodes) {
                    String[] interNodeInfo = p.split(" ");
                    if (interNodeInfo.length < 2)
                        System.out.println(p);
                    nodeList.add(new RoadNode(miniNodeCount + "-", Double.parseDouble(df.format(Double.parseDouble(interNodeInfo[1]))),
                            Double.parseDouble
                                    (df.format(Double.parseDouble(interNodeInfo[0])))));
                    miniNodeCount++;
                }
                nodeList.add(new RoadNode(nodeInfo[1], Double.parseDouble(endPoint[0]), Double.parseDouble(endPoint[1])));
                RoadWay newWay = new RoadWay(i + "");
                newWay.addNodes(nodeList);
                ways.add(newWay);
            }

        }
        this.roadGraph.addNodes(nodes);
        this.roadGraph.addWays(ways);
        mapFileReader.close();
        System.out.println("Old Beijing map read finish, total intersections: " + nodes.size() + ", total road node points:" +
                miniNodeCount + ", total road ways: " + ways.size());
        return roadGraph;
    }

    private boolean isInside(double pointX, double pointY, RoadNetworkGraph roadGraph) {
        if (roadGraph.hasBoundary())
            return pointX > roadGraph.getMinLon() && pointX < roadGraph.getMaxLon() && pointY > roadGraph.getMinLat() && pointY < roadGraph.getMaxLat();
        else return true;
    }
}