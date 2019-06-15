package algorithm.mapinference.roadrunner;

import org.apache.log4j.Logger;
import util.function.DistanceFunction;
import util.function.GreatCircleDistanceFunction;
import util.io.IOService;
import util.io.TrajectoryReader;
import util.object.roadnetwork.RoadNetworkGraph;
import util.object.roadnetwork.RoadNode;
import util.object.roadnetwork.RoadWay;
import util.object.spatialobject.Rect;
import util.object.spatialobject.Trajectory;
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
		
		// remove the previous cache directory
		IOService.createFolder(cacheFolder);
		IOService.cleanFolder(cacheFolder);
		IOService.createFolder(cacheFolder + "index_folder/");
		IOService.cleanFolder(cacheFolder + "index_folder/");
		
		writeConfigureFile(cacheFolder, this.mapBoundary, inputTrajFolder);
		List<String> goCmd = new ArrayList<>();
		List<String> pythonCmd = new ArrayList<>();
		
		// setup each command manually
		goCmd.add("go run " + codeRootFolder + "/GPSTraceServer/create_index.go " + inputTrajFolder + " " + cacheFolder +
				"index_folder/");
		goCmd.add("go run " + codeRootFolder + "/GPSTraceServer/trace_server.go " + cacheFolder + "index_folder/ " + inputTrajFolder + " " + 50000);
		pythonCmd.add("python " + codeRootFolder + "RoadRunner.py " + cacheFolder + "configure.json" + " test_");
		pythonCmd.add("python " + codeRootFolder + "RoadForest2RoadGraph.py " + cacheFolder + "output_file_last" + " " + cacheFolder);
		
		try {
			runCode(goCmd, pythonCmd);
			System.gc();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return readRoadRunnerOutputMap(cacheFolder);
	}
	
	/**
	 * Write the configure file for the following road runner process.
	 *
	 * @param cacheFolder     The cache folder used to store configure file.
	 * @param mapBoundary     The boundary of the map area.
	 * @param inputTrajFolder The trajectory folder.
	 */
	private void writeConfigureFile(String cacheFolder, Rect mapBoundary, String inputTrajFolder) throws IOException {
		DistanceFunction distFunc = new GreatCircleDistanceFunction();
		File confFile = new File(cacheFolder + "configure.json");
		if (confFile.exists()) {
			if (!confFile.delete())
				throw new IOException("Cannot delete previous configuration file.");
		}
		int historyLength = property.getPropertyInteger("algorithm.mapinference.roadrunner.HistoryLength");
		int numOfDeferredBranch = property.getPropertyInteger("algorithm.mapinference.roadrunner.NumberOfDeferredBranch");
		int minTrajCount = property.getPropertyInteger("algorithm.mapinference.roadrunner.MinNumberOfTrajectory");
		List<Trajectory> inputTrajList = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			inputTrajList.add(TrajectoryReader.readTrajectory(inputTrajFolder + "trip_" + i + ".txt", i + "", distFunc));
		}
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
		// add some trajectory start node as start point to ensure correctness
		for (int i = 0; i < inputTrajList.size(); i++) {
			Trajectory traj = inputTrajList.get(i);
			confFileWriter.write("    },\n");
			confFileWriter.write("    {\n");
			confFileWriter.write("      \"Lat\": " + traj.get(0).y() + ",\n");
			confFileWriter.write("      \"Lon\": " + traj.get(0).x() + ",\n");
			confFileWriter.write("	\"ID\": " + (5 + i) + "\n");
		}
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
	 * Convert the RoadRunner output map files to a regular RoadNetworkGraph.
	 *
	 * @param inputMapFolder The folder that contains inferred map information, including vertices_RR.txt and edges_RR.txt
	 * @return The output map
	 */
	private RoadNetworkGraph readRoadRunnerOutputMap(String inputMapFolder) {
		DistanceFunction distFunc = new GreatCircleDistanceFunction();
		List<RoadNode> nodeList = new ArrayList<>();
		List<RoadWay> wayList = new ArrayList<>();
		Set<String> occurredNodeIDSet = new HashSet<>();
		// read road ways
		List<String> nodeLines = IOService.readFile(inputMapFolder + "vertices_RR.txt");
		Map<String, RoadNode> id2NodeMap = new HashMap<>();
		for (String line : nodeLines) {
			RoadNode currNode = RoadNode.parseRoadNode(line, distFunc);
			if (id2NodeMap.containsKey(currNode.getID()))
				throw new IllegalArgumentException("The same node occurs more than once.");
			id2NodeMap.put(currNode.getID(), currNode);
			nodeList.add(currNode);
		}
		
		// read edges
		List<String> wayLines = IOService.readFile(inputMapFolder + "edges_RR.txt");
		int wayCount = 0;
		for (String line : wayLines) {
			String[] lineString = line.split(",");
			if (lineString[0].equals(lineString[1])) {
				LOG.warn("The edge connects two same points: " + lineString[0]);
				continue;
			}
			if (!id2NodeMap.containsKey(lineString[0]) || !id2NodeMap.containsKey(lineString[1]))
				throw new IllegalArgumentException("The end point of this edge is not found: " + lineString[0] + "," + lineString[1]);
			List<RoadNode> wayNodeList = new ArrayList<>();
			wayNodeList.add(id2NodeMap.get(lineString[0]));
			wayNodeList.add(id2NodeMap.get(lineString[1]));
			RoadWay currWay = new RoadWay(wayCount + "", wayNodeList, distFunc);
			wayList.add(currWay);
			occurredNodeIDSet.add(lineString[0]);
			occurredNodeIDSet.add(lineString[1]);
			wayCount++;
		}
		
		RoadNetworkGraph resultMap = new RoadNetworkGraph(false, distFunc);
		for (RoadNode currNode : nodeList) {
			if (occurredNodeIDSet.contains(currNode.getID())) {
				resultMap.addNode(currNode);
			}
		}
		resultMap.addWays(wayList);
		return resultMap;
	}
	
	private void runCode(List<String> goCmd, List<String> pythonCmd) throws Exception {
		// run go command to build the index
		if (goCmd.size() != 2)
			throw new IllegalArgumentException("The Go command for RoadRunner is incorrect.");
		ProcessBuilder goBuilder;
		if (os.equals("Linux")) {
			goBuilder = new ProcessBuilder("/bin/sh", "-c", goCmd.iterator().next());
		} else {
			goBuilder = new ProcessBuilder("cmd.exe", "/c", goCmd.iterator().next());
		}
		goBuilder.redirectErrorStream(true);
		Process goProcess = goBuilder.start();
		BufferedReader goReader = new BufferedReader(new InputStreamReader(goProcess.getInputStream()));
		String goLine;
		while (true) {
			goLine = goReader.readLine();
			if (goLine == null) {
				break;
			}
			LOG.info(goLine);
		}
		
		// start the TraceServer
		if (os.equals("Linux")) {
			Runtime.getRuntime().exec("/bin/sh -c " + goCmd.iterator().next());
		} else {
			Runtime.getRuntime().exec("cmd.exe /c " + goCmd.iterator().next());
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