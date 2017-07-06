package traminer.util.trajectory.transformation;

import traminer.util.exceptions.EmptyParameterException;
import traminer.util.spatial.objects.st.STPoint;
import traminer.util.trajectory.Trajectory;

import java.util.ArrayList;
import java.util.List;

/**
 * Add some extra points to a given trajectory.
 * 
 * @author uqhsu1, uqdalves
 */
@SuppressWarnings("serial")
public class AddPointsTransformation extends TrajectoryTransformation {
    private final double addRate; 
    
    /**
     * Set the default add rate = 0.25 (25%).
     */
    public AddPointsTransformation() {
        this.addRate = 0.25;
    }    
    /**
     * @param addPointsRate Rate of points to add (0.0 = 0%, 1.0 = 100%)
     */
    public AddPointsTransformation(double addPointsRate) {
        if (addPointsRate < 0) {
            throw new IllegalArgumentException(
                    "Add Rate threshold must be positive.");
        }
        this.addRate = addPointsRate;
    }

    @Override
    public Trajectory getTransformation(Trajectory t) {
        if (t.isEmpty()) {
            throw new EmptyParameterException(
                    "Transformation error. Trajectory must not be empty.");
        }
        // make sure the original trajectory is unchanged
        List<STPoint> result = addPoints(t.clone());
        Trajectory tAdd = new Trajectory(result);
        tAdd.setId(t.getId());
        tAdd.setDimension(t.getDimension());
        return new Trajectory(tAdd);
    }

    protected List<STPoint> addPoints(List<STPoint> list) {
        List<STPoint> result = new ArrayList<STPoint>(list.size());

        int addPointCount = (int) (list.size() * addRate);

        if (list.size() < 2) {
            return list;
        }
        if (addPointCount < 1) {
            addPointCount = 1;
        }
        if (addPointCount >= list.size()) {
            addPointCount = list.size() - 1;
        }

        int[] valueList = topN(list.size() - 1, addPointCount);

        for (int i = 0; i < list.size(); i++) {
            result.add(list.get(i));
            for (int j = 0; j < valueList.length; j++) {
                if (valueList[j] == i) {
                    STPoint temp = getMidPoint(list.get(i), list.get(i + 1));
                    result.add(temp);
                }
            }
        }

        return result;
    }

    private STPoint getMidPoint(STPoint p, STPoint q) {
        double x = (p.x() + q.x()) / 2;
        double y = (p.y() + q.y()) / 2;

        long pt = p.time();
        long qt = q.time();
        if (pt > qt) {
            long temp = pt;
            pt = qt;
            qt = temp;
        }
        long time = pt + (qt - pt) / 2;

        return new STPoint(x, y, time);
    }
}
