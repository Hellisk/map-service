package traminer.util.trajectory.segmentation;

import traminer.util.exceptions.EmptyParameterException;
import traminer.util.spatial.SpatialInterface;
import traminer.util.trajectory.Trajectory;

import java.util.List;

/**
 * A common abstract class for segmenting a long trajectory 
 * into multiple short, more meaningful sub-trajectories.
 * 
 * @author uqdalves
 */
public interface TrajectorySegmentation extends SpatialInterface {
    /**
     * Segments the given trajectory into multiple sub-trajectories.
     *
     * @param trajectory The trajectory to segment.
     * @return A list of sub-trajectories.
     * @throws EmptyParameterException If input trajectory is empty.
     */
    List<Trajectory> doSegmentation(Trajectory trajectory)
            throws EmptyParameterException;
}
