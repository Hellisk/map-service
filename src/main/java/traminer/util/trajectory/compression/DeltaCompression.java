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
public class DeltaCompression extends TrajectoryCompression {
    @Override
    public Trajectory doCompression(Trajectory t) {
        return deltaCompress(t);
    }

    private Trajectory deltaCompress(Trajectory t) {
        double[] xDelta = DeltaEncoder.deltaEncode(t.getXArray());
        double[] yDelta = DeltaEncoder.deltaEncode(t.getYArray());
        long[] tDelta = DeltaEncoder.deltaEncode(t.getTimeArray());
        Trajectory result = new Trajectory(t.getId());
        t.cloneTo(result);
        for (int i = 0; i < t.size(); i++) {
            result.add(new STPoint(xDelta[i], yDelta[i], tDelta[i]));
        }
        return result;
    }

}
