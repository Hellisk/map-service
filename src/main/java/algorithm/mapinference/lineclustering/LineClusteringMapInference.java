package algorithm.mapinference.lineclustering;

import algorithm.mapinference.lineclustering.pcurves.PrincipalCurveGenerator;
import org.apache.log4j.Logger;
import util.function.DistanceFunction;
import util.function.HausdorffDistanceFunction;
import util.object.roadnetwork.RoadNetworkGraph;
import util.object.roadnetwork.RoadNode;
import util.object.roadnetwork.RoadWay;
import util.object.spatialobject.Point;
import util.object.spatialobject.Trajectory;
import util.object.structure.Pair;
import util.object.structure.Triplet;
import util.settings.BaseProperty;

import java.util.*;

/**
 * Trace clustering algorithm proposed in "X. Liu, J. Biagioni, J. Eriksson, Y. Wang, G. Forman, and Y. Zhu. Mining large-scale, sparse
 * GPS traces for map inference: Comparison of approaches. In KDD, 2012." It is also used in <tt>CrowdAtlas</tt> map updateGoh method as the map
 * inference step.
 *
 * @author Hellisk
 */
public class LineClusteringMapInference {
	
	private static final Logger LOG = Logger.getLogger(LineClusteringMapInference.class);
	
	/**
	 * Split each unmatched trajectory according to its direction continuity.
	 *
	 * @param inputTraj         The input unmatched trajectory list.
	 * @param angleChangeThresh The maximum allowable angle for a continuous trajectory.
	 * @return The trajectories after split.
	 */
	private List<Trajectory> splitTrajectory(Trajectory inputTraj, double angleChangeThresh, DistanceFunction df) {
		List<Trajectory> trajInfoResult = new ArrayList<>();
		int trajCount = 0;
		Trajectory currTraj = new Trajectory(trajCount + "", df);
		boolean hasPrecedingPoints = false; // the next point is the first point of this trajectory
		if (inputTraj.size() <= 1)
			return new ArrayList<>();
		for (int i = 0; i < inputTraj.size() - 1; i++) {
			if (!hasPrecedingPoints) {
				double angleChangeStart = Math.abs(inputTraj.get(i).heading() - inputTraj.getSegment(i).getHeading());    // angle change
				// between left endpoint and segment
				angleChangeStart = angleChangeStart > 180 ? 360 - angleChangeStart : angleChangeStart;
				if (angleChangeStart < angleChangeThresh) {
					double angleChangeEnd = Math.abs(inputTraj.getSegment(i).getHeading() - inputTraj.get(i + 1).heading());  // angle change
					angleChangeEnd = angleChangeEnd > 180 ? 360 - angleChangeEnd : angleChangeEnd;
					// between segment and right endpoint
					if (angleChangeEnd < angleChangeThresh) {   // at least two points can be inserted into new trajectory
						currTraj.add(inputTraj.get(i));
						currTraj.add(inputTraj.get(i + 1));
						hasPrecedingPoints = true;
					}
					// else, the second point does not follow the same direction, abandon the first point
				}   // else, the first point does not have the same direction as the following segment, abandon the first point
			} else {
				double angleChangeStart = Math.abs(inputTraj.get(i).heading() - inputTraj.getSegment(i).getHeading());    // angle change
				// between left endpoint and segment
				angleChangeStart = angleChangeStart > 180 ? 360 - angleChangeStart : angleChangeStart;
				double angleChangeEnd = Math.abs(inputTraj.getSegment(i).getHeading() - inputTraj.get(i + 1).heading());  // angle change
				// between segment and right endpoint
				angleChangeEnd = angleChangeEnd > 180 ? 360 - angleChangeEnd : angleChangeEnd;
				if (angleChangeStart < angleChangeThresh && angleChangeEnd < angleChangeThresh) {   // the trajectory continues
					currTraj.add(inputTraj.get(i + 1));
				} else {    // either case, the trajectory splits at the current segment
					trajInfoResult.add(currTraj);
					trajCount++;
					currTraj = new Trajectory(trajCount + "", df);
					hasPrecedingPoints = false;
				}
			}
		}
		return trajInfoResult;
	}
	
	/**
	 * Cluster the input trajectories according to their trajectory-wise distance.
	 *
	 * @param unmatchedTraj Input trajectories.
	 * @param df            The distance measurement between points.
	 * @param distThresh    The maximum allowable distance between trajectories within one cluster.
	 * @return A list of clusters containing all trajectories.
	 */
	private List<Cluster> basicUnmatchedClustering(List<Triplet<Trajectory, String, String>> unmatchedTraj, DistanceFunction df, double distThresh) {
		List<Cluster> resultClusterList = new ArrayList<>();
		HausdorffDistanceFunction distFunc = new HausdorffDistanceFunction(df);
		int clusterCount = 0;
		for (Triplet<Trajectory, String, String> trajInfo : unmatchedTraj) {
			Cluster mergedCluster = null;   // the current trajectory is merged to one of the existing cluster, null = not merged to any
			Iterator<Cluster> it = resultClusterList.iterator();
			Trajectory traj = trajInfo._1();
			while (it.hasNext()) {
				Cluster currCluster = it.next();
				if (currCluster.getDistance(traj, distFunc) < distThresh) {  // current trajectory is going to be merged
					if (mergedCluster == null) {  // not yet merged to any cluster, merge to the current one
						currCluster.add(traj);
						currCluster.addStartAnchor(trajInfo._2());
						currCluster.addEndAnchor(trajInfo._3());
						mergedCluster = currCluster;
					} else {
						mergedCluster.merge(currCluster);
						it.remove();
					}
				}
			}
			if (mergedCluster == null) {  // no existing cluster is close to the current trajectory, create a new cluster
				Cluster createCluster = new Cluster(clusterCount + "", traj);
				createCluster.addStartAnchor(trajInfo._2());
				createCluster.addEndAnchor(trajInfo._3());
				resultClusterList.add(createCluster);
				clusterCount++;
			}
		}
		int trajCount = 0;
		for (Cluster cluster : resultClusterList)
			trajCount += cluster.size();
		if (trajCount != unmatchedTraj.size())
			LOG.info("The total number of clustered trajectory is not equivalent to the input: " + trajCount + ", " + unmatchedTraj.size());
		LOG.info("Basic clustering finished. Total number of clusters: " + resultClusterList.size() + ".");
		return resultClusterList;
	}
	
	/**
	 * Cluster the input trajectories according to their trajectory-wise distance.
	 *
	 * @param trajList   Input trajectories.
	 * @param df         The distance measurement between points.
	 * @param distThresh The maximum allowable distance between trajectories within one cluster.
	 * @return A list of clusters containing all trajectories.
	 */
	private List<Cluster> basicClustering(List<Trajectory> trajList, DistanceFunction df, double distThresh) {
		List<Cluster> resultClusterList = new ArrayList<>();
		HausdorffDistanceFunction distFunc = new HausdorffDistanceFunction(df);
		int clusterCount = 0;
		for (Trajectory traj : trajList) {
			Cluster mergedCluster = null;   // the current trajectory is merged to one of the existing cluster, null = not merged to any
			Iterator<Cluster> it = resultClusterList.iterator();
			while (it.hasNext()) {
				Cluster currCluster = it.next();
				if (currCluster.getDistance(traj, distFunc) < distThresh) {  // current trajectory is going to be merged
					if (mergedCluster == null) {  // not yet merged to any cluster, merge to the current one
						currCluster.add(traj);
						mergedCluster = currCluster;
					} else {
						mergedCluster.merge(currCluster);
						it.remove();
					}
				}
			}
			if (mergedCluster == null) {  // no existing cluster is close to the current trajectory, create a new cluster
				Cluster createCluster = new Cluster(clusterCount + "", traj);
				resultClusterList.add(createCluster);
				clusterCount++;
			}
		}
		int trajCount = 0;
		for (Cluster cluster : resultClusterList)
			trajCount += cluster.size();
		if (trajCount != trajList.size())
			LOG.info("The total number of clustered trajectory is not equivalent to the input: " + trajCount + ", " + trajList.size());
		LOG.info("Basic clustering finished. Total number of clusters: " + resultClusterList.size() + ".");
		return resultClusterList;
	}
	
	public List<RoadWay> roadWayUpdateProcess(List<Triplet<Trajectory, String, String>> unmatchedTrajInfo, HashMap<String, Pair<HashSet<String>,
			HashSet<String>>> newRoadID2AnchorPoints, BaseProperty prop, DistanceFunction distFunc) throws InterruptedException {
		double maxAngleChange = prop.getPropertyDouble("algorithm.mapinference.lineclustering.MaximumAngleChangeDegree");
		double maxClusteringDist = prop.getPropertyDouble("algorithm.mapinference.lineclustering.MaximumClusteringDistance");
		double dpEpsilon = prop.getPropertyDouble("algorithm.mapinference.lineclustering.DPEpsilon");
		
		List<Triplet<Trajectory, String, String>> filteredTrajList = new ArrayList<>();
		for (Triplet<Trajectory, String, String> trajInfo : unmatchedTrajInfo) {
			Trajectory traj = trajInfo._1();
			List<Trajectory> trajList = splitTrajectory(traj, maxAngleChange, distFunc);
			for (Trajectory currTraj : trajList) {
				if (currTraj.size() > 0) {
					filteredTrajList.add(new Triplet<>(currTraj, trajInfo._2(), trajInfo._3()));
				}
			}
		}
		List<Cluster> initialClusterList = basicUnmatchedClustering(filteredTrajList, distFunc, maxClusteringDist);
		
		List<RoadWay> outputRoadWay = new ArrayList<>();
		DouglasPeuckerFilter dpFilter = new DouglasPeuckerFilter(dpEpsilon, distFunc);
		PrincipalCurveGenerator principalCurveGen = new PrincipalCurveGenerator(distFunc);
		for (Cluster cluster : initialClusterList) {
			if (cluster.size() == 1) {
				List<RoadNode> currNodeList = new ArrayList<>();
				Trajectory traj = cluster.getTraj(0);
				for (int i = 0; i < traj.size(); i++) {
					Point p = traj.get(i);
					currNodeList.add(new RoadNode(i + "", p.x(), p.y(), distFunc));
				}
				newRoadID2AnchorPoints.put(cluster.getId(), new Pair<>(cluster.getStartAnchorPoints(), cluster.getEndAnchorPoints()));
				if (currNodeList.size() > 0) {
					RoadNode prevNode = currNodeList.get(0);
					boolean isProblematicRoad = false;
					for (int i = 1; i < currNodeList.size(); i++) {
						if (currNodeList.get(i).toPoint().equals2D(prevNode.toPoint())) {
							LOG.debug("map inference result contains duplicate points.");
							isProblematicRoad = true;
						}
						prevNode = currNodeList.get(i);
					}
					if (!isProblematicRoad) {
						RoadWay currWay = new RoadWay(cluster.getId(), currNodeList, distFunc);
						currWay.setNewRoad(true);
						currWay.setConfidenceScore(1);
						outputRoadWay.add(dpFilter.dpSimplifier(currWay));
						continue;
					} else
						continue;
				}
			}
			try {
//                System.out.println("Start " + cluster.getID() + " generation which has " + cluster.getTrajectoryList().size() + " " +
//                        "trajectories.");
				RoadWay inferredRoad = principalCurveGen.startPrincipalCurveGen(cluster);
				newRoadID2AnchorPoints.put(cluster.getId(), new Pair<>(cluster.getStartAnchorPoints(), cluster.getEndAnchorPoints()));
				outputRoadWay.add(dpFilter.dpSimplifier(inferredRoad));
			} catch (IllegalStateException | IndexOutOfBoundsException e) {
//                e.printStackTrace();
//                LOG.warning("WARNING! Ignore cluster " + cluster.getID() + " due to principal curve generation failure.");
			}
		}
		return outputRoadWay;
	}
	
	// TODO work on this implementation
	public RoadNetworkGraph mapInferenceProcess(List<Trajectory> inputTrajList, BaseProperty prop) throws InterruptedException {
		double maxAngleChange = prop.getPropertyDouble("algorithm.mapinference.lineclustering.MaximumAngleChangeDegree");
		double maxClusteringDist = prop.getPropertyDouble("algorithm.mapinference.lineclustering.MaximumClusteringDistance");
		double dpEpsilon = prop.getPropertyDouble("algorithm.mapinference.lineclustering.DPEpsilon");
		DistanceFunction distFunc = inputTrajList.get(0).getDistanceFunction();
		List<Trajectory> trajList = new ArrayList<>();
		for (Trajectory traj : inputTrajList) {
			trajList.addAll(splitTrajectory(traj, maxAngleChange, distFunc));
		}
		List<Cluster> initialClusterList = basicClustering(trajList, distFunc, maxClusteringDist);
		List<RoadWay> outputRoadWay = new ArrayList<>();
		DouglasPeuckerFilter dpFilter = new DouglasPeuckerFilter(dpEpsilon, distFunc);
		PrincipalCurveGenerator principalCurveGen = new PrincipalCurveGenerator(distFunc);
		for (Cluster cluster : initialClusterList) {
			if (cluster.size() == 1) {
				List<RoadNode> currNodeList = new ArrayList<>();
				Trajectory traj = cluster.getTraj(0);
				for (int i = 0; i < traj.size(); i++) {
					Point p = traj.get(i);
					currNodeList.add(new RoadNode(i + "", p.x(), p.y(), distFunc));
				}
				if (currNodeList.size() < 2 || currNodeList.get(0).toPoint().equals2D(currNodeList.get(currNodeList.size() - 1).toPoint()))
					continue;
				RoadWay currWay = new RoadWay(cluster.getId(), currNodeList, distFunc);
				currWay.setConfidenceScore(1);
				outputRoadWay.add(dpFilter.dpSimplifier(currWay));
				continue;
			}
			try {
//                System.out.println("Start " + cluster.getID() + " generation which has " + cluster.getTrajectoryList().size() + " " +
//                        "trajectories.");
				RoadWay inferredRoad = principalCurveGen.startPrincipalCurveGen(cluster);
				outputRoadWay.add(dpFilter.dpSimplifier(inferredRoad));
			} catch (IllegalStateException | IndexOutOfBoundsException e) {
				e.printStackTrace();
				LOG.warn("WARNING! Ignore cluster " + cluster.getId() + " due to principal curve generation failure.");
			}
		}
		
		return convert2Map(outputRoadWay);
	}
	
	private RoadNetworkGraph convert2Map(List<RoadWay> outputRoadWay) {
		DistanceFunction distFunc = outputRoadWay.get(0).getDistanceFunction();
		Map<String, RoadNode> location2NodeMap = new LinkedHashMap<>();
		int nodeCount = 0;
		for (RoadWay currWay : outputRoadWay) {
			List<RoadNode> replaceNodeList = new ArrayList<>();
			String startLocation = currWay.getFromNode().lon() + "_" + currWay.getFromNode().lat();
			if (location2NodeMap.containsKey(startLocation)) {    // the intersection already exists
				replaceNodeList.add(location2NodeMap.get(startLocation));
			} else {
				replaceNodeList.add(currWay.getFromNode());
				currWay.getFromNode().setId(nodeCount + "");
				nodeCount++;
				location2NodeMap.put(startLocation, currWay.getFromNode());
			}
			replaceNodeList.addAll(currWay.getNodes().subList(1, currWay.size() - 1));
			String endLocation = currWay.getToNode().lon() + "_" + currWay.getToNode().lat();
			if (location2NodeMap.containsKey(endLocation)) {    // the intersection already exists
				replaceNodeList.add(location2NodeMap.get(endLocation));
			} else {
				replaceNodeList.add(currWay.getToNode());
				currWay.getToNode().setId(nodeCount + "");
				nodeCount++;
				location2NodeMap.put(startLocation, currWay.getToNode());
			}
			currWay.setNodes(replaceNodeList);
		}
		List<RoadNode> currNodeList = new ArrayList<>();
		for (Map.Entry<String, RoadNode> entry : location2NodeMap.entrySet()) {
			currNodeList.add(entry.getValue());
		}
		RoadNetworkGraph resultMap = new RoadNetworkGraph(false, distFunc);
		resultMap.addNodes(currNodeList);
		resultMap.addWays(outputRoadWay);
		return resultMap;
	}
}