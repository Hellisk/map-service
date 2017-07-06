package traminer.util.trajectory.distance;

import traminer.util.spatial.distance.PointDistanceFunction;
import traminer.util.spatial.objects.st.STPoint;
import traminer.util.spatial.objects.st.STSegment;
import traminer.util.trajectory.Trajectory;

import java.util.List;

/**
 * Frechet: Trajectory Distance measure
 * 
 * @author uqhsu1, Haozhou, uqdalves
 */
@SuppressWarnings("serial")
public class FrechetDistanceCalculator extends TrajectoryDistanceFunction {

    /**
     * @param dist The points distance measure to use.
     */
    public FrechetDistanceCalculator(PointDistanceFunction dist) {
        super(dist);
    }

    @Override
    public double getDistance(Trajectory t1, Trajectory t2) {
        // make sure the original trajectories are unchanged
        return Frechet(t1.clone(), t2.clone());
    }

    protected double Frechet(List<STPoint> r, List<STPoint> s) {
        double duration = 0;
        assert (r.size() > 1 && s.size() > 1);
        double dR = r.get(r.size() - 1).time() - r.get(0).time();
        double dS = s.get(s.size() - 1).time() - s.get(0).time();

        if (dR > dS) {
            duration = dS;
        } else {
            duration = dR;
        }

        double calculateTimeInterval = duration / 1000;
        //int calculateTimes=1000;

        List<STSegment> r3d = getSTPolyLine(r);
        List<STSegment> s3d = getSTPolyLine(s);
        long rStartTime = r.get(0).time();
        long sStartTime = s.get(0).time();

        double max = 0;
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

            long t = (long) (rStartTime + i * calculateTimeInterval);
            rCurrentPoint = r3d.get(rCurrentLine).getPointByTime(t);
            t = (long) (sStartTime + i * calculateTimeInterval);
            sCurrentPoint = s3d.get(sCurrentLine).getPointByTime(t);

            double distance = distFunc
                    .distance(rCurrentPoint, sCurrentPoint);

            if (distance > max) {
                max = distance;
            }
        }

        return max;
    }
}
