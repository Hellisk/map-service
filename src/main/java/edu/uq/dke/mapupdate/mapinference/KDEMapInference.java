package edu.uq.dke.mapupdate.mapinference;

import java.io.BufferedReader;
import java.io.InputStreamReader;

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
    void startMapInference(String rootPath) {
        String[] pythonCmd = new String[9];

        // setup each command manually
        if (this.cellSize != 1 || this.gaussianBlur != 17) {
            pythonCmd[0] = "python " + rootPath + "kde.py -c " + this.cellSize + " -b " + this.gaussianBlur;
        } else {
            pythonCmd[0] = "python " + rootPath + "kde.py";
        }
        pythonCmd[1] = "python " + rootPath + "skeleton.py";
        pythonCmd[2] = "python " + rootPath + "graph_extract.py";
        pythonCmd[3] = "python " + rootPath + "graphdb_matcher_run.py";
        pythonCmd[4] = "python " + rootPath + "process_map_matches.py";
        pythonCmd[5] = "python " + rootPath + "refine_topology.py";
        pythonCmd[6] = "python " + rootPath + "graphdb_matcher_run.py -d skeleton_maps/skeleton_map_1m_mm1_tr.db -o matched_trips_1m_mm1_tr/";
        pythonCmd[7] = "python " + rootPath + "process_map_matches.py -d skeleton_maps/skeleton_map_1m_mm1_tr.db -t matched_trips_1m_mm1_tr -o skeleton_maps/skeleton_map_1m_mm2.db";
        pythonCmd[8] = "python " + rootPath + "streetmap.py";

        StringBuilder command = new StringBuilder();
        command.append(pythonCmd[0]);
        for (int i = 1; i < pythonCmd.length; i++) {
            String pc = pythonCmd[i];
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
