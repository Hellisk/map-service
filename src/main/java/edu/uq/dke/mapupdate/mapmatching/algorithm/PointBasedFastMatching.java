package edu.uq.dke.mapupdate.mapmatching.algorithm;

import edu.uq.dke.mapupdate.io.AllPairsShortestPathFile;
import edu.uq.dke.mapupdate.mapmatching.io.PointWithSegment;
import edu.uq.dke.mapupdate.visualisation.GraphStreamDisplay;
import org.graphstream.ui.view.View;
import org.graphstream.ui.view.Viewer;
import traminer.util.Pair;
import traminer.util.exceptions.MapMatchingException;
import traminer.util.graph.path.Graph;
import traminer.util.graph.path.Vertex;
import traminer.util.map.matching.MapMatchingMethod;
import traminer.util.map.matching.PointNodePair;
import traminer.util.map.roadnetwork.RoadNetworkGraph;
import traminer.util.map.roadnetwork.RoadNode;
import traminer.util.map.roadnetwork.RoadWay;
import traminer.util.spatial.distance.GPSDistanceFunction;
import traminer.util.spatial.distance.PointDistanceFunction;
import traminer.util.spatial.objects.Point;
import traminer.util.spatial.objects.Segment;
import traminer.util.spatial.objects.XYObject;
import traminer.util.spatial.objects.st.STPoint;
import traminer.util.spatial.structures.grid.Grid;
import traminer.util.trajectory.Trajectory;

import java.io.IOException;
import java.util.*;

/**
 * Created by Hellisk on 23/05/2017.
 */
public class PointBasedFastMatching implements MapMatchingMethod {
    private static final double DEFAULT_DISTANCE = 1000;
    private static final double MU_A = 10;
    private static final double MU_D = 0.17;
    private static final double C_A = 4;
    private static final double C_D = 1.4;
    private static final int K = 30;

    private Grid<PointWithSegment> grid;
    private final PointDistanceFunction distanceFunction;
    private boolean isUpdate = false;

    // for graph navigation
    private Graph navigateGraph = new Graph();
    private HashMap<String, Integer> vertexIDMap = new HashMap<>();

    private GraphStreamDisplay gridDisplay = new GraphStreamDisplay();

    private AllPairsShortestPathFile shortestPathFile;

    public PointBasedFastMatching(RoadNetworkGraph inputMap, PointDistanceFunction distFunc, int avgNodePerGrid, String inputShortestPathFile, boolean isUpdate) throws IOException {
        this.distanceFunction = distFunc;
        this.isUpdate = isUpdate;
        buildGridIndex(inputMap, avgNodePerGrid);
        gridDisplay.setGroundTruthGraph(inputMap);
        // read shortest path files
        shortestPathFile = new AllPairsShortestPathFile(inputMap);
        shortestPathFile.readShortestPathFiles(inputShortestPathFile);
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
                double distance = distanceFunction.distance(way.getNodes().get(i).toPoint(), way.getNodes().get(i + 1).toPoint());
                if (vertexIDMap.containsKey(way.getNodes().get(i).getId()) && vertexIDMap.containsKey(way.getNodes().get(i + 1).getId())) {
                    int endPointA = vertexIDMap.get(way.getNodes().get(i).getId());
                    int endPointB = vertexIDMap.get(way.getNodes().get(i + 1).getId());
                    navigateGraph.addAdjacency(endPointA, new Vertex(endPointB, distance));
                } else System.out.println("RoadNode doesn't exist");
            }
        }
        System.out.println("Navigate map created");
    }

    // test whether the knn search works fine
    public void knnSearchTest(Trajectory trajectory, RoadNetworkGraph roadNetworkGraph) {

        // generate candidate segment set for every point in trajectory
//        HashMap<String, List<Segment>> candidateSegmentList = new HashMap<>();
        Point selectPoint = new Point(trajectory.get(2).x(), trajectory.get(2).y());
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

    @Override
    public RoadWay doMatching(Trajectory trajectory, RoadNetworkGraph roadNetworkGraph) throws MapMatchingException {

        // generate candidate segment set for every point in trajectory
//        HashMap<String, List<Segment>> candidateSegmentList = new HashMap<>();
        Point prevPoint = new Point();
        List<Segment> prevCandidateSegments = new ArrayList<>();
        List<Pair<Point, List<PointWithSegment>>> candidatePointList = new ArrayList<>();
        List<Trajectory> unmatchedList = new ArrayList<>();
        for (Point p : trajectory.getCoordinates()) {
            List<XYObject<PointWithSegment>> candidateSet = this.grid.kNearestNeighborsSearch(p.x(), p.y(), K, this.distanceFunction);
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

            List<Segment> filteredSegment = candidateSegmentFilter(p, prevPoint, candidateSegments, prevCandidateSegments);

            System.out.println("The size of filtered candidate set:" + filteredSegment.size());
            if (filteredSegment.isEmpty()) {
                double maxScore = -Double.MAX_VALUE;
                List<Pair<Segment, Double>> edgeScore = new ArrayList<>();
                for (Segment x : candidateSegments) {
                    Pair<Segment, Double> currEdgeScore = calculateScore(p, prevPoint, x);
                    maxScore = maxScore >= currEdgeScore._2() ? maxScore : currEdgeScore._2();
                    edgeScore.add(currEdgeScore);
                }

                for (int i = 0; i < edgeScore.size(); i++) {
                    if (edgeScore.get(i)._2() < 0.8 * maxScore && maxScore >= 0) {
                        edgeScore.remove(i);
                        i--;
                    } else {
                        filteredSegment.add(edgeScore.get(i)._1());
                    }
                }
                if (filteredSegment.isEmpty()) {
                    System.out.println("maxScore = " + maxScore);
                }
            }

            List<PointWithSegment> representativePointList = new ArrayList<>(filteredSegment.size());
            for (Segment e : filteredSegment) {
                PointWithSegment representPoint = representativePointGen(p, e);

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

            candidatePointList.add(new Pair<>(p, representativePointList));
            // store the current point information
            prevPoint = p;
            prevCandidateSegments = filteredSegment;
        }

        RoadWay bestRoute = bestRouteGen(candidatePointList);
        return bestRoute;

    }

    @Override
    public List<PointNodePair> doMatching(Collection<STPoint> pointsList, Collection<RoadNode> nodesList) throws
            MapMatchingException {
        return null;
    }

    private void buildGridIndex(RoadNetworkGraph inputMap, int avgNodePerGrid) {

        // calculate the grid settings
        int cellNum = 0;    // total number of cells
        int rowNum = 0;     // number of rows and columns
        cellNum = inputMap.getNodes().size() / avgNodePerGrid;
        rowNum = (int) Math.ceil(Math.sqrt(cellNum));
        this.grid = new Grid<PointWithSegment>(rowNum, rowNum, inputMap.getMinLon(), inputMap.getMinLat(), inputMap.getMaxLon(), inputMap.getMaxLat());

        System.out.println("Total number of nodes in grid index:" + inputMap.getNodes().size());
        System.out.println("The grid contains " + rowNum + "rows and columns");

        // add all map nodes and edges into a hash map
        Map<String, List<Segment>> adjacentList = new HashMap<>();
        for (RoadNode p : inputMap.getNodes()) {
            if (!adjacentList.containsKey(p.lon() + "," + p.lat())) {
                adjacentList.put(p.lon() + "," + p.lat(), new ArrayList<>());
            } else System.err.println("duplicated RoadNode!");

        }

        for (RoadWay t : inputMap.getWays()) {
            for (Segment a : t.getEdges()) {
                for (Point b : a.getCoordinates()) {
                    if (adjacentList.containsKey(b.x() + "," + b.y())) {
                        adjacentList.get(b.x() + "," + b.y()).add(a);
                    }
                }
            }
        }


        int pointCount = 0;

        for (Map.Entry<String, List<Segment>> entry : adjacentList.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                String[] coordinate = entry.getKey().split(",");
                PointWithSegment newPoint = new PointWithSegment(Double.parseDouble(coordinate[0]), Double.parseDouble(coordinate[1]), entry.getValue());
                XYObject<PointWithSegment> point = new XYObject<PointWithSegment>(newPoint.x(), newPoint.y(), newPoint);
                this.grid.insert(point);
                pointCount++;
            }
        }


        System.out.println("Grid index build successfully, total number of points:" + pointCount);
    }

    private PointWithSegment representativePointGen(Point trajectoryPoint, Segment candidateSegment) {
        double k = ((candidateSegment.y2() - candidateSegment.y1()) * (trajectoryPoint.x() - candidateSegment.x1()) - (candidateSegment.x2() - candidateSegment.x1()) * (trajectoryPoint.y() - candidateSegment.y1())) / (Math.pow((candidateSegment.y2() - candidateSegment.y1()), 2) + (Math.pow(candidateSegment.x2() - candidateSegment.x1(), 2)));

        double representPx = trajectoryPoint.x() - k * (candidateSegment.y2() - candidateSegment.y1());
        double representPy = trajectoryPoint.y() + k * (candidateSegment.x2() - candidateSegment.x1());

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

    private Point representativePointGen2(Point trajectoryPoint, Segment candidateSegment) {
        double k = ((candidateSegment.y2() - candidateSegment.y1()) * (trajectoryPoint.x() - candidateSegment.x1()) - (candidateSegment.x2() - candidateSegment.x1()) * (trajectoryPoint.y() - candidateSegment.y1())) / (Math.pow((candidateSegment.y2() - candidateSegment.y1()), 2) + (Math.pow(candidateSegment.x2() - candidateSegment.x1(), 2)));

        double representPx = trajectoryPoint.x() - k * (candidateSegment.y2() - candidateSegment.y1());
        double representPy = trajectoryPoint.y() + k * (candidateSegment.x2() - candidateSegment.x1());

        return new Point(representPx, representPy);
    }

    private Pair<Segment, Double> calculateScore(Point p, Point prevPoint, Segment e) {
        // calculate the angle between p(i-1)p(i) and edge e
        double trajectoryEdgeX = p.x() - prevPoint.x();
        double trajectoryEdgeY = p.y() - prevPoint.y();
        double candidateSegmentX = e.x2() - e.x1();
        double candidateSegmentY = e.y2() - e.y1();
        // angle = atan2( a.x*b.y - a.y*b.x, a.x*b.x + a.y*b.y )
        double angle = Math.abs(Math.atan2(trajectoryEdgeX * candidateSegmentY - trajectoryEdgeY * candidateSegmentX, trajectoryEdgeX * trajectoryEdgeY + candidateSegmentX * candidateSegmentY));

        //calculate the distance between p and edge e
        GPSDistanceFunction dist = new GPSDistanceFunction();
        double pointToSegmentDistance = dist.pointToSegmentDistance(p, e);

        // final score(p(i),e) calculation
        double score = MU_A * Math.pow(Math.cos(angle), C_A) - MU_D * Math.pow(pointToSegmentDistance, C_D);
        return new Pair<Segment, Double>(e, score);
    }

    /**
     * filter out the candidate roads that are less likely to be matched according to three principles:
     * distance between edge and trajectory point is more than DEFAULT_DISTANCE
     * not appeared in the candidate edge set of ancester of current point
     * not share any endpoint of the candidate edge of ancester of current point
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

        // used for following angle check
        double trajectoryEdgeX = currPoint.x() - prevPoint.x();
        double trajectoryEdgeY = currPoint.y() - prevPoint.y();

//        System.out.println("Current angle is " + angleTrajectoryEdge);
        for (int i = 0; i < filteredSegment.size(); i++) {
            // check if the edge is too far from the point currPoint
            double distance = dist.pointToSegmentDistance(currPoint, filteredSegment.get(i));
//            System.out.println("Distance is:" + dist.pointToSegmentDistance(currPoint, filteredSegment.get(i)));
            if (dist.pointToSegmentDistance(currPoint, filteredSegment.get(i)) <= DEFAULT_DISTANCE) {
                boolean connected = false;
                // check if any of the previous candidate edge connect the current one
                for (Segment e : prevSegmentList) {

                    // TODO report List<Point>.contains doesn't work
                    if (e.getCoordinates().get(0).equals2D(filteredSegment.get(i).getCoordinates().get(0)) || e.getCoordinates().get(1).equals2D(filteredSegment.get(i).getCoordinates().get(0))
                            || e.getCoordinates().get(1).equals2D(filteredSegment.get(i).getCoordinates().get(0)) || e.getCoordinates().get(1).equals2D(filteredSegment.get(i).getCoordinates().get(1))) {
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
                double candidateSegmentX = filteredSegment.get(i).x2() - filteredSegment.get(i).x1();
                double candidateSegmentY = filteredSegment.get(i).y2() - filteredSegment.get(i).y1();
                // angle = atan2( a.x*b.y - a.y*b.x, a.x*b.x + a.y*b.y )
                double angle = Math.atan2(trajectoryEdgeX * candidateSegmentY - trajectoryEdgeY * candidateSegmentX, trajectoryEdgeX * trajectoryEdgeY + candidateSegmentX * candidateSegmentY);
//                System.out.println("angel =" + angle / Math.PI);
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

    private RoadWay bestRouteGen(List<Pair<Point, List<PointWithSegment>>> candidatePoints) {
        // create virtual graph to find shortest path
        Graph bestRouteGraph = new Graph();
        HashMap<Integer, PointWithSegment> IDCandidatePointMap = new HashMap<>();
        int idCount = 0; // the pointer for newly added vertex
        int prevLevelStartID = 0; // the pointer for the start of the last level
        int counter = 0;
//        GPSDistanceFunction dist = new GPSDistanceFunction();
        // i = trajectory point number, j = candidate point id
        for (int i = 0; i < candidatePoints.size() + 1; i++) {
            if (i == 0) {
                bestRouteGraph.addVertex(idCount, new ArrayList<>());
                bestRouteGraph.addVertexInfo(idCount, new Vertex(idCount, 0));
                IDCandidatePointMap.put(idCount, new PointWithSegment(0, 0));
                idCount++;
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
                bestRouteGraph.addVertexInfo(idCount, new Vertex(idCount, 0));
                bestRouteGraph.addVertex(idCount, new ArrayList<>());
                IDCandidatePointMap.put(idCount, new PointWithSegment(0, 0));
                prevLevelStartID = idCount;
                idCount++;
            } else {
                for (int j = 0; j < candidatePoints.get(i)._2().size(); j++) {
                    PointWithSegment currPoint = candidatePoints.get(i)._2().get(j);
                    PointWithSegment ancestorPoint;
                    for (int k = prevLevelStartID; k < idCount; k++) {
                        ancestorPoint = IDCandidatePointMap.get(k);
                        double distance = roadMapDistanceCal(ancestorPoint, currPoint);
                        bestRouteGraph.addAdjacency(k, new Vertex(idCount + j, distance));
                    }
                    bestRouteGraph.addVertexInfo(idCount + j, new Vertex(idCount + j, 0, currPoint.x(), currPoint.y()));
                    bestRouteGraph.addVertex(idCount + j, new ArrayList<Vertex>());
                    IDCandidatePointMap.put(idCount + j, currPoint);
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
        counter = 0;
        RoadWay finalRoute = new RoadWay();
        Point currPoint = new Point();
        for (int i = finalPathID.size() - 1; i > 1; i--) {
            if (i == finalPathID.size() - 1) {
                // the first segment start from the first representative point
                finalRoute.addNode(new RoadNode("", IDCandidatePointMap.get(finalPathID.get(i)).x(), IDCandidatePointMap.get(finalPathID.get(i)).y()));
                List<Point> curEndPoints = IDCandidatePointMap.get(finalPathID.get(i)).getAdjacentSegments().get(0).getCoordinates();
                Point nextEndPoints = IDCandidatePointMap.get(finalPathID.get(i - 1)).getAdjacentSegments().get(0).getCoordinates().get(0);
                // check which end point is closer to the rest points
                double distance0 = shortestPathFile.getShortestDistance(curEndPoints.get(0).x() + "_" + curEndPoints.get(0).y(), nextEndPoints.x() + "_" + nextEndPoints.y());
                double distance1 = shortestPathFile.getShortestDistance(curEndPoints.get(1).x() + "_" + curEndPoints.get(1).y(), nextEndPoints.x() + "_" + nextEndPoints.y());
                RoadWay currRoadWay;
                if (distance0 < distance1) {
                    currRoadWay = shortestPathFile.getShortestPath(curEndPoints.get(0).x() + "_" + curEndPoints.get(0).y(), nextEndPoints.x() + "_" + nextEndPoints.y());
                } else {
                    currRoadWay = shortestPathFile.getShortestPath(curEndPoints.get(1).x() + "_" + curEndPoints.get(1).y(), nextEndPoints.x() + "_" + nextEndPoints.y());
                }
                finalRoute.addNodes(currRoadWay.getNodes());
                Point lastPoint = finalRoute.getNodes().get(finalRoute.getNodes().size() - 1).toPoint();
                // check whether the other end point of the destination segment is included in the shortest path as well, if so, remove the more remote point
                Point secondLastPoint = finalRoute.getNodes().get(finalRoute.getNodes().size() - 2).toPoint();
                if (secondLastPoint == IDCandidatePointMap.get(finalPathID.get(i - 1)).getAdjacentSegments().get(0).getCoordinates().get(1)) {
                    finalRoute.getNodes().remove(finalRoute.getNodes().size() - 1);
                    finalRoute.getNodes().remove(finalRoute.getNodes().size() - 1);
                    currPoint = secondLastPoint;
                } else {
                    finalRoute.getNodes().remove(finalRoute.getNodes().size() - 1);
                    currPoint = lastPoint;
                }
            } else if (i == 2) {
                // the last segment end at the last representative point
                List<Point> nextEndPoints = IDCandidatePointMap.get(finalPathID.get(i - 1)).getAdjacentSegments().get(0).getCoordinates();
                double distance0 = shortestPathFile.getShortestDistance(currPoint.x() + "_" + currPoint.y(), nextEndPoints.get(0).x() + "_" + nextEndPoints.get(0).y());
                double distance1 = shortestPathFile.getShortestDistance(currPoint.x() + "_" + currPoint.y(), nextEndPoints.get(1).x() + "_" + nextEndPoints.get(1).y());
                RoadWay currRoadWay;
                if (distance0 < distance1) {
                    currRoadWay = shortestPathFile.getShortestPath(currPoint.x() + "_" + currPoint.y(), nextEndPoints.get(0).x() + "_" + nextEndPoints.get(0).y());
                } else {
                    currRoadWay = shortestPathFile.getShortestPath(currPoint.x() + "_" + currPoint.y(), nextEndPoints.get(1).x() + "_" + nextEndPoints.get(1).y());
                }
                finalRoute.addNodes(currRoadWay.getNodes());
                finalRoute.addNode(new RoadNode("", IDCandidatePointMap.get(finalPathID.get(i - 1)).x(), IDCandidatePointMap.get(finalPathID.get(i - 1)).y()));

            } else {
                List<Point> currEndPoints = IDCandidatePointMap.get(finalPathID.get(i)).getAdjacentSegments().get(0).getCoordinates();
                if (!currEndPoints.get(0).equals2D(currPoint) && currEndPoints.get(1).equals2D(currPoint)) {
                    System.out.println("Unmatched current end point:" + currPoint.x() + "_" + currPoint.y() + "," + currEndPoints.get(0).x() + "_" + currEndPoints.get(0).y() + "," + currEndPoints.get(1).x() + "_" + currEndPoints.get(1).y());
                    gridDisplay.setSelectPoint(currPoint);
                    List<PointWithSegment> pointList = new ArrayList<>();
                    pointList.add(new PointWithSegment(currEndPoints.get(0).x(), currEndPoints.get(0).y()));
                    pointList.add(new PointWithSegment(currEndPoints.get(1).x(), currEndPoints.get(1).y()));
                    gridDisplay.setCandidatePoints(pointList);
                    Viewer viewer = gridDisplay.generateGraph().display(false);
                    View view = viewer.getDefaultView();
                    view.getCamera().setViewCenter(currPoint.x(), currPoint.y(), 0);
                    view.getCamera().setViewPercent(0.15);
                }
                Point nextEndPoint = IDCandidatePointMap.get(finalPathID.get(i - 1)).getAdjacentSegments().get(0).getCoordinates().get(0);

                RoadWay currRoadWay = shortestPathFile.getShortestPath(currPoint.x() + "_" + currPoint.y(), nextEndPoint.x() + "_" + nextEndPoint.y());
                finalRoute.addNodes(currRoadWay.getNodes());
                Point lastPoint = finalRoute.getNodes().get(finalRoute.getNodes().size() - 1).toPoint();
                // check whether the other end point of the destination segment is included in the shortest path as well, if so, remove the more remote point
                Point secondLastPoint = finalRoute.getNodes().get(finalRoute.getNodes().size() - 2).toPoint();
                if (secondLastPoint == IDCandidatePointMap.get(finalPathID.get(i - 1)).getAdjacentSegments().get(0).getCoordinates().get(1)) {
                    finalRoute.getNodes().remove(finalRoute.getNodes().size() - 1);
                    finalRoute.getNodes().remove(finalRoute.getNodes().size() - 1);
                    currPoint = secondLastPoint;
                } else {
                    finalRoute.getNodes().remove(finalRoute.getNodes().size() - 1);
                    currPoint = lastPoint;
                }
            }
        }
        return finalRoute;
    }

    private double roadMapDistanceCal(PointWithSegment ancestorPoint, PointWithSegment currentPoint) {

        List<Point> ancestorEndPoints = ancestorPoint.getAdjacentSegments().get(0).getCoordinates();
        List<Point> currentEndPoints = currentPoint.getAdjacentSegments().get(0).getCoordinates();
        String ancestorID0 = ancestorEndPoints.get(0).x() + "_" + ancestorEndPoints.get(0).y();
        String ancestorID1 = ancestorEndPoints.get(1).x() + "_" + ancestorEndPoints.get(1).y();
        String currentPointID0 = currentEndPoints.get(0).x() + "_" + currentEndPoints.get(0).y();
        String currentPointID1 = currentEndPoints.get(1).x() + "_" + currentEndPoints.get(1).y();
        boolean ancestorChanged = false;
        boolean currentChanged = false;
        double minDistance = shortestPathFile.getShortestDistance(ancestorID0, currentPointID0);
        if (minDistance == Double.POSITIVE_INFINITY) {
            System.out.println("Error infinite distance");
            List<PointWithSegment> pointList = new ArrayList<>();
            pointList.add(ancestorPoint);
            pointList.add(currentPoint);
            gridDisplay.setCandidatePoints(pointList);
            Viewer viewer = gridDisplay.generateGraph().display(false);
            View view = viewer.getDefaultView();
            view.getCamera().setViewCenter(ancestorPoint.x(), ancestorPoint.y(), 0);
            view.getCamera().setViewPercent(0.15);
            return minDistance;
        } else {
            double actualDistance = 0;
            if (shortestPathFile.getShortestDistance(ancestorID1, currentPointID0) < minDistance) {
                ancestorChanged = true;
                currentChanged = false;
                minDistance = shortestPathFile.getShortestDistance(ancestorID1, currentPointID0);
            }
            if (shortestPathFile.getShortestDistance(ancestorID0, currentPointID1) < minDistance) {
                ancestorChanged = false;
                currentChanged = true;
                minDistance = shortestPathFile.getShortestDistance(ancestorID0, currentPointID1);
            }
            if (shortestPathFile.getShortestDistance(ancestorID1, currentPointID1) < minDistance) {
                ancestorChanged = true;
                currentChanged = true;
                minDistance = shortestPathFile.getShortestDistance(ancestorID1, currentPointID1);
            }
            actualDistance = minDistance;
            if (ancestorChanged) {
                actualDistance += distanceFunction.distance(ancestorPoint.toPoint(), ancestorEndPoints.get(1));
            } else {
                actualDistance += distanceFunction.distance(ancestorPoint.toPoint(), ancestorEndPoints.get(0));
            }
            if (currentChanged) {
                actualDistance += distanceFunction.distance(currentPoint.toPoint(), currentEndPoints.get(1));
            } else {
                actualDistance += distanceFunction.distance(currentPoint.toPoint(), currentEndPoints.get(0));
            }
            return actualDistance;
        }
    }

}
