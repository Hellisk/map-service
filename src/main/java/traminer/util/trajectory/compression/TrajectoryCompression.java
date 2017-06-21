package traminer.util.trajectory.compression;

import traminer.util.spatial.SpatialInterface;
import traminer.util.trajectory.Trajectory;

/**
 * Base class for trajectory data compression methods
 *
 * @author uqdalves
 */
@SuppressWarnings("serial")
public abstract class TrajectoryCompression implements SpatialInterface {
    /**
     * Run the compression algorithm for the
     * given trajectory.
     *
     * @return Return a copy of this trajectory compressed.
     */
    public abstract Trajectory doCompression(Trajectory t);

    protected String getErrorMsg(String name, String message) {
        return "[COMPRESSION ERROR] In '" + name + "': " + message;
    }
}
