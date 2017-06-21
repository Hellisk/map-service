package traminer.util.spatial.objects;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.impl.PackedCoordinateSequence;
import org.graphstream.graph.Graph;
import org.graphstream.graph.implementations.SingleGraph;
import traminer.util.spatial.distance.EuclideanDistanceFunction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Implements a N-Dimensional point entity, with
 * (x1, x2, ..., xn) coordinates.
 * <p>
 * N-Dimensional Point objects may contain both spatial
 * and semantic attributes. Spatial attributes of simple
 * objects, however, are immutable, that means once a
 * N-Dimensional Point object is created, its spatial
 * attributes cannot be changed.
 *
 * @author uqdalves
 */
@SuppressWarnings("serial")
public class PointND extends SimpleSpatialObject {
    private final double[] coordinates;

    // auxiliary point from JTS lib
    private com.vividsolutions.jts.geom.
            Point JTSPoint = null;

    /**
     * Creates an empty d-Dimensional point.
     *
     * @param d Dimension
     */
    public PointND(byte d) {
        this.setDimension(d);
        this.coordinates = new double[d];
    }

    /**
     * Creates a new d-Dimensional point from
     * the given coordinates. Dimension is set
     * as the number of elements in the array.
     */
    public PointND(double[] coordinates) {
        this.coordinates = coordinates;
        this.setDimension((byte) coordinates.length);
    }

    /**
     * Get the N-dimensional coordinates of this PointND
     * as an array of N doubles.
     */
    public double[] getCoordinate() {
        return coordinates;
    }

    @Override
    public List<Point> getCoordinates() {
        ArrayList<Point> list = new ArrayList<Point>();
        list.add(toPoint2D());
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

    /**
     * Returns the Euclidean distance between
     * this point and a given point p.
     */
    public double distance(PointND p) {
        return new EuclideanDistanceFunction()
                .pointToPointDistance(this.coordinates, p.coordinates);
    }

    /**
     * Returns the Euclidean distance between
     * this point and a given point p.
     * Point given by its coordinates vector.
     */
    public double distance(double[] vec) {
        return new EuclideanDistanceFunction()
                .pointToPointDistance(this.coordinates, vec);
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

    public boolean equalsND(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (obj instanceof PointND) {
            PointND p = (PointND) obj;
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

    public boolean equals3D(SimpleSpatialObject obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (obj instanceof PointND) {
            PointND p = (PointND) obj;
            if (this.getDimension() < 3 || p.getDimension() < 3) return false;
            return (coordinates[0] == p.coordinates[0] &&
                    coordinates[1] == p.coordinates[1] &&
                    coordinates[2] == p.coordinates[2]);
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
     * Return this N-dimensional point as a
     * simple Point (2D) object.
     */
    public Point toPoint2D() {
        double x = 0, y = 0;
        if (getDimension() > 0) {
            x = coordinates[0];
        }
        if (getDimension() > 1) {
            y = coordinates[1];
        }
        return new Point(x, y);
    }

    @Override
    public String toString() {
        String s = "";
        for (int i = 0; i < getDimension(); i++) {
            s += " " + String.format("%.3f", coordinates[i]);
        }
        return s.substring(1);
    }

    @Override
    public void print() {
        System.out.println("POINT" + getDimension() + "D ( " + toString() + " )");
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
