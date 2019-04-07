package mapupdate.mapinference;

import org.apache.commons.io.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import static mapupdate.Main.*;

/**
 * Starter of the Biagioni KDE map inference algorithm. The original code is written in Python and we run the Python code through this
 * class.
 * <p>
 * Reference: [SIGSPATIAL12] Map Inference in the Face of Noise and Disparity
 *
 * @author uqpchao
 */

public class KDEMapInference {
    private int cellSize;    // meter
    private int gaussianBlur;

    public KDEMapInference(int cellSize, int gaussianBlur) {
        this.cellSize = cellSize;
        this.gaussianBlur = gaussianBlur;
    }

    public KDEMapInference() {
        this.cellSize = 1;
        this.gaussianBlur = 17;
    }

    // use python script to run map inference python code
    public void startMapInference(String rootPath, String inputTrajPath) throws IOException {
        List<String> pythonCmd = new ArrayList<>();

        // remove the map inference directory
        FileUtils.cleanDirectory(new File(INFERENCE_FOLDER));
        FileUtils.deleteDirectory(new File(INFERENCE_FOLDER));

        // setup each command manually
        pythonCmd.add("python " + rootPath + "kde.py -c " + this.cellSize + " -b " + this.gaussianBlur + " -p " + inputTrajPath);
        pythonCmd.add("python " + rootPath + "skeleton.py");
        pythonCmd.add("python " + rootPath + "graph_extract.py");
        pythonCmd.add("python " + rootPath + "graphdb_matcher_run.py -t " + inputTrajPath);
        pythonCmd.add("python " + rootPath + "process_map_matches.py");
        pythonCmd.add("python " + rootPath + "refine_topology.py");
//        pythonCmd.add("python " + rootPath + "graphdb_matcher_run.py -d skeleton_maps/skeleton_map_1m_mm1_tr.db -o " +
//                "matched_trips_1m_mm1_tr/ -t " + inputTrajPath);
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
        if (WORKSPACE == 3) {
            for (String s : pythonCmd) {
                Runtime r = Runtime.getRuntime();
                Process p = r.exec(s);
                p.waitFor();
                BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String line = "";
                while ((line = br.readLine()) != null) {
                    LOGGER.info(line);
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
                LOGGER.info(line);
            }
        }
    }
}
