package util.object.roadnetwork;

import org.apache.log4j.Logger;
import util.function.DistanceFunction;
import util.object.spatialobject.Rect;

import java.io.Serializable;
import java.util.*;

/**
 * A Road Network Graph object, based on OpenStreetMap (OSM) data model.
 *
 * @author uqdalves, Hellisk
 */
public class RoadNetworkGraph implements Serializable {
	
	private static final Logger LOG = Logger.getLogger(RoadNetworkGraph.class);
	private final DistanceFunction distFunc;
	/**
	 * OSM primitives
	 */
	private List<RoadNode> nodeList = new ArrayList<>();
	private Set<String> nodeIDList = new HashSet<>();
	private List<RoadWay> wayList = new ArrayList<>();
	private Set<String> wayIDList = new HashSet<>();
	/**
	 * Map boundaries
	 */
	private boolean hasBoundary = false;
	private double minLat = Double.NEGATIVE_INFINITY, minLon = Double.NEGATIVE_INFINITY;
	private double maxLat = Double.POSITIVE_INFINITY, maxLon = Double.POSITIVE_INFINITY;
	
	
	private int maxVisitCount = 0;
	
	/**
	 * The current map will be used for map update. The below variables are only useful for an updatable map.
	 */
	private boolean isUpdatable;
	
	/**
	 * The max absolute value of the road way ID, the id can be either positive or negative according to different directions.
	 */
	private long maxAbsWayID = 0;
	
	private long maxRoadNodeID = 0;
	
	private int maxMiniNodeID = 0;
	
	public RoadNetworkGraph(boolean updatable, DistanceFunction df) {
		this.isUpdatable = updatable;
		this.distFunc = df;
	}
	
	/**
	 * @return The list of nodes in this road network graph.
	 */
	public List<RoadNode> getNodes() {
		return nodeList;
	}
	
	/**
	 * Reset the map by firstly setting the road node list.
	 *
	 * @param newNodeList The road node list representing the intersections or road ends.
	 */
	public void setNodes(List<RoadNode> newNodeList) {
		if (!this.wayList.isEmpty() || !this.wayIDList.isEmpty())
			throw new IllegalCallerException("The setNodes() should not be called when there were road ways in the map.");
		this.clear();
		for (RoadNode n : newNodeList) {
			n.clearConnectedWays();
			if (!nodeIDList.contains(n.getID())) {
				n.clearConnectedWays();
				this.nodeList.add(n);
				this.nodeIDList.add(n.getID());
				if (isUpdatable) {
					if (Long.parseLong(n.getID()) > maxRoadNodeID)
						maxRoadNodeID = Long.parseLong(n.getID());
				}
			} else LOG.error("Insert node to network failed. Node already exist: " + n.getID());
		}
		this.addNodes(newNodeList);
	}
	
	/**
	 * Reset the road network graph.
	 */
	private void clear() {
		this.nodeList.clear();
		this.nodeIDList.clear();
		this.wayList.clear();
		this.wayIDList.clear();
		this.maxAbsWayID = 0;
		this.maxRoadNodeID = 0;
		this.maxMiniNodeID = 0;
		this.maxVisitCount = 0;
		this.hasBoundary = false;
		this.minLat = Double.NEGATIVE_INFINITY;
		this.minLon = Double.NEGATIVE_INFINITY;
		this.maxLat = Double.POSITIVE_INFINITY;
		this.maxLon = Double.POSITIVE_INFINITY;
	}
	
	/**
	 * Adds the given node to this road network graph.
	 *
	 * @param node The road node to add.
	 */
	public void addNode(RoadNode node) {
		if (node != null) {
			if (!nodeIDList.contains(node.getID())) {
				node.clearConnectedWays();
				nodeList.add(node);
				nodeIDList.add(node.getID());
				updateBoundary(node);
				if (isUpdatable) {
					if (Long.parseLong(node.getID()) > maxRoadNodeID)
						maxRoadNodeID = Long.parseLong(node.getID());
				}
			} else LOG.error("Insert node to network failed. Node already exist: " + node.getID());
		}
	}
	
	/**
	 * Add all the nodes in the list to this road network graph.
	 *
	 * @param nodes The list of road nodes to add.
	 */
	public void addNodes(List<RoadNode> nodes) {
		if (nodes == null) {
			throw new NullPointerException("List of road nodes to add must not be null.");
		}
		for (RoadNode node : nodes) {
			if (!nodeIDList.contains(node.getID())) {
				node.clearConnectedWays();
				nodeList.add(node);
				nodeIDList.add(node.getID());
				if (isUpdatable) {
					if (Long.parseLong(node.getID()) > maxRoadNodeID)
						maxRoadNodeID = Long.parseLong(node.getID());
				}
			} else LOG.error("Insert node to network failed. Node already exist: " + node.getID());
		}
		updateBoundary();
	}
	
	/**
	 * @return The list of Ways in this road network graph.
	 */
	public List<RoadWay> getWays() {
		return wayList;
	}
	
	/**
	 * Check if the boundary is preset.
	 *
	 * @return True if the boundary is preset
	 */
	public boolean hasBoundary() {
		return hasBoundary;
	}
	
	
	/**
	 * Get all nodes in the map, including intersections and mini nodes.
	 *
	 * @return both intersections and mini nodes
	 */
	public List<RoadNode> getAllTypeOfNodes() {
		List<RoadNode> pointList = new ArrayList<>(this.getNodes());
		for (RoadWay w : this.getWays()) {
			for (RoadNode n : w.getNodes())
				if (!this.nodeIDList.contains(n.getID()))
					pointList.add(n);
		}
		return pointList;
	}
	
	/**
	 * Adds the given Way to this road network graph. Make sure the endpoints of the road way should exist in the current node list
	 * unless it is a temp road.
	 *
	 * @param way The road way to add.
	 */
	public void addWay(RoadWay way) {
		if (way != null) {
			if (!wayIDList.contains(way.getID())) {
				if (!nodeIDList.contains(way.getFromNode().getID()) || !nodeIDList.contains(way.getToNode().getID()))
					LOG.warn("The endpoints of the inserted road way do not exist in the current map: " + way.getFromNode().getID() + ","
							+ way.getToNode().getID());
				wayList.add(way);
				wayIDList.add(way.getID());
				way.getFromNode().addOutGoingWay(way);
				way.getToNode().addInComingWay(way);
				if (this.maxVisitCount < way.getVisitCount())
					this.maxVisitCount = way.getVisitCount();
				for (RoadNode n : way.getNodes())
					updateBoundary(n);
				if (isUpdatable) {
					if (!way.getID().contains("temp_")) {
						maxAbsWayID = Math.abs(Long.parseLong(way.getID())) > maxAbsWayID ? Math.abs(Long.parseLong(way.getID())) :
								maxAbsWayID;
						for (RoadNode n : way.getNodes()) {
							maxMiniNodeID = Integer.parseInt(n.getID().substring(0, n.getID().length() - 1)) > maxMiniNodeID ?
									Integer.parseInt(n.getID().substring(0, n.getID().length() - 1)) : maxMiniNodeID;
						}
					} else    // temporary road way
						LOG.error("Temporary edges should not be included in the road map.");
				}
			} else
				throw new IllegalArgumentException("ERROR! Road way already exist: " + way.getID());
		}
	}
	
	/**
	 * Adds all the ways in the list to this road network graph.
	 *
	 * @param waysList The list of road ways to add.
	 */
	public void addWays(List<RoadWay> waysList) {
		if (waysList == null) {
			throw new NullPointerException("List of road ways to add must not be null.");
		}
		for (RoadWay way : waysList)
			addWay(way);
	}
	
	/**
	 * Set bounding box of the road network.
	 *
	 * @param minLon minimum longitude
	 * @param maxLon maximum longitude
	 * @param minLat minimum latitude
	 * @param maxLat maximum latitude
	 */
	public void setBoundary(double minLon, double maxLon, double minLat, double maxLat) {
		this.minLon = minLon;
		this.maxLon = maxLon;
		this.minLat = minLat;
		this.maxLat = maxLat;
		this.hasBoundary = true;
	}
	
	/**
	 * Reset the boundary to better represent the size.
	 */
	public void updateBoundary() {
		this.setMaxLon(Double.POSITIVE_INFINITY);
		this.setMaxLat(Double.POSITIVE_INFINITY);
		this.setMinLon(Double.NEGATIVE_INFINITY);
		this.setMinLat(Double.NEGATIVE_INFINITY);
		this.hasBoundary = false;
		for (RoadNode n : this.getAllTypeOfNodes())
			updateBoundary(n);
	}
	
	private void updateBoundary(RoadNode node) {
		// update the map boarder
		if (this.maxLon < node.lon()) {
			this.maxLon = node.lon();
		}
		if (this.minLon > node.lon()) {
			this.minLon = node.lon();
		}
		if (this.maxLat < node.lat()) {
			this.maxLat = node.lat();
		}
		if (this.minLat > node.lat()) {
			this.minLat = node.lat();
		}
		this.hasBoundary = true;
	}
	
	/**
	 * @return The minimum latitude value of this road map's boundary.
	 */
	public double getMinLat() {
		return minLat;
	}
	
	/**
	 * Set the minimum latitude value of this road map's boundary.
	 */
	public void setMinLat(double minLat) {
		this.minLat = minLat;
		this.hasBoundary = true;
	}
	
	/**
	 * @return The minimum longitude value of this road map's boundary.
	 */
	public double getMinLon() {
		return minLon;
	}
	
	/**
	 * Set the minimum longitude value of this road map's boundary.
	 */
	public void setMinLon(double minLon) {
		this.minLon = minLon;
		this.hasBoundary = true;
	}
	
	/**
	 * @return The maximum latitude value of this road map's boundary.
	 */
	public double getMaxLat() {
		return maxLat;
	}
	
	/**
	 * Set he maximum latitude value of this road map's boundary.
	 */
	public void setMaxLat(double maxLat) {
		this.maxLat = maxLat;
		this.hasBoundary = true;
	}
	
	/**
	 * @return The maximum longitude value of this road map's boundary.
	 */
	public double getMaxLon() {
		return maxLon;
	}
	
	/**
	 * Set the maximum longitude value of this road map's boundary.
	 */
	public void setMaxLon(double maxLon) {
		this.maxLon = maxLon;
		this.hasBoundary = true;
	}
	
	public Rect getBoundary() {
		if (hasBoundary)
			return new Rect(minLon, minLat, maxLon, maxLat, distFunc);
		else {
			LOG.warn("The current map does not have boundary.");
			return new Rect(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, distFunc);
		}
	}
	
	public DistanceFunction getDistanceFunction() {
		return distFunc;
	}
	
	/**
	 * Check whether this road network graph is empty.
	 *
	 * @return Returns true if this road network graph has no nodes.
	 */
	public boolean isEmpty() {
		return nodeList == null || nodeList.isEmpty();
	}
	
	public void removeRoadWayList(Set<RoadWay> roadWayList) {
		List<RoadWay> removedWayList = new ArrayList<>();
		for (RoadWay way : roadWayList) {
			if (wayIDList.contains(way.getID())) {
				wayIDList.remove(way.getID());
				way.getFromNode().removeInComingWayFromList(way);
				way.getToNode().removeOutGoingWayFromList(way);
			} else
				LOG.error("The road to be removed is not in the map: " + way.getID());
			removedWayList.add(way);
		}
		this.wayList.removeAll(removedWayList);
	}
	
	public int isolatedNodeRemoval() {
		int nodeSize = this.nodeList.size();
		for (Iterator<RoadNode> iterator = this.nodeList.iterator(); iterator.hasNext(); ) {
			RoadNode n = iterator.next();
			if (n.getDegree() == 0) {
				LOG.debug("Removed node ID: " + n.getID());
				iterator.remove();
				this.nodeIDList.remove(n.getID());
			}
		}
		return nodeSize - this.nodeList.size();
	}
	
	public int getMaxVisitCount() {
		return maxVisitCount;
	}
	
	public void setMaxVisitCount(int maxVisitCount) {
		this.maxVisitCount = maxVisitCount;
	}
	
	public boolean containsWay(String id) {
		return this.wayIDList.contains(id);
	}
	
	public boolean containsNode(String id) {
		return this.nodeIDList.contains(id);
	}
	
	public void updateMaxVisitCount(int visitCount) {
		if (this.maxVisitCount < visitCount)
			this.maxVisitCount = visitCount;
	}
	
	public int getMaxMiniNodeID() {
		return maxMiniNodeID;
	}
	
	public long getMaxAbsWayID() {
		return maxAbsWayID;
	}
	
	public long getMaxRoadNodeID() {
		return maxRoadNodeID;
	}
	
	public void setUpdatable(boolean updatable) {
		if (!this.isUpdatable && updatable) {        // enable the map update, update the current value of the IDs
			for (RoadNode n : nodeList) {
				if (Long.parseLong(n.getID()) > maxRoadNodeID)
					maxRoadNodeID = Long.parseLong(n.getID());
			}
			for (RoadWay w : wayList) {
				maxAbsWayID = Math.abs(Long.parseLong(w.getID())) > maxAbsWayID ? Math.abs(Long.parseLong(w.getID())) : maxAbsWayID;
				for (RoadNode n : w.getNodes()) {
					maxMiniNodeID = Integer.parseInt(n.getID().substring(0, n.getID().length() - 1)) > maxMiniNodeID ?
							Integer.parseInt(n.getID().substring(0, n.getID().length() - 1)) : maxMiniNodeID;
				}
			}
		} else if (this.isUpdatable && !updatable) {    // disable the map update
			maxRoadNodeID = 0;
			maxAbsWayID = 0;
			maxMiniNodeID = 0;
		}
	}
	
	@Override
	public RoadNetworkGraph clone() throws CloneNotSupportedException {
		RoadNetworkGraph clone = new RoadNetworkGraph(isUpdatable, distFunc);
		for (RoadNode n : this.getNodes()) {
			clone.addNode(n.clone());
		}
		for (RoadWay w : this.getWays()) {
			clone.addWay(w.clone());
		}
		if (clone.getMaxLon() != this.getMaxLon() || clone.getMinLon() != this.getMinLon() || clone.getMaxLat() != this.getMaxLat()
				|| clone.getMinLat() != this.getMinLat())
			LOG.warn("Clone result has different boundary as the original object.");
		if (clone.maxAbsWayID != this.maxAbsWayID || clone.maxMiniNodeID != this.maxMiniNodeID || clone.maxRoadNodeID != this.maxRoadNodeID)
			LOG.warn("Clone result has different count of roads.");
		return clone;
	}
}