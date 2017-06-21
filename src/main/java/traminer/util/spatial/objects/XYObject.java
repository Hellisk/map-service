package traminer.util.spatial.objects;

import traminer.util.spatial.SpatialInterface;

import java.util.Comparator;

/**
 * An immutable container for a simpler representation of
 * any spatial object as a 2D (x,y) coordinate point.
 * This object is mainly useful for indexing purposes.
 *
 * @param <T> Type of spatial object in this container.
 * @author uqdalves
 */
@SuppressWarnings("serial")
public final class XYObject<T extends SpatialObject> implements SpatialInterface {
    private final double x, y;
    private final T spatialObject;

    public XYObject(double x, double y, T spatialObj) {
        this.x = x;
        this.y = y;
        this.spatialObject = spatialObj;
    }

    public XYObject(double x, double y) {
        this.x = x;
        this.y = y;
        this.spatialObject = null;
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public T getSpatialObject() {
        return spatialObject;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        long temp;
        temp = Double.doubleToLongBits(x);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(y);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    public boolean equals2D(XYObject<T> obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        return (x == obj.x && y == obj.y);
    }

    @Override
    public String toString() {
        String s = String.format("%.5f %.5f", x, y);
        return s;
    }

    public void print() {
        System.out.println("XYOBJECT ( " + toString() + " )");
    }

    /**
     * The Point representation of the
     * spatial object in this XYObject.
     */
    public Point toPoint() {
        return new Point(x, y);
    }

    /**
     * Compare XYObjects by X value.
     */
    @SuppressWarnings("rawtypes")
    public static final Comparator<XYObject> X_COMPARATOR =
            new Comparator<XYObject>() {
                @Override
                public int compare(XYObject o1, XYObject o2) {
                    if (o1.x < o2.x)
                        return -1;
                    if (o1.x > o2.x)
                        return 1;
                    return 0;
                }
            };

    /**
     * Compare XYObjects by Y value.
     */
    @SuppressWarnings("rawtypes")
    public static final Comparator<XYObject> Y_COMPARATOR =
            new Comparator<XYObject>() {
                @Override
                public int compare(XYObject o1, XYObject o2) {
                    if (o1.y < o2.y)
                        return -1;
                    if (o1.y > o2.y)
                        return 1;
                    return 0;
                }
            };
}
