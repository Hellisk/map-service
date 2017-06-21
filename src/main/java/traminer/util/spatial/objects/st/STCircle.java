package traminer.util.spatial.objects.st;

import traminer.util.spatial.objects.Circle;

/**
 * A 2D Spatial-Temporal circle, composed by a
 * spatial region (circle perimeter) and a time
 * interval [t1,t2].
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
     * Creates an empty STCircle.
     */
    public STCircle() {
        super();
        this.t1 = 0;
        this.t2 = 0;
    }

    public STCircle(double centerX, double centerY,
                    double radius, long t1, long t2) {
        super(centerX, centerY, radius);
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
    public boolean equalsST(SpatialTemporalObject obj) {
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
        STCircle clone = new STCircle(
                x(), y(), radius(), t1, t2);
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
        System.out.println("ST_CIRCLE " + toString());
    }
}
