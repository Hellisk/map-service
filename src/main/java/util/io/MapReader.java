package util.io;

import org.apache.log4j.Logger;
import util.function.DistanceFunction;
import util.object.roadnetwork.RoadNetworkGraph;
import util.object.roadnetwork.RoadNode;
import util.object.roadnetwork.RoadWay;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Read map nodes and ways from existing CSV files. The nodes and ways can be read separately for different scenarios.
 *
 * @author Hellisk
 * @since 22/05/2017.
 */
public class MapReader {
	
	private static final Logger LOG = Logger.getLogger(MapReader.class);
	
	/**
	 * Read and parse the map files, including the both the vertices and edges. The given file name does not have the prefix
	 * "vertices_"/"edges_" and will be added during the read process.
	 *
	 * @param filePath    The input file path, which contains the file name without "vertices_"/"edges_" included.
	 * @param isUpdatable True if this map will be used for map update.
	 * @param df          The distance function of the map.
	 * @return A road network graph containing the road nodes and road ways.
	 * @throws IOException File not found
	 */
	public static RoadNetworkGraph readMap(String filePath, boolean isUpdatable, DistanceFunction df) throws IOException {
		String folderPath = filePath.substring(0, filePath.lastIndexOf('/') + 1);
		String fileName = filePath.substring(filePath.lastIndexOf('/') + 1);
		RoadNetworkGraph roadGraph = new RoadNetworkGraph(isUpdatable, df);
		Map<String, RoadNode> index2Node = new HashMap<>();       // maintain a mapping of road location to node index
		// read road nodes
		List<RoadNode> nodelist = readNodes(folderPath + "vertices_" + fileName, df);
		roadGraph.setNodes(nodelist);
		for (RoadNode node : nodelist) {
			index2Node.put(node.getID(), node);
		}
		
		// read road ways
		List<RoadWay> wayList = readWays(folderPath + "edges_" + fileName, index2Node, df);
		roadGraph.addWays(wayList);
		int removedNodeCount = roadGraph.isolatedNodeRemoval();
		LOG.info(fileName + " road map read done. isolate nodes: " + removedNodeCount + ", total nodes:" + roadGraph.getNodes().size() + ", " +
				"total road ways: " + roadGraph.getWays().size());
		return roadGraph;
	}
	
	/**
	 * Read the ground truth map and extract the sub graph enclosed by the bounding box.
	 *
	 * @param filePath    The input file path, which contains the file name without "vertices_"/"edges_" included.
	 * @param isUpdatable True if this map will be used for map update.
	 * @param boundingBox The specified bounding box, same as readMap(0) when the bounding box is empty
	 * @param df          The distance function of the map.
	 * @return The road network graph enclosed by the given bounding box
	 * @throws IOException file not found
	 */
	public static RoadNetworkGraph extractMapWithBoundary(String filePath, boolean isUpdatable, double[] boundingBox,
														  DistanceFunction df) throws IOException {
		// call readMap() if no boundary is set
		if (boundingBox.length != 4)
			return readMap(filePath, isUpdatable, df);
		
		String folderPath = filePath.substring(0, filePath.lastIndexOf('/') + 1);
		String fileName = filePath.substring(filePath.lastIndexOf('/') + 1);
		RoadNetworkGraph roadGraph = new RoadNetworkGraph(isUpdatable, df);
		roadGraph.setBoundary(boundingBox[0], boundingBox[1], boundingBox[2], boundingBox[3]);
		Map<String, RoadNode> index2Node = new HashMap<>();       // maintain a mapping of road location to node index
		
		// read road nodes
		List<RoadNode> nodelist = readNodes(folderPath + "vertices_" + fileName, df);
		nodelist.removeIf(x -> !roadGraph.getBoundary().contains(x.lon(), x.lat()));
		roadGraph.setNodes(nodelist);
		for (RoadNode node : nodelist) {
			index2Node.put(node.getID(), node);
		}
		
		// read road ways
		List<RoadWay> wayList = readWays(folderPath + "edges_" + fileName, index2Node, df);
		wayList.removeIf(RoadWay::isEmpty);
		roadGraph.addWays(wayList);
		
		int removedNodeCount = roadGraph.isolatedNodeRemoval();
		roadGraph.updateBoundary();
		LOG.info(fileName + " road map with boundary read done. isolate nodes: " + removedNodeCount + ", total nodes:" +
				roadGraph.getNodes().size() + ", total road ways: " + roadGraph.getWays().size());
		return roadGraph;
	}
	
	/**
	 * Read the road nodes from file whose file path is given. This function can be used together with a <tt>readWays()</tt> for reading
	 * an entire map or be used individually.
	 *
	 * @param filePath The path of the road node file to be read. The file name should include "vertices_" as prefix.
	 * @param df       The distance function for the road nodes to be read.
	 * @return A list of road nodes from the file.
	 */
	public static List<RoadNode> readNodes(String filePath, DistanceFunction df) {
		if (!filePath.contains("vertices_"))
			LOG.warn("Invalid file name for a road node file: " + filePath);
		List<RoadNode> nodeList = new ArrayList<>();
		// read road nodes
		List<String> lines = IOService.readFile(filePath);
		for (String line : lines) {
			nodeList.add(RoadNode.parseRoadNode(line, df));
		}
		return nodeList;
	}
	
	/**
	 * Read the road ways from file whose file path is given. This function can be used together with a <tt>readNodes()</tt> for reading
	 * an entire map or be used individually.
	 *
	 * @param filePath   The path of the road way file to be read. The file name should include "edges_" as prefix.
	 * @param index2Node The mapping between a road node name and its correspond road node object. Used for linking the road ways to the
	 *                   corresponding road nodes. This field is set empty if the links to the road nodes are not required.
	 * @param df         The distance function for the road ways to be read.
	 * @return A list of road ways from the file.
	 */
	public static List<RoadWay> readWays(String filePath, Map<String, RoadNode> index2Node, DistanceFunction df) {
		if (!filePath.contains("edges_"))
			LOG.warn("Invalid file name for a road way file: " + filePath);
		List<RoadWay> wayList = new ArrayList<>();
		// read road ways
		List<String> lines = IOService.readFile(filePath);
		for (String line : lines) {
			wayList.add(RoadWay.parseRoadWay(line, index2Node, df));
		}
		return wayList;
	}

//	public List<RoadWay> readNewMapEdge(int percentage, int iteration, boolean isTempMap) throws IOException {
//		List<RoadWay> wayList = new ArrayList<>();
//		String inputPath;
//		if (iteration != -1)
//			inputPath = this.csvMapPath + "map/" + iteration + "/";
//		else inputPath = this.csvMapPath;
//		// read road ways
//		BufferedReader brEdges;
//		if (isTempMap)
//			brEdges = new BufferedReader(new FileReader(inputPath + "temp_edges_" + percentage + ".txt"));
//		else brEdges = new BufferedReader(new FileReader(inputPath + "edges_" + percentage + ".txt"));
//		String line;
//		while ((line = brEdges.readLine()) != null) {
//			RoadWay newWay = RoadWay.parseRoadWay(line, new HashMap<>());
//			if (newWay.isNewRoad())
//				wayList.add(newWay);
//		}
//		brEdges.close();
//		return wayList;
//	}
//
//	public List<RoadWay> readRemovedEdges(int percentage, int iteration) throws IOException {
//		HashSet<String> removedRoadIdSet = new HashSet<>();
//		if (percentage == 0)    // no removed edge when percentage is 0
//			return new ArrayList<>();
//		List<RoadWay> removedRoads = new ArrayList<>();
//		String line;
//		// read removed road ways
//		if (iteration != -1)
//			LOG.error("ERROR! Removed road ways is only read outside the iteration.");
//		BufferedReader brEdges = new BufferedReader(new FileReader(this.csvMapPath + "removedEdges_" + percentage + ".txt"));
//		while ((line = brEdges.readLine()) != null) {
//			RoadWay newWay = RoadWay.parseRoadWay(line, new HashMap<>());
//			if (!removedRoadIdSet.contains(newWay.getID())) {
//				removedRoadIdSet.add(newWay.getID());
//				removedRoads.add(newWay);
//			} else
//				LOG.error("ERROR! Duplicated removed road.");
//		}
//		return removedRoads;
//	}
//
//	public List<RoadWay> readInferredEdges(boolean isForUpdate) throws IOException {
//		List<RoadWay> inferredRoads = new ArrayList<>();
//		// read inferred road ways
//		File inferenceFile;
//		if (isForUpdate)
//			inferenceFile = new File(this.csvMapPath + "inferred_edges.txt");
//		else
//			inferenceFile = new File(this.csvMapPath + "inferred_edges_1.txt");
//		if (!inferenceFile.exists()) {
//			System.err.println("ERROR! The inferred roads are not found.");
//			return inferredRoads;
//		}
//		BufferedReader brEdges = new BufferedReader(new FileReader(inferenceFile));
//		String line;
//		while ((line = brEdges.readLine()) != null) {
//			RoadWay newWay = RoadWay.parseRoadWay(line, new HashMap<>());
//			newWay.setId("temp_" + newWay.getID());
//			newWay.setNewRoad(true);
//			inferredRoads.add(newWay);
//		}
//		return inferredRoads;
//	}
}