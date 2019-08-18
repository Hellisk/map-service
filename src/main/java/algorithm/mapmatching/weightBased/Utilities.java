package algorithm.mapmatching.weightBased;

import com.github.davidmoten.rtree.Entry;
import com.github.davidmoten.rtree.geometry.Line;
import util.dijkstra.RoutingGraph;
import util.function.DistanceFunction;
import util.function.GreatCircleDistanceFunction;
import util.io.MapReader;
import util.object.roadnetwork.RoadNetworkGraph;
import util.object.spatialobject.Point;
import util.object.spatialobject.Segment;
import util.object.structure.Pair;
import util.object.structure.PointMatch;
import util.object.structure.Triplet;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;

public class Utilities {

    /**
     * search road segments in vicinity of search points
     *
     * @param radiusM searching radius around gps point
     * @return lists of candidate matches
     * @throws IOException when file not found
     */
    public static List<PointMatch> searchNeighbours(Point from, double radiusM) throws IOException {
        RoadNetworkIndexing.initialize();
        List<Entry<String, Line>> results = RoadNetworkIndexing.search(from.x(), from.y(), radiusM);
        List<PointMatch> neighbours = new ArrayList<>();

        for (Entry<String, Line> pair : results) {
            String wayId = Arrays.asList(pair.value().split("\\|")).get(0);

            double[] startNode = formatDoubles(new double[]{pair.geometry().x1(), pair.geometry().y1()});
            double[] endNode = formatDoubles(new double[]{pair.geometry().x2(), pair.geometry().y2()});

            DistanceFunction df = new GreatCircleDistanceFunction();
            Point closestPoint = df.getClosestPoint(from.x(), from.y(), startNode[0], startNode[1], endNode[0], endNode[1]);

            Segment sg = new Segment(startNode[0], startNode[1], endNode[0], endNode[1], df);

            neighbours.add(new PointMatch(closestPoint, sg, wayId));
        }
        return neighbours;
    }

    /**
     * Find the closest projection from a coord towards a set of segments
     *
     * @param point    GPS point
     * @param segments segments
     * @return the closest projection of coord on one of the segment
     * @throws IOException file not found
     */
    public static PointMatch closesProjection(
            Point point, List<Segment> segments) throws IOException {
        RoadNetworkIndexing.initialize();

        DistanceFunction df = new GreatCircleDistanceFunction();
        Point firstP = new Point(point.x(), point.y(), df);
        double minDist = 1000000;

        Point closestP = new Point(0, 0, df);
        Segment closestSeg = new Segment(0, 0, 0, 0, df);
        String wayId = "";
        for (Segment segment : segments) {
            double dist = df.pointToSegmentProjectionDistance(firstP, segment);
            if (dist < minDist) {
                closestSeg = segment;
                closestP = df.getProjection(firstP, closestSeg);
                wayId = segment.getID();
                minDist = dist;
            }
        }
        return new PointMatch(closestP, closestSeg, wayId);
    }

    /**
     * Find the shortest path from a source node to each destination node
     *
     * @param destinations a set of destination nodes
     * @param source       the source node
     * @param maxDistance  threshold
     * @return shortest paths List<DestinationPM, shortestPathLength, Path>
     */
    public static List<Triplet<PointMatch, Double, List<String>>> getShortestPaths(
            RoutingGraph routingGraph,
            List<PointMatch> destinations, PointMatch source, double maxDistance) {

        // The graph for Dijkstra shortest distance calculation
        List<Pair<Double, List<String>>> shortestPaths = routingGraph.calculateShortestDistanceList(source, destinations, maxDistance);

        List<Triplet<PointMatch, Double, List<String>>> shortestPathToDestPM = new ArrayList<>();

        for (int i = 0; i < destinations.size(); i++) {
            shortestPathToDestPM.add(
                    new Triplet<>(destinations.get(i), shortestPaths.get(i)._1(), shortestPaths.get(i)._2()));
        }

        return shortestPathToDestPM;
    }

    /**
     * Round 14 decimal places to 5 decimal places
     *
     * @param coord 14 decimal_places coords
     * @return 5 decimal_places coords
     */
    public static double[] formatDoubles(double[] coord) {
        DecimalFormat df = new DecimalFormat("#.00000");
        double x = Double.parseDouble(df.format(coord[0]));
        double y = Double.parseDouble(df.format(coord[1]));
        return new double[]{x, y};
    }

    public static RoadNetworkGraph getRoadNetworkGraph(String getMapFolder) {
        return MapReader.readMap(getMapFolder + "0.txt", false, new GreatCircleDistanceFunction());
    }

    /**
     * Round 14 decimal places to 5 decimal places
     *
     * @param number 14 decimal_places number
     * @return 5 decimal_places number
     */
    public static double formatDoubles(double number) {
        DecimalFormat df = new DecimalFormat("#.00000");
        return Double.parseDouble(df.format(number));
    }

    /**
     * Get the weight of shortest path
     *
     * @param prevPoint previous GPS point
     * @param curPoint  current GPS point
     * @param pathDist  shortest path distance between candidate pair
     * @param threshold threshold
     * @return shortest path weight
     */
    public static double shortestPathWeight(Point prevPoint, Point curPoint, double pathDist, double threshold) {
        DistanceFunction df = new GreatCircleDistanceFunction();
        double trjtryDist = df.pointToPointDistance(prevPoint.x(), prevPoint.y(), curPoint.x(), curPoint.y());
        double diff = Math.abs(trjtryDist - pathDist);

        if (diff <= threshold) {
            return 1 - diff / threshold;
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
    public static double penDistanceWeight(Point curPoint, Segment candiSegment) {
        DistanceFunction df = new GreatCircleDistanceFunction();

        double dist = df.pointToSegmentProjectionDistance(
                curPoint.x(), curPoint.y(),
                candiSegment.x1(), candiSegment.y1(), candiSegment.x2(), candiSegment.y2());

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
     * @param threshold      threshold
     * @param headingWC      WC of heading difference
     * @param bearingWC      WC of bearing difference
     * @param pdWC           WC of perpendicular distance
     * @param shortestPathWC WC of shortest path length
     * @return total weighting score
     */
    public static double getTotalWeightScore(Point prePoint, Point curPoint, double curPointDir,
                                             Point preCandi, Point curCandi,
                                             Segment candiSegment,
                                             double shortestPLen, double threshold,
                                             double headingWC, double bearingWC, double pdWC, double shortestPathWC) {
        double headingW = headingDiffWeight(prePoint, curPoint, preCandi, curCandi);
        double bearingW = bearingDiffWeight(curPointDir, candiSegment);
        double pdW = penDistanceWeight(curPoint, candiSegment);
        double shortestPathW = shortestPathWeight(prePoint, curPoint, shortestPLen, threshold);

        return formatDoubles(headingWC * headingW + bearingWC * bearingW + pdWC * pdW + shortestPathWC * shortestPathW);
    }

    /**
     * Rank the likelihood of each candidate path
     *
     * @param candiPaths     all candidate paths
     *                       <<sourcePM,destinationPM>, <shortestPathLength,shortestPathSequence>>
     * @param threshold      threshold
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
            double threshold, double vehicleDir,
            double headingWC, double bearingWC, double pdWC, double shortestPathWC) {
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
                    candiPath.getValue().hashCode(),
                    threshold, headingWC, bearingWC, pdWC, shortestPathWC);
            candiScores.add(new Pair<>(candiPath.getKey(), new Pair<>(tws, candiPath.getValue()._2())));
        }
        return candiScores;
    }


    /**
     * Get the top ranked PointMatch of current GPS point
     *
     * @param rankedCandiPaths Queue<Pair < SourcePM, DestPM>, Pair<totalWeightScore,waySequence>> Ranking of candidate paths
     * @return
     */
    public static PointMatch bestCandi(
            Queue<Pair<Pair<PointMatch, PointMatch>, Pair<Double, List<String>>>> rankedCandiPaths) {
        return rankedCandiPaths.peek()._1()._2();
    }

    /**
     * @param matchedWays
     * @param rankedCandiPaths
     * @return
     */
    public static void updateMatchedWays(
            List<String> matchedWays,
            Queue<Pair<Pair<PointMatch, PointMatch>, Pair<Double, List<String>>>> rankedCandiPaths) {
        List<String> pathWays = rankedCandiPaths.peek()._2()._2();
        matchedWays.addAll(pathWays);
    }

    public static void main(String[] args) {
        DistanceFunction df = new GreatCircleDistanceFunction();
        Point p1 = new Point(116.42743, 39.95933, df);
        Point p2 = new Point(116.42804, 39.95936, df);

        Point preCandi = new Point(116.42743, 39.95949, df);
        Point curCandi = new Point(116.42696, 39.95948, df);

        Segment segment = new Segment(116.42755, 39.95949, 116.42696, 39.95948, df);


        System.out.println(headingDiffWeight(p1, p2, preCandi, curCandi));
        System.out.println(bearingDiffWeight(86.0, segment));
        System.out.println(penDistanceWeight(p2, segment));
        System.out.println(shortestPathWeight(p1, p2, 1, 1000));
    }
}
