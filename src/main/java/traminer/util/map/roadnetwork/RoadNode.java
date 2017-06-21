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
     * Creates and empty road node with coordinates (0,0)
     */
    public RoadNode() {
        this.lon = 0.0;
        this.lat = 0.0;
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
}
