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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
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

        // preset bounds to reduce the map size
        this.roadGraph.setMaxLat(40.00);
        this.roadGraph.setMinLat(39.84);
        this.roadGraph.setMaxLon(116.47);
        this.roadGraph.setMinLon(116.3);
    }

    /**
     * Read and parse the shape file.
     *
     * @return A Road Network Graph containing the
     * Nodes, Ways and Relations in the shape file.
     * @throws JDOMException
     * @throws IOException
     */
    public RoadNetworkGraph readSHP() throws JDOMException, IOException {
        HashMap<String, String> pointIDMap = new HashMap<>();
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
        Map<String, String> duplicateIDMap = new HashMap<>();
        int roadNodeCount = 0;
        int duplicatedPointCount = 0;
        try (FeatureIterator<SimpleFeature> features = vertexCollection.features()) {
            while (features.hasNext()) {
                SimpleFeature feature = features.next();
                MultiPoint point = (MultiPoint) feature.getAttribute(0);
                // check whether the road node is inside the given rectangle
                if (isInside(point.getCoordinate().x, point.getCoordinate().y, roadGraph)) {
                    String pointID = feature.getAttribute(2).toString();
                    if (!pointIDMap.containsKey(point.getCoordinate().x + "_" + point.getCoordinate().y)) {
                        pointIDMap.put(point.getCoordinate().x + "_" + point.getCoordinate().y, pointID);
                        roadNodeCount++;
                        RoadNode newRoadNode = new RoadNode(pointID, point.getCoordinate().x, point.getCoordinate().y);
                        roadNodeList.add(newRoadNode);
                    } else {
//                    System.out.println("Duplicated point");
                        if (!pointID.equals(pointIDMap.get(point.getCoordinate().x + "_" + point.getCoordinate().y))) {
//                            System.out.println("Same point with different IDs:" + pointID + "," + pointIDMap.get(point.getCoordinate().x + "_" + point.getCoordinate().y));
                            duplicateIDMap.put(pointID, pointIDMap.get(point.getCoordinate().x + "_" + point.getCoordinate().y));
                            duplicatedPointCount++;
                        }
                    }
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
        int doubleDirectionCount = 0;
        try (FeatureIterator<SimpleFeature> features = edgeCollection.features()) {
            while (features.hasNext()) {
                SimpleFeature feature = features.next();
                MultiLineString edges = (MultiLineString) feature.getAttribute(0);
                String edgeID = feature.getAttribute(2).toString();
                RoadWay newRoadWay = new RoadWay(edgeID);
                List<RoadNode> intermediateNode = new ArrayList<>();
                int currPointID = roadWayPointID;
                boolean isIncluded = true;
                for (Coordinate e : edges.getCoordinates()) {
                    if (isInside(e.x, e.y, roadGraph)) {
                        intermediateNode.add(new RoadNode(roadWayPointID + "", e.x, e.y));
                        roadWayPointID++;
                    } else {
                        isIncluded = false;
                        break;
                    }
                }
                if (isIncluded) {
                    switch (feature.getAttribute(6).toString()) {
                        case "0":
                        case "1":
                            newRoadWay.addNodes(intermediateNode);
                            roadWayList.add(newRoadWay);
                            RoadWay reverseRoad = new RoadWay("-" + edgeID);
                            for (int i = intermediateNode.size() - 1; i >= 0; i--) {
                                reverseRoad.addNode(intermediateNode.get(i));
                            }
                            doubleDirectionCount++;
                            break;
                        case "2":
                            newRoadWay.addNodes(intermediateNode);
                            roadWayList.add(newRoadWay);
                            break;
                        case "3":
                            for (int i = intermediateNode.size() - 1; i >= 0; i--) {
                                newRoadWay.addNode(intermediateNode.get(i));
                                newRoadWay.setId("-" + edgeID);
                            }
                            roadWayList.add(newRoadWay);
                            break;
                        default:
                            System.out.println("Error direction number:" + feature.getAttribute(6).toString());
                            break;
                    }
                } else {
                    roadWayPointID = currPointID;
//                    System.out.println("Road way excluded");
                }
            }
        }
        System.out.println("Total road node count:" + roadNodeCount);
        System.out.println("Total included road way points:" + roadWayPointID);
        System.out.println("Total duplicated points:" + duplicatedPointCount);
        System.out.println("doubleDirectionCount = " + doubleDirectionCount);
        this.roadGraph.addWays(roadWayList);
        dataStoreEdge.dispose();

        BufferedWriter br = new BufferedWriter(new FileWriter(shpVerticesPath.substring(0, shpVerticesPath.lastIndexOf('/')) + "DuplicateIDMapping.txt"));
        br.write("Original_ID, Mapped_ID\n");
        for (Map.Entry<String, String> s : duplicateIDMap.entrySet()) {
            br.write(s.getKey() + "," + s.getValue() + "\n");
        }
        br.close();
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
