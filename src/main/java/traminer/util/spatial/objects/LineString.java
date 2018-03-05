package traminer.util.spatial.objects;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.impl.PackedCoordinateSequence;
import org.graphstream.graph.Graph;
import org.graphstream.graph.implementations.SingleGraph;
import traminer.util.exceptions.SpatialObjectConstructionException;
import traminer.util.spatial.distance.PointDistanceFunction;

import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Implements a mutable LineString entity.
 * <p>
 * LineString are composed of a list of connected 
 * 2D segments, and may contain both spatial and 
 * semantic attributes.
 * 
 * @author uqdalves
 */
@SuppressWarnings("serial")
public class LineString extends ComplexSpatialObject<Segment> {
    /**
     * Auxiliary LineString from JTS oldversion
     */
    private com.vividsolutions.jts.geom.
            LineString JTSLineString = null;

    /**
     * Creates an empty LineString.
     */
    public LineString() {
    }

    /**
     * Creates a new LineString from the given list of points.
     *
     * @param pointList A ordered list of points.
     */
    public LineString(List<? extends Point> pointList) {
        if (pointList == null) {
            throw new NullPointerException("Points list for "
                    + "LineString construction must not be null.");
        }
        if (pointList.size() < 2) {
            throw new SpatialObjectConstructionException(
                    "LineString must have at least 2 points.");
        }
        Point pi, pj;
        for (int i = 0; i < pointList.size() - 1; i++) {
            pi = pointList.get(i);
            pj = pointList.get(i + 1);
            Segment seg = new Segment(pi.x(), pi.y(), pj.x(), pj.y());
            this.add(seg);
        }
    }

    /**
     * Creates a new LineString from the given sequence of points.
     *
     * @param points A ordered sequence of points.
     */
    public LineString(Point... points) {
        if (points == null) {
            throw new NullPointerException("Points list for "
                    + "LineString construction must not be null.");
        }
        if (points.length < 2) {
            throw new SpatialObjectConstructionException(
                    "LineString must have at least 2 points.");
        }
        Point pi, pj;
        for (int i = 0; i < points.length - 1; i++) {
            pi = points[i];
            pj = points[i + 1];
            Segment seg = new Segment(pi.x(), pi.y(), pj.x(), pj.y());
            this.add(seg);
        }
    }

    /**
     * The length of this LineString.
     *
     * @param distFunc the points distance measure to use.
     */
    public double length(PointDistanceFunction distFunc) {
        if (distFunc == null) {
            throw new NullPointerException("Distance function "
                    + "must not be null.");
        }
        double sum = 0.0;
        if (!isEmpty()) {
            for (Segment s : this) {
                sum += s.length(distFunc);
            }
        }
        return sum;
    }

    /**
     * Get the reverse representation of this LineString.
     * <br>
     * This method does not change the original LineString.
     *
     * @return A copy of the reverse representation of this LineString.
     */
    public LineString reverse() {
        LineString reverse = new LineString();
        for (int i = size() - 1; i >= 0; i--) {
            Segment s = this.get(i);
            reverse.add(s.clone());
        }
        return reverse;
    }

    @Override
    public boolean isClosed() {
        this.toJTSGeometry();
        return JTSLineString.isClosed();
    }

    @Override
    public List<Point> getCoordinates() {
        List<Point> list = new ArrayList<Point>();
        if (!this.isEmpty()) {
            for (Segment s : this) {
                list.add(s.p1());
            }
            // check the last vertex
            Segment s = this.get(this.size() - 1);
            list.add(s.p2());
        }
        return list;
    }

    @Override
    public List<Segment> getEdges() {
        return this;
    }

    /**
     * Convert this LineString object to a AWT Path2D object.
     *
     * @return The Path2D representation of this LineString.
     */
    public Path2D toPath2D() {
        if (isEmpty()) {
            throw new SpatialObjectConstructionException("Segments list for Path2D "
                    + "construction must not be empty.");
        }
        List<Point> pList = getCoordinates();
        Path2D path = new Path2D.Double();
        path.moveTo(pList.get(0).x(), pList.get(0).y());
        for (int i = 1; i < pList.size(); i++) {
            Point p = pList.get(i);
            path.lineTo(p.x(), p.y());
        }

        return path;
    }

    @Override
    public boolean equals2D(SpatialObject obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (obj instanceof LineString) {
            LineString c = (LineString) obj;
            if (this.size() != c.size()) {
                return false;
            }
            for (int i = 0; i < size(); i++) {
                if (!c.get(i).equals2D(this.get(i))) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        String s = "(";
        for (Point p : getCoordinates()) {
            s += ", " + p.toString();
        }
        s.replaceFirst(", ", "");
        return s + ")";
    }

    @Override
    public LineString clone() {
        LineString clone = new LineString();
        for (Segment s : this) {
            clone.add(s.clone());
        }
        super.cloneTo(clone);
        return clone;
    }

    @Override
    public void print() {
        println("LINESTRING " + toString());
    }

    @Override
    public void display() {
        if (isEmpty()) return;

        Graph graph = new SingleGraph("LineString");
        graph.display(false);
        // create one node per trajectory point
        Segment s = this.get(0);
        graph.addNode("N0").setAttribute("xy", s.x1(), s.y1());
        graph.addNode("N1").setAttribute("xy", s.x2(), s.y2());
        graph.addEdge("E1", "N0", "N1");
        for (int i = 1; i < this.size(); i++) {
            s = this.get(i);
            graph.addNode("N" + (i + 1)).setAttribute("xy", s.x2(), s.y2());
            graph.addEdge("E" + (i + 1), "N" + i, "N" + (i + 1));
        }
    }

    @Override
    public Geometry toJTSGeometry() {
        if (JTSLineString == null) {
            Coordinate[] coords = new Coordinate[size()];
            int i = 0;
            for (Point p : getCoordinates()) {
                coords[i++] = new Coordinate(p.x(), p.y());
            }
            PackedCoordinateSequence points =
                    new PackedCoordinateSequence.Double(coords);

            JTSLineString = new com.vividsolutions.jts.geom.
                    LineString(points, new GeometryFactory());
        }
        return JTSLineString;
    }
}
