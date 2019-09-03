package util.object.spatialobject;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.impl.PackedCoordinateSequence;
import org.apache.log4j.Logger;
import util.function.DistanceFunction;
import util.function.GreatCircleDistanceFunction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * A 2D spatial-temporal trajectory entity. Discrete list of spatial-temporal points.
 *
 * @author uqdalves, Hellisk
 */
public class Trajectory extends ComplexSpatialObject<TrajectoryPoint> implements SpatialTemporalObject {
	
	/**
	 * Compare trajectories by LENGTH in ascending order.
	 */
	public static final Comparator<Trajectory> LENGTH_COMPARATOR = Comparator.comparingDouble(Trajectory::length);
	/**
	 * Compare trajectories by SPEED in ascending order.
	 */
	public static final Comparator<Trajectory> SPEED_COMPARATOR = Comparator.comparingDouble(Trajectory::getAverageSpeed);
	private static final Logger LOG = Logger.getLogger(Trajectory.class);
	/**
	 * Compare trajectories by time DURATION in ascending order.
	 */
	public static final Comparator<Trajectory> DURATION_COMPARATOR = Comparator.comparingLong(Trajectory::duration);
	// auxiliary LineString from JTS old version
	private com.vividsolutions.jts.geom.LineString JTSLineString = null;
	
	public Trajectory(DistanceFunction df) {
		super(1, df);
	}
	
	public Trajectory(String id, DistanceFunction df) {
		super(1, df);
		this.setID(id);
	}
	
	public Trajectory(List<TrajectoryPoint> pointList) {
		super(pointList.size(), pointList.size() == 0 ? new GreatCircleDistanceFunction() : pointList.get(0).getDistanceFunction());
		LOG.warn("Trajectory is created without id");
		if (pointList.size() == 0)
			throw new IllegalArgumentException("Create a trajectory with an empty trajectory point list.");
		this.addAll(pointList);
	}
	
	public Trajectory(String id, List<TrajectoryPoint> pointList) {
		super(pointList.size(), pointList.size() == 0 ? new GreatCircleDistanceFunction() : pointList.get(0).getDistanceFunction());
		if (pointList.size() == 0)
			throw new IllegalArgumentException("Create a trajectory with an empty trajectory point list.");
		this.setID(id);
		this.addAll(pointList);
	}
	
	public Trajectory(String id, DistanceFunction df, TrajectoryPoint... points) {
		super(points.length, df);
		this.setID(id);
		this.addAll(Arrays.asList(points));
	}
	
	@Override
	public List<Point> getCoordinates() {
		List<Point> list = new ArrayList<>(size());
		list.addAll(this);
		return list;
	}
	
	public List<TrajectoryPoint> getSTPoints() {
		List<TrajectoryPoint> list = new ArrayList<>(size());
		list.addAll(this);
		return list;
	}
	
	public List<Point> getPoints() {
		List<Point> list = new ArrayList<>(size());
		list.addAll(this);
		return list;
	}
	
	@Override
	public List<Segment> getEdges() {
		List<Segment> list = new ArrayList<>();
		Point pi, pj;
		for (int i = 0; i < size() - 1; i++) {
			pi = this.get(i);
			pj = this.get(i + 1);
			list.add(new Segment(pi.x(), pi.y(), pj.x(), pj.y(), getDistanceFunction()));
		}
		return list;
	}
	
	public List<TrajectorySegment> getSegments() {
		List<TrajectorySegment> list = new ArrayList<>();
		TrajectoryPoint pi, pj;
		for (int i = 0; i < size() - 1; i++) {
			pi = this.get(i);
			pj = this.get(i + 1);
			list.add(new TrajectorySegment(pi, pj));
		}
		return list;
	}
	
	/**
	 * Return the specific trajectory segment given the index of the start point.
	 *
	 * @param index The index of the left endpoint of the segment.
	 * @return The trajectory segment.
	 */
	public TrajectorySegment getSegment(int index) {
		if (index >= this.size() - 1)
			throw new IndexOutOfBoundsException("ERROR! The given index is out of bound.(" + index + " of " + (this.size() - 1) + ")");
		return new TrajectorySegment(this.get(index), this.get(index + 1));
	}
	
	/**
	 * Adds a spatial-temporal point to this trajectory.
	 */
	public void add(double x, double y, long time) {
		this.add(new TrajectoryPoint(x, y, time, getDistanceFunction()));
	}
	
	/**
	 * Adds a spatial-temporal point to this trajectory.
	 */
	public void add(double x, double y, long time, double speed, double heading) {
		this.add(new TrajectoryPoint(x, y, time, speed, heading, getDistanceFunction()
		));
	}
	
	/**
	 * Merges two trajectories. Appends a trajectory t to the end of this trajectory.
	 *
	 * @return This merged trajectory.
	 * @throws NullPointerException Empty input trajectory.
	 */
	public Trajectory add(Trajectory t) {
		if (t == null) {
			throw new NullPointerException("Trajectory for merge must not be null.");
		}
		this.addAll(t);
		return this;
	}
	
	/**
	 * Return the initial time of this trajectory. Time stamp of the first sample point.
	 */
	@Override
	public long timeStart() {
		if (!this.isEmpty()) {
			return head().time();
		}
		return 0;
	}
	
	/**
	 * Return the final time of this trajectory. Time stamp of the last sample point.
	 */
	@Override
	public long timeFinal() {
		if (!this.isEmpty()) {
			return tail().time();
		}
		return 0;
	}
	
	/**
	 * Return the length of this trajectory. Sum of the distances between every point.
	 */
	public double length() {
		double length = 0.0;
		if (!isEmpty()) {
			for (int i = 0; i < size() - 1; i++) {
				length += getDistanceFunction().distance(get(i), get(i + 1));
			}
		}
		return length;
	}
	
	/**
	 * Return the time duration of this trajectory. Time taken from the beginning to the end of the trajectory.
	 */
	public long duration() {
		if (!this.isEmpty()) {
			return (timeFinal() - timeStart());
		}
		return 0;
	}
	
	/**
	 * Return the average speed of this trajectory.
	 */
	public double getAverageSpeed() {
		if (!this.isEmpty() && duration() != 0) {
			return length() / duration();
		}
		return 0.0;
	}
	
	/**
	 * Return the average sample rate of the points in this trajectory (average time between every sample point).
	 *
	 * @return Average time interval.
	 */
	public double getSamplingRate() {
		if (!this.isEmpty()) {
			double rate = 0.0;
			TrajectoryPoint pi, pj;
			for (int i = 0; i < this.size() - 1; i++) {
				pi = this.get(i);
				pj = this.get(i + 1);
				rate += pj.time() - pi.time();
			}
			return (rate / (this.size() - 1));
		}
		return 0.0;
	}
	
	/**
	 * The 'head' of this trajectory: First sample point.
	 */
	public TrajectoryPoint head() {
		if (!this.isEmpty()) {
			return this.get(0);
		}
		LOG.warn("Trajectory head not found: no point in trajectory.");
		return null;
	}
	
	/**
	 * The 'tail' of this trajectory: Last sample point.
	 */
	public TrajectoryPoint tail() {
		if (!this.isEmpty()) {
			return this.get(this.size() - 1);
		}
		LOG.warn("Trajectory tail not found: no point in trajectory.");
		return null;
	}
	
	/**
	 * Return a sub-trajectory of this trajectory, from beginIndex inclusive to endIndex exclusive.
	 * <p>
	 * Note: trajectory index starts from 0 (zero).
	 *
	 * @throws IllegalArgumentException Trajectory does not contain the specific index
	 */
	public Trajectory subTrajectory(int beginIndex, int endIndex) {
		if (beginIndex < 0 || endIndex > size() || beginIndex > endIndex) {
			throw new IllegalArgumentException("Trajectory index out of bound.");
		}
		return new Trajectory(this.getID(), this.subList(beginIndex, endIndex));
	}
	
	/**
	 * Down-sample the trajectory to the <tt>multiplier</tt> times lower sampling rate.
	 *
	 * @param multiplier The new sampling interval is (multiplier * \delta t)
	 * @return Sub-trajectory.
	 */
	public Trajectory subSample(int multiplier) {
		List<TrajectoryPoint> newPointList = new ArrayList<>();
		newPointList.add(this.get(0));
		for (int i = 1; i < this.size(); i += multiplier) {
			newPointList.add(this.get(i));
		}
		newPointList.add(this.get(this.size() - 1));
		return new Trajectory(this.getID(), newPointList);
	}
	
	/**
	 * The reverse representation of this Trajectory.
	 */
	public Trajectory reverse() {
		Trajectory reverse = new Trajectory(getDistanceFunction());
		for (int i = size() - 1; i >= 0; i--) {
			reverse.add(this.get(i).clone());
		}
		return reverse;
	}
	
	@Override
	public boolean isClosed() {
		if (size() < 2) return false;
		return get(0).equals2D(get(size() - 1));
	}
	
	@Override
	public Geometry toJTSGeometry() {
		if (JTSLineString == null) {
			Coordinate[] coords = new Coordinate[size()];
			int i = 0;
			for (Point p : this) {
				coords[i++] = new Coordinate(p.x(), p.y());
			}
			PackedCoordinateSequence points = new PackedCoordinateSequence.Double(coords);
			
			JTSLineString = new com.vividsolutions.jts.geom.LineString(points, new GeometryFactory());
		}
		return JTSLineString;
	}
	
	/**
	 * Returns an array with the X coordinates of this trajectory sample points.
	 */
	public double[] getXValues() {
		double[] result = new double[this.size()];
		int i = 0;
		for (TrajectoryPoint p : this) {
			result[i++] = p.x();
		}
		return result;
	}
	
	/**
	 * Returns an array with the Y coordinates of this trajectory sample points.
	 */
	public double[] getYValues() {
		double[] result = new double[this.size()];
		int i = 0;
		for (TrajectoryPoint p : this) {
			result[i++] = p.y();
		}
		return result;
	}
	
	/**
	 * Returns an array with the TIME coordinates of this trajectory sample points.
	 */
	public long[] getTimeValues() {
		long[] result = new long[this.size()];
		int i = 0;
		for (TrajectoryPoint p : this) {
			result[i++] = p.time();
		}
		return result;
	}
	
	@Override
	public boolean equals2D(SpatialObject obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (obj instanceof Trajectory) {
			Trajectory t = (Trajectory) obj;
			if (this.size() != t.size()) {
				return false;
			}
			for (int i = 0; i < size(); i++) {
				if (!t.get(i).equals2D(this.get(i))) {
					return false;
				}
			}
			return true;
		}
		return false;
	}
	
	/**
	 * Check whether these two trajectories are spatially and temporally equivalents, that is, whether they have the same spatial-temporal
	 * coordinate points. This method verifies if every coordinate of the two trajectories are the same in the exact order they are
	 * declared.
	 *
	 * @param t The trajectory to compare,
	 * @return True if these two trajectories are spatially and temporally equivalents.
	 */
	public boolean equalsST(Trajectory t) {
		if (t == null) return false;
		if (t.size() != this.size()) return false;
		if (t.timeStart() != this.timeStart() ||
				t.timeFinal() != this.timeFinal()) {
			return false;
		}
		for (int i = 0; i < size(); i++) {
			if (!this.get(i).equals(t.get(i))) {
				return false;
			}
		}
		return true;
	}
	
	@Override
	public Trajectory clone() {
		Trajectory clone = new Trajectory(getDistanceFunction());
		for (TrajectoryPoint p : this) {
			clone.add(p.clone());
		}
		super.cloneTo(clone);
		return clone;
	}
	
	@Override
	public String toString() {
		StringBuilder s = new StringBuilder(this.getID() + "|");
		for (TrajectoryPoint p : this) {
			s.append(",").append(p.toString());
		}
		s = new StringBuilder(s.toString().replaceFirst(",", ""));
		return s.toString();
	}
	
	@Override
	public void print() {
		LOG.info("TRAJECTORY ( " + toString() + " )");
	}
}