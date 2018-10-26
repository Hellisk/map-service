package mapupdate.mapmatching.hmm;

import mapupdate.util.dijkstra.RoutingGraph;
import mapupdate.util.function.GreatCircleDistanceFunction;
import mapupdate.util.index.grid.Grid;
import mapupdate.util.index.grid.GridPartition;
import mapupdate.util.object.datastructure.*;
import mapupdate.util.object.roadnetwork.MapInterface;
import mapupdate.util.object.roadnetwork.RoadNetworkGraph;
import mapupdate.util.object.roadnetwork.RoadWay;
import mapupdate.util.object.spatialobject.Point;
import mapupdate.util.object.spatialobject.STPoint;
import mapupdate.util.object.spatialobject.Segment;
import mapupdate.util.object.spatialobject.Trajectory;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.stream.Stream;

import static mapupdate.Main.LOGGER;
import static mapupdate.Main.MAX_TIME_INTERVAL;

/**
 * Created by uqpchao on 22/05/2017.
 */
public class NewsonHMM2009 implements MapInterface {
    private final int candidateRange;    // in meter
    private final int gapExtensionRange; // in meter
    private final int rankLength; // in meter

    /**
     * The distance method to use between points
     */
    private final GreatCircleDistanceFunction distanceFunction;

    /**
     * The probabilities of the HMM lattice
     */
    private final HMMProbabilities hmmProbabilities = new HMMProbabilities();

    /**
     * The grid index for candidate generation
     */
    private Grid<SegmentIndexItem> grid;

    /**
     * the threshold for extra indexing point, segments that exceed such threshold will generate extra indexing point(s)
     */
    private double intervalLength;

    /**
     * The graph for Dijkstra shortest distance calculation
     */
    private final RoutingGraph routingGraph;

    private List<Trajectory> unmatchedTraj = new ArrayList<>();

    public NewsonHMM2009(int candidateRange, int unmatchedTrajThreshold, int rankLength, RoadNetworkGraph roadNetworkGraph) {
        this.distanceFunction = new GreatCircleDistanceFunction();
        this.candidateRange = candidateRange;
        this.gapExtensionRange = unmatchedTrajThreshold;
        this.rankLength = rankLength;
        this.intervalLength = (4 * Math.sqrt(2) - 2) * candidateRange;   // given such length limit, none of the candidate segment can escape
        // the grid search
        buildGridIndex(roadNetworkGraph);   // build grid index
        this.routingGraph = new RoutingGraph(roadNetworkGraph);
    }

    public List<TrajectoryMatchingResult> trajectoryListMatchingProcess(List<Trajectory> rawTrajectory) {

        // sequential test
        List<TrajectoryMatchingResult> result = new ArrayList<>();
        int matchCount = 0;
        int brokenTrajCount = 0;
        for (Trajectory traj : rawTrajectory) {
//            if (matchCount == 2004)
//                LOGGER.info("test");
            Pair<TrajectoryMatchingResult, List<Trajectory>> matchResult = doMatching(traj);
            if (!matchResult._2().isEmpty())
                brokenTrajCount++;
            result.add(matchResult._1());
            if (rawTrajectory.size() > 100)
                if (matchCount % (rawTrajectory.size() / 100) == 0 && matchCount / (rawTrajectory.size() / 100) <= 100)
                    LOGGER.info("Map matching finish " + matchCount / (rawTrajectory.size() / 100) + "%.");
            matchCount++;
            this.unmatchedTraj.addAll(matchResult._2());
        }
        LOGGER.info("All map-matching finished. Total number of broken trajectory: " + brokenTrajCount);
        return result;
    }

    public Stream<Pair<TrajectoryMatchingResult, List<Trajectory>>> trajectoryStreamMatchingProcess(Stream<Trajectory> inputTrajectory)
            throws ExecutionException, InterruptedException {

        if (inputTrajectory == null) {
            throw new IllegalArgumentException("Trajectory stream for map-matching must not be null.");
        }
        if (this.grid == null || this.grid.isEmpty()) {
            throw new IllegalArgumentException("Grid index must not be empty nor null.");
        }

        // parallel processing
        ForkJoinPool forkJoinPool = ForkJoinPool.commonPool();
        ForkJoinTask<Stream<Pair<TrajectoryMatchingResult, List<Trajectory>>>> taskResult =
                forkJoinPool.submit(() -> inputTrajectory.parallel().map(this::doMatching));
        while (!taskResult.isDone())
            Thread.sleep(5);
        return taskResult.get();
    }

    /**
     * Matching entry for map-matching in Global dataset. Only provide matching for single thread.
     *
     * @param inputTrajectory Input trajectory in Global dataset
     * @return Map-matching result
     */
    public TrajectoryMatchingResult trajectorySingleMatchingProcess(Trajectory inputTrajectory) {

        // sequential test
        Pair<TrajectoryMatchingResult, List<Trajectory>> matchResult = doMatching(inputTrajectory);
//            if (inputTrajectory.size() > 100)
//                if (matchCount % (inputTrajectory.size() / 100) == 0)
//                    LOGGER.info("Map matching finish " + matchCount / (inputTrajectory.size() / 100) + "%. Broken trajectory count:" + hmmMapMatching.getBrokenTrajCount() + ".");
//            matchCount++;
        LOGGER.info("Matching finished:" + inputTrajectory.getId());
        return matchResult._1();
    }

    public List<Trajectory> getUnmatchedTraj() {
        return unmatchedTraj;
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
        double gridRadius = candidateRange >= 15 ? 2 * candidateRange : 2 * 15;
        columnNum = (int) Math.floor(lonDistance / gridRadius);
        rowNum = (int) Math.floor(latDistance / gridRadius);
        double lonPerCell = (inputMap.getMaxLon() - inputMap.getMinLon()) / columnNum;
        double latPerCell = (inputMap.getMaxLat() - inputMap.getMinLat()) / columnNum;

        // add extra grid cells around the margin to cover outside trajectory points
        this.grid = new Grid<>(columnNum + 2, rowNum + 2, inputMap.getMinLon() - lonPerCell, inputMap.getMinLat() - latPerCell, inputMap
                .getMaxLon() + lonPerCell, inputMap.getMaxLat() + latPerCell);

//        LOGGER.info("The grid contains " + rowNum + 2 + " rows and " + columnNum + 2 + " columns");

        int pointCount = 0;
        int intermediatePointCount = 0;

        for (RoadWay t : inputMap.getWays()) {
            for (Segment s : t.getEdges()) {
                // -1: left endpoint of the segment, 0: right endpoint of the segment, >0: intermediate point
                SegmentIndexItem segmentItemLeft = new SegmentIndexItem(s, -1, t.getID(), intervalLength);
                XYObject<SegmentIndexItem> segmentIndexLeft = new XYObject<>(segmentItemLeft.x(), segmentItemLeft.y(), segmentItemLeft);
                SegmentIndexItem segmentItemRight = new SegmentIndexItem(s, 0, t.getID(), intervalLength);
                XYObject<SegmentIndexItem> segmentIndexRight = new XYObject<>(segmentItemRight.x(), segmentItemRight.y(), segmentItemRight);
                this.grid.insert(segmentIndexLeft);
                pointCount++;
                this.grid.insert(segmentIndexRight);
                pointCount++;
                // if the length of the segment is longer than two times of the candidate range, insert the intermediate points of the
                // segment
                double segmentDistance = distanceFunction.distance(s.p1(), s.p2());
                int intermediateID = 1;
                while (segmentDistance > intervalLength) {
                    SegmentIndexItem segmentItemIntermediate = new SegmentIndexItem(s, intermediateID, t.getID(), intervalLength);
                    XYObject<SegmentIndexItem> segmentIndexIntermediate = new XYObject<>(segmentItemIntermediate.x(), segmentItemIntermediate.y(),
                            segmentItemIntermediate);
                    this.grid.insert(segmentIndexIntermediate);
                    segmentDistance = segmentDistance - intervalLength;
                    intermediateID++;
                    intermediatePointCount++;
                    pointCount++;
                }
            }
        }

        LOGGER.info("Grid index build successfully, total number of segment items in grid index: " + pointCount + ", number of " +
                "newly created middle points: " + intermediatePointCount);
    }

    /*
     *   Map-matching implementation
     */

    public Pair<TrajectoryMatchingResult, List<Trajectory>> doMatching(final Trajectory trajectory) {
        // Compute the candidate road segment list for every GPS point through grid index
//        long startTime = System.currentTimeMillis();
        int indexBeforeCurrBreak = -1;   // the index of the last point before current broken position, -1 = currently no breakpoint
        final Map<STPoint, Collection<PointMatch>> candidatesMap = new HashMap<>(); //Map each point to a list of candidate nodes
        computeCandidatesFromIndex(trajectory, candidatesMap, grid);
//        computeCandidates(trajectory);
//        LOGGER.info("Time cost on candidate generation is: " + (System.currentTimeMillis() - startTime));
        boolean isBrokenTraj = false;
//        Set<Integer> currBreakIndex = new LinkedHashSet<>();    // points that will be put into the unmatched trajectory
        Map<Integer, Integer> breakPoints = new LinkedHashMap<>();  // the points that break the connectivity and the reason, =1 if
        // the break is caused by no candidate, =2 if it is caused by broken transition, =3 if it is caused by broken transition but used
        // as the initial point of the new sequence

        List<Trajectory> unmatchedTrajectoryList = new ArrayList<>();   // unmatched trajectories

        ViterbiAlgorithm<PointMatch, STPoint, RoadPath> viterbi = new ViterbiAlgorithm<>(rankLength);
        TimeStep<PointMatch, STPoint, RoadPath> prevTimeStep = null;
        List<Pair<List<SequenceState<PointMatch, STPoint, RoadPath>>, Double>> rankedRoadPositionList = new ArrayList<>(rankLength);
        for (int i = 0; i < rankLength; i++) {       // first fill all top k results with empty array
            rankedRoadPositionList.add(new Pair<>(new ArrayList<>(), 0d));
        }

        // start the process
        for (int i = 0; i < trajectory.size(); i++) {
            Collection<PointMatch> candidates;
            TimeStep<PointMatch, STPoint, RoadPath> timeStep;
            STPoint gpsPoint = trajectory.get(i);
            if (candidatesMap.get(gpsPoint).size() == 0) {  // no candidate for the current point, definitely a break point
                isBrokenTraj = true;
                breakPoints.put(i, 1);
            } else {
                candidates = candidatesMap.get(gpsPoint);
                timeStep = new TimeStep<>(gpsPoint, candidates);
                if (prevTimeStep == null) {     // start of the trajectory or the current matching has just been cut off
                    computeEmissionProbabilities(timeStep);
                    viterbi.startWithInitialObservation(timeStep.observation, timeStep.candidates,
                            timeStep.emissionLogProbabilities);
                    if (breakPoints.containsKey(i))
                        breakPoints.put(i, 3);  // start the new match from the current point, set it as the breakpoint type 3
                    // successful initialization
                    prevTimeStep = timeStep;
                } else {   // continue the matching process
                    final double timeDiff = (timeStep.observation.time() - prevTimeStep.observation.time());
                    if (timeDiff > 180) {   // huge time gap, split the trajectory matching result
                        if (indexBeforeCurrBreak != -1) {
                            // we finish the matching before last break point and start a new matching sequence from break point to the
                            // current gap, the previous matching sequence is finished here
                            List<Pair<List<SequenceState<PointMatch, STPoint, RoadPath>>, Double>> temporalRoadPositions =
                                    viterbi.computeMostLikelySequence();
                            resultMerge(rankedRoadPositionList, temporalRoadPositions, trajectory, breakPoints, indexBeforeCurrBreak + 1,
                                    candidatesMap);

                            // restart the matching from the last break point
                            i = indexBeforeCurrBreak;
                            indexBeforeCurrBreak = -1;
                            prevTimeStep = null;
                            continue;
                        }
                        List<Pair<List<SequenceState<PointMatch, STPoint, RoadPath>>, Double>> temporalRoadPositions =
                                viterbi.computeMostLikelySequence();
                        resultMerge(rankedRoadPositionList, temporalRoadPositions, trajectory, breakPoints, i, candidatesMap);

                        // set the current point as break point and restart the matching
                        i--;
                        prevTimeStep = null;
                        continue;
                    }

                    //  no time gap, continue the matching process
                    computeEmissionProbabilities(timeStep);
                    computeTransitionProbabilitiesWithConnectivity(prevTimeStep, timeStep);
                    viterbi.nextStep(
                            timeStep.observation,
                            timeStep.candidates, prevTimeStep.candidates,
                            timeStep.emissionLogProbabilities,
                            timeStep.transitionLogProbabilities,
                            timeStep.roadPaths);

                    if (viterbi.isBroken()) {
                        // the match stops due to no connection, add the current point and its predecessor to the broken list
                        isBrokenTraj = true;
                        if (indexBeforeCurrBreak == -1) {
                            indexBeforeCurrBreak = i - 1;
                            while (breakPoints.containsKey(indexBeforeCurrBreak) && breakPoints.get(indexBeforeCurrBreak) != 3) {
                                indexBeforeCurrBreak--;
                            }
                            if (indexBeforeCurrBreak < rankedRoadPositionList.get(0)._1().size())
                                throw new IndexOutOfBoundsException("ERROR! The current breakpoint index falls into the matched result area");
                        }
                        // mark the broken points and expect a reconnection
                        breakPoints.put(i, 2);
                        viterbi.setToUnbroken();
                    } else {
                        // the match continues
                        if (indexBeforeCurrBreak != -1) {
                            // remove the break flag and continue
                            indexBeforeCurrBreak = -1;
                        }
                        breakPoints.remove(i);
                        prevTimeStep = timeStep;
                    }
                }
            }
            if (i == trajectory.size() - 1) {  // the last point
                // complete the final part of the matching sequence
                if (indexBeforeCurrBreak != -1) {
                    // we finish the matching before last break point and start a new matching sequence from break point to the
                    // current gap and restart the match from the break point
                    List<Pair<List<SequenceState<PointMatch, STPoint, RoadPath>>, Double>> temporalRoadPositions = viterbi
                            .computeMostLikelySequence();
                    resultMerge(rankedRoadPositionList, temporalRoadPositions, trajectory, breakPoints, indexBeforeCurrBreak + 1,
                            candidatesMap);

                    // restart the matching from the last break point
                    i = indexBeforeCurrBreak;
                    indexBeforeCurrBreak = -1;
                    prevTimeStep = null;
                } else {
                    List<Pair<List<SequenceState<PointMatch, STPoint, RoadPath>>, Double>> temporalRoadPositions = viterbi
                            .computeMostLikelySequence();
                    resultMerge(rankedRoadPositionList, temporalRoadPositions, trajectory, breakPoints, trajectory.size(), candidatesMap);
                }
            }
        }

        // Check whether the HMM occurred in the last time step
        if (viterbi.isBroken()) {
            throw new RuntimeException("ERROR! The hmm break still exists after the trajectory is processed.");
        }

//        for (Pair<List<SequenceState<PointMatch, STPoint, RoadPath>>, Double> positionList : rankedRoadPositionList) {   // sort the matching result according to the trajectory point sequence
//            positionList._1().sort(Comparator.comparingLong(m -> m.observation.time()));
//        }
//        LOGGER.info("Time cost on matching is: " + (System.currentTimeMillis() - startTime));

        if (isBrokenTraj) {
            // generate unmatched trajectories
            List<Integer> breakPointList = new ArrayList<>(breakPoints.keySet());
//            List<Integer> breakPointList = new ArrayList<>(breakPoints);
            breakPointList.sort(Comparator.comparingInt(m -> m));
            Set<Integer> extendedBreakPoints = simpleBreakPointExtension(breakPointList, trajectory, candidatesMap);
            if (!extendedBreakPoints.isEmpty()) {
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
//            } else {
//                LOGGER.info("The break point(s) cannot be extended and thus removed. No unmatched trajectory output");
            }
        }
        return new Pair<>(getResult(trajectory, rankedRoadPositionList), unmatchedTrajectoryList);
    }

    /**
     * Extend the breakpoints to sub trajectories that are probably unmatchable.
     *
     * @param breakPointList The breakpoint list
     * @param trajectory     The raw trajectory
     * @param candidatesMap  The candidate matches mapping
     * @return List of trajectory point index representing sub trajectories
     */
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

//    /**
//     * Extend the breakpoints to sub trajectories that are probably unmatchable.
//     *
//     * @param breakPointList The breakpoint list
//     * @param trajectory     The raw trajectory
//     * @param candidatesMap  The candidate matches mapping
//     * @return List of trajectory point index representing sub trajectories
//     */
//    private Set<Integer> advancedBreakPointExtension(List<Integer> breakPointList, Trajectory trajectory, Map<STPoint,
//            Collection<PointMatch>> candidatesMap) {
//        Set<Integer> extendedBreakPoints = new LinkedHashSet<>();
//        int lastUnmatchedPoint = 0;
//        for (int i : breakPointList) {
//            boolean hasNeighbour = false;
//            for (int p = 1; i - p > lastUnmatchedPoint; p++) {
//                if (findMinDist(trajectory.get(i - p), candidatesMap.get(trajectory.get(i - p))) > gapExtensionRange) {
//                    extendedBreakPoints.add(i - p);
//                } else {
//                    if (p != 1) {
//                        hasNeighbour = true;
//                    }
//                    break;
//                }
//            }
//            for (int p = 1; i + p < trajectory.size(); p++) {
//                if (findMinDist(trajectory.get(i + p), candidatesMap.get(trajectory.get(i + p))) > gapExtensionRange) {
//                    extendedBreakPoints.add(i + p);
//                    hasNeighbour = true;
//                } else {
//                    if (p == 1 && !hasNeighbour)
//                        break;
//                    extendedBreakPoints.add(i);
//                    lastUnmatchedPoint = i + p - 1;
//                    hasNeighbour = false;
//                    break;
//                }
//            }
//            if (hasNeighbour) { // if no successive point is extendable
//                extendedBreakPoints.add(i);
//                lastUnmatchedPoint = i;
//            } else {
//                for (int j = -2; j < 3; j++) {
//                    int currIndex = i + j;
//                    if (lastUnmatchedPoint <= currIndex && trajectory.size() > currIndex)
//                        extendedBreakPoints.add(currIndex);
//                }
//            }
//        }
//        return extendedBreakPoints;
//    }

    /**
     * Find the closest match candidate given the trajectory point
     *
     * @param trajPoint        Trajectory point
     * @param trajPointMatches Candidate matches
     * @return The closest match candidate
     */
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

    /**
     * Find the minimum distance between given trajectory point and all its candidate matches
     *
     * @param stPoint      Trajectory point
     * @param pointMatches Candidate matches
     * @return The distance between the trajectory point and its closest candidate
     */
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
     * @param pointsList    List of GPS trajectory points to map.
     * @param candidatesMap the candidate list for every trajectory point
     */
    private void computeCandidatesFromIndex(Collection<STPoint> pointsList, Map<STPoint, Collection<PointMatch>> candidatesMap,
                                            Grid<SegmentIndexItem> grid) {
//        int candidateCount = 0;
        for (STPoint p : pointsList) {
            Set<String> candidateFilter = new HashSet<>();
            // As we set the grid size as the candidateRange, only the partition that contains the query point and its neighbouring
            // partitions can potentially generate candidates
            candidatesMap.put(p, new ArrayList<>());
            List<GridPartition<SegmentIndexItem>> partitionList = new ArrayList<>();
            partitionList.add(grid.partitionSearch(p.x(), p.y()));
            partitionList.addAll(grid.adjacentPartitionSearch(p.x(), p.y()));
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
//                                candidateCount++;
                                candidateFilter.add(indexItem.getSegmentElement().x1() + "," + indexItem.getSegmentElement().y1() + "_" +
                                        indexItem.getSegmentElement().x2() + "," + indexItem.getSegmentElement().y2() + "_" + indexItem.getRoadID());
                            }
                        }
                    }
            }
        }
//        LOGGER.info("Total candidate count: " + candidateCount + ", trajectory point count: " + pointsList.size());
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
     * Compute the transition probabilities between every state, taking the connectivity of the road nodes into account.
     *
     * @param prevTimeStep the time step of the last trajectory point
     * @param timeStep     the current time step
     */
    private void computeTransitionProbabilitiesWithConnectivity(TimeStep<PointMatch, STPoint, RoadPath>
                                                                        prevTimeStep, TimeStep<PointMatch, STPoint, RoadPath> timeStep) {
        final double linearDistance = getDistance(
                prevTimeStep.observation.x(), prevTimeStep.observation.y(),
                timeStep.observation.x(), timeStep.observation.y());
        final double timeDiff = (timeStep.observation.time() - prevTimeStep.observation.time());
        double maxDistance = (50 * timeDiff) < linearDistance * 8 ? 50 * timeDiff : linearDistance * 8; // limit the maximum speed to
        // 180km/h
//        double maxDistance = 50 * timeDiff;
        for (PointMatch from : prevTimeStep.candidates) {
            List<PointMatch> candidates = new ArrayList<>(timeStep.candidates);
            List<Pair<Double, List<String>>> shortestPathResultList = routingGraph.calculateShortestDistanceList(from, candidates, maxDistance);
            for (int i = 0; i < candidates.size(); i++) {
                if (shortestPathResultList.get(i)._1() != Double.POSITIVE_INFINITY) {
                    timeStep.addRoadPath(from, candidates.get(i), new RoadPath(from, candidates.get(i), shortestPathResultList.get(i)._2()));
                    double transitionLogProbability = hmmProbabilities.transitionLogProbability(shortestPathResultList.get(i)._1(),
                            linearDistance, timeDiff);
//                    // apply the penalty if the path incurs an u-turn
//                    if (!from.getRoadID().equals(candidates.get(i).getRoadID()) && Math.abs(Long.parseLong(from.getRoadID())) == Math.abs
//                            (Long.parseLong(candidates.get(i).getRoadID())))
//                        transitionLogProbability += U_TURN_PENALTY;
                    timeStep.addTransitionLogProbability(from, candidates.get(i), transitionLogProbability);
                }
            }
        }
    }

    /**
     * Extract the map-matching result (point-node pairs) from the HMM algorithm result.
     *
     * @param roadPositionList The Viterbi algorithm result (best match).
     * @return A list of Point-to-Vertex pairs with distance.
     */
    private TrajectoryMatchingResult getResult(Trajectory traj, List<Pair<List<SequenceState<PointMatch, STPoint, RoadPath>>, Double>>
            roadPositionList) {
        TrajectoryMatchingResult result = new TrajectoryMatchingResult(traj, rankLength);
        double[] probabilities = new double[rankLength];
        for (int i = 0; i < roadPositionList.size(); i++) {
            Pair<List<SequenceState<PointMatch, STPoint, RoadPath>>, Double> roadPosition = roadPositionList.get(i);
            List<PointMatch> matchPairs = new ArrayList<>();
            Set<String> path = new LinkedHashSet<>();
            for (SequenceState<PointMatch, STPoint, RoadPath> sequence : roadPosition._1()) {
                PointMatch pointMatch = sequence.state;
                if (pointMatch != null) {
                    // make sure it returns a copy of the objects
                    matchPairs.add(pointMatch);
                } else {
                    throw new NullPointerException("ERROR! Matching result should not have NULL value");
                }
                if (sequence.transitionDescriptor != null) {
                    if (sequence.transitionDescriptor.from != null)
                        path.add(sequence.transitionDescriptor.from.getRoadID());
                    path.addAll(sequence.transitionDescriptor.passingRoadID);
                    if (sequence.transitionDescriptor.to != null)
                        path.add(sequence.transitionDescriptor.to.getRoadID());
                }
            }
            result.setMatchingResult(matchPairs, i);
            result.setMatchWayList(new ArrayList<>(path), i);
            // the probability should be converted to non-log mode
//            probabilities[i] = roadPosition._2() == 0 ? 0 : Math.exp(roadPosition._2());
            // probability normalization
            probabilities[i] = roadPosition._2() == 0 ? 0 : Math.exp(roadPosition._2() / traj.size());
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
     * Insert the partial matching result into the final result, the broken points along the way should be inserted as well. The broken
     * points are matched to its geographically closest point or null if no point is close to it. In addition, the probability is
     * accumulated, but it will turn to zero if either the previous or the current probability is zero.
     *
     * @param rankedRoadPositionList final matching result list
     * @param temporalRoadPositions  current temporal matching result list
     * @param trajectory             raw trajectory
     * @param breakPoints            the break point list
     * @param destinationIndex       the size of the matching result after insertion, it should be temporal+breakPoints
     * @param candidatesMap          the candidate map of each raw trajectory point
     */
    private void resultMerge(List<Pair<List<SequenceState<PointMatch, STPoint, RoadPath>>, Double>> rankedRoadPositionList,
                             List<Pair<List<SequenceState<PointMatch, STPoint, RoadPath>>, Double>> temporalRoadPositions, Trajectory
                                     trajectory, Map<Integer, Integer> breakPoints, int destinationIndex, Map<STPoint,
            Collection<PointMatch>> candidatesMap) {
        for (int rank = 0; rank < rankLength; rank++) {
            int validRank = rank < temporalRoadPositions.size() ? rank : temporalRoadPositions.size() - 1;  // fill the rest of the rank
            // list with the last valid sequence
            int startPosition = rankedRoadPositionList.get(rank)._1().size();
            int cursor = 0;
            List<SequenceState<PointMatch, STPoint, RoadPath>> roadPositionList = rankedRoadPositionList.get(rank)._1();
            double unmatchedProbability = 0;  // for each unmatched trajectory point, we add an emission probability and the
            // transition probabilities
            for (int k = startPosition; k < destinationIndex; k++) {
                // if the current point is a breaking point
                if (breakPoints.containsKey(k) && breakPoints.get(k) != 3) {
                    if (breakPoints.get(k) == 1) { // if the point does not have candidate
                        roadPositionList.add(new SequenceState<>(new PointMatch(), trajectory.get(k), null));
                        unmatchedProbability += hmmProbabilities.emissionLogProbability(candidateRange);
                        if (k != 0) {
                            unmatchedProbability += hmmProbabilities.maxTransitionLogProbability(distanceFunction.distance(trajectory.get(k - 1),
                                    trajectory.get(k)), trajectory.get(k).time() - trajectory.get(k - 1).time());
                        }
                    } else {
                        List<String> roadIdList = new ArrayList<>();
                        PointMatch closestMatch = findNearestMatch(trajectory.get(k), candidatesMap.get(trajectory.get(k)));
                        roadIdList.add(closestMatch.getRoadID());
                        roadPositionList.add(new SequenceState<>(closestMatch, trajectory.get(k), new RoadPath(null, null,
                                roadIdList)));
//                            double distance = distanceFunction.distance(closestMatch.getMatchPoint(), trajectory.get(k));
                        unmatchedProbability += hmmProbabilities.emissionLogProbability(candidateRange);
                        if (k != 0) {
                            unmatchedProbability += hmmProbabilities.maxTransitionLogProbability(distanceFunction.distance(trajectory.get(k - 1),
                                    trajectory.get(k)), trajectory.get(k).time() - trajectory.get(k - 1).time());
                        }
                    }
                } else {
                    if (breakPoints.containsKey(k) && breakPoints.get(k) == 3 && cursor != 0)
                        LOGGER.severe("ERROR! The current sequence contains a type 3 break point");
                    if (temporalRoadPositions.get(validRank)._1().get(cursor).observation.equals2D(trajectory.get(k))) {
                        roadPositionList.add(temporalRoadPositions.get(validRank)._1().get(cursor));
                        cursor++;
                    } else
                        LOGGER.severe("ERROR! The matching result mismatch!"); // the result sequence doesn't match the raw
                    // trajectory sequence
                }
            }
            double prevProbability = rankedRoadPositionList.get(rank)._2();
            double currProbability = temporalRoadPositions.get(validRank)._2();
            // add probability, probability = 0 if either of the probability is 0
            if (prevProbability == Double.NEGATIVE_INFINITY || currProbability == Double.NEGATIVE_INFINITY)
                rankedRoadPositionList.get(rank).set_2(Double.NEGATIVE_INFINITY);
            else if (prevProbability == 0)  // empty probability list
                rankedRoadPositionList.get(rank).set_2(currProbability + unmatchedProbability);
            else {
                if (startPosition == 0)
                    LOGGER.severe("ERROR! Non-zero probability for an empty sequence.");
                unmatchedProbability += hmmProbabilities.maxTransitionLogProbability(distanceFunction.distance(trajectory.get(startPosition - 1),
                        trajectory.get(startPosition)), trajectory.get(startPosition).time() - trajectory.get(startPosition - 1).time());
                rankedRoadPositionList.get(rank).set_2(prevProbability + currProbability + unmatchedProbability);
            }
//
//                for (int k = rankedRoadPositionList.get(rank)._1().size(); k < destinationIndex; k++) {
//                    rankedRoadPositionList.get(rank)._1().add(new SequenceState<>(new PointMatch(), trajectory.get(k), null));
//                }
//                rankedRoadPositionList.get(rank).set_2(Double.NEGATIVE_INFINITY);
        }
    }
}