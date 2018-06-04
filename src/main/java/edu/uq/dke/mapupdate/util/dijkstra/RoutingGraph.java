package edu.uq.dke.mapupdate.util.dijkstra;

import edu.uq.dke.mapupdate.util.function.GreatCircleDistanceFunction;
import edu.uq.dke.mapupdate.util.function.PointDistanceFunction;
import edu.uq.dke.mapupdate.util.object.datastructure.Pair;
import edu.uq.dke.mapupdate.util.object.datastructure.PointMatch;
import edu.uq.dke.mapupdate.util.object.roadnetwork.RoadNetworkGraph;
import edu.uq.dke.mapupdate.util.object.roadnetwork.RoadNode;
import edu.uq.dke.mapupdate.util.object.roadnetwork.RoadWay;
import edu.uq.dke.mapupdate.util.object.spatialobject.Segment;

import java.util.*;

public class RoutingGraph {
    private HashMap<Integer, String> index2EdgeID = new HashMap<>();    // the mapping between the mini edge id and its corresponding roadway id
    private HashMap<Segment, Integer> roadSegment2Index = new HashMap<>();  // find the mini road index using the coordinates and road
    // way id
    private HashMap<Integer, Pair<Integer, Integer>> edgeIndex2EndpointsIndex = new HashMap<>();  // find the end point indices given the mini edge index
    private HashMap<Pair<Integer, Integer>, Integer> endPointsIndex2EdgeIndex = new HashMap<>();  // find the mini edge index given the end point indices
    private Node[] nodes;
    private int noOfNodes = 0;
    private Edge[] edges;
    private int noOfEdges = 0;
    private PointDistanceFunction dist = new GreatCircleDistanceFunction();

    public RoutingGraph(RoadNetworkGraph roadNetwork) {
        List<Edge> edgeList = new ArrayList<>();
        // insert the road node into node list
        HashMap<String, Integer> oldNodeIDMapping = new HashMap<>();
        for (RoadNode node : roadNetwork.getNodes()) {
            if (oldNodeIDMapping.containsKey(node.getId()))
                System.err.println("ERROR! Old node ID already exists!");
            oldNodeIDMapping.put(node.getId(), noOfNodes);
            noOfNodes++;
        }
        for (RoadWay way : roadNetwork.getWays()) {

            // insert all the mini nodes to the node list
            for (int i = 0; i < way.getNodes().size() - 1; i++) {
                // insert all mini nodes into the nodeID index
                RoadNode startNode = way.getNode(i);
                if (i != 0) {
                    if (oldNodeIDMapping.containsKey(startNode.getId()))
                        System.err.println("ERROR! Old node ID already exists!");
                    oldNodeIDMapping.put(startNode.getId(), noOfNodes);
                    noOfNodes++;
                }
            }

            // insert mini edges to the edge list
            for (int i = 0; i < way.getNodes().size() - 1; i++) {
                RoadNode startNode = way.getNode(i);
                RoadNode endNode = way.getNode(i + 1);
                index2EdgeID.put(noOfEdges, way.getId());
                Segment roadSegment = new Segment(startNode.lon(), startNode.lat(), endNode.lon(), endNode.lat());
                roadSegment.setId(way.getId());
                roadSegment2Index.put(roadSegment, noOfEdges);
                int startID = oldNodeIDMapping.get(startNode.getId());
                int endID = oldNodeIDMapping.get(endNode.getId());
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
        // create all nodes ready to be updated with the edges
        this.nodes = new Node[noOfNodes];
        for (int n = 0; n < noOfNodes; n++) {
            this.nodes[n] = new Node();
            this.nodes[n].setIndex(n);
        }
        // add all the edges to the nodes, each edge added to only from nodes
        for (int edgeToAdd = 0; edgeToAdd < noOfEdges; edgeToAdd++) {
            this.nodes[edges[edgeToAdd].getFromNodeIndex()].getEdges().add(edges[edgeToAdd]);
        }

        // check the completeness of the graph
        int isolatedNodeCount = 0;
        for (Node n : this.nodes) {
            if (n.getEdges().size() == 0)
                isolatedNodeCount++;
        }
        System.out.println("No. of node having only incoming edges: " + isolatedNodeCount);
        System.out.println("Shortest path graph generated. Total nodes:" + noOfNodes + ", total edges:" + noOfEdges);
    }

    @SuppressWarnings("unchecked")
    // Calculate all shortest distance from given node
    public List<Pair<Double, List<String>>> calculateShortestDistanceList(PointMatch source, List<PointMatch> pointList, double maxDistance) {
        double[] distance = new double[pointList.size()];   // the distance to every destination
        ArrayList<String>[] path = new ArrayList[pointList.size()];     // the path to every destination
        int[] parent = new int[this.nodes.length];  // the index of its preceding point

        // initialization
        Arrays.fill(distance, Double.POSITIVE_INFINITY);
        Arrays.fill(path, new ArrayList<>());
        Arrays.fill(parent, -1);
        for (Node n : this.nodes) {
            n.setDistanceFromSource(0);
            n.setVisit(false);
        }

        HashMap<Integer, Integer> destinationIndex = new HashMap<>();        // (point index in graph, point index in pointList)

        // if source point doesn't exist, return infinity to all distances
        Segment sourceSegment = source.getMatchedSegment().clone();
        sourceSegment.setId(source.getRoadID());
        if (!this.roadSegment2Index.containsKey(sourceSegment)) {
            System.out.println("Shortest distance calculation failed: Source node is not found.");
            return resultOutput(distance, path);
        }

        int startNode = edgeIndex2EndpointsIndex.get(roadSegment2Index.get(sourceSegment))._2();
        double sourceDistance = this.dist.pointToPointDistance(source.lon(), source.lat(), source.getMatchedSegment().x2(), source.getMatchedSegment().y2());

        // attach all destination points to the graph
        int hitCount = 0;

        for (int i = 0; i < pointList.size(); i++) {
            Segment destinationSegment = pointList.get(i).getMatchedSegment().clone();
            destinationSegment.setId(pointList.get(i).getRoadID());
            if (!roadSegment2Index.containsKey(destinationSegment)) {
                System.out.println("Destination node is not found.");
            } else {
                if (destinationSegment.equals(sourceSegment)) {   // two segments refer to the same mini edge
                    distance[i] = dist.pointToPointDistance(source.lon(), source.lat(), pointList.get(i).getMatchPoint().x(), pointList.get(i).getMatchPoint().y());
                    path[i].add(destinationSegment.getId());
                    hitCount++;
                }
                int edgeID = roadSegment2Index.get(destinationSegment);
                destinationIndex.put(edgeIndex2EndpointsIndex.get(edgeID)._1(), i);
            }
        }

        // Dijkstra start node
        nodes[startNode].setDistanceFromSource(0);
        parent[startNode] = startNode;
        MinPriorityQueue minHeap = new MinPriorityQueue();
        int currIndex = startNode;

//        System.out.println("start new shortest distance");
        // visit every node
        while (currIndex != -1 && nodes[currIndex].getDistanceFromSource() < maxDistance) {
            // loop around the edges of current node
//            System.out.println(nodes[currIndex].getDistanceFromSource());
            ArrayList<Edge> currentOutgoingEdges = nodes[currIndex].getEdges();
            for (Edge currentNodeEdge : currentOutgoingEdges) {
                int neighbourIndex = currentNodeEdge.getNeighbourIndex(currIndex);
                double tentative = nodes[currIndex].getDistanceFromSource() + currentNodeEdge.getLength();
                if (!nodes[neighbourIndex].isVisited() && minHeap.decreaseKey(neighbourIndex, tentative)) {
                    nodes[neighbourIndex].setDistanceFromSource(tentative);
                    parent[neighbourIndex] = currIndex;
                }
            }
            // all neighbours checked so node visited
            nodes[currIndex].setVisit(true);
            if (destinationIndex.containsKey(currIndex)) {
                hitCount++;
                int index = destinationIndex.get(currIndex);
                distance[index] = nodes[currIndex].getDistanceFromSource();
                distance[index] += sourceDistance;
                distance[index] += dist.pointToPointDistance(pointList.get(index).getMatchedSegment().x1(), pointList.get(index).getMatchedSegment().y1(), pointList.get(index).lon(), pointList.get(index).lat());
                path[index] = findPath(currIndex, parent);
            }
            if (hitCount == distance.length) {
                return resultOutput(distance, path);
            }
            // next node must be with shortest distance
//            minHeap.buildMinHeap();
//            currNode = minHeap.extractMin().getIndex();
            currIndex = minHeap.extractMin();
//            if(index!=currNode){
//                double value1 = nodes[index].getDistanceFromSource();
//                double value2 = nodes[currNode].getDistanceFromSource();
//                System.out.println("Inconsist index");
//            }
        }
        return resultOutput(distance, path);
    }

    private ArrayList<String> findPath(int index, int[] parent) {
        Set<String> roadIDList = new LinkedHashSet<>();
        while (parent[index] != index) {
            if (parent[index] == -1)
                System.out.println("ERROR! Road path is broken!");
            Pair<Integer, Integer> currentSegment = new Pair<>(parent[index], index);
            int edgeID = endPointsIndex2EdgeIndex.get(currentSegment);
            roadIDList.add(index2EdgeID.get(edgeID));
            index = parent[index];
        }
        return new ArrayList<>(roadIDList);
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
        for (int i = 0; i < nodes.length; i++) {
            double currentDist = nodes[i].getDistanceFromSource();
            if (!nodes[i].isVisited() && currentDist < storedDist) {
                storedDist = currentDist;
                storedNodeIndex = i;
            }
        }
        return storedNodeIndex;
    }

    // display result
    public void printResult() {
        StringBuilder output = new StringBuilder("Number of nodes = " + noOfNodes);
        output.append("\nNumber of edges = ").append(noOfEdges);
        for (int i = 0; i < nodes.length; i++) {
            output.append("\nThe shortest distance from node 0 to node ").append(i).append(" is ").append(nodes[i].getDistanceFromSource());
        }
        System.out.println(output);
    }

    public Node[] getNodes() {
        return nodes;
    }

    public Edge[] getEdges() {
        return edges;
    }

}