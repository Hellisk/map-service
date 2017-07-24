package traminer.util.map.roadnetwork;

import org.graphstream.graph.Graph;
import org.graphstream.graph.implementations.SingleGraph;
import traminer.util.spatial.distance.GPSDistanceFunction;
import traminer.util.spatial.distance.PointDistanceFunction;
import traminer.util.spatial.objects.LineString;
import traminer.util.spatial.objects.Segment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * A way in the road network graph (OSM Way).
 *
 * @author uqdalves
 */
@SuppressWarnings("serial")
public class RoadWay extends RoadNetworkPrimitive {
    /**
     * The list of nodes in this road way
     */
    private List<RoadNode> nodeList = new ArrayList<>(1);

    /**
     * Total distance of the road way
     */
    private double distance;

    private PointDistanceFunction distFunc;

    /**
     * Creates a new empty road way.
     */
    public RoadWay() {
        this.distFunc = new GPSDistanceFunction();
    }

    /**
     * Creates a new empty road way.
     *
     * @param wayId The road way identifier.
     */
    public RoadWay(String wayId) {
        super(wayId);
        this.distFunc = new GPSDistanceFunction();
    }

    /**
     * Creates a new empty road way.
     *
     * @param wayId     The road way identifier.
     * @param timeStamp The road way time-stamp.
     */
    public RoadWay(String wayId, String timeStamp) {
        super(wayId, timeStamp);
        this.distFunc = new GPSDistanceFunction();
    }

    /**
     * Creates a empty road way from the given nodes.
     *
     * @param wayId    The road way identifier.
     * @param nodeList A sorted list of way nodes.
     */
    public RoadWay(String wayId, Collection<RoadNode> nodeList) {
        super(wayId);
        if (nodeList == null) {
            throw new NullPointerException("Road nodes list cannot be null.");
        }
        this.nodeList = new ArrayList<>(nodeList);
        this.distFunc = new GPSDistanceFunction();
        for (int i = 1; i < this.nodeList.size(); i++) {
            this.distance += distFunc.distance(this.nodeList.get(i - 1).toPoint(), this.nodeList.get(i).toPoint());
        }
    }

    /**
     * Creates a empty road way from the given nodes.
     *
     * @param wayId The road way identifier.
     * @param nodes A sorted list of way nodes.
     */
    public RoadWay(String wayId, RoadNode... nodes) {
        super(wayId);
        if (nodes == null) {
            throw new NullPointerException("Road nodes list cannot be null.");
        }
        this.nodeList = new ArrayList<>(nodes.length);
        for (RoadNode node : nodes) {
            if (node != null) this.nodeList.add(node);
        }
        this.distFunc = new GPSDistanceFunction();
        for (int i = 1; i < this.nodeList.size(); i++) {
            this.distance += distFunc.distance(this.nodeList.get(i - 1).toPoint(), this.nodeList.get(i).toPoint());
        }

    }

    /**
     * Creates a new empty road way.
     */
    public RoadWay(PointDistanceFunction distFunc) {
        this.distFunc = distFunc;
    }

    /**
     * Creates a new empty road way.
     *
     * @param wayId The road way identifier.
     */
    public RoadWay(String wayId, PointDistanceFunction distFunc) {
        super(wayId);
        this.distFunc = distFunc;
    }

    /**
     * Creates a new empty road way.
     *
     * @param wayId     The road way identifier.
     * @param timeStamp The road way time-stamp.
     */
    public RoadWay(String wayId, String timeStamp, PointDistanceFunction distFunc) {
        super(wayId, timeStamp);
        this.distFunc = distFunc;
    }

    /**
     * Creates a empty road way from the given nodes.
     *
     * @param wayId    The road way identifier.
     * @param nodeList A sorted list of way nodes.
     */
    public RoadWay(String wayId, Collection<RoadNode> nodeList, PointDistanceFunction distFunc) {
        super(wayId);
        if (nodeList == null) {
            throw new NullPointerException("Road nodes list cannot be null.");
        }
        this.nodeList = new ArrayList<>(nodeList);
        this.distFunc = distFunc;
        for (int i = 1; i < this.nodeList.size(); i++) {
            this.distance += distFunc.distance(this.nodeList.get(i - 1).toPoint(), this.nodeList.get(i).toPoint());
        }
    }

    public RoadWay(String wayId, PointDistanceFunction distFunc, RoadNode... nodes) {
        super(wayId);
        if (nodes == null) {
            throw new NullPointerException("Road nodes list cannot be null.");
        }
        this.nodeList = new ArrayList<>(nodes.length);
        for (int i = 0; i < nodes.length; i++) {
            RoadNode node = nodes[i];
            if (i == 0 || i == nodes.length - 1) {
                node.setToNonMiniNode();
            }
            if (node != null) this.nodeList.add(node);
        }
        this.distFunc = distFunc;
        for (int i = 1; i < this.nodeList.size(); i++) {
            this.distance += distFunc.distance(this.nodeList.get(i - 1).toPoint(), this.nodeList.get(i).toPoint());
        }

    }

    /**
     * @return The list of nodes in this road way.
     */
    public List<RoadNode> getNodes() {
        return nodeList;
    }

    public void setNodes(List<RoadNode> nodes) {
        this.nodeList.clear();
        nodeList.addAll(nodes);
        for (int i = 1; i < this.nodeList.size(); i++) {
            this.distance += distFunc.distance(this.nodeList.get(i - 1).toPoint(), this.nodeList.get(i).toPoint());
        }

    }

    /**
     * Get the road node in the given index position
     * from this road way.
     *
     * @param index The road node position in the road way.
     * @return The road node in the specified position.
     */
    public RoadNode getNode(int index) {
        if (index < 0 || index >= size()) {
            throw new IndexOutOfBoundsException("Node index out of bounds. "
                    + "Index should be greater than or equals to 0, or less than " + size());
        }
        return nodeList.get(index);
    }

    /**
     * Adds a node to this road way.
     * The node is connected to the current end-point node of this road way,
     * creating a new edge.
     *
     * @param node The road node to add.
     */
    public void addNode(RoadNode node) {
        if (node != null) {
            if (this.nodeList.size() > 1) {
                this.nodeList.get(this.nodeList.size() - 1).setToMiniNode();
                node.setToNonMiniNode();
                this.distance += distFunc.distance(this.nodeList.get(this.nodeList.size() - 1).toPoint(), node.toPoint());
            } else if (this.nodeList.size() == 1) {
                this.distance += distFunc.distance(this.nodeList.get(this.nodeList.size() - 1).toPoint(), node.toPoint());
            }
            this.nodeList.add(node);
        }
    }

    /**
     * Adds all nodes in the list to this road way.
     * The nodes are connected sequentially to the end of this road way,
     * creating an new edge between every n and n+1 node.
     *
     * @param nodeList A sorted list of road node to add.
     */
    public void addNodes(Collection<RoadNode> nodeList) {
        if (!nodeList.isEmpty()) {
            int size = this.nodeList.size();
            if (this.nodeList.size() > 1) {
                this.nodeList.get(size - 1).setToMiniNode();
                this.nodeList.addAll(nodeList);
                this.nodeList.get(size).setToMiniNode();
            } else if (this.nodeList.size() == 1) {
                this.nodeList.addAll(nodeList);
                this.nodeList.get(size).setToMiniNode();
            } else {
                this.nodeList.addAll(nodeList);
            }
            // recalculate the distance
            this.distance = 0;
            for (int i = 1; i < this.nodeList.size(); i++) {
                this.distance += distFunc.distance(this.nodeList.get(i - 1).toPoint(), this.nodeList.get(i).toPoint());
            }
        }
    }

    /**
     * Adds the given way to the end of this way (merge ways)
     *
     * @param way The road way to add.
     * @return Return This road way after the merge.
     */

    public RoadWay addWay(RoadWay way) {
        if (this.nodeList.size() > 1) {
            way.getNode(0).setToMiniNode();
            this.nodeList.get(this.nodeList.size() - 1).setToMiniNode();
            this.distance += distFunc.distance(this.nodeList.get(this.nodeList.size() - 1).toPoint(), way.getNode(0).toPoint());
        } else if (this.nodeList.size() == 1) {
            way.getNode(0).setToMiniNode();
            this.distance += distFunc.distance(this.nodeList.get(this.nodeList.size() - 1).toPoint(), way.getNode(0).toPoint());
        }
        for (int i = 1; i < way.size(); i++) {
            this.distance += distFunc.distance(way.getNode(i - 1).toPoint(), way.getNode(i).toPoint());
        }
        this.nodeList.addAll(way.nodeList);
        return this;
    }

    /**
     * The size of this road way.
     *
     * @return The number of nodes in this road way.
     */
    public int size() {
        return nodeList.size();
    }

    /**
     * @return True if there is no node in this road way.
     */
    public boolean isEmpty() {
        return nodeList.isEmpty();
    }

    /**
     * Convert this road way to a list of spatial segments.
     *
     * @return A sorted list of way segments.
     */
    public List<Segment> getEdges() {
        List<Segment> sList = new ArrayList<>(size() - 1);
        RoadNode node1, node2;
        for (int i = 0; i < nodeList.size() - 1; i++) {
            node1 = nodeList.get(i);
            node2 = nodeList.get(i + 1);
            Segment currSegment = new Segment(node1.lon(), node1.lat(), node2.lon(), node2.lat());
            currSegment.setId(this.getId());
            sList.add(currSegment);
        }
        return sList;
    }

    /**
     * @return The LineString spatial object representation of this road way.
     */
    public LineString toLineString() {
        LineString ls = new LineString();
        ls.setId(this.getId());
        ls.addAll(getEdges());
        return ls;
    }

    /**
     * Sorts the nodes in this way by time-stamp.
     */
    public void sortByTimeStamp() {
        nodeList.sort(TIME_COMPARATOR);
    }

    /**
     * Makes an exact copy of this object
     */
    @Override
    public RoadWay clone() {
        RoadWay clone = new RoadWay(getId(), getTimeStamp());
        for (RoadNode node : nodeList) {
            clone.addNode(node.clone());
        }
        return clone;
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        for (RoadNode node : nodeList) {
            if (node != null) {
                s.append(", ").append(node.toString());
            }
        }
        s = new StringBuilder(s.toString().replaceFirst(",", ""));
        s = new StringBuilder(getId() + " (" + s + " )");
        return s.toString();
    }

    @Override
    public void print() {
        System.out.println("ROAD WAY ( " + toString() + " )");
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof RoadWay))
            return false;
        RoadWay other = (RoadWay) obj;
        return this.size() == other.size() && this.getId().equals(other.getId());
    }

    /**
     * Displays this Road Way in a GUI window.
     */
    public void display() {
        if (size() < 2) return;

        Graph graph = new SingleGraph("RoadWay");
        graph.display(false);
        // create one graph edge for every edge in the road way
        for (int i = 0; i < nodeList.size() - 1; i++) {
            RoadNode nodei = nodeList.get(i);
            RoadNode nodej = nodeList.get(i + 1);
            graph.addNode(nodei.getId()).setAttribute("xy", nodei.lon(), nodei.lat());
            graph.addNode(nodej.getId()).setAttribute("xy", nodej.lon(), nodej.lat());
            graph.addEdge(getId() + "_E" + i, nodei.getId(), nodej.getId());
        }
    }

    /**
     * A comparator to compare nodes by time-stamp value.
     * <p>
     * <br> Note: time-stamp values should be able to be parsed to long numbers.
     */
    private static final Comparator<RoadNode> TIME_COMPARATOR =
            new Comparator<RoadNode>() {
                @Override
                public int compare(RoadNode o1, RoadNode o2) {
                    try {
                        long t1 = Long.parseLong(o1.getTimeStamp());
                        long t2 = Long.parseLong(o2.getTimeStamp());
                        if (t1 < t2) return -1;
                        if (t1 > t2) return 1;
                    } catch (NumberFormatException e) {
                        return 0;
                    }
                    return 0;
                }
            };

    public double getDistance() {
        return distance;
    }

    public void setDistanceFunction(PointDistanceFunction distFunc) {
        if (!this.distFunc.equals(distFunc)) {
            this.distFunc = distFunc;
            this.distance = 0;
            for (int i = 1; i < this.nodeList.size(); i++) {
                this.distance += distFunc.distance(this.nodeList.get(i - 1).toPoint(), this.nodeList.get(i).toPoint());
            }
        }
    }
}
