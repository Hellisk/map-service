package edu.uq.dke.mapupdate.util.dijkstra;

import java.util.ArrayList;
import java.util.List;

public class Vertex implements Comparable<Vertex> {
    private double distanceFromSource = Double.POSITIVE_INFINITY;
    private boolean visited = false;
    private List<Edge> outGoingEdges = new ArrayList<Edge>(); // now we must create outGoingEdges
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

    public void setVisit(boolean visited) {
        this.visited = visited;
    }

    public List<Edge> getOutGoingEdges() {
        return outGoingEdges;
    }

    public void setOutGoingEdges(ArrayList<Edge> outGoingEdges) {
        this.outGoingEdges = outGoingEdges;
    }

    @Override
    public int compareTo(Vertex o) {
        if (o == null) {
            System.out.println("ERROR! Vertex to compare is NULL!");
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
