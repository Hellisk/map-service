package traminer.util.spatial.distance;

import traminer.util.spatial.SpatialInterface;
import traminer.util.spatial.objects.Point;

import java.io.Serializable;

/**
 * Interface for distance functions between spatial points.
 *
 * @author uqdalves
 */
public interface PointDistanceFunction extends SpatialInterface, Serializable {
    /**
     * Distance between two spatial points given by their coordinates.
     *
     * @param x1
     * @param y1
     * @param x2
     * @param y2
     * @return The distance between points (x1, y1) and (x2, y2).
     */
    double pointToPointDistance(double x1, double y1, double x2, double y2);

    /**
     * Distance between two spatial points.
     *
     * @param p1
     * @param p2
     * @return The distance between points p1 and p2.
     */
    double pointToPointDistance(Point p1, Point p2);
}
