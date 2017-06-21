package traminer.util.trajectory.segmentation;

import traminer.util.exceptions.EmptyTrajectoryException;
import traminer.util.spatial.distance.EuclideanDistanceFunction;
import traminer.util.spatial.distance.PointDistanceFunction;
import traminer.util.spatial.objects.st.STPoint;
import traminer.util.trajectory.Trajectory;

import java.util.ArrayList;
import java.util.List;

/**
 * Segments a trajectory if two consecutive points have
 * a distance gap greater than a given threshold.
 *
 * @author uqdalves
 */
@SuppressWarnings("serial")
public class MaxDistanceGapSegmentation extends TrajectorySegmentation {
    private final double maximumGap;
    private final PointDistanceFunction distanceFunction;

    /**
     * Use the default Euclidean distance function.
     *
     * @param maximumGap Maximum distance gap allowed
     * @throws IllegalArgumentException
     */
    public MaxDistanceGapSegmentation(double maximumGap) {
        if (maximumGap <= 0) {
            throw new IllegalArgumentException(
                    "Distance Gap Threshold must be positive.");
        }
        this.maximumGap = maximumGap;
        this.distanceFunction = new EuclideanDistanceFunction();
    }

    /**
     * @param maximumGap Maximum distance gap allowed
     * @param distFunc   The point distance function to use.
     * @throws IllegalArgumentException
     */
    public MaxDistanceGapSegmentation(int maximumGap,
                                      PointDistanceFunction distFunc) {
        if (maximumGap <= 0) {
            throw new IllegalArgumentException(
                    "Distance Gap Threshold must be positive.");
        }
        this.maximumGap = maximumGap;
        this.distanceFunction = distFunc;
    }

    @Override
    public List<Trajectory> doSegmentation(Trajectory trajectory) {
        if (trajectory.isEmpty()) {
            throw new EmptyTrajectoryException(
                    "Segmentation error. Trajectory must not be empty.");
        }
        List<Trajectory> result = new ArrayList<Trajectory>();
        Trajectory sub = new Trajectory();
        sub.setParentId(trajectory.getId());
        sub.add(trajectory.get(0));
        double gap = 0.0;
        for (int i = 1; i < trajectory.size(); i++) {
            STPoint pi = trajectory.get(i);
            STPoint pj = trajectory.get(i - 1);
            gap = distanceFunction.pointToPointDistance(pi, pj);
            // split
            if (gap > maximumGap) {
                result.add(sub);
                sub = new Trajectory();
                sub.setParentId(trajectory.getId());
            }
            sub.add(pi);
        }
        // add last sub-trajectory
        result.add(sub);

        return result;
    }

}
