package algorithm.mapinference.roadrunner;

import org.apache.log4j.Logger;
import util.function.DistanceFunction;
import util.function.GreatCircleDistanceFunction;
import util.io.IOService;
import util.object.roadnetwork.RoadNetworkGraph;
import util.object.roadnetwork.RoadNode;
import util.object.roadnetwork.RoadWay;
import util.object.spatialobject.Rect;
import util.settings.BaseProperty;

import java.io.*;
import java.util.*;

/**
 * Command line entrance for He's RoadRunner map inference algorithm. The original code is written in Python and Golang, we prepare the
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
public class RoadRunnerMapInference {
	
	private static final Logger LOG = Logger.getLogger(RoadRunnerMapInference.class);
	private String os;
	private BaseProperty property;
	private Rect mapBoundary;
	
	public RoadRunnerMapInference(BaseProperty prop, Rect boundary) {
		this.os = prop.getPropertyString("OS");
		this.property = prop;
		this.mapBoundary = boundary;
	}
	
	// use scripts to run map inference Python and Golang code
	public RoadNetworkGraph mapInferenceProcess(String codeRootFolder, String inputTrajFolder, String cacheFolder) throws IOException {
		
		DistanceFunction distFunc = new GreatCircleDistanceFunction();

//		// remove the previous cache directory
//		IOService.createFolder(cacheFolder);
//		FileUtils.cleanDirectory(new File(cacheFolder));
//		IOService.createFolder(cacheFolder + "index_folder/");    // create the folder for trajectory index
//		IOService.createFolder(cacheFolder + "output/");    // create the folder for trajectory index
		
		writeConfigureFile(cacheFolder, this.mapBoundary);
		List<String> goCmd = new ArrayList<>();
		List<String> pythonCmd = new ArrayList<>();
		
		// setup each command manually
		goCmd.add("go run " + codeRootFolder + "/GPSTraceServer/create_index.go " + inputTrajFolder + " " + cacheFolder +
				"index_folder/");
		goCmd.add("go run " + codeRootFolder + "/GPSTraceServer/trace_server.go " + cacheFolder + "index_folder/ " + inputTrajFolder + " " + 50000);
		pythonCmd.add("python " + codeRootFolder + "RoadRunner.py " + cacheFolder + "configure.json" + " test_");
		pythonCmd.add("python " + codeRootFolder + "RoadForest2RoadGraph.py " + cacheFolder + "output_file_last" + " test_graph.p");
		
		try {
			runCode(goCmd, pythonCmd);
			System.gc();
		} catch (Exception e) {
			e.printStackTrace();
		}
		String inputMapPath = cacheFolder + "test_graph.p";
		return readRoadRunnerOutputMap(inputMapPath);
	}
	
	/**
	 * Write the configure file for the following road runner process.
	 *
	 * @param cacheFolder The cache folder used to store configure file.
	 * @param mapBoundary The boundary of the map area.
	 */
	private void writeConfigureFile(String cacheFolder, Rect mapBoundary) throws IOException {
		File confFile = new File(cacheFolder + "configure.json");
		int historyLength = property.getPropertyInteger("algorithm.mapinference.roadrunner.HistoryLength");
		int numOfDeferredBranch = property.getPropertyInteger("algorithm.mapinference.roadrunner.NumberOfDeferredBranch");
		int minTrajCount = property.getPropertyInteger("algorithm.mapinference.roadrunner.MinNumberOfTrajectory");
		if (confFile.exists())
			if (!confFile.delete())
				throw new IOException("Cannot delete the current configuration file.");
		BufferedWriter confFileWriter = new BufferedWriter(new FileWriter(confFile));
		confFileWriter.write("{\n");
		confFileWriter.write("  \"Entrances\": [\n");
		confFileWriter.write("    {\n");
		confFileWriter.write("      \"Lat\": " + mapBoundary.minY() + ",\n");
		confFileWriter.write("      \"Lon\": " + mapBoundary.minX() + ",\n");
		confFileWriter.write("	\"ID\": 1\n");
		confFileWriter.write("    },\n");
		confFileWriter.write("    {\n");
		confFileWriter.write("      \"Lat\": " + mapBoundary.maxY() + ",\n");
		confFileWriter.write("      \"Lon\": " + mapBoundary.minX() + ",\n");
		confFileWriter.write("	\"ID\": 2\n");
		confFileWriter.write("    },\n");
		confFileWriter.write("    {\n");
		confFileWriter.write("      \"Lat\": " + mapBoundary.minY() + ",\n");
		confFileWriter.write("      \"Lon\": " + mapBoundary.maxX() + ",\n");
		confFileWriter.write("	\"ID\": 3\n");
		confFileWriter.write("    },\n");
		confFileWriter.write("    {\n");
		confFileWriter.write("      \"Lat\": " + mapBoundary.maxY() + ",\n");
		confFileWriter.write("      \"Lon\": " + mapBoundary.maxX() + ",\n");
		confFileWriter.write("	\"ID\": 4\n");
		confFileWriter.write("    }\n");
		confFileWriter.write("  ],\n");
		confFileWriter.write("  \"CNNInput\": null,\n");
		confFileWriter.write("  \"Exits\": [], \n");
		confFileWriter.write("  \"Region\": [\n");
		confFileWriter.write("    " + mapBoundary.minY() + ",\n");
		confFileWriter.write("    " + mapBoundary.minX() + ",\n");
		confFileWriter.write("    " + mapBoundary.maxY() + ",\n");
		confFileWriter.write("    " + mapBoundary.maxX() + "\n");
		confFileWriter.write("  ],\n");
		confFileWriter.write("  \"RegionMask\": null,\n");
		confFileWriter.write("  \"OutputFolder\": \"" + cacheFolder.substring(0, cacheFolder.length() - 1) + "\",\n");
		confFileWriter.write("  \"history_length\": " + historyLength + ",\n");
		confFileWriter.write("  \"minimal_number_of_trips_deferred_branch\": " + numOfDeferredBranch + ",\n");
		confFileWriter.write("  \"minimal_number_of_trips\": " + minTrajCount + "\n");
		confFileWriter.write("}\n");
		confFileWriter.flush();
		confFileWriter.close();
	}
	
	/**
	 * Convert the KDE output edge file to a regular RoadNetworkGraph.
	 *
	 * @param inputEdgeListPath The generated road list
	 * @return The output map
	 */
	private RoadNetworkGraph readRoadRunnerOutputMap(String inputEdgeListPath) {
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
		for (RoadWay currWay : wayList) {
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
				location2NodeMap.put(startLocation, currWay.getToNode());
			}
			currWay.setNodes(replaceNodeList);
		}
		List<RoadNode> currNodeList = new ArrayList<>();
		for (Map.Entry<String, RoadNode> entry : location2NodeMap.entrySet()) {
			currNodeList.add(entry.getValue());
		}
		RoadNetworkGraph resultMap = new RoadNetworkGraph(false, distFunc);
		resultMap.addNodes(currNodeList);
		resultMap.addWays(wayList);
		return resultMap;
	}
	
	private void runCode(List<String> goCmd, List<String> pythonCmd) throws Exception {
		// run go command to build the index
		if (goCmd.size() != 2)
			throw new IllegalArgumentException("The Go command for RoadRunner is incorrect.");
//		ProcessBuilder goBuilder;
//		if (os.equals("Linux")) {
//			goBuilder = new ProcessBuilder("/bin/sh", "-c", goCmd.get(0));
//		} else {
//			goBuilder = new ProcessBuilder("cmd.exe", "/c", goCmd.get(0));
//		}
//		goBuilder.redirectErrorStream(true);
//		Process goProcess = goBuilder.start();
//		BufferedReader goReader = new BufferedReader(new InputStreamReader(goProcess.getInputStream()));
//		String goLine;
//		while (true) {
//			goLine = goReader.readLine();
//			if (goLine == null) {
//				break;
//			}
//			LOG.info(goLine);
//		}
		
		// start the TraceServer
		if (os.equals("Linux")) {
			Runtime.getRuntime().exec("/bin/sh -c " + goCmd.get(1));
		} else {
			Runtime.getRuntime().exec("cmd.exe /c " + goCmd.get(1));
		}
		Thread.sleep(15000);    // wait for the server to start
		LOG.info("The server is started. Start the inference process.");
		
		// run python code
		StringBuilder pythonCommand = new StringBuilder();
		pythonCommand.append(pythonCmd.get(0));
		for (int i = 1; i < pythonCmd.size(); i++) {
			String pc = pythonCmd.get(i);
			pythonCommand.append(" && ").append(pc);
		}
		ProcessBuilder builder;
		if (os.equals("Linux")) {
			builder = new ProcessBuilder("/bin/sh", "-c", pythonCommand.toString());
		} else {
			builder = new ProcessBuilder("cmd.exe", "/c", pythonCommand.toString());
		}
		builder.redirectErrorStream(true);
		Process p = builder.start();
		BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
		String line;
		while (true) {
			line = r.readLine();
			if (line == null) {
				break;
			}
			LOG.info(line);
		}
	}
}