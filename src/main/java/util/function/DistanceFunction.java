package util.function;

import util.object.spatialobject.Point;
import util.object.spatialobject.Rect;
import util.object.spatialobject.Segment;

/**
 * Interface for distance functions between spatial segments, and between points and segments.
 *
 * @author Hellisk
 */
public interface DistanceFunction {
	
	/**
	 * Distance between two spatial points.
	 *
	 * @param p1 Spatial point P1.
	 * @param p2 Spatial point P2.
	 * @return Distance between P1 and P2.
	 */
	double distance(Point p1, Point p2);
	
	/**
	 * Distance from the point to the closest point on a line segment.
	 *
	 * @param p Spatial point P1.
	 * @param s 2D line segment s.
	 * @return Distance between P1 and P2.
	 */
	double distance(Point p, Segment s);
	
	/**
	 * Projection distance between a point and a line segment.
	 *
	 * @param p Spatial point P1.
	 * @param s 2D line segment s.
	 * @return Distance between P1 and P2.
	 */
	double distanceProjection(Point p, Segment s);
	
	/**
	 * Distance between two line segments without extension.
	 *
	 * @param s 2D line segment S.
	 * @param r 2D line segment R.
	 * @return Distance between S and R.
	 */
	double distance(Segment s, Segment r);
	
	/**
	 * Distance between two spatial points given by their coordinates.
	 *
	 * @return The distance between points (x1, y1) and (x2, y2).
	 */
	double pointToPointDistance(double x1, double y1, double x2, double y2);
	
	/**
	 * Distance between a spatial point and a segment (shortest distance) given by their coordinates.
	 *
	 * @param x   The point X coordinate.
	 * @param y   The point Y coordinate.
	 * @param sx1 The segment's start X coordinate.
	 * @param sy1 The segment's start Y coordinate.
	 * @param sx2 The segment's end X coordinate.
	 * @param sy2 The segment's end Y coordinate.
	 * @return The distance between the point (x1, y1) and the segment (sx1, sy1)(sx2, sy2).
	 */
	double pointToSegmentProjectionDistance(double x, double y, double sx1, double sy1, double sx2, double sy2);
	
	/**
	 * Distance between a spatial point and a segment (shortest distance).
	 *
	 * @param p The given point
	 * @param s The projection segment
	 * @return The distance between the point p and the segment s.
	 */
	double pointToSegmentProjectionDistance(Point p, Segment s);
	
	/**
	 * Distance between two line segments S and R given by their coordinates. Both segments are not extendable.
	 *
	 * @return The distance between line segments S (sx1, sy1)(sx2, sy2) and R (rx1, sry1)(rx2, ry2).
	 */
	double segmentToSegmentDistance(double sx1, double sy1, double sx2, double sy2, double rx1, double ry1, double rx2, double ry2);
	
	/**
	 * Find the closest point of a given point to the segment, the closest point is either the perpendicular point or one of the endpoint.
	 *
	 * @param p 2D point
	 * @param s candidate segment
	 * @return point on s that is closest to p
	 */
	Point getClosestPoint(Point p, Segment s);
	
	Point getClosestPoint(double x, double y, double sx1, double sy1, double sx2, double sy2);
	
	/**
	 * Return the projection point on segment even it is on the extended line.
	 *
	 * @param p 2D point
	 * @param s candidate segment
	 * @return projection point.
	 */
	Point getProjection(Point p, Segment s);
	
	Point getProjection(double x, double y, double sx1, double sy1, double sx2, double sy2);
	
	/**
	 * Convert the actual distance into the coordinate offset of <tt>x</tt>. The distance can only represent a vertical distance.
	 *
	 * @param distance   The actual distance in meter.
	 * @param referenceY The reference <tt>y</tt>, which can be useful in some of the coordination system.
	 * @return the coordinate offset
	 */
	double getCoordinateOffsetX(double distance, double referenceY);
	
	/**
	 * Convert the actual distance into the coordinate offset of <tt>y</tt>. The distance can only represent a horizontal distance.
	 *
	 * @param distance   The actual distance in meter.
	 * @param referenceX The reference <tt>x</tt>, which can be useful in some of the coordination system.
	 * @return the coordinate offset
	 */
	double getCoordinateOffsetY(double distance, double referenceX);
	
	/**
	 * Calculate the area of a rectangle.
	 *
	 * @param rectangle The given rectangle.
	 * @return The area of the rectangle.
	 */
	double area(Rect rectangle);
	
	/**
	 * Calculate the heading of a segment or two points.
	 *
	 * @param x1 The x axis of the starting point.
	 * @param y1 The y axis of the starting point.
	 * @param x2 The x axis of the ending point.
	 * @param y2 The y axis of the ending point.
	 * @return The heading represented by a degree number.
	 */
	double getHeading(double x1, double y1, double x2, double y2);
}
