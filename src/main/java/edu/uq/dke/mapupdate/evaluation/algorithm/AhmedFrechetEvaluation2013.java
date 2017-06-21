/*
Path-based graph distance 1.0
Copyright 2014 Mahmuda Ahmed, K. S. Hickmann and Carola Wenk

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

------------------------------------------------------------------------

This software is based on the following article. Please cite this
article when using this code as part of a research publication:

M. Ahmed, K. S. Hickmann, and C. Wenk.
Path-based distance for street map comparison.
arXiv:1309.6131, 2013.
------------------------------------------------------------------------

Author: Mahmuda Ahmed
Filename: BenchmarkFrechetExperiments.java
 */
package edu.uq.dke.mapupdate.evaluation.algorithm;
//

import edu.uq.dke.mapupdate.evaluation.io.GeneratePaths;
import edu.uq.dke.mapupdate.evaluation.io.MapMatching;
import edu.uq.dke.mapupdate.evaluation.io.ReadFiles;
import edu.uq.dke.mapupdate.evaluation.io.Vertex;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;

public class AhmedFrechetEvaluation2013 {

    public static Logger logger = Logger.getLogger("MapUpdate");

    /**
     * @param cityName           - name of the map.
     * @param inputMapPath       - the path of the input map.
     * @param groundTruthMapPath - the map to be compaired.
     * @param isDirected         - if both of them are directed maps.
     * @param boundary           - the boundaries of matching area. () = no boundary, (xlow,xhigh,ylow,yhigh) = actual boundary.
     */
    public static void AhmedFrechetEvaluation(String cityName, String inputMapPath, String groundTruthMapPath, boolean isDirected, String boundary, String evaluationResultPath, String pathGenerationPath, String linkLength) {

        GeneratePaths gp = new GeneratePaths();
        MapMatching mapMatching = new MapMatching();

        HashMap<Long, Integer> map1 = new HashMap<Long, Integer>();
        HashMap<Long, Integer> map2 = new HashMap<Long, Integer>();

        ArrayList<Vertex> graph1 = new ArrayList<Vertex>();
        ArrayList<Vertex> graph2 = new ArrayList<Vertex>();

        String vertexFile1 = inputMapPath + "vertices.txt";
        String edgeFile1 = inputMapPath + "edges.txt";
        String vertexFile2 = groundTruthMapPath + cityName + "_vertices_osm.txt";
        String edgeFile2 = groundTruthMapPath + cityName + "_edges_osm.txt";

        if (boundary.length() == 2) {
            graph1 = ReadFiles.loadBenchmarkMap(map1, vertexFile1, edgeFile1,
                    isDirected);
            graph2 = ReadFiles.loadBenchmarkMap(map2, vertexFile2, edgeFile2,
                    isDirected);
        } else {
            String[] bounds = boundary.substring(1, boundary.length() - 1).split(",");
            if (bounds.length == 4) {
                double XLow = Double.parseDouble(bounds[0]);
                double XHigh = Double.parseDouble(bounds[1]);
                double YLow = Double.parseDouble(bounds[2]);
                double YHigh = Double.parseDouble(bounds[3]);

                graph1 = ReadFiles.loadBenchmarkMap(map1, vertexFile1, edgeFile1,
                        isDirected, XLow, XHigh, YLow, YHigh);
                graph2 = ReadFiles.loadBenchmarkMap(map2, vertexFile2, edgeFile2,
                        isDirected, XLow, XHigh, YLow, YHigh);
            } else System.err.println("argument boundary error, the length is " + bounds.length);

        }

        File pathFile = new File(pathGenerationPath);

        if (!pathFile.exists())
            pathFile.mkdirs();

        gp.generatePathsLinkLength(graph1, pathGenerationPath, linkLength);

        File folder = new File(pathGenerationPath + linkLength);

        System.out.println("processing linkLength " + linkLength + " paths of "
                + "...");
        Long startTime = System.currentTimeMillis();
        int count = 0;
        for (int l = 0; l < 5; l++) {
            File file2 = new File(pathGenerationPath + folder.getName() + "/" + l);

            if (file2.exists()) {
                mapMatching.pathSimilarity(graph2, file2, evaluationResultPath + linkLength,
                        cityName, l);
                count += (file2.listFiles()).length;
            } else {
                System.out.println("Folder doesn't exits..." + pathGenerationPath
                        + linkLength);
            }
        }

        Long endTime = System.currentTimeMillis();
        logger.info("Evaluation finished, time spent:" + cityName + " " + linkLength + " " + count + " "
                + (endTime - startTime) / 60000.0 + "\n");
    }

}
