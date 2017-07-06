package traminer.util.trajectory.compression;

import traminer.util.DeltaEncoder;
import traminer.util.spatial.objects.st.STPoint;
import traminer.util.trajectory.Trajectory;

/**
 * Implements trajectory Delta compression algorithm.
 * Compression over spatial-temporal attributes only.
 *
 * @author uqdalves
 */
@SuppressWarnings("serial")
public class DeltaCompression implements TrajectoryCompression {

    @Override
    public Trajectory doCompression(Trajectory t) {
        return deltaCompress(t);
    }

    /**
     * Run Delta compression on the given trajectory.
     *
     * @param t Trajectory to compress.
     * @return A copy of this trajectory after delta compression.
     */
    private Trajectory deltaCompress(Trajectory t) {
        double[] xDelta = DeltaEncoder.deltaEncode(t.getXValues());
        double[] yDelta = DeltaEncoder.deltaEncode(t.getYValues());
        long[] tDelta = DeltaEncoder.deltaEncode(t.getTimeValues());
        Trajectory result = new Trajectory(t.getId());
        t.cloneTo(result);
        for (int i = 0; i < t.size(); i++) {
            result.add(new STPoint(xDelta[i], yDelta[i], tDelta[i]));
        }
        return result;
    }

}
