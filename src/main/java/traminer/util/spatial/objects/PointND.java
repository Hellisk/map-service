package traminer.util.spatial.objects;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.impl.PackedCoordinateSequence;
import org.graphstream.graph.Graph;
import org.graphstream.graph.implementations.SingleGraph;
import traminer.util.spatial.distance.EuclideanDistanceFunction;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Implements a N-dimensional spatial point, with
 * (x1, x2, ..., xn) coordinates.
 * <p>
 * N-dimensional points may contain both spatial and semantic 
 * attributes. Spatial attributes of simple objects, however, 
 * are immutable, that means once a N-dimensional Point object 
 * is created, its spatial attributes cannot be changed. 
 * 
 * @author uqdalves
 */
@SuppressWarnings("serial")
public class PointND extends SimpleSpatialObject {
    /**
     * The coordinates of this spatial point
     */
    private final double[] coordinates;

    /**
     * Auxiliary point from JTS oldversion
     */
    private com.vividsolutions.jts.geom.
            Point JTSPoint = null;

    /**
     * Creates an empty d-Dimensional point.
     *
     * @param d Number of dimensions.
     */
    public PointND(byte d) {
        this.setDimension(d);
        this.coordinates = new double[d];
    }

    /**
     * Creates a new d-Dimensional point from the given
     * list of coordinates. Dimension is set as the
     * number of elements in the array.
     *
     * @param coordinates The coordinates of this spatial point.
     */
    public PointND(double[] coordinates) {
        this.coordinates = coordinates;
        this.setDimension((byte) coordinates.length);
    }

    /**
     * @return Array with the coordinates of this PointND.
     */
    public double[] getCoordinate() {
        return coordinates;
    }

    @Override
    public List<Point> getCoordinates() {
        ArrayList<Point> list = new ArrayList<Point>();
        list.add(toPoint());
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

    /**
     * Returns the Euclidean distance between
     * this PointND and a given point p.
     *
     * @param p
     * @return Euclidean distance between this and p.
     */
    public double distance(PointND p) {
        return new EuclideanDistanceFunction()
                .distance(this.coordinates, p.coordinates);
    }

    /**
     * Returns the Euclidean distance between this PointND
     * and the point given by its coordinates vector.
     *
     * @param coordinates
     * @return Euclidean distance between this and the given point.
     */
    public double distance(double[] coordinates) {
        return new EuclideanDistanceFunction()
                .distance(this.coordinates, coordinates);
    }

    @Override
    public PointND clone() {
        // deep copy
        PointND clone = new PointND(this.getDimension());
        for (int i = 0; i < this.getDimension(); i++) {
            clone.coordinates[i] = this.coordinates[i];
        }
        super.cloneTo(clone);
        return clone;
    }

    /**
     * Check whether these two points have the same dimension,
     * and the same spatial coordinates.
     *
     * @param obj The N-dimensional point to check.
     * @return True if these two PointND are spatially equivalent.
     */
    public boolean equalsND(PointND obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (obj instanceof PointND) {
            PointND p = obj;
            if (this.getDimension() != p.getDimension()) return false;
            for (int i = 0; i < getDimension(); i++) {
                if (this.coordinates[i] != p.coordinates[i]) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean equals2D(SpatialObject obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (obj instanceof PointND) {
            PointND p = (PointND) obj;
            if (this.getDimension() < 2 || p.getDimension() < 2) return false;
            return (coordinates[0] == p.coordinates[0] &&
                    coordinates[1] == p.coordinates[1]);
        }
        return false;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(coordinates);
        result = prime * result + getDimension();
        return result;
    }

    /**
     * Convert this N-dimensional point to a Point (x,y) object.
     *
     * @return A Point with the (x,y) coordinates of this PointND.
     */
    public Point toPoint() {
        double x = 0, y = 0;
        if (getDimension() > 0) {
            x = coordinates[0];
        }
        if (getDimension() > 1) {
            y = coordinates[1];
        }
        return new Point(x, y);
    }

    /**
     * Convert this PointND object to a AWT Point2D object.
     *
     * @return The Point2D representation of this PointND.
     */
    public Point2D toPoint2D() {
        double x = 0, y = 0;
        if (getDimension() > 0) {
            x = coordinates[0];
        }
        if (getDimension() > 1) {
            y = coordinates[1];
        }
        return new Point2D.Double(x, y);
    }

    @Override
    public String toString() {
        String s = "";
        for (int i = 0; i < getDimension(); i++) {
            s += " " + String.format("%.5f", coordinates[i]);
        }
        return s.substring(1);
    }

    @Override
    public void print() {
        println("POINT" + getDimension() + "D ( " + toString() + " )");
    }

    @Override
    public void display() {
        if (coordinates == null || getDimension() < 2) return;
        Graph graph = new SingleGraph("PointND");
        graph.display(false);
        graph.addNode("N0").setAttribute("xy", coordinates[0], coordinates[1]);
    }

    @Override
    public Geometry toJTSGeometry() {
        if (JTSPoint == null) {
            PackedCoordinateSequence.Double coord =
                    new PackedCoordinateSequence.Double(this.getCoordinate(), getDimension());
            JTSPoint = new com.vividsolutions.jts.geom.
                    Point(coord, new GeometryFactory());
        }
        return JTSPoint;
    }
}
