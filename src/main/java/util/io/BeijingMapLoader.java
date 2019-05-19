package util.io;

import org.apache.log4j.Logger;
import org.geotools.data.FeatureSource;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import util.function.DistanceFunction;
import util.function.GreatCircleDistanceFunction;
import util.object.roadnetwork.RoadNetworkGraph;
import util.object.roadnetwork.RoadNode;
import util.object.roadnetwork.RoadWay;
import util.object.spatialobject.Point;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Load the Beijing map from the SHP files and convert it into a <tt>RoadNetworkGraph</tt>.
 *
 * @author Hellisk
 * @since 3/07/2017
 */
public class BeijingMapLoader {
	
	private static final Logger LOG = Logger.getLogger(Point.class);
	private final RoadNetworkGraph roadGraph;
	private final String mapFolder;   // general path for old Beijing road map
	private final DistanceFunction distFunc = new GreatCircleDistanceFunction();
	
	public BeijingMapLoader(final String mapFolder) {
		this.roadGraph = new RoadNetworkGraph(false, distFunc);
		this.mapFolder = mapFolder;
	}
	
	/**
	 * Remove the prefix of the point ID of connection node. A connection node is a node which resides on the boarder of a map tile. Each
	 * connection node has two(can be four) sub-nodes with the same coordinates but in different adjacent tiles. A connection node is not an
	 * intersection. This function removes the prefix and make two sub-nodes connected.
	 *
	 * @param pointID           Original point ID.
	 * @param id2ConnectionNode Map of the connection node ID and its adjacent roads(empty at this stage).
	 * @return The refined point ID.
	 */
	// TODO check whether there is connection node with four sub-nodes.
	private static String pointIDConverter(String pointID, HashMap<String, List<String>> id2ConnectionNode) {
		String refinedPointID;
		if (pointID.length() > 10 && (pointID.contains("10000") || pointID.contains("20000"))) {
			refinedPointID = pointID.substring(5);
			id2ConnectionNode.putIfAbsent(refinedPointID, new ArrayList<>());
			return refinedPointID;
		} else if (pointID.length() > 10 && (pointID.substring(0, 4).equals("1000") || pointID.substring(0, 4).equals("2000"))) {
			refinedPointID = pointID.substring(4);
			id2ConnectionNode.putIfAbsent(refinedPointID, new ArrayList<>());
			return refinedPointID;
		} else if (pointID.length() > 10 && (pointID.substring(0, 3).equals("100") || pointID.substring(0, 3).equals("200"))) {
			refinedPointID = pointID.substring(3);
			id2ConnectionNode.putIfAbsent(refinedPointID, new ArrayList<>());
			return refinedPointID;
		}
		return pointID;
	}
	
	/**
	 * Remove the prefix of the point ID of connection node and update its adjacent roads.
	 *
	 * @param pointID           Original point ID.
	 * @param edgeID            The road way ID which contains the point
	 * @param id2ConnectionNode Map of the connection node ID and its adjacent roads.
	 * @return The refined point ID.
	 */
	private static String pointIDFinder(String pointID, String edgeID, HashMap<String, List<String>> id2ConnectionNode) {
		String refinedPointID;
		if (pointID.length() > 10 && (pointID.contains("10000") || pointID.contains("20000"))) {
			refinedPointID = pointID.substring(5);
			connectionNodeFinder(edgeID, id2ConnectionNode, refinedPointID);
			return refinedPointID;
		} else if (pointID.length() > 10 && (pointID.substring(0, 4).equals("1000") || pointID.substring(0, 4).equals("2000"))) {
			refinedPointID = pointID.substring(4);
			connectionNodeFinder(edgeID, id2ConnectionNode, refinedPointID);
			return refinedPointID;
		} else if (pointID.length() > 10 && (pointID.substring(0, 3).equals("100") || pointID.substring(0, 3).equals("200"))) {
			refinedPointID = pointID.substring(3);
			connectionNodeFinder(edgeID, id2ConnectionNode, refinedPointID);
			return refinedPointID;
		}
		return pointID;
	}
	
	/**
	 * Attach the edge ID to the corresponding connection node given the refined node ID.
	 *
	 * @param edgeID            The ID of the connected edge
	 * @param id2ConnectionNode The mapping between connection node and the edges connect to it.
	 * @param refinedPointID    The refined ID of the connection node.
	 */
	private static void connectionNodeFinder(String edgeID, HashMap<String, List<String>> id2ConnectionNode, String refinedPointID) {
		if (id2ConnectionNode.containsKey(refinedPointID)) {
			id2ConnectionNode.get(refinedPointID).add(edgeID);
		} else {
			System.err.println("The connection node " + refinedPointID + " is not found.");
		}
	}
	
	/**
	 * Read and parse the shape file of Beijing road map. This road map contains the following features
	 * 1. Many intersections are compounded by multiple sub-nodes, each pair of which is connected by a tiny edge
	 * 2. The edge connected to the sub node is recorded in the adjacent list of both the primary node and the sub node
	 * 3. The edge connecting the sub node and its primary node is only recorded in the sub node and does not appear in primary node
	 * 4. The link number doesn't contain -edgeID, but appears in matching results
	 * 5. Non-intersection nodes may have multiple adjacent edges
	 * <p>
	 * The node of the output map includes the id, coordinates and the node type, the road ways contains
	 *
	 * @return A Road Network Graph containing the
	 * Nodes, Ways and Relations in the shape file.
	 * @throws IOException File read failure
	 */
	public RoadNetworkGraph loadRawMap() throws IOException {
//        HashMap<String, String> coNodeMapping = new HashMap<>();  // map the co-node in an intersection to its main node
		
		// read vertices
		File vertexFile = new File(mapFolder + "Nbeijing_point.shp");
		FileDataStore dataStoreVertex = FileDataStoreFinder.getDataStore(vertexFile);
		String typeName = dataStoreVertex.getTypeNames()[0];
		FeatureSource<SimpleFeatureType, SimpleFeature> vertexSource = dataStoreVertex.getFeatureSource(typeName);
		Filter filter = Filter.INCLUDE; // ECQL.toFilter("BBOX(THE_GEOM, 10,20,30,40)")
		FeatureCollection<SimpleFeatureType, SimpleFeature> vertexCollection = vertexSource.getFeatures(filter);
		
		// set boundary
		if (this.roadGraph.getMinLat() == Double.NEGATIVE_INFINITY && this.roadGraph.getMaxLat() == Double.POSITIVE_INFINITY && this
				.roadGraph.getMinLon() == Double.NEGATIVE_INFINITY && this.roadGraph.getMaxLon() == Double.POSITIVE_INFINITY) {
			ReferencedEnvelope bounds = vertexCollection.getBounds();
			this.roadGraph.setMinLon(bounds.getMinX());
			this.roadGraph.setMinLat(bounds.getMinY());
			this.roadGraph.setMaxLon(bounds.getMaxX());
			this.roadGraph.setMaxLat(bounds.getMaxY());
		} else
			LOG.error("The raw map has a preset boundary.");
		
		// start reading nodes
		List<RoadNode> roadNodeList = new ArrayList<>();
		Map<String, RoadNode> id2Node = new HashMap<>();
		HashMap<String, List<String>> id2ConnectionNode = new HashMap<>();
		try (FeatureIterator<SimpleFeature> features = vertexCollection.features()) {
			while (features.hasNext()) {
				SimpleFeature feature = features.next();
				MultiPoint point = (MultiPoint) feature.getAttribute(0);
				// check whether the road node is inside the given bounding box
				if (roadGraph.getBoundary().contains(point.getCoordinate().x, point.getCoordinate().y)) {
					String pointID = feature.getAttribute(2).toString();
					short nodeType = Short.parseShort(feature.getAttribute(5).toString());
					pointID = pointIDConverter(pointID, id2ConnectionNode);
					RoadNode newRoadNode = new RoadNode(pointID, point.getCoordinate().x, point.getCoordinate().y, nodeType, distFunc);
					insertNode(roadNodeList, id2Node, pointID, newRoadNode);
				} else
					LOG.error("The given boundary of the raw map cannot enclose all nodes.");
			}
		} catch (IllegalThreadStateException e) {
			e.printStackTrace();
		}
		this.roadGraph.addNodes(roadNodeList);
		dataStoreVertex.dispose();
		
		// start reading edges
		File edgeFile = new File(mapFolder + "Rbeijing_polyline.shp");
		FileDataStore dataStoreEdge = FileDataStoreFinder.getDataStore(edgeFile);
		String edgeTypeName = dataStoreEdge.getTypeNames()[0];
		FeatureSource<SimpleFeatureType, SimpleFeature> edgeSource = dataStoreEdge
				.getFeatureSource(edgeTypeName);
		FeatureCollection<SimpleFeatureType, SimpleFeature> edgeCollection = edgeSource.getFeatures(filter);
		
		List<RoadWay> roadWayList = new ArrayList<>();
		Map<String, Integer> roadTypeDictionary = new HashMap<>();
		initDictionary(roadTypeDictionary);
		int roadWayPointID = 0;
		try (FeatureIterator<SimpleFeature> features = edgeCollection.features()) {
			while (features.hasNext()) {
				SimpleFeature feature = features.next();
				MultiLineString edges = (MultiLineString) feature.getAttribute(0);
				String edgeID = feature.getAttribute(2).toString();
				RoadWay newRoadWay = new RoadWay(edgeID, distFunc);
				int numOfType = Integer.parseInt(feature.getAttribute(3).toString());       // read number of road type code
				if (numOfType == 1) {
					String roadLevel = feature.getAttribute(4).toString().substring(0, 2);
					String roadType = feature.getAttribute(4).toString().substring(2);
					roadLevelConverter(newRoadWay, roadLevel);
					if (roadTypeDictionary.containsKey(roadType))
						newRoadWay.setWayTypeBit(roadTypeDictionary.get(roadType));
					// check whether the current road is for pedestrian, cycling or bus, if so, ignore it
					if (newRoadWay.getWayLevel() == 7 || newRoadWay.getWayLevel() == 9 || Objects.requireNonNull(newRoadWay.getWayType()).get(9)
							|| newRoadWay.getWayType().get(10))
						continue;
				} else if (numOfType > 1) {
					String[] roadTypeList = feature.getAttribute(4).toString().split("\\|");
					String roadLevel = roadTypeList[0].substring(0, 2);
					roadLevelConverter(newRoadWay, roadLevel);
					for (int i = 0; i < numOfType; i++) {
						if (roadTypeDictionary.containsKey(roadTypeList[i].substring(2)))
							newRoadWay.setWayTypeBit(roadTypeDictionary.get(roadTypeList[i].substring(2)));
						else LOG.error("Incorrect road type: " + roadTypeList[i].substring(2));
					}
					// check whether the current road is for pedestrian, cycling or bus, if so, ignore it
					if (newRoadWay.getWayLevel() == 7 || newRoadWay.getWayLevel() == 9 || Objects.requireNonNull(newRoadWay.getWayType()).get(9)
							|| newRoadWay.getWayType().get(10))
						continue;
				} else
					LOG.error("Incorrect number of road types.");
				
				List<RoadNode> miniNode = new ArrayList<>();
				Coordinate[] coordinates = edges.getCoordinates();
				// the endpoints are not included in the current map
				if (!roadGraph.getBoundary().contains(coordinates[0].x, coordinates[0].y) ||
						!roadGraph.getBoundary().contains(coordinates[coordinates.length - 1].x, coordinates[coordinates.length - 1].y))
					continue;
				boolean isCompleteRoad = true;
				for (int i = 0; i < coordinates.length; i++) {
					if (i == 0) {   // start point of road
						String pointID = feature.getAttribute(10).toString();
						isCompleteRoad = insertMiniNode(id2Node, id2ConnectionNode, edgeID, miniNode, pointID);
					} else if (i == coordinates.length - 1) {   // end point of road
						String pointID = feature.getAttribute(11).toString();
						isCompleteRoad = isCompleteRoad && insertMiniNode(id2Node, id2ConnectionNode, edgeID, miniNode, pointID);
					} else {
						miniNode.add(new RoadNode(roadWayPointID + "-", coordinates[i].x, coordinates[i].y, distFunc));
						roadWayPointID++;
					}
				}
				
				if (!isCompleteRoad)
					continue;
				
				switch (feature.getAttribute(6).toString()) {
					case "0":
					case "1": {
						newRoadWay.addNodes(miniNode);
						roadWayList.add(newRoadWay);
						RoadWay reverseRoad = new RoadWay("-" + edgeID, distFunc);
						reverseRoad.setWayLevel(newRoadWay.getWayLevel());
						reverseRoad.setWayType(newRoadWay.getWayType());
						reverseRoad.addNode(newRoadWay.getToNode());
						for (int i = miniNode.size() - 2; i > 0; i--) {
							RoadNode reverseNode = new RoadNode(roadWayPointID + "-", miniNode.get(i).lon(), miniNode.get(i).lat(), distFunc);
							roadWayPointID++;
							reverseRoad.addNode(reverseNode);
						}
						reverseRoad.addNode(newRoadWay.getFromNode());
						roadWayList.add(reverseRoad);
						break;
					}
					case "2": {
						newRoadWay.addNodes(miniNode);
						roadWayList.add(newRoadWay);
						break;
					}
					case "3": {
						for (int i = miniNode.size() - 1; i >= 0; i--)
							newRoadWay.addNode(miniNode.get(i));
						roadWayList.add(newRoadWay);
						break;
					}
					default: {
						LOG.error("The direction indicator number is wrong: " + feature.getAttribute(6).toString());
						break;
					}
				}
			}
		} catch (IllegalThreadStateException e) {
			e.printStackTrace();
		}
		this.roadGraph.addWays(roadWayList);
		long roadLength = 0;
		for (RoadWay w : this.roadGraph.getWays()) {
			roadLength += w.getLength();
		}
		
		int removedNodeCount = roadGraph.isolatedNodeRemoval();
		this.roadGraph.updateBoundary();
		LOG.info("Raw map read finish, " + removedNodeCount + " nodes are removed due to no edges connected. Total " +
				"intersections:" + roadGraph.getNodes().size() + ", total intermediate road node points:" + roadWayPointID +
				", total road ways: " + roadWayList.size() + ", average road way length: " + roadLength / roadGraph.getWays().size());
		dataStoreEdge.dispose();
		
		return this.roadGraph;
	}
	
	private boolean insertMiniNode(Map<String, RoadNode> id2Node, HashMap<String, List<String>> id2ConnectionNode, String edgeID, List<RoadNode> miniNode, String pointID) {
		pointID = pointIDFinder(pointID, edgeID, id2ConnectionNode);
		if (!id2Node.containsKey(pointID)) {
			LOG.error("Input road node is missing.");
			return false;
		}
		miniNode.add(id2Node.get(pointID));
		return true;
	}
	
	static void insertNode(List<RoadNode> roadNodeList, Map<String, RoadNode> id2Node, String pointID, RoadNode newRoadNode) {
		if (!id2Node.containsKey(pointID)) {
			id2Node.put(pointID, newRoadNode);
			roadNodeList.add(newRoadNode);
		} else {
			RoadNode existingNode = id2Node.get(pointID);
			if (existingNode.lon() != newRoadNode.lon() || existingNode.lat() != newRoadNode.lat())
				LOG.error("The same road node has different location");
//            else
//                LOG.warning("WARNING! Same node occurred multiple times: " + pointID);
		}
	}
	
	private void roadLevelConverter(RoadWay newRoadWay, String roadLevel) {
		switch (roadLevel) {
			case "0a":
				newRoadWay.setWayLevel((short) 5);
				break;
			case "0b":
				newRoadWay.setWayLevel((short) 7);
				break;
			default:
				newRoadWay.setWayLevel(Short.parseShort(roadLevel));
				break;
		}
	}
	
	/**
	 * Initialize the road type dictionary. The dictionary is used for road type code conversion.
	 *
	 * @param roadTypeDictionary Dictionary to be initialized
	 */
	private void initDictionary(Map<String, Integer> roadTypeDictionary) {
		roadTypeDictionary.put("00", 0);
		roadTypeDictionary.put("01", 1);
		roadTypeDictionary.put("02", 2);
		roadTypeDictionary.put("03", 3);
		roadTypeDictionary.put("04", 4);
		roadTypeDictionary.put("05", 5);
		roadTypeDictionary.put("06", 6);
		roadTypeDictionary.put("07", 7);
		roadTypeDictionary.put("08", 8);
		roadTypeDictionary.put("09", 9);
		roadTypeDictionary.put("11", 10);
		roadTypeDictionary.put("12", 11);
		roadTypeDictionary.put("13", 12);
		roadTypeDictionary.put("14", 13);
		roadTypeDictionary.put("15", 14);
		roadTypeDictionary.put("16", 15);
		roadTypeDictionary.put("17", 16);
		roadTypeDictionary.put("18", 17);
		roadTypeDictionary.put("19", 18);
		roadTypeDictionary.put("0a", 19);
		roadTypeDictionary.put("0b", 20);
		roadTypeDictionary.put("0c", 21);
		roadTypeDictionary.put("0d", 22);
		roadTypeDictionary.put("0e", 23);
		roadTypeDictionary.put("0f", 24);
	}
}