package traminer.util.trajectory.distance;

import traminer.util.spatial.objects.Point;
import traminer.util.spatial.objects.st.STPoint;
import traminer.util.trajectory.Trajectory;

import java.util.ArrayList;
import java.util.List;

/**
 * DISSIM: Dissimilarity distance measure.
 * 
 * </br> Uniform and non-uniform sampling rates.
 * </br> Spatial and temporal dimensions.
 * </br> Discrete and continuous trajectories (uses integrate).
 * </br> Not robust for trajectories with different speeds.
 * </br> Not a metric.
 * </br> Euclidean space only.
 * 
 * @author uqdalves
 */
@SuppressWarnings("serial")
public class DISSIMDistanceCalculator extends TrajectoryDistanceFunction {
    private long TIME_INCR = 1;

    /**
     * Set time increment as default = 1 unit of time.
     */
    public DISSIMDistanceCalculator() {
    }

    /**
     * @param increment Time increment in the integrate calculation.
     */
    public DISSIMDistanceCalculator(int increment) {
        this.TIME_INCR = increment;
    }

    @Override
    public double getDistance(final Trajectory t1, final Trajectory t2) {
        // Time range (parameters - given)
        long t0 = getTimeIni(t1, t2);
        long tn = getTimeEnd(t1, t2);

        // make sure the original trajectories are unchanged
        return DISSIM(t1.clone(), t2.clone(), t0, tn);
    }

    /**
     * Distance within the given time interval [t0, tn].
     */
    public double getDistance(final Trajectory t1, final Trajectory t2, long t0, long tn) {
        // make sure the original trajectories are unchanged
        return DISSIM(t1.clone(), t2.clone(), t0, tn);
    }

    /**
     * Dissimilarity distance between two moving objects.
     */
    protected double DISSIM(List<STPoint> r, List<STPoint> s, long t0, long tn) {
        if (r.size() < 2 || s.size() < 2) {
            return INFINITY;
        }

        List<Double> dist_t = new ArrayList<Double>();
        int index_r = 0, index_s = 0;
        for (long t = t0; t <= tn; t += TIME_INCR) {
            STPoint r_p1 = r.get(index_r);
            STPoint r_p2 = r.get(index_r + 1);
            STPoint s_p1 = s.get(index_s);
            STPoint s_p2 = s.get(index_s + 1);

            // get the 'distance' between the two trajectories at time t
            dist_t.add(getDistance(r_p1, r_p2, s_p1, s_p2, t));

            // current time reached the time of the next point
            if (t > r_p2.time() && index_r < r.size() - 2) {
                index_r++;
            }
            if (t > s_p2.time() && index_s < s.size() - 2) {
                index_s++;
            }
        }

        double dissim = 0;
        for (int i = 0; i < dist_t.size() - 1; i++) {
            dissim += (dist_t.get(i) + dist_t.get(i + 1)) * TIME_INCR;
        }

        return dissim / 2;
    }

    /**
     * Euclidean distance between two points moving with linear
     * functions of time between consecutive time-stamps. Using
     * factors of the trinomials a, b and c
     */
    private double getDistance(STPoint r_p1, STPoint r_p2, STPoint s_p1, STPoint s_p2, long time) {
        // get the factors
        double a = getA(r_p1, r_p2, s_p1, s_p2);
        double b = getB(r_p1, r_p2, s_p1, s_p2);
        double c = getC(r_p1, r_p2, s_p1, s_p2);

        double dist = Math.sqrt(a * Math.pow(time, 2) + b * time + c);

        return dist;
    }

    /**
     * Get initial time period t1
     */
    private long getTimeIni(List<STPoint> r, List<STPoint> s) {
        // Get the trajectory with latest first point
        long t1 = s.get(0).time() > r.get(0).time() ?
                s.get(0).time() : r.get(0).time();
        return t1;
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

    /**
     * Calculate the factors of the trinomial A, B and C
     */
    private double getA(STPoint p_p1, STPoint p_p2, STPoint q_p1, STPoint q_p2) {
        double a1, a2;
        a1 = q_p2.x() - q_p1.x() -
                p_p2.x() + p_p1.x();
        a2 = q_p2.y() - q_p1.y() -
                p_p2.y() + p_p1.y();

        double a = Math.pow(a1, 2) + Math.pow(a2, 2);

        return a;
    }

    private double getB(Point p_p1, Point p_p2, Point q_p1, Point q_p2) {
        double b1, b2, b3, b4;
        b1 = q_p2.x() - q_p1.x() -
                p_p2.x() + p_p1.x();
        b2 = q_p1.x() - p_p1.x();
        b3 = q_p2.y() - q_p1.y() -
                p_p2.y() + p_p1.y();
        b4 = q_p1.y() - p_p1.y();

        double b = 2 * (b1 * b2 + b3 * b4);

        return b;
    }

    private double getC(Point p_p1, Point p_p2, Point q_p1, Point q_p2) {
        double c1, c2;
        c1 = q_p1.x() - p_p1.x();
        c2 = q_p1.y() - p_p1.y();

        double c = Math.pow(c1, 2) + Math.pow(c2, 2);

        return c;
    }
}
