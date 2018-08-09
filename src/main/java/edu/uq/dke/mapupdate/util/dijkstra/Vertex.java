package edu.uq.dke.mapupdate.util.dijkstra;

import java.util.ArrayList;
import java.util.List;

public class Vertex {
    private List<Edge> outGoingEdges = new ArrayList<Edge>(); // now we must create outGoingEdges
    private int index;

    public List<Edge> getOutGoingEdges() {
        return outGoingEdges;
    }

    public void setOutGoingEdges(ArrayList<Edge> outGoingEdges) {
        this.outGoingEdges = outGoingEdges;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }
}
