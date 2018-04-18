package edu.uq.dke.mapupdate.util.io;

import edu.uq.dke.mapupdate.util.object.SpatialInterface;
import edu.uq.dke.mapupdate.util.object.roadnetwork.RoadNetworkGraph;
import edu.uq.dke.mapupdate.util.object.roadnetwork.RoadNode;
import edu.uq.dke.mapupdate.util.object.roadnetwork.RoadWay;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Random;

/**
 * Created by uqpchao on 22/05/2017.
 */
public class CSVMapWriter implements SpatialInterface {
    private final RoadNetworkGraph roadGraph;
    private final String csvMapPath;


    public CSVMapWriter(RoadNetworkGraph roadNetworkGraph, String csvMapPath) {
        this.roadGraph = roadNetworkGraph;
        this.csvMapPath = csvMapPath;
    }

    /**
     * write a road network to files
     *
     * @param percentage percentage of removed edges
     * @throws IOException
     */
    public void writeMap(int percentage) throws IOException {

        DecimalFormat df = new DecimalFormat(".00000");

        // create directories before writing
        File file = new File(csvMapPath.substring(0, csvMapPath.lastIndexOf('/')));
        if (!file.exists()) {
            file.mkdirs();
        }
        // write vertex file
        BufferedWriter bwVertices = new BufferedWriter(new FileWriter(csvMapPath + "vertices_" + percentage + ".txt"));
        for (RoadNode n : roadGraph.getNodes()) {
            bwVertices.write(n.getId() + "," + df.format(n.lon()) + "," + df.format(n.lat()) + "\n");
        }
        bwVertices.close();

        // write road way file
        BufferedWriter bwEdges = new BufferedWriter(new FileWriter(csvMapPath + "edges_" + percentage + ".txt"));
        for (RoadWay w : roadGraph.getWays()) {
            bwEdges.write(w.getId());
            for (RoadNode n : w.getNodes()) {
                bwEdges.write("|" + n.getId() + "," + df.format(n.lon()) + "," + df.format(n.lat()));
            }
            bwEdges.write("\n");
        }

        bwEdges.close();
        System.out.println("Write " + percentage + "% road map finished.");
    }

    public void randomBasedRoadRemoval(int percentage) throws IOException {

        if (percentage == 0)
            return;

        // create directories before writing
        File file = new File(csvMapPath.substring(0, csvMapPath.lastIndexOf('/')));
        if (!file.exists()) {
            file.mkdirs();
        }

        HashMap<String, Integer> nodeRemovalCount = new HashMap<>();   // for each vertex, the total count of its edges that are removed
        // write road way file
        BufferedWriter bwEdges = new BufferedWriter(new FileWriter(csvMapPath + "edges_" + percentage + ".txt"));
        BufferedWriter bwRemovedEdges = new BufferedWriter(new FileWriter(csvMapPath + "removedEdges_" + percentage + ".txt"));
        int roadRemovalCount = 0;
        int vertexRemovalCount = 0;
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

                // remove one road way from the corresponding end points
                if (nodeRemovalCount.containsKey(w.getFromNode().getId())) {
                    nodeRemovalCount.replace(w.getFromNode().getId(), nodeRemovalCount.get(w.getFromNode().getId()) + 1);
                } else nodeRemovalCount.put(w.getFromNode().getId(), 1);
                if (nodeRemovalCount.containsKey(w.getToNode().getId())) {
                    nodeRemovalCount.replace(w.getToNode().getId(), nodeRemovalCount.get(w.getToNode().getId()) + 1);
                } else nodeRemovalCount.put(w.getToNode().getId(), 1);
                roadRemovalCount++;
            }
        }
        bwEdges.close();
        bwRemovedEdges.close();
        // write vertex file
        BufferedWriter bwVertices = new BufferedWriter(new FileWriter(csvMapPath + "vertices_" + percentage + ".txt"));
        for (RoadNode n : roadGraph.getNodes()) {
            if (!nodeRemovalCount.containsKey(n.getId()) || nodeRemovalCount.get(n.getId()) != n.getDegree()) {
                bwVertices.write(n.getId() + "," + n.lon() + "," + n.lat() + "\n");
            } else {
                vertexRemovalCount++;
            }
        }

        System.out.println("Random road Removal done. Total removed roads: " + roadRemovalCount + ", total removed nodes:" + vertexRemovalCount);
        bwVertices.close();
    }
}