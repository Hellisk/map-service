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
    private double minLat, minLon;
    private double maxLat, maxLon;


    private boolean hasBoundary = false;

    /**
     * @return The list of Nodes in this road network graph.
     */
    public List<RoadNode> getNodes() {
        return nodesList;
    }

    /**
     * Adds the given Node to this road network graph.
     *
     * @param node The road node to add.
     */
    public void addNode(RoadNode node) {
        if (node != null) {
            if (!nodeIDList.contains(node.getId())) {
                nodesList.add(node);
                nodeIDList.add(node.getId());
            } else System.out.println("Node already exist: " + node.getId());
        }
    }

    /**
     * Adds all the nodes in the list to this road network graph.
     *
     * @param nodes The list of road nodes to add.
     */
    public void addNodes(List<RoadNode> nodes) {
        if (nodes == null) {
            throw new NullPointerException(
                    "List of road nodes to add must not be null.");
        } else {
            for (RoadNode node : nodes) {
                this.addNode(node);
            }
        }
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
     * @return
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
            pointList.addAll(w.getNodes());
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
            if (!wayIDList.contains(way.getId())) {
                waysList.add(way);
                wayIDList.add(way.getId());
                way.getFromNode().increaseOutGoingDegree();
                way.getToNode().increaseInComingDegree();
            } else System.out.println("Road way already exist: " + way.getId());
        }
    }

    /**
     * Adds all the ways in the list to this road network graph.
     *
     * @param waysList The list of road ways to add.
     */
    public void addWays(List<RoadWay> waysList) {
        if (waysList == null) {
            throw new NullPointerException(
                    "List of road ways to add must not be null.");
        }
        for (RoadWay way : waysList) {
            if (!wayIDList.contains(way.getId()))
                addWay(way);
        }
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
}
