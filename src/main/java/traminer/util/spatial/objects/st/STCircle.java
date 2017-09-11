package traminer.util.spatial.objects.st;

import traminer.util.spatial.objects.Circle;

/**
 * A 2D Spatial-Temporal circle (circle with temporal feature), composed by a 
 * spatial region (circle perimeter) and a time interval [t1,t2].
 * <p>
 * STCircle objects may contain both spatial-temporal and 
 * semantic attributes. Spatial-temporal attributes of ST
 * spatial objects, however, are immutable, that means once 
 * a STCircle object is created its spatial attributes cannot 
 * be changed.
 *  
 * @author uqdalves
 */
@SuppressWarnings("serial")
public class STCircle extends Circle implements SpatialTemporalObject {
    /**
     * Time interval
     */
    private final long t1, t2;

    /**
     * Creates a new empty spatial-temporal Circle.
     */
    public STCircle() {
        super(0, 0, 0);
        this.t1 = 0;
        this.t2 = 0;
    }

    /**
     * Creates a new spatial-temporal Circle with the given
     * dimensions and time-stamp interval.
     *
     * @param x      Circle's center X coordinate.
     * @param y      Circle's center Y coordinate.
     * @param radius Circle's radius.
     * @param t1     Time-stamp start.
     * @param t2     Time-stamp end.
     */
    public STCircle(double x, double y, double radius, long t1, long t2) {
        super(x, y, radius);
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
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (obj instanceof STCircle) {
            STCircle c = (STCircle) obj;
            if (!super.equals2D(c)) return false;
            return (c.t1 == t1 && c.t2 == t2);
        }
        return false;
    }

    @Override
    public STCircle clone() {
        STCircle clone = new STCircle(x(), y(), radius(), t1, t2);
        super.cloneTo(clone);
        return clone;
    }

    @Override
    public String toString() {
        String s = "( ";
        s += super.toString() + " " + t1 + " " + t2;
        return s + " )";
    }

    @Override
    public void print() {
        println("ST_CIRCLE " + toString());
    }
}
