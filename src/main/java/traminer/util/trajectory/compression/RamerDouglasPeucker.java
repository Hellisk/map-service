package traminer.util.trajectory.compression;

import traminer.util.spatial.objects.Point;
import traminer.util.spatial.objects.st.STPoint;
import traminer.util.trajectory.Trajectory;

import java.util.ArrayList;
import java.util.List;

/**
 * Implements the Ramer-Douglas-Peucker line
 * simplification algorithm.
 *
 * @author uqhsu1, uqdalves
 */
@SuppressWarnings("serial")
public class RamerDouglasPeucker extends TrajectoryCompression {
    //threshold should be decided carefully
    private double threshold;

    /**
     * Set default distance threshold as 1.0.
     */
    public RamerDouglasPeucker() {
        this.threshold = 1.0;
    }

    /**
     * @param threshold Distance threshold
     */
    public RamerDouglasPeucker(double threshold) {
        this.threshold = threshold;
    }

    @Override
    public Trajectory doCompression(Trajectory t) {
        List<STPoint> list = getKeyPointsArrayList(t);
        Trajectory result = new Trajectory(list);
        t.cloneTo(result);
        return result;
    }

    private List<STPoint> getKeyPointsArrayList(List<STPoint> points) {
        List<STPoint> result = new ArrayList<STPoint>();
        double maxDistance = 0.0;
        int index = 0;
        for (int i = 0; i < points.size() - 1; i++) {
            double distance = perpendicularDistance(
                    points.get(0), points.get(points.size() - 1), points.get(i));
            if (distance > maxDistance) {
                index = i;
                maxDistance = distance;
            }
        }

        if (maxDistance > threshold) {
            List<STPoint> leftPoints = getArrayList(points, 0, index + 1);
            List<STPoint> rightPoints = getArrayList(points, index, points.size() - index);
            List<STPoint> tempLeft = getKeyPointsArrayList(leftPoints);
            List<STPoint> tempRight = getKeyPointsArrayList(rightPoints);
            result = mergeResult(tempLeft, tempRight);
        } else {
            result.add(points.get(0));
            result.add(points.get(points.size() - 1));
        }

        return result;
    }

    private double perpendicularDistance(STPoint start, Point end, Point p) {
        double x1 = start.x();
        double y1 = start.y();
        double x2 = end.x();
        double y2 = end.y();
        double x3 = p.x();
        double y3 = p.y();

        double result = 0.0;
        double k = 0.0, b = 0.0;

        if (x1 == x2) {
            return Math.abs(x3 - x1);
        }

        k = (y1 - y2) / (x1 - x2);
        b = (x1 * y2 - x2 * y1) / (x1 - x2);

        double A = k, B = -1, C = b;

        result = Math.abs((A * x3 + B * y3 + C) / Math.sqrt(A * A + B * B));

        return result;
    }

    private List<STPoint> mergeResult(List<STPoint> left, List<STPoint> right) {
        List<STPoint> result = new ArrayList<STPoint>();
        for (int i = 0; i < left.size(); i++) {
            result.add(left.get(i));
        }
        for (int i = 1; i < right.size(); i++) {
            result.add(right.get(i));
        }

        return result;
    }

    private List<STPoint> getArrayList(List<STPoint> list, int start, int length) {
        List<STPoint> result = new ArrayList<STPoint>();
        for (int i = start; i < start + length; i++) {
            result.add(list.get(i));
        }
        return result;
    }
}
