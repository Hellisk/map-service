package traminer.util.map.matching.hmm;

import traminer.util.exceptions.MapMatchingException;
import traminer.util.map.matching.MapMatchingMethod;
import traminer.util.map.matching.PointNodePair;
import traminer.util.map.roadnetwork.RoadNetworkGraph;
import traminer.util.map.roadnetwork.RoadNode;
import traminer.util.map.roadnetwork.RoadWay;
import traminer.util.spatial.distance.EuclideanDistanceFunction;
import traminer.util.spatial.distance.PointDistanceFunction;
import traminer.util.spatial.objects.st.STPoint;
import traminer.util.trajectory.Trajectory;

import java.util.*;

/**
 * Implementation of Hidden Markov Model map-matching algorithm,
 * using the hmm-lib.
 * <p>
 * Based on Newson, Paul, and John Krumm. "Hidden Markov map matching through noise and sparseness."
 * Proceedings of the 17th ACM SIGSPATIAL International Conference on Advances in Geographic
 * Information Systems. ACM, 2009.
 *
 * @author uqdalves
 * @see https://github.com/bmwcarit/hmm-lib.
 */
@SuppressWarnings("serial")
public class HMMMapMatching implements MapMatchingMethod {
    /**
     * The distance method to use between points
     */
    private final PointDistanceFunction distanceFunction;
    /**
     * Map each point to a list of candidate nodes
     */
    private final Map<STPoint, Collection<RoadNode>> candidatesMap = new HashMap<>();
    /**
     * The probabilities of the HMM lattice
     */
    private final HMMProbabilities hmmProbabilities = new HMMProbabilities();
    /**
     * Transitions (connectivity) between nodes in the road path
     */
    private final Set<Transition<RoadNode>> transitions = new HashSet<>();
    /**
     * The distance threshold for the candidate points
     */
    private final double candidatesThreshold;

    /**
     * Creates a new HMM map-matching method with default
     * Euclidean distance function.
     *
     * @param candidatesThreshold The distance threshold for the candidate points.
     */
    public HMMMapMatching(double candidatesThreshold) {
        this.distanceFunction = new EuclideanDistanceFunction();
        this.candidatesThreshold = candidatesThreshold;
    }

    /**
     * Creates a new HMM map-matching method with the
     * given distance function.
     *
     * @param distFunc            The point-to-node distance function to use.
     * @param candidatesThreshold The distance threshold for the candidate points.
     */
    public HMMMapMatching(PointDistanceFunction distFunc, double candidatesThreshold) {
        this.distanceFunction = distFunc;
        this.candidatesThreshold = candidatesThreshold;
    }

    @Override
    public List<PointNodePair> doMatching(Trajectory trajectory, RoadNetworkGraph roadNetwrokGraph)
            throws MapMatchingException {
        // Compute the candidates list for every GPS point
        computeCandidates(trajectory, roadNetwrokGraph.getNodes());
        // Compute the connectivity of the
        computeTransitions(roadNetwrokGraph.getWays());

        ViterbiAlgorithm<RoadNode, STPoint, RoadPath> viterbi = new ViterbiAlgorithm<>();
        TimeStep<RoadNode, STPoint, RoadPath> prevTimeStep = null;
        // build the lattice
        for (STPoint gpsPoint : trajectory) {
            final Collection<RoadNode> candidates = candidatesMap.get(gpsPoint);
            final TimeStep<RoadNode, STPoint, RoadPath> timeStep = new TimeStep<>(gpsPoint, candidates);
            computeEmissionProbabilities(timeStep);
            if (prevTimeStep == null) {
                viterbi.startWithInitialObservation(timeStep.observation, timeStep.candidates,
                        timeStep.emissionLogProbabilities);
            } else {
                computeTransitionProbabilitiesWithConnectivity(prevTimeStep, timeStep);
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
        List<SequenceState<RoadNode, STPoint, RoadPath>> roadPositions =
                viterbi.computeMostLikelySequence();

        // Check whether the HMM occurred in the last time step
        if (viterbi.isBroken()) {
            throw new MapMatchingException("Unnable to compute HMM Map-Matching, Viterbi computation "
                    + "is borken (the probability of all states equals zero).");
        }

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
        computeCandidates(pointsList, nodesList);

        ViterbiAlgorithm<RoadNode, STPoint, RoadPath> viterbi = new ViterbiAlgorithm<>();
        TimeStep<RoadNode, STPoint, RoadPath> prevTimeStep = null;
        // build the lattice
        for (STPoint gpsPoint : pointsList) {
            final Collection<RoadNode> candidates = candidatesMap.get(gpsPoint);
            final TimeStep<RoadNode, STPoint, RoadPath> timeStep = new TimeStep<>(gpsPoint, candidates);
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
        List<SequenceState<RoadNode, STPoint, RoadPath>> roadPositions =
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
     * @param nodesList  List of road network nodes to search for candidates.
     */
    private void computeCandidates(Collection<STPoint> pointsList, Collection<RoadNode> nodesList) {
        double distance;
        // for every point find the nodes within a distance = 'candidatesThreshold'
        for (STPoint gpsPoint : pointsList) {
            candidatesMap.put(gpsPoint, new ArrayList<RoadNode>());
            for (RoadNode node : nodesList) {
                distance = getDistance(gpsPoint.x(), gpsPoint.y(), node.lon(), node.lat());
                if (distance <= candidatesThreshold) {
                    candidatesMap.get(gpsPoint).add(node);
                }
            }
        }
    }

    /**
     * Compute the transition (connectivity) between two consecutive nodes
     * in the road network. Connectivity is based on the road way/path.
     *
     * @param ways The ways in the road network graph.
     */
    private void computeTransitions(Collection<RoadWay> ways) {
        for (RoadWay way : ways) {
            RoadNode from, to;
            for (int i = 0; i < way.size() - 1; i++) {
                from = way.getNode(i);
                to = way.getNode(i + 1);
                transitions.add(new Transition<RoadNode>(from, to));
            }
        }
    }

    /**
     * Compute the emission probabilities between every GPS point and its candidates.
     *
     * @param timeStep
     */
    private void computeEmissionProbabilities(TimeStep<RoadNode, STPoint, RoadPath> timeStep) {
        for (RoadNode candidate : timeStep.candidates) {
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
            TimeStep<RoadNode, STPoint, RoadPath> prevTimeStep,
            TimeStep<RoadNode, STPoint, RoadPath> timeStep) {
        final double linearDistance = getDistance(
                prevTimeStep.observation.x(), prevTimeStep.observation.y(),
                timeStep.observation.x(), timeStep.observation.y());
        final double timeDiff = (timeStep.observation.time() - prevTimeStep.observation.time());

        for (RoadNode from : prevTimeStep.candidates) {
            for (RoadNode to : timeStep.candidates) {
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
            TimeStep<RoadNode, STPoint, RoadPath> prevTimeStep,
            TimeStep<RoadNode, STPoint, RoadPath> timeStep) {
        final double linearDistance = getDistance(
                prevTimeStep.observation.x(), prevTimeStep.observation.y(),
                timeStep.observation.x(), timeStep.observation.y());
        final double timeDiff = (timeStep.observation.time() - prevTimeStep.observation.time());

        for (RoadNode from : prevTimeStep.candidates) {
            for (RoadNode to : timeStep.candidates) {
                Transition<RoadNode> transition = new Transition<RoadNode>(from, to);
                timeStep.addRoadPath(from, to, new RoadPath(from, to));
                double routeLength;
                // they are connected
                if (transitions.contains(transition)) {
                    routeLength = getDistance(from.lon(), from.lat(), to.lon(), to.lat());
                }
                // they are not connected, mark their distance as infinity
                else {
                    routeLength = INFINITY;
                }
                double transitionLogProbability = hmmProbabilities.transitionLogProbability(
                        routeLength, linearDistance, timeDiff);
                timeStep.addTransitionLogProbability(from, to, transitionLogProbability);
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
    private List<PointNodePair> getResult(List<SequenceState<RoadNode, STPoint, RoadPath>> roadPositions) {
        List<PointNodePair> matchPairs = new ArrayList<>();
        double distance;
        for (SequenceState<RoadNode, STPoint, RoadPath> sequence : roadPositions) {
            STPoint point = sequence.observation;
            RoadNode node = sequence.state;
            distance = getDistance(point.x(), point.y(), node.lon(), node.lat());
            // make sure it returns a copy of the objects
            matchPairs.add(new PointNodePair(point.clone(), node.clone(), distance));
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
}
