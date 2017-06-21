package edu.uq.dke.mapupdate.mapmatching.algorithm;

import edu.uq.dke.mapupdate.mapmatching.io.PointWithEdges;
import traminer.util.Pair;
import traminer.util.exceptions.MapMatchingException;
import traminer.util.graph.path.Graph;
import traminer.util.graph.path.Vertex;
import traminer.util.map.matching.MapMatchingMethod;
import traminer.util.map.matching.MatchPair;
import traminer.util.map.roadnetwork.RoadNetworkGraph;
import traminer.util.map.roadnetwork.RoadNode;
import traminer.util.map.roadnetwork.RoadWay;
import traminer.util.spatial.distance.EuclideanDistanceFunction;
import traminer.util.spatial.distance.GPSDistanceFunction;
import traminer.util.spatial.distance.PointDistanceFunction;
import traminer.util.spatial.objects.Edges;
import traminer.util.spatial.objects.Point;
import traminer.util.spatial.objects.XYObject;
import traminer.util.spatial.objects.st.STPoint;
import traminer.util.spatial.structures.grid.Grid;
import traminer.util.trajectory.Trajectory;

import java.util.*;

/**
 * Created by Hellisk on 23/05/2017.
 */
public class PointBasedFastMatching implements MapMatchingMethod {
    private static final double DEFAULT_DISTANCE = 18;
    private static final double MU_A = 10;
    private static final double MU_D = 0.17;
    private static final double C_A = 4;
    private static final double C_D = 1.4;

    private Grid<PointWithEdges> grid;
    private final PointDistanceFunction distanceFunction;

    public PointBasedFastMatching(RoadNetworkGraph inputMap, PointDistanceFunction distFunc, int avgNodePerGrid) {
        this.distanceFunction = distFunc;
        buildGridIndex(inputMap, avgNodePerGrid);
    }

    public PointBasedFastMatching(RoadNetworkGraph inputMap, int avgNodePerGrid) {
        this.distanceFunction = new EuclideanDistanceFunction();
        buildGridIndex(inputMap, avgNodePerGrid);
    }

    @Override
    public RoadWay doMatching(Trajectory trajectory, RoadNetworkGraph roadNetworkGraph) throws MapMatchingException {

        // generate candidate segment set for every point in trajectory
//        HashMap<String, List<Edges>> candidateEdgeList = new HashMap<>();
        Point prevPoint = new Point();
        List<Edges> prevCandidateEdges = new ArrayList<>();
        List<Pair<Point, List<Point>>> candidatePointList = new ArrayList<>();
        for (Point p : trajectory.getCoordinates()) {
            List<XYObject<PointWithEdges>> candidateSet = this.grid.kNearestNeighborsSearch(p.x(), p.y(), 50, this.distanceFunction);
            if (candidateSet.size() != 50) {
                System.out.println("SEVERE ERROR, size is:" + candidateSet.size());
            }
//        for(XYObject<PointWithEdges> p : candidateSet){
//            p.getSpatialObject().display();
//        }

//            boolean test2 = false;
//            if(!prevPoint.equals(new Point())) {
//                for (XYObject<PointWithEdges> x : candidateSet) {
//                    if (!prevCandidatePoints.contains(x)) {
//                        test2 = true;
//                        System.out.println("Point list not completely same");
//                    }
//                }
//            }
            // generate candidate segment set
            List<Edges> candidateEdges = new ArrayList<>();
            for (XYObject<PointWithEdges> t : candidateSet) {
                for (Edges a : t.getSpatialObject().getAdjacentEdges()) {
                    if (!candidateEdges.contains(a)) {
                        candidateEdges.add(a);
                    }
                }
            }
//            boolean test = false;
//            if(!prevCandidateEdges.isEmpty()) {
//                for (Edges x : candidateEdges) {
//                    if (!prevCandidateEdges.contains(x)) {
//                        test = true;
//                        System.out.println("edge list not completely same");
//                    }
//                }
//            }

            List<Edges> filteredEdges = candidateEdgeFilter(p, prevPoint, candidateEdges, prevCandidateEdges);

            if (filteredEdges.isEmpty()) {
                double maxScore = -Double.MAX_VALUE;
                List<Pair<Edges, Double>> edgeScore = new ArrayList<>();
                for (Edges x : candidateEdges) {
                    Pair<Edges, Double> currentEdgeScore = calculateScore(p, prevPoint, x);
                    maxScore = maxScore >= currentEdgeScore._2() ? maxScore : currentEdgeScore._2();
                    edgeScore.add(currentEdgeScore);
                }

                for (int i = 0; i < edgeScore.size(); i++) {
                    if (edgeScore.get(i)._2() < 0.8 * maxScore && maxScore >= 0) {
                        edgeScore.remove(i);
                        i--;
                    } else {
                        filteredEdges.add(edgeScore.get(i)._1());
                    }
                }
                if (filteredEdges.isEmpty()) {
                    System.out.println("maxScore = " + maxScore);
                }
            }

            List<Point> representativePointList = new ArrayList<>(filteredEdges.size());
            for (Edges e : filteredEdges) {
                Point representPoint = representativePointGen(p, e);
                representativePointList.add(representPoint);
            }

            candidatePointList.add(new Pair<Point, List<Point>>(p, representativePointList));
            // store the current point information
            prevPoint = p;
            prevCandidateEdges = candidateEdges;
        }

        List<RoadNode> bestRoutePointList = bestRouteGen(candidatePointList);

        if (bestRoutePointList.size() == trajectory.getCoordinates().size()) {

            return new RoadWay(String.valueOf(trajectory.getId()), bestRoutePointList);
        } else {
            System.err.println("ERROR,size compare:" + bestRoutePointList.size() + "," + trajectory.getCoordinates().size());
            return null;
        }

    }

    @Override
    public List<MatchPair> doMatching(Collection<STPoint> pointsList, Collection<RoadNode> nodesList) throws
            MapMatchingException {
        return null;
    }

    private void buildGridIndex(RoadNetworkGraph inputMap, int avgNodePerGrid) {

        // calculate the grid settings
        int cellNum = 0;    // total number of cells
        int rowNum = 0;     // number of rows and columns
        cellNum = inputMap.getNodes().size() / avgNodePerGrid;
        rowNum = (int) Math.ceil(Math.sqrt(cellNum));
        this.grid = new Grid<PointWithEdges>(rowNum, rowNum, inputMap.getMinLon(), inputMap.getMinLat(), inputMap.getMaxLon(), inputMap.getMaxLat());

        System.out.println("Total number of nodes in grid index:" + inputMap.getNodes().size());
        System.out.println("The grid contains " + rowNum + "rows and columns");

        // add all map nodes and edges into a hash map
        Map<String, List<Edges>> adjacentList = new HashMap<>();
        for (RoadNode p : inputMap.getNodes()) {
            if (!adjacentList.containsKey(p.lon() + "," + p.lat())) {
                adjacentList.put(p.lon() + "," + p.lat(), new ArrayList<>());
            } else System.err.println("duplicated RoadNode!");

        }

        for (RoadWay t : inputMap.getWays()) {
            for (Edges a : t.getEdges()) {
                for (Point b : a.getCoordinates()) {
                    if (adjacentList.containsKey(b.x() + "," + b.y())) {
                        adjacentList.get(b.x() + "," + b.y()).add(a);
                    }
                }
            }
        }

        int pointCount = 0;
        for (Map.Entry<String, List<Edges>> entry : adjacentList.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                String[] coordinate = entry.getKey().split(",");
                PointWithEdges newPoint = new PointWithEdges(Double.parseDouble(coordinate[0]), Double.parseDouble(coordinate[1]), entry.getValue());
                XYObject<PointWithEdges> point = new XYObject<PointWithEdges>(newPoint.x(), newPoint.y(), newPoint);
                this.grid.insert(point);
                pointCount++;
            }
        }

        System.out.println("Grid index build successfully, total number of points:" + pointCount);
    }

    private Point representativePointGen(Point trajectoryPoint, Edges candidateEdge) {
        double a = (candidateEdge.y1() - candidateEdge.y2()) / (candidateEdge.x1() - candidateEdge.x2());
        double b = (candidateEdge.y1() - a * candidateEdge.y2());
        double m = trajectoryPoint.x() + a * trajectoryPoint.y();

        double representPx = (m - a * b) / (a * a + 1);
        double representPy = a * representPx + b;

        // check whether the perpendicular point is outside the segment
        if (candidateEdge.x1() < candidateEdge.x2()) {
            if (representPx < candidateEdge.x1()) {
                representPx = candidateEdge.x1();
                representPy = candidateEdge.y1();
            } else if (representPx > candidateEdge.x2()) {
                representPx = candidateEdge.x2();
                representPy = candidateEdge.y2();
            }
        } else if (representPx < candidateEdge.x2()) {
            representPx = candidateEdge.x2();
            representPy = candidateEdge.y2();
        } else if (representPx > candidateEdge.x1()) {
            representPx = candidateEdge.x1();
            representPy = candidateEdge.y1();
        }
        return new Point(representPx, representPy);
    }

    private Pair<Edges, Double> calculateScore(Point p, Point prevPoint, Edges e) {
        // calculate the angle between p(i-1)p(i) and edge e
        double angleTrajectoryEdge = Math.atan2(p.y() - prevPoint.y(), p.x() - prevPoint.x());
        double angleCandidateEdge = Math.atan2(e.y2() - e.y1(), e.x2() - e.x1());
        double angle = Math.abs(angleCandidateEdge - angleTrajectoryEdge);

        //calculate the distance between p and edge e
        GPSDistanceFunction dist = new GPSDistanceFunction();
        double pointToEdgeDistance = dist.pointToSegmentDistance(p, e);

        // final score(p(i),e) calculation
        double score = MU_A * Math.pow(Math.cos(angle), C_A) - MU_D * Math.pow(pointToEdgeDistance, C_D);
        return new Pair<Edges, Double>(e, score);
    }

    /**
     * filter out the candidate roads that are less likely to be matched according to three principles:
     * distance between edge and trajectory point is more than DEFAULT_DISTANCE
     * not appeared in the candidate edge set of ancester of current point
     * not share any endpoint of the candidate edge of ancester of current point
     *
     * @param currentPoint     current trajectory point
     * @param prevPoint        the ancestor of current point
     * @param currentEdgesList candidate edges list of current point
     * @param prevEdgesList    the ancestor of current point   @return filtered Edges
     */
    private List<Edges> candidateEdgeFilter(Point currentPoint, Point prevPoint, List<Edges> currentEdgesList, List<Edges> prevEdgesList) {
        GPSDistanceFunction dist = new GPSDistanceFunction();

        // for the first point of the trajectory
        if (prevEdgesList.isEmpty() || prevPoint.equals(new Point())) {
            return currentEdgesList;
        }
        List<Edges> filteredEdges = new ArrayList<>();
        filteredEdges.addAll(currentEdgesList);
        double angleTrajectoryEdge = Math.atan2(currentPoint.y() - prevPoint.y(), currentPoint.x() - prevPoint.x());
//        System.out.println("Current angle is " + angleTrajectoryEdge);
        for (int i = 0; i < filteredEdges.size(); i++) {
            // check if the edge is too far from the point currentPoint
            double distance = dist.pointToSegmentDistance(currentPoint, filteredEdges.get(i));
//            System.out.println("Distance is:" + dist.pointToSegmentDistance(currentPoint, filteredEdges.get(i)));
            if (dist.pointToSegmentDistance(currentPoint, filteredEdges.get(i)) <= DEFAULT_DISTANCE) {
                // check if the current edge occurred in the previous candidate set.
                if (!prevEdgesList.contains(filteredEdges.get(i))) {
                    // check if any of the previous candidate edge connect the current one
                    boolean connected = false;
                    for (Edges e : prevEdgesList) {
                        if (e.getCoordinates().contains(new Point(filteredEdges.get(i).x1(), filteredEdges.get(i).y1()))
                                || e.getCoordinates().contains(new Point(filteredEdges.get(i).x2(), filteredEdges.get(i).y2()))) {
                            connected = true;
                            break;
                        }
                    }
                    if (!connected) {
                        filteredEdges.remove(i);
                        i--;
                        continue;
                    }
                }

                // check the direction of the candidate edge and trajectory segment
                double angleCandidateEdge = Math.atan2(filteredEdges.get(i).y2() - filteredEdges.get(i).y1(), filteredEdges.get(i).x2() - filteredEdges.get(i).x1());
//                System.out.println("angel =" + (angleCandidateEdge - angleTrajectoryEdge) / Math.PI);
                if (Math.abs(angleCandidateEdge - angleTrajectoryEdge) / Math.PI > 0.5) {
                    filteredEdges.remove(i);
                    i--;
                }
            } else {
                filteredEdges.remove(i);
                i--;
            }
        }
        return filteredEdges;
    }

    public List<RoadNode> bestRouteGen(List<Pair<Point, List<Point>>> candidatePoints) {
        // create virtual graph to find shortest path
        Graph g = new Graph();
        int idCount = 0; // the pointer for newly added vertex
        int prevLevelStartID = 0; // the pointer for the start of the last level
        int counter = 0;
        GPSDistanceFunction dist = new GPSDistanceFunction();
        for (int i = 0; i < candidatePoints.size() + 1; i++) {
            if (i == 0) {
                g.addVertex(idCount, new ArrayList<>());
                g.addVertexInfo(idCount, new Vertex(idCount, 0));
                idCount++;
                for (int j = 0; j < candidatePoints.get(i)._2().size(); j++) {
                    Point newPoint = candidatePoints.get(i)._2().get(j);
                    g.addAdjacency(prevLevelStartID, new Vertex(idCount + j, 0, newPoint.x(), newPoint.y()));
                    g.addVertex(idCount + j, new ArrayList<>());
                    g.addVertexInfo(idCount + j, new Vertex(idCount + j, 0, newPoint.x(), newPoint.y()));
                    counter = j;
                }
                prevLevelStartID = idCount;
                idCount = idCount + counter + 1;
            } else if (i == candidatePoints.size()) {
                for (int k = prevLevelStartID; k < idCount; k++) {
                    g.addAdjacency(k, new Vertex(idCount, 0));
                }
                g.addVertexInfo(idCount, new Vertex(idCount, 0));
                g.addVertex(idCount, new ArrayList<>());
                prevLevelStartID = idCount;
                idCount++;
            } else {
                for (int j = 0; j < candidatePoints.get(i)._2().size(); j++) {
                    Point newPoint = candidatePoints.get(i)._2().get(j);
                    Point ancestorPoint;
                    for (int k = prevLevelStartID; k < idCount; k++) {
                        ancestorPoint = new Point(g.getVertexInfo(k).x, g.getVertexInfo(k).y);
                        g.addAdjacency(k, new Vertex(idCount + j, dist.pointToPointDistance(newPoint, ancestorPoint)));
                    }
                    g.addVertexInfo(idCount + j, new Vertex(idCount + j, 0, newPoint.x(), newPoint.y()));
                    g.addVertex(idCount + j, new ArrayList<Vertex>());
                    counter = j;
                }
                if (candidatePoints.get(i)._2().size() != 0) {
                    prevLevelStartID = idCount;
                    idCount = idCount + counter + 1;
                }
            }
        }
        List<Integer> finalPathID = g.getShortestPath(0, prevLevelStartID);
        List<RoadNode> finalPathPoint = new ArrayList<>();

        // remove the final point and reverse the list
        counter = 0;
        for (int i = finalPathID.size() - 1; i > 0; i--) {
            RoadNode newPoint = new RoadNode(String.valueOf(counter), g.getVertexInfo(finalPathID.get(i)).x, g.getVertexInfo(finalPathID.get(i)).y);
            finalPathPoint.add(newPoint);
        }
        return finalPathPoint;

    }

}
