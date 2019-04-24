package algorithm.mapinference.kde;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import util.settings.BaseProperty;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Starter of the Biagioni KDE map inference algorithm. The original code is written in Python and we run the Python code through this
 * class.
 * <p>
 * Reference: [SIGSPATIAL12] Map Inference in the Face of Noise and Disparity
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
	
	public KDEMapInference() {
		this.cellSize = 1;
		this.gaussianBlur = 17;
	}
	
	// use python script to run map inference python code
	public void startMapInference(String rootPath, String inputTrajFolder, String outputMapFolder) throws IOException {
		List<String> pythonCmd = new ArrayList<>();
		
		// remove the map inference directory
		FileUtils.cleanDirectory(new File(outputMapFolder));
		FileUtils.deleteDirectory(new File(outputMapFolder));
		
		// setup each command manually
		pythonCmd.add("python " + rootPath + "kde.py -c " + this.cellSize + " -b " + this.gaussianBlur + " -p " + inputTrajFolder);
		pythonCmd.add("python " + rootPath + "skeleton.py");
		pythonCmd.add("python " + rootPath + "graph_extract.py");
		pythonCmd.add("python " + rootPath + "graphdb_matcher_run.py -t " + inputTrajFolder);
		pythonCmd.add("python " + rootPath + "process_map_matches.py");
		pythonCmd.add("python " + rootPath + "refine_topology.py");
//        pythonCmd.add("python " + rootPath + "graphdb_matcher_run.py -d skeleton_maps/skeleton_map_1m_mm1_tr.db -o " +
//                "matched_trips_1m_mm1_tr/ -t " + inputTrajFolder);
//        pythonCmd.add("python " + rootPath + "process_map_matches.py -d skeleton_maps/skeleton_map_1m_mm1_tr.db -t " +
//                "matched_trips_1m_mm1_tr - o skeleton_maps/skeleton_map_1m_mm2.db");
		pythonCmd.add("python " + rootPath + "streetmap.py");
		
		try {
			runCode(pythonCmd);
			System.gc();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void runCode(List<String> pythonCmd) throws Exception {
		if (os.equals("linux")) {
			for (String s : pythonCmd) {
				Runtime r = Runtime.getRuntime();
				Process p = r.exec(s);
				p.waitFor();
				BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
				String line = "";
				while ((line = br.readLine()) != null) {
					LOG.info(line);
				}
			}
		} else {
			StringBuilder command = new StringBuilder();
			command.append(pythonCmd.get(0));
			for (int i = 1; i < pythonCmd.size(); i++) {
				String pc = pythonCmd.get(i);
				command.append(" && ").append(pc);
			}
			ProcessBuilder builder = new ProcessBuilder("cmd.exe", "/c", command.toString());
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
}
