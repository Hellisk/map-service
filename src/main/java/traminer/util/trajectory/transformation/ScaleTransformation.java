package traminer.util.trajectory.transformation;

import traminer.util.exceptions.EmptyParameterException;
import traminer.util.spatial.objects.st.STPoint;
import traminer.util.trajectory.Trajectory;

import java.util.ArrayList;
import java.util.List;

/**
 * Changes the scale of a trajectory by a given ratio.
 * 
 * @author uqhsu1, uqdalves
 */
@SuppressWarnings("serial")
public class ScaleTransformation extends TrajectoryTransformation {
    private final double scaleRatio;

    /**
     * Set the default new scale = 1.5 (150%)
     */
    public ScaleTransformation() {
        this.scaleRatio = 1.5;
    }
    /**
     * @param scaleRatio The new scale ratio (0.5 = 50%, 1.0 = 100%, 2.0 = 200%, and so on)
     */
    public ScaleTransformation(double scaleRatio) {
        if (scaleRatio < 0) {
            throw new IllegalArgumentException(
                    "Scale threshold must be positive.");
        }
        this.scaleRatio = scaleRatio;
    }

    @Override
    public Trajectory getTransformation(Trajectory t) {
        if (t.isEmpty()) {
            throw new EmptyParameterException(
                    "Transformation error. Trajectory must not be empty.");
        }
        // make sure the original trajectory is unchanged
        List<STPoint> result = changeScale(t.clone());
        Trajectory tNew = new Trajectory(result);
        tNew.setId(t.getId());
        tNew.setDimension(t.getDimension());
        return new Trajectory(tNew);
    }

    protected List<STPoint> changeScale(List<STPoint> list) {
        List<STPoint> result = new ArrayList<STPoint>();
        STPoint p0 = list.get(0);
        result.add(p0);
        double lengthX, lengthY, x, y;
        for (int i = 1; i < list.size(); i++) {
            lengthX = (list.get(i).x() - p0.x()) * scaleRatio;
            lengthY = (list.get(i).y() - p0.y()) * scaleRatio;
            x = p0.x() + lengthX;
            y = p0.y() + lengthY;

            result.add(new STPoint(x, y, list.get(i).time()));
        }

        return result;
    }
}
