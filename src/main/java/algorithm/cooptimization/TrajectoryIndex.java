package algorithm.cooptimization;

import util.function.DistanceFunction;
import util.function.SpatialUtils;
import util.index.rtree.STRNode;
import util.index.rtree.STRTree;
import util.io.TrajectoryReader;
import util.object.roadnetwork.RoadNode;
import util.object.roadnetwork.RoadWay;
import util.object.spatialobject.Point;
import util.object.spatialobject.Rect;
import util.object.spatialobject.Trajectory;
import util.object.spatialobject.TrajectoryPoint;
import util.object.structure.XYObject;
import util.settings.BaseProperty;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * @author Hellisk
 * @since 19/04/2019
 */
public class TrajectoryIndex {
	
	static STRTree<Point> buildTrajectoryIndex(String fileFolder, DistanceFunction df) {
		List<Trajectory> trajList = TrajectoryReader.readTrajectoriesToList(fileFolder, 1, df);
		List<XYObject<Point>> indexPointList = new ArrayList<>();
		for (Trajectory trajectory : trajList) {
			for (TrajectoryPoint trajectoryPoint : trajectory) {
				Point currPoint = new Point(trajectoryPoint.x(), trajectoryPoint.y(), trajectoryPoint.getDistanceFunction());
				currPoint.setID(trajectory.getID());
				XYObject<Point> indexItem = new XYObject<>(trajectoryPoint.x(), trajectoryPoint.y(), trajectoryPoint);
				indexPointList.add(indexItem);
			}
		}
		return new STRTree<>(indexPointList, indexPointList.size() / 10000);
	}
	
	/**
	 * For each new road in the current map, we find all trajectories which is close to the new road.
	 *
	 * @param newWayList           List of new roads
	 * @param trajectoryPointIndex Trajectory point R-Tree index.
	 * @return A set of trajectory IDs.
	 */
	static LinkedHashSet<String> trajectoryIDSearch(List<RoadWay> newWayList, STRTree<Point> trajectoryPointIndex,
													BaseProperty prop) {
		LinkedHashSet<String> trajIDSet = new LinkedHashSet<>();
		double range = prop.getPropertyDouble("algorithm.mapmatching.CandidateRange");
		double bbFactor = prop.getPropertyDouble("algorithm.cooptimization.IndexBBFactor");
		for (RoadWay w : newWayList) {
			Rect boundingBox = findBoundingBox(w, range, bbFactor);
			List<STRNode<Point>> resultPartitionList = trajectoryPointIndex.rangePartitionSearch(boundingBox);
			if (resultPartitionList != null) {
				for (STRNode<Point> n : resultPartitionList) {
					for (XYObject<Point> p : n.getObjectsList()) {
						if (boundingBox.contains(p.x(), p.y()))
							trajIDSet.add(p.getSpatialObject().getID());
					}
				}
			}
		}
//        System.out.println("Adjacent trajectory search finished, Total number of successful road search: " + roadCount);
		return trajIDSet;
	}
	
	/**
	 * Find the minimum bounding box for a given road way. The bounding box can be extended according to the given factor for broader
	 * search range.
	 *
	 * @param way    The query road way.
	 * @param range  The search range.
	 * @param factor The enlarge factor. 0= normal size.
	 * @return The MBB rectangle.
	 */
	private static Rect findBoundingBox(RoadWay way, double range, double factor) {
		List<Point> pointList = new ArrayList<>();
		for (RoadNode n : way.getNodes()) {
			pointList.add(n.toPoint());
		}
		Rect bb = SpatialUtils.getBoundingBox(pointList, way.getDistanceFunction());
		if (factor != 0) {
			double minLon = bb.minX();
			double maxLon = bb.maxX();
			double minLat = bb.minY();
			double maxLat = bb.maxY();
			minLon -= way.getDistanceFunction().getCoordinateOffsetX(range, (minLat + maxLat) / 2) * factor;
			maxLon += way.getDistanceFunction().getCoordinateOffsetX(range, (minLat + maxLat) / 2) * factor;
			minLat -= way.getDistanceFunction().getCoordinateOffsetY(range, (minLon + maxLon) / 2) * factor;
			maxLat += way.getDistanceFunction().getCoordinateOffsetY(range, (minLon + maxLon) / 2) * factor;
			return new Rect(minLon, minLat, maxLon, maxLat, way.getDistanceFunction());
		}
		return bb;
	}
}
