package util.function;

import util.exceptions.DistanceFunctionException;
import util.object.spatialobject.Point;
import util.object.spatialobject.Rect;
import util.object.spatialobject.Segment;
import util.object.spatialobject.Vector2D;

import java.text.DecimalFormat;

/**
 * Euclidean Distance function for spatial objects.
 *
 * @author uqdalves, Hellisk
 */
public class EuclideanDistanceFunction implements DistanceFunction, VectorDistanceFunction {
	private final DecimalFormat df = new DecimalFormat(".00000");
	
	/**
	 * Euclidean distance between two spatial points.
	 */
	@Override
	public double distance(Point p1, Point p2) {
		if (p1 == null || p2 == null) {
			throw new NullPointerException("Points for distance calculation cannot be null.");
		}
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
	
	/**
	 * Euclidean distance between two line segments.
	 */
	@Override
	public double distance(Segment s, Segment r) {
		if (s == null || r == null) {
			throw new NullPointerException("Segments for distance calculation cannot be null.");
		}
		return segmentToSegmentDistance(s.x1(), s.x2(), s.y1(), s.y2(), r.x1(), r.x2(), r.y1(), r.y2());
	}
	
	/**
	 * Euclidean distance between two N-dimensional vectors.
	 */
	@Override
	public double distance(double[] v, double[] u) throws DistanceFunctionException {
		if (v == null || u == null) {
			throw new NullPointerException("Vectors for Euclidean distance calculation cannot be null.");
		}
		if (v.length != u.length) {
			throw new DistanceFunctionException("Vectors should be of same size for Jaccard distance calculation.");
		}
		if (v.length == 0) {
			return 0;
		}
		
		double distance = 0;
		for (int i = 0; i < v.length; i++) {
			distance += (v[i] - u[i]) * (v[i] - u[i]);
		}
		
		return Math.sqrt(distance);
	}
	
	/**
	 * Euclidean distance between 2D points.
	 * <br> {@inheritDoc}
	 */
	@Override
	public double pointToPointDistance(double x1, double y1, double x2, double y2) {
		double dist2 = (x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2);
		return Math.sqrt(dist2);
	}
	
	/**
	 * Euclidean projection distance between point and segment.
	 * <br> {@inheritDoc}
	 */
	@Override
	public double pointToSegmentProjectionDistance(double x, double y, double sx1, double sy1, double sx2, double sy2) {
		double xDelta = sx2 - sx1;
		double yDelta = sy2 - sy1;
		
		if ((xDelta == 0) && (yDelta == 0))
			throw new IllegalArgumentException("Segment start equals segment end");
		// triangle height
		double num = (sy2 - sy1) * x - (sx2 - sx1) * y + sx2 * sy1 - sy2 * sx1;
		double den = (sy2 - sy1) * (sy2 - sy1) + (sx2 - sx1) * (sx2 - sx1);
		return Math.abs(num) / Math.sqrt(den);
	}
	
	/**
	 * Euclidean distance between point and segment.
	 * <br> {@inheritDoc}
	 */
	@Override
	public double pointToSegmentProjectionDistance(Point p, Segment s) {
		if (p == null) {
			throw new NullPointerException("Point for distance calculation cannot be null.");
		}
		if (s == null) {
			throw new NullPointerException("Segment for distance calculation cannot be null.");
		}
		return pointToSegmentProjectionDistance(p.x(), p.y(), s.x1(), s.y1(), s.x2(), s.y2());
	}
	
	/**
	 * Euclidean distance between line segments.
	 */
	@Override
	public double segmentToSegmentDistance(double sx1, double sy1, double sx2, double sy2, double rx1, double ry1, double rx2, double ry2) {
		// if they intersect the shortest distance is zero
		if (Segment.segmentsCross(sx1, sy1, sx2, sy2, rx1, ry1, rx2, ry2)) {
			return 0.0;
		}
		Point s1p = getClosestPoint(sx1, sy1, rx1, ry1, rx2, ry2);
		Point s2p = getClosestPoint(sx2, sy2, rx1, ry1, rx2, ry2);
		double s1d = pointToPointDistance(sx1, sy1, s1p.x(), s1p.y());
		double s2d = pointToPointDistance(sx2, sy2, s2p.x(), s2p.y());
		return s1d < s2d ? s1d : s2d;
	}
	
	@Override
	public Point getClosestPoint(Point p, Segment s) {
		return getClosestPoint(p.x(), p.y(), s.x1(), s.y1(), s.x2(), s.y2());
	}
	
	@Override
	public Point getClosestPoint(double x, double y, double sx1, double sy1, double sx2, double sy2) {
		
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
	public Point getProjection(Point p, Segment s) {
		return getProjection(p.x(), p.y(), s.x1(), s.y1(), s.x2(), s.y2());
	}
	
	@Override
	public Point getProjection(double x, double y, double sx1, double sy1, double sx2, double sy2) {
		// segments vector
		double v1x = sx2 - sx1;
		double v1y = sy2 - sy1;
		double v2x = x - sx1;
		double v2y = y - sy1;
		// get squared length of this segment e
		double len2 = (sx2 - sx1) * (sx2 - sx1) + (sy2 - sy1) * (sy2 - sy1);
		if (len2 == 0)
			throw new IllegalArgumentException("Segment start equals segment end");
		// the projection falls where d = [(p - p1) . (p2 - p1)] / |p2 - p1|^2
		double d = Vector2D.dotProduct(v2x, v2y, v1x, v1y) / len2;
		
		// projection is "in between" s.p1 and s.p2 get projection coordinates
		double px = sx1 + d * (sx2 - sx1);
		double py = sy1 + d * (sy2 - sy1);
		
		return new Point(px, py, this);
	}
	
	@Override
	public double getCoordinateOffsetX(double distance, double referenceY) {
		return getCoordinateOffset(distance);
	}
	
	@Override
	public double getCoordinateOffsetY(double distance, double referenceX) {
		return getCoordinateOffset(distance);
	}
	
	@Override
	public double area(Rect rectangle) {
		if (rectangle.maxX() < rectangle.minX() || rectangle.maxY() < rectangle.minY())
			throw new IllegalArgumentException("The input rectangle is illegal: " + rectangle.maxY() + "," + rectangle.minY() + ","
					+ rectangle.maxX() + "," + rectangle.minX());
		return (rectangle.maxY() - rectangle.minY()) * (rectangle.maxX() - rectangle.minX());
	}
	
	// TODO: Check the correctness of the algorithm
	@Override
	public double getHeading(double x1, double y1, double x2, double y2) {
		double diffX = x2 - x1;
		double diffY = y2 - y1;
		return Math.toDegrees(Math.atan(diffY / diffX));
	}
	
	/**
	 * The x and y axes have the same <tt>distance per degree</tt>. Hence, use the same function for calculation.
	 *
	 * @param distance The distance between two points with the same x/y coordinate in meter
	 * @return The actual coordinate offset.
	 */
	private double getCoordinateOffset(double distance) {
		return distance;
	}
}