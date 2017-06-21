package traminer.util.trajectory.distance;

import traminer.util.spatial.distance.PointDistanceFunction;
import traminer.util.spatial.objects.st.STPoint;
import traminer.util.trajectory.Trajectory;

import java.util.ArrayList;
import java.util.List;

/**
 * PDTW: Trajectory distance measure
 *
 * @author uqhsu1, Haozhou, uqdalves
 */
@SuppressWarnings("serial")
public class PDTWDistanceCalculator extends TrajectoryDistanceFunction {
    private double c;

    /**
     * Set default PDTW reduction rate parameter as 3.0x
     *
     * @param dist The points distance measure to use.
     */
    public PDTWDistanceCalculator(PointDistanceFunction dist) {
        super(dist);
        this.c = 3;
    }

    /**
     * @param dist The points distance measure to use.
     * @param n    PDTW algorithm parameter: size reduction rate
     *             (e.g. 2.0x, 3.0x, .., 10.0x), must be greater than or
     *             equals to one 1.0.
     */
    public PDTWDistanceCalculator(double n, PointDistanceFunction dist) {
        super(dist);
        if (n < 1) {
            throw new IllegalArgumentException(
                    "PDTW paramenter must greater than one (1.0).");
        }
        this.c = n;
    }

    @Override
    public double getDistance(final Trajectory t1, final Trajectory t2) {
        // make sure the original trajectories are unchanged
        return PDTW(t1.clone(), t2.clone());
    }

    protected double PDTW(List<STPoint> rO, List<STPoint> sO) {
        List<STPoint> r = reduceDimension(rO);
        List<STPoint> s = reduceDimension(sO);

        DTWDistanceCalculator dtw =
                new DTWDistanceCalculator(distFunc);

        return dtw.DTW(r, s);
    }

    private List<STPoint> reduceDimension(List<STPoint> t) {
        if (c == 0) {
            throw new IllegalArgumentException(
                    "PDTW paramenter must no be zero.");
        }

        List<STPoint> result = new ArrayList<STPoint>();
        if (t.size() == 0) {
            return result;
        }

        double temp_x = 0.0;
        double temp_y = 0.0;
        for (int i = 0; i < t.size(); i++) {
            STPoint p = t.get(i);
            temp_x += p.x();
            temp_y += p.y();

            if ((i + 1) % c == 0) {
                temp_x /= c;
                temp_y /= c;

                result.add(new STPoint(temp_x, temp_y));

                temp_x = 0.0;
                temp_y = 0.0;
            } else if (i == t.size() - 1) {
                double lastCount = t.size() % c;
                temp_x /= lastCount;
                temp_y /= lastCount;

                result.add(new STPoint(temp_x, temp_y));

                temp_x = 0.0;
                temp_y = 0.0;
            }
        }

        return result;
    }

}
