package traminer.util.spatial.objects.st;

import traminer.util.spatial.objects.Point;

import java.util.Comparator;

/**
 * Implements a simple Spatial-Temporal point (point with time-stamp), 
 * with (x,y) coordinates and time-stamp. 
 * <p>
 * STPoint objects may contain both spatial-temporal and 
 * semantic attributes. Spatial-temporal attributes of ST
 * objects, however, are immutable, that means once a STPoint
 * object is created its spatial attributes cannot be changed.
 * 
 * @author uqdalves
 */
@SuppressWarnings("serial")
public class STPoint extends Point implements SpatialTemporalObject {
    /**
     * Time-stamp
     */
    private final long time;

    /**
     * Create a new empty point with zero time stamp.
     */
    public STPoint() {
        super(0, 0);
        this.time = 0;
    }

    /**
     * Create a new point with the given coordinates and
     * zero time stamp.
     *
     * @param x Point X coordinate.
     * @param y Point Y coordinate.
     */
    public STPoint(double x, double y) {
        super(x, y);
        this.time = 0;
    }

    /**
     * Create a new point with the given coordinates and
     * time stamp.
     *
     * @param x    Point X coordinate.
     * @param y    Point Y coordinate.
     * @param time Point time-stamp.
     */
    public STPoint(double x, double y, long time) {
        super(x, y);
        this.time = time;
    }

    /**
     * @return Point time-stamp.
     */
    public long time() {
        return time;
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
    public STPoint clone() {
        STPoint clone = new STPoint(x(), y(), time);
        super.cloneTo(clone);
        return clone;
    }

    @Override
    public boolean equalsST(SpatialTemporalObject obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (obj instanceof STPoint) {
            STPoint p = (STPoint) obj;
            if (!super.equals2D(p)) return false;
            return (p.time == time);
        }
        return false;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + (int) (time ^ (time >>> 32));
        return result;
    }

    /**
     * Compares these two STPoints for order using the given comparator.
     *
     * @param p The point to compare to.
     * @param comparator The STPoint comparator to use.
     * @return Returns a negative integer, zero, or a positive integer as this
     * point is less than, equal to, or greater than the given point p.
     */
    public int compareTo(STPoint p, Comparator<STPoint> comparator) {
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
        return (super.toString() + " " + time);
    }

    @Override
    public void print() {
        println("ST_POINT ( " + toString() + " )");
    }
}
