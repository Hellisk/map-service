package traminer.util.trajectory.transformation;

import traminer.util.exceptions.EmptyParameterException;
import traminer.util.spatial.objects.st.STPoint;
import traminer.util.trajectory.Trajectory;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * 
 * @author uqhsu1, uqdalves
 */
@SuppressWarnings("serial")
public class RandomShiftTransformation extends TrajectoryTransformation {
    private final double shiftRate;
    private final double shiftDistance;

    /**
     * Set the default shift rate = 0.25 (25%), and shift distance = 0.001.
     */
    public RandomShiftTransformation() {
        this.shiftRate = 0.25;
        this.shiftDistance = 0.001;
    }

    /**
     * @param shiftRate The rate of points to shift (0.0 = 0%, 1.0 = 100%)
     * @param shiftDistance Distance for random shifting points.
     */
    public RandomShiftTransformation(double shiftRate, double shiftDistance) {
        if (shiftRate < 0 || shiftDistance < 0) {
            throw new IllegalArgumentException(
                    "Shift thresholds must be positive.");
        }
        this.shiftRate = shiftRate;
        this.shiftDistance = shiftDistance;
    }

    @Override
    public Trajectory getTransformation(Trajectory t) {
        if (t.isEmpty()) {
            throw new EmptyParameterException(
                    "Transformation error. Trajectory must not be empty.");
        }
        // make sure the original trajectory is unchanged
        List<STPoint> result = randomShift(t.clone(), new ArrayList<STPoint>());
        Trajectory tShift = new Trajectory(result);
        tShift.setId(t.getId());
        tShift.setDimension(t.getDimension());
        return new Trajectory(tShift);
    }

    /**
     * Shift points from t, except those in escapeList.
     */
    public Trajectory getTransformation(Trajectory t, List<STPoint> escapeList) {
        if (t.isEmpty()) {
            throw new EmptyParameterException(
                    "Trajectory for transformation must not be empty.");
        }
        // make sure the original trajectory is unchanged
        List<STPoint> result = randomShift(t.clone(), escapeList);
        Trajectory tShift = new Trajectory(result);
        tShift.setId(t.getId());
        tShift.setDimension(t.getDimension());
        return new Trajectory(tShift);
    }

    protected List<STPoint> randomShift(List<STPoint> list, List<STPoint> escapeList) {
        List<STPoint> result = new ArrayList<STPoint>(list.size());

        int shiftCount = (int) (list.size() * shiftRate);

        if (list.size() - shiftCount <= escapeList.size()) {
            shiftCount = list.size() - escapeList.size();
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

        int[] shiftList = topN(list.size(), shiftCount, value);

        for (int i = 0; i < shiftList.length; i++) {
            mark[shiftList[i]] = true;
        }

        for (int i = 0; i < mark.length; i++) {
            if (!mark[i]) {
                result.add(list.get(i));
            } else {
                STPoint temp = getShiftPoint(list.get(i));
                result.add(temp);
            }
        }

        return result;
    }

    private STPoint getShiftPoint(STPoint p) {
        double cosCurvex = 2 * (Math.random() - 0.5) * shiftDistance;
        double cosCurvey = 2 * (Math.random() - 0.5) * shiftDistance;

        double x = p.x() + cosCurvex;
        double y = p.y() + cosCurvey;

        return new STPoint(x, y, p.time());
    }
}
