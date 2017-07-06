package traminer.util.trajectory.compression;

import traminer.util.exceptions.EmptyParameterException;
import traminer.util.spatial.SpatialInterface;
import traminer.util.trajectory.Trajectory;

/**
 * Base interface for trajectory data compression methods
 * 
 * @author uqdalves
 *
 */
public interface TrajectoryCompression extends SpatialInterface {
    /**
     * Run the compression algorithm for the given trajectory.
     *
     * @param t The trajectory to compress.
     * @return A copy of this trajectory after compression.
     * @throws EmptyParameterException If input trajectory is empty.
     */
    Trajectory doCompression(Trajectory t)
            throws EmptyParameterException;
}
