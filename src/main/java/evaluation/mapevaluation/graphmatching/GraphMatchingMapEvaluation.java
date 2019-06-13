package evaluation.mapevaluation.graphmatching;

import org.apache.log4j.Logger;
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
 * Match each map node and map edge between output and ground-truth map, then use precision/recall/F-score to measure the similarity
 * between two maps. Method proposed in:
 * <p>
 * Karagiorgou, Sophia, Dieter Pfoser, and Dimitrios Skoutas. "A layered approach for more robust generation of road network maps from
 * vehicle tracking data." ACM Transactions on Spatial Algorithms and Systems (TSAS) 3.1 (2017): 3.
 *
 * @author uqpchao
 * Created 11/06/2019
 */
public class GraphMatchingMapEvaluation {
	
	private static final Logger LOG = Logger.getLogger(GraphMatchingMapEvaluation.class);
	
	public static String precisionRecallGraphMatchingMapEval(RoadNetworkGraph outputMap, RoadNetworkGraph gtMap, double maxDist) {
		
		if (outputMap.getDistanceFunction().getClass() != gtMap.getDistanceFunction().getClass())
			throw new IllegalArgumentException("Input map and ground-truth map has different coordinate system.");
		
		RoadNetworkGraph outputSimpleMap = outputMap.toSimpleMap();
		RoadNetworkGraph gtSimpleMap = gtMap.toSimpleMap();
		
		DistanceFunction distFunc = outputMap.getDistanceFunction();
		
		// two maps share the same region
		double maxLon = outputSimpleMap.getMaxLon() > gtSimpleMap.getMaxLon() ? outputSimpleMap.getMaxLon() : gtSimpleMap.getMaxLon();
		double maxLat = outputSimpleMap.getMaxLat() > gtSimpleMap.getMaxLat() ? outputSimpleMap.getMaxLat() : gtSimpleMap.getMaxLat();
		double minLon = outputSimpleMap.getMinLon() < gtSimpleMap.getMinLon() ? outputSimpleMap.getMinLon() : gtSimpleMap.getMinLon();
		double minLat = outputSimpleMap.getMinLat() < gtSimpleMap.getMinLat() ? outputSimpleMap.getMinLat() : gtSimpleMap.getMinLat();
		// build index for both output map and ground-truth map
		double lonDistance = distFunc.pointToPointDistance(maxLon, (maxLat + minLat) / 2, minLon, (maxLat + minLat) / 2);
		double latDistance = distFunc.pointToPointDistance((maxLon + minLon) / 2, maxLat, (maxLon + minLon) / 2, minLat);
		int columnNum = (int) Math.floor(lonDistance / maxDist);
		int rowNum = (int) Math.floor(latDistance / maxDist);
		Grid<Point> outputMapGrid = new Grid<>(columnNum, rowNum, minLon, minLat, maxLon, maxLat, distFunc);
		Grid<Point> gtMapGrid = new Grid<>(columnNum, rowNum, minLon, minLat, maxLon, maxLat, distFunc);
		
		// insert intersections to the index, each index node has its corresponding road node stored in nodeMapping
		List<XYObject<Point>> outputIndexPointList = new ArrayList<>();
		List<XYObject<Point>> gtIndexPointList = new ArrayList<>();
		List<RoadNode> outputNodeMapping = new ArrayList<>();    // for each index, the corresponding road node in output map
		List<RoadNode> gtNodeMapping = new ArrayList<>();    // for each index, the corresponding road node in ground-truth map
		Map<String, Integer> id2IndexOutputMap = new HashMap<>();    // for each road node in output map, its ID to its index in
		// outputNodeMapping
		Map<String, Integer> id2IndexGTMap = new HashMap<>();    // for each road node in ground-truth map, its ID to its index in
		// outputNodeMapping
		Map<Integer, Set<Integer>> output2GTIndexMap = new HashMap<>();    // for each index of output map node, the set of gt node
		// indices that match it.
		Map<Integer, Set<Integer>> gt2OutputIndexMap = new HashMap<>();    // for each index of output map node, the set of gt node
		// indices that match it.
		Set<String> outputWayIndexLinkSet = new HashSet<>();    // road way end nodes' ID String in output map
		Set<String> gtWayIndexLinkSet = new HashSet<>();    // road way end nodes' ID String in ground-truth
		int index = 0;
		for (RoadNode node : outputSimpleMap.getNodes()) {
			Point currPoint = node.toPoint();
			currPoint.setID(index + "");
			outputNodeMapping.add(node);
			if (id2IndexOutputMap.containsKey(node.getID()))
				LOG.warn("Duplicated node ID found in map: " + node.getID());
			id2IndexOutputMap.put(node.getID(), index);
			outputIndexPointList.add(new XYObject<>(currPoint.x(), currPoint.y(), currPoint));
			index++;
		}
		
		index = 0;
		for (RoadNode node : gtSimpleMap.getNodes()) {
			Point currPoint = node.toPoint();
			currPoint.setID(index + "");
			gtNodeMapping.add(node);
			if (id2IndexGTMap.containsKey(node.getID()))
				LOG.warn("Duplicated node ID found in map: " + node.getID());
			id2IndexGTMap.put(node.getID(), index);
			gtIndexPointList.add(new XYObject<>(currPoint.x(), currPoint.y(), currPoint));
			index++;
		}
		
		outputMapGrid.insertAll(outputIndexPointList);
		gtMapGrid.insertAll(gtIndexPointList);
		
		// start node match
		int nodeMatchOutputInGT = 0;    // for each output node, whether it has matching node in ground-truth
		for (int i = 0; i < outputNodeMapping.size(); i++) {
			RoadNode node = outputNodeMapping.get(i);
			List<Point> indexPointList = new ArrayList<>();
			GridPartition<Point> gtCandidatePartition = gtMapGrid.partitionSearch(node.lon(), node.lat());
			if (gtCandidatePartition != null) {
				for (XYObject<Point> pointXYObject : gtCandidatePartition.getObjectsList()) {
					indexPointList.add(pointXYObject.getSpatialObject());
				}
			}
			List<GridPartition<Point>> adjacentPartitionList = gtMapGrid.adjacentPartitionSearch(node.lon(), node.lat());
			for (GridPartition<Point> partition : adjacentPartitionList) {
				if (partition != null && !partition.isEmpty()) {
					for (XYObject<Point> point : partition.getObjectsList()) {
						indexPointList.add(point.getSpatialObject());
					}
				}
			}
			boolean isMatchFound = false;
			for (Point pointObject : indexPointList) {
				if (pointObject != null && distFunc.distance(pointObject, node.toPoint()) <= maxDist) {
					isMatchFound = true;
					if (!output2GTIndexMap.containsKey(i)) {
						Set<Integer> gtMatchSet = new LinkedHashSet<>();
						gtMatchSet.add(Integer.parseInt(pointObject.getID()));
						output2GTIndexMap.put(i, gtMatchSet);
					} else {
						output2GTIndexMap.get(i).add(Integer.parseInt(pointObject.getID()));
					}
				}
			}
			if (isMatchFound) {
				nodeMatchOutputInGT++;
			}
		}
		
		int nodeMatchGTInOutput = 0;    // for each output node, whether it has matching node in ground-truth
		for (int i = 0; i < gtNodeMapping.size(); i++) {
			RoadNode node = gtNodeMapping.get(i);
			List<Point> indexPointList = new ArrayList<>();
			GridPartition<Point> outputCandidatePartition = outputMapGrid.partitionSearch(node.lon(), node.lat());
			if (outputCandidatePartition != null) {
				for (XYObject<Point> pointXYObject : outputCandidatePartition.getObjectsList()) {
					indexPointList.add(pointXYObject.getSpatialObject());
				}
			}
			List<GridPartition<Point>> adjacentPartitionList = outputMapGrid.adjacentPartitionSearch(node.lon(), node.lat());
			for (GridPartition<Point> partition : adjacentPartitionList) {
				if (partition != null && !partition.isEmpty())
					for (XYObject<Point> point : partition.getObjectsList()) {
						indexPointList.add(point.getSpatialObject());
					}
			}
			boolean isMatchFound = false;
			for (Point pointObject : indexPointList) {
				if (pointObject != null && distFunc.distance(pointObject, node.toPoint()) <= maxDist) {
					isMatchFound = true;
					if (!gt2OutputIndexMap.containsKey(i)) {
						Set<Integer> outputMatchSet = new LinkedHashSet<>();
						outputMatchSet.add(Integer.parseInt(pointObject.getID()));
						gt2OutputIndexMap.put(i, outputMatchSet);
					} else {
						gt2OutputIndexMap.get(i).add(Integer.parseInt(pointObject.getID()));
					}
				}
			}
			if (isMatchFound) {
				nodeMatchGTInOutput++;
			}
		}
		
		double nodePrecision = (nodeMatchOutputInGT / (double) outputNodeMapping.size());
		double nodeRecall = (nodeMatchGTInOutput / (double) gtNodeMapping.size());
		double nodeFScore = 2 * nodePrecision * nodeRecall / (nodePrecision + nodeRecall);
		
		// start edge match, register all road way indices first
		for (RoadWay way : outputSimpleMap.getWays()) {
			int startIndex = id2IndexOutputMap.get(way.getFromNode().getID());
			int endIndex = id2IndexOutputMap.get(way.getToNode().getID());
			outputWayIndexLinkSet.add(startIndex + "_" + endIndex);
		}
		
		for (RoadWay way : gtSimpleMap.getWays()) {
			int startIndex = id2IndexGTMap.get(way.getFromNode().getID());
			int endIndex = id2IndexGTMap.get(way.getToNode().getID());
			gtWayIndexLinkSet.add(startIndex + "_" + endIndex);
		}
		
		// find output map correspondence
		int edgeMatchOutputInGT = 0;
		for (RoadWay way : outputSimpleMap.getWays()) {
			boolean isFound = false;
			int startIndex = id2IndexOutputMap.get(way.getFromNode().getID());
			int endIndex = id2IndexOutputMap.get(way.getToNode().getID());
			if (output2GTIndexMap.containsKey(startIndex) && output2GTIndexMap.containsKey(endIndex)) {
				for (Integer startMatchIndex : output2GTIndexMap.get(startIndex)) {
					for (Integer endMatchIndex : output2GTIndexMap.get(endIndex)) {
						if (gtWayIndexLinkSet.contains(startMatchIndex + "_" + endMatchIndex)) {
							isFound = true;
							break;
						}
					}
					if (isFound) {
						edgeMatchOutputInGT++;
						break;
					}
				}
			}
		}
		
		// find ground-truth map correspondence
		int edgeMatchGTInOutput = 0;
		for (RoadWay way : gtSimpleMap.getWays()) {
			boolean isFound = false;
			int startIndex = id2IndexGTMap.get(way.getFromNode().getID());
			int endIndex = id2IndexGTMap.get(way.getToNode().getID());
			if (gt2OutputIndexMap.containsKey(startIndex) && gt2OutputIndexMap.containsKey(endIndex)) {
				for (Integer startMatchIndex : gt2OutputIndexMap.get(startIndex)) {
					for (Integer endMatchIndex : gt2OutputIndexMap.get(endIndex)) {
						if (outputWayIndexLinkSet.contains(startMatchIndex + "_" + endMatchIndex)) {
							isFound = true;
							break;
						}
					}
					if (isFound) {
						edgeMatchGTInOutput++;
						break;
					}
				}
			}
		}
		
		double edgePrecision = (edgeMatchOutputInGT / (double) outputSimpleMap.getWays().size());
		double edgeRecall = (edgeMatchGTInOutput / (double) gtSimpleMap.getWays().size());
		double edgeFScore = 2 * edgePrecision * edgeRecall / (edgePrecision + edgeRecall);
		
		LOG.info("Graph item matching evaluation done.");
		LOG.info("Node precision=" + nodePrecision + ", node recall=" + nodeRecall + ", node F-score=" + nodeFScore);
		LOG.info("Edge precision=" + edgePrecision + ", edge recall=" + edgeRecall + ", edge F-score=" + edgeFScore);
		
		return nodePrecision + ", " + nodeRecall + ", " + nodeFScore + ", " + edgePrecision + ", " + edgeRecall + ", " + edgeFScore;
	}
}
