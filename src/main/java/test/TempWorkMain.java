package test;

import algorithm.mapmatching.MapMatchingMain;
import algorithm.mapmatching.MapMatchingMethod;
import org.apache.log4j.Logger;
import util.function.DistanceFunction;
import util.function.GreatCircleDistanceFunction;
import util.io.MatchResultWriter;
import util.io.TrajectoryReader;
import util.object.roadnetwork.RoadNetworkGraph;
import util.object.roadnetwork.RoadNode;
import util.object.roadnetwork.RoadWay;
import util.object.spatialobject.Trajectory;
import util.object.structure.SimpleTrajectoryMatchResult;
import util.settings.MapMatchingProperty;
import util.settings.MapServiceLogger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

/**
 * @author uqpchao
 * Created 5/09/2019
 */
public class TempWorkMain {
	
	public static void main(String[] args) throws IOException, ExecutionException, InterruptedException {
		String mapFile = "/media/TraminerData/MapMatchingInput/beijing";
		String trajFolder = "/media/TraminerData/MapMatchingInput/";
		String outputFolder = "/media/TraminerData/MapMatchingOutput/";
		DistanceFunction distFunc = new GreatCircleDistanceFunction();
		// initialize arguments
		MapMatchingProperty property = new MapMatchingProperty();
		property.loadPropertiesFromResourceFile("mapmatching.properties", args);
		MapServiceLogger.logInit("/media/TraminerData/log/", "Mapmatching_" + System.currentTimeMillis());
		final Logger LOG = Logger.getLogger(TempWorkMain.class);
		String matchingMethod = property.getPropertyString("algorithm.mapmatching.MatchingMethod");
		int numOfThreads = property.getPropertyInteger("algorithm.mapmatching.NumOfThreads");
		RoadNetworkGraph roadMap = loadPreprocessedMap(mapFile, LOG);
		for (int i = 1; i <= 31; i++) {
			String currTrajFolder = trajFolder + i + "/";
			String currOutputFolder = outputFolder + i + "/";
			Stream<Trajectory> trajectoryStream = TrajectoryReader.readTrajectoriesToStream(currTrajFolder, 1, 0, distFunc);
			MapMatchingMethod mapMatchingMethod = MapMatchingMain.chooseMatchMethod(matchingMethod, roadMap, property);
			List<SimpleTrajectoryMatchResult> matchResult = mapMatchingMethod.parallelMatching(trajectoryStream, numOfThreads, false);
			MatchResultWriter.writeMatchResults(matchResult, outputFolder);
			LOG.info("Finish matching of the " + i + " folder.");
		}
	}
	
	/**
	 * Read and parse the csv file of Beijing road map from Lei's output. The format is:
	 * <p>
	 * First Line:
	 * Total Node Number, Min Lat, Max Lat, Min Lon, Max Lon
	 * <p>
	 * Data, has Total Node Number lines
	 * NodeID, Nan, lat, lon, Nan, Nan, Out-Neighbor number, [Neighbor Node ID, Distance], Out-Neighbor number, [Neighbor Node ID, RoadID], Nan
	 * <p>
	 * The node of the output map includes the id, coordinates and the node type, the road ways contains
	 *
	 * @return A Road Network Graph containing the
	 * Nodes, Ways and Relations in the shape file.
	 * @throws IOException File read failure
	 */
	public static RoadNetworkGraph loadPreprocessedMap(String mapFile, Logger LOG) throws IOException {
		DistanceFunction distFunc = new GreatCircleDistanceFunction();
		BufferedReader reader = new BufferedReader(new FileReader(new File(mapFile)));
		List<RoadWay> roadWayList = new ArrayList<>();
		List<RoadNode> roadNodeList = new ArrayList<>();
		String firstLine = reader.readLine();
		String line;
		Map<String, String[]> id2NodeInfo = new LinkedHashMap<>();
		Map<String, RoadNode> id2NodeMapping = new HashMap<>();
		Set<String> wayIDSet = new HashSet<>();
		Set<String> nodeLocSet = new HashSet<>();
		while ((line = reader.readLine()) != null) {
			String[] info = line.split("\t");
			if (info.length < 8)
				throw new IllegalArgumentException("Input line format is wrong: " + line);
			String nodeID = info[0];
			RoadNode currNode = new RoadNode(nodeID, Double.parseDouble(info[3]), Double.parseDouble(info[2]), distFunc);
			String currLoc = info[3] + "_" + info[2];
			if (nodeLocSet.contains(currLoc))
				LOG.debug("The node " + nodeID + "'s location occurred multiple times.");
			else
				nodeLocSet.add(currLoc);
			roadNodeList.add(currNode);
			id2NodeInfo.put(nodeID, info);
			id2NodeMapping.put(nodeID, currNode);
		}
		
		if (roadNodeList.size() != Integer.parseInt(firstLine.split("\t")[0]))
			throw new IllegalArgumentException("The input node size is inconsistent with the given number: " + roadNodeList.size()
					+ "," + firstLine.split("\t")[0]);
		
		for (Map.Entry<String, String[]> entry : id2NodeInfo.entrySet()) {
			RoadNode currNode = id2NodeMapping.get(entry.getKey());
			String[] currInfo = entry.getValue();
			int index = 6;    // outgoing edge count;
			int outEdgeCount = Integer.parseInt(currInfo[index]);
			if (outEdgeCount != 0) {
				int secondIndex = index + outEdgeCount * 2 + 1;    // index for the second outgoing edge count
				if (outEdgeCount != Integer.parseInt(currInfo[secondIndex]))
					throw new IllegalArgumentException("The second outgoing edge count is not the same as the first one: " + currNode.getID());
				for (int i = 0; i < outEdgeCount; i++) {
					if (!currInfo[index + i * 2 + 1].equals(currInfo[secondIndex + i * 2 + 1]))        // not sure if the node info comes
						// at the same position.
						throw new IllegalArgumentException("The outgoing edge info is organised in different order: "
								+ currInfo[index + i + 1] + "," + currInfo[secondIndex + i + 1]);
					
					List<RoadNode> wayNodeList = new ArrayList<>();
					RoadNode secondNode = id2NodeMapping.get(currInfo[index + i * 2 + 1]);
					if (currNode.getID().equals(secondNode.getID()))
						throw new IllegalArgumentException("The end points of the current road is the same: " + currNode.getID()
								+ "," + (index + i * 2 + 1));
					if (currNode.toPoint().equals2D(secondNode.toPoint()))
						throw new IllegalArgumentException("The end points of the current road have the same location: "
								+ currNode.getID() + "," + secondNode.getID());
					wayNodeList.add(currNode);
					wayNodeList.add(secondNode);
					String wayID = currInfo[secondIndex + i * 2 + 2];
					if (wayIDSet.contains(wayID)) {
						wayID = "-" + wayID;
						if (wayIDSet.contains(wayID))
							throw new IllegalArgumentException("The same road way ID occurred the third times: " + wayID);
						wayIDSet.add(wayID);
					} else
						wayIDSet.add(wayID);
					RoadWay currWay = new RoadWay(wayID, wayNodeList, distFunc);
					double distance = Double.parseDouble(currInfo[index + i * 2 + 2]);
					if (Math.abs(distance - currWay.getLength()) > 50)
						LOG.debug("Distance difference is significant: " + distance + "," + currWay.getLength());
//					currWay.setLength(distance);
					roadWayList.add(currWay);
				}
			}
		}
		RoadNetworkGraph resultGraph = new RoadNetworkGraph(false, distFunc);
		resultGraph.setNodes(roadNodeList);
		resultGraph.addWays(roadWayList);
		LOG.info("The map read finished, total number of node: " + roadNodeList.size() + ", number of ways: " + roadWayList.size()
				+ ", bounding box: " + resultGraph.getBoundary().toString());
		return resultGraph;
	}
}