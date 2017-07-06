package edu.uq.dke.mapupdate.mapmatching.io;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.impl.PackedCoordinateSequence;
import org.graphstream.graph.Graph;
import org.graphstream.graph.implementations.SingleGraph;
import traminer.util.spatial.distance.EuclideanDistanceFunction;
import traminer.util.spatial.distance.PointDistanceFunction;
import traminer.util.spatial.objects.*;

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
public class PointWithSegment extends SimpleSpatialObject {
    private final double x;
    private final double y;
    private final ArrayList<Segment> adjacentSegments;

    // auxiliary point from JTS lib
    private com.vividsolutions.jts.geom.
            Point JTSPoint = null;

    /**
     * Creates an empty Point with default
     * (0,0,0) coordinates.
     */
    public PointWithSegment() {
        this.x = 0.0;
        this.y = 0.0;
        this.adjacentSegments = new ArrayList<>();
    }

    /**
     * Creates a 2D point with the given
     * coordinates, and z set as zero.
     */
    public PointWithSegment(double x, double y) {
        this.x = x;
        this.y = y;
        this.adjacentSegments = new ArrayList<>();
    }

    /**
     * Create a 2D point with the given
     * coordinates and connected road list.
     */
    public PointWithSegment(double x, double y, List<Segment> edges) {
        this.x = x;
        this.y = y;
        this.adjacentSegments = new ArrayList<>();
        adjacentSegments.addAll(edges);
    }

    public Point toPoint() {
        return new Point(this.x, this.y);
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public Point getPoint() {
        return new Point(this.x(), this.y());
    }

    /**
     * Return the list of segments whose end point is this point.
     */
    public List<Segment> getAdjacentSegments() {
        return this.adjacentSegments;
    }

    /**
     * Add an adjacent segment to this point
     *
     * @param segment
     * @return true if added successfully, otherwise false
     */

    public boolean addAdjacentSegment(Segment segment) {
        return this.adjacentSegments.add(segment);
    }

    /**
     * Returns the default Euclidean distance between
     * this point and a given point p.
     */
    public double distance(PointWithSegment p) {
        return new EuclideanDistanceFunction()
                .distance(p.getPoint(), this.getPoint());
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
    public double distance(PointWithSegment p, PointDistanceFunction dist) {
        return dist.distance(p.getPoint(), this.getPoint());
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
        return new double[]{x, y};
    }

    @Override
    public List<Point> getCoordinates() {
        ArrayList<Point> list = new ArrayList<Point>();
        list.add(this.getPoint());
        return list;
    }

    @Override
    public List<Segment> getEdges() {
        return new ArrayList<Segment>();
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
            return ((Segment) obj).touches(this.getPoint());
        }
        if (obj instanceof Circle) {
            return ((Circle) obj).touches(this);
        }
        for (Segment s : obj.getEdges()) {
            if (s.touches(this.getPoint())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public PointWithSegment clone() {
        PointWithSegment clone = new PointWithSegment(x, y, this.getAdjacentSegments());
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
        if (obj instanceof PointWithSegment) {
            PointWithSegment p = (PointWithSegment) obj;
            for (Segment t : p.getAdjacentSegments()) {
                if (!this.getAdjacentSegments().contains(t)) {
                    return false;
                }
            }
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
        Graph graph = new SingleGraph("Point with segments");
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
    public int compareTo(PointWithSegment p, Comparator<PointWithSegment> comparator) {
        return comparator.compare(this, p);
    }

    /**
     * Compare points by X value.
     */
    public static final Comparator<PointWithSegment> X_COMPARATOR =
            new Comparator<PointWithSegment>() {
                @Override
                public int compare(PointWithSegment o1, PointWithSegment o2) {
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
    public static final Comparator<PointWithSegment> Y_COMPARATOR =
            new Comparator<PointWithSegment>() {
                @Override
                public int compare(PointWithSegment o1, PointWithSegment o2) {
                    if (o1.y < o2.y)
                        return -1;
                    if (o1.y > o2.y)
                        return 1;
                    return 0;
                }
            };

}
