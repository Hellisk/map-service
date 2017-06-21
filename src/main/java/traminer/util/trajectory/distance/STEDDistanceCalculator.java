package traminer.util.trajectory.distance;

import traminer.util.spatial.distance.PointDistanceFunction;
import traminer.util.spatial.objects.st.STPoint;
import traminer.util.spatial.objects.st.STSegment;
import traminer.util.trajectory.Trajectory;

import java.util.List;

/**
 * STED: Trajectory distance measure
 *
 * @author uqhsu1, Haozhou, uqdalves
 */
@SuppressWarnings("serial")
public class STEDDistanceCalculator extends TrajectoryDistanceFunction {
    /**
     * @param dist The points distance measure to use.
     */
    public STEDDistanceCalculator(PointDistanceFunction dist) {
        super(dist);
    }

    @Override
    public double getDistance(final Trajectory t1, final Trajectory t2) {
        // make sure the original trajectories are unchanged
        return STED(t1.clone(), t2.clone());
    }

    protected double STED(List<STPoint> r, List<STPoint> s) {
        assert (r.size() > 1 && s.size() > 1);

        double duration = 0.0;
        double dR = r.get(r.size() - 1).time() - r.get(0).time();
        double dS = s.get(s.size() - 1).time() - s.get(0).time();

        if (dR <= 0 || dS <= 0) {
            return 0;
        }

        if (dR > dS) {
            duration = dS;
        } else {
            duration = dR;
        }

        double calculateTimeInterval = duration / 1000;

        List<STSegment> r3d = getSTPolyLine(r);
        List<STSegment> s3d = getSTPolyLine(s);
        long rStartTime = r.get(0).time();
        long sStartTime = s.get(0).time();

        double sum = 0.0;
        for (int i = 0; i < 1000; i++) {
            STPoint rCurrentPoint;
            STPoint sCurrentPoint;

            int rCurrentLine = 0;
            int sCurrentLine = 0;

            for (int j = 0; j < r.size() - 1; j++) {
                if (rStartTime + i * calculateTimeInterval >= r.get(j).time() &&
                        rStartTime + i * calculateTimeInterval <= r.get(j + 1).time()) {
                    rCurrentLine = j;
                    break;
                }
            }

            for (int j = 0; j < s.size() - 1; j++) {
                if (sStartTime + i * calculateTimeInterval >= s.get(j).time() &&
                        sStartTime + i * calculateTimeInterval <= s.get(j + 1).time()) {
                    sCurrentLine = j;
                    break;
                }
            }

            rCurrentPoint = r3d.get(rCurrentLine).getPointByTime((long) (rStartTime + i * calculateTimeInterval));
            sCurrentPoint = s3d.get(sCurrentLine).getPointByTime((long) (sStartTime + i * calculateTimeInterval));

            double distance = distFunc.pointToPointDistance(rCurrentPoint, sCurrentPoint);

            sum += distance;
        }

        return (sum / Math.abs(duration));
    }
}
