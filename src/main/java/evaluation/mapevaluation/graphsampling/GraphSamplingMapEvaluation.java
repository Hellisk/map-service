package evaluation.mapevaluation.graphsampling;

import evaluation.mapevaluation.graphmatching.GraphMatchingMapEvaluation;
import org.apache.log4j.Logger;
import util.dijkstra.MinPriorityQueue;
import util.function.DistanceFunction;
import util.index.grid.Grid;
import util.index.grid.GridPartition;
import util.object.roadnetwork.RoadNetworkGraph;
import util.object.roadnetwork.RoadNode;
import util.object.roadnetwork.RoadWay;
import util.object.spatialobject.Point;
import util.object.structure.XYObject;

import java.util.*;

/**
 * Randomly sample points on ground-truth and output maps and extend them alongside the maps. Use precision/recall/F-score to measure the
 * point matches and the similarity between two maps. Method proposed in:
 * <p>
 * Biagioni, James, and Jakob Eriksson. "Inferring road maps from global positioning system traces: Survey and comparative evaluation."
 * Transportation research record 2291.1 (2012): 61-71.
 *
 * @author uqpchao
 * Created 12/06/2019
 */
public class GraphSamplingMapEvaluation {
	private static final Logger LOG = Logger.getLogger(GraphMatchingMapEvaluation.class);
	
	/**
	 * Use precision recall and F-score to measure the map accuracy based on graph sampling method.
	 *
	 * @param outputMap  The constructed map.
	 * @param gtMap      The ground-truth map.
	 * @param hopDist    The distance between two sampled points.
	 * @param radius     The maximum distance of a sampled point to its root.
	 * @param matchDist  The maximum distance to be regarded as match.
	 * @param numOfRoots The total number of seeds.
	 * @return The precision/recall/F-score results.
	 */
	public static String precisionRecallGraphSamplingMapEval(RoadNetworkGraph outputMap, RoadNetworkGraph gtMap, double hopDist,
															 double radius, double matchDist, int numOfRoots) {
		
		int maxRootDist = 5;    // the maximum allowable distance between the root points in ground-truth and output maps.
		if (outputMap.getDistanceFunction().getClass() != gtMap.getDistanceFunction().getClass())
			throw new IllegalArgumentException("Input map and ground-truth map has different coordinate system.");
		
		DistanceFunction distFunc = outputMap.getDistanceFunction();
		
		// two maps share the same region
		double maxLon = outputMap.getMaxLon() > gtMap.getMaxLon() ? outputMap.getMaxLon() : gtMap.getMaxLon();
		double maxLat = outputMap.getMaxLat() > gtMap.getMaxLat() ? outputMap.getMaxLat() : gtMap.getMaxLat();
		double minLon = outputMap.getMinLon() < gtMap.getMinLon() ? outputMap.getMinLon() : gtMap.getMinLon();
		double minLat = outputMap.getMinLat() < gtMap.getMinLat() ? outputMap.getMinLat() : gtMap.getMinLat();
		// build index for both output map and ground-truth map
		double lonDistance = distFunc.pointToPointDistance(maxLon, (maxLat + minLat) / 2, minLon, (maxLat + minLat) / 2);
		double latDistance = distFunc.pointToPointDistance((maxLon + minLon) / 2, maxLat, (maxLon + minLon) / 2, minLat);
		int columnNum = (int) Math.floor(lonDistance / maxRootDist);
		int rowNum = (int) Math.floor(latDistance / maxRootDist);
		Grid<Point> outputMapGrid = new Grid<>(columnNum, rowNum, minLon, minLat, maxLon, maxLat, distFunc);
		
		// insert road nodes (including mini nodes) to the index, each node contains its corresponding node and way information stored in
		// the array lists.
		List<XYObject<Point>> indexPointList = new ArrayList<>();
		List<RoadNode> allOutputNodeMapping = new ArrayList<>();    // for each index, the corresponding road node
		List<RoadWay> allOutputWayMapping = new ArrayList<>();    // for each index, the corresponding road way, = null if the node is an
		// intersection.
		int index = 0;
		for (RoadWay way : outputMap.getWays()) {
			for (int i = 1; i < way.getNodes().size() - 1; i++) {
				RoadNode currNode = way.getNode(i);
				allOutputNodeMapping.add(currNode);
				allOutputWayMapping.add(way);
				Point currPoint = currNode.toPoint();
				currPoint.setID(index + "");
				indexPointList.add(new XYObject<>(currPoint.x(), currPoint.y(), currPoint));
				index++;
			}
		}
		RoadWay emptyWay = new RoadWay("null", distFunc);
		for (RoadNode node : outputMap.getNodes()) {
			Point currPoint = node.toPoint();
			currPoint.setID(index + "");
			allOutputNodeMapping.add(node);
			allOutputWayMapping.add(emptyWay);
			indexPointList.add(new XYObject<>(currPoint.x(), currPoint.y(), currPoint));
			index++;
		}
		if (allOutputNodeMapping.size() != allOutputWayMapping.size() || allOutputNodeMapping.size() != indexPointList.size())
			throw new IllegalArgumentException("The size of node and way mapping is different: " + allOutputNodeMapping.size() + "," +
					allOutputWayMapping.size() + "," + indexPointList.size());
		outputMapGrid.insertAll(indexPointList);
		
		Random random = new Random(30);
		int totalGTSampleCount = 0;
		int totalOutputSampleCount = 0;
		int totalMatchedMarbleHoleCount = 0;
		int missingMatchCount = 0;
		for (int rootCount = 0; rootCount < numOfRoots; rootCount++) {
			RoadWay gtRootWay = gtMap.getWay(random.nextInt(gtMap.getWays().size()));
			RoadNode currGTRoot;
			double currGTHeading;
			List<Point> currHoleSampleList;
			if (gtRootWay.size() > 2) {
				int position = 1 + random.nextInt(gtRootWay.size() - 2);
				currGTRoot = gtRootWay.getNode(position);    // avoid selecting intersections
				RoadNode nextNode = gtRootWay.getNode(position + 1);
				currGTHeading = distFunc.getHeading(currGTRoot.lon(), currGTRoot.lat(), nextNode.lon(), nextNode.lat());
				currHoleSampleList = mapTraverse(gtMap, currGTRoot, gtRootWay, hopDist, radius);
			} else {    // node with no mini point
				currGTRoot = gtRootWay.getFromNode();
				currGTHeading = distFunc.getHeading(currGTRoot.lon(), currGTRoot.lat(), gtRootWay.getToNode().lon(), gtRootWay.getToNode().lat());
				currHoleSampleList = mapTraverse(gtMap, currGTRoot, null, hopDist, radius);
			}
			
			// find the corresponding root on the output map
			List<Point> candidatePointList = new ArrayList<>();
			GridPartition<Point> outputCandidatePartition = outputMapGrid.partitionSearch(currGTRoot.lon(), currGTRoot.lat());
			if (outputCandidatePartition != null) {
				for (XYObject<Point> pointXYObject : outputCandidatePartition.getObjectsList()) {
					candidatePointList.add(pointXYObject.getSpatialObject());
				}
			}
			List<GridPartition<Point>> adjacentPartitionList = outputMapGrid.adjacentPartitionSearch(currGTRoot.lon(), currGTRoot.lat());
			for (GridPartition<Point> partition : adjacentPartitionList) {
				if (partition != null && !partition.isEmpty()) {
					for (XYObject<Point> point : partition.getObjectsList()) {
						candidatePointList.add(point.getSpatialObject());
					}
				}
			}
			double minDist = Double.POSITIVE_INFINITY;
			RoadNode currOutputRoot = null;
			RoadWay outputRootWay = null;
			double minHeadingDiff = 180;
			for (Point pointObject : candidatePointList) {
				if (pointObject != null) {
					if (distFunc.distance(pointObject, currGTRoot.toPoint()) < minDist) {
						int position = Integer.parseInt(pointObject.getID());
						currOutputRoot = allOutputNodeMapping.get(position);
						minDist = distFunc.distance(pointObject, currGTRoot.toPoint());
						if (allOutputWayMapping.get(position).equals(emptyWay)) {    // it is an intersection
							outputRootWay = null;
							minHeadingDiff = 180;
						} else {
							outputRootWay = allOutputWayMapping.get(position);
							int pointIndex = -1;
							for (int i = 0; i < outputRootWay.size(); i++) {
								if (outputRootWay.getNode(i).equals(currOutputRoot)) {
									pointIndex = i;
									break;
								}
							}
							if (pointIndex == -1)
								throw new IllegalArgumentException("The root node is not on the road way given.");
							double currOutputHeading = distFunc.getHeading(currOutputRoot.lon(), currOutputRoot.lat(),
									outputRootWay.getNode(pointIndex + 1).lon(), outputRootWay.getNode(pointIndex + 1).lat());
							minHeadingDiff = currGTHeading - currOutputHeading;
							if (minHeadingDiff < 0) {
								minHeadingDiff += 360;
							}
						}
					} else if (distFunc.distance(pointObject, currGTRoot.toPoint()) == minDist) {    // can potentially replace the
						// current candidate, check the heading difference
						int position = Integer.parseInt(pointObject.getID());
						if (!allOutputWayMapping.get(position).equals(emptyWay)) {    // it is not an intersection
							RoadNode currNode = allOutputNodeMapping.get(position);
							RoadWay currWay = allOutputWayMapping.get(position);
							int pointIndex = -1;
							for (int i = 0; i < currWay.size(); i++) {
								if (currWay.getNode(i).equals(currNode)) {
									pointIndex = i;
									break;
								}
							}
							if (pointIndex == -1)
								throw new IllegalArgumentException("The root node is not on the road way given.");
							double currOutputHeading = distFunc.getHeading(currNode.lon(), currNode.lat(),
									currWay.getNode(pointIndex + 1).lon(), currWay.getNode(pointIndex + 1).lat());
							double angle = currGTHeading - currOutputHeading;
							if (angle < 0) {
								angle += 360;
							}
							if (angle < minHeadingDiff) {
								outputRootWay = currWay;
								minHeadingDiff = angle;
								currOutputRoot = allOutputNodeMapping.get(position);
							}
						}
					}
				}
			}
			
			if (currOutputRoot == null) {
				LOG.warn("The current root " + currGTRoot.getID() + " does not have correspondence in output map.");
				missingMatchCount++;
				totalGTSampleCount += currHoleSampleList.size();
				continue;
			}
			List<Point> currMarbleSampleList = mapTraverse(outputMap, currOutputRoot, outputRootWay, hopDist, radius);
			totalOutputSampleCount += currMarbleSampleList.size();
			totalGTSampleCount += currHoleSampleList.size();
			// find the matched marbles
			for (Point marble : currMarbleSampleList) {
				boolean isMatched = false;
				for (Point hole : currHoleSampleList) {
					if (distFunc.distance(marble, hole) < matchDist) {
						isMatched = true;
						break;
					}
				}
				if (isMatched)
					totalMatchedMarbleHoleCount++;
			}
			System.out.println(rootCount + " seeds processed. Total seed required: " + numOfRoots);
		}
		
		double precision = totalMatchedMarbleHoleCount / (double) totalOutputSampleCount;
		double recall = totalMatchedMarbleHoleCount / (double) totalGTSampleCount;
		double fScore = 2 * precision * recall / (precision + recall);
		
		LOG.info("Graph sampling evaluation done. Total missing seed match: " + missingMatchCount);
		LOG.info("Precision=" + precision + ", edge recall=" + recall + ", edge F-score=" + fScore);
		
		return precision + ", " + recall + ", " + fScore;
	}
	
	/**
	 * Find all sample points given a root on the map.
	 *
	 * @param inputMap The underlying map.
	 * @param currRoot The current root to start with.
	 * @param rootWay  The way that the root located.
	 * @param hopDist  The distance for each hop.
	 * @param radius   The maximum distance between a sample to its root.
	 * @return List of sample points.
	 */
	private static List<Point> mapTraverse(RoadNetworkGraph inputMap, RoadNode currRoot, RoadWay rootWay, double hopDist, double radius) {
		
		DistanceFunction distFunc = inputMap.getDistanceFunction();
		Set<String> traversedWaySet = new HashSet<>();    // road way ids that has been inserted to queues. All ways should be traversed
		// by at most once.
		Set<String> traversedForwardNodeIDSet = new HashSet<>();    // road way index (in forwardQueueNodeList) that has been inserted to queues
		MinPriorityQueue forwardTraversalQueue = new MinPriorityQueue();
		Map<Integer, Double> forwardIndex2DistanceMap = new HashMap<>();    // for each node visited, its forward index and its distance to
		// root
		List<RoadNode> forwardQueueNodeList = new ArrayList<>();    // node list that has been visited at least once.
		Set<String> traversedBackwardNodeIDSet = new HashSet<>();    // road way index (in backwardQueueNodeList) that has been inserted
		// to queues
		Map<Integer, Double> backwardIndex2DistanceMap = new HashMap<>();    // for each node visited, its forward index and its distance
		// to root
		List<RoadNode> backwardQueueNodeList = new ArrayList<>();    // node list that has been visited at least once.
		MinPriorityQueue backwardTraversalQueue = new MinPriorityQueue();
		List<Point> resultPointList = new ArrayList<>();
		RoadNode currIntersection;
		double remainingLength = 0;    // the remaining length after last point is extracted
		double distanceToRoot = 0;    // the distance from current node to the root
		
		// forward traversal
		if (rootWay != null) {
			traversedWaySet.add(rootWay.getID());
			List<RoadNode> nodes = rootWay.getNodes();
			int pointIndex = -1;
			for (int i = 0; i < nodes.size(); i++) {
				if (nodes.get(i).equals(currRoot)) {
					pointIndex = i;
					break;
				}
			}
			if (pointIndex == -1)
				throw new IllegalArgumentException("The root point is not on the road way given.");
			for (int j = pointIndex; pointIndex < nodes.size() - 1; pointIndex++) {
				double currEdgeDist = distFunc.distance(nodes.get(j).toPoint(), nodes.get(j + 1).toPoint());
				remainingLength += currEdgeDist;
				while (remainingLength >= hopDist && distanceToRoot < radius) {
					remainingLength -= hopDist;
					distanceToRoot += hopDist;
					double ratio = remainingLength / currEdgeDist;
					double currLon = nodes.get(j + 1).lon() - (nodes.get(j + 1).lon() - nodes.get(j).lon()) * ratio;
					double currLat = nodes.get(j + 1).lat() - (nodes.get(j + 1).lat() - nodes.get(j).lat()) * ratio;
					resultPointList.add(new Point(currLon, currLat, distFunc));
				}
			}
			currIntersection = rootWay.getToNode();
		} else {    // current node is an intersection
			currIntersection = currRoot;
		}
		forwardQueueNodeList.add(currIntersection);
		int startIndex = forwardQueueNodeList.size() - 1;
		forwardIndex2DistanceMap.put(startIndex, distanceToRoot);
		forwardTraversalQueue.decreaseKey(startIndex, distanceToRoot);
		while (!forwardTraversalQueue.isEmpty()) {    // continue traversing the map
			int currNodeIndex = forwardTraversalQueue.extractMin();
			distanceToRoot = forwardIndex2DistanceMap.get(currNodeIndex);
			for (RoadWay currWay : currIntersection.getOutGoingWayList()) {
				RoadNode endNode = currWay.getToNode();
				if (!traversedWaySet.contains(currWay.getID())) {    // current way is not traversed yet
					// register the end node of the current way
					double currDistance = distanceToRoot + currWay.getLength();
					traversedWaySet.add(currWay.getID());
					if (currDistance < radius && !traversedForwardNodeIDSet.contains(endNode.getID())) {
						// still within the circle after traversing this road
						if (forwardQueueNodeList.contains(endNode)) {    // the node is in the queue but not visited yet
							int currIndex = forwardQueueNodeList.indexOf(endNode);
							forwardTraversalQueue.decreaseKey(currIndex, currDistance);
							double prevDistance = forwardIndex2DistanceMap.get(currIndex);
							forwardIndex2DistanceMap.put(currIndex, prevDistance > currDistance ? currDistance : prevDistance);
						} else {
							forwardQueueNodeList.add(endNode);
							int currIndex = forwardQueueNodeList.size() - 1;
							forwardTraversalQueue.decreaseKey(currIndex, currDistance);
							forwardIndex2DistanceMap.put(currIndex, currDistance);
						}
						
					}
					// start generating points
					remainingLength = distanceToRoot % hopDist;
					double currDistanceToRoot = distanceToRoot;
					for (int i = 0; i < currWay.size() - 1; i++) {
						double currEdgeDist = distFunc.distance(currWay.getNode(i).toPoint(), currWay.getNode(i + 1).toPoint());
						remainingLength += currEdgeDist;
						while (remainingLength >= hopDist && currDistanceToRoot < radius) {
							remainingLength -= hopDist;
							currDistanceToRoot += hopDist;
							double ratio = remainingLength / currEdgeDist;
							double currLon =
									currWay.getNode(i + 1).lon() - (currWay.getNode(i + 1).lon() - currWay.getNode(i).lon()) * ratio;
							double currLat =
									currWay.getNode(i + 1).lat() - (currWay.getNode(i + 1).lat() - currWay.getNode(i).lat()) * ratio;
							resultPointList.add(new Point(currLon, currLat, distFunc));
						}
					}
				}
			}
			traversedForwardNodeIDSet.add(forwardQueueNodeList.get(currNodeIndex).getID());
		}
		
		// backward traversal
		remainingLength = 0;
		distanceToRoot = 0;
		if (rootWay != null) {
			List<RoadNode> nodes = rootWay.getNodes();
			int pointIndex = -1;
			for (int i = 0; i < nodes.size(); i++) {
				if (nodes.get(i).equals(currRoot)) {
					pointIndex = i;
					break;
				}
			}
			if (pointIndex == -1)
				throw new IllegalArgumentException("The root point is not on the road way given.");
			for (int j = pointIndex; pointIndex > 0; pointIndex--) {
				double currEdgeDist = distFunc.distance(nodes.get(j).toPoint(), nodes.get(j - 1).toPoint());
				remainingLength += currEdgeDist;
				while (remainingLength >= hopDist && distanceToRoot < radius) {
					remainingLength -= hopDist;
					distanceToRoot += hopDist;
					double ratio = remainingLength / currEdgeDist;
					double currLon = nodes.get(j - 1).lon() - (nodes.get(j - 1).lon() - nodes.get(j).lon()) * ratio;
					double currLat = nodes.get(j - 1).lat() - (nodes.get(j - 1).lat() - nodes.get(j).lat()) * ratio;
					resultPointList.add(new Point(currLon, currLat, distFunc));
				}
			}
			currIntersection = rootWay.getFromNode();
		} else {    // current node is an intersection
			currIntersection = currRoot;
		}
		backwardQueueNodeList.add(currIntersection);
		startIndex = backwardQueueNodeList.size() - 1;
		backwardIndex2DistanceMap.put(startIndex, distanceToRoot);
		backwardTraversalQueue.decreaseKey(startIndex, distanceToRoot);
		while (!backwardTraversalQueue.isEmpty()) {    // continue traversing the map
			int currNodeIndex = backwardTraversalQueue.extractMin();
			distanceToRoot = backwardIndex2DistanceMap.get(currNodeIndex);
			for (RoadWay currWay : currIntersection.getInComingWayList()) {
				RoadNode endNode = currWay.getFromNode();
				double currDistance = distanceToRoot + currWay.getLength();
				if (!traversedWaySet.contains(currWay.getID())) {    // current way is not traversed yet
					// register the end node of the current way
					traversedWaySet.add(currWay.getID());
					if (currDistance < radius && !traversedBackwardNodeIDSet.contains(endNode.getID())) {
						// still within the circle after traversing this road
						if (backwardQueueNodeList.contains(endNode)) {    // the node is in the queue but not visited yet
							int currIndex = backwardQueueNodeList.indexOf(endNode);
							backwardTraversalQueue.decreaseKey(currIndex, currDistance);
							double prevDistance = backwardIndex2DistanceMap.get(currIndex);
							backwardIndex2DistanceMap.put(currIndex, prevDistance > currDistance ? currDistance : prevDistance);
						} else {
							backwardQueueNodeList.add(endNode);
							int currIndex = backwardQueueNodeList.size() - 1;
							backwardTraversalQueue.decreaseKey(currIndex, currDistance);
							backwardIndex2DistanceMap.put(currIndex, currDistance);
						}
					}
					// start generating points
					remainingLength = distanceToRoot % hopDist;
					double currDistanceToRoot = distanceToRoot;
					for (int i = currWay.size() - 1; i > 0; i--) {
						double currEdgeDist = distFunc.distance(currWay.getNode(i).toPoint(), currWay.getNode(i - 1).toPoint());
						remainingLength += currEdgeDist;
						while (remainingLength >= hopDist && currDistanceToRoot < radius) {
							remainingLength -= hopDist;
							currDistanceToRoot += hopDist;
							double ratio = remainingLength / currEdgeDist;
							double currLon =
									currWay.getNode(i - 1).lon() - (currWay.getNode(i - 1).lon() - currWay.getNode(i).lon()) * ratio;
							double currLat =
									currWay.getNode(i - 1).lat() - (currWay.getNode(i - 1).lat() - currWay.getNode(i).lat()) * ratio;
							resultPointList.add(new Point(currLon, currLat, distFunc));
						}
					}
				} else if (currDistance < radius && !traversedBackwardNodeIDSet.contains(endNode.getID())) {
					// skip the current road way but continue the search as the end point is still within the circle
					if (backwardQueueNodeList.contains(endNode)) {    // the node is in the queue but not visited yet
						int currIndex = backwardQueueNodeList.indexOf(endNode);
						backwardTraversalQueue.decreaseKey(currIndex, currDistance);
						double prevDistance = backwardIndex2DistanceMap.get(currIndex);
						backwardIndex2DistanceMap.put(currIndex, prevDistance > currDistance ? currDistance : prevDistance);
					} else {
						backwardQueueNodeList.add(endNode);
						int currIndex = backwardQueueNodeList.size() - 1;
						backwardTraversalQueue.decreaseKey(currIndex, currDistance);
						backwardIndex2DistanceMap.put(currIndex, currDistance);
					}
				}
			}
			traversedBackwardNodeIDSet.add(backwardQueueNodeList.get(currNodeIndex).getID());
		}
		return resultPointList;
	}
}
