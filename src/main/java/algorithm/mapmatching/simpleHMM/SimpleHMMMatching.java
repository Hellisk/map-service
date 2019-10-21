package algorithm.mapmatching.simpleHMM;


import algorithm.mapmatching.MapMatchingMethod;
import algorithm.mapmatching.hmm.HMMProbabilities;
import algorithm.mapmatching.weightBased.Utilities;
import util.dijkstra.RoutingGraph;
import util.function.DistanceFunction;
import util.index.rtree.RTreeIndexing;
import util.object.roadnetwork.RoadNetworkGraph;
import util.object.spatialobject.Point;
import util.object.spatialobject.Trajectory;
import util.object.spatialobject.TrajectoryPoint;
import util.object.structure.Pair;
import util.object.structure.PointMatch;
import util.object.structure.SimpleTrajectoryMatchResult;
import util.object.structure.Triplet;
import util.settings.BaseProperty;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


public class SimpleHMMMatching implements MapMatchingMethod, Serializable {
    private final RoadNetworkGraph roadMap;
    private final RoutingGraph routingGraph;
    private final DistanceFunction distFunc;
    private final RTreeIndexing rtree;
    private HMMProbabilities hmmProbabilities;
    private double candidateRange;
    //    private double dijkstraDist;
    private long maxWaitingTime;
    private double gamma;
    private double turnWeight;
    private String hmmMethod;

    public SimpleHMMMatching(RoadNetworkGraph roadMap, BaseProperty property) {
        this.roadMap = roadMap;
        this.routingGraph = new RoutingGraph(roadMap, false, property);
        this.distFunc = roadMap.getDistanceFunction();
        this.rtree = new RTreeIndexing(roadMap);
        double sigma = property.getPropertyDouble("algorithm.mapmatching.Sigma");
        double beta = property.getPropertyDouble("algorithm.mapmatching.hmm.Beta");
        gamma = property.getPropertyDouble("algorithm.mapmatching.hmm.Eddy.Gamma");
        hmmMethod = property.getPropertyString("algorithm.mapmatching.MatchingMethod");
        turnWeight = property.getPropertyDouble("algorithm.mapmatching.hmm.turnWeight");
        this.hmmProbabilities = new HMMProbabilities(sigma, beta);
        this.candidateRange = property.getPropertyDouble("algorithm.mapmatching.CandidateRange");
//        this.dijkstraDist = property.getPropertyDouble("algorithm.mapmatching.wgt.DijkstraThreshold");
        this.maxWaitingTime = property.getPropertyLong("algorithm.mapmatching.WindowSize");
    }

    /**
     * Gets transitions and its transition probabilities for each pair of state candidates t and t-1
     *
     * @param prevMemory StateMemory object of predecessor state candidate
     * @param candidates Tuple of a set of state candidate at t and its respective measurement sample.
     * @return Maps each predecessor state candidate at t-1 to all state candidates at t.
     * All transitions from t-1 to t and its transition probability, or null if there no transition.
     */
    private Map<String, Map<String, Pair<StateTransition, Double>>> transitions(
            StateMemory prevMemory, Pair<StateSample, Set<StateCandidate>> candidates) {

        StateSample sample = candidates._1();
        StateSample previous = prevMemory.getSample();
        double linearDist = distFunc.pointToPointDistance(sample.x(), sample.y(), previous.x(), previous.y());

        final List<PointMatch> targets = new LinkedList<>();
        for (StateCandidate candidate : candidates._2()) {
            targets.add(candidate.getPointMatch());
        }

        final Map<String, Map<String, Pair<StateTransition, Double>>> transitions = new ConcurrentHashMap<>();

        for (StateCandidate predecessor : prevMemory.getStateCandidates().values()) {
            Map<String, Pair<StateTransition, Double>> result = new HashMap<>();
            double timeDiff = sample.getTime() - previous.getTime();
            double maxDistance = Math.min((50 * timeDiff), linearDist * 8);
            List<Triplet<PointMatch, Double, List<String>>> shortestPath = Utilities.getShortestPaths(
                    routingGraph, targets, predecessor.getPointMatch(), new Point(sample.x(), sample.y(), distFunc), maxDistance);
            //List<Triplet<PointMatch, Double, List<String>>> shortestPath = Utilities.getShortestPaths(
            //                    routingGraph, targets, predecessor.getPointMatch(), dijkstraDist);

            Map<PointMatch, Pair<Double, List<String>>> map = new HashMap<>();
            for (Triplet<PointMatch, Double, List<String>> triplet : shortestPath) {
                map.put(triplet._1(), new Pair<>(triplet._2(), triplet._3()));
            }


            for (StateCandidate candidate : candidates._2()) {
                if (map.containsKey(candidate.getPointMatch())) {
                    // the predecessor is able to reach the candidate
                    double distance = map.get(candidate.getPointMatch())._1();
//                    double timeDiff = sample.getTime() - previous.getTime();
                    List<String> path = map.get(candidate.getPointMatch())._2();
                    if (hmmMethod.toLowerCase().contains("frechet") && path.size() > 0) linearDist = 0;
                    double transition = turnWeight <= 0 ?
                            hmmProbabilities.transitionProbability(distance, linearDist, timeDiff) :
                            hmmProbabilities.transitionProbabilityWithTurn(distance, linearDist, timeDiff, path, roadMap,
                                    turnWeight);

                    result.put(candidate.getId(),
                            new Pair<>(new StateTransition(map.get(candidate.getPointMatch())._2()), transition));
                }
            }
            transitions.put(predecessor.getId(), result);
        }
        return transitions;
    }


    /**
     * Gets state vector, which is a StateMemory objects and with its emission
     * probability.
     *
     * @param prevStateMemory Predecessor state memory
     * @param sample          current sample
     * @return Set of tuples consisting of a {@link StateCandidate} and its emission probability.
     */
    private Set<StateCandidate> getNeighbourPoints(StateMemory prevStateMemory, StateSample sample) {

        List<PointMatch> neighbourPms = this.rtree.searchNeighbours(sample.getSampleMeasurement(), candidateRange);
//        Set<PointMatch> neighbourPms = Minset.minimize(neighbourPms_, this.roadMap, this.distFunc);

//        Map<String, PointMatch> map = new HashMap<>();
//        for (PointMatch neighbourPm : neighbourPms) {
//            map.put(neighbourPm.getRoadID(), neighbourPm);
//        }
//
//        if (prevStateMemory != null) {
//            Set<StateCandidate> predecessors = new LinkedHashSet<>(prevStateMemory.getStateCandidates().values());
//
//            for (StateCandidate predecessor : predecessors) {
//                PointMatch curPM = map.get(predecessor.getPointMatch().getRoadID());
//                if (curPM != null && curPM.getMatchedSegment() != null) {
//                    // the cur pm and the predecessor pm is on a same roadway
//                    Segment matchedSeg = curPM.getMatchedSegment();
//
//                    if (// the dijkstraDist between predecessor and current sample is less than measurement deviation
//                            distFunc.pointToPointDistance(curPM.lat(), curPM.lon(),
//                                    predecessor.getPointMatch().lat(), predecessor.getPointMatch().lon())
//                                    < hmmProbabilities.getSigma())
//
//                        if (
//                            // same direction, cur PM should be closer to endpoint than predecessor, otherwise it is a wrong candidate
//                                (Math.abs(Utilities.computeHeading(matchedSeg.x1(), matchedSeg.y1(), matchedSeg.x2(), matchedSeg.y2())
//                                        - prevStateMemory.getSample().getHeading()) < 45
//                                        && distFunc.pointToPointDistance(curPM.lon(), curPM.lat(), matchedSeg.x2(), matchedSeg.y2())
//                                        > distFunc.pointToPointDistance(predecessor.lon(), predecessor.lat(), matchedSeg.x2(), matchedSeg.y2()))
//
//                                        // opposite direction, cur PM should be further to endpoint, otherwise it is a incorrect
//                                        || (Math.abs(Utilities.computeHeading(matchedSeg.x1(), matchedSeg.y1(), matchedSeg.x2(), matchedSeg.y2())
//                                        - prevStateMemory.getSample().getHeading()) >= 135
//                                        && distFunc.pointToPointDistance(curPM.lon(), curPM.lat(), matchedSeg.x2(), matchedSeg.y2())
//                                        < distFunc.pointToPointDistance(predecessor.lon(), predecessor.lat(), matchedSeg.x2(), matchedSeg.y2()))) {
//
//                            neighbourPms.remove(curPM);
//                            neighbourPms.add(predecessor.getPointMatch());
//                        }
//                }
//            }
//        }

        Set<StateCandidate> candidates = new LinkedHashSet<>();
        for (PointMatch neighbourPm : neighbourPms) {
            StateCandidate candidate = new StateCandidate(neighbourPm, sample);
            double dz = distFunc.pointToPointDistance(neighbourPm.lon(), neighbourPm.lat(), sample.x(), sample.y());
//            if (hmmMethod.toLowerCase().contains("frechet")) {
//                double timDiff = prevStateMemory == null ? 1 : sample.getTime() - prevStateMemory.getSample().getTime();
//                candidate.setEmiProb(hmmProbabilities.emissionProbabilityWithTime(dz, timDiff));
//            } else {
//                candidate.setEmiProb(hmmProbabilities.emissionProbability(dz));
//            }
            candidate.setEmiProb(hmmProbabilities.emissionProbability(dz));
            candidates.add(candidate);
        }
        return candidates;
    }


    /**
     * Executes Hidden Markov Model (HMM) filter iteration that determines for a given measurement
     * sample (a StateSample object) and of a predecessor state vector, which is a set of StateCandidate objects,
     * a state vector with filter and sequence probabilities set.
     * <p>
     * Note: The set of state candidates is allowed to be empty.
     * This is either the initial case or an HMM break occurred, which is no state candidates representing
     * the measurement sample could be found.
     *
     * @param prevStateMemory prevStateMemory, may be empty
     * @param sample          current sample
     * @return StateMemory    which may be empty if an HMM break occurred.
     */
    public StateMemory execute(StateMemory prevStateMemory, StateSample sample) {
        Set<StateCandidate> predecessors = new HashSet<>();
        /* prevStateMemory is null if initial MM */
        if (prevStateMemory != null) {
            predecessors = new LinkedHashSet<>(prevStateMemory.getStateCandidates().values());
        }

        Set<StateCandidate> stateCandidates = new HashSet<>();

        /* Get neighbouring points to this sample. If none, return empty an empty StateMemory object */
        Set<StateCandidate> neighbourPoints = getNeighbourPoints(prevStateMemory, sample);
        if (neighbourPoints.isEmpty()) return new StateMemory(stateCandidates, sample);

        double normSum = 0;

        if (!predecessors.isEmpty()) {

            Map<String, Map<String, Pair<StateTransition, Double>>> transitions =
                    transitions(prevStateMemory, new Pair<>(sample, neighbourPoints));

            /* Assign the most likely predecessor for each neighbouring point */
            for (StateCandidate neighbourPoint : neighbourPoints) {
                double maxSeqProb = -1; // seqProb used to find backTrackingPointer

                /* Find a predecessor that maximize filtProb for the neighbouring point */
                for (StateCandidate predecessor : predecessors) {
                    Pair<StateTransition, Double> transition = transitions.get(predecessor.getId()).get(neighbourPoint.getId());

                    if (transition == null || transition._2() == 0) {
                        continue;
                    }
                    double seqProb = predecessor.getFiltProb() * transition._2();
                    if (seqProb > maxSeqProb) {
                        neighbourPoint.setPredecessor(predecessor);
                        neighbourPoint.setTransition(transition._1());
                        maxSeqProb = seqProb;
                    }
                }

                /* A neighbouring point is a valid candidate for this sample only if it connects to a predecessor */
                if (neighbourPoint.getPredecessor() != null) {
                    neighbourPoint.setFiltProb(maxSeqProb * neighbourPoint.getEmiProb());
                    stateCandidates.add(neighbourPoint);
                    normSum += neighbourPoint.getFiltProb();
                }

            }
        }

        /* stateCandidates is empty if none of the neighbouring point connect to a predecessor (i.e. HMM break)*/
        /* predecessors is empty if HMM break happened in the previous (last) state */
        if (stateCandidates.isEmpty() || predecessors.isEmpty()) {
            // either because initial map-matching or matching break
            for (StateCandidate candidate : neighbourPoints) {
                if (candidate.getEmiProb() == 0) {
                    continue;
                }
                normSum += candidate.getEmiProb();
                candidate.setFiltProb(candidate.getEmiProb());
                stateCandidates.add(candidate);

            }
        }

        for (StateCandidate candidate : stateCandidates) {
            candidate.setFiltProb(candidate.getFiltProb() / normSum);
        }
        return new StateMemory(stateCandidates, sample);
    }

    private Pair<List<Double>, Pair<List<PointMatch>, List<String>>> pullMatchResult(SequenceMemory sequence,
                                                                                     Trajectory trajectory) {
        if (trajectory.size() == 0) {
            throw new RuntimeException("Invalid trajectory");
        }
        List<StateSample> samples = new LinkedList<>();
        for (int i = 0; i < trajectory.getPoints().size(); i++) {
            samples.add(new StateSample(trajectory.get(i), trajectory.get(i).heading(), trajectory.get(i).time()));
        }

        samples.sort((left, right) -> (int) (left.getTime() - right.getTime()));

        Map<String, StateCandidate> optimalCandidateSeq = new HashMap<>(); // key is state id
        // calculate latency
        List<Double> latency = new ArrayList<>();
        // Record states have been matched
        Set<String> preStatesRecord = new HashSet<>();
        for (StateSample sample : samples) {
            StateMemory vector = execute(sequence.lastStateMemory(), sample);
            // ignore a gps point which doesn't have candidate point
            if (!vector.getStateCandidates().isEmpty()) {
                if (hmmMethod.toLowerCase().contains("eddy")) {
                    sequence.updateEddy(vector, sample, optimalCandidateSeq, gamma);
                } else if (hmmMethod.toLowerCase().contains("goh")) {
                    sequence.updateGoh(vector, sample, optimalCandidateSeq);
                } else if (hmmMethod.toLowerCase().contains("fix")) {
                    sequence.updateFixed(vector, sample, optimalCandidateSeq);
                } else sequence.updateFixed(vector, sample, optimalCandidateSeq); // offline mode
            } else {
                // the sample got no neighbouring point on road network
                optimalCandidateSeq.put(Double.toString(sample.getTime()), new StateCandidate());
            }

            if (hmmMethod.toLowerCase().contains("on")) {
                if (preStatesRecord.size() != optimalCandidateSeq.size()) {
                    // optimalCandiSeq update
                    Set<String> updateStoredStates = new HashSet<>(optimalCandidateSeq.keySet());
                    for (String newStateName : updateStoredStates) {
                        if (!preStatesRecord.contains(newStateName)) {
                            latency.add(sample.getTime() - Double.parseDouble(newStateName));
                        }
                    }
                    preStatesRecord = updateStoredStates;
                }
            }
        }

        List<StateMemory> stateMemories = sequence.getStateMemoryVector();
        if (stateMemories.size() > 0) {
            // calculate latency if online scenario
            if (hmmMethod.toLowerCase().contains("on")) {
                double lastSampleTime = stateMemories.get(stateMemories.size() - 1).getSample().getTime();
                for (StateMemory stateMemory : stateMemories) {
                    latency.add(lastSampleTime - stateMemory.getSample().getTime());
                }
            }

            if (hmmMethod.toLowerCase().contains("eddy")) {
                sequence.forceFinalOutput(optimalCandidateSeq, gamma);
            } else {
                // both goh and fixed-window use this method to get last states
                sequence.reverse(optimalCandidateSeq, sequence.getStateMemoryVector().size() - 1);
            }
        }

        List<String> routeMatchResult = new LinkedList<>();
        List<PointMatch> pointMatchResult = new LinkedList<>();

        for (TrajectoryPoint trajectoryPoint : trajectory) {
            String id = Double.toString(trajectoryPoint.time());
            if (optimalCandidateSeq.get(id).getPointMatch() != null) {
                StateCandidate candidate = optimalCandidateSeq.get(id);
                StateSample sample = candidate.getStateSample();
                if (candidate.getId().charAt(0) == '_') continue;
                Point point = distFunc.getClosestPoint(
                        sample.getSampleMeasurement(), candidate.getPointMatch().getMatchedSegment());
                PointMatch pm = new PointMatch(point, candidate.getPointMatch().getMatchedSegment(), candidate.getId());
                pointMatchResult.add(pm);
                routeMatchResult.addAll(candidate.getTransition().getRoute());
            } else {
                pointMatchResult.add(new PointMatch(distFunc));
            }
        }
        return new Pair<>(latency, new Pair<>(pointMatchResult, routeMatchResult));
    }

    /**
     * Matches a full sequence of samples, StateSample objects and returns state
     * representation of the full matching which is a SequenceMemory object. Output the map-matching result eventually.
     *
     * @param trajectory Sequence of samples, StateSample objects.
     * @return State representation of the full matching which is a SequenceMemory object.
     */
    @Override
    public SimpleTrajectoryMatchResult offlineMatching(Trajectory trajectory) {
        if (trajectory == null) return null;
        Pair<List<PointMatch>, List<String>> pointToRouteResult =
                pullMatchResult(new SequenceMemory(), trajectory)._2();

        List<PointMatch> pointMatchResult = pointToRouteResult._1();
        List<String> routeMatchResult = pointToRouteResult._2();
        return new SimpleTrajectoryMatchResult(trajectory.getID(), pointMatchResult, routeMatchResult);
    }

    @Override
    public Pair<List<Double>, SimpleTrajectoryMatchResult> onlineMatching(Trajectory trajectory) {
        if (trajectory == null) return null;
        SequenceMemory sequenceMemory = null;
        if (hmmMethod.toLowerCase().contains("goh") || hmmMethod.toLowerCase().contains("fix")) {
            sequenceMemory = new SequenceMemory(maxWaitingTime);
        } else if (hmmMethod.toLowerCase().contains("eddy")) {
            sequenceMemory = new SequenceMemory();
        }
        Pair<List<Double>, Pair<List<PointMatch>, List<String>>> result = pullMatchResult(sequenceMemory, trajectory);
        List<Double> latency = result._1();
        Pair<List<PointMatch>, List<String>> pointToRouteResult = result._2();
        List<PointMatch> pointMatchResult = pointToRouteResult._1();
        List<String> routeMatchResult = pointToRouteResult._2();

        return new Pair<>(latency,
                new SimpleTrajectoryMatchResult(trajectory.getID(), pointMatchResult, routeMatchResult));
    }
}
