package edu.uq.dke.mapupdate.mapmatching.hmm;

import edu.uq.dke.mapupdate.datatype.MatchingPoint;
import edu.uq.dke.mapupdate.util.dijkstra.Graph;
import traminer.util.exceptions.MapMatchingException;
import traminer.util.map.matching.MapMatchingMethod;
import traminer.util.map.matching.PointNodePair;
import traminer.util.map.roadnetwork.RoadNetworkGraph;
import traminer.util.map.roadnetwork.RoadNode;
import traminer.util.map.roadnetwork.RoadWay;
import traminer.util.spatial.distance.GreatCircleDistanceFunction;
import traminer.util.spatial.objects.Point;
import traminer.util.spatial.objects.Segment;
import traminer.util.spatial.objects.st.STPoint;
import traminer.util.trajectory.Trajectory;

import java.util.*;

/**
 * Implementation of Hidden Markov Model map-matching algorithm,
 * using the hmm-oldversion.
 * <p>
 * Based on Newson, Paul, and John Krumm. "Hidden Markov map matching through noise and sparseness."
 * Proceedings of the 17th ACM SIGSPATIAL International Conference on Advances in Geographic
 * Information Systems. ACM, 2009.
 *
 * @author uqdalves
 * @link https://github.com/bmwcarit/hmm-oldversion.
 */
@SuppressWarnings("serial")
public class HMMMapMatching implements MapMatchingMethod {
    /**
     * The distance method to use between points
     */
    private final GreatCircleDistanceFunction distanceFunction;
    /**
     * Map each point to a list of candidate nodes
     */
    private final Map<STPoint, Collection<MatchingPoint>> candidatesMap = new HashMap<>();
    /**
     * The probabilities of the HMM lattice
     */
    private final HMMProbabilities hmmProbabilities = new HMMProbabilities();
    /**
     * The distance threshold for the candidate points
     */
    private final double candidatesThreshold;

    /**
     * unmatched trajectory threshold
     */
    private final double unmatchedTrajThreshold;
    /**
     * The graph for Dijkstra shortest distance calculation
     */
    private final Graph roadNetworkGraph;
    /**
     * unmatched trajectory segment list
     */
    private List<Trajectory> unmatchedTrajectoryList = new ArrayList<>();
    /**
     * number of broken trajectories
     */
    private int brokenTrajCount = 0;
    /**
     * the current broken point index
     */
    private int lastUnbrokenIndex = -1;
    /**
     * the last position that sent to unmatched trajectory
     */
    private int lastBrokenIndex = -1;

    /**
     * Creates a new HMM map-matching method with default
     * Euclidean distance function.
     *
     * @param candidatesThreshold The distance threshold for the candidate points.
     */
    public HMMMapMatching(double candidatesThreshold, double unmatchedTrajThreshold, RoadNetworkGraph roadNetworkGraph) {
        this.distanceFunction = new GreatCircleDistanceFunction();
        this.candidatesThreshold = candidatesThreshold;
        this.roadNetworkGraph = new Graph(roadNetworkGraph);
        this.unmatchedTrajThreshold = unmatchedTrajThreshold;
    }

    /**
     * Creates a new HMM map-matching method with the
     * given distance function.
     *
     * @param distFunc            The point-to-node distance function to use.
     * @param candidatesThreshold The distance threshold for the candidate points.
     */
    public HMMMapMatching(GreatCircleDistanceFunction distFunc, double candidatesThreshold, double unmatchedTrajThreshold, RoadNetworkGraph roadNetworkGraph) {
        this.distanceFunction = distFunc;
        this.candidatesThreshold = candidatesThreshold;
        this.roadNetworkGraph = new Graph(roadNetworkGraph);
        this.unmatchedTrajThreshold = unmatchedTrajThreshold;
    }

    @Override
    public List<PointNodePair> doMatching(Trajectory trajectory, RoadNetworkGraph roadNetworkGraph)
            throws MapMatchingException {
        // Compute the candidate road segment list for every GPS point
        computeCandidates(trajectory, roadNetworkGraph.getWays());
        boolean brokenTraj = false;
        /**
         * list of break point index
         */
        List<Integer> currBreakIndex = new ArrayList<>();
        /**
         * the actual break point
         */
        List<Integer> breakPoints = new ArrayList<>();

        ViterbiAlgorithm<MatchingPoint, STPoint, RoadPath> viterbi = new ViterbiAlgorithm<>();
        TimeStep<MatchingPoint, STPoint, RoadPath> prevTimeStep = null;
        List<SequenceState<MatchingPoint, STPoint, RoadPath>> roadPositions = new ArrayList<>();
        // store all states during the process, prepare for the breaks
        List<TimeStep<MatchingPoint, STPoint, RoadPath>> timeStepList = new ArrayList<>();
        List<Map<MatchingPoint, Double>> messageList = new ArrayList<>();
        List<Collection<MatchingPoint>> candidateList = new ArrayList<>();
        List<Map<MatchingPoint, ViterbiAlgorithm.ExtendedState<MatchingPoint, STPoint, RoadPath>>> extendedStates = new ArrayList<>();
        boolean[] hasCandidate = new boolean[trajectory.size()];
        for (int i = 0; i < hasCandidate.length; i++)
            hasCandidate[i] = true;
        int missingCandidateCount = 0;
        // build the lattice
        for (int i = 0; i < trajectory.size(); i++) {
            Collection<MatchingPoint> candidates;
            TimeStep<MatchingPoint, STPoint, RoadPath> timeStep;
            STPoint gpsPoint = trajectory.get(i);
            if (candidatesMap.get(gpsPoint).size() == 0) {
                hasCandidate[i] = false;
                timeStep = new TimeStep<>(gpsPoint, new ArrayList<>());
            } else {
                hasCandidate[i] = true;
                candidates = candidatesMap.get(gpsPoint);
                timeStep = new TimeStep<>(gpsPoint, candidates);
            }
            if (prevTimeStep == null) {
                if (lastUnbrokenIndex != -1) {
                    // break points happen in the middle of the trajectory
                    // store the matching result of all points before broken point
                    List<SequenceState<MatchingPoint, STPoint, RoadPath>> temporalRoadPositions =
                            viterbi.computeMostLikelySequence();
                    roadPositions.addAll(temporalRoadPositions);
//                    for (int j = i - missingCandidateCount; j < i; j++) {
//                        roadPositions.add(new SequenceState<>(null, trajectory.get(i), null));
//                    }
                    currBreakIndex.clear();
                    breakPoints.add(lastUnbrokenIndex);
                    lastUnbrokenIndex = -1;
                    lastBrokenIndex = i - 1;
                }
                if (hasCandidate[i]) {
                    computeEmissionProbabilities(timeStep);
                    viterbi.startWithInitialObservation(timeStep.observation, timeStep.candidates,
                            timeStep.emissionLogProbabilities);
                    // successful initialization
                    timeStepList.add(timeStep);
                    messageList.add(viterbi.getMessage());
                    candidateList.add(viterbi.getPrevCandidates());
                    extendedStates.add(viterbi.getLastExtendedStates());
                    prevTimeStep = timeStep;
                    continue;
                } else {
                    roadPositions.add(new SequenceState<>(null, trajectory.get(i), null));
                    timeStepList.add(null);
                    messageList.add(null);
                    candidateList.add(null);
                    extendedStates.add(null);
                    continue;
                }
            } else if (hasCandidate[i]) {
                final double timeDiff = (timeStep.observation.time() - prevTimeStep.observation.time());
                if (timeDiff > 180) {   // time different factor
                    List<SequenceState<MatchingPoint, STPoint, RoadPath>> temporalRoadPositions =
                            viterbi.computeMostLikelySequence();
                    roadPositions.addAll(temporalRoadPositions);
                    prevTimeStep = null;
                    if (lastUnbrokenIndex == -1) {
                        i--;
                        continue;
                    } else {
                        i = lastUnbrokenIndex;
                        continue;
                    }
                }
                computeEmissionProbabilities(timeStep);
                // TODO: If two points are exactly the same, no connection detected
                computeTransitionProbabilitiesWithConnectivity(prevTimeStep, timeStep);
                viterbi.nextStep(
                        timeStep.observation,
                        timeStep.candidates,
                        timeStep.emissionLogProbabilities,
                        timeStep.transitionLogProbabilities,
                        timeStep.roadPaths);
            }
            // the incorrect part is not end
            if (viterbi.isBroken() || !hasCandidate[i]) {
                brokenTraj = true;
                int prevPoint = -1;
                if (currBreakIndex.isEmpty()) {
                    // The break happens
                    currBreakIndex.add(i);
                    if (i != 0) {
                        currBreakIndex.add(i - 1);
                        this.lastUnbrokenIndex = i - 1;
                        prevPoint = i - 1;
                    } else {
                        System.out.println("ERROR: The first point crashes.");
                    }
                } else {
                    currBreakIndex.add(i);
                    prevPoint = this.lastUnbrokenIndex * 2 - i + 1;
                    if (prevPoint > this.lastBrokenIndex)
                        // There is still some point correctly matched
                        currBreakIndex.add(prevPoint);
                }
                if (prevPoint > this.lastBrokenIndex + 1) {
                    prevTimeStep = timeStepList.get(prevPoint - 1);
                    viterbi.setMessage(messageList.get(prevPoint - 1));
                    viterbi.setPrevCandidates(candidateList.get(prevPoint - 1));
                    viterbi.setLastExtendedStates(extendedStates.get(prevPoint - 1));
                } else {
                    prevTimeStep = null;
                    i = lastUnbrokenIndex;
                    viterbi.setMessage(messageList.get(this.lastUnbrokenIndex));
                    viterbi.setPrevCandidates(candidateList.get(this.lastUnbrokenIndex));
                    viterbi.setLastExtendedStates(extendedStates.get(this.lastUnbrokenIndex));
//                    while (!hasCandidate[i + 1]) {
//                        i++;
//                        missingCandidateCount++;
//                    }
                }
                viterbi.setBroken(false);
            } else {
                if (lastUnbrokenIndex != -1) {
                    // continuous after removing points, deal with removed points
                    // set matches of the breaking points to its closest road
                    currBreakIndex.sort(Comparator.comparingInt(m -> m));
                    for (int j = currBreakIndex.get(0); j < i; j++) {
                        STPoint currPoint = trajectory.get(j);
                        SequenceState<MatchingPoint, STPoint, RoadPath> currMatch;
                        if (candidatesMap.get(currPoint).size() != 0) {
                            List<MatchingPoint> currPointCandidates = new ArrayList<>(candidatesMap.get(currPoint));
                            double minDistance = this.distanceFunction.pointToPointDistance(currPoint.x(), currPoint.y(), currPointCandidates.get(0).lon(), currPointCandidates.get(0).lat());
                            MatchingPoint choseMatch = currPointCandidates.get(0);
                            for (MatchingPoint m : currPointCandidates) {
                                double currDistance = this.distanceFunction.pointToPointDistance(currPoint.x(), currPoint.y(), m.lon(), m.lat());

                                if (minDistance > currDistance) {
                                    minDistance = currDistance;
                                    choseMatch = m;
                                }
                            }
                            currMatch = new SequenceState<>(choseMatch, currPoint, null);
                        } else {
                            currMatch = new SequenceState<>(null, currPoint, null);
                        }
                        roadPositions.add(currMatch);
                        if (j > lastUnbrokenIndex) {
                            timeStepList.add(null);
                            messageList.add(null);
                            candidateList.add(null);
                            extendedStates.add(null);
                        }
                    }
                    breakPoints.add(lastUnbrokenIndex);
                    currBreakIndex.clear();
                    lastUnbrokenIndex = -1;
                    lastBrokenIndex = i - 1;
                }
                // successful iteration
                prevTimeStep = timeStep;
                timeStepList.add(timeStep);
                messageList.add(viterbi.getMessage());
                candidateList.add(viterbi.getPrevCandidates());
                extendedStates.add(viterbi.getLastExtendedStates());
            }
        }

        // run HMM
        List<SequenceState<MatchingPoint, STPoint, RoadPath>> lastRoadPositions =
                viterbi.computeMostLikelySequence();
        roadPositions.addAll(lastRoadPositions);
        // Check whether the HMM occurred in the last time step
        if (viterbi.isBroken())

        {
            throw new MapMatchingException("Unnable to compute HMM Map-Matching, Viterbi computation "
                    + "is borken (the probability of all states equals zero).");
        }

        // if the last point is broken point
        if (lastUnbrokenIndex != -1)

        {
            currBreakIndex.sort(Comparator.comparingInt(m -> m));
            for (int j = currBreakIndex.get(currBreakIndex.size() - 1); j < trajectory.size(); j++) {
                STPoint currPoint = trajectory.get(j);
                SequenceState<MatchingPoint, STPoint, RoadPath> currMatch;
                if (candidatesMap.get(currPoint).size() != 0) {
                    List<MatchingPoint> currPointCandidates = new ArrayList<>(candidatesMap.get(currPoint));
                    double minDistance = this.distanceFunction.pointToPointDistance(currPoint.x(), currPoint.y(), currPointCandidates.get(0).lon(), currPointCandidates.get(0).lat());
                    MatchingPoint choseMatch = currPointCandidates.get(0);
                    for (MatchingPoint m : currPointCandidates) {
                        double currDistance = this.distanceFunction.pointToPointDistance(currPoint.x(), currPoint.y(), m.lon(), m.lat());

                        if (minDistance > currDistance) {
                            minDistance = currDistance;
                            choseMatch = m;
                        }
                    }
                    currMatch = new SequenceState<>(choseMatch, currPoint, null);
                } else
                    currMatch = new SequenceState<>(null, currPoint, null);
                roadPositions.add(currMatch);
                if (j > lastUnbrokenIndex) {
                    messageList.add(null);
                    candidateList.add(null);
                    extendedStates.add(null);
                    timeStepList.add(null);
                }
            }
            breakPoints.add(lastUnbrokenIndex);
            currBreakIndex.clear();
            lastUnbrokenIndex = -1;
            lastBrokenIndex = trajectory.size() - 1;
        }

        if (timeStepList.size() != trajectory.size()) {
            System.out.println("Time step size is not equal to trajectory size");
        }

        if (roadPositions.size() != trajectory.size()) {
            System.out.println("Matching result size is not equal to trajectory size");
        }

        roadPositions.sort(Comparator.comparingLong(m -> m.observation.time()));

        if (brokenTraj) {
            brokenTrajCount += 1;
            // generate unmatched trajectories
            breakPoints.sort(Comparator.comparingInt(m -> m));
            int prevEndPoint = 0;
            for (int i : breakPoints) {
                int startPoint;
                int endPoint;
                if (i > prevEndPoint + 1) {
                    for (startPoint = i - 1; startPoint > prevEndPoint; startPoint--) {
                        if (roadPositions.get(startPoint).state != null) {
                            STPoint currGPSPoint = roadPositions.get(startPoint).observation;
                            Point currMatchPoint = roadPositions.get(startPoint).state.getMatchPoint();
                            if (this.distanceFunction.pointToPointDistance(currGPSPoint.x(), currGPSPoint.y(), currMatchPoint.x(), currMatchPoint.y()) < unmatchedTrajThreshold) {
                                startPoint += 1;
                                break;
                            }
                        }
                    }
                } else {
                    startPoint = i;
                }
                if (i < trajectory.size() - 2) {
                    for (endPoint = i + 2; endPoint < trajectory.size(); endPoint++) {
                        if (roadPositions.get(endPoint).state != null) {
                            STPoint currGPSPoint = roadPositions.get(endPoint).observation;
                            Point currMatchPoint = roadPositions.get(endPoint).state.getMatchPoint();
                            if (this.distanceFunction.pointToPointDistance(currGPSPoint.x(), currGPSPoint.y(), currMatchPoint.x(), currMatchPoint.y()) < unmatchedTrajThreshold) {
                                endPoint -= 1;
                                break;
                            }
                        }
                    }
                } else {
                    endPoint = i + 1;
                }
                unmatchedTrajectoryList.add(trajectory.subTrajectory(startPoint, endPoint));
                prevEndPoint = endPoint;
            }
        }
        candidatesMap.clear();
        return getResult(roadPositions);

    }

    /**
     * {@inheritDoc}
     * <p>
     * Note that since only a collections of road nodes is passed to this method,
     * the method does not take the connectivity of the road nodes into account.
     */
    @Override
    public List<PointNodePair> doMatching(Collection<STPoint> pointsList, Collection<RoadNode> nodesList)
            throws MapMatchingException {
        // Compute the candidates list for every GPS point
        this.computePointBasedCandidates(pointsList, nodesList);

        ViterbiAlgorithm<MatchingPoint, STPoint, RoadPath> viterbi = new ViterbiAlgorithm<>();
        TimeStep<MatchingPoint, STPoint, RoadPath> prevTimeStep = null;
        // build the lattice
        for (STPoint gpsPoint : pointsList) {
            final Collection<MatchingPoint> candidates = candidatesMap.get(gpsPoint);
            final TimeStep<MatchingPoint, STPoint, RoadPath> timeStep = new TimeStep<>(gpsPoint, candidates);
            computeEmissionProbabilities(timeStep);
            if (prevTimeStep == null) {
                viterbi.startWithInitialObservation(timeStep.observation, timeStep.candidates,
                        timeStep.emissionLogProbabilities);
            } else {
                computeTransitionProbabilities(prevTimeStep, timeStep);
                viterbi.nextStep(
                        timeStep.observation,
                        timeStep.candidates,
                        timeStep.emissionLogProbabilities,
                        timeStep.transitionLogProbabilities,
                        timeStep.roadPaths);
            }
            prevTimeStep = timeStep;
        }
        // run HMM
        List<SequenceState<MatchingPoint, STPoint, RoadPath>> roadPositions =
                viterbi.computeMostLikelySequence();

        // Check whether the HMM occurred in the last time step
        if (viterbi.isBroken()) {
            throw new MapMatchingException("Unnable to compute HMM Map-Matching, Viterbi computation "
                    + "is borken (the probability of all states equals zero).");
        }

        return getResult(roadPositions);
    }

    /**
     * Compute the candidates list for every GPS point using a radius query.
     *
     * @param pointsList List of GPS trajectory points to map.
     * @param wayList    List of road network nodes to search for candidates.
     */
    private void computeCandidates(Collection<STPoint> pointsList, Collection<RoadWay> wayList) {
        double distance;
        // for every point find the nodes within a distance = 'candidatesThreshold'
        for (STPoint gpsPoint : pointsList) {
            candidatesMap.put(gpsPoint, new ArrayList<MatchingPoint>());
            for (RoadWay way : wayList) {
                for (Segment s : way.getEdges()) {
                    distance = distanceFunction.pointToSegmentDistance(gpsPoint, s);
                    if (distance <= candidatesThreshold) {
                        Point matchPoint = distanceFunction.findPerpendicularPoint(gpsPoint, s);
                        MatchingPoint currMatchingPoint = new MatchingPoint(matchPoint, s, way.getId());
                        candidatesMap.get(gpsPoint).add(currMatchingPoint);
                    }
                }
            }
        }
    }

    /**
     * Compute the candidates list for every GPS point using a radius query.
     *
     * @param pointsList List of GPS trajectory points to map.
     * @param nodesList  List of road network nodes to search for candidates.
     */
    private void computePointBasedCandidates(Collection<STPoint> pointsList, Collection<RoadNode> nodesList) {
        double distance;
        // for every point find the nodes within a distance = 'candidatesThreshold'
        for (STPoint gpsPoint : pointsList) {
            candidatesMap.put(gpsPoint, new ArrayList<MatchingPoint>());
            for (RoadNode node : nodesList) {
                distance = getDistance(gpsPoint.x(), gpsPoint.y(), node.lon(), node.lat());
                if (distance <= candidatesThreshold) {
                    MatchingPoint currMatchingPoint = new MatchingPoint(new Point(node.lon(), node.lat()), new Segment(), node.getId());
                    candidatesMap.get(gpsPoint).add(currMatchingPoint);
                }
            }
        }
    }

    /**
     * Compute the emission probabilities between every GPS point and its candidates.
     *
     * @param timeStep
     */
    private void computeEmissionProbabilities(TimeStep<MatchingPoint, STPoint, RoadPath> timeStep) {
        for (MatchingPoint candidate : timeStep.candidates) {
            double distance = getDistance(
                    timeStep.observation.x(), timeStep.observation.y(),
                    candidate.lon(), candidate.lat());
            timeStep.addEmissionLogProbability(candidate,
                    hmmProbabilities.emissionLogProbability(distance));
        }
    }

    /**
     * Compute the transition probabilities between every state.
     *
     * @param prevTimeStep
     * @param timeStep
     */
    private void computeTransitionProbabilities(
            TimeStep<MatchingPoint, STPoint, RoadPath> prevTimeStep,
            TimeStep<MatchingPoint, STPoint, RoadPath> timeStep) {
        final double linearDistance = getDistance(
                prevTimeStep.observation.x(), prevTimeStep.observation.y(),
                timeStep.observation.x(), timeStep.observation.y());
        final double timeDiff = (timeStep.observation.time() - prevTimeStep.observation.time());

        for (MatchingPoint from : prevTimeStep.candidates) {
            for (MatchingPoint to : timeStep.candidates) {
                final double routeLength = getDistance(
                        from.lon(), from.lat(), to.lon(), to.lat());
                final double transitionLogProbability = hmmProbabilities.transitionLogProbability(
                        routeLength, linearDistance, timeDiff);
                timeStep.addRoadPath(from, to, new RoadPath(from, to));
                timeStep.addTransitionLogProbability(from, to, transitionLogProbability);
            }
        }
    }

    /**
     * Compute the transition probabilities between every state,
     * taking the connectivity of the road nodes into account.
     *
     * @param prevTimeStep
     * @param timeStep
     */
    private void computeTransitionProbabilitiesWithConnectivity(
            TimeStep<MatchingPoint, STPoint, RoadPath> prevTimeStep,
            TimeStep<MatchingPoint, STPoint, RoadPath> timeStep) {
        final double linearDistance = getDistance(
                prevTimeStep.observation.x(), prevTimeStep.observation.y(),
                timeStep.observation.x(), timeStep.observation.y());
        final double timeDiff = (timeStep.observation.time() - prevTimeStep.observation.time());
        double maxDistance = 0;
        if (timeDiff > 30 && linearDistance > 1) {
            maxDistance = (50 * timeDiff) < linearDistance * 8 ? 50 * timeDiff : linearDistance * 8;
        } else
            maxDistance = 50 * timeDiff;
        for (MatchingPoint from : prevTimeStep.candidates) {
            List<MatchingPoint> candidates = new ArrayList<>(timeStep.candidates);
            double[] distanceList = this.roadNetworkGraph.calculateShortestDistanceList(from, candidates, maxDistance);
            for (int i = 0; i < candidates.size(); i++) {
                if (distanceList[i] != Double.MAX_VALUE) {
                    timeStep.addRoadPath(from, candidates.get(i), new RoadPath(from, candidates.get(i)));
                    double transitionLogProbability = hmmProbabilities.transitionLogProbability(
                            distanceList[i], linearDistance, timeDiff);
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
    private List<PointNodePair> getResult(List<SequenceState<MatchingPoint, STPoint, RoadPath>> roadPositions) {
        List<PointNodePair> matchPairs = new ArrayList<>();
        double distance;
        for (SequenceState<MatchingPoint, STPoint, RoadPath> sequence : roadPositions) {
            STPoint point = sequence.observation;
            MatchingPoint matchingPoint = sequence.state;
            if (matchingPoint != null) {
                distance = getDistance(point.x(), point.y(), matchingPoint.lon(), matchingPoint.lat());
                // make sure it returns a copy of the objects
                matchPairs.add(new PointNodePair(point.clone(), matchingPoint.clone(), distance));
            } else {
                matchPairs.add(new PointNodePair(point.clone(), null, Double.MAX_VALUE));
            }
        }
        return matchPairs;
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

    public List<Trajectory> getUnMatchedTraj() {
        return unmatchedTrajectoryList;
    }

    public int getBrokenTrajCount() {
        return brokenTrajCount;
    }
}
