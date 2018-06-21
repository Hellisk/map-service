package edu.uq.dke.mapupdate.util.object.roadnetwork;

import edu.uq.dke.mapupdate.util.function.GreatCircleDistanceFunction;
import edu.uq.dke.mapupdate.util.function.PointDistanceFunction;
import edu.uq.dke.mapupdate.util.object.spatialobject.Point;
import edu.uq.dke.mapupdate.util.object.spatialobject.Segment;

import java.text.DecimalFormat;
import java.util.*;

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
     * Total roadLength of the road way
     */
    private double roadLength;

    private PointDistanceFunction distFunc;

    /**
     * The spatially center of the road way. The center is the middle point between two end points
     */
    private Point virtualCenter;

    /**
     * Number of times that the road way is visited
     */
    private int visitCount = 0;

    /**
     * The road level, which consists of the follows:
     * 0 = highway, 1 = city highway, 2 = national route, 3 = state route, 4 = intercity route, 5 = ferry, 6 = city route,
     * 7 = pedestrian route, 8 = others, 9 = Cycling route
     */
    private short roadWayLevel = -1;

    /**
     * The road type. There are 25 different types stored in a 25-bit binary. Each road has at least one type but may have multiple types.
     * The meaning of each binary bit:
     * 0~9: Round about, No type, Separated double direction, JCT, Inner-intersection link, Highway entrance/exit (IC), Parking, Service
     * zone, Bridge, Pedestrian only
     * 10~19: Bus only, Right-turn ready lane, Scenery route, Internal route, Left-turn ready route, U turn route, Route between main and
     * side road, Virtual connection, Parking guide line, Side road
     * 20~24: Ramp, Fully enclosed road, Undefined area, Connection to POI, Tunnel
     */
    private BitSet roadWayType = new BitSet(25);

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
     * Creates a new empty road way
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
        calculateCenter();
        this.distFunc = new GreatCircleDistanceFunction();
        for (int i = 1; i < this.nodeList.size(); i++) {
            this.roadLength += distFunc.distance(this.nodeList.get(i - 1).toPoint(), this.nodeList.get(i).toPoint());
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
        calculateCenter();
        this.distFunc = distFunc;
        for (int i = 1; i < this.nodeList.size(); i++) {
            this.roadLength += distFunc.distance(this.nodeList.get(i - 1).toPoint(), this.nodeList.get(i).toPoint());
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
        this.virtualCenter = new Point((this.nodeList.get(0).lon() + this.nodeList.get(this.nodeList.size() - 1).lon()) / 2.0, (this
                .nodeList.get(0).lat() + this.nodeList.get(this.nodeList.size() - 1).lat()) / 2.0);
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
        calculateCenter();
        for (int i = 1; i < this.nodeList.size(); i++) {
            this.roadLength += distFunc.distance(this.nodeList.get(i - 1).toPoint(), this.nodeList.get(i).toPoint());
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
            this.nodeList.add(node);
            calculateCenter();
            this.roadLength += distFunc.distance(this.nodeList.get(this.nodeList.size() - 1).toPoint(), node.toPoint());
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
            this.nodeList.addAll(nodeList);
            calculateCenter();
            // recalculate the roadLength
            this.roadLength = 0;
            for (int i = 1; i < this.nodeList.size(); i++)
                this.roadLength += distFunc.distance(this.nodeList.get(i - 1).toPoint(), this.nodeList.get(i).toPoint());
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
     * Makes an exact copy of this object
     */
    @Override
    public RoadWay clone() {
        RoadWay clone = new RoadWay(getId(), getTimeStamp(), this.distFunc);
        clone.addNodes(nodeList);
        clone.setNewRoad(isNewRoad);
        clone.setVisitCount(visitCount);
        clone.setRoadWayType(roadWayType);
        clone.setRoadWayLevel(roadWayLevel);
        return clone;
    }

    /**
     * Convert the road way into string, for write purpose. The format is as follows:
     * ID|RoadLevel|RoadType|ConScore|InfScore|RoadNode1,RoadNode2...
     *
     * @return String that contains all road way information
     */
    @Override
    public String toString() {
        DecimalFormat df = new DecimalFormat(".00000");
        StringBuilder s = new StringBuilder(this.getId() + "|");
        s.append(this.getRoadWayLevel()).append("|").append(this.getRoadWayType().toString()).append("|");
        s.append(this.getConfidenceScore()).append("|").append(this.getInfluenceScore()).append("|");
        s.append(this.getVisitCount());
        for (RoadNode n : this.getNodes()) {
            s.append("|").append(n.getId()).append(",").append(df.format(n.lon())).append(",").append(df.format(n.lat()));
        }
        return s.toString();
    }

    /**
     * Parse the given string into a road way instance
     *
     * @param s          The input string
     * @param index2Node The road node list that used to find the end points of the road way
     * @return The generated road way instance
     */
    public static RoadWay parseRoadWay(String s, Map<String, RoadNode> index2Node) {
        String[] edgeInfo = s.split("\\|");
        if (!edgeInfo[6].contains(","))
            throw new IndexOutOfBoundsException("ERROR! Failed to read road way: input data format is wrong. " + s);
        RoadWay newWay = new RoadWay(edgeInfo[0]);
        List<RoadNode> miniNode = new ArrayList<>();
        // the road way record is complete and the endpoints exist
        if (index2Node.containsKey(edgeInfo[6].split(",")[0]) && index2Node.containsKey(edgeInfo[edgeInfo.length - 1].split(",")[0])) {
            for (int i = 6; i < edgeInfo.length; i++) {
                String[] roadWayPoint = edgeInfo[i].split(",");
                if (roadWayPoint.length == 3) {
                    RoadNode newNode;
                    if (i == 6) {
                        newNode = index2Node.get(roadWayPoint[0]);
                    } else if (i == edgeInfo.length - 1) {
                        newNode = index2Node.get(roadWayPoint[0]);
                    } else
                        newNode = new RoadNode(roadWayPoint[0], Double.parseDouble(roadWayPoint[1]), Double.parseDouble(roadWayPoint[2]));

                    miniNode.add(newNode);
                } else throw new IllegalArgumentException("ERROR! Failed reading mini node: input data format is wrong. " + edgeInfo[i]);
            }
        } else if (index2Node.isEmpty()) {
            for (int i = 6; i < edgeInfo.length; i++) {
                String[] roadWayPoint = edgeInfo[i].split(",");
                if (roadWayPoint.length == 3) {
                    RoadNode newNode = new RoadNode(roadWayPoint[0], Double.parseDouble(roadWayPoint[1]), Double.parseDouble(roadWayPoint[2]));
                    miniNode.add(newNode);
                } else throw new IllegalArgumentException("ERROR! Failed reading mini node: input data format is wrong. " + edgeInfo[i]);
            }
        } else return new RoadWay();
        newWay.setRoadWayLevel(Short.parseShort(edgeInfo[1]));
        String[] wayTypeList = edgeInfo[2].substring(1, edgeInfo[2].length() - 1).split(", ");
        for (String type : wayTypeList) {
            if (Integer.parseInt(type) < newWay.getRoadWayType().size())
                newWay.getRoadWayType().set(Integer.parseInt(type));
            else throw new IndexOutOfBoundsException("ERROR! The road type is incorrect." + type);
        }
        if (!edgeInfo[3].equals("0") || !edgeInfo[4].equals("0")) {
            newWay.setNewRoad(true);
            newWay.setConfidenceScore(Double.parseDouble(edgeInfo[3]));
            newWay.setInfluenceScore(Double.parseDouble(edgeInfo[4]));
            newWay.setVisitCount(Integer.parseInt(edgeInfo[5]));
        }
        newWay.setNodes(miniNode);
        return newWay;
    }

    @Override
    public void print() {
        System.out.println("Road way ( " + toString() + " )");
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

    public double getRoadLength() {
        return roadLength;
    }

    public RoadNode getFromNode() {
        return this.nodeList.size() != 0 ? this.nodeList.get(0) : null;
    }

    public RoadNode getToNode() {
        return this.nodeList.size() != 0 ? this.nodeList.get(this.nodeList.size() - 1) : null;
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
            this.roadLength = 0;
            for (int i = 1; i < this.nodeList.size(); i++) {
                this.roadLength += distFunc.distance(this.nodeList.get(i - 1).toPoint(), this.nodeList.get(i).toPoint());
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

    public int getVisitCount() {
        return visitCount;
    }

    public void setVisitCount(int count) {
        this.visitCount = count;
    }

    public BitSet getRoadWayType() {
        return roadWayType;
    }

    public void setRoadWayType(BitSet roadWayType) {
        this.roadWayType = roadWayType;
    }

    public void setRoadWayTypeBit(int roadWayType) {
        this.roadWayType.set(roadWayType);
    }

    public short getRoadWayLevel() {
        return roadWayLevel;
    }

    public void setRoadWayLevel(short roadWayLevel) {
        this.roadWayLevel = roadWayLevel;
    }
}