package util.object.roadnetwork;

import org.apache.log4j.Logger;
import util.function.DistanceFunction;
import util.object.spatialobject.Point;

import java.text.DecimalFormat;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A node in the road network graph (OSM Vertex).
 * <br> The spatial coordinates of the node are immutable.
 *
 * @author uqdalves, Hellisk
 */
public final class RoadNode extends RoadNetworkPrimitive {
	
	private static final Logger LOG = Logger.getLogger(RoadNode.class);
	
	/**
	 * Longitude coordinate (x)
	 */
	private double lon; // x
	
	/**
	 * Latitude coordinate (y)
	 */
	private double lat; // y
	
	/**
	 * In-coming and out-going way lists. An intersection should have non-empty way lists, whereas the mini nodes have empty lists.
	 */
	private Set<RoadWay> inComingWayList = new LinkedHashSet<>();
	private Set<RoadWay> outGoingWayList = new LinkedHashSet<>();
	
	/**
	 * Create and empty road node with coordinates (0,0)
	 */
	public RoadNode(DistanceFunction df) {
		super(df);
		this.lon = 0.0;
		this.lat = 0.0;
	}
	
	/**
	 * Create a road node.
	 *
	 * @param id  Vertex ID
	 * @param lon Longitude coordinate (x)
	 * @param lat Latitude coordinate (y)
	 */
	public RoadNode(String id, double lon, double lat, DistanceFunction df) {
		super(id, df);
		this.lon = lon;
		this.lat = lat;
	}
	
	/**
	 * Create a road node.
	 *
	 * @param id       Vertex ID
	 * @param lon      Longitude coordinate (x)
	 * @param lat      Latitude coordinate (y)
	 * @param nodeType Type of the node (intersection/non-intersection)
	 */
	public RoadNode(String id, double lon, double lat, short nodeType, DistanceFunction df) {
		super(id, df);
		this.lon = lon;
		this.lat = lat;
		this.setNodeType(nodeType);
	}
	
	/**
	 * Create a road node.
	 *
	 * @param id              Vertex ID.
	 * @param lon             Longitude coordinate (x).
	 * @param lat             Latitude coordinate (y).
	 * @param inComingWayList List of incoming road ways.
	 * @param outGoingWayList List of outgoing road ways.
	 */
	public RoadNode(String id, double lon, double lat, Set<RoadWay> inComingWayList, Set<RoadWay> outGoingWayList, short nodeType,
					DistanceFunction df) {
		super(id, df);
		this.lon = lon;
		this.lat = lat;
		this.inComingWayList.addAll(inComingWayList);
		this.outGoingWayList.addAll(outGoingWayList);
		this.setNodeType(nodeType);
	}
	
	public static RoadNode parseRoadNode(String s, DistanceFunction df) {
		String[] nodeInfo = s.split(",");
		if (nodeInfo.length != 4) throw new IndexOutOfBoundsException("Failed to read road node: input data format is wrong. " + s);
		return new RoadNode(nodeInfo[0], Double.parseDouble(nodeInfo[1]), Double.parseDouble(nodeInfo[2]), Short.parseShort(nodeInfo[3]),
				df);
	}
	
	/**
	 * Vertex longitude value.
	 */
	public double lon() {
		return lon;
	}
	
	/**
	 * Vertex latitude value.
	 */
	public double lat() {
		return lat;
	}
	
	public void setLocation(double lon, double lat) {
		this.lon = lon;
		this.lat = lat;
	}
	
	public Set<RoadWay> getInComingWayList() {
		return inComingWayList;
	}
	
	public Set<RoadWay> getOutGoingWayList() {
		return outGoingWayList;
	}
	
	/**
	 * @return Return the spatial point object representation
	 * of this road node.
	 */
	public Point toPoint() {
		Point p = new Point(lon, lat, getDistanceFunction());
		p.setID(getID());
		return p;
	}
	
	@Override
	public String toString() {
		DecimalFormat df = new DecimalFormat("0.00000");
		return getID() + "," + df.format(lon()) + "," + df.format(lat()) + "," + getNodeType();
	}
	
	/**
	 * Makes an exact copy of this object
	 */
	@Override
	public RoadNode clone() {
		return new RoadNode(getID(), lon, lat, inComingWayList, outGoingWayList, getNodeType(), getDistanceFunction());
	}
	
	@Override
	public void print() {
		LOG.info("ROAD NODE ( " + toString() + " )");
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
		return getInComingDegree() + getOutGoingDegree();
	}
	
	public void clearConnectedWays() {
		this.inComingWayList.clear();
		this.outGoingWayList.clear();
	}
	
	public void addInComingWay(RoadWay way) {
		this.inComingWayList.add(way);
	}
	
	public void addOutGoingWay(RoadWay way) {
		this.outGoingWayList.add(way);
	}
	
	public void setInComingWayList(Set<RoadWay> inComingWayList) {
		this.inComingWayList = inComingWayList;
	}
	
	public void setOutGoingWayList(Set<RoadWay> outGoingWayList) {
		this.outGoingWayList = outGoingWayList;
	}
	
	public void removeInComingWayFromList(RoadWay way) {
		this.inComingWayList.removeIf(w -> w.getID().equals(way.getID()));
	}
	
	public void removeOutGoingWayFromList(RoadWay way) {
		this.outGoingWayList.removeIf(w -> w.getID().equals(way.getID()));
	}
	
	public int getInComingDegree() {
		return inComingWayList.size();
	}
	
	public int getOutGoingDegree() {
		return outGoingWayList.size();
	}
	
	
	// TODO -2 should be changed.
	
	/**
	 * Road node type, 0 = non-intersection node, 1 = sub-node of the intersection, 2 = single-node intersection, 3 = main node of the
	 * intersection, 4 = mini node, -1 = unknown, -2 = null value
	 */
	public short getNodeType() {
		if (getTags().get("nodeType") == null)
			return -2;
		return (short) getTags().get("nodeType");
	}
	
	public void setNodeType(short nodeType) {
		if (nodeType != -2)
			addTag("nodeType", nodeType);
	}
	
}
