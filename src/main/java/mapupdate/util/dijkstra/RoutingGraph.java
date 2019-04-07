package mapupdate.util.dijkstra;

import mapupdate.util.function.GreatCircleDistanceFunction;
import mapupdate.util.function.PointDistanceFunction;
import mapupdate.util.object.datastructure.Pair;
import mapupdate.util.object.datastructure.PointMatch;
import mapupdate.util.object.roadnetwork.MapInterface;
import mapupdate.util.object.roadnetwork.RoadNetworkGraph;
import mapupdate.util.object.roadnetwork.RoadNode;
import mapupdate.util.object.roadnetwork.RoadWay;

import java.util.*;

import static mapupdate.Main.BACKWARDS_FACTOR;
import static mapupdate.Main.CANDIDATE_RANGE;
import static mapupdate.Main.LOGGER;

public class RoutingGraph implements MapInterface {
    private HashMap<Integer, String> index2RoadIDAndSN = new HashMap<>();    // the mapping between the mini edge index and its corresponding
    // roadway id and serial number, format: (index,roadID,serialNum)
    private HashMap<String, Integer> endPointLoc2EdgeIndex = new HashMap<>();  // find the mini edge index using the coordinates and road
    // way id, format: (x1_x2,y1_y2,id)
    private HashMap<Integer, Pair<Integer, Integer>> edgeIndex2EndpointsIndex = new HashMap<>();  // find the end point indices given the mini edge index
    private HashMap<Pair<Integer, Integer>, Integer> endPointsIndex2EdgeIndex = new HashMap<>();  // find the mini edge index given the end point indices
    private HashMap<Integer, Pair<Integer, Double>> edgeIndex2LeftMostEdgeIndexDist = new HashMap<>();  // find the first routing edge
    // and its distance to the current edge
    private RoutingVertex[] vertices;
    private RoutingEdge[] routingEdges;
    private PointDistanceFunction distFunc = new GreatCircleDistanceFunction();
    private HashSet<Integer> newNodeSet = new HashSet<>();  // useful only when isPartial = true;
    private HashSet<Integer> newEdgeSet = new HashSet<>();  // useful only when isPartial = true;
    private HashMap<String, List<Integer>> roadID2NewNodeList = new HashMap<>();  // for each new road, the generated node ID list.
    private HashMap<String, List<Integer>> roadID2NewEdgeList = new HashMap<>();  // for each new road, the generated edge ID list.

    /**
     * Create routing graph for map-matching.
     *
     * @param roadNetwork The map used to build routing graph.
     * @param isPartial   Is new roads to be added together into the shortest path.
     */
    public RoutingGraph(RoadNetworkGraph roadNetwork, boolean isPartial) {
        // insert the road node into node list
        HashMap<String, Integer> nodeID2Index = new HashMap<>();
        HashSet<Integer> outGoingNodeSet = new HashSet<>();
        // the size of the vertex list and the current index of the new vertex
        int noOfVertices = 0;
        for (RoadNode node : roadNetwork.getNodes()) {
            if (nodeID2Index.containsKey(node.getID()))
                LOGGER.severe("ERROR! Road node ID already exists: " + node.getID());
            nodeID2Index.put(node.getID(), noOfVertices);
            noOfVertices++;
        }

        List<RoutingEdge> routingEdgeList = new ArrayList<>();
        int noOfEdges = 0;
        for (RoadWay way : roadNetwork.getWays()) {
            // insert all the mini vertices to the node list
            if (isPartial && way.isNewRoad()) {
                roadID2NewNodeList.put(way.getID(), new ArrayList<>());
                roadID2NewEdgeList.put(way.getID(), new ArrayList<>());
            }
            for (int i = 1; i < way.getNodes().size() - 1; i++) {
                // insert all mini vertices into the nodeID index
                RoadNode startNode = way.getNode(i);
                if (nodeID2Index.containsKey(startNode.getID()))
                    LOGGER.severe("ERROR! Road node ID for mini node already exists: " + startNode.getID());
                nodeID2Index.put(startNode.getID(), noOfVertices);
                if (isPartial && way.isNewRoad()) {
                    newNodeSet.add(noOfVertices);
                    roadID2NewNodeList.get(way.getID()).add(noOfVertices);
                }
                noOfVertices++;
            }

            // insert mini routingEdges to the edge list
            double currDist = 0;
            int startEdgeIndex = 0;
            for (int i = 0; i < way.getNodes().size() - 1; i++) {
                RoadNode startNode = way.getNode(i);
                RoadNode endNode = way.getNode(i + 1);
                index2RoadIDAndSN.put(noOfEdges, way.getID() + "," + (i + 1));
                endPointLoc2EdgeIndex.put(startNode.lon() + "_" + startNode.lat() + "," + endNode.lon() + "_" + endNode.lat() + "," + way.getID(), noOfEdges);
                int startID = nodeID2Index.get(startNode.getID());
                int endID = nodeID2Index.get(endNode.getID());
                Pair<Integer, Integer> endPointIndices = new Pair<>(startID, endID);
                edgeIndex2EndpointsIndex.put(noOfEdges, endPointIndices);
                endPointsIndex2EdgeIndex.put(endPointIndices, noOfEdges);
                if (i == 0) {
                    startEdgeIndex = noOfEdges;
                    edgeIndex2LeftMostEdgeIndexDist.put(noOfEdges, new Pair<>(startEdgeIndex, currDist));
                } else {
                    edgeIndex2LeftMostEdgeIndexDist.put(noOfEdges, new Pair<>(startEdgeIndex, currDist));
                }
                currDist += distFunc.distance(startNode.toPoint(), endNode.toPoint());
                RoutingEdge currRoutingEdge = new RoutingEdge(startID, endID, distFunc.distance(startNode.toPoint(), endNode.toPoint()));
                currRoutingEdge.setIndex(noOfEdges);
                routingEdgeList.add(currRoutingEdge);
                if (isPartial && way.isNewRoad()) {
                    newEdgeSet.add(noOfEdges);
                    roadID2NewEdgeList.get(way.getID()).add(noOfEdges);
                }
                noOfEdges++;
            }
        }
        this.routingEdges = routingEdgeList.toArray(new RoutingEdge[0]);

        // create all vertices ready to be updated with the routingEdges
        this.vertices = new RoutingVertex[noOfVertices];
        for (int n = 0; n < noOfVertices; n++) {
            this.vertices[n] = new RoutingVertex();
            this.vertices[n].setIndex(n);
        }
        // add all the routingEdges to the vertices, each edge added to only from vertices
        for (int i = 0; i < noOfEdges; i++) {
            if (!isPartial || !newEdgeSet.contains(i))
                this.vertices[routingEdges[i].getFromNodeIndex()].getOutGoingRoutingEdges().add(routingEdges[i]);
        }

        // check the completeness of the graph
        for (RoutingVertex n : this.vertices) {
            if (n.getOutGoingRoutingEdges().size() != 0) {
                for (RoutingEdge e : n.getOutGoingRoutingEdges()) {
                    outGoingNodeSet.add(e.getToNodeIndex());
                }
            }
        }
        for (int i = 0; i < vertices.length; i++) {
            if (this.vertices[i].getOutGoingRoutingEdges().size() == 0 && !outGoingNodeSet.contains(i) && !newNodeSet.contains(i))
                LOGGER.severe("ERROR! Isolated node detected: No. " + i);
        }
        LOGGER.info("Shortest path graph generated. Total vertices:" + noOfVertices + ", total edges:" + noOfEdges);
    }

    @SuppressWarnings("unchecked")
    // Calculate all shortest distance from given node
    public List<Pair<Double, List<String>>> calculateShortestDistanceList(PointMatch source, List<PointMatch> pointList, double maxDistance) {
        double[] distance = new double[pointList.size()];   // the distance to every destination
        List<String>[] path = new ArrayList[pointList.size()];     // the path to every destination
        HashMap<Integer, Integer> parent = new HashMap<>();
        HashMap<Integer, Double> vertexDistFromSource = new HashMap<>();
        HashSet<Integer> vertexVisited = new HashSet<>();
        List<Pair<Double, List<String>>> result;

        Arrays.fill(distance, Double.POSITIVE_INFINITY);
        for (int i = 0; i < path.length; i++)
            path[i] = new ArrayList<>();
        // the variables have been initialized during the last calculation. Start the process right away
        HashMap<Integer, Set<Pair<Integer, Double>>> nodeIndex2DestPointSet = new HashMap<>();        // (vertex index in graph, point index
        // in pointList)

        // if source point doesn't exist, return infinity to all distances
        String sourceLocID = source.getMatchedSegment().x1() + "_" + source.getMatchedSegment().y1() + "," + source.getMatchedSegment()
                .x2() + "_" + source.getMatchedSegment().y2() + "," + source.getRoadID();
        if (!this.endPointLoc2EdgeIndex.containsKey(sourceLocID)) {
            LOGGER.severe("ERROR! Shortest distance calculation failed: Source node is not found.");
            result = new ArrayList<>(resultOutput(distance, path));
            return result;
        }

        // the start node of the current Dijkstra rotation
        int startEdgeIndex = endPointLoc2EdgeIndex.get(sourceLocID);
        String startRoadID = index2RoadIDAndSN.get(startEdgeIndex).split(",")[0];
        int startRoadSN = Integer.parseInt(index2RoadIDAndSN.get(startEdgeIndex).split(",")[1]);
        int startNodeIndex = edgeIndex2EndpointsIndex.get(startEdgeIndex)._2();
        double sourceDistance = this.distFunc.distance(source.getMatchPoint(), source.getMatchedSegment().p2());

        // attach all destination points to the graph
        int destPointCount = pointList.size();
        for (int i = 0; i < pointList.size(); i++) {
            String destLocID = pointList.get(i).getMatchedSegment().x1() + "_" + pointList.get(i).getMatchedSegment().y1() + "," +
                    pointList.get(i).getMatchedSegment().x2() + "_" + pointList.get(i).getMatchedSegment().y2() + "," + pointList.get(i)
                    .getRoadID();
            if (!endPointLoc2EdgeIndex.containsKey(destLocID)) {
                LOGGER.severe("ERROR! Destination node is not found.");
                destPointCount--;
//            } else if (pointList.get(i).getMatchPoint().equals2D(pointList.get(i).getMatchedSegment().p1())) {
//                destPointCount--;
            } else {
                int destEdgeIndex = endPointLoc2EdgeIndex.get(destLocID);
                String destRoadID = index2RoadIDAndSN.get(destEdgeIndex).split(",")[0];
                if (destRoadID.equals(startRoadID)) {   // two segments refer to the same road
                    if (distFunc.distance(source.getMatchPoint(), pointList.get(i).getMatchPoint()) == 0) { // located in the same road
                        // and do not move
                        distance[i] = 0;
                        destPointCount--;
                    } else if (destEdgeIndex == startEdgeIndex && distFunc.distance(source.getMatchPoint(), source.getMatchedSegment().p2()) > distFunc.distance(pointList.get(i)
                            .getMatchPoint(), pointList.get(i).getMatchedSegment().p2())) {   // two segments refer to the same mini edge
                        // and they are in the right sequence
                        distance[i] = distFunc.distance(source.getMatchPoint(), pointList.get(i).getMatchPoint());
                        path[i].add(pointList.get(i).getRoadID());
                        destPointCount--;
                    } else if (destEdgeIndex == startEdgeIndex && distFunc.distance(source.getMatchPoint(),
                            pointList.get(i).getMatchPoint()) < CANDIDATE_RANGE * BACKWARDS_FACTOR) {  // vehicle may stop on the road and the sampling
                        // point may be located backward
                        distance[i] = 1.1 * distFunc.distance(source.getMatchPoint(), pointList.get(i).getMatchPoint());
                        path[i].add(pointList.get(i).getRoadID());
                        destPointCount--;
                    } else {    // they are in different mini edges or too distant inside one mini edge
                        int destRoadSN = Integer.parseInt(index2RoadIDAndSN.get(destEdgeIndex).split(",")[1]);
                        int destNodeIndex = edgeIndex2EndpointsIndex.get(destEdgeIndex)._1();
                        if (startRoadSN < destRoadSN) { // the start node is located at the upstream of the destination node within the same
                            // road
                            distance[i] += sourceDistance;
                            distance[i] += distanceWithinEdge(startNodeIndex, startEdgeIndex, destNodeIndex);
                            distance[i] += distFunc.distance(pointList.get(i).getMatchedSegment().p1(), pointList.get(i).getMatchPoint());
                            path[i].add(pointList.get(i).getRoadID());
                            destPointCount--;
                        } else if (destRoadSN < startRoadSN) {
                            int startIndex = edgeIndex2EndpointsIndex.get(destEdgeIndex)._2();
                            int destIndex = edgeIndex2EndpointsIndex.get(startEdgeIndex)._1();
                            double dist = distFunc.distance(pointList.get(i).getMatchPoint(), pointList.get(i).getMatchedSegment().p2());
                            dist += distFunc.distance(source.getMatchedSegment().p1(), source.getMatchPoint());
                            dist += distanceWithinEdge(startIndex, destEdgeIndex, destIndex);
                            if (dist < CANDIDATE_RANGE * BACKWARDS_FACTOR) {   // although in different mini edge, they are very close so
                                // that it can be just noise
                                distance[i] = 1.2 * dist;
                                path[i].add(pointList.get(i).getRoadID());
                                destPointCount--;
                            } else {
                                insertDestPoint(nodeIndex2DestPointSet, i, destEdgeIndex);
                            }
                        } else {    // inside the same mini edge but too far away
                            insertDestPoint(nodeIndex2DestPointSet, i, destEdgeIndex);
                        }
                    }
                } else {
                    insertDestPoint(nodeIndex2DestPointSet, i, destEdgeIndex);
                }
            }
        }

        // the rest of the destinations are on different roads, now set the end of the current road as start vertex
        if (destPointCount > 0) {

            boolean isTheSameRoad = true;   // find the end of the current road as the start of the new sequence
            while (isTheSameRoad) {
                List<RoutingEdge> outGoingRoutingEdgeList = this.vertices[startNodeIndex].getOutGoingRoutingEdges();
                if (outGoingRoutingEdgeList.size() == 0)
                    isTheSameRoad = false;
                for (RoutingEdge routingEdge : outGoingRoutingEdgeList) {
                    if (routingEdge.getIndex() != startEdgeIndex) {    // the current routingEdge is not the reverse routingEdge
                        if (index2RoadIDAndSN.get(routingEdge.getIndex()).split(",")[0].equals(startRoadID)) {  // still on the same road
                            sourceDistance += routingEdge.getLength();
                            startEdgeIndex = routingEdge.getIndex();
                            startNodeIndex = routingEdge.getToNodeIndex();
                            break;
                        } else {
                            isTheSameRoad = false;
                            break;
                        }
                    }
                }
            }

            // Dijkstra start node
            vertexDistFromSource.put(startNodeIndex, 0d);
            parent.put(startNodeIndex, startNodeIndex);
            MinPriorityQueue minHeap = new MinPriorityQueue();
            int currIndex = startNodeIndex;

//        LOGGER.info("start new shortest distance");
            // visit every node
            while (currIndex != -1 && vertexDistFromSource.get(currIndex) < (maxDistance - sourceDistance)) {
                // loop around the edges of current node
//            LOGGER.info(vertices[currIndex].getDistanceFromSource());
                List<RoutingEdge> currentOutgoingRoutingEdges = vertices[currIndex].getOutGoingRoutingEdges();
                for (RoutingEdge currentNodeRoutingEdge : currentOutgoingRoutingEdges) {
                    int nextVertexIndex = currentNodeRoutingEdge.getToNodeIndex();
                    double tentative = vertexDistFromSource.get(currIndex) + currentNodeRoutingEdge.getLength();
                    if (!vertexVisited.contains(nextVertexIndex) && minHeap.decreaseKey(nextVertexIndex, tentative)) {
                        vertexDistFromSource.put(nextVertexIndex, tentative);
                        parent.put(nextVertexIndex, currIndex);
                    }
                }
                // all neighbours checked so node visited
                vertexVisited.add(currIndex);
                if (nodeIndex2DestPointSet.containsKey(currIndex)) {
                    for (Pair<Integer, Double> resultIndex : nodeIndex2DestPointSet.get(currIndex)) {
                        destPointCount--;
                        distance[resultIndex._1()] = vertexDistFromSource.get(currIndex);
                        distance[resultIndex._1()] += sourceDistance;
                        distance[resultIndex._1()] += resultIndex._2();
                        distance[resultIndex._1()] += distFunc.distance(pointList.get(resultIndex._1()).getMatchedSegment().p1(), pointList
                                .get(resultIndex._1()).getMatchPoint());
                        path[resultIndex._1()] = findPath(currIndex, parent);
                        if (distFunc.distance(source.getMatchPoint(), source.getMatchedSegment().p2()) == 0) {
                            path[resultIndex._1()].remove(source.getRoadID());
                        } else {
                            List<String> refinedPath = new ArrayList<>();
                            refinedPath.add(source.getRoadID());
                            refinedPath.addAll(path[resultIndex._1()]);
                            path[resultIndex._1()] = refinedPath;
                        }
                        if (distFunc.distance(pointList.get(resultIndex._1()).getMatchedSegment().p1(),
                                pointList.get(resultIndex._1()).getMatchPoint()) == 0 && resultIndex._2() == 0) {
                            path[resultIndex._1()].remove(pointList.get(resultIndex._1()).getRoadID());
                        } else {
                            path[resultIndex._1()].remove(pointList.get(resultIndex._1()).getRoadID());
                            path[resultIndex._1()].add(pointList.get(resultIndex._1()).getRoadID());
                        }
                    }
                }
                if (destPointCount == 0) {
                    result = new ArrayList<>(resultOutput(distance, path));
                    return result;
                }
                // next node must be with shortest distance
//                minHeap.buildMinHeap();
//                currNode = minHeap.extractMin().getIndex();
                currIndex = minHeap.extractMin();
//            if(index!=currNode){
//                double value1 = vertices[index].getDistanceFromSource();
//                double value2 = vertices[currNode].getDistanceFromSource();
//                LOGGER.info("Inconsist index");
//            }
            }
//            LOGGER.info((pointList.size() - destPointCount) + "/" + pointList.size() + "distances are found.");
            result = new ArrayList<>(resultOutput(distance, path));
            return result;
        }
        result = new ArrayList<>(resultOutput(distance, path));
        return result;
    }

    private double distanceWithinEdge(int startNodeIndex, int currEdgeIndex, int destNodeIndex) {
        double distance = 0;
        while (startNodeIndex != destNodeIndex) {
            List<RoutingEdge> outGoingRoutingEdgeList = this.vertices[startNodeIndex].getOutGoingRoutingEdges();
            if (outGoingRoutingEdgeList.size() > 2)
                LOGGER.severe("ERROR! Current mini node has more than two outgoing edges:" + startNodeIndex);
            for (RoutingEdge routingEdge : outGoingRoutingEdgeList) {
                if (routingEdge.getIndex() != currEdgeIndex) {    // the current routingEdge is not the reverse routingEdge
                    distance += routingEdge.getLength();
                    currEdgeIndex = routingEdge.getIndex();
                    startNodeIndex = routingEdge.getToNodeIndex();
                    break;
                }
            }
        }
        return distance;
    }

    private void insertDestPoint(HashMap<Integer, Set<Pair<Integer, Double>>> nodeIndex2DestPointSet, int pointIndex, int destEdgeIndex) {
        int firstEdgeOfDest = edgeIndex2LeftMostEdgeIndexDist.get(destEdgeIndex)._1();
        if (nodeIndex2DestPointSet.containsKey(edgeIndex2EndpointsIndex.get(firstEdgeOfDest)._1())) {
            Set<Pair<Integer, Double>> currDestPointSet = nodeIndex2DestPointSet.get(edgeIndex2EndpointsIndex.get(firstEdgeOfDest)._1());
            currDestPointSet.add(new Pair<>(pointIndex, edgeIndex2LeftMostEdgeIndexDist.get(destEdgeIndex)._2()));
        } else {
            Set<Pair<Integer, Double>> currDestPointSet = new LinkedHashSet<>();
            currDestPointSet.add(new Pair<>(pointIndex, edgeIndex2LeftMostEdgeIndexDist.get(destEdgeIndex)._2()));
            nodeIndex2DestPointSet.put(edgeIndex2EndpointsIndex.get(firstEdgeOfDest)._1(), currDestPointSet);
        }
    }

    private List<String> findPath(int index, HashMap<Integer, Integer> parent) {
        Set<String> roadIDSet = new LinkedHashSet<>();
        while (parent.get(index) != index) {
            if (parent.get(index) == -1)
                LOGGER.severe("ERROR! Road path is broken!");
            Pair<Integer, Integer> currentSegment = new Pair<>(parent.get(index), index);
            int edgeID = endPointsIndex2EdgeIndex.get(currentSegment);
            roadIDSet.add(index2RoadIDAndSN.get(edgeID).split(",")[0]);
            index = parent.get(index);
        }
        List<String> roadIDList = new ArrayList<>(roadIDSet);
        Collections.reverse(roadIDList);
        return roadIDList;
    }

    private List<Pair<Double, List<String>>> resultOutput(double[] distance, List<String>[] path) {
        List<Pair<Double, List<String>>> result = new ArrayList<>();
        for (int i = 0; i < distance.length; i++) {
            result.add(new Pair<>(distance[i], path[i]));
        }
        return result;
    }

    public void addRoadByID(String roadID) {
        if (!roadID2NewEdgeList.containsKey(roadID) || !roadID2NewNodeList.containsKey(roadID))
            throw new IllegalArgumentException("ERROR! The road to be inserted has wrong ID: " + roadID);

        for (int i : roadID2NewNodeList.get(roadID))
            newNodeSet.remove(i);
        // add all the routingEdges to the vertices, each edge added to only from vertices
        for (int i : roadID2NewEdgeList.get(roadID)) {
            this.vertices[routingEdges[i].getFromNodeIndex()].getOutGoingRoutingEdges().add(routingEdges[i]);
            newEdgeSet.remove(i);
        }
    }

    public void removeRoadByID(String roadID) {
        if (!roadID2NewEdgeList.containsKey(roadID) || !roadID2NewNodeList.containsKey(roadID))
            throw new IllegalArgumentException("ERROR! The road to be removed has wrong ID.");

        newNodeSet.addAll(roadID2NewNodeList.get(roadID));
        // add all the routingEdges to the vertices, each edge added to only from vertices
        for (int i : roadID2NewEdgeList.get(roadID)) {
            this.vertices[routingEdges[i].getFromNodeIndex()].getOutGoingRoutingEdges().remove(routingEdges[i]);
        }
        newEdgeSet.addAll(roadID2NewEdgeList.get(roadID));
    }
}