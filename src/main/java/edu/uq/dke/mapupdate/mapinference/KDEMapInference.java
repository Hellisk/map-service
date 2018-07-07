package edu.uq.dke.mapupdate.mapinference;

import org.apache.commons.io.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import static edu.uq.dke.mapupdate.Main.INFERENCE_FOLDER;

class KDEMapInference {
    private double cellSize;    // meter
    private int gaussianBlur;

    KDEMapInference(double cellSize, int gaussianBlur) {
        this.cellSize = cellSize;
        this.gaussianBlur = gaussianBlur;
    }

    KDEMapInference() {
        this.cellSize = 1;
        this.gaussianBlur = 17;
    }

    // use python script to run map inference python code
    void startMapInference(String rootPath, String inputTrajPath) throws IOException {
        List<String> pythonCmd = new ArrayList<>();

        // remove the map inference directory
        FileUtils.deleteDirectory(new File(INFERENCE_FOLDER));

        // setup each command manually
        if (this.cellSize != 1 || this.gaussianBlur != 17) {
            pythonCmd.add("python " + rootPath + "kde.py -c " + this.cellSize + " -b " + this.gaussianBlur + " -p " + inputTrajPath);
        } else {
            pythonCmd.add("python " + rootPath + "kde.py" + " -p " + inputTrajPath);
        }
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

        StringBuilder command = new StringBuilder();
        command.append(pythonCmd.get(0));
        for (int i = 1; i < pythonCmd.size(); i++) {
            String pc = pythonCmd.get(i);
            command.append(" && ").append(pc);
        }
        System.out.println(command);
        try {
            runCode(command.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void runCode(String s) throws Exception {
        ProcessBuilder builder = new ProcessBuilder(
                "cmd.exe", "/c", s);
        builder.redirectErrorStream(true);
        Process p = builder.start();
        BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String line;
        while (true) {
            line = r.readLine();
            if (line == null) {
                break;
            }
            System.out.println(line);
        }
    }
}
