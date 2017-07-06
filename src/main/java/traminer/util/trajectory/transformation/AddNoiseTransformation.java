package traminer.util.trajectory.transformation;

import traminer.util.exceptions.EmptyParameterException;
import traminer.util.spatial.objects.st.STPoint;
import traminer.util.trajectory.Trajectory;

import java.util.ArrayList;
import java.util.List;

/**
 * Add some noise to a given trajectory.
 * 
 * @author uqhsu1, uqdalves
 */
@SuppressWarnings("serial")
public class AddNoiseTransformation extends TrajectoryTransformation {
    private final double noiseRate;
    private final double noiseDistance;

    /**
     * Setup default noise rate = 0.25 (25%), and noise distance = 0.01.
     */
    public AddNoiseTransformation() {
        this.noiseRate = 0.25;
        this.noiseDistance = 0.01;
    }
    /**
     * @param noiseRate Rate of noise to add (0.0 = 0%, 1.0 = 100%)
     * @param noiseDistance Distance threshold for noisy points.
     */
    public AddNoiseTransformation(double noiseRate, double noiseDistance) {
        if (noiseDistance < 0 || noiseRate < 0) {
            throw new IllegalArgumentException(
                    "Noise thresholds must be positive.");
        }
        this.noiseRate = noiseRate;
        this.noiseDistance = noiseDistance;
    }

    @Override
    public Trajectory getTransformation(Trajectory t) {
        if (t.isEmpty()) {
            throw new EmptyParameterException(
                    "Transformation error. Trajectory must not be empty.");
        }
        // make sure the original trajectory is unchanged
        List<STPoint> result = addNoise(t.clone());
        Trajectory tNoisy = new Trajectory(result);
        tNoisy.setId(t.getId());
        tNoisy.setDimension(t.getDimension());
        return new Trajectory(tNoisy);
    }

    protected List<STPoint> addNoise(List<STPoint> list) {
        List<STPoint> result = new ArrayList<STPoint>(list.size());

        double ratio = Math.random();
        int addPointCount = (int) (list.size() * noiseRate);

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
            boolean t = true;
            for (int j = 0; j < valueList.length; j++) {
                if (valueList[j] == i) {
                    double choice = Math.random();
                    if (choice < ratio) {
                        STPoint temp = getMidNoisePoint(list.get(i), list.get(i + 1));
                        result.add(temp);
                    } else {
                        STPoint temp = getNoisePoint(list.get(i));
                        result.add(temp);
                        t = false;
                    }
                }
            }
            if (t) {
                result.add(list.get(i));
            }
        }

        return result;
    }

    private STPoint getNoisePoint(STPoint p) {
        double noisex = (Math.random() * 2 - 1) * noiseDistance;
        double noisey = (Math.random() * 2 - 1) * noiseDistance;

        double x = p.x() + noisex;
        double y = p.y() + noisey;

        return new STPoint(x, y, p.time());
    }

    private STPoint getMidNoisePoint(STPoint p, STPoint q) {
        double noisex = (Math.random() * 2 - 1) * noiseDistance;
        double noisey = (Math.random() * 2 - 1) * noiseDistance;

        double x = (p.x() + q.x()) / 2 + noisex;
        double y = (p.y() + q.y()) / 2 + noisey;

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
