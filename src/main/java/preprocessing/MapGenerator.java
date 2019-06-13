package preprocessing;

import util.function.DistanceFunction;
import util.index.grid.Grid;
import util.index.grid.GridPartition;
import util.object.roadnetwork.RoadNetworkGraph;
import util.object.roadnetwork.RoadNode;
import util.object.roadnetwork.RoadWay;
import util.object.spatialobject.Point;
import util.object.spatialobject.Segment;
import util.object.structure.XYObject;

import java.text.DecimalFormat;
import java.util.*;

/**
 * Generate synthetic maps for measures test. The error types contains: Topological Error (TE), geographical Error (GE), Road Loss (RL),
 * Spurious Road (SR), zigzag Road Shape Error (RSE), Intersection Layout Error (ILE)
 *
 * @author uqpchao
 * Created 31/05/2019
 */
public class MapGenerator {
	private static double CANDIDATE_RANGE = 5;
	
	/**
	 * Generate map which contains only topological errors.
	 *
	 * @param originalGraph    Original road map.
	 * @param percentage       The percentage of errors.
	 * @param isCompleteRandom The randomness is based on complete random or weighted random on road visit frequency.
	 * @return The returning error map.
	 */
	public static RoadNetworkGraph topoErrorGenerator(RoadNetworkGraph originalGraph, double percentage, boolean isCompleteRandom) {
		List<RoadWay> candidateWayList;
		if (isCompleteRandom)
			candidateWayList = completeRandomWayList(originalGraph, percentage);
		else
			candidateWayList = weightedRandomWayList(originalGraph, percentage);
		
		Set<RoadWay> removedWayList = new LinkedHashSet<>();
		Set<String> completeWaySet = new HashSet<>();    // roads that has been processed by its reverse direction
		Set<String> insertNodeIDSet = new HashSet<>();
		for (RoadWay currWay : candidateWayList) {
			if (completeWaySet.contains(currWay.getID()))
				continue;
			if (currWay.getLength() < CANDIDATE_RANGE) {
				removedWayList.add(currWay);
				continue;
			}
			// remove the connection
			RoadNode prevIntersection = currWay.getToNode();
			prevIntersection.removeInComingWayFromList(currWay);
			String replaceID = currWay.getToNode().getID() + "+";
			while (insertNodeIDSet.contains(replaceID))
				replaceID = replaceID + "+";
			Segment lastSegment = currWay.getEdges().get(currWay.size() - 2);    // the last segment
			currWay.getNodes().remove(currWay.getToNode());
			RoadNode newIntersection;
			if (lastSegment.length() < CANDIDATE_RANGE) {
				newIntersection = currWay.getToNode();
			} else {
				double ratio = (lastSegment.length() - CANDIDATE_RANGE) / lastSegment.length();
				double replaceX = lastSegment.x2() - (lastSegment.x2() - lastSegment.x1()) * ratio;
				double replaceY = lastSegment.y2() - (lastSegment.y2() - lastSegment.y1()) * ratio;
				newIntersection = new RoadNode(replaceID, replaceX, replaceY, currWay.getDistanceFunction());
			}
			newIntersection.setId(replaceID);
			newIntersection.addInComingWay(currWay);
			currWay.addNode(newIntersection);
			completeWaySet.add(currWay.getID());
			// find if the reverse road exists, remove the corresponding point as well
			RoadWay reverseWay = null;
			for (RoadWay way : prevIntersection.getOutGoingWayList()) {
				if (way.getToNode().toPoint().equals2D(currWay.getFromNode().toPoint())) {
					reverseWay = way;
					way.getNodes().remove(0);
					List<RoadNode> nodeList = new ArrayList<>();
					if (newIntersection.toPoint().equals2D(way.getFromNode().toPoint())) {
						way.getNodes().remove(0);
					}
					nodeList.add(newIntersection);
					nodeList.addAll(way.getNodes());
					way.setNodes(nodeList);
					way.getFromNode().addOutGoingWay(way);
					completeWaySet.add(way.getID());
					break;
				}
			}
			if (reverseWay != null)
				prevIntersection.removeOutGoingWayFromList(reverseWay);
			originalGraph.addNode(newIntersection);
			insertNodeIDSet.add(newIntersection.getID());
		}
		originalGraph.removeRoadWayList(removedWayList);
		return originalGraph;
	}
	
	/**
	 * Generate map which contains only geographical errors. It is always complete random.
	 *
	 * @param originalMap Original road map.
	 * @param percentage  The percentage of errors.
	 * @param radius      The maximum distance between the error position and the original position.
	 * @return The returning error map.
	 */
	public static RoadNetworkGraph geoErrorMapGenerator(RoadNetworkGraph originalMap, double percentage, double radius) {
		// randomly select intersections
		Set<Integer> selectedIndex = new LinkedHashSet<>();
		DecimalFormat decFor = new DecimalFormat("0.00000");
		DistanceFunction distFunc = originalMap.getDistanceFunction();
		Random random = new Random(10);
		int nodeSize = originalMap.getNodes().size();
		Map<String, RoadNode> nodeLocationSet = new HashMap<>();    // store all intersection locations to avoid overlapping
		// nodes
		for (RoadNode node : originalMap.getNodes()) {
			nodeLocationSet.put(node.lon() + "_" + node.lat(), node);
		}
		while (selectedIndex.size() < nodeSize * percentage / 100.0) {
			selectedIndex.add(random.nextInt(nodeSize));
		}
		
		for (Integer index : selectedIndex) {
			// find the new location of the intersection
			RoadNode originalNode = originalMap.getNode(index);
			double originalX = originalNode.lon();
			double originalY = originalNode.lat();
			double newX, newY;
			do {
				double distance = random.nextDouble() * radius;
				double angle = random.nextDouble() * 2 * Math.PI;
				newX = Double.parseDouble(decFor.format(originalNode.lon() + distFunc.getCoordinateOffsetX(distance * Math.cos(angle),
						originalNode.lat())));
				newY = Double.parseDouble(decFor.format(originalNode.lat() + distFunc.getCoordinateOffsetY(distance * Math.sin(angle),
						originalNode.lon())));
			} while (nodeLocationSet.containsKey(newX + "_" + newY));
			nodeLocationSet.put(newX + "_" + newY, originalNode);
			originalNode.setLocation(newX, newY);
			for (RoadWay way : originalNode.getInComingWayList()) {
				// adjust the location of each mini node, the start nodes are fixed.
				RoadNode startNode = way.getFromNode();
				List<RoadNode> nodes = way.getNodes();
				for (int i = 1; i < nodes.size() - 1; i++) {
					RoadNode node = nodes.get(i);
					double ratio = distFunc.distance(node.toPoint(), startNode.toPoint()) / distFunc.pointToPointDistance(originalX,
							originalY, startNode.lon(), startNode.lat());
					double newMiniX = startNode.lon() + ratio * (newX - startNode.lon());
					double newMiniY = startNode.lat() + ratio * (newY - startNode.lat());
					node.setLocation(newMiniX, newMiniY);
				}
			}
			for (RoadWay way : originalNode.getOutGoingWayList()) {
				// adjust the location of each mini node, the end nodes are fixed.
				RoadNode endNode = way.getToNode();
				List<RoadNode> nodes = way.getNodes();
				for (int i = 1; i < nodes.size() - 1; i++) {
					RoadNode node = nodes.get(i);
					double ratio = distFunc.distance(node.toPoint(), endNode.toPoint()) / distFunc.pointToPointDistance(originalX,
							originalY, endNode.lon(), endNode.lat());
					double newMiniX = endNode.lon() + ratio * (newX - endNode.lon());
					double newMiniY = endNode.lat() + ratio * (newY - endNode.lat());
					node.setLocation(newMiniX, newMiniY);
				}
			}
		}
		return originalMap;
	}
	
	/**
	 * Generate map which loses a certain percentage of the roads.
	 *
	 * @param originalMap      Original road map.
	 * @param percentage       The percentage of loses.
	 * @param isCompleteRandom The randomness is based on complete random or weighted random on road visit frequency.
	 * @return The returning error map.
	 */
	public static RoadNetworkGraph roadLossErrorMapGenerator(RoadNetworkGraph originalMap, double percentage, boolean isCompleteRandom) {
		List<RoadWay> candidateWayList;
		if (isCompleteRandom)
			candidateWayList = completeRandomWayList(originalMap, percentage);
		else
			candidateWayList = weightedRandomWayList(originalMap, percentage);
		originalMap.removeRoadWayList(candidateWayList);
		return originalMap;
	}
	
	/**
	 * Generate map which contains spurious roads.
	 *
	 * @param originalMap Original road map.
	 * @param percentage  The percentage of errors.
	 * @return The returning error map.
	 */
	public static RoadNetworkGraph spuriousRoadErrorMapGenerator(RoadNetworkGraph originalMap, double percentage) {
		double totalMapLength = 0;
		for (RoadWay way : originalMap.getWays()) {
			totalMapLength += way.getLength();
		}
		double averageLength = totalMapLength / originalMap.getWays().size();
		
		DistanceFunction distFunc = originalMap.getDistanceFunction();
		
		// build index for spurious road search
		double lonDistance = distFunc.pointToPointDistance(originalMap.getMaxLon(), 0d, originalMap.getMinLon(), 0d);
		double latDistance = distFunc.pointToPointDistance(0d, originalMap.getMaxLat(), 0d, originalMap.getMinLat());
		int columnNum = (int) Math.round(lonDistance / (2 * averageLength));
		int rowNum = (int) Math.round(latDistance / (2 * averageLength));
		Grid<Point> grid = new Grid<>(columnNum + 2, rowNum + 2, originalMap.getMinLon(), originalMap.getMinLat(),
				originalMap.getMaxLon(), originalMap.getMaxLat(), distFunc);
		
		// insert road nodes (including mini nodes) to the index, each node contains its corresponding node and way information stored in
		// the array lists.
		List<XYObject<Point>> indexPointList = new ArrayList<>();
		List<RoadNode> allNodeMapping = new ArrayList<>();    // for each index, the corresponding road node
		List<RoadWay> allWayMapping = new ArrayList<>();    // for each index, the corresponding road way, = null if the node is an
		// intersection.
		int index = 0;
		for (RoadWay way : originalMap.getWays()) {
			for (int i = 1; i < way.getNodes().size() - 1; i++) {
				RoadNode currNode = way.getNode(i);
				allNodeMapping.add(currNode);
				allWayMapping.add(way);
				Point currPoint = currNode.toPoint();
				currPoint.setID(index + "");
				indexPointList.add(new XYObject<>(currPoint.x(), currPoint.y(), currPoint));
				index++;
			}
		}
		RoadWay emptyWay = new RoadWay("null", distFunc);
		for (RoadNode node : originalMap.getNodes()) {
			Point currPoint = node.toPoint();
			currPoint.setID(index + "");
			allNodeMapping.add(node);
			allWayMapping.add(emptyWay);
			indexPointList.add(new XYObject<>(currPoint.x(), currPoint.y(), currPoint));
			index++;
		}
		if (allNodeMapping.size() != allWayMapping.size() || allNodeMapping.size() != indexPointList.size())
			throw new IllegalArgumentException("The size of node and way mapping is different: " + allNodeMapping.size() + "," +
					allWayMapping.size() + "," + indexPointList.size());
		grid.insertAll(indexPointList);
		
		Random random = new Random(20);
		double spuriousRoadLength = 0;
		Set<String> selectedPairSet = new HashSet<>();    // store all pairs that have been checked (not necessarily valid) to avoid double
		Set<String> addedWayIDSet = new HashSet<>();    // avoid roads with the same id, it can happen when one road is split twice
		Set<String> removedWayIDSet = new HashSet<>();    // avoide removing the same road twice
		// check
		List<RoadWay> newWayList = new ArrayList<>();
		List<RoadNode> newNodeList = new ArrayList<>();
		List<RoadWay> removedWayList = new ArrayList<>();
		while (spuriousRoadLength < totalMapLength * percentage / 100) {
			int position = random.nextInt(rowNum * columnNum);
			int row = position / rowNum;
			int column = position % rowNum;
			if (grid.get(row, column) == null || grid.get(row, column).isEmpty())
				continue;
			List<XYObject<Point>> centreCandidateList = grid.get(row, column).getObjectsList();
			List<GridPartition<Point>> adjacentPartitionList = grid.adjacentPartitionSearch(row, column);
			List<XYObject<Point>> adjacentCandidateList = new ArrayList<>();
			for (GridPartition<Point> partition : adjacentPartitionList) {
				if (partition != null && !partition.isEmpty())
					adjacentCandidateList.addAll(partition.getObjectsList());
			}
			if (adjacentCandidateList.isEmpty())
				continue;
			Point startPoint = centreCandidateList.get(random.nextInt(centreCandidateList.size())).getSpatialObject();
			Point endPoint = adjacentCandidateList.get(random.nextInt(adjacentCandidateList.size())).getSpatialObject();
			if (!selectedPairSet.contains(startPoint.getID()) && !selectedPairSet.contains(endPoint.getID())) {    // current point pair
				// is never touched
				selectedPairSet.add(startPoint.getID());
				selectedPairSet.add(endPoint.getID());
				boolean isValidPair = true;
				RoadWay startWay = allWayMapping.get(Integer.parseInt(startPoint.getID()));
				RoadWay endWay = allWayMapping.get(Integer.parseInt(endPoint.getID()));
				RoadNode startNode = allNodeMapping.get(Integer.parseInt(startPoint.getID()));
				RoadNode endNode = allNodeMapping.get(Integer.parseInt(endPoint.getID()));
				// check if the two points are on the same way or not, if so, ignore the current pair
				if (startWay.equals(emptyWay)) {    // start node is an intersection
					if (endWay.equals(emptyWay)) {    // two nodes are all intersection nodes
						for (RoadWay way : startNode.getOutGoingWayList()) {
							if (endNode.getInComingWayList().contains(way)) {    // two nodes are already connected, discard this pair
								isValidPair = false;
								break;
							}
						}
					} else {
						if (startNode.getOutGoingWayList().contains(endWay) || startNode.getInComingWayList().contains(endWay)
								|| removedWayIDSet.contains(endWay.getID()))
							isValidPair = false;
					}
				} else {    // start node is not an intersection
					if (endWay.equals(emptyWay)) {
						if (endNode.getInComingWayList().contains(startWay) || endNode.getOutGoingWayList().contains(startWay)
								|| removedWayIDSet.contains(startWay.getID()))
							isValidPair = false;
					} else {
						String reverseStartID = startWay.getID().charAt(0) == '-' ? startWay.getID().substring(1) :
								"-" + startWay.getID();
						if (startWay.getID().equals(endWay.getID()) || reverseStartID.equals(endWay.getID())
								|| removedWayIDSet.contains(startWay.getID()) || removedWayIDSet.contains(endWay.getID()))
							isValidPair = false;
					}
				}
				if (isValidPair) {
					spuriousRoadLength += distFunc.distance(startPoint, endPoint);
					
					if (!startWay.equals(emptyWay)) {    // split the current roads and add new roads
						startWay.getFromNode().removeOutGoingWayFromList(startWay);
						startWay.getToNode().removeInComingWayFromList(startWay);
						removedWayList.add(startWay);
						removedWayIDSet.add(startWay.getID());
						newWayList.addAll(startWay.splitAtNode(startNode));
						newNodeList.add(startNode);
					}
					if (!endWay.equals(emptyWay)) {    // split the current roads and add new roads
						endWay.getFromNode().removeOutGoingWayFromList(endWay);
						endWay.getToNode().removeInComingWayFromList(endWay);
						removedWayList.add(endWay);
						removedWayIDSet.add(endWay.getID());
						newWayList.addAll(endWay.splitAtNode(endNode));
						newNodeList.add(endNode);
					}
					List<RoadNode> spuriousNodeList = new ArrayList<>();
					spuriousNodeList.add(startNode);
					spuriousNodeList.add(endNode);
					RoadWay spuriousWay = new RoadWay(startNode.getID() + "_" + endNode.getID(), spuriousNodeList, distFunc);
					newWayList.add(spuriousWay);
//						count++;
				}
			}
		}
		originalMap.addNodes(newNodeList);
		originalMap.removeRoadWayList(removedWayList);
		for (RoadWay currWay : newWayList) {
			while (addedWayIDSet.contains(currWay.getID())) {
				currWay.setId(currWay.getID() + "+");
			}
			addedWayIDSet.add(currWay.getID());
		}
		originalMap.addWays(newWayList);
		return originalMap;
	}
	
	/**
	 * Generate map which contains only zigzag road shapes.
	 *
	 * @param originalMap Original road map.
	 * @param percentage  The percentage of errors.
	 * @param distance    The fixed distance between the error position and the original position. It is also the interval length
	 *                    between two noisy point
	 * @return The returning error map.
	 */
	public static RoadNetworkGraph roadShapeErrorMapGenerator(RoadNetworkGraph originalMap, double percentage, double distance) {
		DistanceFunction distFunc = originalMap.getDistanceFunction();
		List<RoadWay> candidateWayList = completeRandomWayList(originalMap, percentage);
		Set<String> id2NewNodeSet = new HashSet<>();
		HashMap<String, List<RoadNode>> id2processedWayMap = new HashMap<>();
		boolean direction = false;
		for (RoadWay way : candidateWayList) {
			String reverseWayID = way.getID().charAt(0) == '-' ? way.getID().substring(1) : '-' + way.getID();
			if (id2processedWayMap.containsKey(reverseWayID)) {    // the reverse direction road should have the same shape with the
				// original one
				List<RoadNode> reverseNodeList = new ArrayList<>();
				List<RoadNode> originalNodeList = id2processedWayMap.get(reverseWayID);
				reverseNodeList.add(originalNodeList.get(originalNodeList.size() - 1));
				for (int i = originalNodeList.size() - 2; i > 0; i--) {
					String reverseNodeID =
							originalNodeList.get(i).getID().charAt(originalNodeList.get(i).getID().length() - 1) == '-' ?
									originalNodeList.get(i).getID().substring(0, originalNodeList.get(i).getID().length() - 1) :
									originalNodeList.get(i).getID() + "-";
					while (id2NewNodeSet.contains(reverseNodeID)) {
						reverseNodeID = reverseNodeID + "-";
					}
					RoadNode reverseNode = new RoadNode(reverseNodeID, originalNodeList.get(i).lon(), originalNodeList.get(i).lat(), distFunc);
					id2NewNodeSet.add(reverseNodeID);
					reverseNodeList.add(reverseNode);
				}
				reverseNodeList.add(originalNodeList.get(0));
				way.setNodes(reverseNodeList);
				continue;
			}
			// no reverse road has been processed.
			List<RoadNode> newNodeList = new ArrayList<>();
			newNodeList.add(way.getNode(0));
			for (int i = 0; i < way.getNodes().size() - 1; i++) {
				RoadNode prevNode = way.getNode(i);        // the location of the last stored point (not necessarily mini node)
				RoadNode nextNode = way.getNode(i + 1);    // the location of the next mini node
				int idIndex = 0;
				double length = distFunc.distance(prevNode.toPoint(), nextNode.toPoint());
				while (length > distance) {    // insert new nodes.
					double angle = distFunc.getHeading(way.getNode(i).lon(), way.getNode(i).lat(), nextNode.lon(), nextNode.lat());
					if (direction)
						angle = angle + Math.PI / 2;
					else
						angle = angle - Math.PI / 2;
					double ratio = (length - distance) / length;
					double newLon =
							(nextNode.lon() - (nextNode.lon() - prevNode.lon()) * ratio) + distFunc.getCoordinateOffsetX(Math.cos(angle) * distance / 2,
									prevNode.lat());
					double newLat =
							(nextNode.lat() - (nextNode.lat() - prevNode.lat()) * ratio) + distFunc.getCoordinateOffsetX(Math.sin(angle) * distance / 2,
									prevNode.lon());
					String currNodeID = way.getNode(i).getID() + "+" + idIndex;
					while (id2NewNodeSet.contains(currNodeID)) {
						currNodeID = currNodeID + "-";
					}
					RoadNode newNode = new RoadNode(currNodeID, newLon, newLat, distFunc);
					id2NewNodeSet.add(currNodeID);
					newNodeList.add(newNode);
					direction = !direction;
					idIndex++;
					length -= distance;
					prevNode = newNode;
				}
				newNodeList.add(nextNode);
			}
			way.setNodes(newNodeList);
			id2processedWayMap.put(way.getID(), newNodeList);
		}
		return originalMap;
	}
	
	/**
	 * Generate map which contains only split intersections.
	 *
	 * @param originalMap Original road map.
	 * @param percentage  The percentage of errors.
	 * @return The returning error map.
	 */
	public static RoadNetworkGraph intersectionErrorMapGenerator(RoadNetworkGraph originalMap, double percentage) {
		// randomly select intersections
		Set<Integer> selectedIndex = new LinkedHashSet<>();
		DistanceFunction distFunc = originalMap.getDistanceFunction();
		Random random = new Random(30);
		int processedNodeCount = 0;
		int nodeSize = originalMap.getNodes().size();
		int index;
		while (processedNodeCount < nodeSize * percentage / 100.0) {
			index = random.nextInt(nodeSize);
			if (selectedIndex.contains(index))
				continue;
			selectedIndex.add(index);
			RoadNode currNode = originalMap.getNode(index);
			if ((currNode.getDegree() == 4 && currNode.getInComingDegree() == currNode.getOutGoingDegree()) || currNode.getDegree() > 4) {
				double angle = random.nextDouble() * Math.PI * 2;
				// two intersections are symmetric centred at currNode
				double newLon = currNode.lon() + distFunc.getCoordinateOffsetX(CANDIDATE_RANGE * Math.cos(angle), currNode.lat());
				double newLat = currNode.lat() + distFunc.getCoordinateOffsetY(CANDIDATE_RANGE * Math.sin(angle), currNode.lon());
				double changedLon = currNode.lon() - distFunc.getCoordinateOffsetX(CANDIDATE_RANGE * Math.cos(angle), currNode.lat());
				double changedLat = currNode.lat() - distFunc.getCoordinateOffsetY(CANDIDATE_RANGE * Math.sin(angle), currNode.lon());
				RoadNode newNode = new RoadNode(currNode.getID() + "-", newLon, newLat, distFunc);
				currNode.setLocation(changedLon, changedLat);
				while (newNode.getDegree() < currNode.getDegree()) {
					if (currNode.getInComingDegree() > currNode.getOutGoingDegree()) {    // select a road from in-coming roads.
						RoadWay currWay = currNode.getInComingWayList().iterator().next();
						String reverseID = currWay.getID().charAt(0) == '-' ? currWay.getID().substring(1) : '-' + currWay.getID();
						RoadWay reverseWay = null;
						for (RoadWay way : currNode.getOutGoingWayList()) {
							if (way.getID().equals(reverseID)) {    // switch the reverse road to the new intersection
								reverseWay = way;
								List<RoadNode> wayNodeList = new ArrayList<>();
								wayNodeList.add(newNode);
								wayNodeList.addAll(way.getNodes().subList(1, way.getNodes().size()));
								way.setNodes(wayNodeList);
								newNode.addOutGoingWay(way);
							}
						}
						if (reverseWay != null)
							currNode.removeOutGoingWayFromList(reverseWay);
						currNode.removeInComingWayFromList(currWay);
						currWay.getNodes().remove(currWay.getNodes().size() - 1);
						currWay.addNode(newNode);
						newNode.addInComingWay(currWay);
					} else {
						RoadWay currWay = currNode.getOutGoingWayList().iterator().next();
						String reverseID = currWay.getID().charAt(0) == '-' ? currWay.getID().substring(1) : '-' + currWay.getID();
						RoadWay reverseWay = null;
						for (RoadWay way : currNode.getInComingWayList()) {
							if (way.getID().equals(reverseID)) {    // switch the reverse road to the new intersection
								reverseWay = way;
								way.getNodes().remove(way.getNodes().size() - 1);
								way.addNode(newNode);
								newNode.addInComingWay(way);
							}
						}
						if (reverseWay != null)
							currNode.removeInComingWayFromList(reverseWay);
						currNode.removeOutGoingWayFromList(currWay);
						List<RoadNode> wayNodeList = new ArrayList<>();
						wayNodeList.add(newNode);
						wayNodeList.addAll(currWay.getNodes().subList(1, currWay.getNodes().size()));
						currWay.setNodes(wayNodeList);
						newNode.addOutGoingWay(currWay);
					}
				}
				String interWayID = newNode.getID() + "_" + currNode.getID();
				List<RoadNode> interWayList = new ArrayList<>();
				List<RoadNode> reverseInterWayList = new ArrayList<>();
				interWayList.add(currNode);
				interWayList.add(newNode);
				RoadWay interWay = new RoadWay(interWayID, interWayList, distFunc);
				reverseInterWayList.add(newNode);
				reverseInterWayList.add(currNode);
				RoadWay reverseInterWay = new RoadWay("-" + interWayID, reverseInterWayList, distFunc);
				originalMap.addNode(newNode);
				originalMap.addWay(interWay);
				originalMap.addWay(reverseInterWay);
				processedNodeCount++;
			}
		}
		return originalMap;
	}
	
	/**
	 * Obtain a list of roads from original map which is selected randomly. Double directed roads are selected together.
	 *
	 * @param originalMap The original map, can be either directed or undirected.
	 * @param percentage  The percentage of road to be selected.
	 * @return A list of roads from original map, double directed roads are selected together.
	 */
	private static List<RoadWay> completeRandomWayList(RoadNetworkGraph originalMap, double percentage) {
		List<RoadWay> wayList = originalMap.getWays();
		Set<String> selectIDSet = new LinkedHashSet<>();
		Random random = new Random(30);
		while (selectIDSet.size() < wayList.size() * percentage / 100.0) {    // keep finding the next road
			String wayID = wayList.get(random.nextInt(wayList.size())).getID();
			selectIDSet.add(wayID);
			String reverseWayID = wayID.charAt(0) == '-' ? wayID.substring(1) : '-' + wayID;
			if (originalMap.containsWay(reverseWayID))
				selectIDSet.add(reverseWayID);
		}
		List<RoadWay> resultList = new ArrayList<>();
		for (RoadWay way : wayList) {
			if (selectIDSet.contains(way.getID())) {
				resultList.add(way);
			}
		}
		return resultList;
	}
	
	/**
	 * Obtain a list of roads from original map which is weighted by their visit count and picked randomly. Double directed roads are
	 * picked together.
	 *
	 * @param originalMap The original map, can be either directed or undirected.
	 * @param percentage  The percentage of road to be selected.
	 * @return A list of roads from original map, double directed roads are selected together.
	 */
	private static List<RoadWay> weightedRandomWayList(RoadNetworkGraph originalMap, double percentage) {
		int weightSum = 0;    // the summary of all weight
		double[] itemRange = new double[originalMap.getWays().size()];    // for each road, mark the range of its random
		List<RoadWay> wayList = originalMap.getWays();
		Set<String> selectIDSet = new LinkedHashSet<>();
		// number
		for (int i = 0; i < wayList.size(); i++) {
			RoadWay currWay = wayList.get(i);
			if (currWay.getVisitCount() == -1)
				throw new NullPointerException("Input map does not have visit count.");
			weightSum += currWay.getVisitCount();
			itemRange[i] = weightSum;
		}
		Random random = new Random(30);
		while (selectIDSet.size() < itemRange.length * percentage / 100.0) {    // keep finding the next road
			int value = random.nextInt(weightSum);
			int index = (int) Math.floor(value / (weightSum / (double) itemRange.length));
			while (value > itemRange[index] || (index != 0 && value < itemRange[index - 1])) {
				if (value > itemRange[index]) {
					index++;
				} else if (value < itemRange[index - 1]) {
					index--;
					if (index == 0)
						break;
				} else
					break;
			}
			String wayID = wayList.get(index).getID();
			selectIDSet.add(wayID);
			String reverseWayID = wayID.charAt(0) == '-' ? wayID.substring(1) : '-' + wayID;
			if (originalMap.containsWay(reverseWayID))
				selectIDSet.add(reverseWayID);
//			if (selectIDSet.size() % (itemRange.length / 100) == 0)
//				System.out.println(selectIDSet.size() / (itemRange.length / 100) + "");
			
		}
		List<RoadWay> resultList = new ArrayList<>();
		
		for (RoadWay way : wayList) {
			if (selectIDSet.contains(way.getID()))
				resultList.add(way);
		}
		return resultList;
	}
}
