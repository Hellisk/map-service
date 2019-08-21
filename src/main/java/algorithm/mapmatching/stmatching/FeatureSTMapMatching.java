package algorithm.mapmatching.stmatching;

import algorithm.mapinference.lineclustering.DouglasPeuckerFilter;
import org.apache.log4j.Logger;
import util.dijkstra.RoutingGraph;
import util.function.DistanceFunction;
import util.index.rtree.RTreeIndexing;
import util.object.roadnetwork.RoadNetworkGraph;
import util.object.spatialobject.Trajectory;
import util.object.spatialobject.TrajectoryPoint;
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
	
	/**
	 * parameters for the algorithm.
	 */
	private final int candidateRange;    // in meter
	private final double tolerance;    // Douglas-Peucker distance threshold
	
	private final DistanceFunction distFunc;
	private final BaseProperty prop;
	private final RTreeIndexing rtree;
	private final RoutingGraph routingGraph;
	
	public FeatureSTMapMatching(RoadNetworkGraph roadMap, BaseProperty property) {
		this.prop = property;
		this.distFunc = roadMap.getDistanceFunction();
		this.candidateRange = property.getPropertyInteger("algorithm.mapmatching.CandidateRange");
		this.tolerance = property.getPropertyDouble("algorithm.mapmatching.fst.Tolerance");
		this.rtree = new RTreeIndexing(roadMap);
		this.routingGraph = new RoutingGraph(roadMap, false, prop);
	}
	
	public SimpleTrajectoryMatchResult doMatching(Trajectory traj) {
		
		// find the key GPS points through Douglas-Peucker algorithm
		DouglasPeuckerFilter dpFilter = new DouglasPeuckerFilter(tolerance, distFunc);
		List<Integer> keyTrajPointList = dpFilter.dpSimplifier(traj);    // the indices of the key trajectory points for segmentation
		
		for (int i = 0; i < keyTrajPointList.size() - 1; i++) {
			List<TrajectoryPoint> currSubTrajPointList = traj.subList(keyTrajPointList.get(i), keyTrajPointList.get(i) + 1);
//			Trajectory currTraj =
		}
		
		List<String> routeMatchList = new ArrayList<>();
		List<PointMatch> pointMatchList = new ArrayList<>();
		return new SimpleTrajectoryMatchResult(traj.getID(), pointMatchList, routeMatchList);
	}
}
