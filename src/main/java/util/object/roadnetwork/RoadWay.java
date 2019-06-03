package util.object.roadnetwork;

import org.apache.log4j.Logger;
import util.function.DistanceFunction;
import util.function.SpatialUtils;
import util.object.spatialobject.Point;
import util.object.spatialobject.Rect;
import util.object.spatialobject.Segment;

import javax.annotation.Nullable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Map;

/**
 * A way in the road network graph (OSM Way).
 *
 * @author Hellisk, uqdalves
 */
public class RoadWay extends RoadNetworkPrimitive {
	
	private static final Logger LOG = Logger.getLogger(RoadWay.class);
	
	/**
	 * The list of nodes in this road way
	 */
	private List<RoadNode> nodeList = new ArrayList<>();
	
	/**
	 * Total length of the road way
	 */
	private double length = 0;
	
	/**
	 * The spatially center of the road way. The center is the middle point between two end points
	 */
	private Point virtualCenter;
	
	/**
	 * The new road indicator, if a road is new, it will not have valid road id, road level, road type and mini node id, but it will have
	 * influence score and confidence score
	 */
	private boolean isNewRoad = false;
	
	/**
	 * Creates a new empty road way
	 */
	public RoadWay(DistanceFunction df) {
		super(df);
	}
	
	/**
	 * Creates a new empty road way.
	 *
	 * @param wayId The road way identifier.
	 * @param df    The distance function.
	 */
	public RoadWay(String wayId, DistanceFunction df) {
		super(wayId, df);
	}
	
	/**
	 * Creates a new empty road way.
	 *
	 * @param wayId     The road way identifier.
	 * @param timeStamp The road way time-stamp.
	 * @param df        The distance function.
	 */
	public RoadWay(String wayId, String timeStamp, DistanceFunction df) {
		super(wayId, timeStamp, df);
	}
	
	/**
	 * Creates a new road way from the given nodes.
	 *
	 * @param wayId    The road way identifier.
	 * @param nodeList A sorted list of way nodes.
	 * @param df       The distance function.
	 */
	public RoadWay(String wayId, List<RoadNode> nodeList, DistanceFunction df) {
		super(wayId, df);
		if (nodeList.size() < 2) {
			throw new IllegalArgumentException("Road way " + wayId + " contains less than two nodes.");
		}
		this.nodeList = nodeList;
		this.length = 0;
		calculateCenter();
		for (int i = 1; i < this.nodeList.size(); i++) {
			this.length += getDistanceFunction().distance(this.nodeList.get(i - 1).toPoint(), this.nodeList.get(i).toPoint());
		}
	}
	
	/**
	 * Parse the given string into a road way instance
	 *
	 * @param s          The input string
	 * @param index2Node The road node list that used to find the pointer to the end points of the road way. Leave empty if nodes and
	 *                   ways are not required to be linked.
	 * @return The generated road way instance
	 */
	public static RoadWay parseRoadWay(String s, Map<String, RoadNode> index2Node, DistanceFunction df) {
		String[] edgeInfo = s.split("\\|");
		if (edgeInfo[6].contains(",") || !edgeInfo[7].contains(","))
			throw new IndexOutOfBoundsException("Failed to read road way: input data format is wrong. " + s);
		RoadWay newWay;
		List<RoadNode> miniNode = new ArrayList<>();
		newWay = new RoadWay(edgeInfo[0], df);
		newWay.setVisitCount(Integer.parseInt(edgeInfo[6]));
		newWay.setNewRoad(edgeInfo[5].equals("true"));
		if (edgeInfo[0].equals("null")) {
			// the current edge is fresh(not yet processed by map merge), the endpoints are not able to be matched to existing nodes
			for (int i = 7; i < edgeInfo.length; i++) {
				String[] roadWayPoint = edgeInfo[i].split(",");
				if (roadWayPoint.length == 3) {
					RoadNode newNode = new RoadNode(roadWayPoint[0], Double.parseDouble(roadWayPoint[1]), Double.parseDouble
							(roadWayPoint[2]), df);
					miniNode.add(newNode);
				} else
					throw new IllegalArgumentException("Failed reading mini node for new road: input data format is wrong. " + edgeInfo[i]);
			}
		} else {
			if (index2Node == null || index2Node.isEmpty()) {
				// the read only happens in map road
				for (int i = 7; i < edgeInfo.length; i++) {
					String[] roadWayPoint = edgeInfo[i].split(",");
					if (roadWayPoint.length == 3) {
						RoadNode newNode = new RoadNode(roadWayPoint[0], Double.parseDouble(roadWayPoint[1]),
								Double.parseDouble(roadWayPoint[2]), df);
						miniNode.add(newNode);
					} else
						throw new IllegalArgumentException("Failed reading mini node: input data format is wrong. " + edgeInfo[i]);
				}
			} else if (index2Node.containsKey(edgeInfo[7].split(",")[0]) && index2Node.containsKey(edgeInfo[edgeInfo.length - 1].split(",")[0])) {
				// the road way record is complete and the endpoints exist
				for (int i = 7; i < edgeInfo.length; i++) {
					String[] roadWayPoint = edgeInfo[i].split(",");
					if (roadWayPoint.length == 3) {
						RoadNode newNode;
						if (i == 7) {
							newNode = index2Node.get(roadWayPoint[0]);
						} else if (i == edgeInfo.length - 1) {
							newNode = index2Node.get(roadWayPoint[0]);
						} else {
							newNode = new RoadNode(roadWayPoint[0], Double.parseDouble(roadWayPoint[1]),
									Double.parseDouble(roadWayPoint[2]), df);
						}
						miniNode.add(newNode);
					} else
						throw new IllegalArgumentException("Failed reading mini node: input data format is wrong. " + edgeInfo[i]);
				}
			} else {
				// it happens during the extraction of map with boundary. Otherwise, it should be a mistake.
//				LOG.warn("The endpoints of the road way cannot be found in node list: " + newWay.getID());
				return new RoadWay(df);
			}
		}
		
		newWay.setWayLevel(Short.parseShort(edgeInfo[1]));
		if (!edgeInfo[2].equals("{}") && !edgeInfo[2].equals("null")) {
			String[] wayTypeList = edgeInfo[2].substring(1, edgeInfo[2].length() - 1).split(", ");
			for (String type : wayTypeList) {
				newWay.setWayTypeBit(Integer.parseInt(type));
			}
		}
		newWay.setInfluenceScore(Double.parseDouble(edgeInfo[3]));
		newWay.setConfidenceScore(Double.parseDouble(edgeInfo[4]));
		newWay.setNodes(miniNode);
		return newWay;
	}
	
	/**
	 * Calculate the virtual center of the road way, for better index accuracy. It should be called once fromNode and toNode are updated.
	 */
	private void calculateCenter() {
		if (nodeList.size() == 0)
			throw new IllegalArgumentException("Cannot calculate center: the node list is empty.");
		if (nodeList.size() == 1) {
			this.virtualCenter = new Point(nodeList.get(0).lon(), nodeList.get(0).lat(), getDistanceFunction());
		} else {
			List<Point> pointList = new ArrayList<>();
			for (RoadNode n : nodeList) {
				pointList.add(n.toPoint());
			}
			Rect bb = SpatialUtils.getBoundingBox(pointList, getDistanceFunction());
			this.virtualCenter = new Point((bb.maxX() + bb.minX()) / 2, (bb.maxY() + bb.minY()) / 2.0, getDistanceFunction());
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
		calculateCenter();
		this.length = 0;
		for (int i = 1; i < this.nodeList.size(); i++) {
			this.length += getDistanceFunction().distance(this.nodeList.get(i - 1).toPoint(), this.nodeList.get(i).toPoint());
		}
	}
	
	/**
	 * Get the road node in the given index position from this road way.
	 *
	 * @param index The road node position in the road way.
	 * @return The road node in the specified position.
	 */
	public RoadNode getNode(int index) {
		if (index < 0 || index >= size()) {
			throw new IndexOutOfBoundsException("Node index out of bounds. Index should be greater than or equals to 0, or less than " + size());
		}
		return nodeList.get(index);
	}
	
	/**
	 * Adds a node to this road way. The node is connected to the current end-point node of this road way, creating a new edge.
	 *
	 * @param node The road node to add.
	 */
	public void addNode(RoadNode node) {
		if (node != null) {
			this.nodeList.add(node);
			calculateCenter();
			if (this.nodeList.size() > 1)
				this.length += getDistanceFunction().distance(this.nodeList.get(this.nodeList.size() - 2).toPoint(), node.toPoint());
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
			// recalculate the length
			this.length = 0;
			for (int i = 1; i < this.nodeList.size(); i++)
				this.length += getDistanceFunction().distance(this.nodeList.get(i - 1).toPoint(), this.nodeList.get(i).toPoint());
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
	 * Convert this road way to a list of spatial segments. Each segment inherit the same road id.
	 *
	 * @return A sorted list of way segments.
	 */
	public List<Segment> getEdges() {
		List<Segment> sList = new ArrayList<>(size() - 1);
		RoadNode node1, node2;
		for (int i = 0; i < nodeList.size() - 1; i++) {
			node1 = nodeList.get(i);
			node2 = nodeList.get(i + 1);
			Segment currSegment = new Segment(node1.lon(), node1.lat(), node2.lon(), node2.lat(), getDistanceFunction());
			currSegment.setID(this.getID());
			sList.add(currSegment);
		}
		return sList;
	}
	
	/**
	 * Makes an exact copy of this object. SHOULD NOT BE USED if the reference of the endpoints are to be preserved.
	 */
	@Override
	public RoadWay clone() throws CloneNotSupportedException {
		RoadWay clone = (RoadWay) super.clone();
		for (RoadNode n : nodeList) {
			clone.addNode(n.clone());
		}
		clone.addNodes(nodeList);
		clone.setNewRoad(isNewRoad);
		clone.length = getLength();
		clone.calculateCenter();
		return clone;
	}
	
	/**
	 * Convert the road way into string, for write purpose. The format is as follows:
	 * ID|RoadLevel|RoadType|InfScore|ConScore|isNewRoad|visitCount|RoadNode1,RoadNode2...
	 *
	 * @return String that contains all road way information
	 */
	@Override
	public String toString() {
		DecimalFormat df = new DecimalFormat("0.00000");
		StringBuilder s = new StringBuilder(this.getID() + "|");
		s.append(this.getWayLevel()).append("|").append(this.getWayType() == null ? "null" : this.getWayType().toString()).append("|");
		s.append(this.getInfluenceScore()).append("|").append(this.getConfidenceScore()).append("|");
		s.append(this.isNewRoad ? "true" : "false").append("|");
		s.append(this.getVisitCount());
		for (RoadNode n : this.getNodes()) {
			s.append("|").append(n.getID()).append(",").append(df.format(n.lon())).append(",").append(df.format(n.lat()));
		}
		return s.toString();
	}
	
	@Override
	public void print() {
		LOG.info("Road way ( " + toString() + " )");
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
		return this.size() == other.size() && this.getID().equals(other.getID());
	}
	
	public double getLength() {
		return length;
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
	
	/**
	 * The weights of the road used for map-trajectory co-optimization.
	 * <p>
	 * The confidence score indicates the confidence of the map inference. The influence score indicates the influence of the road to the
	 * map-matching results. Both scores are zero if it is not a new road.
	 */
	public double getConfidenceScore() {
		if (getTags().get("confidenceScore") == null)
			return -1;
		return (double) getTags().get("confidenceScore");
	}
	
	public void setConfidenceScore(double confidenceScore) {
		getTags().put("confidenceScore", confidenceScore);
	}
	
	/**
	 * The weights of the road used for map-trajectory co-optimization.
	 * <p>
	 * The confidence score indicates the confidence of the map inference. The influence score indicates the influence of the road to the
	 * map-matching results. Both scores are zero if it is not a new road.
	 */
	public double getInfluenceScore() {
		if (getTags().get("influenceScore") == null)
			return -1;
		return (double) getTags().get("influenceScore");
	}
	
	public void setInfluenceScore(double influenceScore) {
		getTags().put("influenceScore", influenceScore);
	}
	
	public boolean isNewRoad() {
		return isNewRoad;
	}
	
	public void setNewRoad(boolean newRoad) {
		isNewRoad = newRoad;
	}
	
	/**
	 * Number of times that the road way is visited by trajectories. Also used in map merge where the visitCount is set to 1 when it is
	 * processed
	 */
	public int getVisitCount() {
		if (getTags().get("visitCount") == null)
			return -1;
		return (int) getTags().get("visitCount");
	}
	
	public void setVisitCount(int count) {
		this.getTags().put("visitCount", count);
	}
	
	/**
	 * Beijing dataset only
	 * The road type. There are 25 different types stored in a 25-bit binary. Each road has at least one type but may have multiple types.
	 * The meaning of each binary bit:
	 * 0~9: Round about, No type, Separated double direction, JCT, Inner-intersection link, Highway entrance/exit (IC), Parking, Service
	 * zone, Bridge, Pedestrian only
	 * 10~19: Bus only, Right-turn ready lane, Scenery route, Internal route, Left-turn ready route, U turn route, Route between main and
	 * side road, Virtual connection, Parking guide line, Side road
	 * 20~24: Ramp, Fully enclosed road, Undefined area, Connection to POI, Tunnel
	 */
	@Nullable
	public BitSet getWayType() {
		return (BitSet) getTags().get("wayType");
	}
	
	public void setWayType(BitSet wayType) {
		getTags().put("wayType", wayType);
	}
	
	public void setWayTypeBit(int roadWayType) {
		if (getTags().get("wayType") == null) {
			BitSet currSet = new BitSet(25);
			currSet.set(roadWayType);
			getTags().put("wayType", currSet);
		}
		((BitSet) getTags().get("wayType")).set(roadWayType);
	}
	
	/**
	 * The road level, which consists of the follows(short value):
	 * 0 = highway, 1 = city highway, 2 = national route, 3 = state route, 4 = intercity route, 5 = ferry, 6 = city route,
	 * 7 = pedestrian route, 8 = others, 9 = Cycling route, -1 = null value
	 */
	public short getWayLevel() {
		if (getTags().get("wayLevel") == null)
			return -1;
		return (Short) getTags().get("wayLevel");
	}
	
	public void setWayLevel(short roadWayLevel) {
		this.getTags().put("wayLevel", roadWayLevel);
	}
	
	
	public List<RoadWay> splitAtNode(RoadNode intersectionNode) {
		return splitAtNode(intersectionNode, null);
	}
	
	/**
	 * Split the current road into two roads based on the given the intersection node which is on one of its road segment. The ID of the
	 * new roads is the original ID with an additional '+' or '-' based on whether it is the upper part or lower part, respectively.
	 *
	 * @param intersectionNode The intersection node.
	 * @param edge             The segment which the intersection located. =null if the intersection is one of its mini node.
	 * @return List of roads (two roads) after split.
	 */
	public List<RoadWay> splitAtNode(RoadNode intersectionNode, Segment edge) {
		List<RoadNode> startMiniNodeList = new ArrayList<>();
		String id = this.getID();
		int splitIndex = 0;
		if (edge != null) {
			while (!this.getNode(splitIndex).toPoint().equals2D(edge.p1()) && splitIndex < this.size() - 1) {
				startMiniNodeList.add(this.getNode(splitIndex));
				splitIndex++;
			}
			if (splitIndex >= this.size() - 1 || !this.getNode(splitIndex + 1).toPoint().equals2D(edge.p2()))
				throw new IndexOutOfBoundsException("New intersection found, but cannot locate it.");
			// the intersection occurs
			startMiniNodeList.add(this.getNode(splitIndex));
			startMiniNodeList.add(intersectionNode);
		} else {
			while (!this.getNode(splitIndex).toPoint().equals2D(intersectionNode.toPoint()) && splitIndex < this.size() - 1) {
				startMiniNodeList.add(this.getNode(splitIndex));
				splitIndex++;
			}
			if (splitIndex >= this.size() - 1)
				throw new IndexOutOfBoundsException("New intersection found, but cannot locate it.");
			startMiniNodeList.add(this.getNode(splitIndex));
		}
		RoadWay startWay = new RoadWay(id + "+", startMiniNodeList, this.getDistanceFunction());
		List<RoadNode> endMiniNodeList = new ArrayList<>();
		endMiniNodeList.add(intersectionNode);
		endMiniNodeList.addAll(this.getNodes().subList(splitIndex + 1, this.size()));
		RoadWay endWay = new RoadWay(id + "-", endMiniNodeList, this.getDistanceFunction());
		List<RoadWay> splitWayList = new ArrayList<>();
		splitWayList.add(startWay);
		splitWayList.add(endWay);
		return splitWayList;
	}
}