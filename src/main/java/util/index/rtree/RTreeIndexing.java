package util.index.rtree;

import com.github.davidmoten.grumpy.core.Position;
import com.github.davidmoten.rtree.Entry;
import com.github.davidmoten.rtree.RTree;
import com.github.davidmoten.rtree.geometry.Geometries;
import com.github.davidmoten.rtree.geometry.Line;
import com.github.davidmoten.rtree.geometry.Rectangle;
import com.github.davidmoten.rtree.geometry.internal.PointDouble;
import rx.Observable;
import rx.functions.Func1;
import util.function.DistanceFunction;
import util.function.GreatCircleDistanceFunction;
import util.object.roadnetwork.RoadNetworkGraph;
import util.object.roadnetwork.RoadNode;
import util.object.roadnetwork.RoadWay;
import util.object.spatialobject.Point;
import util.object.spatialobject.Segment;
import util.object.structure.PointMatch;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

/**
 * This class index road network using an R tree
 */
public class RTreeIndexing {
	// load road nodes
	
	private static RTree<String, Line> rTree = RTree.star().create();
	private static RoadNetworkGraph currMap = null;
	
	public RTreeIndexing(RoadNetworkGraph currMap) {
		RTreeIndexing.currMap = currMap;
		buildTree();
	}
	
	
	/**
	 * Add polylines to rtree. One polyline is allowed to contain multiple simple lines.
	 * Each simple line is an geometric object in rtree with a unique id in the tree: polylineID + startNodeId + endNodeID
	 */
	private void buildTree() {
		for (RoadWay way : currMap.getWays()) {
			String polylineID = way.getID();
			for (int i = 0; i < way.getNodes().size() - 1; i++) {
				RoadNode startNode = way.getNode(i);
				double[] curCoord = new double[]{startNode.lon(), startNode.lat()};
				
				RoadNode endNode = way.getNode(i + 1);
				double[] nextCoord = new double[]{endNode.lon(), endNode.lat()};

//				String lineID = polylineID + "|" + startNode.getID() + "|" + endNode.getID();
				String lineID = polylineID + "|" + i;
				rTree = rTree.add(lineID, Geometries.line(curCoord[0], curCoord[1], nextCoord[0], nextCoord[1]));
			}
		}
	}
	
	/**
	 * Get the road network rTree
	 *
	 * @return loaded rtree
	 */
	public static RTree<String, Line> getTree() {
		return rTree;
	}
	
	/**
	 * Point query the rtree
	 *
	 * @param lon       longitude of the query point
	 * @param lat       latitude of the query point
	 * @param distanceM search radius (m)
	 * @return list of line objects that intersect the search box
	 */
	public List<Entry<String, Line>> search(double lon, double lat, final double distanceM) {
		return priSearch(lon, lat, distanceM).toList().toBlocking().single();
	}
	
	/**
	 * Point query the rtree
	 *
	 * @param lon       longitude of the query point
	 * @param lat       latitude of the query point
	 * @param distanceM search radius (m)
	 * @return Observations of lines
	 */
	private Observable<Entry<String, Line>> priSearch(double lon, double lat, final double distanceM) {
		// First we need to calculate an enclosing lat long rectangle for this
		// distance then we refine on the exact distance
		final Position from = Position.create(lat, lon);
		Rectangle bounds = createBounds(from, distanceM * 1.5 / 1000);
		
		return rTree
				// do the first search using the bounds (using L2 distance)
				.search(bounds)
				// refine using the exact distance
				.filter(new Func1<Entry<String, Line>, Boolean>() {
					@Override
					public Boolean call(Entry<String, Line> entry) {
						Line line = entry.geometry();
						
						DistanceFunction df = currMap.getDistanceFunction();
						return df.pointToSegmentProjectionDistance(lon, lat, line.x1(), line.y1(), line.x2(), line.y2()) < distanceM;
					}
				});
	}
	
	/**
	 * create searching box around the query point
	 *
	 * @param from       Position of the query point
	 * @param distanceKm length of the searching radius (km)
	 * @return searching box
	 */
	private Rectangle createBounds(final Position from, final double distanceKm) {
		// this calculates a pretty accurate bounding box. Depending on the
		// performance you require you wouldn't have to be this accurate because
		// accuracy is enforced later
		Position north = from.predict(distanceKm, 0);
		Position east = from.predict(distanceKm, 90);
		Position south = from.predict(distanceKm, 180);
		Position west = from.predict(distanceKm, 270);
		
		return Geometries.rectangle(west.getLon(), south.getLat(), east.getLon(), north.getLat());
	}
	
	/**
	 * search road segments in vicinity of search points
	 *
	 * @param radiusM searching radius around gps point
	 * @return lists of candidate matches
	 */
	public List<PointMatch> searchNeighbours(Point from, double radiusM) {
		List<Entry<String, Line>> results = search(from.x(), from.y(), radiusM);
		List<PointMatch> neighbours = new ArrayList<>();
		
		for (Entry<String, Line> pair : results) {
			double[] startNode = formatDoubles(new double[]{pair.geometry().x1(), pair.geometry().y1()});
			double[] endNode = formatDoubles(new double[]{pair.geometry().x2(), pair.geometry().y2()});
			
			Point closestPoint = from.getDistanceFunction().getClosestPoint(from.x(), from.y(), startNode[0], startNode[1], endNode[0], endNode[1]);
			
			Segment sg = new Segment(startNode[0], startNode[1], endNode[0], endNode[1], from.getDistanceFunction());
			
			
			neighbours.add(new PointMatch(closestPoint, sg, pair.value()));
		}
		return neighbours;
	}
	
	/**
	 * search road segments in vicinity of search points
	 *
	 * @param from           Query point.
	 * @param candidateCount Total number of candidate required.
	 * @param maxRadiusM     The maximum distance allowed for a candidate.
	 * @return lists of candidate matches
	 */
	public List<PointMatch> searchKNeighbours(Point from, int candidateCount, double maxRadiusM) {
		List<Entry<String, Line>> results = knnSearch(from, candidateCount, maxRadiusM);
		List<PointMatch> neighbourList = new ArrayList<>();
		
		for (Entry<String, Line> pair : results) {
			double[] startNode = formatDoubles(new double[]{pair.geometry().x1(), pair.geometry().y1()});
			double[] endNode = formatDoubles(new double[]{pair.geometry().x2(), pair.geometry().y2()});
			
			Point closestPoint = from.getDistanceFunction().getClosestPoint(from.x(), from.y(), startNode[0], startNode[1], endNode[0], endNode[1]);
			
			Segment sg = new Segment(startNode[0], startNode[1], endNode[0], endNode[1], from.getDistanceFunction());
			
			neighbourList.add(new PointMatch(closestPoint, sg, pair.value()));
		}
		return neighbourList;
	}
	
	/**
	 * Round 14 decimal places to 5 decimal places
	 *
	 * @param coord 14 decimal_places coords
	 * @return 5 decimal_places coords
	 */
	private static double[] formatDoubles(double[] coord) {
		DecimalFormat df = new DecimalFormat("#.00000");
		double x = Double.parseDouble(df.format(coord[0]));
		double y = Double.parseDouble(df.format(coord[1]));
		return new double[]{x, y};
	}
	
	/**
	 * K-nearest neighbour search for point query.
	 *
	 * @param searchPoint    The query point
	 * @param candidateCount Number of candidate required for the point
	 * @param radiusM        Maximum distance used to search the index
	 * @return Observations of lines
	 */
	private List<Entry<String, Line>> knnSearch(Point searchPoint, final int candidateCount, final double radiusM) {
		
		final PointDouble from = PointDouble.create(searchPoint.x(), searchPoint.y());
		DistanceFunction df = searchPoint.getDistanceFunction();
		if (searchPoint.getDistanceFunction() instanceof GreatCircleDistanceFunction) {
			Observable<Entry<String, Line>> roughRes = rTree.nearest(from, radiusM * 4, (int) (candidateCount * 1.5));  // obtain
			// more candidate than required since they use different distance function.
			List<Entry<String, Line>> candidateList = roughRes.filter(entry -> {
				Line line = entry.geometry();
				
				return df.pointToSegmentProjectionDistance(searchPoint.x(), searchPoint.y(), line.x1(), line.y1(), line.x2(), line.y2()) < radiusM;
			}).toList().toBlocking().single();
			
			if (candidateList.size() <= candidateCount)    // the number of candidate already no more than requirement, output right
				// away.
				return candidateList;
			
			// otherwise, pick up the top-k results
			PriorityQueue<DistanceItem> candidateQueue = new PriorityQueue<>();
			for (Entry<String, Line> entry : candidateList) {
				Line line = entry.geometry();
				double distance = df.pointToSegmentProjectionDistance(searchPoint.x(), searchPoint.y(), line.x1(), line.y1(), line.x2(),
						line.y2());
				candidateQueue.add(new DistanceItem(entry, distance));
			}
			List<Entry<String, Line>> resList = new ArrayList<>();
			for (int i = 0; i < candidateCount; i++) {
				if (candidateQueue.size() == 0)
					throw new IllegalArgumentException("The candidate queue should not be empty.");
				resList.add(candidateQueue.poll().getItem());
			}
			return resList;
		} else {    // Euclidean distance, which is exactly what RTree is using
			return rTree.nearest(from, radiusM, candidateCount).toList().toBlocking().single();
		}
	}
	
	private static class DistanceItem implements Comparable<DistanceItem> {
		private final Entry<String, Line> item;
		private final double distance;
		
		DistanceItem(Entry<String, Line> item, double distance) {
			this.item = item;
			this.distance = distance;
		}
		
		public Entry<String, Line> getItem() {
			return item;
		}
		
		public double getDistance() {
			return distance;
		}
		
		@Override
		public int compareTo(DistanceItem o) {
			return Double.compare(this.distance, o.distance);
		}
	}
}
