package traminer.util.trajectory.transformation;

import traminer.util.exceptions.EmptyTrajectoryException;
import traminer.util.spatial.objects.st.STPoint;
import traminer.util.trajectory.Trajectory;

import java.util.ArrayList;
import java.util.List;

/**
 * Shifts the time period of a trajectory,
 * make it starts from time t = startTime
 *
 * @author uqdalves
 */
@SuppressWarnings("serial")
public class TimeShiftTransformation extends TrajectoryTransformation {
    private final long startTime;

    /**
     * Set the default start time = 0.
     */
    public TimeShiftTransformation() {
        this.startTime = 0;
    }

    /**
     * @param startTime Make the trajectory starts from time t = startTime.
     */
    public TimeShiftTransformation(long startTime) {
        if (startTime < 0) {
            throw new IllegalArgumentException(
                    "Start time must be positive.");
        }
        this.startTime = startTime;
    }

    @Override
    public Trajectory getTransformation(Trajectory t) {
        if (t.isEmpty()) {
            throw new EmptyTrajectoryException(
                    "Transformation error. Trajectory must not be empty.");
        }
        // make sure the original trajectory is unchanged
        List<STPoint> result = shiftTime(t.clone());
        Trajectory tNew = new Trajectory(result);
        tNew.setId(t.getId());
        tNew.setDimension(t.getDimension());
        return new Trajectory(tNew);
    }

    protected List<STPoint> shiftTime(List<STPoint> list) {
        // shift time
        List<STPoint> result = new ArrayList<>();
        long t0 = list.get(0).time();
        long newTime;
        STPoint pi;
        for (int i = 0; i < list.size(); i++) {
            pi = list.get(i);
            newTime = (pi.time() - t0) + startTime;
            result.add(new STPoint(pi.x(), pi.y(), newTime));
        }

        return result;
    }
}
