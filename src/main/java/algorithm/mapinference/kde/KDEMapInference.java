package algorithm.mapinference.kde;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import util.function.DistanceFunction;
import util.function.GreatCircleDistanceFunction;
import util.io.IOService;
import util.object.roadnetwork.RoadNetworkGraph;
import util.object.roadnetwork.RoadNode;
import util.object.roadnetwork.RoadWay;
import util.settings.BaseProperty;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

/**
 * Command line entrance for Biagioni KDE map inference algorithm. The original code is written in Python and we run the Python code
 * through this class.
 * <p>
 * Reference:
 * <p>
 * Biagioni, James, and Jakob Eriksson. "Map inference in the face of noise and disparity." Proceedings of the 20th International
 * Conference on Advances in Geographic Information Systems. ACM, 2012.
 *
 * @author Hellisk
 */

public class KDEMapInference {
	
	private static final Logger LOG = Logger.getLogger(KDEMapInference.class);
	private int cellSize;    // meter
	private int gaussianBlur;
	private String os;
	
	public KDEMapInference(BaseProperty prop) {
		this.cellSize = prop.getPropertyInteger("algorithm.mapinference.kde.CellSize");
		this.gaussianBlur = prop.getPropertyInteger("algorithm.mapinference.kde.GaussianBlur");
		this.os = prop.getPropertyString("OS");
	}
	
	// use python script to run map inference python code
	public RoadNetworkGraph mapInferenceProcess(String codeRootFolder, String inputTrajFolder, String cacheFolder) throws IOException {
		List<String> pythonCmd = new ArrayList<>();

		// remove the map inference directory
		IOService.createFolder(cacheFolder);
		FileUtils.cleanDirectory(new File(cacheFolder));
		FileUtils.deleteDirectory(new File(cacheFolder));

		// setup each command manually
		pythonCmd.add("python " + codeRootFolder + "kde.py -c " + this.cellSize + " -b " + this.gaussianBlur + " -i " + inputTrajFolder + " -f " + cacheFolder);
		pythonCmd.add("python " + codeRootFolder + "skeleton.py -f " + cacheFolder);
		pythonCmd.add("python " + codeRootFolder + "graph_extract.py -f " + cacheFolder);
		pythonCmd.add("python " + codeRootFolder + "graphdb_matcher_run.py -f " + cacheFolder + " -t " + inputTrajFolder);
		pythonCmd.add("python " + codeRootFolder + "process_map_matches.py -f " + cacheFolder);
		pythonCmd.add("python " + codeRootFolder + "refine_topology.py -f " + cacheFolder);
		pythonCmd.add("python " + codeRootFolder + "graphdb_matcher_run.py -d skeleton_maps/skeleton_map_1m_mm1_tr.db -o " +
				"matched_trips_1m_mm1_tr/ -t " + inputTrajFolder + " -f " + cacheFolder);
		pythonCmd.add("python " + codeRootFolder + "process_map_matches.py -f " + cacheFolder + " -d skeleton_maps/skeleton_map_1m_mm1_tr" +
				".db -t matched_trips_1m_mm1_tr -o skeleton_maps/skeleton_map_1m_mm2.db");
		pythonCmd.add("python " + codeRootFolder + "streetmap.py -f " + cacheFolder);
		
		try {
			runCode(pythonCmd);
			System.gc();
		} catch (Exception e) {
			e.printStackTrace();
		}
		String inputMapPath = cacheFolder + "inferred_edges.txt";
		return readKDEOutputMap(inputMapPath);
	}
	
	/**
	 * Convert the KDE output edge file to a regular RoadNetworkGraph.
	 *
	 * @param inputEdgeListPath The generated road list
	 * @return The output map
	 */
	private RoadNetworkGraph readKDEOutputMap(String inputEdgeListPath) {
		DistanceFunction distFunc = new GreatCircleDistanceFunction();
		List<RoadWay> wayList = new ArrayList<>();
		// read road ways
		List<String> lines = IOService.readFile(inputEdgeListPath);
		for (String line : lines) {
			RoadWay currWay = RoadWay.parseRoadWay(line, new HashMap<>(), distFunc);
			wayList.add(currWay);
		}
		Map<String, RoadNode> location2NodeMap = new LinkedHashMap<>();
		int nodeCount = 0;
		List<RoadWay> removedWayList = new ArrayList<>();
		for (RoadWay currWay : wayList) {
			if (currWay.getFromNode().toPoint().equals2D(currWay.getToNode().toPoint())) {
				removedWayList.add(currWay);
				continue;
			}
			List<RoadNode> replaceNodeList = new ArrayList<>();
			String startLocation = currWay.getFromNode().lon() + "_" + currWay.getFromNode().lat();
			if (location2NodeMap.containsKey(startLocation)) {    // the intersection already exists
				replaceNodeList.add(location2NodeMap.get(startLocation));
			} else {
				replaceNodeList.add(currWay.getFromNode());
				currWay.getFromNode().setId(nodeCount + "");
				nodeCount++;
				location2NodeMap.put(startLocation, currWay.getFromNode());
			}
			replaceNodeList.addAll(currWay.getNodes().subList(1, currWay.size() - 1));
			String endLocation = currWay.getToNode().lon() + "_" + currWay.getToNode().lat();
			if (location2NodeMap.containsKey(endLocation)) {    // the intersection already exists
				replaceNodeList.add(location2NodeMap.get(endLocation));
			} else {
				replaceNodeList.add(currWay.getToNode());
				currWay.getToNode().setId(nodeCount + "");
				nodeCount++;
				location2NodeMap.put(endLocation, currWay.getToNode());
			}
			currWay.setNodes(replaceNodeList);
		}
		wayList.removeIf(removedWayList::contains);
		List<RoadNode> currNodeList = new ArrayList<>();
		for (Map.Entry<String, RoadNode> entry : location2NodeMap.entrySet()) {
			currNodeList.add(entry.getValue());
		}
		RoadNetworkGraph resultMap = new RoadNetworkGraph(false, distFunc);
		resultMap.addNodes(currNodeList);
		resultMap.addWays(wayList);
		return resultMap;
	}
	
	private void runCode(List<String> pythonCmd) throws Exception {
		LOG.info("Start the python map inference process.");
		StringBuilder command = new StringBuilder();
		command.append(pythonCmd.get(0));
		for (int i = 1; i < pythonCmd.size(); i++) {
			String pc = pythonCmd.get(i);
			command.append(" && ").append(pc);
		}
		ProcessBuilder builder;
		
		if (os.equals("Linux")) {
			builder = new ProcessBuilder("/bin/sh", "-c", command.toString());
		} else {
			builder = new ProcessBuilder("cmd.exe", "/c", command.toString());
		}
		builder.redirectErrorStream(true);
		Process p = builder.start();
		BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
		String line;
		while ((line = br.readLine()) != null) {
			LOG.info(line);
		}
	}
}
