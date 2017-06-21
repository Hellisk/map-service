package traminer.util.spatial.objects.st;

import traminer.util.spatial.objects.Edges;
import traminer.util.spatial.objects.Vector2D;

/**
 * A 2D Spatial-Temporal line segment object with time-stamp.
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
public class STSegment extends Edges implements SpatialTemporalObject {
    /**
     * End points time-stamp. Time interval
     */
    private final long t1, t2;

    public STSegment() {
        super();
        this.t1 = 0;
        this.t2 = 0;
    }

    public STSegment(double x1, double y1, long t1,
                     double x2, double y2, long t2) {
        super(x1, y1, x2, y2);
        this.t1 = t1;
        this.t2 = t2;
    }

    public STSegment(STPoint p1, STPoint p2) {
        super(p1.x(), p1.y(), p2.x(), p2.y());
        this.t1 = p1.time();
        this.t2 = p2.time();
    }

    /**
     * Get the fist spatial-temporal endpoint (x1,y1,t1).
     */
    @Override
    public STPoint p1() {
        return new STPoint(x1(), y1(), t1);
    }

    /**
     * Get the second spatial-temporal endpoint (x2,y2,t2).
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
     * Return the point on the this segment for the given time-stamp t.
     * <br> Note that time t must be s.t1 =< t =< s.t2.
     */
    public STPoint getPointByTime(long t) {
        assert (t1 <= t && t >= t2) :
                "Time t must be s.t1 <= t <= s.t2";

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
     * @return Return the projection with time-stamp.
     */
    public STPoint projection(STPoint p) {
        return projectionWithTime(p.x(), p.y(), p.time(),
                x1(), y1(), t1, x2(), y2(), t2);
    }

    /**
     * Calculate the projection of the given spatial-temporal point
     * p = (x,y,time) on to this spatial-temporal line segment.
     *
     * @return Return the projection with time-stamp.
     */
    public STPoint getProjectionWithTime(double x, double y, long time) {
        return projectionWithTime(x, y, time,
                x1(), y1(), t1, x2(), y2(), t2);
    }

    /**
     * Calculate the projection with time-stamp of a given
     * spatial-temporal point p = (x,y,t) on to the given
     * spatial-temporal line segment s = (x1,y1,t1)--(x2,y2,t2).
     *
     * @return Return the projection of p onto s.
     */
    public static STPoint projectionWithTime(
            double x, double y, long t,
            double x1, double y1, long t1,
            double x2, double y2, long t2) {
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
    public boolean equalsST(SpatialTemporalObject obj) {
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
        String s = "(";
        s += String.format("%.3f %.3f", x1(), y1()) + " " + t1 + ", ";
        s += String.format("%.3f %.3f", x2(), y2()) + " " + t2;
        return s + ")";
    }

    @Override
    public void print() {
        System.out.println("ST_SEGMENT " + toString());
    }
}
