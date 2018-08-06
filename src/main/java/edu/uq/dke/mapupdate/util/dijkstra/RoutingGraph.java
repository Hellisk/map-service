package edu.uq.dke.mapupdate.util.dijkstra;

import edu.uq.dke.mapupdate.util.function.GreatCircleDistanceFunction;
import edu.uq.dke.mapupdate.util.function.PointDistanceFunction;
import edu.uq.dke.mapupdate.util.object.datastructure.Pair;
import edu.uq.dke.mapupdate.util.object.datastructure.PointMatch;
import edu.uq.dke.mapupdate.util.object.roadnetwork.RoadNetworkGraph;
import edu.uq.dke.mapupdate.util.object.roadnetwork.RoadNode;
import edu.uq.dke.mapupdate.util.object.roadnetwork.RoadWay;

import java.util.*;

public class RoutingGraph {
    private HashMap<Integer, String> index2RoadIDAndSN = new HashMap<>();    // the mapping between the mini edge index and its corresponding
    // roadway id and serial number, format: (index,roadID,serialNum)
    private HashMap<String, Integer> endPointLoc2Index = new HashMap<>();  // find the mini edge index using the coordinates and road
    // way id, format: (x1_x2,y1_y2,id)
    private HashMap<Integer, Pair<Integer, Integer>> edgeIndex2EndpointsIndex = new HashMap<>();  // find the end point indices given the mini edge index
    private HashMap<Pair<Integer, Integer>, Integer> endPointsIndex2EdgeIndex = new HashMap<>();  // find the mini edge index given the end point indices
    private Vertex[] vertices;
    private int noOfVertices = 0;   // the size of the vertex list and the current index of the new vertex
    private Edge[] edges;           // the size of the edge list and the current index of the new edge
    private int noOfEdges = 0;
    private int[] parent;  // the index of its preceding point
    private PointDistanceFunction dist = new GreatCircleDistanceFunction();

    public RoutingGraph(RoadNetworkGraph roadNetwork) {
        // insert the road node into node list
        HashMap<String, Integer> nodeID2Index = new HashMap<>();
        HashSet<Integer> outGoingNodeSet = new HashSet<>();
        for (RoadNode node : roadNetwork.getNodes()) {
            if (nodeID2Index.containsKey(node.getID()))
                System.err.println("ERROR! Road node ID already exists: " + node.getID());
            nodeID2Index.put(node.getID(), noOfVertices);
            noOfVertices++;
        }

        List<Edge> edgeList = new ArrayList<>();
        for (RoadWay way : roadNetwork.getWays()) {
            // insert all the mini vertices to the node list
            for (int i = 1; i < way.getNodes().size() - 1; i++) {
                // insert all mini vertices into the nodeID index
                RoadNode startNode = way.getNode(i);
                if (nodeID2Index.containsKey(startNode.getID()))
                    System.err.println("ERROR! Road node ID for mini node already exists: " + startNode.getID());
                nodeID2Index.put(startNode.getID(), noOfVertices);
                noOfVertices++;
            }

            // insert mini edges to the edge list
            for (int i = 0; i < way.getNodes().size() - 1; i++) {
                RoadNode startNode = way.getNode(i);
                RoadNode endNode = way.getNode(i + 1);
                index2RoadIDAndSN.put(noOfEdges, way.getID() + "," + (i + 1));
                endPointLoc2Index.put(startNode.lon() + "_" + startNode.lat() + "," + endNode.lon() + "_" + endNode.lat() + "," + way.getID(), noOfEdges);
                int startID = nodeID2Index.get(startNode.getID());
                int endID = nodeID2Index.get(endNode.getID());
                Pair<Integer, Integer> endPointIndices = new Pair<>(startID, endID);
                edgeIndex2EndpointsIndex.put(noOfEdges, endPointIndices);
                endPointsIndex2EdgeIndex.put(endPointIndices, noOfEdges);
                Edge currEdge = new Edge(startID, endID, dist.pointToPointDistance(startNode.lon(), startNode.lat(), endNode.lon(), endNode.lat()));
                currEdge.setIndex(noOfEdges);
                edgeList.add(currEdge);
                noOfEdges++;
            }
        }
        Edge[] edges = edgeList.toArray(new Edge[0]);

        this.edges = edges;
        // create all vertices ready to be updated with the edges
        this.vertices = new Vertex[noOfVertices];
        for (int n = 0; n < noOfVertices; n++) {
            this.vertices[n] = new Vertex();
            this.vertices[n].setIndex(n);
        }
        // add all the edges to the vertices, each edge added to only from vertices
        for (int i = 0; i < noOfEdges; i++) {
            this.vertices[edges[i].getFromNodeIndex()].getOutGoingEdges().add(edges[i]);
        }

        // check the completeness of the graph
        for (Vertex n : this.vertices) {
            if (n.getOutGoingEdges().size() != 0) {
                for (Edge e : n.getOutGoingEdges()) {
                    outGoingNodeSet.add(e.getToNodeIndex());
                }
            }
        }
        for (int i = 0; i < vertices.length; i++) {
            if (!outGoingNodeSet.contains(i) && this.vertices[i].getOutGoingEdges().size() == 0)
                System.out.println("ERROR! Isolated node detected: No. " + i);
        }
        parent = new int[this.vertices.length];
        Arrays.fill(parent, -1);
        System.out.println("Shortest path graph generated. Total vertices:" + noOfVertices + ", total edges:" + noOfEdges);
    }

    @SuppressWarnings("unchecked")
    // Calculate all shortest distance from given node
    // TODO performance improvement
    public List<Pair<Double, List<String>>> calculateShortestDistanceList(PointMatch source, List<PointMatch> pointList, double maxDistance) {
        double[] distance = new double[pointList.size()];   // the distance to every destination
        List<String>[] path = new ArrayList[pointList.size()];     // the path to every destination
        HashSet<Integer> visitedIndexSet = new LinkedHashSet<>();
        List<Pair<Double, List<String>>> result;

        Arrays.fill(distance, Double.POSITIVE_INFINITY);
        for (int i = 0; i < path.length; i++)
            path[i] = new ArrayList<>();
        // the variables have been initialized during the last calculation. Start the process right away
        HashMap<Integer, Set<Integer>> edgeIndex2DestPointSet = new HashMap<>();        // (vertex index in graph, point index in pointList)

        // if source point doesn't exist, return infinity to all distances
        String sourceLocID = source.getMatchedSegment().x1() + "_" + source.getMatchedSegment().y1() + "," + source.getMatchedSegment()
                .x2() + "_" + source.getMatchedSegment().y2() + "," + source.getRoadID();
        if (!this.endPointLoc2Index.containsKey(sourceLocID)) {
            System.out.println("Shortest distance calculation failed: Source node is not found.");
            result = new ArrayList<>(resultOutput(distance, path));
            restoreVariables(visitedIndexSet);
            return result;
        }

        // the start node of the current Dijkstra rotation
        int startEdgeIndex = endPointLoc2Index.get(sourceLocID);
        String startRoadID = index2RoadIDAndSN.get(startEdgeIndex).split(",")[0];
        int startRoadSN = Integer.parseInt(index2RoadIDAndSN.get(startEdgeIndex).split(",")[1]);
        int startNodeIndex = edgeIndex2EndpointsIndex.get(startEdgeIndex)._2();
        double sourceDistance = this.dist.pointToPointDistance(source.lon(), source.lat(), source.getMatchedSegment().x2(), source.getMatchedSegment().y2());

        // attach all destination points to the graph
        int destPointCount = pointList.size();
        for (int i = 0; i < pointList.size(); i++) {
            String destLocID = pointList.get(i).getMatchedSegment().x1() + "_" + pointList.get(i).getMatchedSegment().y1() + "," +
                    pointList.get(i).getMatchedSegment().x2() + "_" + pointList.get(i).getMatchedSegment().y2() + "," + pointList.get(i)
                    .getRoadID();
            if (!endPointLoc2Index.containsKey(destLocID)) {
                System.out.println("ERROR! Destination node is not found.");
                destPointCount--;
            } else {
                int destEdgeIndex = endPointLoc2Index.get(destLocID);
                String destRoadID = index2RoadIDAndSN.get(destEdgeIndex).split(",")[0];
                if (destRoadID.equals(startRoadID)) {   // two segments refer to the same road
                    if (destEdgeIndex == startEdgeIndex) {   // two segments refer to the same mini edge
                        distance[i] = dist.pointToPointDistance(source.lon(), source.lat(), pointList.get(i).lon(), pointList.get(i).lat());
                        path[i].add(pointList.get(i).getRoadID());
                        destPointCount--;
                    } else {
                        int destRoadSN = Integer.parseInt(index2RoadIDAndSN.get(destEdgeIndex).split(",")[1]);
                        int destNodeIndex = edgeIndex2EndpointsIndex.get(destEdgeIndex)._1();
                        if (startRoadSN < destRoadSN) { // the start node is located at the upstream of the destination node within the same
                            // road
                            distance[i] += sourceDistance;
                            int currIndex = startNodeIndex;
                            int currEdgeIndex = startEdgeIndex;
                            while (currIndex != destNodeIndex) {
                                List<Edge> outGoingEdgeList = this.vertices[currIndex].getOutGoingEdges();
                                if (outGoingEdgeList.size() > 2)
                                    System.out.println("ERROR! Current mini node has more than two outgoing edges:" + currIndex);
                                for (Edge edge : outGoingEdgeList) {
                                    if (edge.getIndex() != currEdgeIndex) {    // the current edge is not the reverse edge
                                        distance[i] += edge.getLength();
                                        currEdgeIndex = edge.getIndex();
                                        currIndex = edge.getToNodeIndex();
                                        break;
                                    }
                                }
                            }
                            distance[i] += dist.pointToPointDistance(pointList.get(i).getMatchedSegment().x1(), pointList.get(i)
                                    .getMatchedSegment().y1(), pointList.get(i).lon(), pointList.get(i).lat());
                            path[i].add(pointList.get(i).getRoadID());
                            destPointCount--;
                        } else {
                            insertDestPoint(edgeIndex2DestPointSet, i, destEdgeIndex);
                        }
                    }
                } else {
                    insertDestPoint(edgeIndex2DestPointSet, i, destEdgeIndex);
                }
            }
        }

        // the rest of the destinations are on different roads, now set the end of the current road as start vertex
        boolean isTheSameRoad = true;
        while (isTheSameRoad) {
            List<Edge> outGoingEdgeList = this.vertices[startNodeIndex].getOutGoingEdges();
            if (outGoingEdgeList.size() == 0)
                isTheSameRoad = false;
            for (Edge edge : outGoingEdgeList) {
                if (edge.getIndex() != startEdgeIndex) {    // the current edge is not the reverse edge
                    if (index2RoadIDAndSN.get(edge.getIndex()).split(",")[0].equals(startRoadID)) {  // still on the same road
                        sourceDistance += edge.getLength();
                        startEdgeIndex = edge.getIndex();
                        startNodeIndex = edge.getToNodeIndex();
                        break;
                    } else {
                        isTheSameRoad = false;
                        break;
                    }
                }
            }
        }

        if (destPointCount > 0) {
            // Dijkstra start node
            visitedIndexSet.add(startNodeIndex);
            vertices[startNodeIndex].setDistanceFromSource(0);
            parent[startNodeIndex] = startNodeIndex;
            MinPriorityQueue minHeap = new MinPriorityQueue();
            int currIndex = startNodeIndex;

//        System.out.println("start new shortest distance");
            // visit every node
            while (currIndex != -1 && vertices[currIndex].getDistanceFromSource() < maxDistance) {
                // loop around the edges of current node
//            System.out.println(vertices[currIndex].getDistanceFromSource());
                List<Edge> currentOutgoingEdges = vertices[currIndex].getOutGoingEdges();
                for (Edge currentNodeEdge : currentOutgoingEdges) {
                    int nextVertexIndex = currentNodeEdge.getToNodeIndex();
                    double tentative = vertices[currIndex].getDistanceFromSource() + currentNodeEdge.getLength();
                    visitedIndexSet.add(nextVertexIndex);
                    if (!vertices[nextVertexIndex].isVisited() && minHeap.decreaseKey(nextVertexIndex, tentative)) {
                        vertices[nextVertexIndex].setDistanceFromSource(tentative);
                        parent[nextVertexIndex] = currIndex;
                    }
                }
                // all neighbours checked so node visited
                vertices[currIndex].setVisit(true);
                if (edgeIndex2DestPointSet.containsKey(currIndex)) {
                    for (int resultIndex : edgeIndex2DestPointSet.get(currIndex)) {
                        destPointCount--;
                        distance[resultIndex] = vertices[currIndex].getDistanceFromSource();
                        distance[resultIndex] += sourceDistance;
                        distance[resultIndex] += dist.pointToPointDistance(pointList.get(resultIndex).getMatchedSegment().x1(), pointList.get(resultIndex).getMatchedSegment().y1(), pointList.get(resultIndex).lon(), pointList.get(resultIndex).lat());
                        path[resultIndex] = findPath(currIndex, parent);
                    }
                }
                if (destPointCount == 0) {
                    result = new ArrayList<>(resultOutput(distance, path));
                    restoreVariables(visitedIndexSet);
                    return result;
                }
                // next node must be with shortest distance
//            minHeap.buildMinHeap();
//            currNode = minHeap.extractMin().getIndex();
                currIndex = minHeap.extractMin();
//            if(index!=currNode){
//                double value1 = vertices[index].getDistanceFromSource();
//                double value2 = vertices[currNode].getDistanceFromSource();
//                System.out.println("Inconsist index");
//            }
            }
//            System.out.println((pointList.size() - destPointCount) + "/" + pointList.size() + "distances are found.");
            result = new ArrayList<>(resultOutput(distance, path));
            restoreVariables(visitedIndexSet);
            return result;
        }
        result = new ArrayList<>(resultOutput(distance, path));
        restoreVariables(visitedIndexSet);
        return result;
    }

    private void restoreVariables(HashSet<Integer> visitedIndexSet) {
        // initialization
        for (int index : visitedIndexSet) {
            this.vertices[index].setDistanceFromSource(0);
            this.vertices[index].setVisit(false);
            this.parent[index] = -1;
        }
    }

    private void insertDestPoint(HashMap<Integer, Set<Integer>> edgeIndex2DestPointSet, int pointIndex, int destEdgeIndex) {
        if (edgeIndex2DestPointSet.containsKey(edgeIndex2EndpointsIndex.get(destEdgeIndex)._1())) {
            Set<Integer> currDestPointSet = edgeIndex2DestPointSet.get(edgeIndex2EndpointsIndex.get(destEdgeIndex)._1());
            currDestPointSet.add(pointIndex);
        } else {
            Set<Integer> currDestPointSet = new LinkedHashSet<>();
            currDestPointSet.add(pointIndex);
            edgeIndex2DestPointSet.put(edgeIndex2EndpointsIndex.get(destEdgeIndex)._1(), currDestPointSet);
        }
    }

    private List<String> findPath(int index, int[] parent) {
        Set<String> roadIDSet = new LinkedHashSet<>();
        while (parent[index] != index) {
            if (parent[index] == -1)
                System.out.println("ERROR! Road path is broken!");
            Pair<Integer, Integer> currentSegment = new Pair<>(parent[index], index);
            int edgeID = endPointsIndex2EdgeIndex.get(currentSegment);
            roadIDSet.add(index2RoadIDAndSN.get(edgeID).split(",")[0]);
            index = parent[index];
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

    // calculate the shortest distance in each iteration
    private int getNodeShortestDistance() {
        int storedNodeIndex = -1;
        double storedDist = Double.POSITIVE_INFINITY;
        for (int i = 0; i < vertices.length; i++) {
            double currentDist = vertices[i].getDistanceFromSource();
            if (!vertices[i].isVisited() && currentDist < storedDist) {
                storedDist = currentDist;
                storedNodeIndex = i;
            }
        }
        return storedNodeIndex;
    }

    // display result
    public void printResult() {
        StringBuilder output = new StringBuilder("Number of vertices = " + noOfVertices);
        output.append("\nNumber of edges = ").append(noOfEdges);
        for (int i = 0; i < vertices.length; i++) {
            output.append("\nThe shortest distance from node 0 to node ").append(i).append(" is ").append(vertices[i].getDistanceFromSource());
        }
        System.out.println(output);
    }

    public Vertex[] getVertices() {
        return vertices;
    }

    public Edge[] getEdges() {
        return edges;
    }

}