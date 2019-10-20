package util.object.roadnetwork;

import org.apache.log4j.Logger;
import util.function.DistanceFunction;
import util.function.SpatialUtils;
import util.object.spatialobject.Point;
import util.object.spatialobject.Rect;
import util.object.spatialobject.Segment;

import javax.annotation.Nullable;
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
	 * Only useful when length is given, the length will not be written to file, hence, it will be lost after I/O.
	 */
	private boolean isPresetLength = false;
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
	 * @param s          The input string.
	 * @param index2Node The road node list that used to find the pointer to the end points of the road way. Leave empty if nodes and
	 *                   ways are not required to be linked.
	 * @param df         Distance function.
	 * @return The generated road way instance.
	 */
	public static RoadWay parseRoadWay(String s, Map<String, RoadNode> index2Node, DistanceFunction df) {
		String[] edgeInfo = s.split("\\|");
		if (edgeInfo.length < 3 || edgeInfo.length > 4)
			throw new IndexOutOfBoundsException("Failed to read road way: input data format is wrong. " + s);
		RoadWay newWay = new RoadWay(edgeInfo[0], df);
		List<RoadNode> miniNode = new ArrayList<>();
		newWay.setNewRoad(edgeInfo[2].equals("true"));
		if (edgeInfo[0].equals("null")) {
			String[] nodeInfo = edgeInfo[1].split(",");
			// the current edge is fresh (not yet processed by map merge), the endpoints are not able to be matched to existing nodes
			for (String info : nodeInfo) {
				miniNode.add(RoadNode.parseRoadNode(info, df));
			}
		} else {
			if (index2Node == null || index2Node.isEmpty()) {
				// the read only happens in map road
				String[] nodeInfo = edgeInfo[1].split(",");
				for (String info : nodeInfo) {
					miniNode.add(RoadNode.parseRoadNode(info, df));
				}
			} else {
				// the end nodes are able to be found in node mapping
				String[] nodeInfo = edgeInfo[1].split(",");
				String firstNodeID = nodeInfo[0].split(" ")[0];
				String lastNodeID = nodeInfo[nodeInfo.length - 1].split(" ")[0];
				if (index2Node.containsKey(firstNodeID) && index2Node.containsKey(lastNodeID)) {
					// the road way record is complete and the endpoints exist
					miniNode.add(index2Node.get(firstNodeID));
					for (int i = 1; i < nodeInfo.length - 1; i++) {
						miniNode.add(RoadNode.parseRoadNode(nodeInfo[i], df));
					}
					miniNode.add(index2Node.get(lastNodeID));
				} else {
					// it happens during the extraction of map with boundary. Otherwise, it should be a mistake.
//					LOG.warn("The endpoints of the road way cannot be found in node list: " + newWay.getID());
					return new RoadWay(df);
				}
			}
		}
		if (edgeInfo.length == 4) {
			// attributes exists
			String[] attributeList = edgeInfo[3].split("_");
			for (String attribute : attributeList) {
				String[] values = attribute.split(":");
				if (values.length != 2)
					throw new IllegalArgumentException("The attribute format is wrong: " + attribute);
				if (values[0].equals("wayType")) {    // way type is a BitSet, which has to be parsed carefully
					if (!values[1].equals("{}") && !values[1].equals("null")) {
						String[] wayTypeList = values[1].substring(1, values[1].length() - 1).split(", ");
						for (String type : wayTypeList) {
//							if (type.equals(""))
//								System.out.println("TEST");
							newWay.setWayTypeBit(Integer.parseInt(type));
						}
					}
				} else {
					newWay.addTag(values[0], values[1]);
				}
			}
		}
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
		if (isPresetLength)
			clone.setLength(getLength());
		clone.calculateCenter();
		return clone;
	}
	
	/**
	 * Convert the road way into string, for write purpose. The format is as follows:
	 * ID|RoadNode1,RoadNode2...|isNewRoad|attribute1:value1_attribute2:value2
	 *
	 * @return String that contains all road way information
	 */
	@Override
	public String toString() {
		StringBuilder s = new StringBuilder(this.getID() + "|");
		for (RoadNode n : this.getNodes()) {
			s.append(",").append(n.toString());
		}
		s.append("|").append(this.isNewRoad ? "true" : "false");
		if (!getTags().isEmpty()) {
			s.append("|");
			for (Map.Entry<String, Object> entry : getTags().entrySet()) {
				s.append(entry.getKey()).append(":").append(entry.getValue()).append("_");
			}
		}
		
		return s.deleteCharAt(s.length() - 1).toString().replaceFirst(",", "");
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
	
	public void setLength(double length) {
		this.isPresetLength = true;
		this.length = length;
	}
	
	public double getLength() {
		return length;
	}
	
	public RoadNode getFromNode() {
		if (this.nodeList.size() == 0)
			throw new IndexOutOfBoundsException("The current road way is empty.");
		
		return this.nodeList.get(0);
	}
	
	public RoadNode getToNode() {
		if (this.nodeList.size() == 0)
			throw new IndexOutOfBoundsException("The current road way is empty.");
		return this.nodeList.get(this.nodeList.size() - 1);
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
		return Double.parseDouble(getTags().get("confidenceScore").toString());
	}
	
	public void setConfidenceScore(double confidenceScore) {
		this.addTag("confidenceScore", confidenceScore);
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
		return Double.parseDouble(getTags().get("influenceScore").toString());
	}
	
	public void setInfluenceScore(double influenceScore) {
		this.addTag("influenceScore", influenceScore);
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
		return Integer.parseInt(getTags().get("visitCount").toString());
	}
	
	public void setVisitCount(int count) {
		this.addTag("visitCount", count);
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
		this.addTag("wayType", wayType);
	}
	
	public void setWayTypeBit(int roadWayType) {
		if (getTags().get("wayType") == null) {
			BitSet currSet = new BitSet(25);
			currSet.set(roadWayType);
			this.addTag("wayType", currSet);
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
		return Short.parseShort(getTags().get("wayLevel").toString());
	}
	
	public void setWayLevel(short roadWayLevel) {
		this.addTag("wayLevel", roadWayLevel);
	}
	
	/**
	 * The speed limit for each road, measured by km/h.
	 *
	 * @return The speed limit.
	 */
	public int getSpeedLimit() {
		if (getTags().get("speedLimit") == null)
			return -1;
		return Integer.parseInt(getTags().get("speedLimit").toString());
	}
	
	public void setSpeedLimit(int speedLimit) {
		this.addTag("speedLimit", speedLimit);
	}
	
	public List<RoadWay> splitAtNode(RoadNode intersectionNode) {
		return splitAtNode(intersectionNode, null);
	}
	
	
	/**
	 * Split the current road into two roads based on the given intersection node which is on one of its road segment. The ID of the
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