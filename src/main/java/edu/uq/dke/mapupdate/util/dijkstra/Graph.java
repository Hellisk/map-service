package edu.uq.dke.mapupdate.util.dijkstra;

import edu.uq.dke.mapupdate.datatype.MatchingPoint;
import traminer.util.map.roadnetwork.RoadNetworkGraph;
import traminer.util.map.roadnetwork.RoadNode;
import traminer.util.map.roadnetwork.RoadWay;
import traminer.util.spatial.distance.GreatCircleDistanceFunction;
import traminer.util.spatial.distance.PointDistanceFunction;
import traminer.util.spatial.objects.Point;
import traminer.util.spatial.objects.Segment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class Graph {
    HashMap<String, Integer> locNodeIndex = new HashMap<>();
    private Node[] nodes;
    private int noOfNodes;
    private Edge[] edges;
    private int noOfEdges;
    private PointDistanceFunction dist = new GreatCircleDistanceFunction();

    public Graph(Edge[] edges, PointDistanceFunction dist) {
        this.edges = edges;
        this.dist = dist;
        // create all nodes ready to be updated with the edges
        this.noOfNodes = calculateNoOfNodes(edges);
        this.nodes = new Node[this.noOfNodes];
        for (int n = 0; n < this.noOfNodes; n++) {
            this.nodes[n] = new Node();
        }
        // add all the edges to the nodes, each edge added to two nodes (to and from)
        this.noOfEdges = edges.length;
        for (int edgeToAdd = 0; edgeToAdd < this.noOfEdges; edgeToAdd++) {
            this.nodes[edges[edgeToAdd].getFromNodeIndex()].getEdges().add(edges[edgeToAdd]);
            this.nodes[edges[edgeToAdd].getToNodeIndex()].getEdges().add(edges[edgeToAdd]);
        }
    }

    public Graph(RoadNetworkGraph roadNetwork) {
        List<Edge> edgeList = new ArrayList<>();
        HashSet<String> locWayMapping = new HashSet<>();    // format: sx1_sy1,sx2_sy2
        int nodeIndex = 0;
        for (RoadNode node : roadNetwork.getNodes()) {
            if (!locNodeIndex.containsKey(node.lon() + "_" + node.lat())) {
                locNodeIndex.put(node.lon() + "_" + node.lat(), nodeIndex);
                nodeIndex++;
            }
        }
        for (RoadWay way : roadNetwork.getWays()) {
            for (Segment s : way.getEdges()) {
                if (!locWayMapping.contains(s.x1() + "_" + s.y1() + "," + s.x2() + "_" + s.y2())) {
                    int fromNodeIndex, toNodeIndex;
                    if (!locNodeIndex.containsKey(s.x1() + "_" + s.y1())) {
                        locNodeIndex.put(s.x1() + "_" + s.y1(), nodeIndex);
                        fromNodeIndex = nodeIndex;
                        nodeIndex++;
                    } else {
                        fromNodeIndex = locNodeIndex.get(s.x1() + "_" + s.y1());
                    }
                    if (!locNodeIndex.containsKey(s.x2() + "_" + s.y2())) {
                        locNodeIndex.put(s.x2() + "_" + s.y2(), nodeIndex);
                        toNodeIndex = nodeIndex;
                        nodeIndex++;
                    } else {
                        toNodeIndex = locNodeIndex.get(s.x2() + "_" + s.y2());
                    }
                    edgeList.add(new Edge(fromNodeIndex, toNodeIndex, dist.pointToPointDistance(s.x1(), s.y1(), s.x2(), s.y2())));
                    locWayMapping.add(s.x1() + "_" + s.y1() + "," + s.x2() + "_" + s.y2());
                } else System.out.println("Duplicate road when generating shortest path graph.");
            }
        }
        Edge[] edges = edgeList.toArray(new Edge[edgeList.size()]);

        this.edges = edges;
        // create all nodes ready to be updated with the edges
        this.noOfNodes = calculateNoOfNodes(edges);
        if (this.noOfNodes != locNodeIndex.size()) {
            System.out.println("Inconsistent node count:" + this.noOfNodes + ", " + locNodeIndex.size());
        }
        this.nodes = new Node[this.noOfNodes];
        for (int n = 0; n < this.noOfNodes; n++) {
            this.nodes[n] = new Node();
            this.nodes[n].setIndex(n);
        }
        // add all the edges to the nodes, each edge added to two nodes (to and from)
        this.noOfEdges = edges.length;
        for (int edgeToAdd = 0; edgeToAdd < this.noOfEdges; edgeToAdd++) {
            this.nodes[edges[edgeToAdd].getFromNodeIndex()].getEdges().add(edges[edgeToAdd]);
            this.nodes[edges[edgeToAdd].getToNodeIndex()].getEdges().add(edges[edgeToAdd]);
        }

        // check the completeness of the graph
        int isolatedNodeCount = 0;
        for (Node n : this.nodes) {
            if (n.getEdges().size() == 0)
                isolatedNodeCount++;
        }
        System.out.println("isolatedNodeCount = " + isolatedNodeCount);
        System.out.println("Shortest path graph generated. Total nodes:" + noOfNodes + ", total edges:" + noOfEdges);
    }

    private int calculateNoOfNodes(Edge[] edges) {
        int noOfNodes = 0;
        for (Edge e : edges) {
            if (e.getToNodeIndex() > noOfNodes)
                noOfNodes = e.getToNodeIndex();
            if (e.getFromNodeIndex() > noOfNodes)
                noOfNodes = e.getFromNodeIndex();
        }
        noOfNodes++;
        return noOfNodes;
    }

    // Calculate all shortest distance from given source to destination
    public double calculateShortestDistances(double sourceX, double sourceY, double destX, double destY) {

        if (!this.locNodeIndex.containsKey(sourceX + "_" + sourceY)) {
            System.out.println("Shortest distance calculation failed: Source node is not found.");
            return Double.MAX_VALUE;
        }
        if (!this.locNodeIndex.containsKey(destX + "_" + destY)) {
            System.out.println("Shortest distance calculation failed: Destination node is not found.");
            return Double.MAX_VALUE;
        }
        // node 0 as source
        int currNode = locNodeIndex.get(sourceX + "_" + sourceY);
        int destNode = locNodeIndex.get(destX + "_" + destY);
        this.nodes[currNode].setDistanceFromSource(0);
        int nextNode = currNode;
        // visit every node
        for (int i = 0; i < this.nodes.length; i++) {
            // loop around the edges of current node
            ArrayList<Edge> currentNodeEdges = this.nodes[nextNode].getEdges();
            for (Edge currentNodeEdge : currentNodeEdges) {
                int neighbourIndex = currentNodeEdge.getNeighbourIndex(nextNode);
                // only if not visited
                if (!this.nodes[neighbourIndex].isVisited()) {
                    double tentative = this.nodes[nextNode].getDistanceFromSource() + currentNodeEdge.getLength();
                    if (tentative < nodes[neighbourIndex].getDistanceFromSource()) {
                        nodes[neighbourIndex].setDistanceFromSource(tentative);
                    }
                }
            }
            // all neighbours checked so node visited
            nodes[nextNode].setVisited(true);

            if (nextNode == destNode) {
                return nodes[destNode].getDistanceFromSource();
            }
            // next node must be with shortest distance
            nextNode = getNodeShortestDistance();
        }
        return Double.MAX_VALUE;
    }

    // Calculate all shortest distance from given node
    public double[] calculateShortestDistanceList(MatchingPoint source, List<MatchingPoint> pointList, double maxDistance) {
        double[] distance = new double[pointList.size()];
        // initialize the distance matrix
        for (int i = 0; i < distance.length; i++) {
            distance[i] = Double.MAX_VALUE;
        }

        for (Node n : this.nodes) {
            n.setDistanceFromSource(Double.MAX_VALUE);
            n.setVisited(false);
        }

        HashMap<Integer, Integer> destinationIndex = new HashMap<>();        // (point index in graph, point index in pointList)

        // if source point doesn't exist, return infinity to all distances
        if (!this.locNodeIndex.containsKey(source.getMatchedSegment().x2() + "_" + source.getMatchedSegment().y2())) {
            System.out.println("Shortest distance calculation failed: Source node is not found.");
            return distance;
        }

        int currNode = locNodeIndex.get(source.getMatchedSegment().x2() + "_" + source.getMatchedSegment().y2());
        double sourceDistance = this.dist.pointToPointDistance(source.lon(), source.lat(), source.getMatchedSegment().x2(), source.getMatchedSegment().y2());
        // Dijkstra start node
        this.nodes[currNode].setDistanceFromSource(0);
//        MinPriorityQueue minHeap = new MinPriorityQueue(this.nodes.clone());
//        minHeap.buildMinHeap();
//        minHeap.extractMin();

        for (int i = 0; i < pointList.size(); i++) {
            Point endPoint = pointList.get(i).getMatchedSegment().getCoordinates().get(0);
            if (!this.locNodeIndex.containsKey(endPoint.x() + "_" + endPoint.y())) {
                System.out.println("Destination node is not found.");
            } else {
                destinationIndex.put(this.locNodeIndex.get(endPoint.x() + "_" + endPoint.y()), i);
            }
        }

        int nextNode = currNode;
        int hitCount = 0;
        // visit every node
        while (nodes[nextNode].getDistanceFromSource() < maxDistance) {
            // loop around the edges of current node
            ArrayList<Edge> currentNodeEdges = this.nodes[nextNode].getEdges();
            for (int joinedEdge = 0; joinedEdge < currentNodeEdges.size(); joinedEdge++) {
                int neighbourIndex = currentNodeEdges.get(joinedEdge).getNeighbourIndex(nextNode);
                // only if not visited
                if (!this.nodes[neighbourIndex].isVisited()) {
                    double tentative = this.nodes[nextNode].getDistanceFromSource() + currentNodeEdges.get(joinedEdge).getLength();
                    if (tentative < nodes[neighbourIndex].getDistanceFromSource()) {
                        nodes[neighbourIndex].setDistanceFromSource(tentative);
                    }
                }
            }
            // all neighbours checked so node visited
            nodes[nextNode].setVisited(true);
            if (destinationIndex.containsKey(nextNode)) {
                hitCount++;
                int index = destinationIndex.get(nextNode);
                distance[index] = nodes[nextNode].getDistanceFromSource();
                distance[index] += sourceDistance;
                distance[index] += this.dist.pointToPointDistance(pointList.get(index).getMatchedSegment().x1(), pointList.get(index).getMatchedSegment().y1(), pointList.get(index).lon(), pointList.get(index).lat());
                if (hitCount == distance.length) {
                    return distance;
                }
            }
            // next node must be with shortest distance
//            minHeap.buildMinHeap();
//            nextNode = minHeap.extractMin().getIndex();
            if (nextNode != getNodeShortestDistance()) {
                nextNode = getNodeShortestDistance();
            } else {
                break;
            }
//            if(index!=nextNode){
//                double value1 = nodes[index].getDistanceFromSource();
//                double value2 = nodes[nextNode].getDistanceFromSource();
//                System.out.println("Inconsist index");
//            }
        }
        return distance;
    }

    // calculate the shortest distance in each iteration
    private int getNodeShortestDistance() {
        int storedNodeIndex = 0;
        double storedDist = Double.MAX_VALUE;
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
        String output = "Number of nodes = " + this.noOfNodes;
        output += "\nNumber of edges = " + this.noOfEdges;
        for (int i = 0; i < this.nodes.length; i++) {
            output += ("\nThe shortest distance from node 0 to node " + i + " is " + nodes[i].getDistanceFromSource());
        }
        System.out.println(output);
    }

    public Node[] getNodes() {
        return nodes;
    }

    public int getNoOfNodes() {
        return noOfNodes;
    }

    public Edge[] getEdges() {
        return edges;
    }

    public int getNoOfEdges() {
        return noOfEdges;
    }
}