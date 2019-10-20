package util.dijkstra;

import util.object.spatialobject.Point;

import java.util.ArrayList;
import java.util.List;

public class RoutingVertex {
    private List<RoutingEdge> outGoingRoutingEdges = new ArrayList<RoutingEdge>(); // now we must create outGoingRoutingEdges
    private Point vertexPoint;
    private int index;
    
    List<RoutingEdge> getOutGoingRoutingEdges() {
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
    
    public Point getVertexPoint() {
        return vertexPoint;
    }
    
    public void setVertexPoint(Point vertexPoint) {
        this.vertexPoint = vertexPoint;
    }
}
