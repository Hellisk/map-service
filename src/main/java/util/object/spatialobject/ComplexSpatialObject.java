package util.object.spatialobject;

import com.vividsolutions.jts.geom.Coordinate;
import org.checkerframework.checker.nullness.qual.NonNull;
import util.function.DistanceFunction;

import java.util.ArrayList;
import java.util.List;

/**
 * Base superclass for complex spatial objects.
 * <p>
 * Complex spatial objects are made of a list of simple spatial objects.
 *
 * @author uqdalves, Hellisk
 */
public abstract class ComplexSpatialObject<T extends SimpleSpatialObject>
		extends ArrayList<T> implements SpatialObject {
	private final DistanceFunction distFunc;
	/**
	 * Spatial object identifier.
	 */
	private String oid = null;
	/**
	 * Number of spatial dimension of this object (2 by default).
	 */
	private byte dimension = 2;
	
	/**
	 * Constructs an empty complex spatial object.
	 */
	public ComplexSpatialObject(DistanceFunction df) {
		super(1);
		this.distFunc = df;
	}
	
	/**
	 * Constructs an empty complex spatial object with the specified initial capacity.
	 *
	 * @param initialCapacity The initial capacity.
	 */
	public ComplexSpatialObject(int initialCapacity, DistanceFunction df) {
		super(initialCapacity);
		this.distFunc = df;
	}
	
	/**
	 * Constructs an empty complex spatial object with the given id.
	 *
	 * @param id Spatial object identifier.
	 */
	public ComplexSpatialObject(String id, DistanceFunction df) {
		this.oid = id;
		this.distFunc = df;
	}
	
	@Override
	public String getID() {
		return oid;
	}
	
	@Override
	public void setID(String id) {
		this.oid = id;
	}
	
	@Override
	public byte getDimension() {
		return dimension;
	}
	
	@Override
	public void setDimension(byte dim) {
		if (dim < 1) {
			throw new IllegalArgumentException("Number of spatial dimensions must be positive [1..127].");
		}
		this.dimension = dim;
	}
	
	@NonNull
	@Override
	public DistanceFunction getDistanceFunction() {
		return distFunc;
	}
	
	@Override
	public abstract ComplexSpatialObject<T> clone();
	
	/**
	 * Check whether these two complex spatial objects are adjacent (i.e. If they share any edge).
	 *
	 * @param obj Object to check.
	 * @return True if these two spatial objects are adjacent.
	 */
	public boolean isAdjacent(ComplexSpatialObject<SimpleSpatialObject> obj) {
		if (obj == null) {
			throw new NullPointerException("Spatial object to check adjacency must not be null.");
		}
		for (Segment e1 : this.getEdges()) {
			for (Segment e2 : obj.getEdges()) {
				if (e1.equals2D(e2)) {
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * @return The centroid/center of this complex spatial object.
	 */
	public Point centroid() {
		double x = 0, y = 0;
		for (Point p : this.getCoordinates()) {
			x += p.x();
			y += p.y();
		}
		x = x / (size() - 1);
		y = y / (size() - 1);
		
		return new Point(x, y, distFunc);
	}
	
	@Override
	public Rect mbr() {
		if (!this.isEmpty()) {
			double minX = Double.POSITIVE_INFINITY, maxX = Double.NEGATIVE_INFINITY;
			double minY = Double.POSITIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
			for (Point p : this.getCoordinates()) {
				if (p.x() > maxX) maxX = p.x();
				if (p.x() < minX) minX = p.x();
				if (p.y() > maxY) maxY = p.y();
				if (p.y() < minY) minY = p.y();
			}
			return new Rect(minX, minY, maxX, maxY, distFunc);
		}
		return new Rect(0.0, 0.0, 0.0, 0.0, distFunc);
	}
	
	/**
	 * Computes the smallest convex Polygon that contains
	 * all the points in the Spatial Object.
	 *
	 * @return The convex hull of this spatial object.
	 */
	public Polygon convexHull() {
		Coordinate[] coordList = this.toJTSGeometry().
				convexHull().getCoordinates();
		Polygon convexHull = new Polygon(distFunc);
		for (Coordinate c : coordList) {
			convexHull.add(c.x, c.y);
		}
		
		return convexHull;
	}
	
	/**
	 * Computes the smallest convex Polygon that contains
	 * all the points in the Spatial Object.
	 *
	 * @return The convex hull of this spatial object
	 * represented as a list of coordinate points.
	 */
	public List<Point> convexHullCoordinates() {
		Coordinate[] coordList = this.toJTSGeometry().
				convexHull().getCoordinates();
		List<Point> convexHull = new ArrayList<>();
		for (Coordinate c : coordList) {
			convexHull.add(new Point(c.x, c.y, distFunc));
		}
		
		return convexHull;
	}
}
