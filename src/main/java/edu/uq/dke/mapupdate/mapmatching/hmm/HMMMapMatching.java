package edu.uq.dke.mapupdate.mapmatching.hmm;

import edu.uq.dke.mapupdate.datatype.PointMatch;
import edu.uq.dke.mapupdate.datatype.TrajectoryMatchResult;
import edu.uq.dke.mapupdate.util.dijkstra.Graph;
import traminer.util.Pair;
import traminer.util.exceptions.MapMatchingException;
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
     * the index of the last point before current broken position, -1: complete matching sequence
     */
    private int currBreakPointIndex = -1;
    /**
     * the last position that preserved in the current matching result
     */
    private int lastReservededPointIndex = -1;
    /**
     * the points before this index has been well processed
     */
    private int newMatchStartPointIndex = 0;

    /**
     * Creates a new HMM map-matching method with the
     * given distance function.
     *
     * @param distFunc            The point-to-node distance function to use.
     * @param candidatesThreshold The distance threshold for the candidate points.
     */
    HMMMapMatching(GreatCircleDistanceFunction distFunc, double candidatesThreshold, double unmatchedTrajThreshold, RoadNetworkGraph roadNetworkGraph) {
        this.distanceFunction = distFunc;
        this.candidatesThreshold = candidatesThreshold;
        this.roadNetworkGraph = new Graph(roadNetworkGraph);
        this.unmatchedTrajThreshold = unmatchedTrajThreshold;
    }

    public TrajectoryMatchResult doMatching(Trajectory trajectory, RoadNetworkGraph roadNetworkGraph)
            throws MapMatchingException {
        // Compute the candidate road segment list for every GPS point
        // TODO optimize accuracy & efficiency
        computeCandidates(trajectory, roadNetworkGraph.getWays());
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
        // check if every point has candidates
        // build the lattice
        for (int i = 0; i < trajectory.size(); i++) {
            Collection<PointMatch> candidates;
            TimeStep<PointMatch, STPoint, RoadPath> timeStep;
            STPoint gpsPoint = trajectory.get(i);
            if (candidatesMap.get(gpsPoint).size() == 0) {
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
//                    messageList.add(viterbi.getMessage());
//                    candidateList.add(viterbi.getPrevCandidates());
//                    extendedStates.add(viterbi.getLastExtendedStates());
                    prevTimeStep = timeStep;
                } else {
                    if (currBreakPointIndex != -1) {
                        System.out.println("ERROR! The first point has no candidate");
                        breakPoints.add(i); // directly add to the break point list
                    } else {
                        // already broken, add a new point
                        currBreakIndex.add(i);
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
//                        lastReservededPointIndex = i - 1;
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
                        lastReservededPointIndex = -1;
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
                computeTransitionProbabilitiesWithConnectivity(prevTimeStep, timeStep);
                viterbi.nextStep(
                        timeStep.observation,
                        timeStep.candidates,
                        timeStep.emissionLogProbabilities,
                        timeStep.transitionLogProbabilities,
                        timeStep.roadPaths);

                if (viterbi.isBroken()) {
                    // the match stops due to no connection, add the current point and its predecessor to the broken list
                    brokenTraj = true;
                    if (currBreakPointIndex == -1) {
                        currBreakPointIndex = i - 1;
                        lastReservededPointIndex = i - 2;
                        currBreakIndex.add(i);
                        currBreakIndex.add(i - 1);
                        breakPoints.add(currBreakPointIndex);
                        if (lastReservededPointIndex < newMatchStartPointIndex) {
                            // new match breaks at the second position, start from the third point
                            viterbi.setMessage(null);
                            viterbi.setLastExtendedStates(null);
                            prevTimeStep = null;
                            currBreakPointIndex = -1;
                            lastReservededPointIndex = -1;
                            continue;
                        }
                    } else {
                        currBreakIndex.add(i);
                        currBreakIndex.add(lastReservededPointIndex);
                        lastReservededPointIndex--;
                        if (lastReservededPointIndex < newMatchStartPointIndex) {
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
                            lastReservededPointIndex = -1;
                            continue;
                        }
                    }
                    // remove the broken points and expect a reconnection
                    viterbi.setMessage(messageList.get(lastReservededPointIndex));
                    viterbi.setPrevCandidates(candidatesMap.get(trajectory.get(lastReservededPointIndex)));
                    viterbi.setBroken(false);
                } else {
                    // the match continues
                    if (currBreakPointIndex != -1) {
                        // continuous after removing points, deal with the removed points, remove the break flag and continue
                        currBreakPointIndex = -1;
                        lastReservededPointIndex = -1;
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
//                        lastReservededPointIndex = i - 1;
//                    }
                    // successful iteration
                    prevTimeStep = timeStep;
                    messageList.put(i, viterbi.getMessage());
                }
            } else {
                if (currBreakPointIndex != -1) {
                    breakPoints.add(i);
                    currBreakIndex.add(i);
                } else {
                    currBreakPointIndex = i - 1;
                    lastReservededPointIndex = i - 1;
                }
            }
        }

        // complete the final part of the matching sequence
        if (currBreakPointIndex != -1) {
            viterbi.setMessage(messageList.get(currBreakPointIndex));
            // we finish the matching before break point and waive the rest of the sequence
            currBreakPointIndex = -1;
            lastReservededPointIndex = -1;
        }
        List<SequenceState<PointMatch, STPoint, RoadPath>> lastRoadPositions =
                viterbi.computeMostLikelySequence();
        roadPositions.addAll(lastRoadPositions);
        // Check whether the HMM occurred in the last time step
        if (viterbi.isBroken())

        {
            throw new MapMatchingException("ERROR! Unnable to compute HMM Map-Matching, Viterbi computation "
                    + "is borken (the probability of all states equals zero).");
        }

        roadPositions.sort(Comparator.comparingLong(m -> m.observation.time()));

        if (brokenTraj) {
            brokenTrajCount += 1;
            // generate unmatched trajectories
            breakPoints.sort(Comparator.comparingInt(m -> m));
            Set<Integer> extendedBreakPoints = new LinkedHashSet<>();
            int lastUnmatchedPoint = 0;
            for (int i : breakPoints) {
                if (i > 0) {
                    extendedBreakPoints.add(i - 1);
                }
                extendedBreakPoints.add(i);
                extendedBreakPoints.add(i + 1);

                for (int p = 2; i - p > lastUnmatchedPoint; p++) {
                    if (findMinimumDist(trajectory.get(i - p), candidatesMap.get(trajectory.get(i - p))) > unmatchedTrajThreshold) {
                        extendedBreakPoints.add(i - p);
                    } else {
                        break;
                    }
                }
                for (int p = 2; i + p < trajectory.size(); p++) {
                    if (findMinimumDist(trajectory.get(i + p), candidatesMap.get(trajectory.get(i + p))) > unmatchedTrajThreshold) {
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
        }
        candidatesMap.clear();
        return getResult(trajectory, roadPositions);

    }

    private double findMinimumDist(STPoint stPoint, Collection<PointMatch> pointMatches) {
        double minDistance = Double.MAX_VALUE;
        for (PointMatch p : pointMatches) {
            double dist = this.distanceFunction.distance(p.getMatchPoint(), stPoint);
            minDistance = dist < minDistance ? dist : minDistance;
        }
        return minDistance;
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
        // TODO no index, brut-force solution
        for (STPoint gpsPoint : pointsList) {
            candidatesMap.put(gpsPoint, new ArrayList<PointMatch>());
            for (RoadWay way : wayList) {
                for (Segment s : way.getEdges()) {
                    distance = distanceFunction.pointToSegmentDistance(gpsPoint, s);
                    if (distance <= candidatesThreshold) {
                        Point matchPoint = distanceFunction.findPerpendicularPoint(gpsPoint, s);
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
     * @param nodesList  List of road network nodes to search for candidates.
     */
    private void computePointBasedCandidates
    (Collection<STPoint> pointsList, Collection<RoadNode> nodesList) {
        double distance;
        // for every point find the nodes within a distance = 'candidatesThreshold'
        for (STPoint gpsPoint : pointsList) {
            candidatesMap.put(gpsPoint, new ArrayList<>());
            for (RoadNode node : nodesList) {
                distance = getDistance(gpsPoint.x(), gpsPoint.y(), node.lon(), node.lat());
                if (distance <= candidatesThreshold) {
                    PointMatch currPointMatch = new PointMatch(new Point(node.lon(), node.lat()), new Segment(), node.getId());
                    candidatesMap.get(gpsPoint).add(currPointMatch);
                }
            }
        }
    }

    /**
     * Compute the emission probabilities between every GPS point and its candidates.
     *
     * @param timeStep
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
     * @param prevTimeStep
     * @param timeStep
     */
    private void computeTransitionProbabilitiesWithConnectivity(
            TimeStep<PointMatch, STPoint, RoadPath> prevTimeStep,
            TimeStep<PointMatch, STPoint, RoadPath> timeStep) {
        final double linearDistance = getDistance(
                prevTimeStep.observation.x(), prevTimeStep.observation.y(),
                timeStep.observation.x(), timeStep.observation.y());
        final double timeDiff = (timeStep.observation.time() - prevTimeStep.observation.time());
        double maxDistance;

        if (timeDiff > 30 && linearDistance > 1) {
            maxDistance = (50 * timeDiff) < linearDistance * 8 ? 50 * timeDiff : linearDistance * 8;
        } else
            maxDistance = 50 * timeDiff;
        for (PointMatch from : prevTimeStep.candidates) {
            List<PointMatch> candidates = new ArrayList<>(timeStep.candidates);
            List<Pair<Double, List<String>>> shortestPathResultList = this.roadNetworkGraph.calculateShortestDistanceList(from, candidates, maxDistance);
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
        List<PointNodePair> matchPairs = new ArrayList<>();
        Set<String> path = new LinkedHashSet<>();
        double distance;
        for (SequenceState<PointMatch, STPoint, RoadPath> sequence : roadPositions) {
            STPoint point = sequence.observation;
            PointMatch pointMatch = sequence.state;
            if (pointMatch != null) {
                distance = getDistance(point.x(), point.y(), pointMatch.lon(), pointMatch.lat());
                // make sure it returns a copy of the objects
                matchPairs.add(new PointNodePair(point.clone(), pointMatch.clone(), distance));
            } else {
                System.out.println("ERROR! Matching result should not have NULL value");
                matchPairs.add(new PointNodePair(point.clone(), null, Double.MAX_VALUE));
            }
            if (sequence.transitionDescriptor != null) {
                path.addAll(sequence.transitionDescriptor.passingRoadID);
            }
        }
        TrajectoryMatchResult result = new TrajectoryMatchResult(traj.getId());
        result.setMatchingResult(matchPairs);
        result.setMatchWayList(new ArrayList<>(path));
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

    public List<Trajectory> getUnMatchedTraj() {
        return unmatchedTrajectoryList;
    }

    public int getBrokenTrajCount() {
        return brokenTrajCount;
    }
}
