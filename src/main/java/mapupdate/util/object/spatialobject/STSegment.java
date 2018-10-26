package mapupdate.util.object.spatialobject;

import static mapupdate.Main.LOGGER;

/**
 * A 2D Spatial-Temporal line segment (segment with temporal features).
 * Line segment from coordinate points (x1,y1) to (x2,y2) and
 * a time interval [t1,t2].
 * <p>
 * STSegment objects may contain both spatial-temporal and
 * semantic attributes. Spatial-temporal attributes of ST
 * spatial objects, however, are immutable, that means once
 * a STSegment object is created its spatial attributes cannot
 * be changed.
 *
 * @author uqdalves
 */
@SuppressWarnings("serial")
public class STSegment extends Segment implements SpatialTemporalObject {
    /**
     * End points time-stamp. Time interval
     */
    private final long t1, t2;

    /**
     * Creates a new empty spatial-temporal segment.
     */
    public STSegment() {
        super(0, 0, 0, 0);
        this.t1 = 0;
        this.t2 = 0;
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
    public STSegment(double x1, double y1, long t1,
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
    }

    /**
     * Creates a new spatial-temporal segment with the given
     * start and end spatial-temporal points.
     *
     * @param p1 Start-point with time-stamp.
     * @param p2 End-point with time-stamp.
     */
    public STSegment(STPoint p1, STPoint p2) {
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
    }

    /**
     * Get the first/start spatial-temporal point of this STSegment.
     *
     * @return The segment's start-point (x1,y1,t1).
     */
    @Override
    public STPoint p1() {
        return new STPoint(x1(), y1(), t1);
    }

    /**
     * Get the second/final spatial-temporal point of this STSegment.
     *
     * @return The segment's end-point (x2,y2,t2).
     */
    @Override
    public STPoint p2() {
        return new STPoint(x2(), y2(), t2);
    }

    @Override
    public long timeStart() {
        return t1;
    }

    @Override
    public long timeFinal() {
        return t2;
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
    public STPoint getPointByTime(long t) {
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
            return new STPoint(x1(), y1(), t);
        } else if (isX) {
            double aaa = (double) (t - t1);
            double bbb = (double) (t2 - t1);

            double yy = ((aaa) * (bbb) / (y2() - y1())) + y1();

            return new STPoint(x1(), yy, t);
        } else {
            double aaa = (double) (t - t1);
            double bbb = (double) (t2 - t1);

            double yy = ((aaa) * (bbb) / (y2() - y1())) + y1();
            double xx = ((aaa) * (bbb) / (x2() - x1())) + x1();

            return new STPoint(xx, yy, t);
        }
    }

    /**
     * Calculate the projection of the given spatial-temporal point
     * p on to this spatial-temporal line segment.
     *
     * @param p The spatial-temporal point to project onto this
     *          spatial-temporal segment.
     * @return The projection of p onto this spatial-temporal line segment.
     */
    public STPoint getProjectionWithTime(STPoint p) {
        if (p == null) {
            throw new NullPointerException("Point for projection "
                    + "computation must not be null.");
        }
        return pointToSegmentProjectionWithTime(p.x(), p.y(), p.time(),
                x1(), y1(), t1, x2(), y2(), t2);
    }

    /**
     * Calculate the projection of the given spatial-temporal point
     * p = (x,y,time) on to this spatial-temporal line segment.
     *
     * @param x
     * @param y
     * @param time
     * @return The projection of p = (x,y,time) onto this spatial-temporal
     * line segment.
     */
    public STPoint getProjectionWithTime(double x, double y, long time) {
        if (time < 0) {
            throw new IllegalArgumentException("Time-stamp for projection "
                    + "computation must not be negative.");
        }
        return pointToSegmentProjectionWithTime(x, y, time,
                x1(), y1(), t1, x2(), y2(), t2);
    }

    /**
     * Calculate the projection with time-stamp of a given
     * spatial-temporal point p = (x,y,t) on to the given
     * spatial-temporal line segment s = (x1,y1,t1)(x2,y2,t2).
     *
     * @param x  Point X coordinate.
     * @param y  Point Y coordinate.
     * @param t  Point time-stamp.
     * @param x1 Segment start-point X coordinate.
     * @param y1 Segment start-point Y coordinate.
     * @param t1 Segment start-point time-stamp.
     * @param x2 Segment end-point X coordinate.
     * @param y2 Segment end-point Y coordinate.
     * @param t2 Segment end-point time-stamp.
     * @return The projection of p = (x,y,t) onto the spatial-temporal
     * line segment s = (x1,y1,t1)(x2,y2,t2).
     */
    public static STPoint pointToSegmentProjectionWithTime(
            double x, double y, long t,
            double x1, double y1, long t1,
            double x2, double y2, long t2) {
        if (t < 0 || t1 < 0 || t2 < 0) {
            throw new IllegalArgumentException("Time-stamps for spatial-temporal"
                    + "projection must not be negative.");
        }
        if (t2 < t1) {
            throw new IllegalArgumentException("Initial time-stamp must be "
                    + "smaller than final time-stamp.");
        }
        if (t < t1 || t > t2) {
            throw new IllegalArgumentException("Point time-stamp t for "
                    + "spatial-temporal projection must be t1 <= t <= t2");
        }
        // segments vector
        double v1x = x2 - x1;
        double v1y = y2 - y1;
        double v2x = x - x1;
        double v2y = y - y1;

        // get squared length of this segment e
        double len2 = (x2 - x1) * (x2 - x1) +
                (y2 - y1) * (y2 - y1);

        // p1 and p2 are the same point
        if (len2 == 0) {
            return new STPoint(x1, y1, t1);
        }

        // the projection falls where
        // d = [(p - p1) . (p2 - p1)] / |p2 - p1|^2
        double d = Vector2D.dotProduct(v2x, v2y, v1x, v1y) / len2;

        // "Before" s.p1 on the line
        if (d < 0.0) {
            return new STPoint(x1, y1, t1);
        }
        // after s.p2 on the line
        if (d > 1.0) {
            return new STPoint(x2, y2, t2);
        }

        // projection is "in between" s.p1 and s.p2
        // get projection coordinates and time-stamp
        double px = x1 + d * (x2 - x1);
        double py = y1 + d * (y2 - y1);
        long pt = (long) (t1 + d * (t2 - t1));

        return new STPoint(px, py, pt);
    }

    @Override
    public STSegment clone() {
        STSegment clone = new STSegment(x1(), y1(), t1, x2(), y2(), t2);
        super.cloneTo(clone);
        return clone;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + (int) (t1 ^ (t1 >>> 32));
        result = prime * result + (int) (t2 ^ (t2 >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (obj instanceof STSegment) {
            STSegment s = (STSegment) obj;
            if (!super.equals2D(s)) return false;
            return (s.t1 == t1 && s.t2 == t2);
        }
        return false;
    }

    @Override
    public String toString() {
        String s = "( ";
        s += String.format("%.5f %.5f", x1(), y1()) + " " + t1 + ", ";
        s += String.format("%.5f %.5f", x2(), y2()) + " " + t2;
        return s + " )";
    }

    @Override
    public void print() {
        LOGGER.info("ST_SEGMENT " + toString());
    }
}
