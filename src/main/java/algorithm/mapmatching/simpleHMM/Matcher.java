package algorithm.mapmatching.simpleHMM;

import algorithm.mapmatching.hmm.HMMProbabilities;
import algorithm.mapmatching.weightBased.Utilities;
import util.dijkstra.RoutingGraph;
import util.function.DistanceFunction;
import util.index.rtree.RTreeIndexing;
import util.object.roadnetwork.RoadNetworkGraph;
import util.object.spatialobject.Segment;
import util.object.spatialobject.Trajectory;
import util.object.structure.Pair;
import util.object.structure.PointMatch;
import util.object.structure.Triplet;
import util.settings.BaseProperty;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Matcher {

    private final RoadNetworkGraph roadMap;
    private final RoutingGraph routingGraph;
    private final DistanceFunction distFunc;
    private final RTreeIndexing rtree;
    private HMMProbabilities hmmProbabilities;
    private double candidateRange;
    private double dijkstraDist;
    private List<Pair<Integer, List<String>>> outputRouteMatchResult = new LinkedList<>();


    public Matcher(RoadNetworkGraph roadMap, BaseProperty prop,
                   HMMProbabilities hmmProbabilities, double candidateRange, double dijkstraDist) {
        this.roadMap = roadMap;
        this.routingGraph = new RoutingGraph(roadMap, false, prop);
        this.distFunc = roadMap.getDistanceFunction();
        this.rtree = new RTreeIndexing(roadMap);
        this.hmmProbabilities = hmmProbabilities;
        this.candidateRange = candidateRange;
        this.dijkstraDist = dijkstraDist;
    }


    /**
     * Gets transitions and its transition probabilities for each pair of state candidates t and t-1
     *
     * @param prevMemory StateMemory object of predecessor state candidate
     * @param candidates Tuple of a set of state candidate at t and its respective measurement sample.
     * @return Maps each predecessor state candidate at t-1 to all state candidates at t.
     * All transitions from t-1 to t and its transition probability, or null if there no transition.
     */
    public Map<String, Map<String, Pair<StateTransition, Double>>> transitions(
            StateMemory prevMemory, Pair<StateSample, Set<StateCandidate>> candidates) {

        StateSample sample = candidates._1();
        StateSample previous = prevMemory.getSample();
        double linearDist = distFunc.pointToPointDistance(sample.x(), sample.y(), previous.x(), previous.y());

        final List<PointMatch> targets = new LinkedList<>();
        for (StateCandidate candidate : candidates._2()) {
            targets.add(candidate.getPointMatch());
        }

        final Map<String, Map<String, Pair<StateTransition, Double>>> transitions =
                new ConcurrentHashMap<>();

        for (StateCandidate predecessor : prevMemory.getStateCandidates()) {
            Map<String, Pair<StateTransition, Double>> result = new HashMap<>();

            List<Triplet<PointMatch, Double, List<String>>> shortestPath = Utilities.getShortestPaths(
                    routingGraph, targets, predecessor.getPointMatch(), dijkstraDist);

            Map<PointMatch, Pair<Double, List<String>>> map = new HashMap<>();
            for (Triplet<PointMatch, Double, List<String>> triplet : shortestPath) {
                map.put(triplet._1(), new Pair<>(triplet._2(), triplet._3()));
            }

            for (StateCandidate candidate : candidates._2()) {
                if (map.containsKey(candidate.getPointMatch())) {
                    // the predecessor is able to reach the candidate
                    double distance = map.get(candidate.getPointMatch())._1();
                    double timeDiff = sample.getTime() - previous.getTime();
                    double transition = hmmProbabilities.transitionProbability(distance, linearDist, timeDiff);
                    result.put(candidate.getId(), new Pair<>(new StateTransition(map.get(candidate.getPointMatch())._2()), transition));
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
    private Set<StateCandidate> candidates(StateMemory prevStateMemory, StateSample sample) {

        List<PointMatch> neighbourPms = this.rtree.searchNeighbours(sample.getSampleMeasurement(), candidateRange);
//        Set<PointMatch> neighbourPms = Minset.minimize(neighbourPms_, this.roadMap, this.distFunc);

        Map<String, PointMatch> map = new HashMap<>();
        for (PointMatch neighbourPm : neighbourPms) {
            map.put(neighbourPm.getRoadID(), neighbourPm);
        }

        if (prevStateMemory != null) {
            Set<StateCandidate> predecessors = prevStateMemory.getStateCandidates();

            for (StateCandidate predecessor : predecessors) {
                PointMatch curPM = map.get(predecessor.getPointMatch().getRoadID());
                if (curPM != null && curPM.getMatchedSegment() != null) {
                    // the cur pm and the predecessor pm is on a same roadway
                    Segment matchedSeg = curPM.getMatchedSegment();

                    if (// the dijkstraDist between predecessor and current sample is less than measurement deviation
                            distFunc.pointToPointDistance(curPM.lat(), curPM.lon(),
                                    predecessor.getPointMatch().lat(), predecessor.getPointMatch().lon())
                                    < hmmProbabilities.getSigma())

                        if (
                            // same direction, cur PM should be closer to endpoint than predecessor, otherwise it is a wrong candidate
                                (Math.abs(Utilities.computeHeading(matchedSeg.x1(), matchedSeg.y1(), matchedSeg.x2(), matchedSeg.y2())
                                        - prevStateMemory.getSample().getHeading()) < 45
                                        && distFunc.pointToPointDistance(curPM.lon(), curPM.lat(), matchedSeg.x2(), matchedSeg.y2())
                                        > distFunc.pointToPointDistance(predecessor.lon(), predecessor.lat(), matchedSeg.x2(), matchedSeg.y2()))

                                        // opposite direction, cur PM should be further to endpoint, otherwise it is a incorrect
                                        || (Math.abs(Utilities.computeHeading(matchedSeg.x1(), matchedSeg.y1(), matchedSeg.x2(), matchedSeg.y2())
                                        - prevStateMemory.getSample().getHeading()) >= 135
                                        && distFunc.pointToPointDistance(curPM.lon(), curPM.lat(), matchedSeg.x2(), matchedSeg.y2())
                                        < distFunc.pointToPointDistance(predecessor.lon(), predecessor.lat(), matchedSeg.x2(), matchedSeg.y2()))) {

                            neighbourPms.remove(curPM);
                            neighbourPms.add(predecessor.getPointMatch());
                        }
                }
            }
        }

        Set<StateCandidate> candidates = new HashSet<>();
        for (PointMatch neighbourPm : neighbourPms) {
            StateCandidate candidate = new StateCandidate(neighbourPm, sample);
            double dz = distFunc.pointToPointDistance(neighbourPm.lon(), neighbourPm.lat(), sample.x(), sample.y());
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
        if (prevStateMemory != null) {
            predecessors = prevStateMemory.getStateCandidates();
        }


        Set<StateCandidate> result = new HashSet<>();
        Set<StateCandidate> candidates = candidates(prevStateMemory, sample);

        double normsum = 0;

        if (!predecessors.isEmpty()) {

            Map<String, Map<String, Pair<StateTransition, Double>>> transitions =
                    transitions(prevStateMemory, new Pair<>(sample, candidates));

            for (StateCandidate candidate : candidates) {
                candidate.setSeqProb(Double.NEGATIVE_INFINITY);

                for (StateCandidate predecessor : predecessors) {
                    Set<String> preds_ = transitions.keySet();
                    String id = predecessor.getId();
                    Pair<StateTransition, Double> transition = transitions.get(predecessor.getId()).get(candidate.getId());

                    if (transition == null || transition._2() == 0) {
                        continue;
                    }

                    candidate.setFiltProb(
                            candidate.getFiltProb() + (transition._2() * predecessor.getFiltProb()));

                    double seqprob = predecessor.getSeqProb() + Math.log10(transition._2())
                            + Math.log10(candidate.getEmiProb());


                    if (seqprob > candidate.getSeqProb()) {
                        candidate.setPredecessor(predecessor);
                        candidate.setTransition(transition._1());
                        candidate.setSeqProb(seqprob);
                    }
                }


                if (candidate.getFiltProb() == 0) {
                    continue;
                }

                candidate.setFiltProb(candidate.getFiltProb() * candidate.getEmiProb());
                result.add(candidate);

                normsum += candidate.getFiltProb();
            }
        }


        if (result.isEmpty() || predecessors.isEmpty()) {
            for (StateCandidate candidate : candidates) {
                if (candidate.getEmiProb() == 0) {
                    continue;
                }
                normsum += candidate.getEmiProb();
                candidate.setFiltProb(candidate.getEmiProb());
                candidate.setSeqProb(Math.log10(candidate.getEmiProb()));
                result.add(candidate);

            }
        }


        for (StateCandidate candidate : result) {
            candidate.setFiltProb(candidate.getFiltProb() / normsum);
        }
        return new StateMemory(result, sample);
    }

    /**
     * Matches a full sequence of samples, StateSample objects and returns state
     * representation of the full matching which is a SequenceMemory object.
     *
     * @param trajectory Sequence of samples, StateSample objects.
     * @return State representation of the full matching which is a SequenceMemory object.
     */
    public List<StateCandidate> mmatch(Trajectory trajectory) {
        List<StateSample> samples = new LinkedList<>();

        for (int i = 0; i < trajectory.getPoints().size(); i++) {
            samples.add(new StateSample(trajectory.get(i), trajectory.get(i).heading(), trajectory.get(i).time()));
        }

        Collections.sort(samples, new Comparator<StateSample>() {
            @Override
            public int compare(StateSample left, StateSample right) {
                return (int) (left.getTime() - right.getTime());
            }
        });

        SequenceMemory sequence = new SequenceMemory();


        for (StateSample sample : samples) {
            StateMemory vector = execute(sequence.lastStateMemory(), sample);
            sequence.update(vector, sample);
        }

        return reverse(sequence);
    }


    /**
     * Gets the most likely sequence of state candidates
     *
     * @return List of the most likely sequence of state candidates.
     */
    private List<StateCandidate> reverse(SequenceMemory sequence) {
        if (sequence.getStateMemoryVector().isEmpty()) {
            return null;
        }

        StateCandidate kestimate = sequence.optimalPredecessor();
        LinkedList<StateCandidate> ksequence = new LinkedList<>();

        for (int i = sequence.getStateMemoryVector().size() - 1; i >= 0; --i) {
            if (kestimate != null) {
                ksequence.push(kestimate);
                kestimate = kestimate.getPredecessor();
            }
        }

        return ksequence;
    }


    public void pullResult(List<StateCandidate> sequence, int trajectoryID) {
        List<String> matchRoute = new LinkedList<>();

        for (StateCandidate candidate : sequence) {
            matchRoute.addAll(candidate.getTransition().getRoute());
        }
        this.outputRouteMatchResult.add(new Pair<>(trajectoryID, matchRoute));
    }

    public List<Pair<Integer, List<String>>> getOutputRouteMatchResult() {
        return this.outputRouteMatchResult;
    }
}
