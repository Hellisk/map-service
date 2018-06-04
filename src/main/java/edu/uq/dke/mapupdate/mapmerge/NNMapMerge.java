package edu.uq.dke.mapupdate.mapmerge;

import edu.uq.dke.mapupdate.util.function.GreatCircleDistanceFunction;
import edu.uq.dke.mapupdate.util.index.grid.Grid;
import edu.uq.dke.mapupdate.util.object.datastructure.XYObject;
import edu.uq.dke.mapupdate.util.object.roadnetwork.RoadNetworkGraph;
import edu.uq.dke.mapupdate.util.object.roadnetwork.RoadNode;
import edu.uq.dke.mapupdate.util.object.roadnetwork.RoadWay;
import edu.uq.dke.mapupdate.util.object.spatialobject.Point;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NNMapMerge {
    private RoadNetworkGraph rawMap = new RoadNetworkGraph();
    private RoadNetworkGraph inferredGraph = new RoadNetworkGraph();
    private Map<String, RoadWay> locPairRoadWayMap = new HashMap<>();
    private GreatCircleDistanceFunction distFunc = new GreatCircleDistanceFunction();
    private Grid<Point> grid;
    private int avgNodePerGrid = 64;
    private int maxDistanceThreshold = 50;

    public NNMapMerge(RoadNetworkGraph rawMap, RoadNetworkGraph inferredGraph, int avgNodePerGrid, int maxDistanceThreshold) {
        this.rawMap = rawMap;
        this.inferredGraph = inferredGraph;
        this.avgNodePerGrid = avgNodePerGrid;
        this.maxDistanceThreshold = maxDistanceThreshold;
    }

    public RoadNetworkGraph NearestNeighbourMapMerge() {
        buildGridIndex();
        int matchFoundCount = 0;
        for (RoadWay w : inferredGraph.getWays()) {
            Point startPoint = this.grid.nearestNeighborSearch(w.getFromNode().lon(), w.getFromNode().lat(), distFunc).getSpatialObject();
            Point endPoint = this.grid.nearestNeighborSearch(w.getToNode().lon(), w.getToNode().lat(), distFunc).getSpatialObject();
            if (distFunc.distance(startPoint, w.getFromNode().toPoint()) < maxDistanceThreshold && distFunc.distance(endPoint, w.getToNode()
                    .toPoint()) < maxDistanceThreshold)
                matchFoundCount++;
            // change end points to its closest intersection
            List<RoadNode> roadWayNode = w.getNodes();
            roadWayNode.set(0, new RoadNode(startPoint.getId(), startPoint.x(), startPoint.y()));
            roadWayNode.set(roadWayNode.size() - 1, new RoadNode(endPoint.getId(), endPoint.x(), endPoint.y()));
            w.setNodes(roadWayNode);
            w.setNewRoad(true);
            rawMap.addWay(w);
        }
        System.out.println("Nearest neighbour map merge completed. Total number of road match found:" + matchFoundCount + ", total number " +
                "of road way added:" + inferredGraph.getWays().size());
        return rawMap;
    }

    private void buildGridIndex() {
        // calculate the grid settings
        int cellNum;    // total number of cells
        int rowNum;     // number of rows and columns
        int nodeCount = rawMap.getNodes().size();
//        Set<String> nodeLocationList = new HashSet<>();
//        for (RoadWay w : rawMap.getWays()) {
//            for (RoadNode n : w.getNodes()) {
//                if (!nodeLocationList.contains(n.lon() + "_" + n.lat())) {
//                    nodeLocationList.add(n.lon() + "_" + n.lat());
//                    nodeCount++;
//                } else {
//                    System.out.println("Duplicated road nodes in nearest neighbour network index");
//                }
//            }
//        }
        cellNum = nodeCount / avgNodePerGrid;
        rowNum = (int) Math.ceil(Math.sqrt(cellNum));
        this.grid = new Grid<>(rowNum, rowNum, rawMap.getMinLon(), rawMap.getMinLat(), rawMap.getMaxLon(), rawMap.getMaxLat());

        for (RoadNode n : rawMap.getNodes()) {
            Point nodeIndex = new Point(n.lon(), n.lat());
            nodeIndex.setId(n.getId());
            XYObject<Point> nodeIndexObject = new XYObject<>(nodeIndex.x(), nodeIndex.y(), nodeIndex);
            this.grid.insert(nodeIndexObject);
        }
        System.out.println("Total number of nodes in grid index:" + rawMap.getNodes().size());
        System.out.println("The grid contains " + rowNum + "rows and columns");
    }

}
