package traminer.util.spatial.objects.st;

import traminer.util.spatial.objects.Point;

import java.util.Comparator;

/**
 * Implements a simple 3D Spatial-Temporal Point entity,
 * with (x,y,z) coordinates and time-stamp.
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
     * Point time-stamp
     */
    private final long time;

    /**
     * Creates as empty STPoint with default attributes (0,0,0)
     */
    public STPoint() {
        super();
        this.time = 0;
    }

    /**
     * Set time as zero = 0
     */
    public STPoint(double x, double y) {
        super(x, y);
        this.time = 0;
    }

    public STPoint(double x, double y, long time) {
        super(x, y);
        this.time = time;
    }

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
     * Compare two points by their spatial-temporal coordinate
     * values, first by X, then by Y, then by Z, then by TIME.
     */
    public int compareTo(STPoint p, Comparator<STPoint> comparator) {
        return comparator.compare(this, p);
    }

    @Override
    public String toString() {
        return (super.toString() + " " + time);
    }

    @Override
    public void print() {
        System.out.println("ST_POINT ( " + toString() + " )");
    }
}
