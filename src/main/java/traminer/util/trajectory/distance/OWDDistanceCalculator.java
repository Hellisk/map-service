package traminer.util.trajectory.distance;

import traminer.util.spatial.distance.PointDistanceFunction;
import traminer.util.spatial.objects.Point;
import traminer.util.spatial.objects.Segment;
import traminer.util.spatial.objects.st.STPoint;
import traminer.util.trajectory.Trajectory;

import java.util.List;

/**
 * OWD: trajecotry distance measure.
 *
 * @author uqhsu1, uqdalves
 */
@SuppressWarnings("serial")
public class OWDDistanceCalculator extends TrajectoryDistanceFunction {

    /**
     * @param dist The points distance measure to use.
     */
    public OWDDistanceCalculator(PointDistanceFunction dist) {
        super(dist);
    }

    @Override
    public double getDistance(final Trajectory t1, final Trajectory t2) {
        // make sure the original trajectories are unchanged
        return OWD(t1.clone(), t2.clone());
    }

    protected double OWD(List<STPoint> r, List<STPoint> s) {
        double rOWD = getOWD(r, s);
        double sOWD = getOWD(s, r);

        return (rOWD + sOWD) / 2;
    }

    private double getOWD(List<STPoint> t, List<STPoint> tt) {
        List<Segment> ttl = getPolyline(tt);

        double owd = 0;
        for (int i = 0; i < t.size(); i++) {
            owd += minDistance(t.get(i), tt, ttl);
        }

        return owd / t.size();
    }

    private double minDistance(Point p, List<STPoint> t, List<Segment> l) {
        double min = distFunc.distance(p, t.get(0));
        for (int i = 0; i < t.size(); i++) {
            double temp = distFunc.distance(p, t.get(i));
            if (temp < min) {
                min = temp;
            }
        }
        for (int i = 0; i < l.size(); i++) {
            double temp = l.get(i).distance(p);
            if (temp < min) {
                min = temp;
            }
        }

        return min;
    }
}
