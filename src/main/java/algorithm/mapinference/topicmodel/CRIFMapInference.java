package algorithm.mapinference.topicmodel;

import org.apache.log4j.Logger;
import util.function.DistanceFunction;
import util.function.EuclideanDistanceFunction;
import util.io.IOService;
import util.object.roadnetwork.RoadNetworkGraph;
import util.object.roadnetwork.RoadNode;
import util.object.roadnetwork.RoadWay;
import util.object.spatialobject.Rect;
import util.settings.BaseProperty;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
				this.k = 1000;
				break;
			case "Beijing-M":
				this.k = 5000;
				break;
			case "Beijing-L":
				this.k = 20000;
				break;
		}
		this.cellWidth = prop.getPropertyInteger("algorithm.mapinference.crif.CellWidth");
		this.boundary = crifBoundary;
		this.os = prop.getPropertyString("OS");
	}
	
	// use python script to run map inference python code
	public RoadNetworkGraph mapInferenceProcess(String codeRootFolder, String inputTrajFolder, String cacheFolder) {
		List<String> pythonCmd = new ArrayList<>();
		// remove the map inference directory
		IOService.createFolder(cacheFolder);
		IOService.cleanFolder(cacheFolder);
		String inputTrajFile = inputTrajFolder + dataset + ".pickle";
		
		// setup each command manually
		pythonCmd.add("python " + codeRootFolder + "src/sacred_trajmap.py with ex_name=trajmap_k data_file=" + inputTrajFile +
				" map_min_x=" + boundary.minX() + " map_max_x=" + boundary.maxX() + " map_min_y=" + boundary.minY() + " map_max_y=" + boundary.maxY() +
				" side=" + side + " k=" + k + " ratio=" + ratio + " percent=" + percent + " width=" + cellWidth + " alpha=" + alpha
				+ " max_value=" + maxValue + " topic_model=" + topicModel + " output_folder=" + cacheFolder);
		
		try {
			runCode(pythonCmd);
			System.gc();
		} catch (Exception e) {
			e.printStackTrace();
		}
		String inputMapPath = cacheFolder + "inferred_map_CRIF.txt";
		return readCRIFOutputMap(inputMapPath);
	}
	
	/**
	 * Convert the KDE output edge file to a regular RoadNetworkGraph.
	 *
	 * @param inputEdgeListPath The generated road list
	 * @return The output map
	 */
	private RoadNetworkGraph readCRIFOutputMap(String inputEdgeListPath) {
		DistanceFunction distFunc = new EuclideanDistanceFunction();
		List<RoadNode> nodeList = new ArrayList<>();
		List<RoadWay> wayList = new ArrayList<>();
		// read road ways
		Map<String, RoadNode> location2NodeMap = new LinkedHashMap<>();
		List<String> lines = IOService.readFile(inputEdgeListPath);
		int nodeCount = 0;
		// the first line is title, skip it
		for (int i = 1; i < lines.size(); i += 2) {
			if (!lines.get(i).contains(","))    // reach the file end
				break;
			if (i + 1 == lines.size())
				throw new IllegalArgumentException("The output map does not have even number of records.");
			String[] startPointInfo = lines.get(i).split(",");
			String[] endPointInfo = lines.get(i + 1).split(",");
			if (startPointInfo.length != 4 || endPointInfo.length != 4) {
				LOG.warn("The current road way info is incomplete: " + lines.get(i) + "," + lines.get(i + 1));
				continue;
			}
			List<RoadNode> currWayNodeList = new ArrayList<>();
			String startLocation = startPointInfo[2] + "_" + startPointInfo[3];
			String endLocation = endPointInfo[2] + "_" + endPointInfo[3];
			if (location2NodeMap.containsKey(startLocation)) {    // the intersection already exists
				currWayNodeList.add(location2NodeMap.get(startLocation));
			} else {
				RoadNode startNode = new RoadNode(nodeCount + "", Double.parseDouble(startPointInfo[2]),
						Double.parseDouble(startPointInfo[3]), distFunc);
				currWayNodeList.add(startNode);
				nodeList.add(startNode);
				nodeCount++;
				location2NodeMap.put(startLocation, startNode);
			}
			
			if (location2NodeMap.containsKey(endLocation)) {    // the intersection already exists
				currWayNodeList.add(location2NodeMap.get(endLocation));
			} else {
				RoadNode endNode = new RoadNode(nodeCount + "", Double.parseDouble(endPointInfo[2]),
						Double.parseDouble(endPointInfo[3]), distFunc);
				currWayNodeList.add(endNode);
				nodeList.add(endNode);
				nodeCount++;
				location2NodeMap.put(endLocation, endNode);
			}
			String wayID = startPointInfo[1];
			RoadWay currWay = new RoadWay(wayID, currWayNodeList, distFunc);
			wayList.add(currWay);
		}
		RoadNetworkGraph resultMap = new RoadNetworkGraph(false, distFunc);
		resultMap.addNodes(nodeList);
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
