package traminer.util.spatial.distance;

import traminer.util.exceptions.DistanceFunctionException;
import traminer.util.spatial.SpatialInterface;
import traminer.util.spatial.objects.Point;
import traminer.util.spatial.objects.Segment;

/**
 * Interface for distance functions between spatial segments,
 * and between points and segments.
 * 
 * @author uqdalves
 */
public interface SegmentDistanceFunction extends SpatialInterface {

    /**
     * Distance between two line segments (shortest distance).
     *
     * @param s 2D line segment S.
     * @param r 2D line segment R.
     * @return Distance between S and R.
     * @throws DistanceMeasurementException
     */
    double distance(Segment s, Segment r) throws DistanceFunctionException;

    /**
     * Distance between a spatial point and a segment
     * (shortest distance) given by their coordinates.
     *
     * @param x The point X coordinate.
     * @param y The point Y coordinate.
     * @param sx1 The segment's start X coordinate.
     * @param sy1 The segment's start Y coordinate.
     * @param sx2 The segment's end X coordinate.
     * @param sy2 The segment's end Y coordinate.
     *
     * @return The distance between the point (x1, y1) and
     * 			the segment (sx1, sy1)(sx2, sy2).
     *
     * @throws DistanceMeasurementException
     */
    double pointToSegmentDistance(
            double x, double y, double sx1, double sy1, double sx2, double sy2)
            throws DistanceFunctionException;


    /**
     * Distance between a spatial point and a segment (shortest distance).
     *
     * @param p
     * @param s
     *
     * @return The distance between the point p and the segment s.
     *
     * @throws DistanceMeasurementException
     */
    double pointToSegmentDistance(Point p, Segment s)
            throws DistanceFunctionException;

    /**
     * Distance between two line segments S and R given by
     * their coordinates.
     *
     * @param sx1
     * @param sy1
     * @param sx2
     * @param sy2
     * @param rx1
     * @param ry1
     * @param rx2
     * @param ry2
     *
     * @return The distance between line segments S (sx1, sy1)(sx2, sy2)
     * 		and R (rx1, sry1)(rx2, ry2).
     *
     * @throws DistanceMeasurementException
     */
    double segmentToSegmentDistance(
            double sx1, double sy1, double sx2, double sy2,
            double rx1, double ry1, double rx2, double ry2)
            throws DistanceFunctionException;
}
