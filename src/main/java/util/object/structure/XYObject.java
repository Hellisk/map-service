package util.object.structure;

import org.apache.log4j.Logger;
import util.object.spatialobject.Point;
import util.object.spatialobject.SpatialObject;

import java.util.Comparator;

/**
 * An immutable container for a simpler representation of any spatial object as a 2D (x,y) coordinate point. This object is mainly useful
 * for indexing and query purposes.
 *
 * @param <T> Type of spatial object in this container.
 * @author uqdalves, Hellisk
 */
public final class XYObject<T extends SpatialObject> {
	
	/**
	 * Comparator to compare XYObjects by their X value.
	 */
	public static final Comparator<XYObject> X_COMPARATOR = Comparator.comparingDouble(o -> o.x);
	/**
	 * Comparator to compare XYObjects by their Y value.
	 */
	public static final Comparator<XYObject> Y_COMPARATOR = Comparator.comparingDouble(o -> o.y);
	private static final Logger LOG = Logger.getLogger(XYObject.class);
	/**
	 * Object (x,y) coordinates
	 */
	private final double x, y;
	/**
	 * The spatial object in this container
	 */
	private final T spatialObject;
	
	/**
	 * Creates a immutable container for a simpler representation of a spatial object as a 2D (x,y) coordinate point.
	 *
	 * @param x          The X coordinate representing this object.
	 * @param y          The Y coordinate representing this object.
	 * @param spatialObj The spatial object to wrap up in this container.
	 */
	public XYObject(double x, double y, T spatialObj) {
		this.x = x;
		this.y = y;
		this.spatialObject = spatialObj;
	}
	
	/**
	 * Creates a immutable container for a simpler representation of a spatial object as a 2D (x,y) coordinate point.
	 *
	 * @param x The X coordinate representing this object.
	 * @param y The Y coordinate representing this object.
	 */
	public XYObject(double x, double y) {
		this.x = x;
		this.y = y;
		this.spatialObject = null;
	}
	
	/**
	 * @return The X coordinate representing this object.
	 */
	public double x() {
		return x;
	}
	
	/**
	 * @return The Y coordinate representing this object.
	 */
	public double y() {
		return y;
	}
	
	/**
	 * @return The spatial object in this container.
	 */
	public T getSpatialObject() {
		return spatialObject;
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
	
	/**
	 * Check whether these two spatial objects have the same (x,y) spatial coordinates.
	 *
	 * @param obj The spatial object to compare.
	 * @return True if these two spatial objects have the same spatial coordinates.
	 */
	public boolean equals2D(XYObject<T> obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		return (x == obj.x && y == obj.y);
	}
	
	@Override
	public String toString() {
		return String.format("%.5f %.5f", x, y);
	}
	
	/**
	 * Print this XYObject to the system output.
	 */
	public void print() {
		LOG.info("XYOBJECT ( " + toString() + " )");
	}
	
	/**
	 * @return The 2D point representation of the spatial object in this container.
	 */
	public Point toPoint() {
		if (spatialObject != null) {
			return new Point(x, y, spatialObject.getDistanceFunction());
		} else
			throw new IllegalArgumentException("The object has no distance function defined.");
	}
}