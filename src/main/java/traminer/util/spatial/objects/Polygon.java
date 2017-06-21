package traminer.util.spatial.objects;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.impl.PackedCoordinateSequence;
import org.graphstream.graph.Graph;
import org.graphstream.graph.implementations.SingleGraph;

import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Implements a mutable 2D Polygon entity.
 * <p>
 * Internally, a polygon comprises of a list of (x,y) coordinate pairs,
 * where each pair defines a vertex of the polygon, and two successive
 * pairs are the end-points of a line that is a side of the polygon.
 * Polygon objects may contain both spatial and semantic attributes.
 *
 * @author uqdalves
 */
@SuppressWarnings("serial")
public class Polygon extends ComplexSpatialObject<Point> {
    private int size = 0;

    // auxiliary Polygon from JTS lib
    private com.vividsolutions.jts.geom.
            Polygon JTSPolygon = null;

    /**
     * Creates an empty polygon.
     */
    public Polygon() {
    }

    /**
     * Creates a Polygon and add its vertexes in the same
     * order as in the given points list.
     */
    public Polygon(List<? extends Point> vertexList) {
        for (Point p : vertexList) {
            this.add(p);
        }
    }

    /**
     * Create a Polygon and add its vertexes in the same
     * order as in the given vertex list.
     */
    public Polygon(Point... vertexes) {
        for (Point p : vertexes) {
            this.add(p);
        }
    }

    /**
     * The number of points (vertexes) in this polygon.
     */
    @Override
    public int size() {
        return size;
    }

    /**
     * Add the point p to the end of this polygon.
     */
    @Override
    public boolean add(Point p) {
        if (size == 0) {
            super.add(p);
        } else if (size == 1) {
            super.add(p);
            super.add(this.get(0)); // close polygon
        } else {
            super.add(size, p);
        }
        size++;
        return true;
    }

    /**
     * Add point p = (x,y) to the end of this polygon.
     */
    public boolean add(double x, double y) {
        return this.add(new Point(x, y));
    }

    /**
     * Add a list of points (vertexes) to the end of this polygon.
     */
    @Override
    public boolean addAll(Collection<? extends Point> list) {
        for (Point p : list) {
            this.add(p);
        }
        return true;
    }

    @Override
    public List<Point> getCoordinates() {
        return this;
    }

    @Override
    public List<Edges> getEdges() {
        List<Edges> list = new ArrayList<Edges>();
        Point pi, pj;
        for (int i = 0; i < size; i++) {
            pi = this.get(i);
            pj = this.get(i + 1);
            list.add(new Edges(pi.x(), pi.y(), pj.x(), pj.y()));
        }
        return list;
    }

    /**
     * The perimeter of this polygon.
     */
    public double perimeter() {
        double sum = 0.0;
        for (int i = 0; i < size; i++) {
            sum += this.get(i).distance(this.get(i + 1));
        }
        return sum;
    }

    /**
     * The signed area of this polygon.
     */
    public double area() {
        double sum = 0.0;
        for (int i = 0; i < size; i++) {
            sum = sum + (this.get(i).x() * this.get(i + 1).y()) -
                    (this.get(i).y() * this.get(i + 1).x());
        }
        return (0.5 * sum);
    }

    /**
     * Check if this Polygon contains the point p.
     * If p is on boundary then 0 or 1 is returned,
     * and p is in exactly one point of every partition of plane.
     * <p>
     * Uses ray casting algorithm.
     */
    public boolean contains2(Point p) {
        int crossings = 0;
        for (int i = 0; i < size; i++) {
            int j = i + 1;
            Point pi = this.get(i);
            Point pj = this.get(j);
            boolean cond1 = (pi.y() <= p.y()) && (p.y() < pj.y());
            boolean cond2 = (pj.y() <= p.y()) && (p.y() < pj.y());
            if (cond1 || cond2) {
                // need to cast to double
                if (p.x() < (pj.x() - pi.x()) * (p.y() - pi.y()) /
                        (pj.y() - pi.y()) + pi.x())
                    crossings++;
            }
        }
        return crossings % 2 == 1;
    }

    /**
     * Check if this Polygon contains the point p.
     * <p>
     * Uses winding number algorithm.
     */
    public boolean contains(Point p) {
        int winding = 0;
        for (int i = 0; i < size; i++) {
            Point pi = this.get(i);
            Point pj = this.get(i + 1);
            int ccw = SpatialObject.isCCW(pi, pj, p);
            if (pj.y() > p.y() && p.y() >= pi.y()) // upward crossing
                if (ccw == +1) winding++;
            if (pj.y() <= p.y() && p.y() < pi.y())  // downward crossing
                if (ccw == -1) winding--;
        }
        return winding != 0;
    }

    @Override
    public boolean isClosed() {
        return size > 1;
    }

    /**
     * Get the Polygon2D (AWT) representation of this polygon.
     */
    public Polygon2D toPolygon2D() {
        return new Polygon2D(this.subList(0, size));
    }


    @Override
    public Polygon clone() {
        Polygon clone = new Polygon();
        for (Point p : this) {
            clone.add(p.clone());
        }
        super.cloneTo(clone);
        return clone;
    }

    @Override
    public boolean equals2D(SpatialObject obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (obj instanceof Polygon) {
            Polygon poly = (Polygon) obj;
            if (this.size != poly.size) {
                return false;
            }
            for (int i = 0; i < size; i++) {
                if (!poly.get(i).equals2D(this.get(i))) {
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
        for (Point p : this) {
            s += ", " + p.toString();
        }
        s.replaceFirst(", ", "");
        return s + ")";
    }

    @Override
    public void display() {
        if (this.isEmpty()) return;

        Graph graph = new SingleGraph("Polygon");
        graph.display(false);
        // create one node per polygon vertex
        Point p = this.get(0);
        int size = this.size();
        graph.addNode("N0").setAttribute("xyz", p.x(), p.y(), 0);
        for (int i = 1; i < size; i++) {
            p = this.get(i);
            graph.addNode("N" + i).setAttribute("xyz", p.x(), p.y(), 0);
            graph.addEdge("E" + (i - 1) + "-" + i, "N" + (i - 1), "N" + i);
        }
        // close the polygon
        graph.addEdge("E" + size + "-" + 0, "N" + (size - 1), "N0");
    }

    @Override
    public void print() {
        System.out.println("POLYGON " + toString());
    }

    @Override
    public Geometry toJTSGeometry() {
        if (JTSPolygon == null) {
            Coordinate[] coords = new Coordinate[size + 1];
            int i = 0;
            for (Point p : this) {
                coords[i++] = new Coordinate(p.x(), p.y(), 0);
            }
            LinearRing shell = new LinearRing(new PackedCoordinateSequence.
                    Double(coords), new GeometryFactory());

            JTSPolygon = new com.vividsolutions.jts.geom.
                    Polygon(shell, null, new GeometryFactory());
        }
        return JTSPolygon;
    }

    /**
     * Auxiliary Polygon2D object. Adaptation of a
     * closed Path2D from the java.awt.geom library.
     */
    public static class Polygon2D extends Path2D.Double {
        public Polygon2D(List<Point> vextexList) {
            if (vextexList == null || vextexList.isEmpty()) {
                throw new IllegalArgumentException("Vertex list "
                        + "for Polygon2D must not be empty.");
            }
            this.moveTo(vextexList.get(0).x(), vextexList.get(0).y());
            for (int i = 1; i < vextexList.size(); i++) {
                Point p = vextexList.get(i);
                this.lineTo(p.x(), p.y());
            }
            this.closePath();
        }
    }
}
