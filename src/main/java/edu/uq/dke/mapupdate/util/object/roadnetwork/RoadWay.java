package edu.uq.dke.mapupdate.util.object.roadnetwork;

import edu.uq.dke.mapupdate.util.function.GreatCircleDistanceFunction;
import edu.uq.dke.mapupdate.util.function.PointDistanceFunction;
import edu.uq.dke.mapupdate.util.object.spatialobject.Point;
import edu.uq.dke.mapupdate.util.object.spatialobject.Segment;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * A way in the road network graph (OSM Way).
 *
 * @author uqpchao, uqdalves
 */
@SuppressWarnings("serial")
public class RoadWay extends RoadNetworkPrimitive {

    /**
     * The list of nodes in this road way
     */
    private List<RoadNode> nodeList = new ArrayList<>();

    /**
     * Total distance of the road way
     */
    private double distance;

    private PointDistanceFunction distFunc;

    /**
     * The two endpoints of the road way
     */
    private RoadNode fromNode;
    private RoadNode toNode;

    /**
     * The spatially center of the road way. The center is the middle point between two end points
     */
    private Point virtualCenter;

    /**
     * The weights of the road used for map-trajectory co-optimization
     * The confidence score indicates the confidence of the map inference
     * The influence score indicates the influence of the road to the map-matching results
     * Both scores are zero if it is not a new road
     */
    private double confidenceScore = 0;
    private double influenceScore = 0;

    /**
     * The new road indicator
     */
    private boolean isNewRoad = false;

    /**
     * Creates a new empty road way.
     */
    public RoadWay() {
        this.distFunc = new GreatCircleDistanceFunction();
    }

    /**
     * Creates a new empty road way.
     *
     * @param wayId The road way identifier.
     */
    public RoadWay(String wayId) {
        super(wayId);
        this.distFunc = new GreatCircleDistanceFunction();
    }

    /**
     * Creates a new empty road way.
     *
     * @param wayId     The road way identifier.
     * @param timeStamp The road way time-stamp.
     */
    public RoadWay(String wayId, String timeStamp) {
        super(wayId, timeStamp);
        this.distFunc = new GreatCircleDistanceFunction();
    }

    /**
     * Creates a new road way from the given nodes.
     *
     * @param wayId    The road way identifier.
     * @param nodeList A sorted list of way nodes.
     */
    public RoadWay(String wayId, List<RoadNode> nodeList) {
        super(wayId);
        if (nodeList.size() < 2) {
            throw new NullPointerException("Road way " + wayId + " contains less than two nodes.");
        }
        this.nodeList = nodeList;
        this.fromNode = this.nodeList.get(0);
        this.toNode = this.nodeList.get(nodeList.size() - 1);
        calculateCenter();
        this.distFunc = new GreatCircleDistanceFunction();
        for (int i = 1; i < this.nodeList.size(); i++) {
            this.distance += distFunc.distance(this.nodeList.get(i - 1).toPoint(), this.nodeList.get(i).toPoint());
        }
    }

    /**
     * Creates a empty road way from the given nodes.
     *
     * @param wayId    The road way identifier.
     * @param nodeList A sorted list of way nodes.
     */
    public RoadWay(String wayId, List<RoadNode> nodeList, PointDistanceFunction distFunc) {
        super(wayId);
        if (nodeList.size() < 2) {
            throw new NullPointerException("Road way " + wayId + " contains less than two nodes.");
        }
        this.nodeList = nodeList;
        this.fromNode = this.nodeList.get(0);
        this.toNode = this.nodeList.get(nodeList.size() - 1);
        calculateCenter();
        this.distFunc = distFunc;
        for (int i = 1; i < this.nodeList.size(); i++) {
            this.distance += distFunc.distance(this.nodeList.get(i - 1).toPoint(), this.nodeList.get(i).toPoint());
        }
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
     * Calculate the virtual center of the road way, for better index accuracy. It should be called once fromNode and toNode are updated.
     */
    private void calculateCenter() {
        this.virtualCenter = new Point((this.fromNode.lon() + this.toNode.lon()) / 2.0, (this.fromNode.lat() + this.toNode.lat()) / 2.0);
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
        this.fromNode = this.nodeList.get(0);
        this.toNode = this.nodeList.get(nodeList.size() - 1);
        calculateCenter();
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
            if (this.nodeList.size() > 0) {
                this.toNode = node;
                calculateCenter();
                this.distance += distFunc.distance(this.nodeList.get(this.nodeList.size() - 1).toPoint(), node.toPoint());
            } else {
                this.fromNode = node;
            }
            this.nodeList.add(node);
        }
    }

    /**
     * Adds all nodes in the list to this road way.
     * The nodes are connected sequentially to the end of this road way,
     * creating an new edge between every n and n+1 node.
     *
     * @param nodeList A sequence of road node to add.
     */
    public void addNodes(List<RoadNode> nodeList) {
        if (!nodeList.isEmpty()) {
            if (this.nodeList.isEmpty()) {
                this.fromNode = nodeList.get(0);
            }
            this.nodeList.addAll(nodeList);
            this.toNode = nodeList.get(nodeList.size() - 1);
            calculateCenter();
            // recalculate the distance
            this.distance = 0;
            for (int i = 1; i < this.nodeList.size(); i++)
                this.distance += distFunc.distance(this.nodeList.get(i - 1).toPoint(), this.nodeList.get(i).toPoint());
        }
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
        RoadWay clone = new RoadWay(getId(), getTimeStamp(), this.distFunc);
        clone.addNodes(nodeList);
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
     * A comparator to compare nodes by time-stamp value.
     *
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

    public RoadNode getFromNode() {
        return fromNode;
    }

    public RoadNode getToNode() {
        return toNode;
    }

    public Point getVirtualCenter() {
        return virtualCenter;
    }

    public PointDistanceFunction getDistFunc() {
        return distFunc;
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

    public double getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(double confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    public double getInfluenceScore() {
        return influenceScore;
    }

    public void setInfluenceScore(double influenceScore) {
        this.influenceScore = influenceScore;
    }

    public boolean isNewRoad() {
        return isNewRoad;
    }

    public void setNewRoad(boolean newRoad) {
        isNewRoad = newRoad;
    }
}