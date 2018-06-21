package edu.uq.dke.mapupdate.mapmatching.hmm;

import edu.uq.dke.mapupdate.util.dijkstra.RoutingGraph;
import edu.uq.dke.mapupdate.util.function.GreatCircleDistanceFunction;
import edu.uq.dke.mapupdate.util.index.grid.Grid;
import edu.uq.dke.mapupdate.util.index.grid.GridPartition;
import edu.uq.dke.mapupdate.util.object.datastructure.*;
import edu.uq.dke.mapupdate.util.object.roadnetwork.RoadNetworkGraph;
import edu.uq.dke.mapupdate.util.object.roadnetwork.RoadWay;
import edu.uq.dke.mapupdate.util.object.spatialobject.Point;
import edu.uq.dke.mapupdate.util.object.spatialobject.STPoint;
import edu.uq.dke.mapupdate.util.object.spatialobject.Segment;
import edu.uq.dke.mapupdate.util.object.spatialobject.Trajectory;

import java.util.*;

/**
 * Implementation of Hidden Markov Model map-matching algorithm,
 * using the hmm-oldversion.
 * <p>
 * Based on Newson, Paul, and John Krumm. "Hidden Markov map matching through noise and sparseness."
 * Proceedings of the 17th ACM SIGSPATIAL International Conference on Advances in Geographic
 * Information Systems. ACM, 2009.
 *
 * @author uqdalves, uqpchao
 * @link https://github.com/bmwcarit/.
 */
@SuppressWarnings("serial")
public class HMMMapMatching {

    /**
     * The distance method to use between points
     */
    private final GreatCircleDistanceFunction distanceFunction;

    /**
     * The probabilities of the HMM lattice
     */
    private final HMMProbabilities hmmProbabilities = new HMMProbabilities();

    /**
     * The distance threshold for the candidate points
     */
    private final double candidateRange;

    /**
     * The distance threshold for an unmatched point, the unmatched point should have no candidate within this distance
     */
    private final double gapExtensionRange;

    /**
     * The k top-ranked matching result sequence that will be stored
     */
    private final int rankLength;
    /**
     * The graph for Dijkstra shortest distance calculation
     */
    private final RoutingGraph shortestPathGraph;

    /**
     * The road network graph for candidate generation
     */
    private final RoadNetworkGraph roadNetworkGraph;

    /**
     * The grid index for candidate generation
     */
    private Grid<SegmentIndexItem> grid;

    /**
     * Unmatched trajectory segment list
     */
    private List<Trajectory> unmatchedTrajectoryList = new ArrayList<>();

    /**
     * the index of the last point before current broken position, -1: complete matching sequence
     */
    private int currBreakPointIndex = -1;

    /**
     * the threshold for extra indexing point, segments that exceed such threshold will generate extra indexing point(s)
     */
    private double limitedLength;

    /**
     * Creates a new HMM map-matching method with the
     * given distance function.
     *
     * @param distFunc       The point-to-node distance function to use.
     * @param candidateRange The distance threshold for the candidate points.
     */
    HMMMapMatching(GreatCircleDistanceFunction distFunc, double candidateRange, double gapExtensionRange, int rankLength, RoadNetworkGraph
            roadNetworkGraph) {
        this.distanceFunction = distFunc;
        this.candidateRange = candidateRange;
        this.limitedLength = (2 * Math.sqrt(2) - 2) * candidateRange;   // given such length limit, none of the candidate segment can
        // escape the grid search
        this.shortestPathGraph = new RoutingGraph(roadNetworkGraph);
        this.gapExtensionRange = gapExtensionRange;
        this.rankLength = rankLength;
        buildGridIndex(roadNetworkGraph);   // build grid index
        this.roadNetworkGraph = roadNetworkGraph;
    }

    TrajectoryMatchResult doMatching(Trajectory trajectory) {
        // Compute the candidate road segment list for every GPS point through grid index
//        long startTime = System.currentTimeMillis();

        final Map<STPoint, Collection<PointMatch>> candidatesMap = new HashMap<>(); //Map each point to a list of candidate nodes
        computeCandidatesFromIndex(trajectory, candidatesMap);
//        computeCandidates(trajectory);
//        System.out.println("Time cost on candidate generation is: " + (System.currentTimeMillis() - startTime));
        boolean isBrokenTraj = false;
//        Set<Integer> currBreakIndex = new LinkedHashSet<>();    // points that will be put into the unmatched trajectory
        Map<Integer, Boolean> breakPoints = new LinkedHashMap<>();  // the points that break the connectivity and the reason, =true if
        // the break is caused by no candidate, =false if it is caused by broken transition

        ViterbiAlgorithm<PointMatch, STPoint, RoadPath> viterbi = new ViterbiAlgorithm<>(rankLength);
        TimeStep<PointMatch, STPoint, RoadPath> prevTimeStep = null;
        Map<Integer, List<SequenceState<PointMatch, STPoint, RoadPath>>> rankedRoadPositionList = new LinkedHashMap<>(rankLength);
        for (int i = 0; i < rankLength; i++)       // first fill all top k results with empty array
            rankedRoadPositionList.computeIfAbsent(i, k -> new ArrayList<>());
        // store all states during the process, prepare for the breaks
//        Map<Integer, TimeStep<PointMatch, STPoint, RoadPath>> timeStepList = new HashMap<>();
        Map<PointMatch, Double> lastMatchSequenceMessage = new HashMap<>();
        Map<PointMatch, ViterbiAlgorithm.ExtendedState<PointMatch, STPoint, RoadPath>> lastExtendedStates = new HashMap<>();
//        List<Collection<PointMatch>> candidateList = new ArrayList<>();
//        List<Map<PointMatch, ViterbiAlgorithm.ExtendedState<PointMatch, STPoint, RoadPath>>> extendedStates = new ArrayList<>();
//        long endTime = System.currentTimeMillis();
//        int type = 0;
        // check if every point has candidates
        // build the lattice

        for (int i = 0; i < trajectory.size(); i++) {
//            System.out.println("Last point spent:" + (System.currentTimeMillis() - endTime) + "ms, which was " + type);
//            endTime = System.currentTimeMillis();
            Collection<PointMatch> candidates;
            TimeStep<PointMatch, STPoint, RoadPath> timeStep;
            STPoint gpsPoint = trajectory.get(i);
            if (candidatesMap.get(gpsPoint).size() == 0) {  // no candidate for the current point, definitely a break point
                isBrokenTraj = true;
                breakPoints.put(i, true);
                continue;
            }
            candidates = candidatesMap.get(gpsPoint);
            timeStep = new TimeStep<>(gpsPoint, candidates);
            if (prevTimeStep == null) {     // start of the trajectory or the current matching has just been cut off
                computeEmissionProbabilities(timeStep);
                viterbi.startWithInitialObservation(timeStep.observation, timeStep.candidates,
                        timeStep.emissionLogProbabilities);
                breakPoints.remove(i);  // start the new match from the current point, so it is no longer a break point.
                // successful initialization
                prevTimeStep = timeStep;
            } else {   // continue the matching process
                final double timeDiff = (timeStep.observation.time() - prevTimeStep.observation.time());
                if (timeDiff > 180) {   // huge time gap, split the trajectory
                    // matching result
                    if (currBreakPointIndex != -1) {
                        // we finish the matching before last break point and start a new matching sequence from break point to the
                        // current gap, the previous matching sequence is finished here
                        List<List<SequenceState<PointMatch, STPoint, RoadPath>>> temporalRoadPositions =
                                viterbi.computeMostLikelySequence(rankLength);
                        resultMerge(rankedRoadPositionList, temporalRoadPositions, trajectory, breakPoints, currBreakPointIndex + 1,
                                candidatesMap);

                        // restart the matching from the last break point
                        i = currBreakPointIndex;
                        currBreakPointIndex = -1;
                        prevTimeStep = null;
                        viterbi.setMessage(null);
                        viterbi.setLastExtendedStates(null);
                        continue;
                    }
                    List<List<SequenceState<PointMatch, STPoint, RoadPath>>> temporalRoadPositions =
                            viterbi.computeMostLikelySequence(rankLength);
                    resultMerge(rankedRoadPositionList, temporalRoadPositions, trajectory, breakPoints, i + 1, candidatesMap);

                    // set the current point as break point and restart the matching
                    breakPoints.put(i, false);
                    i--;
                    prevTimeStep = null;
                    viterbi.setMessage(null);
                    viterbi.setLastExtendedStates(null);
                    continue;
                }

                //  no time gap, continue the matching process
                computeEmissionProbabilities(timeStep);
//                endTime = System.currentTimeMillis();
                computeTransitionProbabilitiesWithConnectivity(prevTimeStep, timeStep);
//                System.out.println("shortest path:" + (System.currentTimeMillis() - endTime) + "ms");
//                endTime = System.currentTimeMillis();
                viterbi.nextStep(
                        timeStep.observation,
                        timeStep.candidates,
                        timeStep.emissionLogProbabilities,
                        timeStep.transitionLogProbabilities,
                        timeStep.roadPaths);
//                System.out.println("HMM:" + (System.currentTimeMillis() - endTime) + "ms");
//                endTime = System.currentTimeMillis();

                if (viterbi.isBroken()) {
                    // the match stops due to no connection, add the current point and its predecessor to the broken list
                    isBrokenTraj = true;
                    if (currBreakPointIndex == -1) {
                        currBreakPointIndex = i - 1;
                        while (breakPoints.containsKey(currBreakPointIndex)) {
                            currBreakPointIndex--;
                        }
                        if (currBreakPointIndex < rankedRoadPositionList.get(0).size())
                            throw new IndexOutOfBoundsException("ERROR! No preceding initiated point before break point.");
                    }
                    // mark the broken points and expect a reconnection
                    breakPoints.put(i, false);
                    viterbi.setBroken(false);
                } else {
                    // the match continues
                    if (currBreakPointIndex != -1) {
                        // remove the break flag and continue
                        currBreakPointIndex = -1;
                    }
                    breakPoints.remove(i);
                    prevTimeStep = timeStep;
                }
            }
            if (i == trajectory.size() - 1) {  // the last point
                // complete the final part of the matching sequence
                if (currBreakPointIndex != -1) {
                    // we finish the matching before last break point and start a new matching sequence from break point to the
                    // current gap and restart the match from the break point
                    List<List<SequenceState<PointMatch, STPoint, RoadPath>>> temporalRoadPositions = viterbi.computeMostLikelySequence
                            (rankLength);
                    resultMerge(rankedRoadPositionList, temporalRoadPositions, trajectory, breakPoints, currBreakPointIndex + 1,
                            candidatesMap);

                    // restart the matching from the last break point
                    i = currBreakPointIndex;
                    currBreakPointIndex = -1;
                    prevTimeStep = null;
                    viterbi.setLastExtendedStates(null);
                    viterbi.setMessage(null);
                } else {
                    List<List<SequenceState<PointMatch, STPoint, RoadPath>>> temporalRoadPositions = viterbi.computeMostLikelySequence
                            (rankLength);
                    resultMerge(rankedRoadPositionList, temporalRoadPositions, trajectory, breakPoints, trajectory.size(), candidatesMap);
                }
            }
        }

        // Check whether the HMM occurred in the last time step
        if (viterbi.isBroken()) {
            throw new RuntimeException("ERROR! The hmm break still exists after the trajectory is processed.");
        }

        for (int i = 0; i < rankedRoadPositionList.size(); i++) {
            rankedRoadPositionList.get(i).sort(Comparator.comparingLong(m -> m.observation.time()));
        }
//        System.out.println("Time cost on matching is: " + (System.currentTimeMillis() - startTime));

        if (isBrokenTraj) {
            // generate unmatched trajectories
            List<Integer> breakPointList = new ArrayList<>(breakPoints.keySet());
//            List<Integer> breakPointList = new ArrayList<>(breakPoints);
            breakPointList.sort(Comparator.comparingInt(m -> m));
            Set<Integer> extendedBreakPoints = simpleBreakPointExtension(breakPointList, trajectory, candidatesMap);
            if (extendedBreakPoints.isEmpty()) {
                System.out.println("The break point(s) cannot be extended and thus removed. No unmatched trajectory output");
            } else {
                List<Integer> extendedBreakPointList = new ArrayList<>(extendedBreakPoints);
                extendedBreakPointList.sort(Comparator.comparingInt(m -> m));
                int start = extendedBreakPointList.get(0);
                int end;
                for (int i = 1; i < extendedBreakPointList.size(); i++) {
                    if (extendedBreakPointList.get(i) != extendedBreakPointList.get(i - 1) + 1) {
                        end = extendedBreakPointList.get(i - 1) + 1;
                        unmatchedTrajectoryList.add(trajectory.subTrajectory(start, end));
                        start = extendedBreakPointList.get(i);
                    }
                }
                unmatchedTrajectoryList.add(trajectory.subTrajectory(start, extendedBreakPointList.get(extendedBreakPointList.size() - 1)
                        + 1));
            }
        } else
            System.out.print("Complete match result with no break. ");
        return getResult(trajectory, rankedRoadPositionList, viterbi.getProbabilities());
    }

    private Set<Integer> simpleBreakPointExtension(List<Integer> breakPointList, Trajectory trajectory, Map<STPoint,
            Collection<PointMatch>> candidatesMap) {
        Set<Integer> extendedBreakPoints = new LinkedHashSet<>();
        int lastUnmatchedPoint = 0;
        for (int i : breakPointList) {
            boolean hasNeighbour = false;
            for (int p = 1; i - p > lastUnmatchedPoint; p++) {
                if (findMinDist(trajectory.get(i - p), candidatesMap.get(trajectory.get(i - p))) > gapExtensionRange) {
                    extendedBreakPoints.add(i - p);
                } else {
                    if (p != 1) {
                        hasNeighbour = true;
                    }
                    break;
                }
            }
            for (int p = 1; i + p < trajectory.size(); p++) {
                if (findMinDist(trajectory.get(i + p), candidatesMap.get(trajectory.get(i + p))) > gapExtensionRange) {
                    extendedBreakPoints.add(i + p);
                    hasNeighbour = true;
                } else {
                    if (p == 1 && !hasNeighbour)
                        break;
                    extendedBreakPoints.add(i);
                    lastUnmatchedPoint = i + p - 1;
                    hasNeighbour = false;
                    break;
                }
            }
            if (hasNeighbour) { // if no successive point is extendable
                extendedBreakPoints.add(i);
                lastUnmatchedPoint = i;
            }
        }
        return extendedBreakPoints;
    }

    private PointMatch findNearestMatch(STPoint trajPoint, Collection<PointMatch> trajPointMatches) {
        PointMatch nearestPointMatch = trajPointMatches.iterator().next();
        double distance = Double.POSITIVE_INFINITY;
        for (PointMatch m : trajPointMatches) {
            double currDistance = getDistance(trajPoint.x(), trajPoint.y(), m.lon(), m.lat());
            if (currDistance < distance) {
                nearestPointMatch = m;
                distance = currDistance;
            }
        }
        return nearestPointMatch;
    }

    private double findMinDist(STPoint stPoint, Collection<PointMatch> pointMatches) {
        double minDistance = Double.POSITIVE_INFINITY;
        for (PointMatch p : pointMatches) {
            double dist = distanceFunction.distance(p.getMatchPoint(), stPoint);
            minDistance = dist < minDistance ? dist : minDistance;
        }
        return minDistance;
    }

    /**
     * Compute the candidates list for every GPS point using a radius query.
     *
     * @param pointsList List of GPS trajectory points to map.
     */
    private void computeCandidates(Collection<STPoint> pointsList, Map<STPoint, Collection<PointMatch>> candidatesMap) {
        double distance;
        // for every point find the nodes within a distance = 'candidateRange'
        for (STPoint gpsPoint : pointsList) {
            candidatesMap.put(gpsPoint, new ArrayList<>());
            for (RoadWay way : roadNetworkGraph.getWays()) {
                for (Segment s : way.getEdges()) {
                    distance = distanceFunction.pointToSegmentDistance(gpsPoint, s);
                    if (distance <= candidateRange) {
                        Point matchPoint = distanceFunction.findClosestPoint(gpsPoint, s);
                        PointMatch currPointMatch = new PointMatch(matchPoint, s, way.getId());
                        candidatesMap.get(gpsPoint).add(currPointMatch);
                    }
                }
            }
        }
    }

    /**
     * Compute the candidates list for every GPS point using a radius query.
     *
     * @param pointsList    List of GPS trajectory points to map.
     * @param candidatesMap the candidate list for every trajectory point
     */
    private void computeCandidatesFromIndex(Collection<STPoint> pointsList, Map<STPoint, Collection<PointMatch>> candidatesMap) {
        int candidateCount = 0;
        for (STPoint p : pointsList) {
            Set<String> candidateFilter = new HashSet<>();
            // As we set the grid size as the candidateRange, only the partition that contains the query point and its neighbouring
            // partitions can potentially generate candidates
            candidatesMap.put(p, new ArrayList<>());
            List<GridPartition<SegmentIndexItem>> partitionList = new ArrayList<>();
            partitionList.add(this.grid.partitionSearch(p.x(), p.y()));
            partitionList.addAll(this.grid.adjacentPartitionSearch(p.x(), p.y()));
            for (GridPartition<SegmentIndexItem> partition : partitionList) {
                if (partition != null)
                    for (XYObject<SegmentIndexItem> item : partition.getObjectsList()) {
                        SegmentIndexItem indexItem = item.getSpatialObject();
                        if (!candidateFilter.contains(indexItem.getSegmentElement().x1() + "," + indexItem.getSegmentElement().y1() + "_" +
                                indexItem.getSegmentElement().x2() + "," + indexItem.getSegmentElement().y2() + "_" + indexItem.getRoadID())) {
                            Point matchingPoint = distanceFunction.findClosestPoint(p, indexItem.getSegmentElement());
                            if (distanceFunction.distance(p, matchingPoint) < candidateRange) {
                                PointMatch candidate = new PointMatch(matchingPoint, indexItem.getSegmentElement(), indexItem.getRoadID());
                                candidatesMap.get(p).add(candidate);
                                candidateCount++;
                                candidateFilter.add(indexItem.getSegmentElement().x1() + "," + indexItem.getSegmentElement().y1() + "_" +
                                        indexItem.getSegmentElement().x2() + "," + indexItem.getSegmentElement().y2() + "_" + indexItem.getRoadID());
                            }
                        }
                    }
            }
        }
        System.out.println("Total candidate count: " + candidateCount + ", trajectory point count: " + pointsList.size());
    }

    /**
     * Create grid index for fast candidate computing
     *
     * @param inputMap the input road network
     */
    private void buildGridIndex(RoadNetworkGraph inputMap) {

        // calculate the grid settings
        int rowNum;     // number of rows
        int columnNum;     // number of columns

        if (inputMap.getNodes().isEmpty())
            throw new IllegalStateException("Cannot create location index of empty graph!");

        // calculate the total number of rows and columns. The size of each grid cell equals the candidate range
        double lonDistance = distanceFunction.pointToPointDistance(inputMap.getMaxLon(), 0d, inputMap.getMinLon(), 0d);
        double latDistance = distanceFunction.pointToPointDistance(0d, inputMap.getMaxLat(), 0d, inputMap.getMinLat());
        columnNum = (int) Math.round(lonDistance / candidateRange);
        rowNum = (int) Math.round(latDistance / candidateRange);
        double lonPerCell = (inputMap.getMaxLon() - inputMap.getMinLon()) / columnNum;
        double latPerCell = (inputMap.getMaxLat() - inputMap.getMinLat()) / columnNum;

        // add extra grid cells around the margin to cover outside trajectory points
        this.grid = new Grid<>(columnNum + 2, rowNum + 2, inputMap.getMinLon() - lonPerCell, inputMap.getMinLat() - latPerCell, inputMap
                .getMaxLon() + lonPerCell, inputMap.getMaxLat() + latPerCell);

        System.out.println("The grid contains " + rowNum + 2 + " rows and " + columnNum + 2 + " columns");

        int pointCount = 0;
        int intermediatePointCount = 0;

        for (RoadWay t : inputMap.getWays()) {
            for (Segment s : t.getEdges()) {
                // -1: left endpoint of the segment, 0: right endpoint of the segment, >0: intermediate point
                SegmentIndexItem segmentItemLeft = new SegmentIndexItem(s, -1, t.getId(), limitedLength);
                XYObject<SegmentIndexItem> segmentIndexLeft = new XYObject<>(segmentItemLeft.x(), segmentItemLeft.y(), segmentItemLeft);
                SegmentIndexItem segmentItemRight = new SegmentIndexItem(s, 0, t.getId(), limitedLength);
                XYObject<SegmentIndexItem> segmentIndexRight = new XYObject<>(segmentItemRight.x(), segmentItemRight.y(),
                        segmentItemRight);
                this.grid.insert(segmentIndexLeft);
                pointCount++;
                this.grid.insert(segmentIndexRight);
                pointCount++;
                // if the length of the segment is longer than two times of the candidate range, insert the intermediate points of the
                // segment
                double segmentDistance = distanceFunction.distance(s.p1(), s.p2());
                int intermediateID = 1;
                while (segmentDistance > limitedLength) {
                    SegmentIndexItem segmentItemIntermediate = new SegmentIndexItem(s, intermediateID, t.getId(), limitedLength);
                    XYObject<SegmentIndexItem> segmentIndexIntermediate = new XYObject<>(segmentItemIntermediate.x(), segmentItemIntermediate.y(),
                            segmentItemIntermediate);
                    this.grid.insert(segmentIndexIntermediate);
                    segmentDistance = segmentDistance - limitedLength;
                    intermediateID++;
                    intermediatePointCount++;
                    pointCount++;
                }
            }
        }

        System.out.println("Grid index build successfully, total number of segment items in grid index: " + pointCount + ", number of " +
                "newly created middle points: " + intermediatePointCount);
    }

    /**
     * Compute the emission probabilities between every GPS point and its candidates.
     *
     * @param timeStep the observation and its candidate
     */
    private void computeEmissionProbabilities(TimeStep<PointMatch, STPoint, RoadPath> timeStep) {
        for (PointMatch candidate : timeStep.candidates) {
            double distance = getDistance(
                    timeStep.observation.x(), timeStep.observation.y(),
                    candidate.lon(), candidate.lat());
            timeStep.addEmissionLogProbability(candidate,
                    hmmProbabilities.emissionLogProbability(distance));
        }
    }

    /**
     * Compute the transition probabilities between every state,
     * taking the connectivity of the road nodes into account.
     *
     * @param prevTimeStep the time step of the last trajectory point
     * @param timeStep     the current time step
     */
    private void computeTransitionProbabilitiesWithConnectivity(
            TimeStep<PointMatch, STPoint, RoadPath> prevTimeStep,
            TimeStep<PointMatch, STPoint, RoadPath> timeStep) {
        final double linearDistance = getDistance(
                prevTimeStep.observation.x(), prevTimeStep.observation.y(),
                timeStep.observation.x(), timeStep.observation.y());
        final double timeDiff = (timeStep.observation.time() - prevTimeStep.observation.time());
        double maxDistance = (50 * timeDiff) < linearDistance * 8 ? 50 * timeDiff : linearDistance * 8;     // limit the maximum speed to
        // 180km/h
        for (PointMatch from : prevTimeStep.candidates) {
            List<PointMatch> candidates = new ArrayList<>(timeStep.candidates);
            List<Pair<Double, List<String>>> shortestPathResultList = shortestPathGraph.calculateShortestDistanceList(from, candidates, maxDistance);
            for (int i = 0; i < candidates.size(); i++) {
                if (shortestPathResultList.get(i)._1() != Double.POSITIVE_INFINITY) {
                    timeStep.addRoadPath(from, candidates.get(i), new RoadPath(from, candidates.get(i), shortestPathResultList.get(i)._2()));
                    double transitionLogProbability = hmmProbabilities.transitionLogProbability(
                            shortestPathResultList.get(i)._1(), linearDistance, timeDiff);
                    timeStep.addTransitionLogProbability(from, candidates.get(i), transitionLogProbability);
                }
            }
        }
    }

    /**
     * Extract the map-matching result (point-node pairs) from the
     * HMM algorithm result.
     *
     * @param roadPositionList The Viterbi algorithm result (best match).
     * @param probabilities    The probabilities of the matching results.
     * @return A list of Point-to-Node pairs with distance.
     */
    private TrajectoryMatchResult getResult(Trajectory traj, Map<Integer, List<SequenceState<PointMatch, STPoint, RoadPath>>>
            roadPositionList, double[] probabilities) {
        TrajectoryMatchResult result = new TrajectoryMatchResult(traj, rankLength);
        for (Map.Entry<Integer, List<SequenceState<PointMatch, STPoint, RoadPath>>> roadPosition : roadPositionList.entrySet()) {
            List<PointMatch> matchPairs = new ArrayList<>();
            Set<String> path = new LinkedHashSet<>();
            int rank = roadPosition.getKey();
            for (SequenceState<PointMatch, STPoint, RoadPath> sequence : roadPosition.getValue()) {
                PointMatch pointMatch = sequence.state;
                if (pointMatch != null) {
                    // make sure it returns a copy of the objects
                    matchPairs.add(pointMatch);
                } else {
                    throw new NullPointerException("ERROR! Matching result should not have NULL value");
                }
                if (sequence.transitionDescriptor != null) {
                    path.addAll(sequence.transitionDescriptor.passingRoadID);
                }
            }
            result.setMatchingResult(matchPairs, rank);
            result.setMatchWayList(new ArrayList<>(path), rank);
        }
        result.setProbabilities(probabilities);
        return result;
    }

    /**
     * The distance between the two given coordinates, using
     * the specified distance function.
     *
     * @param x1 longitude of the first point
     * @param y1 latitude of the first point
     * @param x2 longitude of the second point
     * @param y2 latitude of the second point
     * @return Distance from (x1,y1) to (x2,y2).
     */
    private double getDistance(double x1, double y1, double x2, double y2) {
        return distanceFunction.pointToPointDistance(x1, y1, x2, y2);
    }

    /**
     * Insert the partial matching result into the final result, the broken points along the way should be inserted as well
     *
     * @param rankedRoadPositionList final matching result list
     * @param temporalRoadPositions  current temporal matching result list
     * @param trajectory             raw trajectory
     * @param breakPoints            the break point list
     * @param destinationIndex       the size of the matching result after insertion, it should be temporal+breakPoints
     * @param candidatesMap          the candidate map of each raw trajectory point
     */
    private void resultMerge(Map<Integer, List<SequenceState<PointMatch, STPoint, RoadPath>>> rankedRoadPositionList,
                             List<List<SequenceState<PointMatch, STPoint, RoadPath>>> temporalRoadPositions, Trajectory trajectory,
                             Map<Integer, Boolean> breakPoints, int destinationIndex, Map<STPoint, Collection<PointMatch>> candidatesMap) {
        for (int j = 0; j < rankLength; j++) {
            if (j < temporalRoadPositions.size()) {    // the rank j result exists
                int cursor = 0;
                for (int k = rankedRoadPositionList.get(j).size(); k < destinationIndex; k++) {
                    // if the current point is a breaking point
                    if (breakPoints.containsKey(k)) {
                        if (breakPoints.get(k))  // if the point does not have candidate
                            rankedRoadPositionList.get(j).add(new SequenceState<>(new PointMatch(), trajectory.get(k), null));
                        else {
                            List<String> roadIdList = new ArrayList<>();
                            PointMatch closestMatch = findNearestMatch(trajectory.get(k), candidatesMap.get(trajectory.get(k)));
                            roadIdList.add(closestMatch.getRoadID());
                            rankedRoadPositionList.get(j).add(new SequenceState<>(closestMatch, trajectory.get(k), new
                                    RoadPath(null, null, roadIdList)));
                        }
                    } else {
                        // TODO bug reported, IndexOutOfBound for cursor
                        rankedRoadPositionList.get(j).add(temporalRoadPositions.get(j).get(cursor));
                        cursor++;
                    }
                }
            } else {
                for (int k = rankedRoadPositionList.get(j).size(); k < destinationIndex; k++) {
                    rankedRoadPositionList.get(j).add(new SequenceState<>(new PointMatch(), trajectory.get(k), null));
                }
            }
        }
    }

    List<Trajectory> getUnMatchedTraj() {
        return unmatchedTrajectoryList;
    }
}