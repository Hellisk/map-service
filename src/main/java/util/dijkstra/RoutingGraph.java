package util.dijkstra;

import org.apache.log4j.Logger;
import util.function.DistanceFunction;
import util.object.roadnetwork.RoadNetworkGraph;
import util.object.roadnetwork.RoadNode;
import util.object.roadnetwork.RoadWay;
import util.object.spatialobject.Point;
import util.object.structure.Pair;
import util.object.structure.PointMatch;
import util.settings.BaseProperty;

import java.io.Serializable;
import java.util.*;

/**
 * Graph object used in Dijkstra shortest path search algorithm.
 */
public class RoutingGraph implements Serializable {
	
	private static final Logger LOG = Logger.getLogger(RoutingGraph.class);
	private HashMap<Integer, String> edgeIndex2RoadID = new HashMap<>();    // the mapping between the mini edge index and its corresponding
	// roadway id and serial number, format: (index,roadID,serialNum), serialNum starts from 0
	private HashMap<String, Integer> endPointLoc2EdgeIndex = new LinkedHashMap<>();  // find the mini edge index using the coordinates and
	// road way id, format: (x1_x2,y1_y2,id)
	private HashMap<Pair<Integer, Integer>, Integer> endPointsIndex2EdgeIndex = new HashMap<>();  // find the mini edge index given the end point indices
	// and its distance to the current edge
	private RoutingVertex[] vertices;
	private RoutingEdge[] routingEdges;
	private DistanceFunction distFunc;
	private HashSet<Integer> newNodeSet = new HashSet<>();  // useful only when isPartial = true;
	private HashSet<Integer> newEdgeSet = new HashSet<>();  // useful only when isPartial = true;
	private HashMap<String, List<Integer>> roadID2NewNodeList = new HashMap<>();  // for each new road, the generated node ID list.
	private HashMap<String, List<Integer>> roadID2NewEdgeList = new HashMap<>();  // for each new road, the generated edge ID list.
	
	/**
	 * Create routing graph for map-matching.
	 *
	 * @param roadNetwork       The map used to build routing graph.
	 * @param isNewRoadIncluded Is new roads to be added together into the shortest path, only = true when in map updateGoh process.
	 */
	public RoutingGraph(RoadNetworkGraph roadNetwork, boolean isNewRoadIncluded, BaseProperty prop) {
		this.distFunc = roadNetwork.getDistanceFunction();
		// insert the road node into node list
		HashMap<String, Integer> nodeID2Index = new HashMap<>();
		HashMap<Integer, Point> vertexID2Loc = new LinkedHashMap<>();
		HashSet<Integer> outGoingNodeSet = new HashSet<>();
		// the size of the vertex list and the current index of the new vertex
		int vertexIndex = 0;
		for (RoadNode node : roadNetwork.getNodes()) {
			if (nodeID2Index.containsKey(node.getID()))
				throw new IllegalArgumentException("Road node ID already exists: " + node.getID());
			nodeID2Index.put(node.getID(), vertexIndex);
			vertexID2Loc.put(vertexIndex, node.toPoint());
			vertexIndex++;
		}
		
		List<RoutingEdge> routingEdgeList = new ArrayList<>();
		int edgeIndex = 0;
		for (RoadWay way : roadNetwork.getWays()) {
			// insert all the mini vertices to the node list
			if (isNewRoadIncluded && way.isNewRoad()) {
				roadID2NewNodeList.put(way.getID(), new ArrayList<>());
				roadID2NewEdgeList.put(way.getID(), new ArrayList<>());
			}
			for (int i = 1; i < way.getNodes().size() - 1; i++) {
				// insert all mini vertices into the nodeID index
				RoadNode startNode = way.getNode(i);
				if (nodeID2Index.containsKey(startNode.getID()))
					throw new IllegalArgumentException("Road node ID for mini node already exists: " + startNode.getID());
				nodeID2Index.put(startNode.getID(), vertexIndex);
				vertexID2Loc.put(vertexIndex, startNode.toPoint());
				if (isNewRoadIncluded && way.isNewRoad()) {
					newNodeSet.add(vertexIndex);
					roadID2NewNodeList.get(way.getID()).add(vertexIndex);
				}
				vertexIndex++;
			}
			
			// insert mini routingEdges to the edge list
			for (int i = 0; i < way.getNodes().size() - 1; i++) {
				RoadNode startNode = way.getNode(i);
				RoadNode endNode = way.getNode(i + 1);
				edgeIndex2RoadID.put(edgeIndex, way.getID());    // sn from 0 to way.getNodes.size()-2
				if (endPointLoc2EdgeIndex.containsKey(startNode.lon() + "_" + startNode.lat() + "," + endNode.lon() + "_" + endNode.lat() + "," + way.getID())) {
					throw new IllegalArgumentException("The same start and end nodes generate multiple roads: " + edgeIndex);
				}
				endPointLoc2EdgeIndex.put(startNode.lon() + "_" + startNode.lat() + "," + endNode.lon() + "_" + endNode.lat() + "," + way.getID(), edgeIndex);
				int startIndex = nodeID2Index.get(startNode.getID());
				int endIndex = nodeID2Index.get(endNode.getID());
				Pair<Integer, Integer> endPointIndices = new Pair<>(startIndex, endIndex);
				if (endPointsIndex2EdgeIndex.containsKey(endPointIndices))
					throw new IllegalArgumentException("The same start and end node id refer to multiple roads: " + edgeIndex);
				endPointsIndex2EdgeIndex.put(endPointIndices, edgeIndex);
				RoutingEdge currRoutingEdge = new RoutingEdge(edgeIndex, startIndex, endIndex, distFunc.distance(startNode.toPoint(),
						endNode.toPoint()));
				routingEdgeList.add(currRoutingEdge);
				if (isNewRoadIncluded && way.isNewRoad()) {
					newEdgeSet.add(edgeIndex);
					roadID2NewEdgeList.get(way.getID()).add(edgeIndex);
				}
				edgeIndex++;
			}
		}
		this.routingEdges = new RoutingEdge[routingEdgeList.size()];
		for (int i = 0; i < routingEdgeList.size(); i++) {
			RoutingEdge currRoutingEdge = routingEdgeList.get(i);
			if (currRoutingEdge.getIndex() != i)
				throw new IllegalArgumentException("The current routing edge id is inconsistent: " + currRoutingEdge.getIndex() + "," + i + ".");
			this.routingEdges[i] = currRoutingEdge;
		}
		
		// create all vertices ready to be updated with the routingEdges
		this.vertices = new RoutingVertex[vertexIndex];
		for (int n = 0; n < vertexIndex; n++) {
			this.vertices[n] = new RoutingVertex();
			this.vertices[n].setIndex(n);
			this.vertices[n].setVertexPoint(vertexID2Loc.get(n));
		}
		
		// add all the routingEdges to the vertices, each edge added to only from vertices
		for (int i = 0; i < this.routingEdges.length; i++) {
			if (!isNewRoadIncluded || !newEdgeSet.contains(i))
				this.vertices[routingEdges[i].getFromNodeIndex()].getOutGoingRoutingEdges().add(routingEdges[i]);
		}
		
		// check the completeness of the graph
		for (RoutingVertex currVertex : this.vertices) {
			if (currVertex.getOutGoingRoutingEdges().size() != 0) {
				for (RoutingEdge e : currVertex.getOutGoingRoutingEdges()) {
					outGoingNodeSet.add(e.getToNodeIndex());
				}
			}
		}
		for (int i = 0; i < vertices.length; i++) {
			if (this.vertices[i].getOutGoingRoutingEdges().size() == 0 && !outGoingNodeSet.contains(i) && !newNodeSet.contains(i))
				LOG.error("Isolated node detected: No. " + i);
		}
		LOG.info("Shortest path graph generated. Total vertices:" + vertexIndex + ", total edges:" + edgeIndex);
	}
	
	/**
	 * Given a source match point and a set of destination points, the function calculate the shortest path to each destination and their
	 * distance using Dijkstra algorithm.
	 *
	 * @param source        The source match point and its segment.
	 * @param pointList     The destination match point list.
	 * @param maxSearchDist The maximum search range where shortest path search terminates.
	 * @return List of results which contain distance and shortest path. distance = Double.POSITIVE_INFINITY and path is empty if not
	 * reachable within maxSearchDist.
	 */
	public List<Pair<Double, List<String>>> calculateOneToNDijkstraSP(PointMatch source, List<PointMatch> pointList, double maxSearchDist) {
		double[] distance = new double[pointList.size()];   // the distance to every destination
		List<List<String>> path = new ArrayList<>(pointList.size());     // the path to every destination
		HashMap<Integer, Integer> parent = new HashMap<>();        // the parent of each vertex, used during Dijkstra traversal
		HashMap<Integer, Double> vertexDistFromSource = new HashMap<>();    // the distance from the vertex to the source node
		HashSet<Integer> vertexVisited = new HashSet<>();        // set of vertices visited
		List<Pair<Double, List<String>>> result;
		
		Arrays.fill(distance, Double.POSITIVE_INFINITY);
		for (int i = 0; i < pointList.size(); i++) {
			path.add(new ArrayList<>());
		}
		// the variables have been initialized during the last calculation. Start the process right away
		HashMap<Integer, Integer> vertexID2DestIndexSet = new HashMap<>();        // all destinations that requires Dijkstra search,
		// (destination vertex ID and destination index in pointList)
		
		
		// if source point doesn't exist, return infinity to all distances
//		String sourceLocID = source.getMatchedSegment().x1() + "_" + source.getMatchedSegment().y1() + "," + source.getMatchedSegment()
//				.x2() + "_" + source.getMatchedSegment().y2() + "," + source.getRoadID();
		
		String sourceLocID = source.getMatchedSegment().x1() + "_" + source.getMatchedSegment().y1() +
				"," + source.getMatchedSegment().x2() + "_" +
				source.getMatchedSegment().y2() + "," + source.getRoadID().strip().split("\\|")[0];
		
		if (!this.endPointLoc2EdgeIndex.containsKey(sourceLocID)) {
			LOG.error("Shortest distance calculation failed: Source node is not found: " + sourceLocID);
			result = new ArrayList<>(resultOutput(distance, path));
			return result;
		}
		
		// the start node of the current Dijkstra rotation
		int startEdgeIndex = endPointLoc2EdgeIndex.get(sourceLocID);
		String startRoadID = edgeIndex2RoadID.get(startEdgeIndex);
		int startNodeIndex = this.routingEdges[startEdgeIndex].getToNodeIndex();
		double sourceDistance = this.distFunc.distance(source.getMatchPoint(), source.getMatchedSegment().p2());
		
		// attach all destination points to the graph
		int destPointCount = pointList.size();
		for (int i = 0; i < pointList.size(); i++) {

//			String destLocID = pointList.get(i).getMatchedSegment().x1() + "_" + pointList.get(i).getMatchedSegment().y1() + "," +
//					pointList.get(i).getMatchedSegment().x2() + "_" + pointList.get(i).getMatchedSegment().y2() + "," + pointList.get(i)
//					.getRoadID();
			String destLocID = pointList.get(i).getMatchedSegment().x1() + "_" + pointList.get(i).getMatchedSegment().y1()
					+ "," + pointList.get(i).getMatchedSegment().x2() + "_" + pointList.get(i).getMatchedSegment().y2()
					+ "," + pointList.get(i).getRoadID().strip().split("\\|")[0];
			
			if (!endPointLoc2EdgeIndex.containsKey(destLocID)) {
				LOG.error("Destination node is not found: " + destLocID);
				destPointCount--;
//            } else if (pointList.get(i).getMatchPoint().equals2D(pointList.get(i).getMatchedSegment().p1())) {
//                destPointCount--;
			} else {
				int destEdgeIndex = endPointLoc2EdgeIndex.get(destLocID);
				String destRoadID = edgeIndex2RoadID.get(destEdgeIndex);
//				double candidateRange = prop.getPropertyDouble("algorithm.mapmatching.CandidateRange");
//				double backwardsFactor = prop.getPropertyDouble("algorithm.mapmatching.hmm.BackwardsFactor");
				if (destEdgeIndex == startEdgeIndex && distFunc.distance(source.getMatchPoint(), source.getMatchedSegment().p2()) >=
						distFunc.distance(pointList.get(i).getMatchPoint(), pointList.get(i).getMatchedSegment().p2())) {    // two segments
					// refer to the same mini edge and they are in the right order
					if (!startRoadID.equals(destRoadID))
						throw new IllegalArgumentException("Same mini edge occurred in different roads: " + destEdgeIndex + ".");
					distance[i] = distFunc.distance(source.getMatchPoint(), pointList.get(i).getMatchPoint());
//						path.get(i).add(pointList.get(i).getRoadID());
					path.get(i).add(destRoadID);
					destPointCount--;
//					} else if (destEdgeIndex == startEdgeIndex && distFunc.distance(source.getMatchPoint(),
//							pointList.get(i).getMatchPoint()) < candidateRange * backwardsFactor) {  // vehicle may stop on the road
//						// and the sampling point may be located backward, it is not initially inside the algorithm
//						distance[i] = 1.1 * distFunc.distance(source.getMatchPoint(), pointList.get(i).getMatchPoint());
//						path.get(i).add(pointList.get(i).getRoadID());
//						destPointCount--;
				} else {
					vertexID2DestIndexSet.put(this.routingEdges[destEdgeIndex].getFromNodeIndex(), i);
				}
			}
		}
		
		// the rest of the destinations are on different mini edges, now set the end of the current mini edge as start vertex
		if (destPointCount > 0) {
			// Dijkstra start node
			vertexDistFromSource.put(startNodeIndex, 0d);
			parent.put(startNodeIndex, startNodeIndex);
			MinPriorityQueue minHeap = new MinPriorityQueue();
			int currIndex = startNodeIndex;

//			LOG.info("start new shortest distance");
			// visit every node
			while (currIndex != -1 && vertexDistFromSource.get(currIndex) < (maxSearchDist - sourceDistance)) {
				// loop around the edges of current node
//				LOG.info(vertexDistFromSource.get(currIndex));
				List<RoutingEdge> currentOutgoingRoutingEdges = vertices[currIndex].getOutGoingRoutingEdges();
				for (RoutingEdge currEdge : currentOutgoingRoutingEdges) {
					int nextVertexIndex = currEdge.getToNodeIndex();
					double tentative = vertexDistFromSource.get(currIndex) + currEdge.getLength();
					if (!vertexVisited.contains(nextVertexIndex) && minHeap.decreaseKey(nextVertexIndex, tentative)) {
						vertexDistFromSource.put(nextVertexIndex, tentative);
						parent.put(nextVertexIndex, currIndex);
					}
				}
				// all neighbours checked so node visited
				vertexVisited.add(currIndex);
				if (vertexID2DestIndexSet.containsKey(currIndex)) {
					int i = vertexID2DestIndexSet.get(currIndex);
					destPointCount--;
					distance[i] = vertexDistFromSource.get(currIndex);
					distance[i] += sourceDistance;
					distance[i] += distFunc.distance(pointList.get(i).getMatchedSegment().p1(), pointList
							.get(i).getMatchPoint());
					if (sourceDistance != 0)
						path.get(i).add(startRoadID);
					path.get(i).addAll(findPath(currIndex, parent));
					if (path.get(i).size() > 1 && path.get(i).get(0).equals(path.get(i).get(1)))
						path.get(i).remove(1);    // remove the duplicated start road ID
					String lastRoadID = pointList.get(i).getRoadID().strip().split("\\|")[0];
					if (!path.get(i).isEmpty() && !lastRoadID.equals(path.get(i).get(path.get(i).size() - 1)))
						path.get(i).add(lastRoadID);
				}
				if (destPointCount == 0) {
					result = new ArrayList<>(resultOutput(distance, path));
					return result;
				}
				// next node must be with shortest distance
//                minHeap.buildMinHeap();
//                currNode = minHeap.extractMin().getIndex();
				currIndex = minHeap.extractMin();
//            if(index!=currNode){
//                double value1 = vertices[index].getDistanceFromSource();
//                double value2 = vertices[currNode].getDistanceFromSource();
//                LOG.info("Inconsistent index");
//            }
			}
//            LOG.info((pointList.size() - destPointCount) + "/" + pointList.size() + "distances are found.");
			result = new ArrayList<>(resultOutput(distance, path));
			return result;
		}
		result = new ArrayList<>(resultOutput(distance, path));
		return result;
	}
	
	/**
	 * Given a source match point and a set of destination points, the function calculate the shortest path to each destination and their
	 * distance using A* algorithm.
	 *
	 * @param source         The source match point and its segment.
	 * @param pointList      The destination match point list.
	 * @param referencePoint The point used to calculate heuristic reference distance.
	 * @param maxSearchDist  The maximum search range where shortest path search terminates.
	 * @return List of results which contain distance and shortest path. distance = Double.POSITIVE_INFINITY and path is empty if not
	 * reachable within maxSearchDist.
	 */
	public List<Pair<Double, List<String>>> calculateOneToNAStarSP(PointMatch source, List<PointMatch> pointList,
																   Point referencePoint, double maxSearchDist) {
		double[] distance = new double[pointList.size()];   // the distance to every destination
		List<List<String>> path = new ArrayList<>(pointList.size());     // the path to every destination
		HashMap<Integer, Integer> parent = new HashMap<>();        // the parent of each vertex, used during A* traversal
		HashMap<Integer, Double> vertexDistFromSource = new HashMap<>();    // the distance from the vertex to the source node
		HashSet<Integer> vertexVisited = new HashSet<>();        // set of vertices visited
		List<Pair<Double, List<String>>> result;
		
		Arrays.fill(distance, Double.POSITIVE_INFINITY);
		for (int i = 0; i < pointList.size(); i++) {
			path.add(new ArrayList<>());
		}
		// the variables have been initialized during the last calculation. Start the process right away
		HashMap<Integer, Integer> vertexID2DestIndexSet = new HashMap<>();        // all destinations that requires A* search,
		// (destination vertex ID and destination index in pointList)
		
		
		// if source point doesn't exist, return infinity to all distances
//		String sourceLocID = source.getMatchedSegment().x1() + "_" + source.getMatchedSegment().y1() + "," + source.getMatchedSegment()
//				.x2() + "_" + source.getMatchedSegment().y2() + "," + source.getRoadID();
		
		String sourceLocID = source.getMatchedSegment().x1() + "_" + source.getMatchedSegment().y1() +
				"," + source.getMatchedSegment().x2() + "_" +
				source.getMatchedSegment().y2() + "," + source.getRoadID().strip().split("\\|")[0];
		
		if (!this.endPointLoc2EdgeIndex.containsKey(sourceLocID)) {
			LOG.error("Shortest distance calculation failed: Source node is not found: " + sourceLocID);
			result = new ArrayList<>(resultOutput(distance, path));
			return result;
		}
		
		// the start node of the current A* rotation
		int startEdgeIndex = endPointLoc2EdgeIndex.get(sourceLocID);
		String startRoadID = edgeIndex2RoadID.get(startEdgeIndex);
		int startNodeIndex = this.routingEdges[startEdgeIndex].getToNodeIndex();
		double sourceDistance = this.distFunc.distance(source.getMatchPoint(), source.getMatchedSegment().p2());
		
		// attach all destination points to the graph
		int destPointCount = pointList.size();
		for (int i = 0; i < pointList.size(); i++) {

//			String destLocID = pointList.get(i).getMatchedSegment().x1() + "_" + pointList.get(i).getMatchedSegment().y1() + "," +
//					pointList.get(i).getMatchedSegment().x2() + "_" + pointList.get(i).getMatchedSegment().y2() + "," + pointList.get(i)
//					.getRoadID();
			String destLocID = pointList.get(i).getMatchedSegment().x1() + "_" + pointList.get(i).getMatchedSegment().y1()
					+ "," + pointList.get(i).getMatchedSegment().x2() + "_" + pointList.get(i).getMatchedSegment().y2()
					+ "," + pointList.get(i).getRoadID().strip().split("\\|")[0];
			
			if (!endPointLoc2EdgeIndex.containsKey(destLocID)) {
				LOG.error("Destination node is not found: " + destLocID);
				destPointCount--;
//            } else if (pointList.get(i).getMatchPoint().equals2D(pointList.get(i).getMatchedSegment().p1())) {
//                destPointCount--;
			} else {
				int destEdgeIndex = endPointLoc2EdgeIndex.get(destLocID);
				String destRoadID = edgeIndex2RoadID.get(destEdgeIndex);
//				double candidateRange = prop.getPropertyDouble("algorithm.mapmatching.CandidateRange");
//				double backwardsFactor = prop.getPropertyDouble("algorithm.mapmatching.hmm.BackwardsFactor");
				if (destEdgeIndex == startEdgeIndex && distFunc.distance(source.getMatchPoint(), source.getMatchedSegment().p2()) >=
						distFunc.distance(pointList.get(i).getMatchPoint(), pointList.get(i).getMatchedSegment().p2())) {    // two segments
					// refer to the same mini edge and they are in the right order
					if (!startRoadID.equals(destRoadID))
						throw new IllegalArgumentException("Same mini edge occurred in different roads: " + destEdgeIndex + ".");
					distance[i] = distFunc.distance(source.getMatchPoint(), pointList.get(i).getMatchPoint());
//						path.get(i).add(pointList.get(i).getRoadID());
					path.get(i).add(destRoadID);
					destPointCount--;
//					} else if (destEdgeIndex == startEdgeIndex && distFunc.distance(source.getMatchPoint(),
//							pointList.get(i).getMatchPoint()) < candidateRange * backwardsFactor) {  // vehicle may stop on the road
//						// and the sampling point may be located backward, it is not initially inside the algorithm
//						distance[i] = 1.1 * distFunc.distance(source.getMatchPoint(), pointList.get(i).getMatchPoint());
//						path.get(i).add(pointList.get(i).getRoadID());
//						destPointCount--;
				} else {
					vertexID2DestIndexSet.put(this.routingEdges[destEdgeIndex].getFromNodeIndex(), i);
				}
			}
		}
		
		// the rest of the destinations are on different mini edges, now set the end of the current mini edge as start vertex
		if (destPointCount > 0) {
			// A* start node
			double hDist;
			double distFromSource;
			double searchDist;
			int nextVertexIndex;
			vertexDistFromSource.put(startNodeIndex, 0d);
			parent.put(startNodeIndex, startNodeIndex);
			MinPriorityQueue minHeap = new MinPriorityQueue();
			int currIndex = startNodeIndex;

//			LOG.info("start new shortest distance");
			// visit every node
			while (currIndex != -1 && vertexDistFromSource.get(currIndex) < (maxSearchDist - sourceDistance)) {
				// loop around the edges of current node
//				LOG.info(vertexDistFromSource.get(currIndex));
				List<RoutingEdge> currentOutgoingRoutingEdges = vertices[currIndex].getOutGoingRoutingEdges();
				for (RoutingEdge currEdge : currentOutgoingRoutingEdges) {
					nextVertexIndex = currEdge.getToNodeIndex();
					distFromSource = vertexDistFromSource.get(currIndex) + currEdge.getLength();
					hDist = distFunc.distance(this.vertices[nextVertexIndex].getVertexPoint(), referencePoint);
					searchDist = distFromSource + hDist;
					if (!vertexVisited.contains(nextVertexIndex) && minHeap.decreaseKey(nextVertexIndex, searchDist)
							&& (!vertexDistFromSource.containsKey(nextVertexIndex) || vertexDistFromSource.get(nextVertexIndex) > distFromSource)) {
						vertexDistFromSource.put(nextVertexIndex, distFromSource);
						parent.put(nextVertexIndex, currIndex);
					}
				}
				// all neighbours checked so node visited
				vertexVisited.add(currIndex);
				if (vertexID2DestIndexSet.containsKey(currIndex)) {
					int i = vertexID2DestIndexSet.get(currIndex);
					destPointCount--;
					distance[i] = vertexDistFromSource.get(currIndex);
					distance[i] += sourceDistance;
					distance[i] += distFunc.distance(pointList.get(i).getMatchedSegment().p1(), pointList
							.get(i).getMatchPoint());
					if (sourceDistance != 0)
						path.get(i).add(startRoadID);
					path.get(i).addAll(findPath(currIndex, parent));
					if (path.get(i).size() > 1 && path.get(i).get(0).equals(path.get(i).get(1)))
						path.get(i).remove(1);    // remove the duplicated start road ID
					String lastRoadID = pointList.get(i).getRoadID().strip().split("\\|")[0];
					if (!path.get(i).isEmpty() && !lastRoadID.equals(path.get(i).get(path.get(i).size() - 1)))
						path.get(i).add(lastRoadID);
				}
				if (destPointCount == 0) {
					result = new ArrayList<>(resultOutput(distance, path));
					return result;
				}
				// next node must be with shortest distance
//                minHeap.buildMinHeap();
//                currNode = minHeap.extractMin().getIndex();
				currIndex = minHeap.extractMin();
//            if(index!=currNode){
//                double value1 = vertices[index].getDistanceFromSource();
//                double value2 = vertices[currNode].getDistanceFromSource();
//                LOG.info("Inconsistent index");
//            }
			}
//            LOG.info((pointList.size() - destPointCount) + "/" + pointList.size() + "distances are found.");
			result = new ArrayList<>(resultOutput(distance, path));
			return result;
		}
		result = new ArrayList<>(resultOutput(distance, path));
		return result;
	}
	
	private List<String> findPath(int index, HashMap<Integer, Integer> parent) {
		Set<String> roadIDSet = new LinkedHashSet<>();
		while (parent.get(index) != index) {
			if (parent.get(index) == -1)
				LOG.error("Road path is broken!");
			Pair<Integer, Integer> currentSegment = new Pair<>(parent.get(index), index);
			int edgeID = endPointsIndex2EdgeIndex.get(currentSegment);
			roadIDSet.add(edgeIndex2RoadID.get(edgeID));
			index = parent.get(index);
		}
		List<String> roadIDList = new ArrayList<>(roadIDSet);
		Collections.reverse(roadIDList);
		return roadIDList;
	}
	
	private List<Pair<Double, List<String>>> resultOutput(double[] distance, List<List<String>> path) {
		List<Pair<Double, List<String>>> result = new ArrayList<>();
		for (int i = 0; i < distance.length; i++) {
			result.add(new Pair<>(distance[i], path.get(i)));
		}
		return result;
	}
	
	public void addRoadByID(String roadID) {
		if (!roadID2NewEdgeList.containsKey(roadID) || !roadID2NewNodeList.containsKey(roadID))
			throw new IllegalArgumentException("ERROR! The road to be inserted has wrong ID: " + roadID);
		
		for (int i : roadID2NewNodeList.get(roadID))
			newNodeSet.remove(i);
		// add all the routingEdges to the vertices, each edge added to only from vertices
		for (int i : roadID2NewEdgeList.get(roadID)) {
			this.vertices[routingEdges[i].getFromNodeIndex()].getOutGoingRoutingEdges().add(routingEdges[i]);
			newEdgeSet.remove(i);
		}
	}
	
	public void removeRoadByID(String roadID) {
		if (!roadID2NewEdgeList.containsKey(roadID) || !roadID2NewNodeList.containsKey(roadID))
			throw new IllegalArgumentException("ERROR! The road to be removed has wrong ID.");
		
		newNodeSet.addAll(roadID2NewNodeList.get(roadID));
		// add all the routingEdges to the vertices, each edge added to only from vertices
		for (int i : roadID2NewEdgeList.get(roadID)) {
			this.vertices[routingEdges[i].getFromNodeIndex()].getOutGoingRoutingEdges().remove(routingEdges[i]);
		}
		newEdgeSet.addAll(roadID2NewEdgeList.get(roadID));
	}
}