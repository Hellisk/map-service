package algorithm.mapmatching.weightBased;


import algorithm.mapmatching.MapMatchingMethod;
import util.dijkstra.RoutingGraph;
import util.function.DistanceFunction;
import util.index.rtree.RTreeIndexing;
import util.object.roadnetwork.RoadNetworkGraph;
import util.object.spatialobject.Point;
import util.object.spatialobject.Segment;
import util.object.spatialobject.Trajectory;
import util.object.structure.Pair;
import util.object.structure.PointMatch;
import util.object.structure.SimpleTrajectoryMatchResult;
import util.object.structure.Triplet;
import util.settings.BaseProperty;

import java.io.Serializable;
import java.util.*;

/**
 * Subsequent MM
 * Implementation of initial map-matching in Quddus, M., & Washington, S. (2015).
 */
public class WeightBasedMapMatching implements MapMatchingMethod, Serializable {

    private double candidateRange;
    private DistanceFunction distFunc;
    private RTreeIndexing rtree;
    private RoutingGraph routingGraph;

    //	private double dijkstraThreshold;
    private double headingWC;
    private double bearingWC;
    private double pdWC;
    private double shortestPathWC;

//	private List<Pair<Integer, List<String>>> outputRouteMatchResult = new ArrayList<>();
//	private List<Pair<Integer, List<PointMatch>>> outputPointMatchResult = new ArrayList<>();

    public WeightBasedMapMatching(RoadNetworkGraph roadMap, BaseProperty property) {
        this.distFunc = roadMap.getDistanceFunction();
        this.candidateRange = property.getPropertyInteger("algorithm.mapmatching.CandidateRange");
        this.rtree = new RTreeIndexing(roadMap);
        this.routingGraph = new RoutingGraph(roadMap, false, property);

//		this.dijkstraThreshold = property.getPropertyDouble("algorithm.mapmatching.wgt.DijkstraThreshold");
        this.headingWC = property.getPropertyDouble("algorithm.mapmatching.wgt.HeadingWC");
        this.bearingWC = property.getPropertyDouble("algorithm.mapmatching.wgt.BearingWC");
        this.pdWC = property.getPropertyDouble("algorithm.mapmatching.wgt.PDWC");
        this.shortestPathWC = property.getPropertyDouble("algorithm.mapmatching.wgt.ShortestPathWC");
    }

    /**
     * Find all shortest paths between each candidate pair
     *
     * @param sources      candidate set of initial point
     * @param destinations candidate set of second point
     * @param maxDistance  searching threshold
     * @return Map<Pair < sourcePM, destinationPM>, Pair<shortestPathLength, PathSequence>>
     */
    private Map<Pair<PointMatch, PointMatch>, Pair<Double, List<String>>> getAllShortestPaths(
            List<PointMatch> destinations, List<PointMatch> sources, double maxDistance) {

        Map<Pair<PointMatch, PointMatch>, Pair<Double, List<String>>> shortestPaths = new HashMap<>();
        for (PointMatch source : sources) {

            // List<DestinationPM, shortestPathLength, Path>
            List<Triplet<PointMatch, Double, List<String>>> shortestPathToDestPm
                    = Utilities.getShortestPaths(routingGraph, destinations, source, maxDistance);

            for (Triplet<PointMatch, Double, List<String>> triplet : shortestPathToDestPm) {
                shortestPaths.put(new Pair<>(source, triplet._1()), new Pair<>(triplet._2(), triplet._3()));
            }
        }
        return shortestPaths;
    }

    /**
     * Initial map-matching
     */
    private Pair<PointMatch, Integer> initialMM(
            Trajectory trajectory, int sampleIndex, List<String> matchedWaySequence,
            Map<Integer, PointMatch> matchedPointSequence) {

        Map<Pair<PointMatch, PointMatch>, Pair<Double, List<String>>> candiPaths = new HashMap<>();

        int iterations = 0;
        while (candiPaths.size() == 0 && sampleIndex < trajectory.size() - 1) {
            List<PointMatch> firstCandiPMs = rtree.searchNeighbours(trajectory.get(sampleIndex), candidateRange);
            List<PointMatch> secCandiPMs = rtree.searchNeighbours(trajectory.get(sampleIndex + 1), candidateRange);

            // double is shortest path length

            double dijkstraThreshold = getDijkstraDistance(sampleIndex, sampleIndex + 1, trajectory);
            candiPaths = getAllShortestPaths(secCandiPMs, firstCandiPMs, dijkstraThreshold);

            sampleIndex += 1;
            iterations += 1;
        }

        while (iterations > 1) {
            matchedPointSequence.put(sampleIndex - iterations, new PointMatch(distFunc));
            iterations -= 1;
        }

        if (candiPaths.size() == 0) {
            if (!matchedPointSequence.containsKey(sampleIndex - 1)) {
                matchedPointSequence.put(sampleIndex - 1, new PointMatch(distFunc));
            }
            matchedPointSequence.put(sampleIndex, new PointMatch(distFunc));
            return new Pair<>(new PointMatch(new Point(distFunc),
                    new Segment(distFunc), "***"), sampleIndex + 1);
        }

        // double is tws
        double dijkstraThreshold = getDijkstraDistance(sampleIndex - 1, sampleIndex, trajectory);
        Queue<Pair<Pair<PointMatch, PointMatch>, Pair<Double, List<String>>>> scoredCandiPaths =
                Utilities.rankCandiMatches(candiPaths, trajectory.get(sampleIndex - 1), trajectory.get(sampleIndex), dijkstraThreshold,
                        trajectory.get(sampleIndex).heading(), headingWC, bearingWC, pdWC, shortestPathWC);

        List<String> pmIds = scoredCandiPaths.peek()._2()._2();
        for (String pmId : pmIds) {
            matchedWaySequence.add(pmId.split("\\|")[0]);
        }

        if (!matchedPointSequence.containsKey(sampleIndex - 1)) {
            matchedPointSequence.put(sampleIndex - 1, scoredCandiPaths.peek()._1()._1());
        }
        matchedPointSequence.put(sampleIndex, scoredCandiPaths.peek()._1()._2());
        return new Pair<>(scoredCandiPaths.peek()._1()._2(), sampleIndex + 1);
    }

    private Pair<PointMatch, Integer> subsqtMM(
            PointMatch prevMatchedPM, Trajectory trajectory, int sampleIndex,
            List<String> matchedWaySequence, Map<Integer, PointMatch> matchedPointSequence) {
        List<PointMatch> secCandiPMs = rtree.searchNeighbours(trajectory.get(sampleIndex), candidateRange);

        // List<DestinationPM, shortestPathLength, Path>
        double dijkstraThreshold = getDijkstraDistance(sampleIndex - 1, sampleIndex, trajectory);
        List<Triplet<PointMatch, Double, List<String>>> candiPaths =
                Utilities.getShortestPaths(routingGraph, secCandiPMs, prevMatchedPM, dijkstraThreshold);

        // double is shortest path length
        Map<Pair<PointMatch, PointMatch>, Pair<Double, List<String>>> shortestPaths = new HashMap<>();
        for (Triplet<PointMatch, Double, List<String>> triplet : candiPaths) {
            shortestPaths.put(new Pair<>(prevMatchedPM, triplet._1()), new Pair<>(triplet._2(), triplet._3()));
        }

        // double is tws
        Queue<Pair<Pair<PointMatch, PointMatch>, Pair<Double, List<String>>>> scoredCandiPaths =
                Utilities.rankCandiMatches(
                        shortestPaths, trajectory.get(sampleIndex - 1), trajectory.get(sampleIndex),
                        dijkstraThreshold, trajectory.get(sampleIndex).heading(),
                        headingWC, bearingWC, pdWC, shortestPathWC);

        if (scoredCandiPaths.size() == 0) {
            // break
            return initialMM(trajectory, sampleIndex - 1, matchedWaySequence, matchedPointSequence);
        }

        List<String> pmIds = scoredCandiPaths.peek()._2()._2();
        for (String pmId : pmIds) {
            matchedWaySequence.add(pmId.split("\\|")[0]);
        }

        matchedPointSequence.put(sampleIndex, scoredCandiPaths.peek()._1()._2());
        return new Pair<>(scoredCandiPaths.peek()._1()._2(), sampleIndex + 1);
    }

    private double getDijkstraDistance(int prevSampleIndex, int curSampleIndex, Trajectory trajectory) {
        double timeDiff = trajectory.get(curSampleIndex).time() - trajectory.get(prevSampleIndex).time();
        double linearDist = distFunc.pointToPointDistance(trajectory.get(curSampleIndex).x(),
                trajectory.get(curSampleIndex).y(), trajectory.get(prevSampleIndex).x(),
                trajectory.get(prevSampleIndex).y());
        return Math.min((50 * timeDiff), linearDist * 8);
    }

    @Override
    public Pair<List<Double>, SimpleTrajectoryMatchResult> onlineMatching(final Trajectory trajectory) {
        // initialMM
        List<String> matchedWaySequence = new LinkedList<>();
        Map<Integer, PointMatch> matchedPointSequenceMap = new HashMap<>();
        Pair<PointMatch, Integer> result = initialMM(trajectory, 0, matchedWaySequence, matchedPointSequenceMap);
        while (result._2() < trajectory.size()) {
            result = subsqtMM(result._1(), trajectory, result._2(), matchedWaySequence, matchedPointSequenceMap);
        }
        // Store map-matching result
        if (matchedPointSequenceMap.size() != trajectory.size()) {
            throw new RuntimeException("Output number inconsistent");
        }
        return new Pair<>(new ArrayList<>(), new SimpleTrajectoryMatchResult(trajectory.getID(),
                new LinkedList<>(matchedPointSequenceMap.values()), matchedWaySequence));
    }
}
