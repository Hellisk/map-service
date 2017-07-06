package traminer.util.trajectory.distance;

import traminer.util.spatial.SpatialInterface;
import traminer.util.spatial.distance.PointDistanceFunction;
import traminer.util.spatial.objects.Point;
import traminer.util.spatial.objects.Segment;
import traminer.util.spatial.objects.st.STPoint;
import traminer.util.spatial.objects.st.STSegment;
import traminer.util.trajectory.Trajectory;

import java.util.ArrayList;
import java.util.List;

/**
 * Base interface for distance functions between trajectories.
 * 
 * @author uqdalves
 *
 */
@SuppressWarnings("serial")
public abstract class TrajectoryDistanceFunction implements SpatialInterface {
    /**
     * The points distance measure to use
     **/
    protected PointDistanceFunction distFunc = null;

    /**
     * @param distFunc The points distance measure to use
     */
    protected TrajectoryDistanceFunction(PointDistanceFunction distFunc) {
        this.distFunc = distFunc;
    }

    protected TrajectoryDistanceFunction() {
    }

    /**
     * Distance between two trajectories.
     */
    public abstract double getDistance(final Trajectory t1, final Trajectory t2);

    /**
     * Return a polyline (list of segments) for the given
     * list of points.
     */
    protected List<Segment> getPolyline(List<? extends Point> list) {
        List<Segment> polyline = new ArrayList<Segment>();
        if (list.size() < 2) {
            return polyline;
        }
        Segment tempLine;
        Point pi, pj;
        for (int i = 0; i < list.size() - 1; i++) {
            pi = list.get(i);
            pj = list.get(i + 1);
            tempLine = new Segment(pi.x(), pi.y(), pj.x(), pj.y());
            polyline.add(tempLine);
        }
        return polyline;
    }

    /**
     * Return a spatial-temporal polyline (list of segments with time-stamped
     * points) for the given list of spatial-temporal points.
     */
    protected List<STSegment> getSTPolyLine(List<STPoint> r) {
        List<STSegment> result = new ArrayList<STSegment>();
        if (r.size() < 2) {
            return result;
        }
        STSegment tempLine;
        for (int i = 0; i < r.size() - 1; i++) {
            tempLine = new STSegment(r.get(i), r.get(i + 1));
            result.add(tempLine);
        }

        return result;
    }
}
