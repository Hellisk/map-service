package mapupdate.util.object.spatialobject;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.impl.PackedCoordinateSequence;
import mapupdate.util.function.GreatCircleDistanceFunction;
import mapupdate.util.function.PointDistanceFunction;
import org.graphstream.graph.Graph;
import org.graphstream.graph.implementations.SingleGraph;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static mapupdate.Main.LOGGER;

/**
 * Implements a simple 2D point entity, with (x,y) coordinates.
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
    /**
     * Point coordinates
     */
    private final double x;
    private final double y;

    /**
     * Auxiliary point from JTS oldversion
     */
    private com.vividsolutions.jts.geom.
            Point JTSPoint = null;

    /**
     * Creates an empty Point with default (0,0) coordinates.
     */
    public Point() {
        this.x = 0.0;
        this.y = 0.0;
    }

    /**
     * Creates a 2D point with the given coordinates.
     *
     * @param x Point X/Longitude coordinate.
     * @param y Point Y/Latitude coordinate.
     */
    public Point(double x, double y) {
        this.x = x;
        this.y = y;
    }

    /**
     * @return Point X coordinate.
     */
    public double x() {
        return x;
    }

    /**
     * @return Point Y coordinate.
     */
    public double y() {
        return y;
    }

    /**
     * Returns the GreatCircle distance between
     * this point and a given point p.
     *
     * @param p
     * @return The GreatCircle distance between this point and p.
     */
    public double distance(Point p) {
        if (p == null) {
            throw new NullPointerException("Point for distance "
                    + "calculation must not be null.");
        }
        return new GreatCircleDistanceFunction()
                .distance(p, this);
    }

    /**
     * Returns the GreatCircle distance between
     * this point and a given point p = (x,y).
     *
     * @param x
     * @param y
     * @return The GreatCircle distance between this point
     * and p = (x,y).
     */
    public double distance(double x, double y) {
        return new GreatCircleDistanceFunction()
                .pointToPointDistance(x, y, this.x, this.y);
    }

    /**
     * Returns the distance between this point and the point p
     * using the given point distance function.
     *
     * @param p        The point to calculate the distance to.
     * @param distFunc The point distance function to use.
     * @return The distance between this point and p, w.r.t distFunc.
     */
    public double distance(Point p, PointDistanceFunction distFunc) {
        if (p == null) {
            throw new NullPointerException("Point for distance "
                    + "calculation must not be null.");
        }
        if (distFunc == null) {
            throw new NullPointerException("Distance function for "
                    + "point distance calculation must not be null.");
        }
        return distFunc.distance(p, this);
    }

    /**
     * Returns the distance between this point and the point
     * p = (x,y) using the given point distance function.
     *
     * @param x
     * @param y
     * @param distFunc The point distance function to use.
     * @return The distance between this point and p = (x,y), w.r.t distFunc.
     */
    public double distance(double x, double y, PointDistanceFunction distFunc) {
        if (distFunc == null) {
            throw new NullPointerException("Distance function for "
                    + "point distance calculation must not be null.");
        }
        return distFunc.pointToPointDistance(x, y, this.x, this.y);
    }

    /**
     * @return Array containing the [x,y] coordinates of this point.
     */
    public double[] getCoordinate() {
        double[] coord = new double[]{x, y};
        return coord;
    }

    @Override
    public List<Point> getCoordinates() {
        ArrayList<Point> list = new ArrayList<Point>(1);
        list.add(this);
        return list;
    }

    @Override
    public List<Segment> getEdges() {
        return new ArrayList<Segment>(0);
    }

    @Override
    public boolean isClosed() {
        return false;
    }

    //@Override
    public boolean touches(SpatialObject obj) {
        if (obj == null) {
            return false;
        }
        if (obj instanceof Point) {
            return false;
        }
        if (obj instanceof Segment) {
            return ((Segment) obj).touches(this);
        }
        if (obj instanceof Circle) {
            return ((Circle) obj).touches(this);
        }
        for (Segment s : obj.getEdges()) {
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
     * Convert this point object to a AWT Point2D object.
     *
     * @return The Point2D representation of this point.
     */
    public Point2D toPoint2D() {
        return new Point2D.Double(x, y);
    }

    @Override
    public String toString() {
        return String.format("%.5f %.5f", x, y);
    }

    @Override
    public void print() {
        LOGGER.info("POINT (" + toString() + ")");
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
     * Compares these two points for order using the given comparator.
     *
     * @param p          The point to compare to.
     * @param comparator The point comparator to use.
     * @return Returns a negative integer, zero, or a positive integer as this
     * point is less than, equal to, or greater than the given point p.
     */
    public int compareTo(Point p, Comparator<Point> comparator) {
        if (p == null) {
            throw new NullPointerException(
                    "Point for compareTo must not be null.");
        }
        if (comparator == null) {
            throw new NullPointerException(
                    "Point comparator must not be null.");
        }
        return comparator.compare(this, p);
    }

    /**
     * Comparator to compare points by their X value.
     */
    public static final Comparator<Point> X_COMPARATOR =
            Comparator.comparingDouble(o -> o.x);

    /**
     * Comparator to compare points by their Y value.
     */
    public static final Comparator<Point> Y_COMPARATOR =
            Comparator.comparingDouble(o -> o.y);

}
