package edu.uq.dke.mapupdate.mapmerge;

import edu.uq.dke.mapupdate.util.object.roadnetwork.RoadNetworkGraph;
import edu.uq.dke.mapupdate.util.object.roadnetwork.RoadNode;
import edu.uq.dke.mapupdate.util.object.roadnetwork.RoadWay;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SPBasedRoadWayFiltering {
    private RoadNetworkGraph originalGraph = new RoadNetworkGraph();
    private RoadNetworkGraph inferenceGraph = new RoadNetworkGraph();
    private RoadNetworkGraph removedGraph = new RoadNetworkGraph();
    private Map<String, RoadWay> locPairRoadWayMap = new HashMap<>();
    private int avgNodePerGrid = 64;

    public SPBasedRoadWayFiltering(RoadNetworkGraph originalGraph, RoadNetworkGraph inferenceGraph, RoadNetworkGraph removedGraph, int avgNodePerGrid) {
        this.originalGraph = originalGraph;
        this.inferenceGraph = inferenceGraph;
        this.removedGraph = removedGraph;
        this.avgNodePerGrid = avgNodePerGrid;
    }

    public RoadNetworkGraph SPBasedMapMerge() {
        RoadNetworkGraph concatenatedInferenceResult = graphOptimisation(inferenceGraph);

        // calculate the grid settings
        int cellNum;    // total number of cells
        int rowNum;     // number of rows and columns
        int nodeCount = originalGraph.getNodes().size();
        Set<String> nodeLocationList = new HashSet<>();
        for (RoadWay w : originalGraph.getWays()) {
            for (RoadNode n : w.getNodes()) {
                if (!nodeLocationList.contains(n.lon() + "_" + n.lat())) {
                    nodeLocationList.add(n.lon() + "_" + n.lat());
                    nodeCount++;
                } else {
                    System.out.println("Duplicated road nodes in sp-based network index");
                }
            }
        }
        cellNum = nodeCount / avgNodePerGrid;
        rowNum = (int) Math.ceil(Math.sqrt(cellNum));
//        this.grid = new Grid<>(rowNum, rowNum, originalGraph.getMinLon(), originalGraph.getMinLat(), originalGraph.getMaxLon(), originalGraph.getMaxLat());

        System.out.println("Total number of nodes in grid index:" + originalGraph.getNodes().size());
        System.out.println("The grid contains " + rowNum + "rows and columns");


        return null;
    }

    private RoadNetworkGraph graphOptimisation(RoadNetworkGraph currGraph) {

        return null;
    }
}
