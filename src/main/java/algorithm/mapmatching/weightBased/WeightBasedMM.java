package algorithm.mapmatching.weightBased;


import util.dijkstra.RoutingGraph;
import util.function.DistanceFunction;
import util.index.rtree.RTreeIndexing;
import util.object.roadnetwork.RoadNetworkGraph;
import util.object.spatialobject.Point;
import util.object.spatialobject.Segment;
import util.object.spatialobject.Trajectory;
import util.object.structure.Pair;
import util.object.structure.PointMatch;
import util.object.structure.Triplet;
import util.settings.BaseProperty;

import java.io.IOException;
import java.util.*;

/**
 * Subsequent MM
 * Implementation of initial map-matching in Quddus, M., & Washington, S. (2015).
 */
public class WeightBasedMM {

    private double candidateRange = 0d;
    private DistanceFunction distFunc;
    private BaseProperty property;
    private RTreeIndexing rtree;
    private RoutingGraph routingGraph;

    private double djkstraThreshold = 0d;
    private double headingWC = 0d;
    private double bearingWC = 0d;
    private double pdWC = 0d;
    private double shortestPathWC = 0d;

    private List<String> matchedWaySequence = new ArrayList<>();
    private List<PointMatch> matchedPointSequence = new ArrayList<>();
    private List<Pair<Integer, List<String>>> outputRouteMatchResult = new ArrayList<>();
    private List<Pair<Integer, List<PointMatch>>> outputPointMatchResult = new ArrayList<>();
    private String routeMatchResultFolder = null;
    private String pointMatchingResultFolder = null;
    private String traFolder = null;

    /**
     * @param roadMap
     * @param property
     * @param djkstraThreshold
     * @param headingWC
     * @param bearingWC
     * @param pdWC
     * @param shortestPathWC
     */
    public WeightBasedMM(
            RoadNetworkGraph roadMap, BaseProperty property,
            double djkstraThreshold,
            double headingWC, double bearingWC, double pdWC, double shortestPathWC) {
        this.property = property;
        this.distFunc = roadMap.getDistanceFunction();
        this.candidateRange = property.getPropertyInteger("algorithm.mapmatching.CandidateRange");
        this.rtree = new RTreeIndexing(roadMap);
        this.routingGraph = new RoutingGraph(roadMap, false, this.property);

        this.djkstraThreshold = djkstraThreshold;
        this.headingWC = headingWC;
        this.bearingWC = bearingWC;
        this.pdWC = pdWC;
        this.shortestPathWC = shortestPathWC;
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
    private Pair<PointMatch, Integer> initialMM(Trajectory trajectory, int timestamp) {

        Map<Pair<PointMatch, PointMatch>, Pair<Double, List<String>>> candiPaths = new HashMap<>();

        while (candiPaths.size() == 0 && timestamp < trajectory.size() - 1) {
            List<PointMatch> firstCandiPMs = rtree.searchNeighbours(trajectory.get(timestamp), candidateRange);
            List<PointMatch> secCandiPMs = rtree.searchNeighbours(trajectory.get(timestamp + 1), candidateRange);

            // double is shortest path length
            candiPaths = getAllShortestPaths(secCandiPMs, firstCandiPMs, djkstraThreshold);

            timestamp += 1;
        }

        if (candiPaths.size() == 0) {
            return new Pair<>(new PointMatch(new Point(trajectory.getDistanceFunction()),
                    new Segment(trajectory.getDistanceFunction()), "***"), timestamp + 1);
        }

        // double is tws
        Queue<Pair<Pair<PointMatch, PointMatch>, Pair<Double, List<String>>>> scoredCandiPaths =
                Utilities.rankCandiMatches(
                        candiPaths, trajectory.get(timestamp), trajectory.get(timestamp + 1),
                        djkstraThreshold, trajectory.get(timestamp + 1).heading(),
                        headingWC, bearingWC, pdWC, shortestPathWC);

        List<String> pmIds = scoredCandiPaths.peek()._2()._2();
        for (String pmId : pmIds) {
            matchedWaySequence.add(pmId.split("\\|")[0]);
        }
        matchedPointSequence.add(scoredCandiPaths.peek()._1()._1());
        matchedPointSequence.add(scoredCandiPaths.peek()._1()._2());
        return new Pair<>(scoredCandiPaths.peek()._1()._2(), timestamp + 1);
    }

    private Pair<PointMatch, Integer> subsqtMM(
            PointMatch prevMatchedPM, Trajectory trajectory, int timestamp) throws IOException {
        List<PointMatch> secCandiPMs = rtree.searchNeighbours(trajectory.get(timestamp), candidateRange);

        // List<DestinationPM, shortestPathLength, Path>
        List<Triplet<PointMatch, Double, List<String>>> candiPaths =
                Utilities.getShortestPaths(routingGraph, secCandiPMs, prevMatchedPM, djkstraThreshold);

        // double is shortest path length
        Map<Pair<PointMatch, PointMatch>, Pair<Double, List<String>>> shortestPaths = new HashMap<>();
        for (Triplet<PointMatch, Double, List<String>> triplet : candiPaths) {
            shortestPaths.put(new Pair<>(prevMatchedPM, triplet._1()), new Pair<>(triplet._2(), triplet._3()));
        }

        // double is tws
        Queue<Pair<Pair<PointMatch, PointMatch>, Pair<Double, List<String>>>> scoredCandiPaths =
                Utilities.rankCandiMatches(
                        shortestPaths, trajectory.get(timestamp - 1), trajectory.get(timestamp),
                        djkstraThreshold, trajectory.get(timestamp).heading(),
                        headingWC, bearingWC, pdWC, shortestPathWC);

        if (scoredCandiPaths.size() == 0) {
            return initialMM(trajectory, timestamp - 1);
        }

        List<String> pmIds = scoredCandiPaths.peek()._2()._2();
        for (String pmId : pmIds) {
            matchedWaySequence.add(pmId.split("\\|")[0]);
        }

        matchedPointSequence.add(scoredCandiPaths.peek()._1()._2());
        return new Pair<>(scoredCandiPaths.peek()._1()._2(), timestamp + 1);
    }

    public void doMatching(final Trajectory trajectory) throws IOException {
        // initialMM

        Pair<PointMatch, Integer> result = initialMM(trajectory, 0);
        while (result._2() < trajectory.size() - 1) {
            result = subsqtMM(result._1(), trajectory, result._2());
        }
        // Store map-matching result
        outputRouteMatchResult.add(new Pair<>(Integer.parseInt(trajectory.getID()), matchedWaySequence));
        outputPointMatchResult.add(new Pair<>(Integer.parseInt(trajectory.getID()), matchedPointSequence));

        matchedPointSequence = new ArrayList<>();
        matchedWaySequence = new ArrayList<>();
    }

    public List<Pair<Integer, List<String>>> getOutputRouteMatchResult() {
        return outputRouteMatchResult;
    }

    public List<Pair<Integer, List<PointMatch>>> getOutputPointMatchResult() {
        return outputPointMatchResult;
    }
}
