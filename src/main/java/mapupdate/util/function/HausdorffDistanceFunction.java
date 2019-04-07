package mapupdate.util.function;

import mapupdate.util.object.spatialobject.Trajectory;
import mapupdate.util.object.spatialobject.TrajectoryPoint;

public class HausdorffDistanceFunction {
    private PointDistanceFunction distFunc;

    public HausdorffDistanceFunction(PointDistanceFunction distFunc) {
        this.distFunc = distFunc;
    }

    public double distance(Trajectory traj1, Trajectory traj2) {
        return Math.max(maxMinDistance(traj1, traj2), maxMinDistance(traj2, traj1));
    }

    // TODO: Optimize the performance if needed.
    private double maxMinDistance(Trajectory traj1, Trajectory traj2) {
        double maxDistance = 0;
        for (TrajectoryPoint aTraj1 : traj1) {
            double minDistance = Double.POSITIVE_INFINITY;
            for (TrajectoryPoint aTraj2 : traj2) {
                double currDistance = distFunc.distance(aTraj1, aTraj2);
                minDistance = minDistance > currDistance ? currDistance : minDistance;
            }
            maxDistance = maxDistance < minDistance ? minDistance : maxDistance;
        }
        return maxDistance;
    }
}
