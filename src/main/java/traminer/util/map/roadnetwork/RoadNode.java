package traminer.util.map.roadnetwork;

import traminer.util.spatial.objects.Point;

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
     * In-coming and out-going degree of the node, an intersection should have a non-zero degree, while the mini points are zero
     **/
    private int inComingDegree;
    private int outGoingDegree;

    /**
     * Create and empty road node with coordinates (0,0)
     */
    public RoadNode() {
        this.lon = 0.0;
        this.lat = 0.0;
        this.inComingDegree = 0;
        this.outGoingDegree = 0;
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
        this.inComingDegree = 0;
        this.outGoingDegree = 0;
    }

    /**
     * Create a road node.
     *
     * @param id  Node ID
     * @param lon Longitude coordinate (x)
     * @param lat Latitude coordinate (y)
     */
    public RoadNode(String id, double lon, double lat, String timeStamp, int inComingDegree, int outGoingDegree) {
        super(id, timeStamp);
        this.lon = lon;
        this.lat = lat;
        this.inComingDegree = inComingDegree;
        this.outGoingDegree = outGoingDegree;
    }

    /**
     * Create a road node.
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
        this.inComingDegree = 0;
        this.outGoingDegree = 0;
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
        return new RoadNode(getId(), lon, lat, getTimeStamp());
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

    public void increaseInComingDegree() {
        this.inComingDegree++;
    }

    public void increaseOutGoingDegree() {
        this.outGoingDegree++;
    }

    public int getInComingDegree() {
        return inComingDegree;
    }

    public int getOutGoingDegree() {
        return outGoingDegree;
    }

}
