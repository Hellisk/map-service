package util.io;

import org.apache.log4j.Logger;
import util.object.roadnetwork.RoadNetworkGraph;
import util.object.roadnetwork.RoadNode;
import util.object.roadnetwork.RoadWay;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Static class for map file writer. The <tt>RoadNetworkGraph</tt> map are written in CSV format, and the road nodes and edges are
 * written in separated files with the name prefix "vertices_"/"edges_".
 *
 * @author Hellisk
 * @since 22/05/2017
 */
public class MapWriter {
	
	private static final Logger LOG = Logger.getLogger(MapWriter.class);
	
	/**
	 * Write a road network to files
	 *
	 * @param roadGraph The map to be written.
	 * @param filePath  The name of the file to be written. The final name for edge and vertex file is "edges_fileName"/"vertices_fileName"
	 * @throws IOException Failed map writing.
	 */
	public static void writeMap(RoadNetworkGraph roadGraph, String filePath) throws IOException {
		String folderPath = filePath.substring(0, filePath.lastIndexOf('/') + 1);
		String fileName = filePath.substring(filePath.lastIndexOf('/') + 1);
		// create directories before writing
		File file = new File(folderPath);
		if (!file.exists()) {
			if (!file.mkdirs()) throw new IOException("Failed to create folder for map write: " + file.getPath());
		}
		writeNodes(roadGraph.getNodes(), folderPath + "vertices_" + fileName);
		writeWays(roadGraph.getWays(), folderPath + "edges_" + fileName);
	}
	
	/**
	 * Write a list of road way results.
	 *
	 * @param wayList  The list of road ways to be written.
	 * @param filePath Output file path, which should always contains a "edges_" as file name prefix.
	 * @throws IOException Create folder failed.
	 */
	public static void writeWays(List<RoadWay> wayList, String filePath) throws IOException {
		if (!filePath.contains("edges_"))
			LOG.warn("Invalid file name for a road way file: " + filePath);
		String folderPath = filePath.substring(0, filePath.lastIndexOf('/') + 1);
		String fileName = filePath.substring(filePath.lastIndexOf('/') + 1);
		// create directories before writing
		File file = new File(folderPath);
		if (!file.exists()) {
			if (!file.mkdirs()) throw new IOException("Failed to create folder for map write: " + file.getPath());
		}
		// write road node file
		List<String> fileLines = new ArrayList<>(wayList.size());
		for (RoadWay n : wayList) {
			fileLines.add(n.toString());
		}
		IOService.writeFile(fileLines, folderPath, fileName);
		LOG.debug("Write " + fileName + " finished.");
	}
	
	/**
	 * Write a list of road nodes to a file.
	 *
	 * @param nodeList The list of nodes to be written.
	 * @param filePath Output file path, which should always contains a "vertices_" as file name prefix.
	 * @throws IOException Failed map writing
	 */
	public static void writeNodes(List<RoadNode> nodeList, String filePath) throws IOException {
		if (!filePath.contains("vertices_"))
			LOG.warn("Invalid file name for a road node file: " + filePath);
		String folderPath = filePath.substring(0, filePath.lastIndexOf('/') + 1);
		String fileName = filePath.substring(filePath.lastIndexOf('/') + 1);
		// create directories before writing
		File file = new File(folderPath);
		if (!file.exists()) {
			if (!file.mkdirs()) throw new IOException("Failed to create folder for map write: " + file.getPath());
		}
		// write road node file
		List<String> fileLines = new ArrayList<>(nodeList.size());
		for (RoadNode n : nodeList) {
			fileLines.add(n.toString());
		}
		IOService.writeFile(fileLines, folderPath, fileName);
		LOG.debug("Write " + fileName + " finished.");
	}
}
