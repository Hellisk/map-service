package algorithm.mapmatching.stmatching;

import com.github.davidmoten.grumpy.core.Position;
import com.github.davidmoten.rtree.Entry;
import com.github.davidmoten.rtree.RTree;
import com.github.davidmoten.rtree.geometry.Geometries;
import com.github.davidmoten.rtree.geometry.Line;
import com.github.davidmoten.rtree.geometry.Rectangle;
import rx.Observable;
import rx.functions.Func1;
import util.function.DistanceFunction;
import util.function.GreatCircleDistanceFunction;
import util.object.roadnetwork.RoadNetworkGraph;
import util.object.roadnetwork.RoadNode;
import util.object.roadnetwork.RoadWay;

import java.io.IOException;
import java.util.List;

/**
 * This class index road network using an R tree
 */
public class RTreeIndexing {
	// load road nodes
	
	private static RTree<String, Line> rTree;
	private static RoadNetworkGraph currMap;
	
	RTreeIndexing(RoadNetworkGraph currMap) {
		RTreeIndexing.currMap = currMap;
		buildTree();
	}
	
	/**
	 * Initialize rTree
	 *
	 * @throws IOException file not found
	 */
	public static void initialize() throws IOException {
		rTree = buildTree();
	}
	
	/**
	 * Add polylines to rtree. One polyline is allowed to contain multiple simple lines.
	 * Each simple line is an geometric object in rtree with a unique id in the tree: polylineID + startNodeId + endNodeID
	 *
	 * @return RTree
	 */
	private static RTree<String, Line> buildTree() {
		RTree<String, Line> rTree = RTree.star().create();
		
		for (RoadWay way : currMap.getWays()) {
			String polylineID = way.getID();
			for (int i = 0; i < way.getNodes().size() - 1; i++) {
				RoadNode startNode = way.getNode(i);
				double[] curCoord = new double[]{startNode.lon(), startNode.lat()};
				
				RoadNode endNode = way.getNode(i);
				double[] nextCoord = new double[]{endNode.lon(), endNode.lat()};
				
				String lineID = polylineID + "|" + startNode.getID() + "|" + endNode.getID();
				rTree = rTree.add(lineID, Geometries.line(curCoord[0], curCoord[1], nextCoord[0], nextCoord[1]));
			}
		}
		
		return rTree;
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
	public static List<Entry<String, Line>> search(double lon, double lat, final double distanceM) {
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
	private static Observable<Entry<String, Line>> priSearch(double lon, double lat, final double distanceM) {
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
						
						DistanceFunction df = new GreatCircleDistanceFunction();
						return df.pointToSegmentProjectionDistance(
								lon, lat,
								line.x1(), line.y1(), line.x2(), line.y2()) < distanceM;
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
	private static Rectangle createBounds(final Position from, final double distanceKm) {
		// this calculates a pretty accurate bounding box. Depending on the
		// performance you require you wouldn't have to be this accurate because
		// accuracy is enforced later
		Position north = from.predict(distanceKm, 0);
		Position east = from.predict(distanceKm, 90);
		Position south = from.predict(distanceKm, 180);
		Position west = from.predict(distanceKm, 270);
		
		return Geometries.rectangle(west.getLon(), south.getLat(), east.getLon(), north.getLat());
	}
}
