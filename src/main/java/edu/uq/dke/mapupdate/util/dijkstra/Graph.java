package edu.uq.dke.mapupdate.util.dijkstra;

import edu.uq.dke.mapupdate.datatype.MatchingPoint;
import edu.uq.dke.mapupdate.mapmatching.hmm.MiniRoadSegment;
import traminer.util.Pair;
import traminer.util.map.roadnetwork.RoadNetworkGraph;
import traminer.util.map.roadnetwork.RoadNode;
import traminer.util.map.roadnetwork.RoadWay;
import traminer.util.spatial.distance.GreatCircleDistanceFunction;
import traminer.util.spatial.distance.PointDistanceFunction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Graph {
    private HashMap<String, Integer> oldNodeIDMapping = new HashMap<>();    // the mapping between node id in RoadNetworkGraph(random) and Graph(Sequence)
    //    private HashMap<Integer, String> edgeIDOwner = new HashMap<>();    // the mapping between the mini edge id and its corresponding roadway id
    private HashMap<MiniRoadSegment, Integer> findMiniEdgeIndex = new HashMap<>();  // find the mini road index using the coordinates and road way id
    private HashMap<Integer, Pair<Integer, Integer>> findEndPointIndicesOfEdge = new HashMap<>();  // find the end point indices given the mini edge index
    private Node[] nodes;
    private int noOfNodes = 0;
    private Edge[] edges;
    private int noOfEdges = 0;
    private PointDistanceFunction dist = new GreatCircleDistanceFunction();

//    public Graph(Edge[] edges, PointDistanceFunction dist) {
//        this.edges = edges;
//        this.dist = dist;
//        // create all nodes ready to be updated with the edges
//        this.noOfNodes = calculateNoOfNodes(edges);
//        this.nodes = new Node[this.noOfNodes];
//        for (int n = 0; n < this.noOfNodes; n++) {
//            this.nodes[n] = new Node();
//        }
//        // add all the edges to the nodes, each edge added to two nodes (to and from)
//        this.noOfEdges = edges.length;
//        for (int edgeToAdd = 0; edgeToAdd < this.noOfEdges; edgeToAdd++) {
//            this.nodes[edges[edgeToAdd].getFromNodeIndex()].getEdges().add(edges[edgeToAdd]);
//            this.nodes[edges[edgeToAdd].getToNodeIndex()].getEdges().add(edges[edgeToAdd]);
//        }
//    }

    public Graph(RoadNetworkGraph roadNetwork) {
        List<Edge> edgeList = new ArrayList<>();
//        HashMap<String, List<String>> locIDMapping = new HashMap<>();    // format: sx1_sy1,sx2_sy2
        // insert the road node into node list
        for (RoadNode node : roadNetwork.getNodes()) {
            if (oldNodeIDMapping.containsKey(node.getId()))
                System.err.println("Error: Old node ID already exists!");
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
                        System.err.println("Error: Old node ID already exists!");
                    oldNodeIDMapping.put(startNode.getId(), noOfNodes);
                    noOfNodes++;
                }
            }

            // insert mini edges to the edge list
            for (int i = 0; i < way.getNodes().size() - 1; i++) {
                RoadNode startNode = way.getNode(i);
                RoadNode endNode = way.getNode(i + 1);
//                edgeIDOwner.put(noOfEdges, way.getId());
                MiniRoadSegment roadSegment = new MiniRoadSegment(startNode.lon(), startNode.lat(), endNode.lon(), endNode.lat(), way.getId());
                findMiniEdgeIndex.put(roadSegment, noOfEdges);
                Pair<Integer, Integer> endPointIndices = new Pair<>(oldNodeIDMapping.get(startNode.getId()), oldNodeIDMapping.get(endNode.getId()));
                findEndPointIndicesOfEdge.put(noOfEdges, endPointIndices);
                Edge currEdge = new Edge(oldNodeIDMapping.get(startNode.getId()), oldNodeIDMapping.get(endNode.getId()), dist.pointToPointDistance(startNode.lon(), startNode.lat(), endNode.lon(), endNode.lat()));
                currEdge.setIndex(noOfEdges);
                edgeList.add(currEdge);
                noOfEdges++;
//                // insert the segment into the location-nodeID mapping
//                if (!locIDMapping.containsKey(startNode.lon() + "_" + startNode.lat() + "," + endNode.lon() + "_" + endNode.lat())) {
//                    List<String> currentIDList = new ArrayList<>();
//                    currentIDList.add(startNode.getId() + "_" + endNode.getId());
//                    locIDMapping.put(startNode.lon() + "_" + startNode.lat() + "," + endNode.lon() + "_" + endNode.lat(), currentIDList);
//
//                    edgeList.add(new Edge(fromNodeIndex, toNodeIndex, dist.pointToPointDistance(i.x1(), i.y1(), i.x2(), i.y2())));
//                    locIDMapping.add(i.x1() + "_" + i.y1() + "," + i.x2() + "_" + i.y2());
//                } else System.out.println("Duplicate road when generating shortest path graph.");
            }
        }
        Edge[] edges = edgeList.toArray(new Edge[edgeList.size()]);

        this.edges = edges;
        // create all nodes ready to be updated with the edges
        this.nodes = new Node[this.noOfNodes];
        for (int n = 0; n < this.noOfNodes; n++) {
            this.nodes[n] = new Node();
            this.nodes[n].setIndex(n);
        }
        // add all the edges to the nodes, each edge added to two nodes (to and from)
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
        System.out.println("Shortest path graph generated. Total nodes:" + this.noOfNodes + ", total edges:" + noOfEdges);
    }

//    // Calculate all shortest distance from given source to destination
//    public double calculateShortestDistances(String xid, String yid) {
//
//        if (!this.oldNodeIDMapping.containsKey(xid)) {
//            System.out.println("Shortest distance calculation failed: Source node is not found.");
//            return Double.MAX_VALUE;
//        }
//        if (!this.oldNodeIDMapping.containsKey(yid)) {
//            System.out.println("Shortest distance calculation failed: Destination node is not found.");
//            return Double.MAX_VALUE;
//        }
//        // node 0 as source
//        int currNode = oldNodeIDMapping.get(xid);
//        int destNode = oldNodeIDMapping.get(yid);
//        this.nodes[currNode].setDistanceFromSource(0);
//        int nextNode = currNode;
//
//        // TODO check the correctness
//        // visit every node
//        for (int i = 0; i < this.nodes.length; i++) {
//            // loop around the edges of current node
//            ArrayList<Edge> currentNodeEdges = this.nodes[nextNode].getEdges();
//            for (Edge currentNodeEdge : currentNodeEdges) {
//                int neighbourIndex = currentNodeEdge.getNeighbourIndex(nextNode);
//                // only if not visited
//                if (!this.nodes[neighbourIndex].isVisited()) {
//                    double tentative = this.nodes[nextNode].getDistanceFromSource() + currentNodeEdge.getLength();
//                    if (tentative < nodes[neighbourIndex].getDistanceFromSource()) {
//                        nodes[neighbourIndex].setDistanceFromSource(tentative);
//                    }
//                }
//            }
//            // all neighbours checked so node visited
//            nodes[nextNode].setVisited(true);
//
//            if (nextNode == destNode) {
//                return nodes[destNode].getDistanceFromSource();
//            }
//            // next node must be with shortest distance
//            nextNode = getNodeShortestDistance();
//        }
//        return Double.MAX_VALUE;
//    }

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
        MiniRoadSegment sourceSegment = new MiniRoadSegment(source.getMatchedSegment(), source.getRoadID());
        if (!this.findMiniEdgeIndex.containsKey(sourceSegment)) {
            System.out.println("Shortest distance calculation failed: Source node is not found.");
            return distance;
        }

        int currNode = findEndPointIndicesOfEdge.get(findMiniEdgeIndex.get(sourceSegment))._2();
        double sourceDistance = this.dist.pointToPointDistance(source.lon(), source.lat(), source.getMatchedSegment().x2(), source.getMatchedSegment().y2());
        // Dijkstra start node
        this.nodes[currNode].setDistanceFromSource(0);
        // TODO min-heap Dijkstra implementation
//        MinPriorityQueue minHeap = new MinPriorityQueue(this.nodes.clone());
//        minHeap.buildMinHeap();
//        minHeap.extractMin();

        int hitCount = 0;
        for (int i = 0; i < pointList.size(); i++) {
            MiniRoadSegment destinationSegment = new MiniRoadSegment(pointList.get(i).getMatchedSegment(), pointList.get(i).getRoadID());
            if (!this.findMiniEdgeIndex.containsKey(destinationSegment)) {
                System.out.println("Destination node is not found.");
            } else {
                if (destinationSegment.equals(sourceSegment)) {   // two nodes are in the same mini edge
                    distance[i] = this.dist.pointToPointDistance(source.lon(), source.lat(), pointList.get(i).getMatchPoint().x(), pointList.get(i).getMatchPoint().y());
                    hitCount++;
                }
                destinationIndex.put(findEndPointIndicesOfEdge.get(findMiniEdgeIndex.get(destinationSegment))._1(), i);
            }
        }

        int nextNode = currNode;
        // visit every node
        while (nodes[nextNode].getDistanceFromSource() < maxDistance) {
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
            if (destinationIndex.containsKey(nextNode)) {
                hitCount++;
                int index = destinationIndex.get(nextNode);
                distance[index] = nodes[nextNode].getDistanceFromSource();
                distance[index] += sourceDistance;
                distance[index] += this.dist.pointToPointDistance(pointList.get(index).getMatchedSegment().x1(), pointList.get(index).getMatchedSegment().y1(), pointList.get(index).lon(), pointList.get(index).lat());
            }
            if (hitCount == distance.length) {
                return distance;
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
        StringBuilder output = new StringBuilder("Number of nodes = " + this.noOfNodes);
        output.append("\nNumber of edges = ").append(this.noOfEdges);
        for (int i = 0; i < this.nodes.length; i++) {
            output.append("\nThe shortest distance from node 0 to node ").append(i).append(" is ").append(nodes[i].getDistanceFromSource());
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