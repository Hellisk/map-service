package mapupdate.mapinference.trajectoryclustering;

import mapupdate.mapinference.trajectoryclustering.pcurves.PrincipalCurveGenerator;
import mapupdate.util.function.GreatCircleDistanceFunction;
import mapupdate.util.function.HausdorffDistanceFunction;
import mapupdate.util.function.PointDistanceFunction;
import mapupdate.util.object.datastructure.Pair;
import mapupdate.util.object.datastructure.Triplet;
import mapupdate.util.object.roadnetwork.RoadNode;
import mapupdate.util.object.roadnetwork.RoadWay;
import mapupdate.util.object.spatialobject.Point;
import mapupdate.util.object.spatialobject.Trajectory;

import java.util.*;

import static mapupdate.Main.DP_EPSILON;
import static mapupdate.Main.LOGGER;

public class TrajectoryClusteringMapInference {
    /**
     * Split each unmatched trajectory according to its direction continuity.
     *
     * @param unmatchedTrajList The input unmatched trajectory list.
     * @param angleChangeThresh The maximum allowable angle for a continuous trajectory.
     * @return The trajectories after split.
     */
    private List<Triplet<Trajectory, String, String>> splitTrajectory(List<Triplet<Trajectory, String, String>> unmatchedTrajList,
                                                                      double angleChangeThresh) {
        List<Triplet<Trajectory, String, String>> trajInfoResult = new ArrayList<>();
        int trajCount = 0;
        for (Triplet<Trajectory, String, String> trajInfo : unmatchedTrajList) {
            Trajectory traj = trajInfo._1();
            Trajectory currTraj = new Trajectory(trajCount + "");
            boolean hasPrecedingPoints = false; // the next point is the first point of this trajectory
            if (traj.size() <= 1)
                continue;
            for (int i = 0; i < traj.size() - 1; i++) {
                if (!hasPrecedingPoints) {
                    double angleChangeStart = Math.abs(traj.get(i).heading() - traj.getSegment(i).getHeading());    // angle change
                    // between left endpoint and segment
                    angleChangeStart = angleChangeStart > 180 ? 360 - angleChangeStart : angleChangeStart;
                    if (angleChangeStart < angleChangeThresh) {
                        double angleChangeEnd = Math.abs(traj.getSegment(i).getHeading() - traj.get(i + 1).heading());  // angle change
                        angleChangeEnd = angleChangeEnd > 180 ? 360 - angleChangeEnd : angleChangeEnd;
                        // between segment and right endpoint
                        if (angleChangeEnd < angleChangeThresh) {   // at least two points can be inserted into new trajectory
                            currTraj.add(traj.get(i));
                            currTraj.add(traj.get(i + 1));
                            hasPrecedingPoints = true;
                        }
                        // else, the second point does not follow the same direction, abandon the first point
                    }   // else, the first point does not have the same direction as the following segment, abandon the first point
                } else {
                    double angleChangeStart = Math.abs(traj.get(i).heading() - traj.getSegment(i).getHeading());    // angle change
                    // between left endpoint and segment
                    angleChangeStart = angleChangeStart > 180 ? 360 - angleChangeStart : angleChangeStart;
                    double angleChangeEnd = Math.abs(traj.getSegment(i).getHeading() - traj.get(i + 1).heading());  // angle change
                    // between segment and right endpoint
                    angleChangeEnd = angleChangeEnd > 180 ? 360 - angleChangeEnd : angleChangeEnd;
                    if (angleChangeStart < angleChangeThresh && angleChangeEnd < angleChangeThresh) {   // the trajectory continues
                        currTraj.add(traj.get(i + 1));
                    } else {    // either case, the trajectory splits at the current segment
                        trajInfoResult.add(new Triplet<>(currTraj, trajInfo._2(), trajInfo._3()));
                        trajCount++;
                        currTraj = new Trajectory(trajCount + "");
                        hasPrecedingPoints = false;
                    }
                }
            }
            if (currTraj.size() > 0) {
                trajInfoResult.add(new Triplet<>(currTraj, trajInfo._2(), trajInfo._3()));
                trajCount++;
            }
        }
        LOGGER.info(unmatchedTrajList.size() + " unmatched trajectories are split into " + trajInfoResult.size() + " trajectories.");
        return trajInfoResult;
    }

    /**
     * Cluster the input trajectories according to their trajectory-wise distance.
     *
     * @param unmatchedTraj Input trajectories.
     * @param pointDistFunc The distance measurement between points.
     * @param distThresh    The maximum allowable distance between trajectories within one cluster.
     * @return A list of clusters containing all trajectories.
     */
    private List<Cluster> basicClustering(List<Triplet<Trajectory, String, String>> unmatchedTraj, PointDistanceFunction pointDistFunc, double distThresh) {
        List<Cluster> resultClusterList = new ArrayList<>();
        HausdorffDistanceFunction distFunc = new HausdorffDistanceFunction(pointDistFunc);
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
            LOGGER.info("ERROR! The total number of clustered trajectory is not equivalent to the input: " + trajCount + ", " + unmatchedTraj.size());
        LOGGER.info("Basic clustering finished. Total number of clusters: " + resultClusterList.size() + ".");
        return resultClusterList;
    }


    public List<RoadWay> startMapInferenceProcess(List<Triplet<Trajectory, String, String>> unmatchedTrajInfo, HashMap<String, Pair<HashSet<String>,
            HashSet<String>>> newRoadID2AnchorPoints, double angleChangeThresh, double distThresh) throws InterruptedException {
        List<Triplet<Trajectory, String, String>> filteredTrajList = splitTrajectory(unmatchedTrajInfo, angleChangeThresh);
        PointDistanceFunction distFunc = new GreatCircleDistanceFunction();
        List<Cluster> initialClusterList = basicClustering(filteredTrajList, distFunc, distThresh);

        List<RoadWay> outputRoadWay = new ArrayList<>();
        DouglasPeuckerFilter dpFilter = new DouglasPeuckerFilter(DP_EPSILON);
        PrincipalCurveGenerator principalCurveGen = new PrincipalCurveGenerator();
        for (Cluster cluster : initialClusterList) {
            if (cluster.size() == 1) {
                List<RoadNode> currNodeList = new ArrayList<>();
                Trajectory traj = cluster.getTraj(0);
                for (int i = 0; i < traj.size(); i++) {
                    Point p = traj.get(i);
                    currNodeList.add(new RoadNode(i + "", p.x(), p.y()));
                }
                newRoadID2AnchorPoints.put(cluster.getId(), new Pair<>(cluster.getStartAnchorPoints(), cluster.getEndAnchorPoints()));
                RoadWay currWay = new RoadWay(cluster.getId(), currNodeList);
                currWay.setNewRoad(true);
                currWay.setConfidenceScore(1);
                outputRoadWay.add(dpFilter.dpSimplifier(currWay));
                continue;
            }
            try {
//                System.out.println("Start " + cluster.getID() + " generation which has " + cluster.getTrajectoryList().size() + " " +
//                        "trajectories.");
                RoadWay inferredRoad = principalCurveGen.startPrincipalCurveGen(cluster);
                newRoadID2AnchorPoints.put(cluster.getId(), new Pair<>(cluster.getStartAnchorPoints(), cluster.getEndAnchorPoints()));
                outputRoadWay.add(dpFilter.dpSimplifier(inferredRoad));
            } catch (IllegalStateException | IndexOutOfBoundsException e) {
//                e.printStackTrace();
//                LOGGER.warning("WARNING! Ignore cluster " + cluster.getID() + " due to principal curve generation failure.");
            }
        }
        return outputRoadWay;
    }
}