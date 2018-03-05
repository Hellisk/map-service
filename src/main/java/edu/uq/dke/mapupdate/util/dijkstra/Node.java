package edu.uq.dke.mapupdate.util.dijkstra;

import java.util.ArrayList;

public class Node implements Comparable<Node> {
    private double distanceFromSource = Double.MAX_VALUE;
    private boolean visited;
    private ArrayList<Edge> edges = new ArrayList<Edge>(); // now we must create edges
    private int index;

    public double getDistanceFromSource() {
        return distanceFromSource;
    }

    public void setDistanceFromSource(double distanceFromSource) {
        this.distanceFromSource = distanceFromSource;
    }

    public boolean isVisited() {
        return visited;
    }

    public void setVisited(boolean visited) {
        this.visited = visited;
    }

    public ArrayList<Edge> getEdges() {
        return edges;
    }

    public void setEdges(ArrayList<Edge> edges) {
        this.edges = edges;
    }

    @Override
    public int compareTo(Node o) {
        if (this.distanceFromSource < o.getDistanceFromSource()) {
            return -1;
        } else if (this.distanceFromSource > o.getDistanceFromSource()) {
            return 1;
        } else {
            return 0;
        }
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }
}
