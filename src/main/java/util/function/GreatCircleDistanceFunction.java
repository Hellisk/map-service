package util.function;

import util.object.spatialobject.Point;
import util.object.spatialobject.Rect;
import util.object.spatialobject.Segment;

import java.text.DecimalFormat;

/**
 * Distance calculation for longitude/latitude coordination system.
 *
 * @author Hellisk
 * @since 31/03/2019
 */
public class GreatCircleDistanceFunction implements DistanceFunction {
	
	private final DecimalFormat df = new DecimalFormat(".00000");
	
	@Override
	public double distance(Point p1, Point p2) {
		return pointToPointDistance(p1.x(), p1.y(), p2.x(), p2.y());
	}
	
	@Override
	public double distance(Point p, Segment s) {
		return distance(p, getClosestPoint(p, s));
	}
	
	@Override
	public double distanceProjection(Point p, Segment s) {
		return pointToSegmentProjectionDistance(p, s);
	}
	
	@Override
	public double distance(Segment s, Segment r) {
		return segmentToSegmentDistance(s.x1(), s.y1(), s.x2(), s.y2(), r.x1(), r.y1(), r.x2(), r.y2());
	}
	
	/**
	 * distance calculator between points. Method adopted from GraphHopper
	 *
	 * @param x1 longitude of the start point
	 * @param y1 latitude of the start point
	 * @param x2 longitude of the end point
	 * @param y2 latitude of the end point
	 * @return distance at metres
	 */
	@Override
	public double pointToPointDistance(double x1, double y1, double x2, double y2) {
		double dLat = Math.toRadians(y2 - y1);
		double dLon = Math.toRadians(x2 - x1);
		// use mean latitude as reference point for delta_lon
		double tmp = Math.cos(Math.toRadians((y1 + y2) / 2)) * dLon;
		double normedDist = dLat * dLat + tmp * tmp;
		return SpatialUtils.EARTH_RADIUS * Math.sqrt(normedDist);
	}
	
	/**
	 * Find the closest point of a given point to the segment, the closest point is either the projection point or one of the endpoint
	 *
	 * @param p 2D point
	 * @param s candidate segment
	 * @return point on s that is closest to p
	 */
	@Override
	public Point getClosestPoint(Point p, Segment s) {
		Point returnPoint = getClosestPoint(p.x(), p.y(), s.x1(), s.y1(), s.x2(), s.y2());
		if (returnPoint.x() == s.x1())
			return s.p1();
		else if (returnPoint.x() == s.x2())
			return s.p2();
		else return returnPoint;
	}
	
	/**
	 * Convert the actual distance into the coordinate offset of longitude. The distance can only represent a vertical distance.
	 *
	 * @param distance   The actual distance in meter
	 * @param referenceY The reference latitude for distance estimation since the <tt>distance per degree</tt> is different at different
	 *                   latitudes
	 * @return the coordinate offset
	 */
	@Override
	public double getCoordinateOffsetX(double distance, double referenceY) {
		double radian = distance / (SpatialUtils.EARTH_RADIUS * Math.cos(Math.toRadians(referenceY)));
		return Math.toDegrees(radian);
	}
	
	/**
	 * Convert the actual distance into the coordinate offset of latitude. The distance can only represent a horizontal distance.
	 *
	 * @param distance   The actual distance in meter.
	 * @param referenceX The reference longitude. However, it is useless as the <tt>distance per degree</tt> is always the same.
	 * @return the coordinate offset
	 */
	@Override
	public double getCoordinateOffsetY(double distance, double referenceX) {
		double radian = distance / SpatialUtils.EARTH_RADIUS;
		return Math.toDegrees(radian);
	}
	
	/**
	 * Return the projection point on segment even it is on the extended line.
	 *
	 * @param p 2D point
	 * @param s candidate segment
	 * @return projection point.
	 */
	@Override
	public Point getProjection(Point p, Segment s) {
		return getProjection(p.x(), p.y(), s.x1(), s.y1(), s.x2(), s.y2());
	}
	
	@Override
	public Point getProjection(double x, double y, double sx1, double sy1, double sx2, double sy2) {
		double xDelta = sx2 - sx1;
		double yDelta = sy2 - sy1;
		
		if ((xDelta == 0) && (yDelta == 0))
			throw new IllegalArgumentException("Segment start equals segment end");
		
		// find the projection point pp.
		// the segment is represented as y= ax + b, while the projection line is x= -ay + m
		double a = sy2 - sy1;
		double b = sx1 - sx2;
		double c = sx2 * sy1 - sx1 * sy2;
		
		double ppx = (b * b * x - a * b * y - a * c) / (a * a + b * b);
		double ppy = (-a * b * x + a * a * y - b * c) / (a * a + b * b);
		
		return new Point(ppx, ppy, this);
	}
	
	public Point getProjection2(double x, double y, double sx1, double sy1, double sx2, double sy2) {
		
		double dx = sx1 - sx2;
		double dy = sy1 - sy2;
		
		double u = (x - sx1) * dx + (y - sy1) * dy;
		u /= dx * dx + dy * dy;
		
		double ppx = sx1 + u * dx;
		double ppy = sy1 + u * dy;
		
		return new Point(ppx, ppy, this);
	}
	
	@Override
	public Point getClosestPoint(double x, double y, double sx1, double sy1, double sx2, double sy2) {
		
		// find the projection point pp
		Point pp = getProjection(x, y, sx1, sy1, sx2, sy2);
		double ppx = pp.x();
		double ppy = pp.y();
		
		// check whether the projection point is outside the segment
		if (sx1 < sx2) {
			if (ppx < sx1) {
				ppx = sx1;
				ppy = sy1;
			} else if (ppx > sx2) {
				ppx = sx2;
				ppy = sy2;
			}
		} else if (Double.isNaN(ppx) || Double.isNaN(ppy)) {
			ppx = sx1;
			ppy = sy1;
		} else {
			if (ppx < sx2) {
				ppx = sx2;
				ppy = sy2;
			} else if (ppx > sx1) {
				ppx = sx1;
				ppy = sy1;
			}
		}
		double pointX = 0;
		double pointY = 0;
		try {
			pointX = Double.parseDouble(df.format(ppx));
			pointY = Double.parseDouble(df.format(ppy));
		} catch (NumberFormatException e) {
			e.printStackTrace();
		}
		return new Point(pointX, pointY, this);
	}
	
	@Override
	public double pointToSegmentProjectionDistance(double x, double y, double sx1, double sy1, double sx2, double sy2) {
		double xDelta = sx2 - sx1;
		double yDelta = sy2 - sy1;
		
		if ((xDelta == 0) && (yDelta == 0))
			throw new IllegalArgumentException("Segment start equals segment end");
		
		Point ppPoint = getProjection(x, y, sx1, sy1, sx2, sy2);
		
		return pointToPointDistance(x, y, ppPoint.x(), ppPoint.y());
	}
	
	@Override
	public double pointToSegmentProjectionDistance(Point p, Segment s) {
		return pointToSegmentProjectionDistance(p.x(), p.y(), s.x1(), s.y1(), s.x2(), s.y2());
	}
	
	@Override
	public double segmentToSegmentDistance(double sx1, double sy1, double sx2, double sy2, double rx1, double ry1, double rx2, double ry2) {
		double xDelta = rx2 - rx1;
		double yDelta = ry2 - ry1;
		
		if ((xDelta == 0) && (yDelta == 0))
			throw new IllegalArgumentException("Segment start equals segment end");
		
		if (Segment.segmentsCross(sx1, sy1, sx2, sy2, rx1, ry1, rx2, ry2)) {
			return 0.0;
		}
		Point s1p = getClosestPoint(sx1, sy1, rx1, ry1, rx2, ry2);
		Point s2p = getClosestPoint(sx2, sy2, rx1, ry1, rx2, ry2);
		double s1d = pointToPointDistance(sx1, sy1, s1p.x(), s1p.y());
		double s2d = pointToPointDistance(sx2, sy2, s2p.x(), s2p.y());
		return s1d < s2d ? s1d : s2d;
	}
	
	// TODO test the correctness. Seems wrong currently.
	@Override
	public double area(Rect rectangle) {
		double totalAngle = 0;
		// calculate total inner angle
		Point p1 = new Point(rectangle.minX(), rectangle.minY(), rectangle.getDistanceFunction());
		Point p2 = new Point(rectangle.minX(), rectangle.maxY(), rectangle.getDistanceFunction());
		Point p3 = new Point(rectangle.maxX(), rectangle.maxY(), rectangle.getDistanceFunction());
		Point p4 = new Point(rectangle.maxX(), rectangle.minY(), rectangle.getDistanceFunction());
		totalAngle += getAngle(p1, p2, p3);
		totalAngle += getAngle(p2, p3, p4);
		totalAngle += getAngle(p3, p4, p1);
		totalAngle += getAngle(p4, p1, p2);
		double sphericalExcess = totalAngle - 360;
		if (sphericalExcess > 420.0) {
			totalAngle = 4 * 360.0 - totalAngle;
			sphericalExcess = totalAngle - 360;
		} else if (sphericalExcess > 300.0 && sphericalExcess < 420.0) {
			sphericalExcess = Math.abs(360.0 - sphericalExcess);
		}
		return Math.toRadians(sphericalExcess) * SpatialUtils.EARTH_RADIUS * SpatialUtils.EARTH_RADIUS;
	}
	
	/**
	 * Calculate the angle between two segments (P2,P1) and (P2,P3)
	 *
	 * @param p1 Endpoint of first segment.
	 * @param p2 Intersect point.
	 * @param p3 Endpoint of second segment.
	 */
	public double getAngle(Point p1, Point p2, Point p3) {
		double bearing21 = getHeading(p2.x(), p2.y(), p1.x(), p1.y());
		double bearing23 = getHeading(p2.x(), p2.y(), p3.x(), p3.y());
		var angle = bearing21 - bearing23;
		if (angle < 0) {
			angle += 360;
		}
		return angle;
	}
	
	@Override
	public double getHeading(double x1, double y1, double x2, double y2) {
		double lon1 = Math.toRadians(x1);
		double lat1 = Math.toRadians(y1);
		double lon2 = Math.toRadians(x2);
		double lat2 = Math.toRadians(y2);
		double headingRadians = Math.atan2(Math.asin(lon2 - lon1) * Math.cos(lat2),
				Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(lon2 - lon1));
		if (headingRadians < 0)
			headingRadians += Math.PI * 2.0;
		return Math.toDegrees(headingRadians);
	}
}