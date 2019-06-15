package algorithm.mapinference.topicmodel;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import util.function.DistanceFunction;
import util.function.GreatCircleDistanceFunction;
import util.io.IOService;
import util.object.roadnetwork.RoadNetworkGraph;
import util.object.roadnetwork.RoadNode;
import util.object.roadnetwork.RoadWay;
import util.object.spatialobject.Rect;
import util.settings.BaseProperty;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

/**
 * Command line entrance for Zheng topic model map inference algorithm. The original code is written in Python and we run the Python code
 * through this class.
 * <p>
 * Reference:
 * <p>
 * Zheng, Renjie, et al. "Topic model-based road network inference from massive trajectories." 18th IEEE International Conference on
 * Mobile Data Management (MDM). IEEE, 2017.
 *
 * @author Hellisk
 * @since 15/06/2019
 */

public class CRIFMapInference {
	
	private static final Logger LOG = Logger.getLogger(CRIFMapInference.class);
	private int k;    // number of roads
	private int cellWidth;
	private int side = 10;    // h in paper, usually h*cellWidth should be roughly 50
	private double ratio = 0.9;
	private double percent = 0.02;
	private double alpha = 0.9;
	private double maxValue = 0.2;
	private String topicModel = "pLSA";
	private String os;
	private String dataset;
	private Rect boundary;
	
	public CRIFMapInference(BaseProperty prop, Rect crifBoundary) {
		this.dataset = prop.getPropertyString("data.Dataset");
		switch (dataset) {
			case "Chicago":
				this.k = 50;
				break;
			case "Berlin":
			case "Beijing-S":
				this.k = 400;
				break;
			case "Beijing-M":
				this.k = 800;
				break;
			case "Beijing-L":
				this.k = 1600;
				break;
		}
		this.cellWidth = prop.getPropertyInteger("algorithm.mapinference.crif.CellWidth");
		this.boundary = crifBoundary;
		this.os = prop.getPropertyString("OS");
	}
	
	// use python script to run map inference python code
	public RoadNetworkGraph mapInferenceProcess(String codeRootFolder, String inputTrajFolder, String cacheFolder) throws IOException {
		List<String> pythonCmd = new ArrayList<>();
		// remove the map inference directory
		IOService.createFolder(cacheFolder);
		FileUtils.cleanDirectory(new File(cacheFolder));
		FileUtils.deleteDirectory(new File(cacheFolder));
		String inputTrajFile = inputTrajFolder + dataset + ".pickle";
		
		// setup each command manually
		pythonCmd.add("python " + codeRootFolder + "src/sacred_trajmap.py with ex_name=trajmap_k data_file=" + inputTrajFile + " " +
				"minx=" + boundary.minX() + "maxx=" + boundary.maxX() + " miny=" + boundary.minY() + " maxy" + boundary.maxY() + " side=" +
				side + " k=" + k + "ratio=" + ratio + " percent=" + percent + " width=" + cellWidth + " alpha=" + alpha + " max_value="
				+ maxValue + " topic_model=" + topicModel);
		
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
