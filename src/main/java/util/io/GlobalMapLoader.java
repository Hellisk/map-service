package util.io;

import org.apache.log4j.Logger;
import util.function.DistanceFunction;
import util.function.GreatCircleDistanceFunction;
import util.object.roadnetwork.RoadNetworkGraph;
import util.object.roadnetwork.RoadNode;
import util.object.roadnetwork.RoadWay;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import static util.io.GlobalTrajectoryLoader.stringFormatter;

public class GlobalMapLoader {
	
	private static final Logger LOG = Logger.getLogger(GlobalMapLoader.class);
	
	private final String mapFolder;
	private final DistanceFunction distFunc = new GreatCircleDistanceFunction();
	
	public GlobalMapLoader(String mapFolder) {
		this.mapFolder = mapFolder;
	}
	
	public RoadNetworkGraph readRawMap(int trajNum) throws IOException {
		double maxLat = Double.NEGATIVE_INFINITY, maxLon = Double.NEGATIVE_INFINITY;
		double minLat = Double.POSITIVE_INFINITY, minLon = Double.POSITIVE_INFINITY;        // boarder of the map
		RoadNetworkGraph roadGraph = new RoadNetworkGraph(false, distFunc);
		List<RoadNode> nodes = new ArrayList<>();
		Map<String, RoadNode> index2NodeMapping = new HashMap<>();
		Map<String, String> loc2NodeMapping = new HashMap<>();
		Map<String, String> duplicatePointMapping = new HashMap<>();
		List<RoadWay> ways = new ArrayList<>();
		// read road nodes
		String line;
		int lineCount = 0;
		BufferedReader brVertices = new BufferedReader(new FileReader(this.mapFolder + stringFormatter
				(trajNum) + File.separator + stringFormatter(trajNum) + ".nodes"));
		while ((line = brVertices.readLine()) != null) {
			String[] nodeInfo = line.split("\t");
			double lon = Double.parseDouble(nodeInfo[0]);
			double lat = Double.parseDouble(nodeInfo[1]);
			
			RoadNode newNode = new RoadNode(lineCount + "", lon, lat, distFunc);
			if (!loc2NodeMapping.containsKey(lon + "_" + lat)) {
				loc2NodeMapping.put(lon + "_" + lat, newNode.getID());
				index2NodeMapping.put(newNode.getID(), newNode);
				nodes.add(newNode);
			} else {
				duplicatePointMapping.put(newNode.getID(), loc2NodeMapping.get(lon + "_" + lat));
				LOG.debug("The current node location already exist, redirect to existing node: " + lon + "_" + lat + "," + newNode.getID());
			}
			lineCount++;
		}
		brVertices.close();
		
		// read road ways
		int roadCount = 0;
		BufferedReader brEdges = new BufferedReader(new FileReader(this.mapFolder + stringFormatter
				(trajNum) + File.separator + stringFormatter(trajNum) + ".arcs"));
		Set<String> existingRoadIndexSet = new HashSet<>();
		while ((line = brEdges.readLine()) != null) {
			RoadWay newWay = new RoadWay(distFunc);
			List<RoadNode> miniNode = new ArrayList<>();
			String[] edgeInfo = line.split("\t");
			String startNodeID = edgeInfo[0];
			String endNodeID = edgeInfo[1];
			if (!index2NodeMapping.containsKey(startNodeID)) {
				if (!duplicatePointMapping.containsKey(startNodeID))
					throw new IllegalArgumentException("Cannot find the road node whose id does not appear in both correct and duplicate node " +
							"list: " + startNodeID);
				startNodeID = duplicatePointMapping.get(startNodeID);
			}
			if (!index2NodeMapping.containsKey(endNodeID)) {
				if (!duplicatePointMapping.containsKey(endNodeID))
					throw new IllegalArgumentException("Cannot find the road node whose id does not appear in both correct and duplicate node " +
							"list: " + endNodeID);
				endNodeID = duplicatePointMapping.get(endNodeID);
			}
			miniNode.add(index2NodeMapping.get(startNodeID));
			miniNode.add(index2NodeMapping.get(endNodeID));
			if (miniNode.get(0).toPoint().equals2D(miniNode.get(1).toPoint())) {
				LOG.debug("Road " + roadCount + " has the same start and end point, ignore it: " + miniNode.get(0).toString());
				roadCount++;
				continue;
			}
			if (existingRoadIndexSet.contains(startNodeID + "_" + endNodeID)) {
				LOG.debug("Road that connects " + startNodeID + " and " + endNodeID + " already exists, ignore it: " + roadCount);
				roadCount++;
				continue;
			}
			newWay.setId(roadCount + "");
			existingRoadIndexSet.add(startNodeID + "_" + endNodeID);
			newWay.setNodes(miniNode);
			ways.add(newWay);
			roadCount++;
		}
		brEdges.close();
		
		roadGraph.addNodes(nodes);
		roadGraph.addWays(ways);
		List<RoadNode> removedRoadNodeList = new ArrayList<>();
		for (RoadNode n : nodes) {
			if (n.getDegree() == 0) {
				removedRoadNodeList.add(n);
			}
		}
		int count = removedRoadNodeList.size();
		roadGraph.getNodes().removeAll(removedRoadNodeList);
		LOG.info("Read " + trajNum + "-th Global road map, isolate nodes:" + count + ", total nodes:" + nodes.size() + ", total roads:" + ways.size());
		return roadGraph;
	}
}