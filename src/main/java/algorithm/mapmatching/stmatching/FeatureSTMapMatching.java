package algorithm.mapmatching.stmatching;

import org.apache.log4j.Logger;
import util.function.DistanceFunction;
import util.object.roadnetwork.RoadNetworkGraph;
import util.object.spatialobject.Trajectory;
import util.object.structure.PointMatch;
import util.object.structure.SimpleTrajectoryMatchResult;
import util.settings.BaseProperty;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Offline ST-matching map-matching algorithm implemented according to the paper:
 * <p>
 * Yin, Y., Shah, R.R., Wang, G., Zimmermann, R.: Feature-based map matching for low-sampling-rate gps trajectories. ACM Transactions on
 * Spatial Algorithms and Systems (TSAS) 4(2), 4 (2018)
 * <p>
 * The road speed limit is needed in this algorithm.
 *
 * @author uqpchao
 * Created 18/08/2019
 */
public class FeatureSTMapMatching implements Serializable {
	
	private static final Logger LOG = Logger.getLogger(FeatureSTMapMatching.class);
	
	private final int candidateRange;    // in meter
	private final DistanceFunction distFunc;
	private final BaseProperty prop;
	
	public FeatureSTMapMatching(RoadNetworkGraph roadMap, BaseProperty property) {
		this.prop = property;
		this.distFunc = roadMap.getDistanceFunction();
		this.candidateRange = property.getPropertyInteger("algorithm.mapmatching.CandidateRange");
	}
	
	public SimpleTrajectoryMatchResult doMatching(Trajectory traj) {
		List<String> routeMatchList = new ArrayList<>();
		List<PointMatch> pointMatchList = new ArrayList<>();
		return new SimpleTrajectoryMatchResult(traj.getID(), pointMatchList, routeMatchList);
	}
}
