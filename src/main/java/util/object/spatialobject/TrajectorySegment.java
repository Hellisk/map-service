package util.object.spatialobject;

import org.apache.log4j.Logger;
import util.function.DistanceFunction;

/**
 * A 2D Spatial-Temporal line segment (segment with temporal features). Line segment from coordinate points (x1,y1) to (x2,y2) and a time
 * interval [t1,t2].
 * <p>
 * TrajectorySegment objects may contain both spatial-temporal and semantic attributes. Spatial-temporal attributes of ST spatial
 * objects, however, are immutable, that means once a TrajectorySegment object is created its spatial attributes cannot be changed.
 * Moreover, the TrajectorySegment doesn't have ID.
 *
 * @author uqdalves, Hellisk
 */
public class TrajectorySegment extends Segment implements SpatialTemporalObject {
	
	private static final Logger LOG = Logger.getLogger(TrajectorySegment.class);
	
	private final long t1, t2;      // timestamps
	private final double s1, s2;    // speeds
	private final double h1, h2;    // heading
	private final DistanceFunction distFunc;
	
	/**
	 * Creates a new empty spatial-temporal segment.
	 */
	public TrajectorySegment(DistanceFunction df) {
		super(0, 0, 0, 0, df);
		this.t1 = 0;
		this.t2 = 0;
		this.s1 = 0;
		this.s2 = 0;
		this.h1 = 0;
		this.h2 = 0;
		this.distFunc = df;
	}
	
	/**
	 * Creates a new spatial-temporal segment with the given coordinates and time-stamp interval.
	 *
	 * @param x1 Start-point X coordinate.
	 * @param y1 Start-point Y coordinate.
	 * @param t1 Start-point time-stamp.
	 * @param x2 End-point X coordinate.
	 * @param y2 End-point Y coordinate.
	 * @param t2 End-point time-stamp.
	 */
	public TrajectorySegment(double x1, double y1, long t1, double x2, double y2, long t2, DistanceFunction df) {
		super(x1, y1, x2, y2, df);
		if (t1 < 0 || t2 < 0) {
			throw new IllegalArgumentException("Time-stamp for spatial-temporal object construction must not be negative.");
		}
		if (t2 < t1) {
			throw new IllegalArgumentException("Initial time-stamp must be smaller than final time-stamp.");
		}
		this.t1 = t1;
		this.t2 = t2;
		this.s1 = 0;
		this.s2 = 0;
		this.h1 = 0;
		this.h2 = 0;
		this.distFunc = df;
	}
	
	/**
	 * Creates a new spatial-temporal segment with the given coordinates and time-stamp interval.
	 *
	 * @param x1 Start-point X coordinate.
	 * @param y1 Start-point Y coordinate.
	 * @param t1 Start-point time-stamp.
	 * @param s1 Start-point speed.
	 * @param h1 start-point heading.
	 * @param x2 End-point X coordinate.
	 * @param y2 End-point Y coordinate.
	 * @param t2 End-point time-stamp.
	 * @param s2 End-point speed.
	 * @param h2 End-point heading.
	 */
	public TrajectorySegment(double x1, double y1, long t1, double s1, double h1, double x2, double y2, long t2, double s2, double h2,
							 DistanceFunction df) {
		super(x1, y1, x2, y2, df);
		if (t1 < 0 || t2 < 0) {
			throw new IllegalArgumentException("Time-stamp for spatial-temporal object construction must not be negative.");
		}
		if (t2 < t1) {
			throw new IllegalArgumentException("Initial time-stamp must be smaller than final time-stamp.");
		}
		this.t1 = t1;
		this.t2 = t2;
		this.s1 = s1;
		this.s2 = s2;
		this.h1 = h1;
		this.h2 = h2;
		this.distFunc = df;
	}
	
	/**
	 * Creates a new spatial-temporal segment with the given start and end spatial-temporal points.
	 *
	 * @param p1 Start-point with time-stamp.
	 * @param p2 End-point with time-stamp.
	 */
	public TrajectorySegment(TrajectoryPoint p1, TrajectoryPoint p2) {
		super(p1.x(), p1.y(), p2.x(), p2.y(), p1.getDistanceFunction());
		if (p1.time() < 0 || p2.time() < 0) {
			throw new IllegalArgumentException("Time-stamp for spatial-temporal object construction must not be negative.");
		}
		if (p2.time() < p1.time()) {
			throw new IllegalArgumentException("Initial time-stamp must be smaller than final time-stamp.");
		}
		this.t1 = p1.time();
		this.t2 = p2.time();
		this.s1 = p1.speed();
		this.s2 = p2.speed();
		this.h1 = p1.heading();
		this.h2 = p2.heading();
		if (!p1.getDistanceFunction().getClass().getName().equals(p2.getDistanceFunction().getClass().getName())) {
			throw new IllegalArgumentException("Creating trajectory segment based on two points with different coordinate system: "
					+ p1.getDistanceFunction().getClass().getName() + "," + p2.getDistanceFunction().getClass().getName());
		}
		this.distFunc = p1.getDistanceFunction();
	}
	
	/**
	 * Get the first/start spatial-temporal point of this TrajectorySegment.
	 *
	 * @return The segment's start-point (x1,y1,t1).
	 */
	@Override
	public TrajectoryPoint p1() {
		return new TrajectoryPoint(x1(), y1(), t1, s1, h1, distFunc);
	}
	
	/**
	 * Get the second/final spatial-temporal point of this TrajectorySegment.
	 *
	 * @return The segment's end-point (x2,y2,t2).
	 */
	@Override
	public TrajectoryPoint p2() {
		return new TrajectoryPoint(x2(), y2(), t2, s2, h2, distFunc);
	}
	
	@Override
	public long timeStart() {
		return t1;
	}
	
	@Override
	public long timeFinal() {
		return t2;
	}
	
	public double getAverageSpeed() {
		return distFunc.distance(p1(), p2()) / (timeFinal() - timeStart());
	}
	
	
	public double getHeading() {
		return distFunc.getHeading(x1(), y1(), x2(), y2());
	}
	
	/**
	 * Return the point (position) on the this segment at given time-stamp t.
	 * <br>
	 * Note that time t must be grater than or equals to zero, and s.t1 =< t =< s.t2.
	 *
	 * @param t The time to search the position.
	 * @return The spatial-temporal point with time-stamp = t on this line segment.
	 */
	public TrajectoryPoint getPointByTime(long t) {
		if (t < 0) {
			throw new IllegalArgumentException("Time-stamp must not be negative.");
		}
		if (t < t1 || t > t2) {
			throw new IllegalArgumentException("Time-stamp ts for segment s search must be s.t1 <= ts <= s.t2");
		}
		boolean isX = (x1() == x2());
		boolean isY = (y1() == y2());
		
		if (isX && isY) {
			return new TrajectoryPoint(x1(), y1(), t, s1, h1, distFunc);
		} else if (isX) {
			double aaa = (double) (t - t1);
			double bbb = (double) (t2 - t1);
			
			double yy = ((aaa) * (bbb) / (y2() - y1())) + y1();
			
			return new TrajectoryPoint(x1(), yy, t, this.getAverageSpeed(), this.getHeading(), distFunc);
		} else {
			double aaa = (double) (t - t1);
			double bbb = (double) (t2 - t1);
			
			double yy = ((aaa) * (bbb) / (y2() - y1())) + y1();
			double xx = ((aaa) * (bbb) / (x2() - x1())) + x1();
			
			return new TrajectoryPoint(xx, yy, t, this.getAverageSpeed(), this.getHeading(), distFunc);
		}
	}
	
	@Override
	public TrajectorySegment clone() {
		TrajectorySegment clone = new TrajectorySegment(x1(), y1(), t1, s1, h1, x2(), y2(), t2, s2, h2, distFunc);
		super.cloneTo(clone);
		return clone;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (obj instanceof TrajectorySegment) {
			TrajectorySegment s = (TrajectorySegment) obj;
			if (!super.equals2D(s)) return false;
			return (s.t1 == t1 && s.t2 == t2 && s.s1 == s1 && s.h1 == h1);
		}
		return false;
	}
	
	@Override
	public String toString() {
		String s = "( ";
		s += String.format("%.5f %.5f", x1(), y1()) + " " + t1 + " " + s1 + " " + h1 + ", ";
		s += String.format("%.5f %.5f", x2(), y2()) + " " + t2 + " " + s2 + " " + h2;
		return s + " )";
	}
	
	@Override
	public void print() {
		LOG.info("TRAJECTORY_SEGMENT " + toString());
	}
}
