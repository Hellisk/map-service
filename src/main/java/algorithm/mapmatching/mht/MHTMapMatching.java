package algorithm.mapmatching.mht;

import algorithm.mapmatching.MapMatchingMethod;
import org.apache.log4j.Logger;
import util.dijkstra.RoutingGraph;
import util.function.DistanceFunction;
import util.index.rtree.RTreeIndexing;
import util.object.roadnetwork.RoadNetworkGraph;
import util.object.spatialobject.Trajectory;
import util.object.spatialobject.TrajectoryPoint;
import util.object.structure.Pair;
import util.object.structure.PointMatch;
import util.object.structure.SimpleTrajectoryMatchResult;
import util.settings.BaseProperty;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Online map-matching algorithm based on Multiple Hypothesis Theory and route prediction. Implemented according to the paper:
 * <p>
 * Taguchi, S., Koide, S., & Yoshimura, T. (2018). Online map matching with route prediction. IEEE Transactions on Intelligent
 * Transportation Systems, 20(1), 338-347.
 * <p>
 * The speed of trajectory sample is needed.
 *
 * @author uqpchao
 * Created 18/08/2019
 */
public class MHTMapMatching implements MapMatchingMethod, Serializable {
	
	private static final Logger LOG = Logger.getLogger(MHTMapMatching.class);
	
	/**
	 * parameters for the algorithm.
	 */
	private final int candidateRange;    // for the initial candidate search
	private final double sigma;        // standard deviation GPS error
	private final double sigmaA;    // velocity model system error used in Kalman Filter (m/s)
	private final double sigmaV;    // velocity observation error used in Kalman Filter (km/h)
	private final double thresholdPrediction;    // prediction threshold
	private final double thresholdUpdate;        // update threshold
	private final DistanceFunction distFunc;
	private final BaseProperty prop;
	private final RoadNetworkGraph roadMap;
	private final RoadNetworkGraph originalMap;
	private final RTreeIndexing rtree;
	private final RoutingGraph routingGraph;
	
	public MHTMapMatching(RoadNetworkGraph roadMap, BaseProperty property) {
		this.originalMap = roadMap;
		this.roadMap = roadMap.toLooseMap();
		this.prop = property;
		this.distFunc = roadMap.getDistanceFunction();
		this.candidateRange = property.getPropertyInteger("algorithm.mapmatching.CandidateRange");
		this.sigma = property.getPropertyDouble("algorithm.mapmatching.Sigma");
		this.sigmaA = property.getPropertyDouble("algorithm.mapmatching.mht.SigmaA");
		this.sigmaV = property.getPropertyDouble("algorithm.mapmatching.mht.SigmaV");
		this.thresholdPrediction = property.getPropertyDouble("algorithm.mapmatching.mht.PredictionThreshold");
		this.thresholdUpdate = property.getPropertyDouble("algorithm.mapmatching.mht.UpdateThreshold");
		this.rtree = new RTreeIndexing(this.roadMap);
		this.routingGraph = new RoutingGraph(this.roadMap, false, property);
	}
	
	@Override
	public Pair<List<Double>, SimpleTrajectoryMatchResult> onlineMatching(Trajectory traj) {
		// set the latency to zero as it does not rely on match buffer
		List<Double> latencyList = new ArrayList<>(traj.size());
		for (int i = 0; i < traj.size(); i++) {
			latencyList.add(0d);
		}
		// check if the trajectory contains speed information
		if (traj.get(0).speed() == Double.NEGATIVE_INFINITY) {
			LOG.debug("The current trajectory does not contains speed info, generate it automatically.");
			for (int i = 0; i < traj.size() - 1; i++) {
				TrajectoryPoint currPoint = traj.get(i);
				TrajectoryPoint nextPoint = traj.get(i + 1);
				currPoint.setSpeed(distFunc.distance(nextPoint, currPoint) / (nextPoint.time() - currPoint.time()));
			}
			traj.get(traj.size() - 1).setSpeed(traj.get(traj.size() - 2).speed());
		}
		
		TrajectoryPoint startTrajPoint = traj.get(0);
		List<PointMatch> initialPointMatch = rtree.searchNeighbours(startTrajPoint, candidateRange);
		double observationErrorDist = sigmaV;
		double avgSpd = startTrajPoint.speed();
		
		for (int i = 0; i < traj.size(); i++) {
			TrajectoryPoint currTrajPoint = traj.get(i);
			if (i > 0) {
			
			}
		}
		
		
		SimpleTrajectoryMatchResult matchResult = null;
		
		return new Pair<>(latencyList, matchResult);
	}
}