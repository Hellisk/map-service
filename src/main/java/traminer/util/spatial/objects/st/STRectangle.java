package traminer.util.spatial.objects.st;

import traminer.util.spatial.objects.Rectangle;

/**
 * A 2D Spatial-Temporal Rectangle (rectangle with temporal features), 
 * Composed by a spatial region (rectangle) and a time interval [t1,t2].
 * <p>
 * STRectangle objects may contain both spatial-temporal and 
 * semantic attributes. Spatial-temporal attributes of ST
 * spatial objects, however, are immutable, that means once 
 * a STRectangle object is created its spatial attributes 
 * cannot be changed.
 * 
 * @author uqdalves
 */
@SuppressWarnings("serial")
public class STRectangle extends Rectangle implements SpatialTemporalObject {
    /**
     * Time interval
     */
    private final long t1, t2;

    /**
     * Creates a new empty spatial-temporal rectangle.
     */
    public STRectangle() {
        super(0, 0, 0, 0);
        this.t1 = 0;
        this.t2 = 0;
    }

    /**
     * Create a new spatial-temporal rectangle with the given
     * coordinates and time-stamp interval.
     *
     * @param minX Lower-left corner X.
     * @param minY Lower-left corner Y.
     * @param maxX Upper-right corner X.
     * @param maxY Upper-right corner Y.
     * @param t1   Time-stamp start.
     * @param t2   Time-stamp end.
     */
    public STRectangle(double minX, double minY, double maxX, double maxY,
                       long t1, long t2) {
        super(minX, minY, maxX, maxY);
        this.t1 = t1;
        this.t2 = t2;
    }

    @Override
    public long timeStart() {
        return t1;
    }

    @Override
    public long timeFinal() {
        return t2;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + (int) (t2 ^ (t2 >>> 32));
        result = prime * result + (int) (t1 ^ (t1 >>> 32));
        return result;
    }

    @Override
    public STRectangle clone() {
        STRectangle clone = new STRectangle(
                minX(), minY(), maxX(), maxY(), t1, t2);
        super.cloneTo(clone);
        return clone;
    }

    @Override
    public boolean equalsST(SpatialTemporalObject obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (obj instanceof STRectangle) {
            STRectangle r = (STRectangle) obj;
            if (!super.equals2D(r)) return false;
            return (r.t1 == t1 && r.t2 == t2);
        }
        return false;
    }

    @Override
    public String toString() {
        String s = "( ";
        s += super.toString() + " " + t1 + " " + t2;
        return s + " )";
    }

    @Override
    public void print() {
        println("ST_RECTANGLE " + toString());
    }
}
