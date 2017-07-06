package traminer.util.trajectory.distance;

import traminer.util.spatial.objects.st.STPoint;
import traminer.util.trajectory.Trajectory;

import java.util.List;

/**
 * STLCSS: Spatial-Temporal Largest Common Subsequence distance.
 * 
 * </br> Spatial-temporal similarity.
 * </br> Robust to different sampling rates.
 * </br> Robust to noise.
 * </br> Robust to stretching and translation.
 * </br> Not a metric.
 * 
 * @author HanSu, Haozhou, uqdalves
 */
@SuppressWarnings("serial")
public class STLCSSDistanceCalculator extends TrajectoryDistanceFunction {
    private double distance_threshold;
    private double time_threshold;
    private long startTime1;
    private long startTime2;

    /**
     * Set default distance and time thresholds:
     * <br> Matching distance threshold = 0.001
     * <br> Time threshold = earliest last point
     */
    public STLCSSDistanceCalculator() {
        this.distance_threshold = 0.001;
        this.time_threshold = 0.0;
    }

    /**
     * Set distance and time thresholds.
     *
     * @param distance_threshold Distance match threshold (x and y distance).
     * @param time_threshold     Controls how far in time we can go in order to
     *                           match a point from one trajectory to a point in another trajectory.
     */
    public STLCSSDistanceCalculator(double distance_threshold, long time_threshold) {
        this.distance_threshold = distance_threshold;
        this.time_threshold = time_threshold;
    }

    @Override
    public double getDistance(final Trajectory t1, final Trajectory t2) {
        // make sure the original trajectories are unchanged
        List<STPoint> r_clone = t1.clone();
        List<STPoint> s_clone = t2.clone();

        // get earliest last point
        if (time_threshold == 0.0) {
            time_threshold = getTimeEnd(r_clone, s_clone);
        }
        startTime1 = r_clone.get(0).time();
        startTime2 = s_clone.get(0).time();

        return STLCSS(r_clone, s_clone);
    }

    protected double STLCSS(List<STPoint> r, List<STPoint> s) {
        double[][] LCSSMetric = new double[r.size() + 1][s.size() + 1];
        for (int i = 0; i <= r.size(); i++) {
            LCSSMetric[i][0] = 0;
        }
        for (int i = 0; i <= s.size(); i++) {
            LCSSMetric[0][i] = 0;
        }
        LCSSMetric[0][0] = 0;

        for (int i = 1; i <= r.size(); i++) {
            for (int j = 1; j <= s.size(); j++) {
                if (subcost(r.get(i - 1), s.get(j - 1)) == 1) {
                    LCSSMetric[i][j] = LCSSMetric[i - 1][j - 1] + 1;
                } else {
                    LCSSMetric[i][j] = Math.max(LCSSMetric[i][j - 1],
                            LCSSMetric[i - 1][j]);
                }

            }
        }
        double lcss = LCSSMetric[r.size()][s.size()];
        double distanceV = 1 - (lcss / Math.min(r.size(), s.size()));

        return distanceV;
    }

    private int subcost(STPoint p1, STPoint p2) {
        boolean isSame = true;
        if (Math.abs(p1.x() - p2.x()) > distance_threshold) {
            isSame = false;
        }
        if (Math.abs(p1.y() - p2.y()) > distance_threshold) {
            isSame = false;
        }
        if (Math.abs((p1.time() - startTime1) - (p2.time() - startTime2))
                > time_threshold) {
            isSame = false;
        }

        if (isSame) {
            return 1;
        }
        return 0;
    }

    /**
     * Get final time period tn
     */
    private long getTimeEnd(List<STPoint> r, List<STPoint> s) {
        // Get the trajectory with earliest last point
        long tn = s.get(s.size() - 1).time() < r.get(r.size() - 1).time() ?
                s.get(s.size() - 1).time() : r.get(r.size() - 1).time();
        return tn;
    }
}