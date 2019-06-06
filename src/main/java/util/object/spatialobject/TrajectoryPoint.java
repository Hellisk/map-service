package util.object.spatialobject;

import org.apache.log4j.Logger;
import org.checkerframework.checker.nullness.qual.NonNull;
import util.function.DistanceFunction;

import java.util.Comparator;

/**
 * Implements a simple Spatial-Temporal point (point with time-stamp), with (x,y) coordinates and time-stamp.
 * <p>
 * TrajectoryPoint objects may contain both spatial-temporal and semantic attributes. Spatial-temporal attributes of ST objects, however,
 * are immutable, that means once a TrajectoryPoint object is created its spatial attributes cannot be changed.
 *
 * @author uqpchao
 */
public class TrajectoryPoint extends Point implements SpatialTemporalObject {
	
	private static final Logger LOG = Logger.getLogger(TrajectoryPoint.class);
	
	private final long time;        // timestamps
	private final double speed;        // the instantaneous speed of the object at the current time
	private final double heading;    // the heading of trajectory point ranging between -179~180. Degree 0 = (1,0)
	private DistanceFunction distFunc;
	
	/**
	 * Create a new empty point with zero time stamp.
	 */
	public TrajectoryPoint(DistanceFunction df) {
		super(0, 0, df);
		this.time = Long.MIN_VALUE;
		this.speed = Double.NEGATIVE_INFINITY;
		this.heading = Double.NEGATIVE_INFINITY;
		this.distFunc = df;
	}
	
	/**
	 * Create a new point with the given coordinates and zero time stamp.
	 *
	 * @param x Point X coordinate.
	 * @param y Point Y coordinate.
	 */
	public TrajectoryPoint(double x, double y, DistanceFunction df) {
		super(x, y, df);
		this.time = Long.MIN_VALUE;
		this.speed = Double.NEGATIVE_INFINITY;
		this.heading = Double.NEGATIVE_INFINITY;
		this.distFunc = df;
	}
	
	/**
	 * Create a new point with the given coordinates and time stamp.
	 *
	 * @param x    Point X coordinate.
	 * @param y    Point Y coordinate.
	 * @param time Point time-stamp.
	 */
	public TrajectoryPoint(double x, double y, long time, DistanceFunction df) {
		super(x, y, df);
		this.time = time;
		this.speed = Double.NEGATIVE_INFINITY;
		this.heading = Double.NEGATIVE_INFINITY;
		this.distFunc = df;
	}
	
	/**
	 * Create a new point with the given coordinates, time stamp, speed and heading information.
	 *
	 * @param x    Point X coordinate.
	 * @param y    Point Y coordinate.
	 * @param time Point time-stamp.
	 */
	public TrajectoryPoint(double x, double y, long time, double speed, double heading, DistanceFunction df) {
		super(x, y, df);
		this.time = time;
		this.speed = speed;
		this.heading = heading;
		this.distFunc = df;
	}
	
	public static TrajectoryPoint parseTrajectoryPoint(String s, DistanceFunction df) {
		String[] pointInfo = s.split(" ");
		if (pointInfo.length == 5)
			return new TrajectoryPoint(Double.parseDouble(pointInfo[0]), Double.parseDouble(pointInfo[1]),
					Long.parseLong(pointInfo[2]), Double.parseDouble(pointInfo[3]), Double.parseDouble(pointInfo[4]), df);
		else if (pointInfo.length == 3)
			return new TrajectoryPoint(Double.parseDouble(pointInfo[0]), Double.parseDouble(pointInfo[1]),
					Long.parseLong(pointInfo[2]), df);
		else if (pointInfo.length == 2)
			return new TrajectoryPoint(Double.parseDouble(pointInfo[0]), Double.parseDouble(pointInfo[1]), df);
		throw new IllegalArgumentException("The input text cannot be parsed to a trajectory point: " + s);
	}
	
	/**
	 * @return Point time-stamp.
	 */
	public long time() {
		return time;
	}
	
	public double speed() {
		return speed;
	}
	
	public double heading() {
		return heading;
	}
	
	@Override
	public long timeStart() {
		return time;
	}
	
	@Override
	public long timeFinal() {
		return time;
	}
	
	@NonNull
	@Override
	public DistanceFunction getDistanceFunction() {
		return this.distFunc;
	}
	
	public void setDistFunc(DistanceFunction distFunc) {
		this.distFunc = distFunc;
	}
	
	@Override
	public TrajectoryPoint clone() {
		TrajectoryPoint clone = new TrajectoryPoint(x(), y(), time, speed, heading, distFunc);
		super.cloneTo(clone);
		return clone;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (obj instanceof TrajectoryPoint) {
			TrajectoryPoint p = (TrajectoryPoint) obj;
			if (!super.equals2D(p)) return false;
			return (p.time == time && p.speed == speed && p.heading == heading);
		}
		return false;
	}
	
	/**
	 * Compares these two STPoints for order using the given comparator.
	 *
	 * @param p          The point to compare to.
	 * @param comparator The TrajectoryPoint comparator to use.
	 * @return Returns a negative integer, zero, or a positive integer as this point is less than, equal to, or greater than the given
	 * point p.
	 */
	public int compareTo(TrajectoryPoint p, Comparator<TrajectoryPoint> comparator) {
		if (p == null) {
			throw new NullPointerException("Spatial-temporal Point for compareTo must not be null.");
		}
		if (comparator == null) {
			throw new NullPointerException("Spatial-temporal Point comparator must not be null.");
		}
		return comparator.compare(this, p);
	}
	
	@Override
	public String toString() {
		if (time == Long.MIN_VALUE)
			return super.toString();
		else if (speed == Double.NEGATIVE_INFINITY)
			return super.toString() + " " + time;
		else
			return (super.toString() + " " + time + " " + speed + " " + heading);
	}
	
	@Override
	public void print() {
		LOG.info("TRAJECTORY_POINT ( " + toString() + " )");
	}
}
