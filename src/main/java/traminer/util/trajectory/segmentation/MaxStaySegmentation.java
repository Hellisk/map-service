package traminer.util.trajectory.segmentation;

import traminer.util.exceptions.EmptyParameterException;
import traminer.util.spatial.distance.EuclideanDistanceFunction;
import traminer.util.spatial.distance.PointDistanceFunction;
import traminer.util.spatial.objects.st.STPoint;
import traminer.util.trajectory.Trajectory;

import java.util.ArrayList;
import java.util.List;

/**
 * Segments a trajectory if the moving object moves within
 * a small region (stationary) for a period longer than a
 * given time threshold.
 * <p>
 * That is, if the moving object moved for a distance with 
 * length less than the given distance threshold, and during
 * a period of time greater than the time threshold, then the
 * object is taken as stationary, and its trajectory is split.
 * 
 * @author uqdalves
 */
@SuppressWarnings("serial")
// TODO
class MaxStaySegmentation implements TrajectorySegmentation {
    // maximum stay period allowed in milliseconds (default value is 3 minutes)
    private final long timeThreshold;
    private final double distanceThreshold;
    private final PointDistanceFunction distanceFunction;
    // remove the intermediate points?
    /**
     * Use the default Euclidean distance function.
     *
     * @param maxStayTime
     * @param maxDistanceThreshold
     */
    public MaxStaySegmentation(long timeThreshold, double distanceThreshold) {
        if (timeThreshold <= 0) {
            throw new IllegalArgumentException(
                    "Maximun Stay Time threshold must be positive.");
        }
        if (distanceThreshold < 0) {
            throw new IllegalArgumentException(
                    "Distance threshold must be greater than or equals to zero.");
        }
        this.timeThreshold = timeThreshold;
        this.distanceThreshold = distanceThreshold;
        this.distanceFunction = new EuclideanDistanceFunction();
    }

    /**
     * @param maxStayTime
     * @param distanceThreshold
     * @param distFunc
     */
    public MaxStaySegmentation(long timeThreshold, double distanceThreshold,
                               PointDistanceFunction distFunc) {
        if (timeThreshold <= 0) {
            throw new IllegalArgumentException(
                    "Maximun Stay Time threshold must be positive.");
        }
        if (distanceThreshold < 0) {
            throw new IllegalArgumentException(
                    "Distance threshold must be greater than or equals to zero.");
        }
        this.timeThreshold = timeThreshold;
        this.distanceThreshold = distanceThreshold;
        this.distanceFunction = distFunc;
    }

    // TODO
    @Override
    public List<Trajectory> doSegmentation(Trajectory trajectory) {
        if (trajectory.isEmpty()) {
            throw new EmptyParameterException(
                    "Segmentation error. Trajectory must not be empty.");
        }

        boolean isTime = false, isDist = false;
        boolean isStationary = false;

        double distance, length = 0.0;
        long time, duration = 0;

        STPoint pi, pj;
        STPoint pIni, pEnd;
        List<STPoint> cluster = new ArrayList<>();


        List<Trajectory> result = new ArrayList<Trajectory>();
        Trajectory sub = new Trajectory();
        sub.setParentId(trajectory.getId());
        sub.add(trajectory.get(0));
        for (int i = 1; i < trajectory.size(); i++) {
            pi = trajectory.get(i - 1);
            pj = trajectory.get(i);
            // time and distance between these two points
            time = pj.time() - pi.time();
            distance = distanceFunction.distance(pi, pj);
            length += distance;
            duration += time;
            // check stationary
            isDist = (length <= distanceThreshold);
            isTime = (duration > timeThreshold);

            if (!isDist) {
                sub.add(pj);
            }


            if (isDist) cluster.add(pi);
            // no longer stationary
            if (isStationary && (!isDist || !isTime)) {
                // split here
            }
            isStationary = (isDist && isTime);


            // not stationary
            if (!isDist && !isTime) {
                pIni = pj;
                length = 0.0;
                duration = 0;
            }
        }
        // distancia menor, tempo menor


        return null;
    }
}
