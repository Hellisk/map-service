package traminer.util.spatial.distance;

import traminer.util.exceptions.DistanceFunctionException;
import traminer.util.spatial.SpatialInterface;
import traminer.util.spatial.objects.Point;

/**
 * Interface for distance functions between spatial points.
 * 
 * @author uqdalves
 */
public interface PointDistanceFunction extends SpatialInterface {

    /**
     * Distance between two spatial points.
     *
     * @param p1 Spatial point P1.
     * @param p2 Spatial point P2.
     * @return Distance between P1 and P2.
     * @throws DistanceFunctionException
     */
    double distance(Point p1, Point p2)
            throws DistanceFunctionException;

    /**
     * Distance between two spatial points given by their coordinates.
     *
     * @param x1
     * @param y1
     * @param x2
     * @param y2
     * @return The distance between points (x1, y1) and (x2, y2).
     * @throws DistanceFunctionException
     */
    double pointToPointDistance(double x1, double y1, double x2, double y2)
            throws DistanceFunctionException;
}
