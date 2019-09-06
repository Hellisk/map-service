package util.index;

import org.apache.log4j.Logger;
import util.exceptions.SpatialQueryException;
import util.function.DistanceFunction;
import util.object.spatialobject.Point;
import util.object.spatialobject.Rect;
import util.object.spatialobject.SpatialObject;
import util.object.structure.Pair;
import util.object.structure.XYObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Base interface for spatial data structures, and Spatial partitioning methods.
 *
 * @author uqdalves, Hellisk
 */
public interface SpatialDataStructure<T extends SpatialObject> extends SpatialQuery<T> {
	
	Logger LOG = Logger.getLogger(SpatialDataStructure.class);
	/**
	 * factor that uses to enlarge the search circle
	 */
	double DELTA = 2;
	
	/**
	 * @return Whether or not boundary objects in this space partitioning are replicated.
	 */
	boolean isReplicateBoundary();
	
	/**
	 * Set the boundary replication policy.
	 *
	 * @param replicate Whether or not to replicate boundary objects. If False, objects will be inserted into into theirs first partition
	 *                  occurrence.
	 */
	void setReplicateBoundary(boolean replicate);
	
	/**
	 * @return The model representing this spatial partitioning.
	 */
	SpatialIndexModel getModel();
	
	/**
	 * @return The number of objects in this data structure.
	 */
	long count();
	
	/**
	 * @return True if this spatial partitioning structure has no elements.
	 */
	boolean isEmpty();
	
	/**
	 * Print this spatial data structure to system output console.
	 */
	void print();
	
	/**
	 * Insert the XY spatial object into the appropriate spatial partition (intersecting partition).
	 *
	 * @param obj The XY spatial object to insert.
	 * @return True if the object was successfully inserted. False if the object is not within the boundaries of this spatial data
	 * structure.
	 */
	boolean insert(XYObject<T> obj);
	
	/**
	 * Insert all the XY spatial objects into their appropriate spatial partitions (intersecting partitions).
	 *
	 * @param objList The list of XY spatial objects to insert.
	 */
	default void insertAll(List<XYObject<T>> objList) {
		if (objList == null) {
			throw new NullPointerException("List of spatial objects to insert into a spatial data structure must not be null.");
		}
		for (XYObject<T> obj : objList) {
			insert(obj);
		}
	}
	
	/**
	 * Remove the XY spatial object from this spatial partitioning structure.
	 *
	 * @param obj The XY spatial object to remove.
	 * @return True if the object was successfully removed. False if this spatial data structure does not contain the given object to
	 * remove.
	 */
	boolean remove(XYObject<T> obj);
	
	/**
	 * Remove the XY spatial object from this spatial partitioning structure.
	 *
	 * @param objList The list containing the XY spatial objects to remove.
	 */
	default void removeAll(List<XYObject<T>> objList) {
		if (objList == null) {
			throw new NullPointerException("List of spatial objects to remove from a spatial data structure must not be null.");
		}
		for (XYObject<T> obj : objList) {
			remove(obj);
		}
	}
	
	/**
	 * @return Returns all partitions in this spatial data structure.
	 */
	List<? extends SpatialPartition> getPartitions();
	
	/**
	 * Search and return the spatial partition intersecting the object in the position (x,y).
	 *
	 * @param x The X coordinate to search.
	 * @param y The Y coordinate to search.
	 * @return The spatial partition intersecting the position (x,y).
	 */
	SpatialPartition<XYObject<T>> partitionSearch(double x, double y);
	
	/**
	 * Search and return all spatial partitions intersecting with the given spatial object.
	 *
	 * @param obj The spatial object used to search.
	 * @return All spatial partitions intersecting with the given spatial object.
	 */
	List<? extends SpatialPartition<XYObject<T>>> rangePartitionSearch(SpatialObject obj);
	
	@Override
	default XYObject<T> nearestNeighborSearch(double x, double y, DistanceFunction distFunc) {
		// get the partition containing the object
		SpatialPartition<XYObject<T>> firstPartition = partitionSearch(x, y);
		
		// find the nearest neighbor in the partition containing the object
		double minDistance = Double.POSITIVE_INFINITY;
		double maxDistance = 0;
		double distance;
		XYObject<T> nearest = null;
		if (firstPartition != null) {
			for (XYObject<T> obj : firstPartition.getObjectsList()) {
				distance = distFunc.pointToPointDistance(x, y, obj.x(), obj.y());
				if (distance < minDistance) {
					minDistance = distance;
					nearest = obj;
				}
			}
		} else {
			// if the first partition is empty, use the boundary point as the radius
			List<Point> boundaryPointList = new ArrayList<>(getModel().getBoundary().getCoordinates());
			for (Point p : boundaryPointList) {
				distance = distFunc.pointToPointDistance(x, y, p.x(), p.y());
				if (distance > maxDistance) {
					maxDistance = distance;
				}
			}
		}
		
		// build a square made of the query object as center, and the distance from the query object to its nearest neighbor in the
		// partition as the distance to its edges
		Rect range;
		if (maxDistance == 0) {
			range = new Rect(x - minDistance, y - minDistance, x + minDistance, y + minDistance, distFunc);
		} else {
			range = new Rect(x - maxDistance, y - maxDistance, x + maxDistance, y + maxDistance, distFunc);
		}
		
		// get all partitions intersecting the rectangle region
		List<? extends SpatialPartition<XYObject<T>>> partitionsList = rangePartitionSearch(range);
		
		// the nearest neighbor is in this partition, no need to search further
		if (partitionsList.size() == 1) {
			return nearest;
		}
		
		// search all partitions
		for (SpatialPartition<XYObject<T>> partition : partitionsList) {
			for (XYObject<T> obj : partition.getObjectsList()) {
				distance = distFunc.pointToPointDistance(x, y, obj.x(), obj.y());
				if (distance < minDistance) {
					minDistance = distance;
					nearest = obj;
				}
			}
		}
		if (nearest == null) {
			LOG.info("Nearest neighbour is too far away.");
		}
		return nearest;
	}
	
	@Override
	default List<XYObject<T>> kNearestNeighborsSearch(double x, double y, int k, DistanceFunction distFunc) {
		if (k <= 0) {
			throw new IllegalArgumentException("Number of nearest neighbors (K) must be a positive number.");
		}
		// get the partition containing the object
		SpatialPartition<XYObject<T>> firstPartition = partitionSearch(x, y);
		double knnDistance;
		List<Pair<XYObject<T>, Double>> distanceList = new ArrayList<>();
		double distance;
		// sort by distance
		final Comparator<Pair<XYObject<T>, Double>> distanceComparator =
				Comparator.comparing(Pair::_2);
		if (firstPartition != null) {
			// find the distance between the query point and the objects in this partition
			for (XYObject<T> obj : firstPartition.getObjectsList()) {
				distance = distFunc.pointToPointDistance(x, y, obj.x(), obj.y());
				distanceList.add(new Pair<>(obj, distance));
			}
			
			distanceList.sort(distanceComparator);
			
			// build a circle made of the query object as center, and the distance from the query object to its k-NN in the partition as
			// radius
			
			knnDistance = distanceList.get(Math.min(k, distanceList.size()) - 1)._2();
		} else {
			List<Point> boundaryPointList = new ArrayList<>(getModel().getBoundary().getCoordinates());
			List<Pair<Point, Double>> boundaryPointDistanceList = new ArrayList<>();
			for (Point p : boundaryPointList) {
				distance = distFunc.pointToPointDistance(x, y, p.x(), p.y());
				boundaryPointDistanceList.add(new Pair<>(p, distance));
			}
			final Comparator<Pair<Point, Double>> boundaryDistanceComparator =
					Comparator.comparing(Pair::_2);
			boundaryPointDistanceList.sort(boundaryDistanceComparator);
			knnDistance = boundaryPointDistanceList.get((boundaryPointList.size() - 1))._2();
		}
		
		List<XYObject<T>> knnList = new ArrayList<>(k);
		
		do {
			knnList.clear();
			Rect range = new Rect(x - knnDistance, y - knnDistance, x + knnDistance, y + knnDistance, distFunc);
			
			// get all partitions intersecting the circle region
			List<? extends SpatialPartition<XYObject<T>>> partitionsList = rangePartitionSearch(range);
			
			// if there are more than k element inside, the k nearest neighbor is in this partition, no need to search further kNN is
			// already in this partition
			if (partitionsList.size() == 1) {
				int count = Math.min(k, distanceList.size());
				for (int i = 0; i < count; i++) {
					Pair<XYObject<T>, Double> pair = distanceList.get(i);
					knnList.add(pair._1());
				}
				
				knnDistance = DELTA * knnDistance;
				continue;
			}
			
			// search in the other partitions
			partitionsList.remove(firstPartition);
			for (SpatialPartition<XYObject<T>> partition : partitionsList) {
				for (XYObject<T> obj : partition.getObjectsList()) {
					distance = distFunc.pointToPointDistance(x, y, obj.x(), obj.y());
					distanceList.add(new Pair<>(obj, distance));
				}
			}
			
			// check if there is at least k object in this range
			int count = distanceList.size();
			if (count < k) {
				throw new SpatialQueryException("Partitions must contain at least k objects for k-NN search. Consider changing the " +
						"partitioning granularity.");
			}
			
			// sort by distance
			distanceList.sort(distanceComparator);
			
			// get the kNN
			for (int i = 0; i < k; i++) {
				Pair<XYObject<T>, Double> pair = distanceList.get(i);
				knnList.add(pair._1());
			}
			knnDistance = DELTA * knnDistance;
		} while (knnList.size() < k);
		return knnList;
	}
	
	@Override
	default List<XYObject<T>> rangeSearch(SpatialObject queryObj) {
		if (queryObj == null) {
			throw new NullPointerException("Query object cannot be null.");
		}
		// get all partitions intersecting the query object
		List<? extends SpatialPartition<XYObject<T>>> partitionsList = rangePartitionSearch(queryObj);
		
		// refine the search, check which object in the partitions really intersect with the query object.
		List<XYObject<T>> result = new ArrayList<>();
		for (SpatialPartition<XYObject<T>> partition : partitionsList) {
			for (XYObject<T> obj : partition.getObjectsList()) {
				if (queryObj.intersects(obj.toPoint())) {
					result.add(obj);
				}
			}
		}
		return result;
	}
}
