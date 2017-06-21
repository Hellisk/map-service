package traminer.util.map.matching.hmm;

import com.bmw.hmm.SequenceState;
import com.bmw.hmm.ViterbiAlgorithm;
import traminer.util.map.matching.MapMatchingMethod;
import traminer.util.map.matching.MatchPair;
import traminer.util.map.roadnetwork.RoadNetworkGraph;
import traminer.util.map.roadnetwork.RoadNode;
import traminer.util.map.roadnetwork.RoadWay;
import traminer.util.spatial.distance.EuclideanDistanceFunction;
import traminer.util.spatial.distance.PointDistanceFunction;
import traminer.util.spatial.objects.st.STPoint;
import traminer.util.trajectory.Trajectory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Off-line map matching algorithm, using the HMM library.
 * <p>
 * Hidden Markov Model-based map matching from the paper:
 * <br> "Newson, Paul; Krumm, John. Hidden Markov map matching through
 * noise and sparseness. In: Proceedings of the 17th ACM SIGSPATIAL, 2009."
 *
 * @author uqdalves
 */
@SuppressWarnings("serial")
public class HMMMatching implements MapMatchingMethod {
    /**
     * The distance method to use between points
     */
    private final PointDistanceFunction distanceFunction;
    private HMMProbabilities hmmProbabilities = new HMMProbabilities();
    private double candidatesRadius;

    /**
     * @param distFunc The points distance function to use.
     */
    public HMMMatching(double candidatesRadius, PointDistanceFunction distFunc) {
        this.distanceFunction = distFunc;
        this.candidatesRadius = candidatesRadius;
    }

    public HMMMatching(double candidatesRadius) {
        this.distanceFunction = new EuclideanDistanceFunction();
    }

    @Override
    public RoadWay doMatching(
            Trajectory trajectory, RoadNetworkGraph roadNetwrokGraph) {
        List<SequenceState<RoadNode, STPoint, RoadWay>> hmmResult =
                runHMM(trajectory, roadNetwrokGraph.getNodes());
        // extract result
        RoadWay result = new RoadWay();
        for (SequenceState<RoadNode, STPoint, RoadWay> seq : hmmResult) {
            result.addNode(seq.state);
        }
        return result;
    }

    @Override
    public List<MatchPair> doMatching(
            final Collection<STPoint> pointsList,
            final Collection<RoadNode> nodesList) {
        List<SequenceState<RoadNode, STPoint, RoadWay>> hmmResult =
                runHMM(pointsList, nodesList);
        // extract result
        List<MatchPair> result = new ArrayList<>();
        for (SequenceState<RoadNode, STPoint, RoadWay> seq : hmmResult) {
            result.add(new MatchPair(seq.observation, seq.state));
        }
        return result;
    }

    private List<SequenceState<RoadNode, STPoint, RoadWay>> runHMM(
            Collection<STPoint> gpsPoints,
            Collection<RoadNode> roadNodes) {
        ViterbiAlgorithm<RoadNode, STPoint, RoadWay> viterbi =
                new ViterbiAlgorithm<>();
        TimeStep<RoadNode, STPoint, RoadWay> prevTimeStep = null;

        // build the HMM lattice
        for (STPoint gpsPoint : gpsPoints) {
            final Collection<RoadNode> candidates =
                    computeCandidates(gpsPoint, roadNodes);
            final TimeStep<RoadNode, STPoint, RoadWay> timeStep =
                    new TimeStep<>(gpsPoint, candidates);
            computeEmissionProbabilities(timeStep);
            if (prevTimeStep == null) {
                viterbi.startWithInitialObservation(
                        timeStep.observation, timeStep.candidates,
                        timeStep.emissionLogProbabilities);
            } else {
                computeTransitionProbabilities(prevTimeStep, timeStep);
                viterbi.nextStep(
                        timeStep.observation, timeStep.candidates,
                        timeStep.emissionLogProbabilities,
                        timeStep.transitionLogProbabilities,
                        timeStep.roadPaths);
            }
            prevTimeStep = timeStep;
        }

        // compute the HMM
        List<SequenceState<RoadNode, STPoint, RoadWay>> roadPositions =
                viterbi.computeMostLikelySequence();

        assert (!viterbi.isBroken());

        return roadPositions;
    }

    /**
     * Get candidates nodes within a given radius from the
     * matching point.
     * <p>
     * For real map matching applications, candidates would be
     * computed using a radius query.
     */
    private Collection<RoadNode> computeCandidates(
            STPoint point, Collection<RoadNode> roadNodes) {
        List<RoadNode> candidates = new ArrayList<>();
        double dist = 0.0;
        for (RoadNode node : roadNodes) {
            dist = distanceFunction.pointToPointDistance(node.lon(), node.lat(), point.x(), point.y());
            if (dist <= candidatesRadius) {
                candidates.add(node);
            }
        }
        System.out.println(candidates.size());
        return candidates;
    }

    private void computeEmissionProbabilities(
            TimeStep<RoadNode, STPoint, RoadWay> timeStep) {
        for (RoadNode node : timeStep.candidates) {
            STPoint stepObs = timeStep.observation;
            double distance = distanceFunction.pointToPointDistance(stepObs.x(), stepObs.y(),
                    node.lon(), node.lat());
            timeStep.addEmissionLogProbability(node,
                    hmmProbabilities.emissionLogProbability(distance));
        }
    }

    private void computeTransitionProbabilities(
            TimeStep<RoadNode, STPoint, RoadWay> prevTimeStep,
            TimeStep<RoadNode, STPoint, RoadWay> timeStep) {
        STPoint prevObs = prevTimeStep.observation;
        STPoint stepObs = timeStep.observation;
        double distance = distanceFunction.pointToPointDistance(prevObs.x(), prevObs.y(), stepObs.x(), stepObs.y());
        double timeDiff = (stepObs.time() - prevObs.time()) / 100.0;
        for (RoadNode from : prevTimeStep.candidates) {
            for (RoadNode to : timeStep.candidates) {
                double routeLength = getRouteLength(from, to);
                timeStep.addRoadPath(from, to, new RoadWay("", from, to));
                double transitionLogProbability = hmmProbabilities.transitionLogProbability(
                        routeLength, distance, timeDiff);
                timeStep.addTransitionLogProbability(from, to, transitionLogProbability);
            }
        }
    }

    // For real map matching applications, route lengths and road paths would be
    // computed using a router. The most efficient way is to use a single-source
    // multi-target router.
    private double getRouteLength(RoadNode from, RoadNode to) {
        return distanceFunction.pointToPointDistance(from.lon(), from.lat(), to.lon(), to.lat());
    }
}
