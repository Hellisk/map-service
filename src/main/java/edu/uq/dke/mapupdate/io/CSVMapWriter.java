package edu.uq.dke.mapupdate.io;

/**
 * Created by uqpchao on 4/07/2017.
 */

import traminer.util.map.roadnetwork.RoadNetworkGraph;
import traminer.util.map.roadnetwork.RoadNode;
import traminer.util.map.roadnetwork.RoadWay;
import traminer.util.spatial.SpatialInterface;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

/**
 * Created by uqpchao on 22/05/2017.
 */
public class CSVMapWriter implements SpatialInterface {
    private final RoadNetworkGraph roadGraph;
    private final String csvVerticesPath;
    private final String csvEdgesPath;
    private final String csvRemovedEdgesPath;


    public CSVMapWriter(RoadNetworkGraph roadNetworkGraph, final String csvVertexPath, final String csvEdgePath) {
        this.roadGraph = roadNetworkGraph;
        this.csvVerticesPath = csvVertexPath;
        this.csvEdgesPath = csvEdgePath;
        this.csvRemovedEdgesPath = "";
    }

    public CSVMapWriter(RoadNetworkGraph roadNetworkGraph, final String csvVertexPath, final String csvEdgePath, final String csvRemovedEdgesPath) {
        this.roadGraph = roadNetworkGraph;
        this.csvVerticesPath = csvVertexPath;
        this.csvEdgesPath = csvEdgePath;
        this.csvRemovedEdgesPath = csvRemovedEdgesPath;
    }

    public void writeShapeCSV() throws IOException {
        // create directories before writing
        File file = new File(csvVerticesPath.substring(0, csvVerticesPath.lastIndexOf('/')));
        if (!file.exists()) {
            file.mkdirs();
        }
        // write vertex file
        BufferedWriter bwVertices = new BufferedWriter(new FileWriter(csvVerticesPath));
        for (RoadNode n : roadGraph.getNodes()) {
            bwVertices.write(n.getId() + "," + n.lon() + "," + n.lat() + "\n");
        }
        bwVertices.close();
        // write road way file
        BufferedWriter bwEdges = new BufferedWriter(new FileWriter(csvEdgesPath));
        for (RoadWay w : roadGraph.getWays()) {
            bwEdges.write(w.getId());
            for (RoadNode n : w.getNodes()) {
                bwEdges.write("|" + n.getId() + "," + n.lon() + "," + n.lat());
            }
            bwEdges.write("\n");
        }

        bwEdges.close();
    }

    public void manipulateMap(int percentage) throws IOException {
        // create directories before writing
        File file = new File(csvVerticesPath.substring(0, csvVerticesPath.lastIndexOf('/')));
        if (!file.exists()) {
            file.mkdirs();
        }
        // write vertex file
        BufferedWriter bwVertices = new BufferedWriter(new FileWriter(csvVerticesPath));
        for (RoadNode n : roadGraph.getNodes()) {
            bwVertices.write(n.getId() + "," + n.lon() + "," + n.lat() + "\n");
        }
        bwVertices.close();
        // write road way file
        BufferedWriter bwEdges = new BufferedWriter(new FileWriter(csvEdgesPath));
        BufferedWriter bwRemovedEdges = new BufferedWriter(new FileWriter(csvRemovedEdgesPath));
        Random random = new Random(1);
        for (RoadWay w : roadGraph.getWays()) {
            if (random.nextInt(100) >= percentage) {
                bwEdges.write(w.getId());
                for (RoadNode n : w.getNodes()) {
                    bwEdges.write("|" + n.getId() + "," + n.lon() + "," + n.lat());
                }
                bwEdges.write("\n");
            } else {
                bwRemovedEdges.write(w.getId());
                for (RoadNode n : w.getNodes()) {
                    bwRemovedEdges.write("|" + n.getId() + "," + n.lon() + "," + n.lat());
                }
                bwRemovedEdges.write("\n");
            }
        }
        bwEdges.close();
        bwRemovedEdges.close();
    }
}

