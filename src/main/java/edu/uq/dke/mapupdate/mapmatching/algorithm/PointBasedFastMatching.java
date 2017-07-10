package edu.uq.dke.mapupdate.mapmatching.algorithm;

import edu.uq.dke.mapupdate.mapmatching.io.PointWithSegment;
import edu.uq.dke.mapupdate.visualisation.GraphStreamDisplay;
import org.graphstream.ui.view.View;
import org.graphstream.ui.view.Viewer;
import traminer.util.Pair;
import traminer.util.exceptions.MapMatchingException;
import traminer.util.graph.path.Graph;
import traminer.util.graph.path.Vertex;
import traminer.util.map.roadnetwork.RoadNetworkGraph;
import traminer.util.map.roadnetwork.RoadNode;
import traminer.util.map.roadnetwork.RoadWay;
import traminer.util.spatial.distance.GPSDistanceFunction;
import traminer.util.spatial.distance.PointDistanceFunction;
import traminer.util.spatial.objects.Point;
import traminer.util.spatial.objects.Segment;
import traminer.util.spatial.objects.XYObject;
import traminer.util.spatial.structures.grid.Grid;
import traminer.util.trajectory.Trajectory;

import java.io.IOException;
import java.util.*;

/**
 * Created by Hellisk on 23/05/2017.
 */
public class PointBasedFastMatching {
    private static final double DEFAULT_DISTANCE = 18;
    private static final double MU_A = 10;
    private static final double MU_D = 0.17;
    private static final double C_A = 4;
    private static final double C_D = 1.4;
    private static final int K = 50;    // k nearest neighbour
    private static final int EXTRA_POINT = 1;     // number of extra points for every unmatched trajectory segment
    private static final int NOISE_POINT = 1;     // number of extra points for every unmatched trajectory segment

    private Grid<PointWithSegment> grid;
    private final PointDistanceFunction distanceFunction;
    private boolean isUpdate = false;

    // for graph navigation
    private Graph navigateGraph = new Graph();
    private Map<String, Integer> vertexIDMap = new HashMap<>();
    private Map<String, String> segmentRoadWayMap = new HashMap<>();

    private GraphStreamDisplay gridDisplay = new GraphStreamDisplay();


    public PointBasedFastMatching(RoadNetworkGraph inputMap, PointDistanceFunction distFunc, int avgNodePerGrid, boolean isUpdate) throws IOException {
        this.distanceFunction = distFunc;
        this.isUpdate = isUpdate;
        buildGridIndex(inputMap, avgNodePerGrid);
        gridDisplay.setGroundTruthGraph(inputMap);
    }

    private void buildGridIndex(RoadNetworkGraph inputMap, int avgNodePerGrid) {

        // calculate the grid settings
        int cellNum;    // total number of cells
        int rowNum;     // number of rows and columns
        int nodeCount = inputMap.getNodes().size();
        Set<String> nodeLocationList = new HashSet<>();
        for (RoadWay w : inputMap.getWays()) {
            for (RoadNode n : w.getNodes()) {
                if (!nodeLocationList.contains(n.lon() + "_" + n.lat())) {
                    nodeLocationList.add(n.lon() + "_" + n.lat());
                    nodeCount++;
                }
            }
        }
        cellNum = nodeCount / avgNodePerGrid;
        rowNum = (int) Math.ceil(Math.sqrt(cellNum));
        this.grid = new Grid<>(rowNum, rowNum, inputMap.getMinLon(), inputMap.getMinLat(), inputMap.getMaxLon(), inputMap.getMaxLat());

        System.out.println("Total number of nodes in grid index:" + inputMap.getNodes().size());
        System.out.println("The grid contains " + rowNum + "rows and columns");

        // add all map nodes and edges into a hash map, both incoming and outgoing segments are included
        Map<String, List<Segment>> adjacentList = new HashMap<>();


        for (RoadNode p : inputMap.getNodes()) {
            adjacentList.put(p.lon() + "_" + p.lat(), new ArrayList<>());
        }

        for (RoadWay t : inputMap.getWays()) {
            for (int i = 0; i < t.getNodes().size() - 1; i++) {
                if (!adjacentList.containsKey(t.getNode(i).lon() + "_" + t.getNode(i).lat())) {
                    adjacentList.put(t.getNode(i).lon() + "_" + t.getNode(i).lat(), new ArrayList<>());
                } else {
                    // double direction road way will have overlapped nodes
                    adjacentList.get(t.getNode(i).lon() + "_" + t.getNode(i).lat()).add(t.getEdges().get(i));
                }
                if (!adjacentList.containsKey(t.getNode(i + 1).lon() + "_" + t.getNode(i + 1).lat())) {
                    adjacentList.put(t.getNode(i + 1).lon() + "_" + t.getNode(i + 1).lat(), new ArrayList<>());
                } else {
                    // double direction road way will have overlapped nodes
                    adjacentList.get(t.getNode(i + 1).lon() + "_" + t.getNode(i + 1).lat()).add(t.getEdges().get(i));
                }

                this.segmentRoadWayMap.put(t.getNode(i).lon() + "_" + t.getNode(i).lat() + "," + t.getNode(i + 1).lon() + "_" + t.getNode(i + 1).lat(), t.getId());
            }
        }

        int pointCount = 0;

        for (Map.Entry<String, List<Segment>> entry : adjacentList.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                String[] coordinate = entry.getKey().split("_");
                PointWithSegment newPoint = new PointWithSegment(Double.parseDouble(coordinate[0]), Double.parseDouble(coordinate[1]), entry.getValue());
                XYObject<PointWithSegment> point = new XYObject<>(newPoint.x(), newPoint.y(), newPoint);
                this.grid.insert(point);
                pointCount++;
            }
        }
        System.out.println("Grid index build successfully, total number of points:" + pointCount);
    }

    public Pair<RoadWay, List<Trajectory>> doMatching(Trajectory trajectory) throws MapMatchingException {

        Point prevPoint = new Point();
        List<Segment> prevCandidateSegments = new ArrayList<>();
        List<Pair<Point, List<PointWithSegment>>> candidatePointList = new ArrayList<>();
        List<Integer> unmatchedPointList = new ArrayList<>();
        List<Trajectory> unmatchedTrajList = new ArrayList<>();
        for (int i = 0; i < trajectory.getCoordinates().size(); i++) {
            List<XYObject<PointWithSegment>> candidateSet = this.grid.kNearestNeighborsSearch(trajectory.get(i).x(), trajectory.get(i).y(), K, this.distanceFunction);
            if (candidateSet.size() != K) {
                System.out.println("SEVERE ERROR, size is:" + candidateSet.size());
            }

            // generate candidate segment set
            List<Segment> candidateSegments = new ArrayList<>();
            for (XYObject<PointWithSegment> t : candidateSet) {
                for (Segment a : t.getSpatialObject().getAdjacentSegments()) {
                    if (!candidateSegments.contains(a)) {
                        candidateSegments.add(a);
                    }
                }
            }

            List<Segment> filteredSegment = candidateSegmentFilter(trajectory.get(i), prevPoint, candidateSegments, prevCandidateSegments);
//            System.out.println("filteredSegment.size() = " + filteredSegment.size());

            if (filteredSegment.isEmpty()) {
                if (isUpdate) {
                    unmatchedPointList.add(i);
                }
                double maxScore = -Double.MAX_VALUE;
                List<Pair<Segment, Double>> edgeScore = new ArrayList<>();
                for (Segment x : candidateSegments) {
                    double currEdgeScore = scoreCalculation(x.getCoordinates().get(0), x.getCoordinates().get(1), trajectory.get(i), prevPoint, x);
                    maxScore = maxScore >= currEdgeScore ? maxScore : currEdgeScore;
                    edgeScore.add(new Pair<>(x, currEdgeScore));
                }

                for (int j = 0; j < edgeScore.size(); j++) {
                    if (maxScore >= 0) {
                        if (edgeScore.get(j)._2() < 0.8 * maxScore) {
                            edgeScore.remove(j);
                            j--;
                        } else {
                            filteredSegment.add(edgeScore.get(j)._1());
                        }
                    } else {
                        if (edgeScore.get(j)._2() < maxScore / 0.8) {
                            edgeScore.remove(j);
                            j--;
                        } else {
                            filteredSegment.add(edgeScore.get(j)._1());
                        }
                    }
                }
                if (filteredSegment.isEmpty()) {
                    System.out.println("maxScore = " + maxScore);
                }
            }

            List<PointWithSegment> representativePointList = new ArrayList<>(filteredSegment.size());
            for (Segment e : filteredSegment) {
                PointWithSegment representPoint = representativePointGen(trajectory.get(i), e);

//                // test
//                double distance0 = Math.sqrt(Math.pow(representPoint.x() - p.x(), 2) + Math.pow(representPoint.y() - p.y(), 2));
//                Point representPointWithoutAlign = representativePointGen2(p, e);
//                double distance1 = Math.sqrt(Math.pow(representPointWithoutAlign.x() - p.x(), 2) + Math.pow(representPointWithoutAlign.y() - p.y(), 2));
//                GPSDistanceFunction dist = new GPSDistanceFunction();
//                double distance2 = dist.distance(representPoint, p);
//                double distance3 = dist.distance(p,e.getCoordinates().get(0));
//                double distance4 = dist.distance(p,e.getCoordinates().get(1));
//                double distance5 = dist.pointToSegmentDistance(p, e);

                representativePointList.add(representPoint);
            }

            candidatePointList.add(new Pair<>(trajectory.get(i), representativePointList));
            // store the current point information
            prevPoint = trajectory.get(i);
            prevCandidateSegments = candidateSegments;
        }

        RoadWay resultWay = bestRouteGen(candidatePointList, trajectory);
        resultWay.setId(trajectory.getId());

        if (isUpdate) {
            Collections.sort(unmatchedPointList);
            int trajCount = 0;
            Trajectory newTraj = new Trajectory();
            for (int i = 0; i < unmatchedPointList.size(); i++) {
                if (i == 0) {
                    if (unmatchedPointList.get(i) != 0) {
                        for (int j = EXTRA_POINT; j > 0; j--) {
                            newTraj.add(trajectory.get(unmatchedPointList.get(i) - j));
                        }
                    }
                    newTraj.add(trajectory.get(unmatchedPointList.get(i)));
                    newTraj.setId(trajectory.getId() + "_" + trajCount);
                    trajCount++;
                } else if (unmatchedPointList.get(i - 1) + 2 * EXTRA_POINT < unmatchedPointList.get(i)) {
                    for (int j = 1; j <= EXTRA_POINT; j++) {
                        newTraj.add(trajectory.get(unmatchedPointList.get(i - 1) + j));
                    }
                    Trajectory tempTraj = new Trajectory(newTraj.getId());
                    tempTraj.addAll(newTraj.getPoints());
                    unmatchedTrajList.add(tempTraj);
                    newTraj.clear();
                    newTraj.setId(trajectory.getId() + "_" + trajCount);
                    for (int j = EXTRA_POINT; j > 0; j--) {
                        newTraj.add(trajectory.get(unmatchedPointList.get(i) - j));
                    }
                    newTraj.add(trajectory.get(unmatchedPointList.get(i)));
                    trajCount++;
                    if (i == unmatchedPointList.size() - 1) {
                        if (unmatchedPointList.get(i) != trajectory.getCoordinates().size() - 1) {
                            for (int j = 1; j <= EXTRA_POINT; j++) {
                                newTraj.add(trajectory.get(unmatchedPointList.get(i) + j));
                            }
                        }
                        Trajectory finalTraj = new Trajectory(newTraj.getId());
                        finalTraj.addAll(newTraj.getPoints());
                        unmatchedTrajList.add(finalTraj);
                    }
                } else {
                    for (int j = 1; unmatchedPointList.get(i - 1) + j <= unmatchedPointList.get(i); j++) {
                        newTraj.add(trajectory.get(unmatchedPointList.get(i - 1) + j));
                    }
                }
            }
            for (int i = 0; i < unmatchedTrajList.size(); i++) {
                if (unmatchedTrajList.get(i).getCoordinates().size() - 2 * EXTRA_POINT <= NOISE_POINT) {
                    unmatchedTrajList.remove(unmatchedTrajList.get(i));
                    i--;
                }
            }
        }
        return new Pair<>(resultWay, unmatchedTrajList);

    }

    private PointWithSegment representativePointGen(Point trajectoryPoint, Segment candidateSegment) {

        double a = candidateSegment.y2() - candidateSegment.y1();
        double b = candidateSegment.x1() - candidateSegment.x2();
        double c = candidateSegment.x2() * candidateSegment.y1() - candidateSegment.x1() * candidateSegment.y2();

        double representPx = (b * b * trajectoryPoint.x() - a * b * trajectoryPoint.y() - a * c) / (a * a + b * b);
        double representPy = (-a * b * trajectoryPoint.x() + a * a * trajectoryPoint.y() - b * c) / (a * a + b * b);

        // check whether the perpendicular point is outside the segment
        if (candidateSegment.x1() < candidateSegment.x2()) {
            if (representPx < candidateSegment.x1()) {
                representPx = candidateSegment.x1();
                representPy = candidateSegment.y1();
            } else if (representPx > candidateSegment.x2()) {
                representPx = candidateSegment.x2();
                representPy = candidateSegment.y2();
            }
        } else if (representPx < candidateSegment.x2()) {
            representPx = candidateSegment.x2();
            representPy = candidateSegment.y2();
        } else if (representPx > candidateSegment.x1()) {
            representPx = candidateSegment.x1();
            representPy = candidateSegment.y1();
        }

        PointWithSegment result = new PointWithSegment(representPx, representPy);
        result.addAdjacentSegment(candidateSegment);
        return result;
    }

    private double scoreCalculation(Point currCandidatePoint, Point prevCandidatePoint, Point currRawPoint, Point prevRawPoint, Segment e) {

        // calculate the angle between p(i-1)p(i) and edge e
        double angle = angleCalculation(prevRawPoint, currRawPoint, prevCandidatePoint, currCandidatePoint);

        //calculate the distance between p and edge e
        GPSDistanceFunction dist = new GPSDistanceFunction();
        double pointToSegmentDistance = dist.pointToSegmentDistance(currRawPoint, e);

        // final score(p(i),e) calculation
        return MU_A * Math.pow(Math.cos(angle), C_A) - MU_D * Math.pow(pointToSegmentDistance, C_D);
    }

    /**
     * filter out the candidate roads that are less likely to be matched according to three principles:
     * distance between edge and trajectory point is more than DEFAULT_DISTANCE
     * not appeared in the candidate edge set of ancestor of current point
     * not share any endpoint of the candidate edge of ancestor of current point
     *
     * @param currPoint       current trajectory point
     * @param prevPoint       the ancestor of current point
     * @param currSegmentList candidate edges list of current point
     * @param prevSegmentList the ancestor of current point   @return filtered Segment
     */
    private List<Segment> candidateSegmentFilter(Point currPoint, Point prevPoint, List<Segment> currSegmentList, List<Segment> prevSegmentList) {
        GPSDistanceFunction dist = new GPSDistanceFunction();

        // for the first point of the trajectory
        if (prevSegmentList.isEmpty() || prevPoint.equals(new Point())) {
            return currSegmentList;
        }
        List<Segment> filteredSegment = new ArrayList<>();
        filteredSegment.addAll(currSegmentList);

//        System.out.println("Current angle is " + angleTrajectoryEdge);
        for (int i = 0; i < filteredSegment.size(); i++) {
            // check if the edge is too far from the point currPoint
//            double distance = dist.pointToSegmentDistance(currPoint, filteredSegment.get(i));
//            System.out.println("Distance is:" + dist.pointToSegmentDistance(currPoint, filteredSegment.get(i)));
            Segment currSegment = filteredSegment.get(i);
            if (dist.pointToSegmentDistance(currPoint, currSegment) <= DEFAULT_DISTANCE) {
                boolean connected = false;
                // check if any of the previous candidate edge connect the current one or is same as current one
                for (Segment e : prevSegmentList) {

                    if (e.getCoordinates().get(1).equals2D(currSegment.getCoordinates().get(0)) || segmentRoadWayMap.get(e.x1() + "_" + e.y1() + "," + e.x2() + "_" + e.y2()).equals(segmentRoadWayMap.get(currSegment.x1() + "_" + currSegment.y1() + "," + currSegment.x2() + "_" + currSegment.y2()))) {
                        connected = true;
                        break;
                    }
                }
                if (!connected) {
                    filteredSegment.remove(i);
                    i--;
                    continue;
                }

                // check the direction of the candidate edge and trajectory segment
                double angle = angleCalculation(prevPoint, currPoint, currSegment.getCoordinates().get(0), currSegment.getCoordinates().get(1));
                if (Math.abs(angle) / Math.PI > 0.5) {
                    filteredSegment.remove(i);
                    i--;
                }
            } else {
                filteredSegment.remove(i);
                i--;
            }
        }
        return filteredSegment;
    }

    private double angleCalculation(Point firstLineStart, Point firstLineEnd, Point secondLineStart, Point secondLineEnd) {
        // used for following angle check
        double firstLineX = firstLineEnd.x() - firstLineStart.x();
        double firstLineY = firstLineEnd.y() - firstLineStart.y();
        double secondLineX = secondLineEnd.x() - secondLineStart.x();
        double secondLineY = secondLineEnd.y() - secondLineStart.y();

        // angle = atan2( a.x*b.y - a.y*b.x, a.x*b.x + a.y*b.y )
//        System.out.println("angel =" + Math.abs(angle) / Math.PI);
        return Math.atan2(firstLineX * secondLineY - firstLineY * secondLineX, firstLineX * secondLineX + firstLineY * secondLineY);
    }

    private RoadWay bestRouteGen(List<Pair<Point, List<PointWithSegment>>> candidatePoints, Trajectory rawTraj) {
        // create virtual graph to find shortest path
        Graph bestRouteGraph = new Graph();
        HashMap<Integer, PointWithSegment> IDCandidatePointMap = new HashMap<>();
        int idCount = 0; // the pointer for newly added vertex
        int prevLevelStartID = 0; // the pointer for the start of the last level
        int counter = 0;
//        GPSDistanceFunction dist = new GPSDistanceFunction();
        // i = trajectory point number, j = candidate point id
        for (int i = 0; i < candidatePoints.size() + 1; i++) {

            // the first point is a virtual point that connects all following points with distance 0
            if (i == 0) {
                bestRouteGraph.addVertex(idCount, new ArrayList<>());
                bestRouteGraph.addVertexInfo(idCount, new Vertex(idCount, 0));
                IDCandidatePointMap.put(idCount, new PointWithSegment(0, 0));
                idCount++;
                // add the points in the first candidate set to the virtual start point
                for (int j = 0; j < candidatePoints.get(i)._2().size(); j++) {
                    PointWithSegment currPoint = candidatePoints.get(i)._2().get(j);
                    bestRouteGraph.addAdjacency(prevLevelStartID, new Vertex(idCount + j, 0, currPoint.x(), currPoint.y()));
                    bestRouteGraph.addVertex(idCount + j, new ArrayList<>());
                    bestRouteGraph.addVertexInfo(idCount + j, new Vertex(idCount + j, 0, currPoint.x(), currPoint.y()));
                    IDCandidatePointMap.put(idCount + j, currPoint);
                    counter = j;
                }
                prevLevelStartID = idCount;
                idCount = idCount + counter + 1;
            } else if (i == candidatePoints.size()) {
                for (int k = prevLevelStartID; k < idCount; k++) {
                    bestRouteGraph.addAdjacency(k, new Vertex(idCount, 0));
                }
                bestRouteGraph.addVertex(idCount, new ArrayList<>());
                bestRouteGraph.addVertexInfo(idCount, new Vertex(idCount, 0));
                IDCandidatePointMap.put(idCount, new PointWithSegment(0, 0));
            } else {
                for (int j = 0; j < candidatePoints.get(i)._2().size(); j++) {
                    PointWithSegment currCandidatePoint = candidatePoints.get(i)._2().get(j);
                    PointWithSegment ancestorCandidatePoint;
                    for (int k = prevLevelStartID; k < idCount; k++) {
                        ancestorCandidatePoint = IDCandidatePointMap.get(k);
                        // use the score(pi,e) as the distance
//                        double distance = roadMapDistanceCal(ancestorCandidatePoint, prevPoint);
                        // the weight should be the reverse of score.
                        double distance = -scoreCalculation(currCandidatePoint.toPoint(), ancestorCandidatePoint.toPoint(), rawTraj.get(i), rawTraj.get(i - 1), currCandidatePoint.getAdjacentSegments().get(0));
                        // in case negative score
                        if (distance < 0) {
                            distance = 0;
                        }
                        bestRouteGraph.addAdjacency(k, new Vertex(idCount + j, distance));
                    }
                    bestRouteGraph.addVertex(idCount + j, new ArrayList<>());
                    bestRouteGraph.addVertexInfo(idCount + j, new Vertex(idCount + j, 0, currCandidatePoint.x(), currCandidatePoint.y()));
                    IDCandidatePointMap.put(idCount + j, currCandidatePoint);
                    counter = j;
                }
                if (candidatePoints.get(i)._2().size() != 0) {
                    prevLevelStartID = idCount;
                    idCount = idCount + counter + 1;
                }
            }
        }
        List<Integer> finalPathID = bestRouteGraph.getShortestPath(0, prevLevelStartID);

        // remove the final point and reverse the list
        RoadWay finalRoute = new RoadWay();
        Point prevPoint = new Point();
        Point startPoint;
        Point endPoint;
        // finalPathID.size = trajectory.size + 1, finalPathID[0] = last virtual point
        for (int i = finalPathID.size() - 1; i > 0; i--) {
            if (i == finalPathID.size() - 1) {
                // the first segment start from the last representative point
                Segment currMatchedSegment = IDCandidatePointMap.get(finalPathID.get(i)).getAdjacentSegments().get(0);
                String roadWayID = segmentRoadWayMap.get(currMatchedSegment.x1() + "_" + currMatchedSegment.y1() + "," + currMatchedSegment.x2() + "_" + currMatchedSegment.y2());
                startPoint = IDCandidatePointMap.get(finalPathID.get(i)).getPoint();
                finalRoute.addNode(new RoadNode(roadWayID, startPoint.x(), startPoint.y()));
                // if the representative point is not the endpoint, then add the end point to the final route
                endPoint = currMatchedSegment.getCoordinates().get(1);
                if (!IDCandidatePointMap.get(finalPathID.get(i)).equals2D(endPoint)) {
                    finalRoute.addNode(new RoadNode(roadWayID, endPoint.x(), endPoint.y()));
                }
                prevPoint = endPoint;
//                List<Point> curEndPoints = IDCandidatePointMap.get(finalPathID.get(i)).getAdjacentSegments().get(0).getCoordinates();
//                Point nextEndPoints = IDCandidatePointMap.get(finalPathID.get(i - 1)).getAdjacentSegments().get(0).getCoordinates().get(0);
//                // check which end point is closer to the rest points
//                double distance0 = shortestPathFile.getShortestDistance(curEndPoints.get(0).x() + "_" + curEndPoints.get(0).y(), nextEndPoints.x() + "_" + nextEndPoints.y());
//                double distance1 = shortestPathFile.getShortestDistance(curEndPoints.get(1).x() + "_" + curEndPoints.get(1).y(), nextEndPoints.x() + "_" + nextEndPoints.y());
//                RoadWay currRoadWay;
//                if (distance0 < distance1) {
//                    currRoadWay = shortestPathFile.getShortestPath(curEndPoints.get(0).x() + "_" + curEndPoints.get(0).y(), nextEndPoints.x() + "_" + nextEndPoints.y());
//                } else {
//                    currRoadWay = shortestPathFile.getShortestPath(curEndPoints.get(1).x() + "_" + curEndPoints.get(1).y(), nextEndPoints.x() + "_" + nextEndPoints.y());
//                }
//                finalRoute.addNodes(currRoadWay.getNodes());
//                Point lastPoint = finalRoute.getNode(finalRoute.getNodes().size() - 1).toPoint();
//                // check whether the other end point of the destination segment is included in the shortest path as well, if so, remove the more remote point
//                Point secondLastPoint = finalRoute.getNode(finalRoute.getNodes().size() - 2).toPoint();
//                if (secondLastPoint == IDCandidatePointMap.get(finalPathID.get(i - 1)).getAdjacentSegments().get(0).getCoordinates().get(1)) {
//                    finalRoute.getNodes().remove(finalRoute.getNodes().size() - 1);
//                    finalRoute.getNodes().remove(finalRoute.getNodes().size() - 1);
//                    prevPoint = secondLastPoint;
//                } else {
//                    finalRoute.getNodes().remove(finalRoute.getNodes().size() - 1);
//                    prevPoint = lastPoint;
//                }
            } else if (i == 1) {
                // the last segment before the virtual end point
                Segment currMatchedSegment = IDCandidatePointMap.get(finalPathID.get(i)).getAdjacentSegments().get(0);
                String roadWayID = segmentRoadWayMap.get(currMatchedSegment.x1() + "_" + currMatchedSegment.y1() + "," + currMatchedSegment.x2() + "_" + currMatchedSegment.y2());
                startPoint = currMatchedSegment.getCoordinates().get(0);
                if (!startPoint.equals2D(prevPoint)) {
                    finalRoute.addNode(new RoadNode(roadWayID, startPoint.x(), startPoint.y()));
                }

                if (IDCandidatePointMap.get(finalPathID.get(i)).equals2D(prevPoint)) {
                    System.out.println("two consecutive points are matched to the same point.");
                } else if (!IDCandidatePointMap.get(finalPathID.get(i)).equals2D(startPoint)) {
                    finalRoute.addNode(new RoadNode(roadWayID, IDCandidatePointMap.get(finalPathID.get(i)).x(), IDCandidatePointMap.get(finalPathID.get(i)).y()));
                }

//                List<Point> nextEndPoints = IDCandidatePointMap.get(finalPathID.get(i)).getAdjacentSegments().get(0).getCoordinates();
//                double distance0 = shortestPathFile.getShortestDistance(prevPoint.x() + "_" + prevPoint.y(), nextEndPoints.get(0).x() + "_" + nextEndPoints.get(0).y());
//                double distance1 = shortestPathFile.getShortestDistance(prevPoint.x() + "_" + prevPoint.y(), nextEndPoints.get(1).x() + "_" + nextEndPoints.get(1).y());
//                RoadWay currRoadWay;
//                if (distance0 < distance1) {
//                    currRoadWay = shortestPathFile.getShortestPath(prevPoint.x() + "_" + prevPoint.y(), nextEndPoints.get(0).x() + "_" + nextEndPoints.get(0).y());
//                } else {
//                    currRoadWay = shortestPathFile.getShortestPath(prevPoint.x() + "_" + prevPoint.y(), nextEndPoints.get(1).x() + "_" + nextEndPoints.get(1).y());
//                }
//                finalRoute.addNodes(currRoadWay.getNodes());
//                finalRoute.addNode(new RoadNode("", IDCandidatePointMap.get(finalPathID.get(i - 1)).x(), IDCandidatePointMap.get(finalPathID.get(i - 1)).y()));
//
            } else {
                // points between first and last representative points
                Segment currMatchedSegment = IDCandidatePointMap.get(finalPathID.get(i)).getAdjacentSegments().get(0);
                String roadWayID = segmentRoadWayMap.get(currMatchedSegment.x1() + "_" + currMatchedSegment.y1() + "," + currMatchedSegment.x2() + "_" + currMatchedSegment.y2());
                startPoint = currMatchedSegment.getCoordinates().get(0);
                Point matchedPoint = IDCandidatePointMap.get(finalPathID.get(i)).toPoint();
                endPoint = currMatchedSegment.getCoordinates().get(1);
                if (!startPoint.equals2D(prevPoint)) {
                    finalRoute.addNode(new RoadNode(roadWayID, startPoint.x(), startPoint.y()));
                }
                if (!matchedPoint.equals2D(startPoint)) {
                    if (!matchedPoint.equals2D(endPoint)) {
                        finalRoute.addNode(new RoadNode(roadWayID, matchedPoint.x(), matchedPoint.y()));
                    } else {
                        finalRoute.addNode(new RoadNode(roadWayID, endPoint.x(), endPoint.y()));
                    }
                }
                // add end point to the point list
                finalRoute.addNode(new RoadNode(roadWayID, endPoint.x(), endPoint.y()));
                prevPoint = endPoint;

//                List<Point> currEndPoints = IDCandidatePointMap.get(finalPathID.get(i)).getAdjacentSegments().get(0).getCoordinates();
//                if (!currEndPoints.get(0).equals2D(prevPoint) && currEndPoints.get(1).equals2D(prevPoint)) {
//                    System.out.println("Unmatched current end point:" + prevPoint.x() + "_" + prevPoint.y() + "," + currEndPoints.get(0).x() + "_" + currEndPoints.get(0).y() + "," + currEndPoints.get(1).x() + "_" + currEndPoints.get(1).y());
//                    gridDisplay.setSelectPoint(prevPoint);
//                    List<PointWithSegment> pointList = new ArrayList<>();
//                    pointList.add(new PointWithSegment(currEndPoints.get(0).x(), currEndPoints.get(0).y()));
//                    pointList.add(new PointWithSegment(currEndPoints.get(1).x(), currEndPoints.get(1).y()));
//                    gridDisplay.setCandidatePoints(pointList);
//                    Viewer viewer = gridDisplay.generateGraph().display(false);
//                    View view = viewer.getDefaultView();
//                    view.getCamera().setViewCenter(prevPoint.x(), prevPoint.y(), 0);
//                    view.getCamera().setViewPercent(0.15);
//                }
//                Point nextEndPoint = IDCandidatePointMap.get(finalPathID.get(i - 1)).getAdjacentSegments().get(0).getCoordinates().get(0);
//
//                RoadWay currRoadWay = shortestPathFile.getShortestPath(prevPoint.x() + "_" + prevPoint.y(), nextEndPoint.x() + "_" + nextEndPoint.y());
//                finalRoute.addNodes(currRoadWay.getNodes());
//                Point lastPoint = finalRoute.getNode(finalRoute.getNodes().size() - 1).toPoint();
//                // check whether the other end point of the destination segment is included in the shortest path as well, if so, remove the more remote point
//                Point secondLastPoint = finalRoute.getNode(finalRoute.getNodes().size() - 2).toPoint();
//                if (secondLastPoint == IDCandidatePointMap.get(finalPathID.get(i - 1)).getAdjacentSegments().get(0).getCoordinates().get(1)) {
//                    finalRoute.getNodes().remove(finalRoute.getNodes().size() - 1);
//                    finalRoute.getNodes().remove(finalRoute.getNodes().size() - 1);
//                    prevPoint = secondLastPoint;
//                } else {
//                    finalRoute.getNodes().remove(finalRoute.getNodes().size() - 1);
//                    prevPoint = lastPoint;
//                }
            }
        }
        return finalRoute;
    }

//    // calculate the distance between to points on the road map
//    private double roadMapDistanceCal(PointWithSegment ancestorPoint, PointWithSegment currentPoint) {
//
//        List<Point> ancestorEndPoints = ancestorPoint.getAdjacentSegments().get(0).getCoordinates();
//        List<Point> currentEndPoints = currentPoint.getAdjacentSegments().get(0).getCoordinates();
//        String ancestorID0 = ancestorEndPoints.get(0).x() + "_" + ancestorEndPoints.get(0).y();
//        String ancestorID1 = ancestorEndPoints.get(1).x() + "_" + ancestorEndPoints.get(1).y();
//        String currentPointID0 = currentEndPoints.get(0).x() + "_" + currentEndPoints.get(0).y();
//        String currentPointID1 = currentEndPoints.get(1).x() + "_" + currentEndPoints.get(1).y();
//        boolean ancestorChanged = false;
//        boolean currentChanged = false;
//        double minDistance = shortestPathFile.getShortestDistance(ancestorID0, currentPointID0);
//        if (minDistance == Double.POSITIVE_INFINITY) {
//            System.out.println("Error infinite distance");
//            List<PointWithSegment> pointList = new ArrayList<>();
//            pointList.add(ancestorPoint);
//            pointList.add(currentPoint);
//            gridDisplay.setCandidatePoints(pointList);
//            Viewer viewer = gridDisplay.generateGraph().display(false);
//            View view = viewer.getDefaultView();
//            view.getCamera().setViewCenter(ancestorPoint.x(), ancestorPoint.y(), 0);
//            view.getCamera().setViewPercent(0.15);
//            return minDistance;
//        } else {
//            double actualDistance = 0;
//            if (shortestPathFile.getShortestDistance(ancestorID1, currentPointID0) < minDistance) {
//                ancestorChanged = true;
//                currentChanged = false;
//                minDistance = shortestPathFile.getShortestDistance(ancestorID1, currentPointID0);
//            }
//            if (shortestPathFile.getShortestDistance(ancestorID0, currentPointID1) < minDistance) {
//                ancestorChanged = false;
//                currentChanged = true;
//                minDistance = shortestPathFile.getShortestDistance(ancestorID0, currentPointID1);
//            }
//            if (shortestPathFile.getShortestDistance(ancestorID1, currentPointID1) < minDistance) {
//                ancestorChanged = true;
//                currentChanged = true;
//                minDistance = shortestPathFile.getShortestDistance(ancestorID1, currentPointID1);
//            }
//            actualDistance = minDistance;
//            if (ancestorChanged) {
//                actualDistance += distanceFunction.distance(ancestorPoint.toPoint(), ancestorEndPoints.get(1));
//            } else {
//                actualDistance += distanceFunction.distance(ancestorPoint.toPoint(), ancestorEndPoints.get(0));
//            }
//            if (currentChanged) {
//                actualDistance += distanceFunction.distance(currentPoint.toPoint(), currentEndPoints.get(1));
//            } else {
//                actualDistance += distanceFunction.distance(currentPoint.toPoint(), currentEndPoints.get(0));
//            }
//            return actualDistance;
//        }
//    }

    // test whether the knn search works fine
    public void knnSearchTest(Trajectory trajectory, RoadNetworkGraph roadNetworkGraph) {

        // generate candidate segment set for every point in trajectory
//        HashMap<String, List<Segment>> candidateSegmentList = new HashMap<>();
        for (int i = 0; i < trajectory.getCoordinates().size(); i++) {
            Point selectPoint = new Point(trajectory.get(i).x(), trajectory.get(i).y());
            List<PointWithSegment> candidatePointList = new ArrayList<>();
            List<XYObject<PointWithSegment>> candidateSet = this.grid.kNearestNeighborsSearch(selectPoint.x(), selectPoint.y(), K, this.distanceFunction);
            if (candidateSet.size() != K) {
                System.out.println("SEVERE ERROR, size is:" + candidateSet.size());
            }
            for (XYObject<PointWithSegment> p : candidateSet) {
                candidatePointList.add(p.getSpatialObject());
            }
            gridDisplay.setGroundTruthGraph(roadNetworkGraph);
            gridDisplay.setSelectPoint(selectPoint);
            gridDisplay.setCandidatePoints(candidatePointList);
            Viewer viewer = gridDisplay.generateGraph().display(false);
            View view = viewer.getDefaultView();
            view.getCamera().setViewCenter(selectPoint.x(), selectPoint.y(), 0);
            view.getCamera().setViewPercent(0.15);
        }
    }

    private void roadGraphGen(RoadNetworkGraph inputMap) {

        // create one graph node per road network node.
        int idCount = 0;
        for (RoadNode node : inputMap.getNodes()) {
            navigateGraph.addVertex(idCount, new ArrayList<>());
            if (!vertexIDMap.containsKey(node.getId())) {
                vertexIDMap.put(node.getId(), idCount);
                idCount++;
            } else System.out.println("Duplicated roadNode:" + node.getId());
        }
        // add vertex distance for every edge in the road ways
        for (RoadWay way : inputMap.getWays()) {
            for (int i = 0; i < way.size() - 1; i++) {
                double distance = distanceFunction.distance(way.getNode(i).toPoint(), way.getNode(i + 1).toPoint());
                if (vertexIDMap.containsKey(way.getNode(i).getId()) && vertexIDMap.containsKey(way.getNode(i + 1).getId())) {
                    int endPointA = vertexIDMap.get(way.getNode(i).getId());
                    int endPointB = vertexIDMap.get(way.getNode(i + 1).getId());
                    navigateGraph.addAdjacency(endPointA, new Vertex(endPointB, distance));
                } else System.out.println("RoadNode doesn't exist");
            }
        }
        System.out.println("Navigate map created");
    }

}
