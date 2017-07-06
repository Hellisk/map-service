package traminer.util.trajectory.segmentation;

import traminer.util.exceptions.EmptyParameterException;
import traminer.util.spatial.distance.EuclideanDistanceFunction;
import traminer.util.spatial.distance.PointDistanceFunction;
import traminer.util.spatial.objects.st.STPoint;
import traminer.util.trajectory.Trajectory;

import java.util.ArrayList;
import java.util.List;

/**
 * Segments a trajectory based on a length threshold.
 * That is, segments a trajectory such that the length
 * of every sub-trajectory is no greater than a given 
 * threshold.
 * 
 * @author uqdalves
 */
@SuppressWarnings("serial")
public class ByLengthSegmentation implements TrajectorySegmentation {
    private final double lenghtThreshold;
    private final PointDistanceFunction distFunc;

    /**
     * Use the default Euclidean distance.
     *
     * @throws IllegalArgumentException
     */
    public ByLengthSegmentation(double lenghtThreshold) {
        if (lenghtThreshold < 0) {
            throw new IllegalArgumentException(
                    "Length Threshold must be positive.");
        }
        this.lenghtThreshold = lenghtThreshold;
        this.distFunc = new EuclideanDistanceFunction();
    }

    /**
     * @param distFunc The point distance function to measure the length.
     * @throws IllegalArgumentException
     */
    public ByLengthSegmentation(double lenghtThreshold, PointDistanceFunction distFunc) {
        if (lenghtThreshold < 0) {
            throw new IllegalArgumentException(
                    "Length Threshold must be positive.");
        }
        this.lenghtThreshold = lenghtThreshold;
        this.distFunc = distFunc;
    }

    @Override
    public List<Trajectory> doSegmentation(Trajectory trajectory) {
        if (trajectory.isEmpty()) {
            throw new EmptyParameterException(
                    "Segmentation error. Trajectory must not be empty.");
        }
        List<Trajectory> result = new ArrayList<Trajectory>();
        Trajectory sub = new Trajectory();
        sub.setParentId(trajectory.getId());
        sub.add(trajectory.get(0));
        double dist = 0.0;
        double length = 0.0;
        for (int i = 1; i < trajectory.size(); i++) {
            STPoint pi = trajectory.get(i);
            STPoint pj = trajectory.get(i - 1);
            dist = distFunc.distance(pi, pj);
            // split
            if (length + dist > lenghtThreshold) {
                result.add(sub);
                sub = new Trajectory();
                sub.setParentId(trajectory.getId());
            }
            sub.add(pi);
            length += dist;
        }
        // add last sub-trajectory
        result.add(sub);

        return result;
    }
}
