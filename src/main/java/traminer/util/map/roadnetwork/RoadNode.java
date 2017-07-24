package traminer.util.map.roadnetwork;

import traminer.util.spatial.objects.Point;

import java.util.ArrayList;
import java.util.List;

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
     **/
    private final double lon; // x
    /**
     * Latitude coordinate (y)
     **/
    private final double lat; // y

    /**
     * Degree of the node, an intersection will have a non-zero degree, while the mini points are zero
     **/
    private int degree;

    /**
     * Whether it is a intersection or an intermediate node within a road segment
     */
    private boolean isMiniNode;

    /**
     * List of road ways that start from this road node
     */
    private List<RoadWay> outgoingAdjacentList;

    /**
     * List of road ways that end at this road node
     */
    private List<RoadWay> incomingAdjacentList;

    /**
     * Creates and empty road node with coordinates (0,0)
     */
    public RoadNode() {
        this.lon = 0.0;
        this.lat = 0.0;
        this.degree = 0;
        this.isMiniNode = false;
        this.incomingAdjacentList = new ArrayList<>();
        this.outgoingAdjacentList = new ArrayList<>();
    }

    /**
     * Creates and empty road node.
     *
     * @param id  Node ID
     * @param lon Longitude coordinate (x)
     * @param lat Latitude coordinate (y)
     */
    public RoadNode(String id, double lon, double lat) {
        super(id);
        this.lon = lon;
        this.lat = lat;
        this.degree = 0;
        this.isMiniNode = false;
        this.incomingAdjacentList = new ArrayList<>();
        this.outgoingAdjacentList = new ArrayList<>();
    }

    /**
     * Creates and empty road node.
     *
     * @param id         Node ID
     * @param lon        Longitude coordinate (x)
     * @param lat        Latitude coordinate (y)
     * @param isMiniNode whether it is a intermediate node of a road way
     */
    public RoadNode(String id, double lon, double lat, boolean isMiniNode) {
        super(id);
        this.lon = lon;
        this.lat = lat;
        this.degree = 0;
        this.isMiniNode = isMiniNode;
        this.incomingAdjacentList = new ArrayList<>();
        this.outgoingAdjacentList = new ArrayList<>();
    }

    /**
     * Creates and empty road node.
     *
     * @param id         Node ID
     * @param lon        Longitude coordinate (x)
     * @param lat        Latitude coordinate (y)
     * @param isMiniNode whether it is a intermediate node of a road way
     * @param timeStamp  Node time-stamp
     */
    public RoadNode(String id, double lon, double lat, boolean isMiniNode, String timeStamp) {
        super(id, timeStamp);
        this.lon = lon;
        this.lat = lat;
        this.degree = 0;
        this.isMiniNode = isMiniNode;
        this.incomingAdjacentList = new ArrayList<>();
        this.outgoingAdjacentList = new ArrayList<>();
    }

    /**
     * Creates and empty road node.
     *
     * @param id         Node ID
     * @param lon        Longitude coordinate (x)
     * @param lat        Latitude coordinate (y)
     * @param isMiniNode whether it is a intermediate node of a road way
     * @param timeStamp  Node time-stamp (e.g. seconds, milliseconds)
     */
    public RoadNode(String id, double lon, double lat, boolean isMiniNode, long timeStamp) {
        super(id, "" + timeStamp);
        this.lon = lon;
        this.lat = lat;
        this.degree = 0;
        this.isMiniNode = isMiniNode;
        this.incomingAdjacentList = new ArrayList<>();
        this.outgoingAdjacentList = new ArrayList<>();
    }

    /**
     * Creates and empty road node.
     *
     * @param id        Node ID
     * @param lon       Longitude coordinate (x)
     * @param lat       Latitude coordinate (y)
     * @param timeStamp Node time-stamp
     */
    public RoadNode(String id, double lon, double lat, String timeStamp) {
        super(id, timeStamp);
        this.lon = lon;
        this.lat = lat;
        this.degree = 0;
        this.isMiniNode = false;
        this.incomingAdjacentList = new ArrayList<>();
        this.outgoingAdjacentList = new ArrayList<>();
    }

    /**
     * Creates and empty road node.
     *
     * @param id        Node ID
     * @param lon       Longitude coordinate (x)
     * @param lat       Latitude coordinate (y)
     * @param timeStamp Node time-stamp (e.g. seconds, milliseconds)
     */
    public RoadNode(String id, double lon, double lat, long timeStamp) {
        super(id, "" + timeStamp);
        this.lon = lon;
        this.lat = lat;
        this.degree = 0;
        this.isMiniNode = false;
        this.incomingAdjacentList = new ArrayList<>();
        this.outgoingAdjacentList = new ArrayList<>();
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
        String s = getId() + " " +
                String.format("%.5f %.5f", lon, lat);
        if (!getTimeStamp().equals("")) {
            s += " " + getTimeStamp();
        }
        return s;
    }

    /**
     * Makes an exact copy of this object
     */
    @Override
    public RoadNode clone() {
        RoadNode clone = new RoadNode(getId(), lon, lat, getTimeStamp());
        return clone;
    }

    @Override
    public void print() {
        System.out.println("ROAD NODE ( " + toString() + " )");
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
        if (!this.getId().equals(other.getId()))
            return false;
        if (Double.doubleToLongBits(lat) !=
                Double.doubleToLongBits(other.lat))
            return false;
        return Double.doubleToLongBits(lon) == Double.doubleToLongBits(other.lon);
    }

    public int getDegree() {
        return degree;
    }

    public boolean isMiniNode() {
        return isMiniNode;
    }

    public List<RoadWay> getIncomingAdjacentList() {
        return incomingAdjacentList;
    }

    public void setIncomingAdjacentList(List<RoadWay> incomingAdjacentList) {
        this.degree -= this.incomingAdjacentList.size();
        this.outgoingAdjacentList = incomingAdjacentList;
        this.degree += this.incomingAdjacentList.size();
    }

    public void addIncomingAdjacency(RoadWay adjacentRoadWay) {
        if (!this.incomingAdjacentList.contains(adjacentRoadWay)) {
            this.incomingAdjacentList.add(adjacentRoadWay);
            this.degree++;
        }
    }

    public void addIncomingAdjacency(List<RoadWay> adjacentList) {
        for (RoadWay w : adjacentList) {
            if (!this.incomingAdjacentList.contains(w)) {
                this.incomingAdjacentList.addAll(adjacentList);
                this.degree++;
            }
        }
    }

    public List<RoadWay> getOutgoingAdjacentList() {
        return this.outgoingAdjacentList;
    }

    public void setOutgoingAdjacentList(List<RoadWay> outgoingAdjacentList) {
        this.degree -= this.outgoingAdjacentList.size();
        this.outgoingAdjacentList = outgoingAdjacentList;
        this.degree += this.outgoingAdjacentList.size();
    }

    public void addOutgoingAdjacency(RoadWay adjacentRoadWay) {
        if (!outgoingAdjacentList.contains(adjacentRoadWay)) {
            this.outgoingAdjacentList.add(adjacentRoadWay);
            this.degree++;
        }
    }

    public void addOutgoingAdjacency(List<RoadWay> adjacentList) {
        for (RoadWay w : adjacentList) {
            if (!this.outgoingAdjacentList.contains(w)) {
                this.outgoingAdjacentList.addAll(adjacentList);
                this.degree++;
            }
        }
    }

    // check whether the road node has a right degree value and adjacent lists
    public boolean checkNodeCompleteness() {
        if (this.degree != incomingAdjacentList.size() + outgoingAdjacentList.size() || this.degree < 0) {
            System.out.println("Degree is not equivalent to the adjacent size, node ID:" + this.getId());
            return false;
        }
        if (this.isMiniNode) {
            if (this.degree != 0) {
                System.out.println("Degree is not 0 for a mini node:" + this.getId() + "," + this.getDegree());
                return false;
            } else {
                return true;
            }
        } else return true;
    }

    public void setToMiniNode() {
        this.isMiniNode = true;
    }

    public void setToNonMiniNode() {
        this.isMiniNode = false;
    }
}
