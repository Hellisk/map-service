package traminer.util.trajectory.transformation;

import traminer.util.exceptions.EmptyTrajectoryException;
import traminer.util.spatial.objects.st.STPoint;
import traminer.util.trajectory.Trajectory;

import java.util.ArrayList;
import java.util.List;

/**
 * Changes the sampling rate of the trajectory points.
 * Make the time interval between every sample point of
 * the trajectory equals a given rate.
 *
 * @author uqdalves
 */
@SuppressWarnings("serial")
public class SamplingRateTransformation extends TrajectoryTransformation {
    private final long sampleRate;
    private long startTime;

    /**
     * Set the default sample rate = 1 time unit,
     * and start time as the time-stamp of the
     * trajectory's first sample point.
     */
    public SamplingRateTransformation() {
        this.sampleRate = 1;
        this.startTime = -777;
    }

    /**
     * Set the start time as the time-stamp of the
     * trajectory's first sample point.
     *
     * @param sampleRate New point's time interval.
     */
    public SamplingRateTransformation(long sampleRate) {
        if (sampleRate < 0) {
            throw new IllegalArgumentException(
                    "Sampling Rate threshold must be positive.");
        }
        this.sampleRate = sampleRate;
        this.startTime = -777;
    }

    /**
     * @param sampleRate New point's time interval.
     * @param startTime  New time-stamp of the first
     *                   sample point.
     */
    public SamplingRateTransformation(long sampleRate, long startTime) {
        if (sampleRate < 0 || startTime < 0) {
            throw new IllegalArgumentException(
                    "Sampling Rate and Time thresholds must be positive.");
        }
        this.sampleRate = sampleRate;
        this.startTime = startTime;
    }

    @Override
    public Trajectory getTransformation(Trajectory t) {
        if (t.isEmpty()) {
            throw new EmptyTrajectoryException(
                    "Transformation error. Trajectory must not be empty.");
        }
        // make sure the original trajectory is unchanged
        List<STPoint> result = changeRate(t.clone());
        Trajectory tNew = new Trajectory(result);
        tNew.setId(t.getId());
        tNew.setDimension(t.getDimension());
        return new Trajectory(tNew);
    }

    protected List<STPoint> changeRate(List<STPoint> list) {
        if (startTime == -777) {
            startTime = list.get(0).time();
        }
        // change rate
        List<STPoint> resultList = new ArrayList<>();
        STPoint pi;
        STPoint pj = list.get(0);
        long newTime = startTime;
        resultList.add(new STPoint(pj.x(), pj.y(), newTime));
        for (int i = 1; i < list.size(); i++) {
            pi = list.get(i);
            newTime = pj.time() + sampleRate;
            resultList.add(new STPoint(pi.x(), pi.y(), newTime));
            pj = pi;
        }
        return resultList;
    }
}
