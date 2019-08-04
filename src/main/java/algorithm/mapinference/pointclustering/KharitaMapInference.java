package algorithm.mapinference.pointclustering;

import org.apache.log4j.Logger;
import util.function.DistanceFunction;
import util.function.GreatCircleDistanceFunction;
import util.io.IOService;
import util.object.roadnetwork.RoadNetworkGraph;
import util.object.roadnetwork.RoadNode;
import util.object.roadnetwork.RoadWay;
import util.settings.BaseProperty;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Command line entrance for Stanojevic's Kharita map inference algorithm. The original code is written in Python and Golang, we prepare
 * the
 * command line code and run the algorithms through console. We read the output map and convert into our format after all processes are
 * done.
 * <p>
 * Reference:
 * <p>
 * He, Songtao, et al. "RoadRunner: improving the precision of road network inference from GPS trajectories." Proceedings of the 26th ACM
 * SIGSPATIAL International Conference on Advances in Geographic Information Systems. ACM, 2018.
 *
 * @author Hellisk
 * Created 7/06/2019
 */
public class KharitaMapInference {
	
	private static final Logger LOG = Logger.getLogger(KharitaMapInference.class);
	private String os;
	private double radius;
	private double densifyDistance;
	private double angleTolerance;
	private BaseProperty property;
	
	public KharitaMapInference(BaseProperty prop) {
		this.os = prop.getPropertyString("OS");
		this.radius = prop.getPropertyDouble("algorithm.mapinference.pointclustering.Radius");
		this.densifyDistance = prop.getPropertyDouble("algorithm.mapinference.pointclustering.DensifyDistance");
		this.angleTolerance = prop.getPropertyDouble("algorithm.mapinference.pointclustering.AngleTolerance");
		this.property = prop;
	}
	
	// use scripts to run map inference Python and Golang code
	public RoadNetworkGraph mapInferenceProcess(String codeRootFolder, String inputTrajFolder, String cacheFolder) {
		
		// remove the previous cache directory
		IOService.createFolder(cacheFolder);
		IOService.cleanFolder(cacheFolder);
		String dataset = property.getPropertyString("data.Dataset");
		
		List<String> pythonCmd = new ArrayList<>();
		
		// setup each command manually
		pythonCmd.add("python " + codeRootFolder + "kharita.py -f " + inputTrajFolder + dataset + ".txt" + " -r " + radius + " -s "
				+ densifyDistance + " -a " + angleTolerance + " -p " + cacheFolder + "inferred_edges.txt");
		
		try {
			runCode(pythonCmd);
			System.gc();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		String inputMapPath = cacheFolder + "inferred_edges.txt";
		return readKharitaOutputMap(inputMapPath);
	}
	
	/**
	 * Convert the KDE output edge file to a regular RoadNetworkGraph.
	 *
	 * @param inputEdgeListPath The generated road list
	 * @return The output map
	 */
	private RoadNetworkGraph readKharitaOutputMap(String inputEdgeListPath) {
		DistanceFunction distFunc = new GreatCircleDistanceFunction();
		List<RoadNode> nodeList = new ArrayList<>();
		List<RoadWay> wayList = new ArrayList<>();
		// read road ways
		Map<String, RoadNode> location2NodeMap = new LinkedHashMap<>();
		List<String> lines = IOService.readFile(inputEdgeListPath);
		int nodeCount = 0;
		int wayCount = 0;
		// the first line is title, skip it
		for (int i = 0; i < lines.size(); i += 3) {
			if (!lines.get(i).contains(","))
				break;
			if (i + 2 >= lines.size())
				throw new IllegalArgumentException("The output map does not have even number of records.");
			String[] startPointInfo = lines.get(i).split(",");
			String[] endPointInfo = lines.get(i + 1).split(",");
			if (startPointInfo.length != 2 || endPointInfo.length != 2) {
				LOG.warn("The current road way info is incomplete: " + lines.get(i) + "," + lines.get(i + 1));
				continue;
			}
			List<RoadNode> currWayNodeList = new ArrayList<>();
			String startLocation = startPointInfo[1] + "_" + startPointInfo[0];
			String endLocation = endPointInfo[1] + "_" + endPointInfo[0];
			if (location2NodeMap.containsKey(startLocation)) {    // the intersection already exists
				currWayNodeList.add(location2NodeMap.get(startLocation));
			} else {
				RoadNode startNode = new RoadNode(nodeCount + "", Double.parseDouble(startPointInfo[1]),
						Double.parseDouble(startPointInfo[0]), distFunc);
				currWayNodeList.add(startNode);
				nodeList.add(startNode);
				nodeCount++;
				location2NodeMap.put(startLocation, startNode);
			}
			
			if (location2NodeMap.containsKey(endLocation)) {    // the intersection already exists
				currWayNodeList.add(location2NodeMap.get(endLocation));
			} else {
				RoadNode endNode = new RoadNode(nodeCount + "", Double.parseDouble(endPointInfo[1]),
						Double.parseDouble(endPointInfo[0]), distFunc);
				currWayNodeList.add(endNode);
				nodeList.add(endNode);
				nodeCount++;
				location2NodeMap.put(endLocation, endNode);
			}
			RoadWay currWay = new RoadWay(wayCount + "", currWayNodeList, distFunc);
			wayList.add(currWay);
			wayCount++;
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