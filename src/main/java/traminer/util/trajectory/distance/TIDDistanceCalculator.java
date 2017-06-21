package traminer.util.trajectory.distance;

import traminer.util.spatial.distance.PointDistanceFunction;
import traminer.util.spatial.objects.st.STPoint;
import traminer.util.trajectory.Trajectory;

import java.util.ArrayList;
import java.util.List;

/**
 * TID: Trajectory distance measure.
 *
 * @author uqhsu1, uqdalves
 */
@SuppressWarnings("serial")
public class TIDDistanceCalculator extends TrajectoryDistanceFunction {
    private List<STPoint> r;
    private List<STPoint> s;
    private double[] angle = new double[]
            {0, 20, 40, 60, 80, 100, 120, 140, 160, 180, 200, 220, 240, 260, 280, 300, 320, 340};

    /**
     * @param dist The points distance measure to use.
     */
    public TIDDistanceCalculator(PointDistanceFunction dist) {
        super(dist);
    }

    @Override
    public double getDistance(final Trajectory t1, final Trajectory t2) {
        // make sure the original trajectories are unchanged
        return TID(t1.clone(), t2.clone());
    }

    protected double TID(List<STPoint> rr, List<STPoint> ss) {
        r = this.normalization(rr);
        s = this.normalization(ss);

        DTWDistanceCalculator dtw =
                new DTWDistanceCalculator(distFunc);

        double[] tempResult = new double[angle.length];
        for (int i = 0; i < angle.length; i++) {
            List<STPoint> tt = getTransformation(angle[i]);
            tempResult[i] = dtw.DTW(tt, s);
        }

        return min(tempResult);
    }

    private List<STPoint> getTransformation(double rotationAngle) {
        double cos = Math.cos(rotationAngle);
        double sin = Math.sin(rotationAngle);

        List<STPoint> ret = new ArrayList<STPoint>(r.size());

        double x, y;
        for (int i = 0; i < r.size(); i++) {
            //assert(r.get(i).dimension == 2);
            x = cos * r.get(i).x() - sin * r.get(i).y();
            y = sin * r.get(i).x() + cos * r.get(i).y();

            ret.add(new STPoint(x, y, r.get(i).time()));
        }

        return ret;
    }

    private double min(double[] r) {
        double min = r[0];
        for (int i = 0; i < r.length; i++) {
            if (r[i] < min) {
                min = r[i];
            }
        }
        return min;
    }

    private List<STPoint> normalization(List<STPoint> t) {
        List<STPoint> result = new ArrayList<STPoint>();
        if (t.size() == 0) {
            return result;
        }

        double mean_x = 0.0;
        double mean_y = 0.0;
        for (STPoint p : t) {
            mean_x += p.x();
            mean_y += p.y();
        }
        mean_x /= t.size();
        mean_y /= t.size();

        double std_x = 0.0;
        double std_y = 0.0;
        for (STPoint p : t) {
            std_x += Math.pow((p.x() - mean_x), 2);
            std_y += Math.pow((p.y() - mean_y), 2);
        }
        std_x = Math.sqrt(std_x);
        std_y = Math.sqrt(std_y);

        double temp_x = 0.0;
        double temp_y = 0.0;
        for (STPoint p : t) {
            temp_x = (p.x() - mean_x) / std_x;
            temp_y = (p.y() - mean_y) / std_y;
            result.add(new STPoint(temp_x, temp_y));
        }

        return result;
    }
}
