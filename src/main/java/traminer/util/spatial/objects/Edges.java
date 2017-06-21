package traminer.util.spatial.objects;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineSegment;
import org.graphstream.graph.Graph;
import org.graphstream.graph.implementations.SingleGraph;
import traminer.util.spatial.distance.EuclideanDistanceFunction;
import traminer.util.spatial.distance.PointDistanceFunction;

import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.List;

/**
 * A 2D line segment object. Line segment from coordinate
 * points (x1,y1) to (x2,y2).
 * <p>
 * Edges objects may contain both spatial and semantic
 * attributes. Spatial attributes of simple objects,
 * however, are immutable, that means once a Edges object
 * is created its spatial attributes cannot be changed.
 *
 * @author uqdalves
 */
@SuppressWarnings("serial")
public class Edges extends SimpleSpatialObject {
    /**
     * Start-point
     */
    private final double x1, y1;
    /**
     * End-point
     */
    private final double x2, y2;

    // auxiliary LineSegment from JTS lib
    private
    LineSegment JTSLineSegment = null;

    /**
     * Creates an empty line segment.
     */
    public Edges() {
        this.x1 = 0.0;
        this.y1 = 0.0;
        this.x2 = 0.0;
        this.y2 = 0.0;
    }

    public Edges(double x1, double y1,
                 double x2, double y2) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
    }

    public double x1() {
        return x1;
    }

    public double y1() {
        return y1;
    }

    public double x2() {
        return x2;
    }

    public double y2() {
        return y2;
    }

    @Override
    public List<Point> getCoordinates() {
        ArrayList<Point> list = new ArrayList<Point>();
        list.add(p1());
        list.add(p2());
        return list;
    }

    @Override
    public List<Edges> getEdges() {
        ArrayList<Edges> list = new ArrayList<Edges>();
        list.add(this);
        return list;
    }

    @Override
    public boolean isClosed() {
        return false;
    }

    /**
     * The length of this line segment.
     */
    public double lenght(PointDistanceFunction dist) {
        return dist.pointToPointDistance(x1, y1, x2, y2);
    }

    /**
     * The default Euclidean length of this line segment.
     */
    public double lenght() {
        return lenght(new EuclideanDistanceFunction());
    }

    /**
     * True is this segment is parallel to the Y axis.
     */
    public boolean isVertical() {
        return (Double.doubleToLongBits(x1) ==
                Double.doubleToLongBits(x2));
    }

    /**
     * True is this segment is parallel to the X axis.
     */
    public boolean isHorizontal() {
        return (Double.doubleToLongBits(y1) ==
                Double.doubleToLongBits(y2));
    }

    // TODO add override
    //@Override
    public boolean touches(SpatialObject obj) {
        if (obj instanceof Point) {
            Point p = (Point) obj;
            return contains(p.x(), p.y());
        }
        if (obj instanceof Edges) {
            return this.touches((Edges) obj);
        }
        if (!obj.isClosed()) {
            for (Edges s : obj.getEdges()) {
                if (this.touches(s)) {
                    return true;
                }
            }
        } else {
// TODO
        }


        return false;
    }

    private boolean touches(Edges s) {
        return (contains(s.x1, s.y1) || contains(s.x2, s.y2));
    }

    /**
     * True if these two line segments intersect. Line segment
     * given by x and y endpoint coordinates.
     */
    public boolean intersects(double x1, double y1, double x2, double y2) {
        return segmentsIntersect(x1, y1, x2, y2, this.x1, this.y1, this.x2, this.y2);
    }

    /**
     * True if these two line segments cross (have only one point in common).
     * Line segment given by x and y endpoint coordinates.
     */
    public boolean crosses(double x1, double y1, double x2, double y2) {
        return segmentsCross(x1, y1, x2, y2, this.x1, this.y1, this.x2, this.y2);
    }

    /**
     * Check if the given point p = (x,y) is on this segment.
     */
    public boolean contains(double x, double y) {
        double sx = x1;
        double sy = y1;
        double ex = x2;
        double ey = y2;
        double temp;

        if (sx > ex) {
            temp = sx;
            sx = ex;
            ex = temp;
        }
        if (sy > ey) {
            temp = sy;
            sy = ey;
            ey = temp;
        }

        return x >= sx && x < ex && y >= sy && y < ey;
    }

    /**
     * Find and return the point where these two
     * line segments intersect (if any).
     */
    public Point getIntersection(Edges s) {
        Coordinate c = this.toJTSLineSegment().
                intersection(s.toJTSLineSegment());
        return new Point(c.x, c.y);
    }

    /**
     * Calculate the projection of the given point p
     * on to this line segment.
     */
    public Point getProjection(Point p) {
        return pointToSegmentProjection(p.x(), p.y(), x1, y1, x2, y2);
    }

    /**
     * Calculate the projection of the given point p = (x,y)
     * on to this line segment.
     */
    public Point getProjection(double x, double y) {
        return pointToSegmentProjection(x, y, x1, y1, x2, y2);
    }

    /**
     * Get the fist segment endpoint (x1,y1).
     */
    public Point p1() {
        return new Point(x1, y1);
    }

    /**
     * Get the second segment endpoint (x2,y2).
     */
    public Point p2() {
        return new Point(x2, y2);
    }

    /**
     * The distance between this line segment and
     * the given point (shortest distance).
     */
    public double distance(Point p) {
        return new EuclideanDistanceFunction().pointToSegmentDistance(
                p.x(), p.y(), x1, y1, x2, y2);
    }

    /**
     * The distance between this line segment and
     * the given point (shortest distance). Point
     * given by x and y coordinates.
     */
    public double distance(double x, double y) {
        return new EuclideanDistanceFunction().pointToSegmentDistance(
                x, y, x1, y1, x2, y2);
    }

    /**
     * The Euclidean distance between these two line segments
     * (shortest distance).
     */
    public double distance(Edges s) {
        return new EuclideanDistanceFunction().segmentToSegmentDistance(
                s.x1, s.x2, s.y1, s.y2, x1, y1, x2, y2);

    }

    /**
     * The Euclidean distance between these two line segments
     * (shortest distance). Edges given by x and y
     * end points coordinates.
     */
    public double distance(double x1, double y1, double x2, double y2) {
        return new EuclideanDistanceFunction().segmentToSegmentDistance(
                x1, y1, x2, y2, this.x1, this.y1, this.x2, this.y2);
    }

    /**
     * Check if these two line segments S and R intersect
     * (if they have at least one point in common).
     */
    public static boolean segmentsIntersect(
            double r_x1, double r_y1, double r_x2, double r_y2,
            double s_x1, double s_y1, double s_x2, double s_y2) {
        // vectors r and s
        double rx = r_x2 - r_x1;
        double ry = r_y2 - r_y1;
        double sx = s_x2 - s_x1;
        double sy = s_y2 - s_y1;

        // cross product r x s
        double cross = (rx * sy) - (ry * sx);

        // they are parallel or colinear
        return !(cross == 0.0);
    }

    /**
     * Check if these two line segments S and R cross
     * (if they have only one point in common).
     */
    public static boolean segmentsCross(
            double r_x1, double r_y1, double r_x2, double r_y2,
            double s_x1, double s_y1, double s_x2, double s_y2) {
        // vectors r and s
        double rx = r_x2 - r_x1;
        double ry = r_y2 - r_y1;
        double sx = s_x2 - s_x1;
        double sy = s_y2 - s_y1;

        // cross product r x s
        double cross = (rx * sy) - (ry * sx);

        // they are parallel or colinear
        if (cross == 0.0) return false;

        double t = (s_x1 - r_x1) * sy - (s_y1 - r_y1) * sx;
        t = t / cross;
        double u = (s_x1 - r_x1) * ry - (s_y1 - r_y1) * rx;
        u = u / cross;

        return t > 0.0 && t < 1.0 &&
                u > 0.0 && u < 1.0;
    }

    /**
     * Calculate the projection of a given spatial point p = (x,y)
     * on to the given line segment s = (x1,y1)--(x2,y2).
     *
     * @return Return the projection of p onto s.
     */
    public static Point pointToSegmentProjection(
            double x, double y,
            double x1, double y1,
            double x2, double y2) {
        // segments vector
        double v1x = x2 - x1;
        double v1y = y2 - y1;
        double v2x = x - x1;
        double v2y = y - y1;

        // get squared length of this segment e
        double len2 = (x2 - x1) * (x2 - x1) +
                (y2 - y1) * (y2 - y1);

        // p1 and p2 are the same point
        if (len2 == 0) {
            return new Point(x1, y1);
        }

        // the projection falls where
        // d = [(p - p1) . (p2 - p1)] / |p2 - p1|^2
        double d = Vector2D.dotProduct(v2x, v2y, v1x, v1y) / len2;

        // "Before" s.p1 on the line
        if (d < 0.0) {
            return new Point(x1, y1);
        }
        // after s.p2 on the line
        if (d > 1.0) {
            return new Point(x2, y2);
        }

        // projection is "in between" s.p1 and s.p2
        // get projection coordinates
        double px = x1 + d * (x2 - x1);
        double py = y1 + d * (y2 - y1);

        return new Point(px, py);
    }

    /**
     * Get the Line2D (AWT) representation of this segment.
     */
    public Line2D toLine2D() {
        return new Line2D.Double(x1, y1, x2, y2);
    }

    @Override
    public String toString() {
        String s = "( ";
        s += String.format("%.3f %.3f", x1, y1) + ", ";
        s += String.format("%.3f %.3f", x2, y2);
        return s + " )";
    }

    @Override
    public void print() {
        System.out.println("SEGMENT " + toString());
    }

    @Override
    public void display() {
        Graph graph = new SingleGraph("Line Edges");
        graph.display(false);
        graph.addNode("N1").setAttribute("xy", x1, y1);
        graph.addNode("N2").setAttribute("xy", x2, y2);
        graph.addEdge("E12", "N1", "N2");
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        long temp;
        temp = Double.doubleToLongBits(x1);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(x2);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(y1);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(y2);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (obj instanceof Edges) {
            Edges s = (Edges) obj;
            return (s.x1 == x1 && s.y1 == y1 &&
                    s.x2 == x2 && s.y2 == y2);
        }
        return false;
    }

    @Override
    public boolean equals2D(SpatialObject obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (obj instanceof Edges) {
            Edges s = (Edges) obj;
            return (s.x1 == x1 && s.y1 == y1 &&
                    s.x2 == x2 && s.y2 == y2);
        }
        return false;
    }

    @Override
    public Edges clone() {
        Edges clone = new Edges(x1, y1, x2, y2);
        super.cloneTo(clone);
        return clone;
    }

    @Override
    public Geometry toJTSGeometry() {
        return toJTSLineSegment().toGeometry(new GeometryFactory());
    }

    protected LineSegment toJTSLineSegment() {
        if (JTSLineSegment == null) {
            JTSLineSegment = new LineSegment(x1, y1, x2, y2);
        }
        return JTSLineSegment;
    }
}
