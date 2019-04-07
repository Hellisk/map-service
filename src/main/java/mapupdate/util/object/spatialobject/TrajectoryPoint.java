package mapupdate.util.object.spatialobject;

import java.util.Comparator;

import static mapupdate.Main.LOGGER;

/**
 * Implements a simple Spatial-Temporal point (point with time-stamp),
 * with (x,y) coordinates and time-stamp.
 * <p>
 * TrajectoryPoint objects may contain both spatial-temporal and
 * semantic attributes. Spatial-temporal attributes of ST
 * objects, however, are immutable, that means once a TrajectoryPoint
 * object is created its spatial attributes cannot be changed.
 *
 * @author uqdalves
 */
@SuppressWarnings("serial")
public class TrajectoryPoint extends Point implements SpatialTemporalObject {
    /**
     * Time-stamp
     */
    private final long time;
    private final double speed;        // the instantaneous speed of the object at the current time
    private final double heading;    // the heading of trajectory point ranging between -179~180. Degree 0 = (1,0)

    /**
     * Create a new empty point with zero time stamp.
     */
    public TrajectoryPoint() {
        super(0, 0);
        this.time = 0;
        this.speed = 0;
        this.heading = 0;
    }

    /**
     * Create a new point with the given coordinates and zero time stamp.
     *
     * @param x Point X coordinate.
     * @param y Point Y coordinate.
     */
    public TrajectoryPoint(double x, double y) {
        super(x, y);
        this.time = 0;
        this.speed = 0;
        this.heading = 0;
    }

    /**
     * Create a new point with the given coordinates and time stamp.
     *
     * @param x    Point X coordinate.
     * @param y    Point Y coordinate.
     * @param time Point time-stamp.
     */
    public TrajectoryPoint(double x, double y, long time) {
        super(x, y);
        this.time = time;
        this.speed = 0;
        this.heading = 0;
    }

    /**
     * Create a new point with the given coordinates, time stamp, speed and heading information.
     *
     * @param x    Point X coordinate.
     * @param y    Point Y coordinate.
     * @param time Point time-stamp.
     */
    public TrajectoryPoint(double x, double y, long time, double speed, double heading) {
        super(x, y);
        this.time = time;
        this.speed = speed;
        this.heading = heading;
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

    @Override
    public TrajectoryPoint clone() {
        TrajectoryPoint clone = new TrajectoryPoint(x(), y(), time, speed, heading);
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
     * @return Returns a negative integer, zero, or a positive integer as this
     * point is less than, equal to, or greater than the given point p.
     */
    public int compareTo(TrajectoryPoint p, Comparator<TrajectoryPoint> comparator) {
        if (p == null) {
            throw new NullPointerException(
                    "Spatial-temporal Point for compareTo must not be null.");
        }
        if (comparator == null) {
            throw new NullPointerException(
                    "Spatial-temporal Point comparator must not be null.");
        }
        return comparator.compare(this, p);
    }

    @Override
    public String toString() {
        return (super.toString() + " " + time + " " + speed + " " + heading);
    }

    @Override
    public void print() {
        LOGGER.info("TRAJECTORY_POINT ( " + toString() + " )");
    }
}
