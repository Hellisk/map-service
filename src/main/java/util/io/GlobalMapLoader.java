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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
		roadGraph.setUpdatable(false);
		List<RoadNode> nodes = new ArrayList<>();
		Map<String, RoadNode> index2Node = new HashMap<>();
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
			
			RoadNode newNode = new RoadNode(lineCount + "", lon, lat, distFunc);
			index2Node.put(lineCount + "", newNode);
			nodes.add(newNode);
			lineCount++;
		}
		brVertices.close();
		
		// read road ways
		int roadCount = 0;
		BufferedReader brEdges = new BufferedReader(new FileReader(this.mapFolder + stringFormatter
				(trajNum) + File.separator + stringFormatter
				(trajNum) + ".arcs"));
		while ((line = brEdges.readLine()) != null) {
			RoadWay newWay = new RoadWay(distFunc);
			List<RoadNode> miniNode = new ArrayList<>();
			String[] edgeInfo = line.split("\t");
			if (index2Node.containsKey(edgeInfo[0]) && index2Node.containsKey(edgeInfo[1])) {
				miniNode.add(index2Node.get(edgeInfo[0]));
				miniNode.add(index2Node.get(edgeInfo[1]));
				if (miniNode.get(0).toPoint().equals2D(miniNode.get(1).toPoint())) {
					LOG.debug("Road " + roadCount + " has the same start and end point, ignore it: " + miniNode.get(0).toString());
					roadCount++;
					continue;
				}
				newWay.setId(roadCount + "");
				newWay.setNodes(miniNode);
				ways.add(newWay);
				roadCount++;
			} else {
				LOG.warn("Road endpoint doesn't exist: " + edgeInfo[0] + "," + edgeInfo[1]);
				roadCount++;
			}
		}
		brEdges.close();
		
		roadGraph.addNodes(nodes);
		roadGraph.addWays(ways);
		roadGraph.setBoundary(minLon, maxLon, minLat, maxLat);
		List<RoadNode> removedRoadNodeList = new ArrayList<>();
		for (RoadNode n : nodes) {
			if (n.getDegree() == 0) {
				removedRoadNodeList.add(n);
			}
		}
		roadGraph.getNodes().removeAll(removedRoadNodeList);
		LOG.info("Read " + trajNum + "-th Global road map, isolate nodes:" + removedRoadNodeList.size() + ", total nodes:" + nodes.size() +
				", total roads:" + ways.size());
		return roadGraph;
	}
}