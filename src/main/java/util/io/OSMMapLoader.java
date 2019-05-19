package util.io;

import org.apache.log4j.Logger;
import util.function.DistanceFunction;
import util.function.EuclideanDistanceFunction;
import util.object.roadnetwork.RoadNetworkGraph;
import util.object.roadnetwork.RoadNode;
import util.object.roadnetwork.RoadWay;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static util.io.BeijingMapLoader.insertNode;

/**
 * The OSMMapLoader read the map from UTM - Universal Transverse Mercator coordination. The X and Y are measured in metre.
 *
 * @author Hellisk
 * @since 17/05/2019
 */
public class OSMMapLoader {
	
	private static final Logger LOG = Logger.getLogger(OSMMapLoader.class);
	
	private final String mapFolder;
	private final DistanceFunction distFunc = new EuclideanDistanceFunction();
	
	public OSMMapLoader(String mapFolder) {
		this.mapFolder = mapFolder;
	}
	
	public RoadNetworkGraph loadRawMap(String cityName) {
		RoadNetworkGraph roadGraph = new RoadNetworkGraph(false, distFunc);
		List<RoadNode> nodeList = new ArrayList<>();
		Map<String, RoadNode> id2Node = new HashMap<>();
		List<RoadWay> wayList = new ArrayList<>();
		// read road nodes
		List<String> nodeLines = IOService.readFile(this.mapFolder + cityName.toLowerCase() + "_vertices_osm.txt");
		for (String line : nodeLines) {
			String[] nodeInfo = line.split(",");
			double x = Double.parseDouble(nodeInfo[1]);
			double y = Double.parseDouble(nodeInfo[2]);
			RoadNode newRoadNode = new RoadNode(nodeInfo[0], x, y, distFunc);
			insertNode(nodeList, id2Node, nodeInfo[0], newRoadNode);
		}
		
		// read road ways
		List<String> edgeLines = IOService.readFile(this.mapFolder + cityName.toLowerCase() + "_edges_osm.txt");
		for (String line : edgeLines) {
			RoadWay newWay = new RoadWay(distFunc);
			List<RoadNode> miniNodeList = new ArrayList<>();
			String[] edgeInfo = line.split(",");
			if (id2Node.containsKey(edgeInfo[1]) && id2Node.containsKey(edgeInfo[2])) {
				miniNodeList.add(id2Node.get(edgeInfo[1]));
				miniNodeList.add(id2Node.get(edgeInfo[2]));
				newWay.setId(edgeInfo[0]);
				newWay.setNodes(miniNodeList);
				wayList.add(newWay);
				// insert reverse road
				List<RoadNode> reverseNodeList = new ArrayList<>();
				RoadWay reverseWay = new RoadWay(distFunc);
				reverseNodeList.add(miniNodeList.get(1));
				reverseNodeList.add(miniNodeList.get(0));
				reverseWay.setId("-" + edgeInfo[0]);
				reverseWay.setNodes(reverseNodeList);
				wayList.add(reverseWay);
			} else {
				LOG.warn("Road endpoint doesn't exist: " + edgeInfo[0] + "," + edgeInfo[1]);
			}
		}
		
		roadGraph.addNodes(nodeList);
		roadGraph.addWays(wayList);
		int removedNodeCount = roadGraph.isolatedNodeRemoval();
		roadGraph.updateBoundary();
		LOG.info("Raw map read finish, " + removedNodeCount + " nodes are removed due to no edges connected. Total " +
				"intersections:" + roadGraph.getNodes().size() + ", total road ways: " + roadGraph.getWays().size() + ".");
		LOG.info("Map boundary is: " + roadGraph.getMinLon() + "," + roadGraph.getMaxLon() + "," + roadGraph.getMinLat() + "," + roadGraph.getMaxLat());
		return roadGraph;
	}
}