package edu.uq.dke.mapupdate.util.dijkstra;

import java.util.ArrayList;

public class Node implements Comparable<Node> {
    private double distanceFromSource = Double.POSITIVE_INFINITY;
    private boolean visit = false;
    private ArrayList<Edge> edges = new ArrayList<Edge>(); // now we must create edges
    private int index;

    public double getDistanceFromSource() {
        return distanceFromSource;
    }

    public void setDistanceFromSource(double distanceFromSource) {
        this.distanceFromSource = distanceFromSource;
    }

    public boolean isVisited() {
        return visit;
    }

    public void setVisit(boolean visited) {
        this.visit = visited;
    }

    public ArrayList<Edge> getEdges() {
        return edges;
    }

    public void setEdges(ArrayList<Edge> edges) {
        this.edges = edges;
    }

    @Override
    public int compareTo(Node o) {
        if (o == null) {
            System.out.println("ERROR! Node to compare is NULL!");
            return -1;
        }
        return Double.compare(this.distanceFromSource, o.getDistanceFromSource());
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }
}
