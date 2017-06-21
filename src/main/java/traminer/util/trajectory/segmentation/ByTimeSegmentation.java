package traminer.util.trajectory.segmentation;

import traminer.util.exceptions.EmptyTrajectoryException;
import traminer.util.spatial.objects.st.STPoint;
import traminer.util.trajectory.Trajectory;

import java.util.ArrayList;
import java.util.List;

/**
 * Segments a trajectory based on a time threshold.
 * That is, segments a trajectory such that the time span
 * of every sub-trajectory is no greater than the given
 * threshold.
 *
 * @author uqdalves
 */
@SuppressWarnings("serial")
public class ByTimeSegmentation extends TrajectorySegmentation {
    private final double timeThreshold;

    /**
     * @throws IllegalArgumentException
     */
    public ByTimeSegmentation(long timeThreshold) {
        if (timeThreshold <= 0) {
            throw new IllegalArgumentException(
                    "Time Threshold must be positive.");
        }
        this.timeThreshold = timeThreshold;
    }

    @Override
    public List<Trajectory> doSegmentation(Trajectory trajectory) {
        if (trajectory.isEmpty()) {
            throw new EmptyTrajectoryException(
                    "Segmentation error. Trajectory must not be empty.");
        }
        List<Trajectory> result = new ArrayList<Trajectory>();
        Trajectory sub = new Trajectory();
        sub.setParentId(trajectory.getId());
        sub.add(trajectory.get(0));
        long dist = 0;
        long time = 0;
        for (int i = 1; i < trajectory.size(); i++) {
            STPoint pi = trajectory.get(i);
            STPoint pj = trajectory.get(i - 1);
            dist = pi.time() - pj.time();
            // split
            if (time + dist > timeThreshold) {
                result.add(sub);
                sub = new Trajectory();
                sub.setParentId(trajectory.getId());
            }
            sub.add(pi);
            time += dist;
        }
        // add last sub-trajectory
        result.add(sub);

        return result;
    }

}
