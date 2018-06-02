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
 * @link https://github.com/bmwcarit/hmm-oldversion.
 */
@SuppressWarnings("serial")
public class HMMMapMatching {

    /**
     * The distance method to use between points
     */
    private final GreatCircleDistanceFunction distanceFunction;

    /**
     * Map each point to a list of candidate nodes
     */
    private final Map<STPoint, Collection<PointMatch>> candidatesMap = new HashMap<>();

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
     * unmatched trajectory segment list
     */
    private List<Trajectory> unmatchedTrajectoryList = new ArrayList<>();

    /**
     * number of broken trajectories
     */
    private int brokenTrajCount = 0;

    /**
     * the index of the last point before current broken position, -1: complete matching sequence
     */
    private int currBreakPointIndex = -1;

    /**
     * the last position that preserved in the current matching result
     */
    private int lastReservedPointIndex = -1;

    /**
     * the points before this index has been well processed
     */
    private int newMatchStartPointIndex = 0;

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
        computeCandidatesFromIndex(trajectory);
//        computeCandidates(trajectory);
//        System.out.println("Time cost on candidate generation is: " + (System.currentTimeMillis() - startTime));
        boolean brokenTraj = false;
        Set<Integer> currBreakIndex = new LinkedHashSet<>();    // points that will be put into the unmatched trajectory
        List<Integer> breakPoints = new ArrayList<>();  // points that break the connectivity

        ViterbiAlgorithm<PointMatch, STPoint, RoadPath> viterbi = new ViterbiAlgorithm<>();
        TimeStep<PointMatch, STPoint, RoadPath> prevTimeStep = null;
        List<SequenceState<PointMatch, STPoint, RoadPath>> roadPositions = new ArrayList<>();
        // store all states during the process, prepare for the breaks
//        Map<Integer, TimeStep<PointMatch, STPoint, RoadPath>> timeStepList = new HashMap<>();
        Map<Integer, Map<PointMatch, Double>> messageList = new HashMap<>();
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
            if (candidatesMap.get(gpsPoint).size() == 0) {  // no candidate for the current point
                timeStep = new TimeStep<>(gpsPoint, new ArrayList<>());
            } else {
                candidates = candidatesMap.get(gpsPoint);
                timeStep = new TimeStep<>(gpsPoint, candidates);
            }
            if (prevTimeStep == null) {     // start of the trajectory or the current matching has just been cut off
                if (candidatesMap.get(gpsPoint).size() != 0) {
                    computeEmissionProbabilities(timeStep);
                    viterbi.startWithInitialObservation(timeStep.observation, timeStep.candidates,
                            timeStep.emissionLogProbabilities);
                    // successful initialization
                    newMatchStartPointIndex = i;
                    messageList.put(i, viterbi.getMessage());
                    prevTimeStep = timeStep;
                } else {
                    if (currBreakPointIndex != -1) {
                        System.out.println("ERROR! The first point has no candidate");
                        breakPoints.add(i); // directly add to the break point list
//                        type = 1;
                    } else {
                        // already broken, add a new point
                        currBreakIndex.add(i);
//                        type = 1;
                    }
//                        // break points happen in the middle of the trajectory
//                        // store the matching result of all points before broken point
//                        List<SequenceState<PointMatch, STPoint, RoadPath>> temporalRoadPositions =
//                                viterbi.computeMostLikelySequence();
//                        roadPositions.addAll(temporalRoadPositions);
////                    for (int j = i - missingCandidateCount; j < i; j++) {
////                        roadPositions.add(new SequenceState<>(null, trajectory.get(i), null));
////                    }
//                        breakPoints.addAll(currBreakIndex);
//                        currBreakIndex.clear();
//                        currBreakPointIndex = -1;
//                        lastReservedPointIndex = i - 1;
//                    // both the previous point and current point is unmatchable, insert the current point to the broken list

                }
//                    currBreakPointIndex = i;
//                    currBreakIndex.add(i);
//                    roadPositions.add(new SequenceState<>(null, trajectory.get(i), null));
//                    timeStepList.add(new TimeStep<>(trajectory.get(i), null));
//                    messageList.add(new LinkedHashMap<>());
//                    candidateList.add(null);
//                    extendedStates.add();
//                    continue;
            } else if (candidatesMap.get(gpsPoint).size() != 0) {   // continue the matching process
                final double timeDiff = (timeStep.observation.time() - prevTimeStep.observation.time());
                if (timeDiff > 180) {   // time different factor
                    if (currBreakPointIndex != -1) {
                        // find the timestamp of the last unbroken point, call computeMostLikelySequence
                        viterbi.setMessage(messageList.get(currBreakPointIndex));
                        i = currBreakPointIndex + 1;
                        // we finish the matching before break point and start a new line from break point
                        currBreakPointIndex = -1;
                        lastReservedPointIndex = -1;
                    }
                    List<SequenceState<PointMatch, STPoint, RoadPath>> temporalRoadPositions =
                            viterbi.computeMostLikelySequence();
                    roadPositions.addAll(temporalRoadPositions);
                    viterbi.setMessage(null);
                    viterbi.setLastExtendedStates(null);
                    prevTimeStep = null;
                    continue;
                }

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
                    brokenTraj = true;
                    if (currBreakPointIndex == -1) {
                        currBreakPointIndex = i - 1;
                        lastReservedPointIndex = i - 2;
                        currBreakIndex.add(i);
                        currBreakIndex.add(i - 1);
                        breakPoints.add(currBreakPointIndex);
                        if (lastReservedPointIndex < newMatchStartPointIndex) {
                            // new match breaks at the second position, start from the third point
                            viterbi.setMessage(null);
                            viterbi.setLastExtendedStates(null);
                            prevTimeStep = null;
                            currBreakPointIndex = -1;
                            lastReservedPointIndex = -1;
                            continue;
                        }
                    } else {
                        currBreakIndex.add(i);
                        currBreakIndex.add(lastReservedPointIndex);
                        lastReservedPointIndex--;
                        if (lastReservedPointIndex < newMatchStartPointIndex) {
                            // the breaking segment extends to the start of the match
                            viterbi.setMessage(messageList.get(currBreakPointIndex));
                            List<SequenceState<PointMatch, STPoint, RoadPath>> temporalRoadPositions =
                                    viterbi.computeMostLikelySequence();
                            roadPositions.addAll(temporalRoadPositions);
                            viterbi.setMessage(null);
                            viterbi.setLastExtendedStates(null);
                            prevTimeStep = null;
                            // we finish the matching before break point and start a new line from break point
                            i = currBreakPointIndex + 1;
                            currBreakPointIndex = -1;
                            lastReservedPointIndex = -1;
                            continue;
                        }
                    }
                    // remove the broken points and expect a reconnection
                    viterbi.setMessage(messageList.get(lastReservedPointIndex));
                    viterbi.setPrevCandidates(candidatesMap.get(trajectory.get(lastReservedPointIndex)));
                    viterbi.setBroken(false);
//                    type = 2;
                } else {
                    // the match continues
                    if (currBreakPointIndex != -1) {
                        // continuous after removing points, deal with the removed points, remove the break flag and continue
                        currBreakPointIndex = -1;
                        lastReservedPointIndex = -1;
                    }

//                        currBreakIndex.sort(Comparator.comparingInt(m -> m));
//                        for (int j = currBreakIndex.get(0); j < i; j++) {
//                            STPoint currPoint = trajectory.get(j);
//                            SequenceState<PointMatch, STPoint, RoadPath> currMatch;
//                            if (candidatesMap.get(currPoint).size() != 0) {
//                                List<PointMatch> currPointCandidates = new ArrayList<>(candidatesMap.get(currPoint));
//                                double minDistance = this.distanceFunction.pointToPointDistance(currPoint.x(), currPoint.y(), currPointCandidates.get(0).lon(), currPointCandidates.get(0).lat());
//                                PointMatch choseMatch = currPointCandidates.get(0);
//                                for (PointMatch m : currPointCandidates) {
//                                    double currDistance = this.distanceFunction.pointToPointDistance(currPoint.x(), currPoint.y(), m.lon(), m.lat());
//
//                                    if (minDistance > currDistance) {
//                                        minDistance = currDistance;
//                                        choseMatch = m;
//                                    }
//                                }
//                                currMatch = new SequenceState<>(choseMatch, currPoint, null);
//                            } else {
//                                currMatch = new SequenceState<>(null, currPoint, null);
//                            }
//                            roadPositions.add(currMatch);
//                            if (j > currBreakPointIndex) {
//                                timeStepList.add(null);
//                                messageList.add(null);
//                                candidateList.add(null);
//                                extendedStates.add(null);
//                            }
//                        }
//                        breakPoints.add(currBreakPointIndex);
//                        currBreakIndex.clear();
//                        currBreakPointIndex = -1;
//                        lastReservedPointIndex = i - 1;
//                    }
                    // successful iteration
                    prevTimeStep = timeStep;
                    messageList.put(i, viterbi.getMessage());
//                    type = 3;
                }
            } else {
                if (currBreakPointIndex != -1) {
                    breakPoints.add(i);
                    currBreakIndex.add(i);
                    messageList.put(i, messageList.get(i - 1));
                } else {
                    currBreakPointIndex = i - 1;
                    lastReservedPointIndex = i - 1;
                    messageList.put(i, messageList.get(i - 1));
                }
//                type = 4;
            }
        }

        // complete the final part of the matching sequence
        if (currBreakPointIndex != -1) {
            viterbi.setMessage(messageList.get(currBreakPointIndex));
            // we finish the matching before break point and waive the rest of the sequence
            currBreakPointIndex = -1;
            lastReservedPointIndex = -1;
        }
        List<SequenceState<PointMatch, STPoint, RoadPath>> lastRoadPositions =
                viterbi.computeMostLikelySequence();
        roadPositions.addAll(lastRoadPositions);
        // Check whether the HMM occurred in the last time step
        if (viterbi.isBroken()) {
            System.out.println("ERROR! Unnable to compute HMM Map-Matching, Viterbi computation "
                    + "is broken (the probability of all states equals zero).");
        }

        roadPositions.sort(Comparator.comparingLong(m -> m.observation.time()));
//        System.out.println("Time cost on matching is: " + (System.currentTimeMillis() - startTime));

        if (brokenTraj) {
            brokenTrajCount += 1;
            // generate unmatched trajectories
            List<Integer> breakPointList = new ArrayList<>(currBreakIndex);
//            List<Integer> breakPointList = new ArrayList<>(breakPoints);
            breakPointList.sort(Comparator.comparingInt(m -> m));
            Set<Integer> extendedBreakPoints = new LinkedHashSet<>();
            int lastUnmatchedPoint = 0;
            for (int i : breakPointList) {
                if (i > 0) {
                    extendedBreakPoints.add(i - 1);
                }
                extendedBreakPoints.add(i);
                extendedBreakPoints.add(i + 1);

                for (int p = 2; i - p > lastUnmatchedPoint; p++) {
                    if (findMinimumDist(trajectory.get(i - p), candidatesMap.get(trajectory.get(i - p))) > gapExtensionRange) {
                        extendedBreakPoints.add(i - p);
                    } else {
                        break;
                    }
                }
                for (int p = 2; i + p < trajectory.size(); p++) {
                    if (findMinimumDist(trajectory.get(i + p), candidatesMap.get(trajectory.get(i + p))) > gapExtensionRange) {
                        extendedBreakPoints.add(i + p);
                    } else {
                        lastUnmatchedPoint = i + p - 1;
                        break;
                    }
                }
            }

            List<Integer> extendedBreakPointList = new ArrayList<>(extendedBreakPoints);
            extendedBreakPointList.sort(Comparator.comparingInt(m -> m));
            int start = extendedBreakPointList.get(0);
            int end;
            for (int i = 1; i < extendedBreakPointList.size(); i++) {
                if (extendedBreakPointList.get(i) != extendedBreakPointList.get(i - 1) + 1) {
                    end = extendedBreakPointList.get(i - 1);
                    unmatchedTrajectoryList.add(trajectory.subTrajectory(start, end));
                    start = extendedBreakPointList.get(i);
                }
            }
            unmatchedTrajectoryList.add(trajectory.subTrajectory(start, extendedBreakPointList.get(extendedBreakPointList.size() - 1)));
        } else
            System.out.print("Complete match result with no break. ");
        candidatesMap.clear();
        return getResult(trajectory, roadPositions);
    }

    private double findMinimumDist(STPoint stPoint, Collection<PointMatch> pointMatches) {
        double minDistance = Double.MAX_VALUE;
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
    private void computeCandidates(Collection<STPoint> pointsList) {
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
     * @param pointsList List of GPS trajectory points to map.
     */
    private void computeCandidatesFromIndex(Collection<STPoint> pointsList) {
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
                                this.candidatesMap.get(p).add(candidate);
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
                if (shortestPathResultList.get(i)._1() != Double.MAX_VALUE) {
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
     * @param roadPositions The Viterbi algorithm result (best match).
     * @return A list of Point-to-Node pairs with distance.
     */
    private TrajectoryMatchResult getResult(Trajectory traj, List<SequenceState<PointMatch, STPoint, RoadPath>> roadPositions) {
        List<PointMatch> matchPairs = new ArrayList<>();
        Set<String> path = new LinkedHashSet<>();
        double distance;
        for (SequenceState<PointMatch, STPoint, RoadPath> sequence : roadPositions) {
            STPoint point = sequence.observation;
            PointMatch pointMatch = sequence.state;
            if (pointMatch != null) {
                distance = getDistance(point.x(), point.y(), pointMatch.lon(), pointMatch.lat());
                // make sure it returns a copy of the objects
                matchPairs.add(pointMatch);
            } else {
                throw new NullPointerException("ERROR! Matching result should not have NULL value");
            }
            if (sequence.transitionDescriptor != null) {
                path.addAll(sequence.transitionDescriptor.passingRoadID);
            }
        }
        TrajectoryMatchResult result = new TrajectoryMatchResult(traj.getId(), rankLength);
        result.setMatchingResult(matchPairs, 0);
        result.setMatchWayList(new ArrayList<>(path), 0);
        return result;
    }

    /**
     * The distance between the two given coordinates, using
     * the specified distance function.
     *
     * @param x1
     * @param y1
     * @param x2
     * @param y2
     * @return Distance from (x1,y1) to (x2,y2).
     */
    private double getDistance(double x1, double y1, double x2, double y2) {
        return distanceFunction.pointToPointDistance(x1, y1, x2, y2);
    }

    List<Trajectory> getUnMatchedTraj() {
        return unmatchedTrajectoryList;
    }

    public int getBrokenTrajCount() {
        return brokenTrajCount;
    }
}
