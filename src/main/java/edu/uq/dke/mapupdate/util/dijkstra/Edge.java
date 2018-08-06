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

    public int getIndex() {
        return index;
    }

    public void setIndex(int id) {
        this.index = id;
    }
}
