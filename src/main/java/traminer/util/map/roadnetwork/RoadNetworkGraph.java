package traminer.util.map.roadnetwork;

import org.graphstream.graph.Graph;
import org.graphstream.graph.implementations.SingleGraph;
import traminer.util.map.MapInterface;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A Road Network Graph object, based on OpenStreetMap (OSM) data model.
 * 
 * @author uqdalves
 */
@SuppressWarnings("serial")
public class RoadNetworkGraph implements MapInterface {
    /**
     * OSM primitives
     */
    private Map<String, RoadNode> nodesList =
            new HashMap<>();
    private Map<String, RoadWay> waysList =
            new HashMap<>();
    private Map<String, RoadRelation> relationsList =
            new HashMap<>();
    /**
     * Map boundaries
     */
    private double minLat, minLon;
    private double maxLat, maxLon;

    /**
     * @return The list of Nodes in this road network graph.
     */
    public Collection<RoadNode> getNodes() {
        return nodesList.values();
    }

    /**
     * @param nodeId Node Id to search.
     * @return Return the road node with the given Id.
     */
    public RoadNode getNodeById(String nodeId) {
        return nodesList.get(nodeId);
    }

    /**
     * Adds the given Node to this road network graph.
     *
     * @param node The road node to add.
     */
    public void addNode(RoadNode node) {
        if (node != null) nodesList.put(node.getId(), node);
    }

    /**
     * Adds all the nodes in the list to this road network graph.
     *
     * @param nodesList The list of road nodes to add.
     */
    public void addNodes(List<RoadNode> nodesList) {
        if (nodesList == null) {
            throw new NullPointerException(
                    "List of road nodes to add must not be null.");
        }
        for (RoadNode node : nodesList) {
            addNode(node);
        }
    }

    /**
     * @return The list of Ways in this road network graph.
     */
    public Collection<RoadWay> getWays() {
        return waysList.values();
    }

    /**
     * @param wayId Way Id to search.
     * @return Return the road way with the given Id.
     */
    public RoadWay getWayById(String wayId) {
        return waysList.get(wayId);
    }

    /**
     * Adds the given Way to this road network graph.
     *
     * @param way The road way to add.
     */
    public void addWay(RoadWay way) {
        if (way != null) waysList.put(way.getId(), way);
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
            addWay(way);
        }
    }

    /**
     * @return The list of Relations in this road network graph.
     */
    public Collection<RoadRelation> getRelations() {
        return relationsList.values();
    }

    /**
     * @param relationId Relation Id to search.
     * @return Return the road relation with the given Id.
     */
    public RoadRelation getRelationById(String relationId) {
        return relationsList.get(relationId);
    }

    /**
     * Adds the given Relation to this road network graph.
     *
     * @param relation The road relation to add.
     */
    public void addRelation(RoadRelation relation) {
        if (relation != null) relationsList.put(relation.getId(), relation);
    }

    /**
     * Adds all the relations in the list to this road network graph.
     *
     * @param relationsList The list of road relations to add.
     */
    public void addRelations(List<RoadRelation> relationsList) {
        if (relationsList == null) {
            throw new NullPointerException(
                    "List of road ways to add must not be null.");
        }
        for (RoadRelation r : relationsList) {
            addRelation(r);
        }
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
        if (nodesList == null) {
            return true;
        }
        return nodesList.isEmpty();
    }

    /**
     * Convert this road network graph to a Graph object.
     *
     * @return Returns a graph representation of this road network.
     */
    public Graph toGraph() {
        Graph graph = new SingleGraph("RoadNetworkGraph");

        // create one graph node per road network node.
        for (RoadNode node : getNodes()) {
            graph.addNode(node.getId())
                    .setAttribute("xy", node.lon(), node.lat());
        }
        // create one graph edge for every edge in the road ways
        for (RoadWay way : getWays()) {
            for (int i = 0; i < way.size() - 1; i++) {
                RoadNode nodei = way.getNodes().get(i);
                RoadNode nodej = way.getNodes().get(i + 1);
                graph.addEdge(way.getId() + "_E" + i, nodei.getId(), nodej.getId());
            }
        }

        return graph;
    }

    /**
     * Displays this road network graph in a GUI window.
     */
    public void display() {
        if (this.isEmpty()) return;
        toGraph().display(false);
    }
}
