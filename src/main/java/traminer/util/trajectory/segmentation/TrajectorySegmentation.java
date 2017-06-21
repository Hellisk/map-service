package traminer.util.trajectory.segmentation;

import traminer.util.spatial.SpatialInterface;
import traminer.util.trajectory.Trajectory;

import java.util.List;

/**
 * A common abstract class for segmenting a long trajectory
 * into multiple short, more meaningful sub-trajectories.
 *
 * @author Kevin Zheng, uqdalves
 */
@SuppressWarnings("serial")
public abstract class TrajectorySegmentation implements SpatialInterface {
    /**
     * Segments the given trajectory into multiple sub-trajectories.
     *
     * @return A list of sub-trajectories.
     * @throws EmptyTrajectoryException If input trajectory is empty.
     */
    public abstract List<Trajectory> doSegmentation(Trajectory trajectory);
}
