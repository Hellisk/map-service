package edu.uq.dke.mapupdate.evaluation;

import edu.uq.dke.mapupdate.util.function.GreatCircleDistanceFunction;
import edu.uq.dke.mapupdate.util.function.PointDistanceFunction;
import edu.uq.dke.mapupdate.util.index.grid.Grid;
import edu.uq.dke.mapupdate.util.object.datastructure.XYObject;
import edu.uq.dke.mapupdate.util.object.roadnetwork.RoadNetworkGraph;
import edu.uq.dke.mapupdate.util.object.roadnetwork.RoadNode;
import edu.uq.dke.mapupdate.util.object.roadnetwork.RoadWay;
import edu.uq.dke.mapupdate.util.object.spatialobject.Point;
import edu.uq.dke.mapupdate.util.object.spatialobject.Segment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by uqpchao on 12/07/2017.
 */
public class MapMatchingEvaluation {
    private double DISTANCE_THRESHOLD = 10;

    public MapMatchingEvaluation(double threshold) {
        this.DISTANCE_THRESHOLD = threshold;
    }

    public MapMatchingEvaluation() {
    }

    public void precisionRecallEval(RoadNetworkGraph inferredMap, RoadNetworkGraph removedMap, RoadNetworkGraph rawMap) {
        Map<String, Boolean> pointCategoryMapping = new HashMap<>();     // indicates whether the point belongs to the removed map
        Map<String, String> rawAdjacencyList = new HashMap<>();         // adjacency relationship in raw map
        Map<String, String> removedAdjacencyList = new HashMap<>();     // adjacency relationship in removed map
        Map<String, String> inferredMapPointMapping = new HashMap<>();
        Map<String, List<String>> farAwayPointAdjacentList = new HashMap<>(); // store all points that has no neighbor and their adjacent node list
//        DecimalFormat df = new DecimalFormat("0.00000");
        // build grid index for removed map and raw map
        int avgNodePerGrid = 64;
        int cellNum;    // total number of cells
        int rowNum;     // number of rows and columns
        int roughRawNodeCount = rawMap.getNodes().size();   // roughly summarize the number of intermediate nodes, duplicated nodes are counted
        for (RoadWay w : rawMap.getWays()) {
            roughRawNodeCount += w.getNodes().size();
        }
        cellNum = roughRawNodeCount / avgNodePerGrid;
        rowNum = (int) Math.ceil(Math.sqrt(cellNum));
        Grid<Point> rawGridIndex = new Grid<>(rowNum, rowNum, rawMap.getMinLon(), rawMap.getMinLat(), rawMap.getMaxLon(), rawMap.getMaxLat());
        Grid<Point> removedGridIndex = new Grid<>(rowNum, rowNum, rawMap.getMinLon(), rawMap.getMinLat(), rawMap.getMaxLon(), rawMap.getMaxLat());

        int actualRawNodeCount = 0;
        // add map points into grid
        for (RoadNode p : rawMap.getNodes()) {
            Point point = new Point(p.lon(), p.lat());
            point.setId(p.getId());
            XYObject<Point> xyPoint = new XYObject<>(point.x(), point.y(), point);
            rawGridIndex.insert(xyPoint);
            pointCategoryMapping.put(p.lon() + "_" + p.lat(), false);
            actualRawNodeCount++;
        }

        for (RoadWay t : rawMap.getWays()) {
            for (int i = 1; i < t.getNodes().size(); i++) {
                if (!pointCategoryMapping.containsKey(t.getNode(i).lon() + "_" + t.getNode(i).lat())) {
                    Point point = new Point(t.getNode(i).lon(), t.getNode(i).lat());
                    point.setId(t.getNode(i).getId());
                    XYObject<Point> xyPoint = new XYObject<>(point.x(), point.y(), point);
                    rawGridIndex.insert(xyPoint);
                    pointCategoryMapping.put(t.getNode(i).lon() + "_" + t.getNode(i).lat(), false);
                    actualRawNodeCount++;
                }
                for (int j = 0; j < i; j++) {
                    rawAdjacencyList.put(t.getNode(j).lon() + "_" + t.getNode(j).lat() + "," + t.getNode(i).lon() + "_" + t.getNode(i).lat(), t.getId());
                }
            }
        }

        System.out.println("Grid index build successfully, total number of points:" + actualRawNodeCount + ", rough count was:" + roughRawNodeCount);

        // record removed map information
        for (RoadNode n : removedMap.getNodes()) {
            if (pointCategoryMapping.containsKey(n.lon() + "_" + n.lat())) {
                pointCategoryMapping.replace(n.lon() + "_" + n.lat(), true);
                Point point = new Point(n.lon(), n.lat());
                point.setId(n.getId());
                XYObject<Point> xyPoint = new XYObject<>(point.x(), point.y(), point);
                removedGridIndex.insert(xyPoint);
            } else {
                System.out.println("Removed point cannot be found");
            }
        }
        for (RoadWay w : removedMap.getWays()) {
            List<Segment> edges = w.getEdges();
            for (int i = 0; i < edges.size(); i++) {
                Segment endSegment = edges.get(i);
                for (int j = 0; j <= i; j++) {
                    Segment firstSegment = edges.get(j);
                    Point firstPoint = firstSegment.getCoordinates().get(0);
                    Point secondPoint = endSegment.getCoordinates().get(1);
                    if (pointCategoryMapping.containsKey(firstPoint.x() + "_" + firstPoint.y()) && pointCategoryMapping.containsKey(secondPoint.x() + "_" + secondPoint.y())) {
                        removedAdjacencyList.put(firstPoint.x() + "_" + firstPoint.y() + "," + secondPoint.x() + "_" + secondPoint.y(), w.getId());
                        if (!pointCategoryMapping.get(firstPoint.x() + "_" + firstPoint.y())) {
                            pointCategoryMapping.replace(firstPoint.x() + "_" + firstPoint.y(), true);
                            Point point = new Point(firstPoint.x(), firstPoint.y());
                            point.setId(firstPoint.getId());
                            XYObject<Point> xyPoint = new XYObject<>(point.x(), point.y(), point);
                            removedGridIndex.insert(xyPoint);
                        }
                        if (!pointCategoryMapping.get(secondPoint.x() + "_" + secondPoint.y())) {
                            pointCategoryMapping.replace(secondPoint.x() + "_" + secondPoint.y(), true);
                            Point point = new Point(secondPoint.x(), secondPoint.y());
                            point.setId(secondPoint.getId());
                            XYObject<Point> xyPoint = new XYObject<>(point.x(), point.y(), point);
                            removedGridIndex.insert(xyPoint);
                        }
                    } else {
                        System.out.println("Points in removed road way are not found");
                    }
                }
            }
        }

        PointDistanceFunction distanceFunction = new GreatCircleDistanceFunction();
        int farAwayPointCount = 0;
        // start search the inferred list
        for (RoadNode n : inferredMap.getNodes()) {
            XYObject<Point> nearestNeighbor = removedGridIndex.nearestNeighborSearch(n.lon(), n.lat(), distanceFunction);
            if (distanceFunction.distance(n.toPoint(), nearestNeighbor.getSpatialObject()) < DISTANCE_THRESHOLD) {
                inferredMapPointMapping.put(n.lon() + "_" + n.lat(), nearestNeighbor.getSpatialObject().x() + "_" + nearestNeighbor.getSpatialObject().y());
            } else {
                farAwayPointCount++;
                farAwayPointAdjacentList.put(n.lon() + "_" + n.lat(), new ArrayList<>());
            }
        }

        double matchedRawSegmentCount = 0;
        double matchedRemovedSegmentCount = 0;
        double matchedRawDistance = 0;
        double matchedRemovedDistance = 0;
        double extraRoadCount = 0;

        for (RoadWay w : inferredMap.getWays()) {
            for (Segment s : w.getEdges()) {
                if (inferredMapPointMapping.containsKey(s.x1() + "_" + s.y1()) && inferredMapPointMapping.containsKey(s.x2() + "_" + s.y2())) {
                    String[] coordinate = inferredMapPointMapping.get(s.x1() + "_" + s.y1()).split("_");
                    Point startPoint = new Point(Double.parseDouble(coordinate[0]), Double.parseDouble(coordinate[1]));
                    coordinate = inferredMapPointMapping.get(s.x2() + "_" + s.y2()).split("_");
                    Point endPoint = new Point(Double.parseDouble(coordinate[0]), Double.parseDouble(coordinate[1]));
                    if (removedAdjacencyList.containsKey(inferredMapPointMapping.get(s.x1() + "_" + s.y1()) + "," + inferredMapPointMapping.get(s.x2() + "_" + s.y2()))) {
                        matchedRemovedSegmentCount++;
                        matchedRemovedDistance += distanceFunction.distance(startPoint, endPoint);
                    } else if (rawAdjacencyList.containsKey(inferredMapPointMapping.get(s.x1() + "_" + s.y1()) + "," + inferredMapPointMapping.get(s.x2() + "_" + s.y2()))) {
                        matchedRawSegmentCount++;
                        matchedRawDistance += distanceFunction.distance(startPoint, endPoint);
                    } else {
                        extraRoadCount++;
                    }
                }
            }
        }

        System.out.println("Total matched point:" + farAwayPointCount + ", total found points" + inferredMap.getNodes().size());
        System.out.println("matchedRawSegmentCount = " + matchedRawSegmentCount);
        System.out.println("matchedRemovedSegmentCount = " + matchedRemovedSegmentCount);
        System.out.println("matchedRawDistance = " + matchedRawDistance);
        System.out.println("matchedRemovedDistance = " + matchedRemovedDistance);
        System.out.println("extraRoadCount = " + extraRoadCount);
    }
}
