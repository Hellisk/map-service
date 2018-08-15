package edu.uq.dke.mapupdate.util.object.roadnetwork;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A Road Network Graph object, based on OpenStreetMap (OSM) data model.
 *
 * @author uqdalves, uqpchao
 */
@SuppressWarnings("serial")
public class RoadNetworkGraph implements MapInterface {
    /**
     * OSM primitives
     */
    private List<RoadNode> nodesList = new ArrayList<>();
    private Set<String> nodeIDList = new HashSet<>();
    private List<RoadWay> waysList = new ArrayList<>();
    private Set<String> wayIDList = new HashSet<>();

    /**
     * Map boundaries
     */
    private double minLat = Double.NEGATIVE_INFINITY, minLon = Double.NEGATIVE_INFINITY;
    private double maxLat = Double.POSITIVE_INFINITY, maxLon = Double.POSITIVE_INFINITY;

    private boolean hasBoundary = false;

    private int maxVisitCount = 0;

    /**
     * The global dataset doesn't have problem regarding road way id, so the following variables are unused.
     */
    private boolean isBeijingDataset = true;
    /**
     * The max absolute value of the node ID
     */
    private long maxAbsWayID = 0;

    private long maxRoadNodeID = 0;

    private int maxMiniNodeID = 0;

    /**
     * @return The list of Nodes in this road network graph.
     */
    public List<RoadNode> getNodes() {
        return nodesList;
    }

    public void setNodes(List<RoadNode> nodesList) {
        for (RoadNode n : nodesList) {
            n.clearDegree();
            this.addNode(n);
        }
    }

    /**
     * Adds the given node to this road network graph.
     *
     * @param node The road node to add.
     */
    public void addNode(RoadNode node) {
        if (node != null) {
            if (!nodeIDList.contains(node.getID())) {
                nodesList.add(node);
                nodeIDList.add(node.getID());
                updateBoundingBox(node);
                if (isBeijingDataset)
                    maxRoadNodeID = Long.parseLong(node.getID()) > maxRoadNodeID ? Long.parseLong(node.getID()) : maxRoadNodeID;
            } else System.out.println("ERROR! Node already exist: " + node.getID());
        }
    }

    /**
     * Adds all the nodes in the list to this road network graph.
     *
     * @param nodes The list of road nodes to add.
     */
    public void addNodes(List<RoadNode> nodes) {
        if (nodes == null) {
            throw new NullPointerException("ERROR! List of road nodes to add must not be null.");
        } else {
            for (RoadNode node : nodes) {
                updateBoundingBox(node);
                this.addNode(node);
            }
        }
    }

    private void updateBoundingBox(RoadNode node) {
        // update the map boarder
        if (this.maxLon == Double.POSITIVE_INFINITY || this.maxLon < node.lon()) {
            this.maxLon = node.lon();
        }
        if (this.minLon == Double.NEGATIVE_INFINITY || this.minLon > node.lon()) {
            this.minLon = node.lon();
        }
        if (this.maxLat == Double.POSITIVE_INFINITY || this.maxLat < node.lat()) {
            this.maxLat = node.lat();
        }
        if (this.minLat == Double.NEGATIVE_INFINITY || this.minLat > node.lat()) {
            this.minLat = node.lat();
        }
        this.hasBoundary = true;
    }

    /**
     * @return The list of Ways in this road network graph.
     */
    public List<RoadWay> getWays() {
        return waysList;
    }

    /**
     * check if the boundary is preset
     *
     * @return True if the boundary is preset
     */
    public boolean hasBoundary() {
        return hasBoundary;
    }

    /**
     * @return both intersections and mini nodes
     */
    public List<RoadNode> getAllNodes() {
        List<RoadNode> pointList = new ArrayList<>(this.getNodes());
        for (RoadWay w : this.getWays()) {
            for (RoadNode n : w.getNodes())
                if (!this.nodeIDList.contains(n.getID()))
                    pointList.add(n);
        }
        return pointList;
    }

    /**
     * Adds the given Way to this road network graph.
     *
     * @param way The road way to add.
     */
    public void addWay(RoadWay way) {
        if (way != null) {
            if (!wayIDList.contains(way.getID())) {
                waysList.add(way);
                wayIDList.add(way.getID());
                way.getFromNode().increaseOutGoingDegree();
                way.getToNode().increaseInComingDegree();
                if (this.maxVisitCount < way.getVisitCount())
                    this.maxVisitCount = way.getVisitCount();
                for (RoadNode n : way.getNodes())
                    updateBoundingBox(n);
                if (isBeijingDataset) {
                    if (!way.getID().contains("temp_")) {
                        maxAbsWayID = Math.abs(Long.parseLong(way.getID())) > maxAbsWayID ? Math.abs(Long.parseLong(way.getID())) :
                                maxAbsWayID;
                        for (RoadNode n : way.getNodes()) {
                            maxMiniNodeID = Integer.parseInt(n.getID().substring(0, n.getID().length() - 1)) > maxMiniNodeID ? Integer.parseInt(n
                                    .getID().substring(0, n.getID().length() - 1)) : maxMiniNodeID;
                        }
                    } else
                        System.out.println("ERROR! Temporary edges should not be included in the road map.");
                }
            }
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
    public void setBoundingBox(double minLon, double maxLon, double minLat, double maxLat) {
        this.minLon = minLon;
        this.maxLon = maxLon;
        this.minLat = minLat;
        this.maxLat = maxLat;
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

    /**
     * Check whether this road network graph is empty.
     *
     * @return Returns true if this road network graph
     * has no nodes.
     */
    public boolean isEmpty() {
        return nodesList == null || nodesList.isEmpty();
    }

    public int isolatedNodeRemoval() {
        List<RoadNode> removedRoadNodeList = new ArrayList<>();
        for (RoadNode n : this.nodesList) {
            if (n.getDegree() == 0) {
                removedRoadNodeList.add(n);
                this.nodeIDList.remove(n.getID());
            }
        }
        this.nodesList.removeAll(removedRoadNodeList);
        return removedRoadNodeList.size();
    }

    public int getMaxVisitCount() {
        return maxVisitCount;
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

    public void setBeijingDataset(boolean beijingDataset) {
        isBeijingDataset = beijingDataset;
    }

    public void setMaxVisitCount(int maxVisitCount) {
        this.maxVisitCount = maxVisitCount;
    }
}
