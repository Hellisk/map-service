package traminer.util.trajectory.distance;

import traminer.util.spatial.objects.st.STPoint;
import traminer.util.trajectory.Trajectory;

import java.util.List;

/**
 * LCSS: Largest Common Subsequence distance.
 * 
 * </br> Spatial similarity.
 * </br> Robust to different sampling rates.
 * </br> Robust to noise.
 * </br> Robust to stretching and translation.
 * </br> Not a metric.
 * 
 * @author uqhsu1, Haozhou, uqdalves
 */
@SuppressWarnings("serial")
public class LCSSDistanceCalculator extends TrajectoryDistanceFunction {
    private long time_threshold;
    private double distance_threshold;
    private double[][] costMatrix;

    /**
     * Set default distance and time thresholds:
     * <br> Matching distance threshold = 0.001
     * <br> Time threshold = 1
     */
    public LCSSDistanceCalculator() {
        distance_threshold = 0.001;
        time_threshold = 1;
    }

    /**
     * Set distance and time thresholds.
     *
     * @param distance_threshold Distance match threshold (x and y distance).
     * @param time_threshold     Controls how far in time we can go in order to
     *                           match a point from one trajectory to a point in another trajectory.
     */
    public LCSSDistanceCalculator(double distance_threshold, long time_threshold) {
        this.distance_threshold = distance_threshold;
        this.time_threshold = time_threshold;
    }

    @Override
    public double getDistance(final Trajectory t1, final Trajectory t2) {
        // make sure the original trajectories are unchanged
        return LCSS(t1.clone(), t2.clone());
    }

    /**
     * LCSS dynamic.
     */
    protected double LCSS(List<STPoint> t1, List<STPoint> t2) {
        // current iteration
        int size_t1 = t1.size();
        int size_t2 = t2.size();

        // initialize dynamic matrix
        costMatrix = new double[size_t1 + 1][size_t2 + 1];
        for (int i = 0; i <= size_t1; i++) {
            costMatrix[i][0] = 0;
        }
        for (int i = 0; i <= size_t2; i++) {
            costMatrix[0][i] = 0;
        }

        // dynamic LCSS calculation
        double cost_a, cost_b;
        for (int i = 1; i <= size_t1; i++) {
            STPoint pi = t1.get(i - 1);
            for (int j = 1; j <= size_t2; j++) {
                STPoint pj = t2.get(j - 1);
                if (match(pi, pj)) {
                    costMatrix[i][j] = costMatrix[i - 1][j - 1] + 1;
                } else {
                    cost_a = costMatrix[i - 1][j];
                    cost_b = costMatrix[i][j - 1];
                    costMatrix[i][j] = Math.max(cost_a, cost_b);
                }
            }
        }

        return costMatrix[size_t1][size_t2];
    }

    /**
     * Check the distance and time cost between
     * these two points against the thresholds.
     */
    public boolean match(STPoint p1, STPoint p2) {
        double cost_x = Math.abs(p1.x() - p2.x());
        double cost_y = Math.abs(p1.y() - p2.y());
        long cost_t = Math.abs(p1.time() - p2.time());
        return cost_x < distance_threshold &&
                cost_y < distance_threshold &&
                cost_t <= time_threshold;
    }

}
