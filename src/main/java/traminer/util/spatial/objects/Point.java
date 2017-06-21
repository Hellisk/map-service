package traminer.util.spatial.objects;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.impl.PackedCoordinateSequence;
import org.graphstream.graph.Graph;
import org.graphstream.graph.implementations.SingleGraph;
import traminer.util.spatial.distance.EuclideanDistanceFunction;
import traminer.util.spatial.distance.PointDistanceFunction;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Implements a simple 2D point entity, with
 * (x,y) coordinates.
 * <p>
 * Point objects may contain both spatial and semantic
 * attributes. Spatial attributes of simple objects,
 * however, are immutable, that means once a Point object
 * is created its spatial attributes cannot be changed.
 *
 * @author uqdalves
 */
@SuppressWarnings("serial")
public class Point extends SimpleSpatialObject {
    private final double x;
    private final double y;

    // auxiliary point from JTS lib
    private com.vividsolutions.jts.geom.
            Point JTSPoint = null;

    /**
     * Creates an empty Point with default
     * (0,0,0) coordinates.
     */
    public Point() {
        this.x = 0.0;
        this.y = 0.0;
    }

    /**
     * Creates a 2D point with the given
     * coordinates, and z set as zero.
     */
    public Point(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    /**
     * Returns the default Euclidean distance between
     * this point and a given point p.
     */
    public double distance(Point p) {
        return new EuclideanDistanceFunction()
                .pointToPointDistance(p, this);
    }

    /**
     * Returns the default Euclidean distance between
     * this point and a given point p = (x,y).
     */
    public double distance(double x, double y) {
        return new EuclideanDistanceFunction()
                .pointToPointDistance(x, y, this.x, this.y);
    }

    /**
     * Returns the distance between this point
     * and a given point p.
     *
     * @param dist The point distance measure to use.
     */
    public double distance(Point p, PointDistanceFunction dist) {
        return dist.pointToPointDistance(p, this);
    }

    /**
     * Returns the distance between this point
     * and a given point p = (x,y).
     *
     * @param dist The point distance measure to use.
     */
    public double distance(double x, double y, PointDistanceFunction dist) {
        return dist.pointToPointDistance(x, y, this.x, this.y);
    }

    /**
     * Get the (x,y,z) coordinates of this Point as an array of doubles.
     */
    public double[] getCoordinate() {
        double[] coord = new double[]{x, y};
        return coord;
    }

    @Override
    public List<Point> getCoordinates() {
        ArrayList<Point> list = new ArrayList<Point>();
        list.add(this);
        return list;
    }

    @Override
    public List<Edges> getEdges() {
        return new ArrayList<Edges>();
    }

    @Override
    public boolean isClosed() {
        return false;
    }

    // TODO add override
    //@Override
    public boolean touches(SpatialObject obj) {
        if (obj instanceof Point) {
            return false;
        }
        if (obj instanceof Edges) {
            return ((Edges) obj).touches(this);
        }
        if (obj instanceof Circle) {
            return ((Circle) obj).touches(this);
        }
        for (Edges s : obj.getEdges()) {
            if (s.touches(this)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Point clone() {
        Point clone = new Point(x, y);
        super.cloneTo(clone); // clone semantics
        return clone;
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

    @Override
    public boolean equals2D(SpatialObject obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (obj instanceof Point) {
            Point p = (Point) obj;
            return (p.x == x && p.y == y);
        }
        return false;
    }

    /**
     * Get the AWT Point2D representation of this point.
     */
    public Point2D toPoint2D() {
        return new Point2D.Double(x, y);
    }

    @Override
    public String toString() {
        String s = String.format("%.5f %.5f", x, y);
        return s;
    }

    @Override
    public void print() {
        System.out.println("POINT (" + toString() + ")");
    }

    @Override
    public void display() {
        Graph graph = new SingleGraph("Point");
        graph.display(false);
        graph.addNode("N0").setAttribute("xyz", x, y, 0);
    }

    @Override
    public Geometry toJTSGeometry() {
        if (JTSPoint == null) {
            PackedCoordinateSequence.Double coord =
                    new PackedCoordinateSequence.Double(this.getCoordinate(), 2);
            JTSPoint = new com.vividsolutions.jts.geom.
                    Point(coord, new GeometryFactory());
        }
        return JTSPoint;
    }

    /**
     * Compare two points by using the given comparator.
     */
    public int compareTo(Point p, Comparator<Point> comparator) {
        return comparator.compare(this, p);
    }

    /**
     * Compare points by X value.
     */
    public static final Comparator<Point> X_COMPARATOR =
            new Comparator<Point>() {
                @Override
                public int compare(Point o1, Point o2) {
                    if (o1.x < o2.x)
                        return -1;
                    if (o1.x > o2.x)
                        return 1;
                    return 0;
                }
            };

    /**
     * Compare points by Y value.
     */
    public static final Comparator<Point> Y_COMPARATOR =
            new Comparator<Point>() {
                @Override
                public int compare(Point o1, Point o2) {
                    if (o1.y < o2.y)
                        return -1;
                    if (o1.y > o2.y)
                        return 1;
                    return 0;
                }
            };

}
