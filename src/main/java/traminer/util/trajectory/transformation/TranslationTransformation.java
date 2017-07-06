package traminer.util.trajectory.transformation;

import traminer.util.exceptions.EmptyParameterException;
import traminer.util.spatial.objects.st.STPoint;
import traminer.util.trajectory.Trajectory;

import java.util.ArrayList;
import java.util.List;

/**
 * Translates a trajectory on the XY axis for the given translation values.
 *
 * @author uqhsu1, uqdalves
 */
@SuppressWarnings("serial")
public class TranslationTransformation extends TrajectoryTransformation {
    private final double translationX;
    private final double translationY;

    /**
     * Set the default translation values X = 1.0, and Y = 1.0
     */
    public TranslationTransformation() {
        this.translationX = 1.0;
        this.translationY = 1.0;
    }
    /**
     * @param translationX Translation value over the X axis, 
     * may be positive or negative.
     * @param translationY Translation value over the Y axis, 
     * may be positive or negative.
     */
    public TranslationTransformation(double translationX, double translationY) {
        this.translationX = translationX;
        this.translationY = translationY;
    }

    @Override
    public Trajectory getTransformation(Trajectory t) {
        if (t.isEmpty()) {
            throw new EmptyParameterException(
                    "Transformation error. Trajectory must not be empty.");
        }
        // make sure the original trajectory is unchanged
        List<STPoint> result = translate(t.clone());
        Trajectory tNew = new Trajectory(result);
        tNew.setId(t.getId());
        tNew.setDimension(t.getDimension());
        return new Trajectory(tNew);
    }

    protected List<STPoint> translate(List<STPoint> list) {
        List<STPoint> result = new ArrayList<>();
        double x, y;
        for (STPoint p : list) {
            x = p.x() + translationX;
            y = p.y() + translationY;
            result.add(new STPoint(x, y, p.time()));
        }
        return result;
    }
}
