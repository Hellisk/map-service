package preprocessing;

import org.apache.log4j.Logger;
import util.function.DistanceFunction;
import util.index.grid.Grid;
import util.index.grid.GridPartition;
import util.io.MapWriter;
import util.object.roadnetwork.RoadNetworkGraph;
import util.object.roadnetwork.RoadWay;
import util.object.spatialobject.Point;
import util.object.spatialobject.Segment;
import util.object.structure.XYObject;

import java.io.IOException;
import java.util.*;

/**
 * Perform map preprocess on a road map, including road removal, road type filter, etc.
 *
 * @author Hellisk
 * @since 6/04/2019
 */
public class MapPreprocessing {
	
	private static final Logger LOG = Logger.getLogger(MapPreprocessing.class);
	
	/**
	 * Randomly remove a certain percentage of road from the original map. Both the removed roads and the map after removal are written
	 * to the folder.
	 *
	 * @param roadGraph        The original road map.
	 * @param percentage       The percentage of roads to be removed. Node removal should not affect the number of nodes.
	 * @param outputFileFolder The output map folder.
	 */
	public static void randomRoadRemoval(RoadNetworkGraph roadGraph, int percentage, String outputFileFolder) throws IOException {
		
		if (percentage == 0) {    // no road to be removed
			LOG.warn("The required remove road ratio is " + percentage + ", the removal is not needed.");
			return;
		}
		Map<String, RoadWay> id2RoadWay = new HashMap<>();
		Set<RoadWay> removedRoadWaySet = new HashSet<>();
		Random random = new Random(1);
		for (RoadWay w : roadGraph.getWays()) {
			id2RoadWay.put(w.getID(), w);
		}
		while (removedRoadWaySet.size() / (double) roadGraph.getWays().size() < percentage) {
			int index = random.nextInt(roadGraph.getWays().size());
			RoadWay w = roadGraph.getWays().get(index);
			if (!removedRoadWaySet.contains(w)) {
				removedRoadWaySet.add(w);
				if (w.getID().contains("-")) {
					String reversedRoadID = w.getID().substring(1);
					if (id2RoadWay.containsKey(reversedRoadID))
						removedRoadWaySet.add(id2RoadWay.get(reversedRoadID));
				} else {
					String reversedRoadID = "-" + w.getID();
					if (id2RoadWay.containsKey(reversedRoadID))
						removedRoadWaySet.add(id2RoadWay.get(reversedRoadID));
				}
			}
		}
		int roadRemovalCount = removedRoadWaySet.size();
		roadGraph.removeRoadWayList(removedRoadWaySet);
		List<RoadWay> removedRoadWayList = new ArrayList<>(removedRoadWaySet);
		int vertexRemovalCount = roadGraph.isolatedNodeRemoval();
		MapWriter.writeMap(roadGraph, outputFileFolder + percentage + ".txt");
		MapWriter.writeWays(removedRoadWayList, outputFileFolder + "remove_edges_" + percentage + ".txt");
		
		LOG.info("Random road Removal done. Total removed roads: " + roadRemovalCount + ", total removed nodes:" + vertexRemovalCount);
	}
	
	/**
	 * Remove the edges from fully enclosed roads and separated double direction roads, both directions are guaranteed to be removed.
	 * The removed roads should be long enough.
	 *
	 * @param roadGraph        The original road map.
	 * @param percentage       The removal percentage.
	 * @param cellSize         The size of the grid cells.
	 * @param minRoadLength    The minimum road length for road removal.
	 * @param outputFileFolder The output map file directory.
	 */
	static void popularityBasedRoadRemoval(RoadNetworkGraph roadGraph, int percentage, int cellSize, int minRoadLength, String outputFileFolder) throws IOException {
		
		if (percentage == 0) {
			LOG.warn("The required remove road ratio is " + percentage + ", the removal is not needed.");
			return;
		}
		
		// build index for neighbour search
		Grid<Point> gridIndex = buildGridIndexForCenterPoints(roadGraph, cellSize);
		
		List<RoadWay> wayList = roadGraph.getWays();
		Set<String> removedEdgeIDSet = new LinkedHashSet<>();   // set of removed road ID
		Random random = new Random(10);
		Set<RoadWay> removedWaySet = new HashSet<>();
		List<RoadWay> satisfiedRoadList = new ArrayList<>();    // list of road ways that satisfy the conditions
		HashMap<String, Integer> id2NodeDegreeMapping = new HashMap<>();
		Map<String, RoadWay> id2RoadWay = new HashMap<>();
		for (RoadWay w : wayList) {
			if (isSatisfiedRoad(w, cellSize, minRoadLength, gridIndex)) {
				satisfiedRoadList.add(w);
				id2RoadWay.put(w.getID(), w);
			}
		}
		int satisfiedRoadCount = satisfiedRoadList.size();
		if (satisfiedRoadList.size() * 100 / (double) wayList.size() < percentage)
			throw new IllegalArgumentException("The number of satisfied roads " + satisfiedRoadList.size() + " is less than the " +
					"required road removal " + wayList.size() * percentage / 100 + ". Consider loose the condition or decrease the " +
					"removal percentage.");
		else {
			System.out.println("Number of satisfied removal roads: " + satisfiedRoadList.size() + ", required road: " + wayList.size() * percentage / 100);
		}
		while (removedWaySet.size() * 100 / (double) wayList.size() < percentage) {
			LOG.debug("Current removed road count: " + removedWaySet.size());
			for (RoadWay roadWay : satisfiedRoadList) {
				if (removedWaySet.size() * 100 / (double) wayList.size() > percentage)
					break;
				int randomNumber = random.nextInt((int) Math.floor(100d / percentage));
				if (randomNumber != 0 || removedEdgeIDSet.contains(roadWay.getID()))
					continue;
				removedEdgeIDSet.add(roadWay.getID());
				List<RoadWay> tempRemovedWayList = new ArrayList<>();
				tempRemovedWayList.add(roadWay);
				// put the reversed direction road to the removed road list
				if (roadWay.getID().contains("-")) {
					String reversedRoadID = roadWay.getID().substring(1);
					if (id2RoadWay.containsKey(reversedRoadID))
						if (!removedEdgeIDSet.contains(reversedRoadID)) {
							tempRemovedWayList.add(id2RoadWay.get(reversedRoadID));
						}
				} else {
					String reversedRoadID = "-" + roadWay.getID();
					if (id2RoadWay.containsKey(reversedRoadID))
						if (!removedEdgeIDSet.contains(reversedRoadID)) {
							tempRemovedWayList.add(id2RoadWay.get(reversedRoadID));
						}
				}
				// avoid road node removal
				if (tempRemovedWayList.get(0).getFromNode().getDegree() <= tempRemovedWayList.size() || tempRemovedWayList.get(0).getToNode().getDegree() <= tempRemovedWayList.size())
					continue;
				if (id2NodeDegreeMapping.containsKey(tempRemovedWayList.get(0).getFromNode().getID())) {
					if (id2NodeDegreeMapping.get(tempRemovedWayList.get(0).getFromNode().getID()) <= tempRemovedWayList.size())
						continue;
					else id2NodeDegreeMapping.replace(tempRemovedWayList.get(0).getFromNode().getID(),
							id2NodeDegreeMapping.get(tempRemovedWayList.get(0).getFromNode().getID()) - tempRemovedWayList.size());
				} else {
					id2NodeDegreeMapping.put(tempRemovedWayList.get(0).getFromNode().getID(),
							tempRemovedWayList.get(0).getFromNode().getDegree() - tempRemovedWayList.size());
				}
				if (id2NodeDegreeMapping.containsKey(tempRemovedWayList.get(0).getToNode().getID())) {
					if (id2NodeDegreeMapping.get(tempRemovedWayList.get(0).getToNode().getID()) <= tempRemovedWayList.size())
						continue;
					else id2NodeDegreeMapping.replace(tempRemovedWayList.get(0).getToNode().getID(),
							id2NodeDegreeMapping.get(tempRemovedWayList.get(0).getToNode().getID()) - tempRemovedWayList.size());
				} else {
					id2NodeDegreeMapping.put(tempRemovedWayList.get(0).getToNode().getID(),
							tempRemovedWayList.get(0).getToNode().getDegree() - tempRemovedWayList.size());
				}
				
				for (RoadWay w : tempRemovedWayList) {
					removedEdgeIDSet.add(w.getID());
					removedWaySet.add(w);
				}
			}
		}
		roadGraph.removeRoadWayList(removedWaySet);
		int removedNodeCount = roadGraph.isolatedNodeRemoval();
		
		if (removedNodeCount != 0)
			throw new IllegalStateException("The removed node should be zero: " + removedNodeCount);
		
		List<RoadWay> removedRoadWayList = new ArrayList<>(removedWaySet);
		MapWriter.writeMap(roadGraph, outputFileFolder + percentage + ".txt");
		MapWriter.writeWays(removedRoadWayList, outputFileFolder + "remove_edges_" + percentage + ".txt");
		
		LOG.info("Random road Removal done. Total number of satisfied roads: " + satisfiedRoadCount + ", total removed " +
				"roads: " + removedWaySet.size() + ".");
	}
	
	/**
	 * Generate grid index for the virtual center point of each roadway, which is used for road removal selection.
	 *
	 * @param distance Grid index cell radius.
	 * @return Grid index
	 */
	private static Grid<Point> buildGridIndexForCenterPoints(RoadNetworkGraph roadGraph, int distance) {
		// calculate the grid settings
		int rowNum;     // number of rows
		int columnNum;     // number of columns
		Set<String> locSet = new HashSet<>();  // ensure every inserted node is unique
		if (roadGraph.getNodes().isEmpty())
			throw new IllegalStateException("Cannot create location index of empty graph!");
		
		DistanceFunction distFunc = roadGraph.getDistanceFunction();
		
		// calculate the total number of rows and columns. The size of each grid cell equals the candidate range
		double lonDistance = distFunc.pointToPointDistance(roadGraph.getMaxLon(), 0d, roadGraph.getMinLon(), 0d);
		double latDistance = distFunc.pointToPointDistance(0d, roadGraph.getMaxLat(), 0d, roadGraph.getMinLat());
		columnNum = (int) Math.round(lonDistance / distance);
		rowNum = (int) Math.round(latDistance / distance);
		double lonPerCell = (roadGraph.getMaxLon() - roadGraph.getMinLon()) / columnNum;
		double latPerCell = (roadGraph.getMaxLat() - roadGraph.getMinLat()) / columnNum;
		
		// add extra grid cells around the margin to cover outside trajectory points
		Grid<Point> grid = new Grid<>(columnNum + 2, rowNum + 2, roadGraph.getMinLon() - lonPerCell, roadGraph.getMinLat() -
				latPerCell, roadGraph.getMaxLon() + lonPerCell, roadGraph.getMaxLat() + latPerCell, distFunc);

//        LOG.info("The grid contains " + (rowNum + 2) + " rows and " + (columnNum + 2) + " columns");
		
		int pointCount = 0;
		
		for (RoadWay t : roadGraph.getWays()) {
			for (Segment s : t.getEdges()) {
				Point centerPoint = new Point((s.x1() + s.x2()) / 2, (s.y1() + s.y2()) / 2, distFunc);
				centerPoint.setID(t.getID().replaceAll("-", ""));
				if (!locSet.contains(centerPoint.x() + "_" + centerPoint.y())) {
					XYObject<Point> centerIndex = new XYObject<>(centerPoint.x(), centerPoint.y(), centerPoint);
					grid.insert(centerIndex);
					locSet.add(centerPoint.x() + "_" + centerPoint.y());
					pointCount++;
				}
			}
		}

//        LOG.info("Grid index build successfully, total number of segment center points in grid index: " + pointCount + ", ");
		return grid;
	}
	
	private static boolean isSatisfiedRoad(RoadWay w, int cellSize, int minRoadLength, Grid<Point> gridIndex) {
//        if (w.getWayLevel() <= 4 || w.getWayLevel() == 9)
//            return false;
//        if (!w.getWayType().get(1) && !w.getWayType().get(2) && !w.getWayType().get(21))
//            return false;
		if (w.getLength() < minRoadLength || w.getVisitCount() < 5)
			return false;
		DistanceFunction distFunc = w.getDistanceFunction();
		for (Segment s : w.getEdges()) {
			Point centerPoint = new Point((s.x1() + s.x2()) / 2, (s.y1() + s.y2()) / 2, distFunc);
			
			// find all grid partitions that are close to the given point
			List<GridPartition<Point>> partitionList = new ArrayList<>();
			partitionList.add(gridIndex.partitionSearch(centerPoint.x(), centerPoint.y()));
			partitionList.addAll(gridIndex.adjacentPartitionSearch(centerPoint.x(), centerPoint.y()));
			for (GridPartition<Point> partition : partitionList) {
				if (partition != null)
					for (XYObject<Point> item : partition.getObjectsList()) {
						Point candidatePoint = item.getSpatialObject();
						// if a point is found close enough to the given road, stop the search and return unsatisfied
						if (Math.abs(Long.parseLong(candidatePoint.getID())) != Math.abs(Long.parseLong(w.getID())) && distFunc
								.distance(centerPoint, candidatePoint) < cellSize)
							return false;
					}
			}
		}
		return true;
	}
}
