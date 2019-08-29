package util.object.spatialobject;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.impl.PackedCoordinateSequence;
import org.apache.log4j.Logger;
import org.checkerframework.checker.nullness.qual.NonNull;
import util.function.DistanceFunction;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Implements a simple 2D point entity, with (x,y) coordinates.
 * <p>
 * Point objects may contain both spatial and semantic attributes. Spatial attributes of simple objects, however, are immutable, that
 * means once a Point object is created its spatial attributes cannot be changed.
 *
 * @author uqdalves, Hellisk
 */
public class Point extends SimpleSpatialObject {
	
	/**
	 * Comparator to compare points by their X value.
	 */
	public static final Comparator<Point> X_COMPARATOR = Comparator.comparingDouble(o -> o.x);
	/**
	 * Comparator to compare points by their Y value.
	 */
	public static final Comparator<Point> Y_COMPARATOR = Comparator.comparingDouble(o -> o.y);
	private static final Logger LOG = Logger.getLogger(Point.class);
	/**
	 * Point coordinates
	 */
	private double x;
	private double y;
	private DistanceFunction distFunc;
	/**
	 * Auxiliary point from JTS old version
	 */
	private com.vividsolutions.jts.geom.Point JTSPoint = null;
	
	/**
	 * Creates an empty Point with default (0,0) coordinates.
	 */
	public Point(DistanceFunction df) {
		this.x = 0.0;
		this.y = 0.0;
		this.distFunc = df;
	}
	
	/**
	 * Creates a 2D point with the given coordinates.
	 *
	 * @param x Point X/Longitude coordinate.
	 * @param y Point Y/Latitude coordinate.
	 */
	public Point(double x, double y, DistanceFunction df) {
		this.x = x;
		this.y = y;
		this.distFunc = df;
	}
	
	/**
	 * @return Point X coordinate.
	 */
	public double x() {
		return x;
	}
	
	/**
	 * @return Point Y coordinate.
	 */
	public double y() {
		return y;
	}
	
	/**
	 * Reset the point and set kn kn new
	 *
	 * @param x  Nex X coordinate.
	 * @param y  New Y coordinate.
	 * @param df New distance function.
	 */
	public void setPoint(double x, double y, DistanceFunction df) {
		this.x = x;
		this.y = y;
		this.distFunc = df;
	}
	
	/**
	 * @return Array containing the [x,y] coordinates of this point.
	 */
	public double[] getCoordinate() {
		return new double[]{x, y};
	}
	
	@Override
	public List<Point> getCoordinates() {
		ArrayList<Point> list = new ArrayList<>(1);
		list.add(this);
		return list;
	}
	
	@Override
	public List<Segment> getEdges() {
		return new ArrayList<>(0);
	}
	
	@NonNull
	@Override
	public DistanceFunction getDistanceFunction() {
		return this.distFunc;
	}
	
	@Override
	public boolean isClosed() {
		return false;
	}
	
	//@Override
	public boolean touches(SpatialObject obj) {
		if (obj == null) {
			return false;
		}
		if (obj instanceof Point) {
			return false;
		}
		if (obj instanceof Segment) {
			return ((Segment) obj).touches(this);
		}
		if (obj instanceof Circle) {
			return ((Circle) obj).touches(this);
		}
		for (Segment s : obj.getEdges()) {
			if (s.touches(this)) {
				return true;
			}
		}
		return false;
	}
	
	@Override
	public Point clone() {
		Point clone = new Point(x, y, distFunc);
		super.cloneTo(clone); // clone semantics
		return clone;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(x);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(y);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}
	
	@Override
	public boolean equals2D(SpatialObject obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (obj instanceof Point) {
			Point p = (Point) obj;
			return p.x == x && p.y == y;
		}
		return false;
	}
	
	/**
	 * Convert this point object to a AWT Point2D object.
	 *
	 * @return The Point2D representation of this point.
	 */
	public Point2D toPoint2D() {
		return new Point2D.Double(x, y);
	}
	
	@Override
	public String toString() {
		return String.format("%.5f %.5f", x, y);
	}
	
	@Override
	public void print() {
		LOG.info("POINT (" + toString() + ")");
	}
	
	@Override
	public Geometry toJTSGeometry() {
		if (JTSPoint == null) {
			PackedCoordinateSequence.Double coord = new PackedCoordinateSequence.Double(this.getCoordinate(), 2);
			JTSPoint = new com.vividsolutions.jts.geom.Point(coord, new GeometryFactory());
		}
		return JTSPoint;
	}
	
	/**
	 * Compares these two points for order using the given comparator.
	 *
	 * @param p          The point to compare to.
	 * @param comparator The point comparator to use.
	 * @return Returns a negative integer, zero, or a positive integer as this point is less than, equal to, or greater than the given
	 * point p.
	 */
	public int compareTo(Point p, Comparator<Point> comparator) {
		if (p == null) {
			throw new NullPointerException("Point for compareTo must not be null.");
		}
		if (comparator == null) {
			throw new NullPointerException("Point comparator must not be null.");
		}
		return comparator.compare(this, p);
	}
}
