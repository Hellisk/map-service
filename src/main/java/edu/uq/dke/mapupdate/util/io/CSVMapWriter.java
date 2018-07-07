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
import java.util.*;

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
     * Write a road network to files
     *
     * @param percentage percentage of removed edges
     * @throws IOException Failed map writing
     */
    public void writeMap(int percentage, int iteration, boolean isTempMap) throws IOException {

        String outputMapPath;
        if (iteration == -1)
            outputMapPath = this.csvMapPath;
        else
            outputMapPath = this.csvMapPath + "map/" + iteration + "/";
        // create directories before writing
        File file = new File(outputMapPath.substring(0, outputMapPath.lastIndexOf('/')));
        if (!file.exists()) {
            if (!file.mkdirs()) throw new IOException("ERROR! Failed to create folder.");
        }
        // write vertex file
        BufferedWriter bwVertices;
        if (isTempMap)
            bwVertices = new BufferedWriter(new FileWriter(outputMapPath + "temp_vertices_" + percentage + ".txt"));
        else bwVertices = new BufferedWriter(new FileWriter(outputMapPath + "vertices_" + percentage + ".txt"));
        for (RoadNode n : roadGraph.getNodes())
            bwVertices.write(n.toString() + "\n");
        bwVertices.close();

        // write road way file
        BufferedWriter bwEdges;
        if (isTempMap)
            bwEdges = new BufferedWriter(new FileWriter(outputMapPath + "temp_edges_" + percentage + ".txt"));
        else bwEdges = new BufferedWriter(new FileWriter(outputMapPath + "edges_" + percentage + ".txt"));
        for (RoadWay w : roadGraph.getWays())
            bwEdges.write(w.toString() + "\n");
        bwEdges.close();
        System.out.println("Write " + percentage + "% road map finished.");
    }

    public void randomRoadRemoval(int percentage) throws IOException {

        DecimalFormat df = new DecimalFormat("0.00000");

        if (percentage == 0)
            return;

        // create directories before writing
        File file = new File(csvMapPath.substring(0, csvMapPath.lastIndexOf('/')));
        if (!file.exists()) {
            if (!file.mkdirs()) throw new IOException("ERROR! Failed to create folder.");
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
                bwEdges.write(w.getId() + "|");
                if (w.isNewRoad())
                    bwEdges.write(w.getInfluenceScore() + "|" + w.getConfidenceScore());
                else bwEdges.write("null");
                for (RoadNode n : w.getNodes()) {
                    bwEdges.write("|" + n.getId() + "," + df.format(n.lon()) + "," + df.format(n.lat()));
                }
                bwEdges.write("\n");
            } else {
                bwRemovedEdges.write(w.getId() + "|");
                if (w.isNewRoad())
                    bwRemovedEdges.write(w.getInfluenceScore() + "," + w.getConfidenceScore());
                else bwRemovedEdges.write("null");
                for (RoadNode n : w.getNodes()) {
                    bwRemovedEdges.write("|" + n.getId() + "," + df.format(n.lon()) + "," + df.format(n.lat()));
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
                bwVertices.write(n.getId() + "," + df.format(n.lon()) + "," + df.format(n.lat()) + "\n");
            } else {
                vertexRemovalCount++;
            }
        }

        System.out.println("Random road Removal done. Total removed roads: " + roadRemovalCount + ", total removed nodes:" + vertexRemovalCount);
        bwVertices.close();
    }

    /**
     * Remove the edges from fully enclosed roads and separated double direction roads, both directions are guaranteed to be removed.
     * The removed roads should be long enough.
     *
     * @param percentage The removal percentage
     * @throws IOException File operation exception
     */
    public void popularityBasedRoadRemoval(int percentage) throws IOException {

        if (percentage == 0) {
            System.out.println("WARNING! The reqired remove road ratio is " + percentage + ", the removal is not needed.");
            return;
        }

        // create directories before writing
        File file = new File(csvMapPath.substring(0, csvMapPath.lastIndexOf('/')));
        if (!file.exists()) {
            if (!file.mkdirs()) throw new IOException("ERROR! Failed to create folder.");
        }
        // write road way file
        BufferedWriter bwVertices = new BufferedWriter(new FileWriter(csvMapPath + "vertices_" + percentage + ".txt"));
        BufferedWriter bwEdges = new BufferedWriter(new FileWriter(csvMapPath + "edges_" + percentage + ".txt"));
        BufferedWriter bwRemovedEdges = new BufferedWriter(new FileWriter(csvMapPath + "removedEdges_" + percentage + ".txt"));

        List<RoadWay> wayList = roadGraph.getWays();
        Set<String> removedEdgeIDSet = new LinkedHashSet<>();
        Random random = new Random(10);
        List<RoadWay> removedWayList = new ArrayList<>();
        List<RoadWay> satisfiedRoadList = new ArrayList<>();    // list of road ways that satisfy the conditions
        Map<String, RoadWay> id2RoadWay = new HashMap<>();
        for (RoadWay w : wayList) {
            if (isSatisfiedRoad(w)) {
                satisfiedRoadList.add(w);
                id2RoadWay.put(w.getId(), w);
            }
        }
        int satisfiedRoadCount = satisfiedRoadList.size();
        if (satisfiedRoadList.size() * 100 / (double) wayList.size() < percentage)
            throw new IllegalArgumentException("ERROR! The number of satisfied roads " + satisfiedRoadList.size() + " is less than the " +
                    "required road removal " + wayList.size() * percentage / 100 + ". Consider loose the condition or decrease the " +
                    "removal percentage.");
        while (removedWayList.size() * 100 / (double) wayList.size() < percentage) {
            int currIndex = random.nextInt(satisfiedRoadList.size());
            removedWayList.add(satisfiedRoadList.get(currIndex));
            removedEdgeIDSet.add(satisfiedRoadList.get(currIndex).getId());
            // put the inverse direction road to the removed road list
            if (satisfiedRoadList.get(currIndex).getId().contains("-")) {
                String inverseRoadID = satisfiedRoadList.get(currIndex).getId().substring(1);
                if (id2RoadWay.containsKey(inverseRoadID))
                    if (!removedEdgeIDSet.contains(inverseRoadID)) {
                        removedWayList.add(id2RoadWay.get(inverseRoadID));
                        removedEdgeIDSet.add(inverseRoadID);
                    }
            } else {
                String inversedRoadID = "-" + satisfiedRoadList.get(currIndex).getId();
                if (id2RoadWay.containsKey(inversedRoadID))
                    if (!removedEdgeIDSet.contains(inversedRoadID)) {
                        removedWayList.add(id2RoadWay.get(inversedRoadID));
                        removedEdgeIDSet.add(inversedRoadID);
                    }
            }
        }

        List<RoadNode> nodeList = this.roadGraph.getNodes();
        wayList.removeAll(removedWayList);
        RoadNetworkGraph newGraph = new RoadNetworkGraph();
        newGraph.setNodes(nodeList);
        newGraph.addWays(wayList);
        int removedNodeCount = newGraph.isolatedNodeRemoval();

        // write result to the files
        for (RoadNode n : newGraph.getNodes())
            bwVertices.write(n.toString() + "\n");
        for (RoadWay w : newGraph.getWays())
            bwEdges.write(w.toString() + "\n");
        for (RoadWay w : removedWayList)
            bwRemovedEdges.write(w.toString() + "\n");
        bwVertices.close();
        bwEdges.close();
        bwRemovedEdges.close();

        System.out.println("Random road Removal done. Total number of satisfied roads: " + satisfiedRoadCount + ", total removed " +
                "roads: " + removedWayList.size() + ", total removed nodes:" + removedNodeCount);
    }

    private boolean isSatisfiedRoad(RoadWay w) {
        if (w.getRoadWayLevel() <= 4 || w.getRoadWayLevel() == 9)
            return false;
//        if (!w.getRoadWayType().get(1) && !w.getRoadWayType().get(2) && !w.getRoadWayType().get(21))
//            return false;
        if (w.getRoadLength() < 20)
            return false;
        return w.getVisitCount() >= 3;
    }
}