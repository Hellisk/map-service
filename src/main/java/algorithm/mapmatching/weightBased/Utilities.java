package algorithm.mapmatching.weightBased;

import util.dijkstra.RoutingGraph;
import util.object.spatialobject.Point;
import util.object.spatialobject.Segment;
import util.object.structure.Pair;
import util.object.structure.PointMatch;
import util.object.structure.Triplet;

import java.text.DecimalFormat;
import java.util.*;

//import util.function.DistanceFunction;

public class Utilities {
	
	
	/**
	 * Find the shortest path from a source node to each destination node
	 *
	 * @param destinations   a set of destination nodes
	 * @param source         the source node
	 * @param referencePoint
	 * @param maxDistance    threshold
	 * @return shortest paths List<DestinationPM, shortestPathLength, Path>
	 */
	public static List<Triplet<PointMatch, Double, List<String>>> getShortestPaths(RoutingGraph routingGraph,
																				   List<PointMatch> destinations, PointMatch source, Point referencePoint, double maxDistance) {
		
		// The graph for Dijkstra shortest distance calculation
//		List<Pair<Double, List<String>>> shortestPaths = routingGraph.calculateOneToNDijkstraSP(source, destinations, maxDistance);
		List<Pair<Double, List<String>>> shortestPaths = routingGraph.calculateOneToNAStarSP(source, destinations, referencePoint,
				maxDistance);
		
		List<Triplet<PointMatch, Double, List<String>>> shortestPathToDestPM = new ArrayList<>();
		
		for (int i = 0; i < destinations.size(); i++) {
			if (shortestPaths.get(i)._1() != Double.POSITIVE_INFINITY) {
				shortestPathToDestPM.add(new Triplet<>(destinations.get(i), shortestPaths.get(i)._1(), shortestPaths.get(i)._2()));
			}
		}
		
		return shortestPathToDestPM;
	}
	
	/**
	 * Round 14 decimal places to 5 decimal places
	 *
	 * @param number 14 decimal_places number
	 * @return 5 decimal_places number
	 */
	private static double formatDoubles(double number) {
		DecimalFormat df = new DecimalFormat("#.00000");
		return Double.parseDouble(df.format(number));
	}
	
	/**
	 * Get the weight of shortest path
	 *
	 * @param prevPoint previous GPS point
	 * @param curPoint  current GPS point
	 * @param pathDist  shortest path distance between candidate pair
	 * @return shortest path weight
	 */
    public static double shortestPathWeight(Point prevPoint, Point curPoint, double pathDist) {
		double trjtryDist = prevPoint.getDistanceFunction().pointToPointDistance(prevPoint.x(), prevPoint.y(), curPoint.x(), curPoint.y());
		double diff = Math.abs(trjtryDist - pathDist);

        if (diff <= 1000) {
            return 1 - diff / 1000;
		} else {
			return 0;
		}
	}
	
	/**
	 * Get the weighting score for the difference of heading between
	 * vehicle trajectory heading between p1 & p2 and direction between c1 & c2
	 *
	 * @param prePoint p1
	 * @param curPoint p2
	 * @param preCandi c1
	 * @param curCandi c2
	 * @return weighting score of heading difference
	 */
	public static double headingDiffWeight(Point prePoint, Point curPoint, Point preCandi, Point curCandi) {
		double vehicleHeading = computeHeading(prePoint.x(), prePoint.y(), curPoint.x(), curPoint.y());
		double candiPathHeading = computeHeading(preCandi.x(), preCandi.y(), curCandi.x(), curCandi.y());
		double headingDiff = Math.abs(vehicleHeading - candiPathHeading);
		return Math.abs(Math.cos(Math.toRadians(headingDiff)));
	}
	
	/**
	 * Get the weighting score of bearing difference between vehicle direction and candidate segment direction
	 *
	 * @param curPointDir  vehicle direction
	 * @param candiSegment candidate segment
	 * @return weighting score of bearing difference
	 */
	public static double bearingDiffWeight(double curPointDir, Segment candiSegment) {
		double candiSegDir = computeHeading(candiSegment.x1(), candiSegment.y1(), candiSegment.x2(), candiSegment.y2());
		double bearingDiff = Math.abs(candiSegDir - curPointDir);
		return Math.abs(Math.cos(Math.toRadians(bearingDiff)));
	}
	
	/**
	 * Get the weighting score of perpendicular distance
	 *
	 * @param curPoint     candidate point
	 * @param candiSegment candidate segment
	 * @return weighting score of perpendicular distance
	 */
	// TODO check is projection distance is required.
	public static double penDistanceWeight(Point curPoint, Segment candiSegment) {
		
		double dist = curPoint.getDistanceFunction().pointToSegmentProjectionDistance(
				curPoint.x(), curPoint.y(), candiSegment.x1(), candiSegment.y1(), candiSegment.x2(), candiSegment.y2());
		
		if (dist <= 200) {
			return 1 - dist / 200;
		} else {
			return 0;
		}
	}
	
	/**
	 * Returns the heading from one lonlat to another lonlat. Headings are
	 * expressed in degrees clockwise from North within the range [0,360].
	 *
	 * @return The heading in degrees clockwise from north.
	 */
	public static double computeHeading(double fromLon, double fromLati, double toLon, double toLati) {
		double fromLng = Math.toRadians(fromLon);
		double fromLat = Math.toRadians(fromLati);
		double toLng = Math.toRadians(toLon);
		double toLat = Math.toRadians(toLati);
		double dLng = toLng - fromLng;
		double heading = Math.atan2(
				Math.sin(dLng) * Math.cos(toLat),
				Math.cos(fromLat) * Math.sin(toLat) - Math.sin(fromLat) * Math.cos(toLat) * Math.cos(dLng));
		return wrap(Math.toDegrees(heading), -180, 180);
	}
	
	private static double wrap(double n, double min, double max) {
		return (n >= min && n < max) ? n : (mod(n - min, max - min) + min);
	}
	
	private static double mod(double x, double m) {
		return ((x % m) + m) % m;
		
	}
	
	/**
	 * Get a TWS for each candidate match
	 *
	 * @param prePoint       previous GPS point
	 * @param curPoint       current GPS point
	 * @param curPointDir    vehicle direction
	 * @param preCandi       previously matched point
	 * @param curCandi       current candidate matching point
	 * @param candiSegment   candidate matching segment
	 * @param shortestPLen   shortest path between previously matched point to current candidate matching point
	 * @param headingWC      WC of heading difference
	 * @param bearingWC      WC of bearing difference
	 * @param pdWC           WC of perpendicular distance
	 * @param shortestPathWC WC of shortest path length
	 * @return total weighting score
	 */
	public static double getTotalWeightScore(Point prePoint, Point curPoint, double curPointDir,
                                             Point preCandi, Point curCandi,
                                             Segment candiSegment,
                                             double shortestPLen,
                                             double headingWC, double bearingWC, double pdWC, double shortestPathWC) {
		double headingW = headingDiffWeight(prePoint, curPoint, preCandi, curCandi);
		double bearingW = bearingDiffWeight(curPointDir, candiSegment);
		double pdW = penDistanceWeight(curPoint, candiSegment);
        double shortestPathW = shortestPathWeight(prePoint, curPoint, shortestPLen);
		return formatDoubles(headingWC * headingW + bearingWC * bearingW + pdWC * pdW + shortestPathWC * shortestPathW);
	}
	
	/**
	 * Rank the likelihood of each candidate path
	 *
	 * @param candiPaths     all candidate paths
	 *                       <<sourcePM,destinationPM>, <shortestPathLength,shortestPathSequence>>
	 * @param vehicleDir     vehicle direction (direction of second GPS point)
	 * @param firstPoint     first GPS point
	 * @param secondPoint    second GPS point
	 * @param headingWC      WC of heading difference
	 * @param bearingWC      WC of bearing difference
	 * @param pdWC           WC of perpendicular distance
	 * @param shortestPathWC WC of shortest path length
	 * @return Queue<Pair < SourcePM, DestPM>, Pair<totalWeightScore, waySequence>> Ranking of candidate paths
	 */
	public static Queue<Pair<Pair<PointMatch, PointMatch>, Pair<Double, List<String>>>> rankCandiMatches(
            Map<Pair<PointMatch, PointMatch>, Pair<Double, List<String>>> candiPaths,
            Point firstPoint, Point secondPoint,
            double vehicleDir, double headingWC, double bearingWC, double pdWC,
            double shortestPathWC) {
		/* Customize a pq comparator */
		Comparator<Pair<Pair<PointMatch, PointMatch>, Pair<Double, List<String>>>> pathComparator =
				new Comparator<Pair<Pair<PointMatch, PointMatch>, Pair<Double, List<String>>>>() {
					@Override
					public int compare(Pair<Pair<PointMatch, PointMatch>, Pair<Double, List<String>>> o1,
									   Pair<Pair<PointMatch, PointMatch>, Pair<Double, List<String>>> o2) {
						if (o1._2()._1() < o2._2()._1()) {
							return 1;
						} else if (o1._2()._1() > o2._2()._1()) {
							return -1;
						} else {
							return 0;
						}
					}
				};
		Queue<Pair<Pair<PointMatch, PointMatch>, Pair<Double, List<String>>>> candiScores =
				new PriorityQueue<>(pathComparator);
		
		for (Map.Entry<Pair<PointMatch, PointMatch>, Pair<Double, List<String>>> candiPath : candiPaths.entrySet()) {
			Point sourcePoint = candiPath.getKey()._1().getMatchPoint();
			Point destPoint = candiPath.getKey()._2().getMatchPoint();
			Segment destSegment = candiPath.getKey()._2().getMatchedSegment();
			
			double tws = Utilities.getTotalWeightScore(
					firstPoint, secondPoint, vehicleDir, sourcePoint, destPoint, destSegment,
                    candiPath.getValue()._1(), headingWC, bearingWC, pdWC, shortestPathWC);
			candiScores.add(new Pair<>(candiPath.getKey(), new Pair<>(tws, candiPath.getValue()._2())));
		}
		return candiScores;
	}
}
