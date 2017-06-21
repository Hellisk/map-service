package traminer.util.trajectory.distance;

import traminer.util.spatial.distance.PointDistanceFunction;
import traminer.util.spatial.objects.st.STPoint;
import traminer.util.trajectory.Trajectory;

import java.util.List;

/**
 * DTW: Dynamic Time Warping for time series.
 * <p>
 * </br> Uniform sampling rates only.
 * </br> Spatial dimension only.
 * </br> Robust to local time shift.
 * </br> Sensitive to noise.
 * </br> Not a metric.
 *
 * @author uqhsu1, Haozhou, uqdalves
 */
@SuppressWarnings("serial")
public class DTWDistanceCalculator extends TrajectoryDistanceFunction {
    private double[][] costMatrix;

    /**
     * @param dist The points distance measure to use.
     */
    public DTWDistanceCalculator(PointDistanceFunction dist) {
        super(dist);
    }

    @Override
    public double getDistance(final Trajectory t1, final Trajectory t2) {
        // make sure the original trajectories are unchanged
        return DTW(t1.clone(), t2.clone());
    }

    /**
     * DTW dynamic.
     */
    protected double DTW(List<STPoint> t1, List<STPoint> t2) {
        // current iteration
        int size_t1 = t1.size();
        int size_t2 = t2.size();

        // initialize dynamic matrix
        costMatrix = new double[size_t1 + 1][size_t2 + 1];
        costMatrix[0][0] = 0.0;
        for (int i = 1; i <= size_t1; i++) {
            costMatrix[i][0] = INFINITY;
        }
        for (int i = 1; i <= size_t2; i++) {
            costMatrix[0][i] = INFINITY;
        }

        // dynamic DTW calculation
        double dist, cost_a, cost_b, cost_c;
        for (int i = 1; i <= size_t1; i++) {
            for (int j = 1; j <= size_t2; j++) {
                dist = t1.get(i - 1).distance(t2.get(j - 1), distFunc);
                cost_a = costMatrix[i - 1][j - 1];
                cost_b = costMatrix[i - 1][j];
                cost_c = costMatrix[i][j - 1];
                costMatrix[i][j] = dist + min(cost_a, cost_b, cost_c);
            }
        }

        return costMatrix[size_t1][size_t2];
    }

    /**
     * Minimum of a, b, c.
     */
    private double min(double a, double b, double c) {
        if (a <= b && a <= c) {
            return a;
        }
        if (b <= c) {
            return b;
        }
        return c;
    }

}
