package algorithm.mapmatching.stmatching;

import algorithm.mapinference.lineclustering.DouglasPeuckerFilter;
import algorithm.mapmatching.MapMatchingMethod;
import algorithm.mapmatching.hmm.Distributions;
import org.apache.log4j.Logger;
import util.dijkstra.RoutingGraph;
import util.function.DistanceFunction;
import util.index.rtree.RTreeIndexing;
import util.object.roadnetwork.RoadNetworkGraph;
import util.object.spatialobject.Segment;
import util.object.spatialobject.Trajectory;
import util.object.spatialobject.TrajectoryPoint;
import util.object.structure.Pair;
import util.object.structure.PointMatch;
import util.object.structure.SimpleTrajectoryMatchResult;
import util.settings.BaseProperty;

import java.io.Serializable;
import java.util.*;

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
public class FeatureSTMapMatching extends MapMatchingMethod implements Serializable {
	
	private static final Logger LOG = Logger.getLogger(FeatureSTMapMatching.class);
	
	/**
	 * parameters for the algorithm.
	 */
	private final int candidateRange;    // in meter
	private final int candidateSize;    // number of candidates for each key point
	private final double sigma;        // standard deviation of emission probability
	private final double tolerance;    // Douglas-Peucker distance threshold
	private final int maxCTraj;        // maximum distance weight threshold between candidate segment and trajectory points
	private final double omega;        // balancing factor used between C_len and C_turn in action weight
	private final DistanceFunction distFunc;
	private final BaseProperty prop;
	private final RoadNetworkGraph roadMap;
	private final RTreeIndexing rtree;
	private final RoutingGraph routingGraph;
	
	public FeatureSTMapMatching(RoadNetworkGraph roadMap, BaseProperty property) {
		this.roadMap = roadMap.toLooseMap();    // the current method only accept loose map
		this.prop = property;
		this.distFunc = roadMap.getDistanceFunction();
		this.candidateRange = property.getPropertyInteger("algorithm.mapmatching.CandidateRange");
		this.sigma = property.getPropertyDouble("algorithm.mapmatching.Sigma");
		this.tolerance = property.getPropertyDouble("algorithm.mapmatching.fst.Tolerance");
		this.candidateSize = property.getPropertyInteger("algorithm.mapmatching.fst.CandidateSize");
		this.maxCTraj = property.getPropertyInteger("algorithm.mapmatching.fst.MaxCTraj");
		this.omega = property.getPropertyDouble("algorithm.mapmatching.fst.Omega");
		this.rtree = new RTreeIndexing(this.roadMap);
		this.routingGraph = new RoutingGraph(this.roadMap, false, property);
	}
	
	@Override
	public SimpleTrajectoryMatchResult offlineMatching(Trajectory traj) {
		// find the key GPS points through Douglas-Peucker algorithm
		DouglasPeuckerFilter dpFilter = new DouglasPeuckerFilter(tolerance, distFunc);
		List<Integer> keyTrajPointList = dpFilter.dpSimplifier(traj);    // the indices of the key trajectory points for segmentation
		
		Map<Integer, List<PointMatch>> candidateMap = new HashMap<>();    // the key point index to the candidate set
		Map<Integer, double[]> emissionProbMap = new HashMap<>();    // the key point index to the candidate emission probability
		Map<Integer, double[][]> actionCostMap = new HashMap<>();    // the key point index to the candidate emission probability
		Map<String, List<String>> transitionPathMap = new HashMap<>();    // the transition route between candidate j to k at key point i,
		// format: (i_j_k, list of routeID) where 1<i< keyTrajPointList.size(), 0<j,k<candidateSize
		for (int i = 0; i < keyTrajPointList.size(); i++) {
			TrajectoryPoint currPoint = traj.get(keyTrajPointList.get(i));
			// find all candidates of the current key point
			List<PointMatch> candidateList = rtree.searchKNeighbours(currPoint, candidateSize, candidateRange);
			candidateMap.put(i, candidateList);
			
			// calculate the emission probability of each candidate
			double[] emissionProbList = new double[candidateSize];
			for (int index = 0; index < candidateList.size(); index++) {
				PointMatch pointMatch = candidateList.get(index);
				double distance = distFunc.distance(currPoint, pointMatch.getMatchPoint());
				emissionProbList[index] = Distributions.normalDistribution(sigma, distance);
			}
			emissionProbMap.put(i, emissionProbList);
			
			// compute the shortest path of the pairwise candidates between the current key point and its predecessor.
			if (i > 0) {
				List<TrajectoryPoint> currSubTrajPointList = traj.subList(keyTrajPointList.get(i - 1), keyTrajPointList.get(i) + 1);
				List<PointMatch> startPointMatchList = candidateMap.get(i - 1);
				double[][] actionCostMat = new double[startPointMatchList.size()][candidateList.size()];    // store the action cost of
				// all candidate transitions. actionCostMat[j][k] means the cost whose start candidate is j and end candidate is k.
				for (int j = 0; j < startPointMatchList.size(); j++) {
					PointMatch startPointMatch = startPointMatchList.get(j);
					TrajectoryPoint prevPoint = traj.get(keyTrajPointList.get(i - 1));
					double timeDiff = currPoint.time() - prevPoint.time();
					double linearDistance = distFunc.distance(prevPoint, currPoint);
					double maxDistance = Math.min((50 * timeDiff), linearDistance * 8);        // assume the maximum speed is 180km/h
					List<Pair<Double, List<String>>> shortestPathList = this.routingGraph.calculateShortestDistanceList(startPointMatch,
							candidateList, maxDistance);
					for (int k = 0; k < shortestPathList.size(); k++) {        // shortestPathList.get(k) is equivalent to candidateList.get(k)
						Pair<Double, List<String>> currTransition = shortestPathList.get(k);
						if (currTransition._1() == Double.POSITIVE_INFINITY || currTransition._2().isEmpty()) {    // the current
							// pair of candidates is not reachable. Action cost is max
							actionCostMat[j][k] = 0;
							continue;
						}
						String startPointMatchRoad = startPointMatch.getRoadID().split("\\|")[0];
						String endPointMatchRoad = candidateList.get(k).getRoadID().split("\\|")[0];
						List<String> currRoute = new ArrayList<>();
						// check if the start key point match and end key point match roads are included in the current route match
						if (!startPointMatchRoad.equals(shortestPathList.get(k)._2().get(0)))
							currRoute.add(startPointMatchRoad);
						currRoute.addAll(shortestPathList.get(k)._2());
						if (!endPointMatchRoad.equals(shortestPathList.get(k)._2().get(shortestPathList.get(k)._2().size() - 1)))
							currRoute.add(endPointMatchRoad);
						actionCostMat[j][k] = calculateActionCost(currSubTrajPointList, currRoute, traj.size());
						transitionPathMap.put(i + "_" + j + "_" + k, currRoute);
					}
				}
				actionCostMap.put(i, actionCostMat);
			}
		}
		
		Set<String> resultPath = new LinkedHashSet<>();
		double[] prevCandidateProb = emissionProbMap.get(0);    // store the candidate probability of preceding candidates
		double[] currCandidateProb = new double[candidateSize];    // store the candidate probability of the current step candidates
		int[][] prevCandidateIndexMat = new int[candidateSize][keyTrajPointList.size()];    // pre[j][i]=k means when the preceding
		// point of the candidate j is pre[k][i-1], the probability is maximum. Used in path backtracking.
		for (int[] doubleLines : prevCandidateIndexMat) {
			Arrays.fill(doubleLines, -1);    // initialise the matrix
		}
		
		for (int i = 1; i < keyTrajPointList.size(); i++) {
			List<PointMatch> currCandidateList = candidateMap.get(i);
			List<PointMatch> prevCandidateList = candidateMap.get(i - 1);
			double[] emissionProbList = emissionProbMap.get(i);
			double[][] actionCostMat = actionCostMap.get(i);
			boolean isConnected = false;        // check if there is at least one connection remains
			for (int j = 0; j < currCandidateList.size(); j++) {
				double maxProb = 0;
				for (int k = 0; k < prevCandidateList.size(); k++) {
					double currProb = prevCandidateProb[k] * actionCostMat[k][j] * emissionProbList[j];
					if (currProb > maxProb) {
						maxProb = currProb;
						prevCandidateIndexMat[j][i] = k;
					}
				}
				if (maxProb != 0) {
					isConnected = true;
				}
			}
			
			if (!isConnected) {
				addBestPath(i - 1, prevCandidateProb, prevCandidateIndexMat, transitionPathMap, resultPath);
				// the current step is completely disconnected with the last step, extract the previous path and start an initial matching
				currCandidateProb = emissionProbMap.get(i);
			}
			prevCandidateProb = currCandidateProb;
		}
		
		addBestPath(keyTrajPointList.size() - 1, prevCandidateProb, prevCandidateIndexMat, transitionPathMap, resultPath);    // backtrace
		// the path from the last steps
		List<String> routeMatchList = compactRoadID(resultPath);
		List<PointMatch> pointMatchList = new ArrayList<>();
		return new SimpleTrajectoryMatchResult(traj.getID(), pointMatchList, routeMatchList);
	}
	
	@Override
	public SimpleTrajectoryMatchResult onlineMatching(Trajectory traj) {
		int windowSizeSec = prop.getPropertyInteger("algorithm.mapmatching.WindowSize");
		List<Trajectory> subTrajList = splitTrajByWindowSize(traj, windowSizeSec);
		List<SimpleTrajectoryMatchResult> subTrajResultList = new ArrayList<>();
		for (Trajectory currTraj : subTrajList) {
			subTrajResultList.add(offlineMatching(currTraj));
		}
		List<String> routeMatch = new ArrayList<>();
		for (SimpleTrajectoryMatchResult matchResult : subTrajResultList) {
			List<String> currRouteMatch = matchResult.getRouteMatchResultList();
			if (!routeMatch.isEmpty() && !currRouteMatch.isEmpty()) {
				if (routeMatch.get(routeMatch.size() - 1).equals(currRouteMatch.get(0))) {
					// the route match of the two connecting margin points are the same, deduplicate it.
					routeMatch.addAll(currRouteMatch.subList(1, currRouteMatch.size()));
				}
			} else {
				routeMatch.addAll(currRouteMatch);
			}
		}
		return new SimpleTrajectoryMatchResult(traj.getID(), new ArrayList<>(), routeMatch);
	}
	
	private List<Trajectory> splitTrajByWindowSize(Trajectory originalTraj, int windowSizeSec) {
		List<Trajectory> resultTrajList = new ArrayList<>();
		List<TrajectoryPoint> currTrajPointList = new ArrayList<>();
		long firstPointTime = originalTraj.get(0).time();    // the timestamp of the first point in this window
		for (int i = 1; i < originalTraj.size(); i++) {
			long currPointTime = originalTraj.get(i).time();
			if (currPointTime - firstPointTime <= windowSizeSec || currTrajPointList.size() < 2)
				currTrajPointList.add(originalTraj.get(i));
			else {    // the time gap exceed the window, wrap up the current window and start a new one
				Trajectory currTraj = new Trajectory(originalTraj.getID() + "_" + resultTrajList.size(), currTrajPointList);
				resultTrajList.add(currTraj);
				currTrajPointList = new ArrayList<>();
				currTrajPointList.add(originalTraj.get(i));
				firstPointTime = currPointTime;
			}
		}
		if (currTrajPointList.size() > 1) {
			Trajectory currTraj = new Trajectory(originalTraj.getID() + "_" + resultTrajList.size(), currTrajPointList);
			resultTrajList.add(currTraj);
		} else if (currTrajPointList.size() == 1) {    // the final trajectory only contains 1 point, append to the last trajectory.
			resultTrajList.get(resultTrajList.size() - 1).add(currTrajPointList.get(0));
		}
		return resultTrajList;
	}
	
	/**
	 * Convert road IDs from loose map to compact map. Merge the road IDs that refers the same road.
	 *
	 * @param originalRoads The original road IDs whose format is "roadID_Sindex" e.g.: 123456_S1
	 * @return Route that only contains compact result.
	 */
	private List<String> compactRoadID(Set<String> originalRoads) {
		List<String> resultList = new ArrayList<>();
		for (String road : originalRoads) {
			if (resultList.isEmpty() || !road.split("_")[0].equals(resultList.get(resultList.size() - 1)))
				resultList.add(road.split("_")[0]);
		}
		return resultList;
	}
	
	private void addBestPath(int currStepIndex, double[] finalCandidateProb, int[][] prevCandidateIndexMat,
							 Map<String, List<String>> transitionPathMap, Set<String> resultPath) {
		int bestCandidateIndex = -1;
		double maxProb = 0;
		List<Integer> backTrackIndexList = new ArrayList<>();
		for (int j = 0; j < candidateSize; j++) {
			if (finalCandidateProb[j] > maxProb) {
				maxProb = finalCandidateProb[j];
				bestCandidateIndex = j;
			}
		}
		if (maxProb == 0)
			throw new IllegalArgumentException("The addBestPath fails due to no possible result.");
		while (currStepIndex >= 0 && bestCandidateIndex != -1) {
			backTrackIndexList.add(bestCandidateIndex);
			bestCandidateIndex = prevCandidateIndexMat[bestCandidateIndex][currStepIndex];
			currStepIndex--;
		}
		Collections.reverse(backTrackIndexList);
		currStepIndex += 2;    // start index of the route traversal TODO check the correctness
		for (int i = 0; i < backTrackIndexList.size() - 1; i++) {
			List<String> candidateRoute =
					transitionPathMap.get(currStepIndex + "_" + backTrackIndexList.get(i) + "_" + backTrackIndexList.get(i + 1));
			resultPath.addAll(candidateRoute);
		}
	}
	
	/**
	 * Calculate the action cost for a given trajectory segment (polyline between two trajectory key point)
	 *
	 * @param subTrajPointList The trajectory point list.
	 * @param candidateRoute   The candidate route regarded as the route match of the sub-trajectory.
	 * @return The normalised action cost of the candidate route.
	 */
	private double calculateActionCost(List<TrajectoryPoint> subTrajPointList, List<String> candidateRoute, int totalTrajPointCount) {
		double costRes = 0;
		for (int i = 0; i < candidateRoute.size(); i++) {
			String currRoadID = candidateRoute.get(i);
			Segment currWaySegment = roadMap.getWayByID(currRoadID).getEdges().get(0);        // the road way is guaranteed to have only one segment
			double cTraj = maxCTraj;
			for (TrajectoryPoint trajectoryPoint : subTrajPointList) {
				cTraj = Math.min(distFunc.distance(trajectoryPoint, currWaySegment), cTraj);
			}
			if (i != 0) {
				String prevRoadID = candidateRoute.get(i - 1);
				Segment prevWaySegment = roadMap.getWayByID(prevRoadID).getEdges().get(0);
				double angle = distFunc.getAngle(prevWaySegment, currWaySegment);
				angle = angle > 180 ? 360 - angle : angle;
				double cTurn = angle < 45 ? 0 : (angle >= 135 ? 2 : 1);
				costRes += cTraj * (currWaySegment.length() + omega * cTurn);
			} else {
				costRes += cTraj * currWaySegment.length();
			}
		}
		
		// normalise the action cost result
		double trajLength = 0;    // total distance of the sub-trajectory
		for (int i = 1; i < subTrajPointList.size(); i++) {
			trajLength += distFunc.distance(subTrajPointList.get(i - 1), subTrajPointList.get(i));
		}
		
		if (trajLength == 0)
			throw new IllegalArgumentException("The length of the sub-trajectory is zero.");
		return Math.exp(-(subTrajPointList.size() * costRes) / (totalTrajPointCount * trajLength));
	}
}
