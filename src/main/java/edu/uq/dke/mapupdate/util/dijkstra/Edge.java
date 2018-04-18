package edu.uq.dke.mapupdate.util.dijkstra;

public class Edge {
    private int fromNodeIndex;
    private int toNodeIndex;
    private double length;
    private int index;

    public Edge(int fromNodeIndex, int toNodeIndex, double length) {
        this.fromNodeIndex = fromNodeIndex;
        this.toNodeIndex = toNodeIndex;
        this.length = length;
    }


    public int getFromNodeIndex() {
        return fromNodeIndex;
    }

    public int getToNodeIndex() {
        return toNodeIndex;
    }

    public double getLength() {
        return length;
    }

    // determines the neighbouring node of a supplied node, based on the two nodes connected by this edge
    public int getNeighbourIndex(int nodeIndex) {
        if (this.fromNodeIndex == nodeIndex) {
            return this.toNodeIndex;
        } else if (this.toNodeIndex == nodeIndex) {
            return this.fromNodeIndex;
        } else {
            System.out.println("ERROR! Cannot find the neighbour node index of the given node");
            return nodeIndex;
        }
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int id) {
        this.index = id;
    }
}
