package traminer.util.trajectory.segmentation;

import traminer.util.exceptions.EmptyTrajectoryException;
import traminer.util.spatial.objects.st.STPoint;
import traminer.util.trajectory.Trajectory;

import java.util.ArrayList;
import java.util.List;

/**
 * Segments a trajectory if two consecutive points have
 * a time gap greater than a given threshold.
 *
 * @author uqdalves
 */
@SuppressWarnings("serial")
public class MaxTimeGapSegmentation extends TrajectorySegmentation {
    private final long maximumGap;

    /**
     * @param maximumGap Maximum time gap allowed
     * @throws IllegalArgumentException
     */
    public MaxTimeGapSegmentation(long maximumGap) {
        if (maximumGap <= 0) {
            throw new IllegalArgumentException(
                    "Time Gap Threshold must be positive.");
        }
        this.maximumGap = maximumGap;
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
        long gap = 0;
        for (int i = 1; i < trajectory.size(); i++) {
            STPoint pi = trajectory.get(i);
            STPoint pj = trajectory.get(i - 1);
            gap = pi.time() - pj.time();
            // split
            if (gap > maximumGap) {
                result.add(sub);
                sub = new Trajectory();
                sub.setParentId(trajectory.getId());
            }
            sub.add(pi);
        }
        // add last sub-trajectory
        result.add(sub);

        return result;
    }
}
