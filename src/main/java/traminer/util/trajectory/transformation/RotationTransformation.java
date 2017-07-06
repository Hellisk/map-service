package traminer.util.trajectory.transformation;

import traminer.util.exceptions.EmptyParameterException;
import traminer.util.spatial.objects.st.STPoint;
import traminer.util.trajectory.Trajectory;

import java.util.ArrayList;
import java.util.List;

/**
 * Rotates the whole trajectory by a given angle.
 * 
 * @author uqhsu1, uqdalves
 */
@SuppressWarnings("serial")
public class RotationTransformation extends TrajectoryTransformation {
    private final double rotationAngle;

    /**
     * Set the default rotation angle = 45 degrees
     */
    public RotationTransformation() {
        this.rotationAngle = Math.toRadians(45);
    }

    /**
     * @param rotationAngle Rotation angle (in degrees, e.g. 30, 60, 135, etc.)
     */
    public RotationTransformation(double rotationAngle) {
        this.rotationAngle = Math.toRadians(rotationAngle);
    }

    @Override
    public Trajectory getTransformation(Trajectory t) {
        if (t.isEmpty()) {
            throw new EmptyParameterException(
                    "Transformation error. Trajectory must not be empty.");
        }
        // make sure the original trajectory is unchanged
        List<STPoint> result = rotate(t.clone());
        Trajectory tNew = new Trajectory(result);
        tNew.setId(t.getId());
        tNew.setDimension(t.getDimension());
        return new Trajectory(tNew);
    }

    protected List<STPoint> rotate(List<STPoint> t) {
        List<STPoint> result = new ArrayList<STPoint>(t.size());

        double cos = Math.cos(rotationAngle);
        double sin = Math.sin(rotationAngle);

        double x, y;
        for (STPoint p : t) {
            x = cos * p.x() - sin * p.y();
            y = sin * p.x() + cos * p.y();

            result.add(new STPoint(x, y, p.time()));
        }

        return result;
    }
}
