package traminer.util.trajectory.distance;

import traminer.util.spatial.distance.PointDistanceFunction;
import traminer.util.spatial.objects.st.STPoint;
import traminer.util.trajectory.Trajectory;

import java.util.ArrayList;
import java.util.List;

/**
 * EDC: Euclidean Distance for 2D Point Series (Trajectories).
 * <p>
 * </br> Uniform sampling rates only.
 * </br> Spatial dimension only.
 * </br> ??Cope with local time shift.
 * </br> ??Not sensitive to noise.
 * </br> ??Not a metric.
 *
 * @author uqdalves
 */
@SuppressWarnings("serial")
public class EDCDistanceCalculator extends TrajectoryDistanceFunction {
    /**
     * @param dist The points distance measure to use.
     */
    public EDCDistanceCalculator(PointDistanceFunction dist) {
        super(dist);
    }

    @Override
    public double getDistance(final Trajectory t1, final Trajectory t2) {
        // make sure the original trajectories are unchanged
        return EDC(t1.clone(), t2.clone());
    }

    /**
     * EDC calculation, based on the shortest trajectory.
     */
    protected double EDC(List<STPoint> t1, List<STPoint> t2) {
        if (t1.size() == 0 && t2.size() == 0) {
            return 0;
        }
        if (t1.size() == 0 || t2.size() == 0) {
            return INFINITY;
        }

        // get the shortest and longest trajectories
        List<STPoint> longest = new ArrayList<STPoint>();
        List<STPoint> shortest = new ArrayList<STPoint>();
        if (t1.size() < t2.size()) {
            shortest = t1;
            longest = t2;
        } else {
            shortest = t2;
            longest = t1;
        }

        int n = shortest.size();
        double dist = 0.0;
        for (int i = 0; i < n; i++) {
            STPoint p1 = shortest.get(i);
            STPoint p2 = longest.get(i);
            dist += p1.distance(p2, distFunc);
        }
        dist = dist / n;

        return dist;
    }

    public void print() {
        System.out.println("EuclideanDistanceCalculator.");
    }
}
