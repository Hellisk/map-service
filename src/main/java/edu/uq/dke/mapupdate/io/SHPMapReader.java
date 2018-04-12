package edu.uq.dke.mapupdate.io;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import org.geotools.data.FeatureSource;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import traminer.util.map.roadnetwork.RoadNetworkGraph;
import traminer.util.map.roadnetwork.RoadNode;
import traminer.util.map.roadnetwork.RoadWay;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by uqpchao on 3/07/2017.
 */
public class SHPMapReader {
    private final RoadNetworkGraph roadGraph;
    private final String shpVerticesPath;
    private final String shpEdgesPath;


    public SHPMapReader(final String shpVertexPath, final String shpEdgePath) {
        this.roadGraph = new RoadNetworkGraph();
        this.shpVerticesPath = shpVertexPath;
        this.shpEdgesPath = shpEdgePath;

//        // preset bounds to reduce the map size, si huan
//        this.roadGraph.setBoundingBox(116.20, 116.57, 39.76, 40.03);
//
        // preset bounds to reduce the map size, er huan
        this.roadGraph.setBoundingBox(116.35, 116.44, 39.895, 39.95);
//
//        // preset bounds to reduce the map size, smaller than er huan
//        this.roadGraph.setBoundingBox(116.405423, 116.433773, 39.957972, 39.978720);
    }

    /**
     * Read and parse the shape file.
     *
     * @return A Road Network Graph containing the
     * Nodes, Ways and Relations in the shape file.
     * @throws IOException File read failure
     */
    public RoadNetworkGraph readSHP() throws IOException {
//        HashMap<String, String> coNodeMapping = new HashMap<>();  // map the co-node in an intersection to its main node

        // read vertices
        File vertexFile = new File(shpVerticesPath);
        FileDataStore dataStoreVertex = FileDataStoreFinder.getDataStore(vertexFile);
        String typeName = dataStoreVertex.getTypeNames()[0];
        FeatureSource<SimpleFeatureType, SimpleFeature> vertexSource = dataStoreVertex
                .getFeatureSource(typeName);
        Filter filter = Filter.INCLUDE; // ECQL.toFilter("BBOX(THE_GEOM, 10,20,30,40)")
        FeatureCollection<SimpleFeatureType, SimpleFeature> vertexCollection = vertexSource.getFeatures(filter);

//        // set boundary
//        ReferencedEnvelope bounds = vertexCollection.getBounds();
//        this.roadGraph.setMinLon(bounds.getMinX());
//        this.roadGraph.setMinLat(bounds.getMinY());
//        this.roadGraph.setMaxLon(bounds.getMaxX());
//        this.roadGraph.setMaxLat(bounds.getMaxY());

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
        File edgeFile = new File(shpEdgesPath);
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
        System.out.println("Raw map read finish, total intersections:" + roadNodeCount + ", total road node points:" + roadWayPointID);
        dataStoreEdge.dispose();

        return this.roadGraph;
    }

    private boolean isInside(double pointX, double pointY, RoadNetworkGraph roadGraph) {
        if (roadGraph.hasBoundary())
            return pointX > roadGraph.getMinLon() && pointX < roadGraph.getMaxLon() && pointY > roadGraph.getMinLat() && pointY < roadGraph.getMaxLat();
        else return true;
    }
}