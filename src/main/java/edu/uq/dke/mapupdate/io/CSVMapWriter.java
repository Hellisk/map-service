package edu.uq.dke.mapupdate.io;

/**
 * Created by uqpchao on 4/07/2017.
 */

import org.jdom2.JDOMException;
import traminer.util.map.roadnetwork.RoadNetworkGraph;
import traminer.util.map.roadnetwork.RoadNode;
import traminer.util.map.roadnetwork.RoadWay;
import traminer.util.spatial.SpatialInterface;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Created by uqpchao on 22/05/2017.
 */
public class CSVMapWriter implements SpatialInterface {
    private final RoadNetworkGraph roadGraph;
    private final String csvVerticesPath;
    private final String csvEdgesPath;

    public CSVMapWriter(RoadNetworkGraph roadNetworkGraph, final String csvVertexPath, final String csvEdgePath) {
        this.roadGraph = roadNetworkGraph;
        this.csvVerticesPath = csvVertexPath;
        this.csvEdgesPath = csvEdgePath;
    }

    public void writeShapeCSV() throws JDOMException, IOException {

        File file = new File(csvVerticesPath);

        if (file.exists())
            file.delete();
        file = new File(csvEdgesPath);
        if (file.exists())
            file.delete();

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
}

