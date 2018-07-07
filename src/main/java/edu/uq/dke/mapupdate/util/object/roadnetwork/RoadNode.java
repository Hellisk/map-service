package edu.uq.dke.mapupdate.util.object.roadnetwork;

import edu.uq.dke.mapupdate.util.object.spatialobject.Point;

import java.io.Serializable;
import java.text.DecimalFormat;

/**
 * A node in the road network graph (OSM Node).
 * <br> The spatial coordinates of the node are immutable.
 *
 * @author uqdalves
 */
@SuppressWarnings("serial")
public final class RoadNode extends RoadNetworkPrimitive {

    /**
     * Longitude coordinate (x)
     */
    private final double lon; // x

    /**
     * Latitude coordinate (y)
     */
    private final double lat; // y

    /**
     * In-coming and out-going degree of the node, an intersection should have a non-zero degree, while the mini points are zero
     */
    private int inComingDegree = 0;
    private int outGoingDegree = 0;

    /**
     * Node type, 0 = non-intersection node, 1 = sub-node of the intersection, 2 = single-node intersection, 3 = main node of the
     * intersection, 4 = mini node, -1 = unknown
     */
    private short nodeType;

    /**
     * Create and empty road node with coordinates (0,0)
     */
    public RoadNode() {
        this.lon = 0.0;
        this.lat = 0.0;
    }

    /**
     * Create a road node.
     *
     * @param id  Node ID
     * @param lon Longitude coordinate (x)
     * @param lat Latitude coordinate (y)
     */
    public RoadNode(String id, double lon, double lat) {
        super(id);
        this.lon = lon;
        this.lat = lat;
    }

    /**
     * Create a road node.
     *
     * @param id       Node ID
     * @param lon      Longitude coordinate (x)
     * @param lat      Latitude coordinate (y)
     * @param nodeType Type of the node (intersection/non-intersection)
     */
    public RoadNode(String id, double lon, double lat, short nodeType) {
        super(id);
        this.lon = lon;
        this.lat = lat;
        this.inComingDegree = 0;
        this.outGoingDegree = 0;
        this.nodeType = nodeType;
    }

    /**
     * Create a road node.
     *
     * @param id             Node ID
     * @param lon            Longitude coordinate (x)
     * @param lat            Latitude coordinate (y)
     * @param inComingDegree Incoming degree
     * @param outGoingDegree Outgoing degree
     */
    public RoadNode(String id, double lon, double lat, int inComingDegree, int outGoingDegree, short nodeType) {
        super(id);
        this.lon = lon;
        this.lat = lat;
        this.inComingDegree = inComingDegree;
        this.outGoingDegree = outGoingDegree;
        this.nodeType = nodeType;
    }

    /**
     * Node longitude value.
     */
    public double lon() {
        return lon;
    }

    /**
     * Node latitude value.
     */
    public double lat() {
        return lat;
    }

    /**
     * @return Return the spatial point object representation
     * of this road node.
     */
    public Point toPoint() {
        Point p = new Point(lon, lat);
        p.setId(getId());
        return p;
    }

    @Override
    public String toString() {
        DecimalFormat df = new DecimalFormat("0.00000");
        return getId() + "," + df.format(lon()) + "," + df.format(lat()) + "," + getNodeType();
    }

    public static RoadNode parseRoadNode(String s) {
        String[] nodeInfo = s.split(",");
        if (nodeInfo.length != 4) throw new IndexOutOfBoundsException("ERROR! Failed to read road node: input data format is wrong. " + s);
        return new RoadNode(nodeInfo[0], Double.parseDouble(nodeInfo[1]), Double.parseDouble(nodeInfo[2]), Short.parseShort(nodeInfo[3]));
    }

    /**
     * Makes an exact copy of this object
     */
    @Override
    public RoadNode clone() {
        return new RoadNode(getId(), lon, lat, inComingDegree, outGoingDegree, nodeType);
    }

    @Override
    public void print() {
        System.out.println("ROAD NODE ( " + toString() + " )");
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        long temp;
        temp = Double.doubleToLongBits(lat);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(lon);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof RoadNode))
            return false;
        RoadNode other = (RoadNode) obj;
        return super.equals(other) && Double.doubleToLongBits(lat) == Double.doubleToLongBits(other.lat) && Double.doubleToLongBits(lon) == Double.doubleToLongBits(other.lon);
    }

    public int getDegree() {
        return inComingDegree + outGoingDegree;
    }

    public void clearDegree() {
        this.inComingDegree = 0;
        this.outGoingDegree = 0;
    }

    void increaseInComingDegree() {
        this.inComingDegree++;
    }

    void increaseOutGoingDegree() {
        this.outGoingDegree++;
    }

    public int getInComingDegree() {
        return inComingDegree;
    }

    public int getOutGoingDegree() {
        return outGoingDegree;
    }

    public short getNodeType() {
        return nodeType;
    }

    public void setNodeType(short nodeType) {
        this.nodeType = nodeType;
    }

}
