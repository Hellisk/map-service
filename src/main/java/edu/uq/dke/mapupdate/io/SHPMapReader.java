package edu.uq.dke.mapupdate.io;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import org.geotools.data.FeatureSource;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.jdom2.JDOMException;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import traminer.util.map.roadnetwork.RoadNetworkGraph;
import traminer.util.map.roadnetwork.RoadNode;
import traminer.util.map.roadnetwork.RoadWay;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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

//        // preset bounds to reduce the map size, er huan
//        this.roadGraph.setMaxLat(39.95);
//        this.roadGraph.setMinLat(39.895);
//        this.roadGraph.setMaxLon(116.44);
//        this.roadGraph.setMinLon(116.35);

        // preset bounds to reduce the map size, particular area
        this.roadGraph.setMaxLat(39.978720);
        this.roadGraph.setMinLat(39.957972);
        this.roadGraph.setMaxLon(116.433773);
        this.roadGraph.setMinLon(116.405423);
    }

    /**
     * Read and parse the shape file.
     *
     * @return A Road Network Graph containing the
     * Nodes, Ways and Relations in the shape file.
     * @throws JDOMException
     * @throws IOException
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
                    roadNodeCount++;
                    RoadNode newRoadNode = new RoadNode(pointID, point.getCoordinate().x, point.getCoordinate().y);
                    roadNodeList.add(newRoadNode);
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
                if (!isInside(coordinates[0].x, coordinates[0].y, roadGraph) || !isInside(coordinates[coordinates.length - 1].x, coordinates[coordinates.length - 1].y, roadGraph))
                    continue;
                for (Coordinate e : coordinates) {
                    miniNode.add(new RoadNode(roadWayPointID + "-", e.x, e.y));
                    roadWayPointID++;
                }
                switch (feature.getAttribute(6).toString()) {
                    case "0":
                    case "1":
                        newRoadWay.addNodes(miniNode);
                        newRoadWay.getNode(0).setId((String) feature.getAttribute(10));
                        newRoadWay.getNode(newRoadWay.getNodes().size() - 1).setId((String) feature.getAttribute(11));
                        roadWayList.add(newRoadWay);
                        RoadWay reverseRoad = new RoadWay("-" + edgeID);
                        for (int i = miniNode.size() - 1; i >= 0; i--) {
                            RoadNode reverseNode = new RoadNode(roadWayPointID + "-", miniNode.get(i).lon(), miniNode.get(i).lat());
                            roadWayPointID++;
                            reverseRoad.addNode(reverseNode);
                        }
                        reverseRoad.getNode(0).setId((String) feature.getAttribute(11));
                        reverseRoad.getNode(newRoadWay.getNodes().size() - 1).setId((String) feature.getAttribute(10));
                        roadWayList.add(reverseRoad);
                        break;
                    case "2":
                        newRoadWay.addNodes(miniNode);
                        newRoadWay.getNode(0).setId((String) feature.getAttribute(10));
                        newRoadWay.getNode(newRoadWay.getNodes().size() - 1).setId((String) feature.getAttribute(11));
                        roadWayList.add(newRoadWay);
                        break;
                    case "3":
                        for (int i = miniNode.size() - 1; i >= 0; i--) {
                            newRoadWay.addNode(miniNode.get(i));
                        }
                        newRoadWay.getNode(0).setId((String) feature.getAttribute(11));
                        newRoadWay.getNode(newRoadWay.getNodes().size() - 1).setId((String) feature.getAttribute(10));
                        roadWayList.add(newRoadWay);
                        break;
                    default:
                        System.out.println("Error direction number:" + feature.getAttribute(6).toString());
                        break;
                }
            }
        }
        System.out.println("Raw map read finish, total nodes:" + roadNodeCount + ", total road way points:" + roadWayPointID);
        this.roadGraph.addWays(roadWayList);
        dataStoreEdge.dispose();

        return this.roadGraph;
    }

    private boolean isInside(double pointX, double pointY, RoadNetworkGraph roadGraph) {
        boolean inside = false;
        if (pointX > roadGraph.getMinLon() && pointX < roadGraph.getMaxLon())
            if (pointY > roadGraph.getMinLat() && pointY < roadGraph.getMaxLat())
                inside = true;
        return inside;
    }
}