package edu.uq.dke.mapupdate.util.dijkstra;

import java.util.ArrayList;
import java.util.List;

public class RoutingVertex {
    private List<RoutingEdge> outGoingRoutingEdges = new ArrayList<RoutingEdge>(); // now we must create outGoingRoutingEdges
    private int index;

    public List<RoutingEdge> getOutGoingRoutingEdges() {
        return outGoingRoutingEdges;
    }

    public void setOutGoingRoutingEdges(ArrayList<RoutingEdge> outGoingRoutingEdges) {
        this.outGoingRoutingEdges = outGoingRoutingEdges;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }
}
