package traminer.util.trajectory.transformation;

import traminer.util.exceptions.EmptyTrajectoryException;
import traminer.util.spatial.objects.st.STPoint;
import traminer.util.trajectory.Trajectory;

import java.util.ArrayList;
import java.util.List;

/**
 * Delete some points from a given trajectory.
 *
 * @author uqhsu1, uqdalves
 */
@SuppressWarnings("serial")
public class DeletePointsTransformation extends TrajectoryTransformation {
    private final double deleteRate;

    /**
     * Set default add rate = 0.25 (25%).
     */
    public DeletePointsTransformation() {
        this.deleteRate = 0.25;
    }

    /**
     * @param deleteRate Rate of points to delete (0.0 = 0%, 1.0 = 100%)
     */
    public DeletePointsTransformation(double deleteRate) {
        if (deleteRate < 0) {
            throw new IllegalArgumentException(
                    "Delete Rate threshold must be positive.");
        }
        this.deleteRate = deleteRate;
    }

    @Override
    public Trajectory getTransformation(Trajectory t) {
        if (t.isEmpty()) {
            throw new EmptyTrajectoryException(
                    "Trajectory for transformation must not be empty.");
        }
        // make sure the original trajectory is unchanged
        List<STPoint> result = deletePoints(t.clone(), new ArrayList<STPoint>());
        Trajectory tDel = new Trajectory(result);
        tDel.setId(t.getId());
        tDel.setDimension(t.getDimension());
        return new Trajectory(tDel);
    }

    /**
     * Delete points from t, except those in escapeList.
     */
    public Trajectory getTransformation(Trajectory t, List<STPoint> escapeList) {
        if (t.isEmpty()) {
            throw new EmptyTrajectoryException(
                    "Trajectory for transformation must not be empty.");
        }
        // make sure the original trajectory is unchanged
        List<STPoint> result = deletePoints(t.clone(), escapeList);
        Trajectory tDel = new Trajectory(result);
        tDel.setId(t.getId());
        tDel.setDimension(t.getDimension());
        return new Trajectory(tDel);
    }

    /**
     * Do the deletion transformation.
     */
    private List<STPoint> deletePoints(List<STPoint> list, List<STPoint> escapeList) {
        List<STPoint> result = new ArrayList<STPoint>();

        int deleteCount = (int) (list.size() * deleteRate);

        if (list.size() - deleteCount <= escapeList.size()) {
            return escapeList;
        }

        double[] value = new double[list.size()];
        boolean[] mark = new boolean[list.size()];

        for (int i = 0; i < list.size(); i++) {
            value[i] = Math.random();
            mark[i] = false;
        }

        for (int i = 0; i < list.size(); i++) {
            STPoint temp = list.get(i);
            for (int j = 0; j < escapeList.size(); j++) {
                if (temp.equals(escapeList.get(j))) {
                    value[i] = -1;
                }
            }
        }

        int[] deleteList = topN(list.size(), deleteCount, value);
        for (int i = 0; i < deleteList.length; i++) {
            mark[deleteList[i]] = true;
        }
        for (int i = 0; i < mark.length; i++) {
            if (!mark[i]) {
                result.add(list.get(i));
            }
        }

        return result;
    }

}
