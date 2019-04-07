package mapupdate.util.object.spatialobject;

import static mapupdate.Main.LOGGER;

/**
 * A 2D Spatial-Temporal line segment (segment with temporal features).
 * Line segment from coordinate points (x1,y1) to (x2,y2) and
 * a time interval [t1,t2].
 * <p>
 * TrajectorySegment objects may contain both spatial-temporal and
 * semantic attributes. Spatial-temporal attributes of ST
 * spatial objects, however, are immutable, that means once
 * a TrajectorySegment object is created its spatial attributes cannot
 * be changed.
 *
 * @author uqdalves
 */
@SuppressWarnings("serial")
public class TrajectorySegment extends Segment implements SpatialTemporalObject {
    /**
     * End points time-stamp. Time interval
     */
    private final long t1, t2;  // timestamps
    private final double s1, s2; // speeds
    private final double h1, h2; // heading

    /**
     * Creates a new empty spatial-temporal segment.
     */
    public TrajectorySegment() {
        super(0, 0, 0, 0);
        this.t1 = 0;
        this.t2 = 0;
        this.s1 = 0;
        this.s2 = 0;
        this.h1 = 0;
        this.h2 = 0;
    }

    /**
     * Creates a new spatial-temporal segment with the given
     * coordinates and time-stamp interval.
     *
     * @param x1 Start-point X coordinate.
     * @param y1 Start-point Y coordinate.
     * @param t1 Start-point time-stamp.
     * @param x2 End-point X coordinate.
     * @param y2 End-point Y coordinate.
     * @param t2 End-point time-stamp.
     */
    public TrajectorySegment(double x1, double y1, long t1,
                             double x2, double y2, long t2) {
        super(x1, y1, x2, y2);
        if (t1 < 0 || t2 < 0) {
            throw new IllegalArgumentException("Time-stamp for spatial-temporal"
                    + "object construction must not be negative.");
        }
        if (t2 < t1) {
            throw new IllegalArgumentException("Initial time-stamp must be "
                    + "smaller than final time-stamp.");
        }
        this.t1 = t1;
        this.t2 = t2;
        this.s1 = 0;
        this.s2 = 0;
        this.h1 = 0;
        this.h2 = 0;
    }

    /**
     * Creates a new spatial-temporal segment with the given
     * coordinates and time-stamp interval.
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
    public TrajectorySegment(double x1, double y1, long t1, double s1, double h1,
                             double x2, double y2, long t2, double s2, double h2) {
        super(x1, y1, x2, y2);
        if (t1 < 0 || t2 < 0) {
            throw new IllegalArgumentException("Time-stamp for spatial-temporal"
                    + "object construction must not be negative.");
        }
        if (t2 < t1) {
            throw new IllegalArgumentException("Initial time-stamp must be "
                    + "smaller than final time-stamp.");
        }
        this.t1 = t1;
        this.t2 = t2;
        this.s1 = s1;
        this.s2 = s2;
        this.h1 = h1;
        this.h2 = h2;
    }

    /**
     * Creates a new spatial-temporal segment with the given
     * start and end spatial-temporal points.
     *
     * @param p1 Start-point with time-stamp.
     * @param p2 End-point with time-stamp.
     */
    public TrajectorySegment(TrajectoryPoint p1, TrajectoryPoint p2) {
        super(p1.x(), p1.y(), p2.x(), p2.y());
        if (p1.time() < 0 || p2.time() < 0) {
            throw new IllegalArgumentException("Time-stamp for spatial-temporal"
                    + "object construction must not be negative.");
        }
        if (p2.time() < p1.time()) {
            throw new IllegalArgumentException("Initial time-stamp must be "
                    + "smaller than final time-stamp.");
        }
        this.t1 = p1.time();
        this.t2 = p2.time();
        this.s1 = p1.speed();
        this.s2 = p2.speed();
        this.h1 = p1.heading();
        this.h2 = p2.heading();
    }

    /**
     * Get the first/start spatial-temporal point of this TrajectorySegment.
     *
     * @return The segment's start-point (x1,y1,t1).
     */
    @Override
    public TrajectoryPoint p1() {
        return new TrajectoryPoint(x1(), y1(), t1, s1, h1);
    }

    /**
     * Get the second/final spatial-temporal point of this TrajectorySegment.
     *
     * @return The segment's end-point (x2,y2,t2).
     */
    @Override
    public TrajectoryPoint p2() {
        return new TrajectoryPoint(x2(), y2(), t2, s2, h2);
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
        return distance(p1().x(), p1().y(), p2().x(), p2().y()) / (timeFinal() - timeStart());
    }


    public double getHeading() {
        double lon1 = Math.toRadians(x1());
        double lat1 = Math.toRadians(y1());
        double lon2 = Math.toRadians(x2());
        double lat2 = Math.toRadians(y2());
        double headingRadians = Math.atan2(Math.asin(lon2 - lon1) * Math.cos(lat2),
                Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(lon2 - lon1));
        return Math.toDegrees(headingRadians);
    }

    /**
     * Return the point (position) on the this segment at given
     * time-stamp t.
     * <br>
     * Note that time t must be grater than or equals to zero, and
     * s.t1 =< t =< s.t2.
     *
     * @param t The time to search the position.
     * @return The spatial-temporal point with time-stamp = t on this
     * line segment.
     */
    public TrajectoryPoint getPointByTime(long t) {
        if (t < 0) {
            throw new IllegalArgumentException(
                    "Time-stamp must not be negative.");
        }
        if (t < t1 || t > t2) {
            throw new IllegalArgumentException("Time-stamp ts for "
                    + "segment s search must be s.t1 <= ts <= s.t2");
        }
        boolean isX = (x1() == x2());
        boolean isY = (y1() == y2());

        if (isX && isY) {
            return new TrajectoryPoint(x1(), y1(), t, s1, h1);
        } else if (isX) {
            double aaa = (double) (t - t1);
            double bbb = (double) (t2 - t1);

            double yy = ((aaa) * (bbb) / (y2() - y1())) + y1();

            return new TrajectoryPoint(x1(), yy, t, this.getAverageSpeed(), this.getHeading());
        } else {
            double aaa = (double) (t - t1);
            double bbb = (double) (t2 - t1);

            double yy = ((aaa) * (bbb) / (y2() - y1())) + y1();
            double xx = ((aaa) * (bbb) / (x2() - x1())) + x1();

            return new TrajectoryPoint(xx, yy, t, this.getAverageSpeed(), this.getHeading());
        }
    }

    @Override
    public TrajectorySegment clone() {
        TrajectorySegment clone = new TrajectorySegment(x1(), y1(), t1, s1, h1, x2(), y2(), t2, s2, h2);
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
        LOGGER.info("TRAJECTORY_SEGMENT " + toString());
    }
}
