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
 * Segment objects may contain both spatial and semantic
 * attributes. Spatial attributes of simple objects,
 * however, are immutable, that means once a Segment object
 * is created its spatial attributes cannot be changed.
 *
 * @author uqdalves
 */
@SuppressWarnings("serial")
public class Segment extends SimpleSpatialObject {
    /**
     * Start-point coordinates
     */
    private final double x1, y1;
    /**
     * End-point coordinates
     */
    private final double x2, y2;

    /**
     * Auxiliary LineSegment from JTS oldversion
     */
    private
    LineSegment JTSLineSegment = null;

    /**
     * Creates a new empty line segment.
     */
    public Segment() {
        this.x1 = 0.0;
        this.y1 = 0.0;
        this.x2 = 0.0;
        this.y2 = 0.0;
    }

    /**
     * Creates a new line segment with the given start
     * and end coordinates.
     *
     * @param x1 Start-point X coordinate.
     * @param y1 Start-point Y coordinate.
     * @param x2 End-point X coordinate.
     * @param y2 End-point Y coordinate.
     */
    public Segment(double x1, double y1, double x2, double y2) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
    }

    /**
     * @return Start-point X coordinate.
     */
    public double x1() {
        return x1;
    }

    /**
     * @return Start-point Y coordinate.
     */
    public double y1() {
        return y1;
    }

    /**
     * @return End-point X coordinate.
     */
    public double x2() {
        return x2;
    }

    /**
     * @return End-point Y coordinate.
     */
    public double y2() {
        return y2;
    }

    @Override
    public List<Point> getCoordinates() {
        ArrayList<Point> list = new ArrayList<>(2);
        list.add(p1());
        list.add(p2());
        return list;
    }

    @Override
    public List<Segment> getEdges() {
        List<Segment> list = new ArrayList<>(1);
        list.add(this);
        return list;
    }

    @Override
    public boolean isClosed() {
        return false;
    }

    /**
     * @return The length of this line segment, using default
     * Euclidean distance.
     */
    public double length() {
        return length(new EuclideanDistanceFunction());
    }

    /**
     * @param distFunc The distance function to use.
     * @return The length of this line segment using the given
     * points distance function.
     */
    public double length(PointDistanceFunction distFunc) {
        if (distFunc == null) {
            throw new NullPointerException("Distance function for "
                    + "length calculation must not be null.");
        }
        return distFunc.pointToPointDistance(x1, y1, x2, y2);
    }

    /**
     * Check whether this segment is vertical, i.e.
     * parallel to the Y axis.
     *
     * @return True is this segment is vertical.
     */
    public boolean isVertical() {
        return (Double.doubleToLongBits(x1) ==
                Double.doubleToLongBits(x2));
    }

    /**
     * Check whether this segment is horizontal, i.e.
     * parallel to the X axis.
     *
     * @return True is this segment is vertical.
     */
    public boolean isHorizontal() {
        return (Double.doubleToLongBits(y1) ==
                Double.doubleToLongBits(y2));
    }

    /**
     * Check whether this edge touches the given point.
     *
     * @param p The point to check.
     * @return True if this segment and p touch.
     */
    public boolean touches(Point p) {
        if (p == null) return false;
        return contains(p.x(), p.y());
    }

	/*
    //@Override
	public boolean touches(SpatialObject obj){
		if (obj instanceof Point){
			Point p = (Point)obj;
			return contains(p.x(),p.y());
		}
		if (obj instanceof Segment) {
			return this.touches((Segment)obj);
		}
		if (!obj.isClosed()) {
			for(Segment s : obj.getEdges()){
				if(this.touches(s)){
					return true;
				}
			}
		} else {

		}

		return false;
	}

	private boolean touches(Segment s){
		return (contains(s.x1, s.y1) || contains(s.x2, s.y2));
	}
	*/

    /**
     * Check whether this segment and the segment s = (x1,y1)(x2,y2)
     * intersect, i.e. have at least one point in common.
     *
     * @param x1 Segment S start-point.
     * @param y1 Segment S start-point.
     * @param x2 Segment S end-point.
     * @param y2 Segment S end-point.
     * @return True if these two line segments intersect.
     */
    public boolean intersects(double x1, double y1, double x2, double y2) {
        return segmentsIntersect(x1, y1, x2, y2, this.x1, this.y1, this.x2, this.y2);
    }

    /**
     * Check whether this segment and the segment s = (x1,y1)(x2,y2)
     * cross, i.e. have only one point in common.
     *
     * @param x1 Segment S start-point.
     * @param y1 Segment S start-point.
     * @param x2 Segment S end-point.
     * @param y2 Segment S end-point.
     * @return True if these two line segments cross.
     */
    public boolean crosses(double x1, double y1, double x2, double y2) {
        return segmentsCross(x1, y1, x2, y2, this.x1, this.y1, this.x2, this.y2);
    }

    /**
     * Check whether the given point p = (x,y) is on this segment.
     *
     * @param x
     * @param y
     * @return True if this segment contains the point p = (x,y).
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
     * Find and return the point where these two line segments
     * intersect (if any).
     *
     * @param s
     * @return The intersection point between this segment and s.
     * Return null if they do not cross.
     */
    public Point getIntersection(Segment s) {
        if (s == null) return null;
        Coordinate c = this.toJTSLineSegment().
                intersection(s.toJTSLineSegment());
        if (c == null) {
            return null;
        }
        return new Point(c.x, c.y);
    }

    /**
     * Calculate the projection of the given point p
     * onto this line segment.
     *
     * @param p The point to project onto this segment.
     * @return The projection of p onto this line segment.
     */
    public Point getProjection(Point p) {
        if (p == null) {
            throw new NullPointerException("Point for point-segment "
                    + "projection must not be null.");
        }
        return pointToSegmentProjection(p.x(), p.y(), x1, y1, x2, y2);
    }

    /**
     * Calculate the projection of the given point p = (x,y)
     * onto this line segment.
     *
     * @param x
     * @param y
     * @return The projection of p = (x,y) onto this line segment.
     */
    public Point getProjection(double x, double y) {
        return pointToSegmentProjection(x, y, x1, y1, x2, y2);
    }

    /**
     * Get the first point of this segment.
     *
     * @return The segment's start-point (x1,y1).
     */
    public Point p1() {
        return new Point(x1, y1);
    }

    /**
     * Get the second point of this segment.
     *
     * @return The segment's end-point (x2,y2).
     */
    public Point p2() {
        return new Point(x2, y2);
    }

    /**
     * Calculates the Euclidean distance between this line segment
     * and the given point (shortest distance).
     *
     * @param p
     * @return Point-to-segment Euclidean distance.
     */
    public double distance(Point p) {
        if (p == null) {
            throw new NullPointerException("Point for point-segment "
                    + "distance calculation must not be null.");
        }
        return new EuclideanDistanceFunction().pointToSegmentDistance(
                p.x(), p.y(), x1, y1, x2, y2);
    }

    /**
     * Calculates the Euclidean distance between this line segment
     * and the given point p = (x,y) (shortest distance).
     *
     * @param x
     * @param y
     * @return Point-to-segment Euclidean distance.
     */
    public double distance(double x, double y) {
        return new EuclideanDistanceFunction().pointToSegmentDistance(
                x, y, x1, y1, x2, y2);
    }

    /**
     * Calculates the Euclidean distance between these two
     * line segments (shortest distance).
     *
     * @param s
     * @return Segment-to-segment Euclidean distance.
     */
    public double distance(Segment s) {
        if (s == null) {
            throw new NullPointerException("Segment for segment-segment "
                    + "distance calculation must not be null.");
        }
        return new EuclideanDistanceFunction().segmentToSegmentDistance(
                s.x1, s.x2, s.y1, s.y2, x1, y1, x2, y2);
    }

    /**
     * Calculates the Euclidean distance between this line segment
     * and the segment s = (x1,y1)(x2,y2) (shortest distance).
     *
     * @param x1 Segment S start-point.
     * @param y1 Segment S start-point.
     * @param x2 Segment S end-point.
     * @param y2 Segment S end-point.
     * @return Segment-to-segment Euclidean distance.
     */
    public double distance(double x1, double y1, double x2, double y2) {
        return new EuclideanDistanceFunction().segmentToSegmentDistance(
                x1, y1, x2, y2, this.x1, this.y1, this.x2, this.y2);
    }

    /**
     * Check whether these two line segments R and S intersect
     * (if they have at least one point in common).
     *
     * @param rx1 Segment R start-point.
     * @param ry1 Segment R start-point.
     * @param rx2 Segment R end-point.
     * @param ry2 Segment R end-point.
     * @param sx1 Segment S start-point.
     * @param sy1 Segment S start-point.
     * @param sx2 Segment S end-point.
     * @param sy2 Segment S end-point.
     * @return True if R and S intersect each other.
     */
    public static boolean segmentsIntersect(
            double rx1, double ry1, double rx2, double ry2,
            double sx1, double sy1, double sx2, double sy2) {
        // vectors r and s
        double rx = rx2 - rx1;
        double ry = ry2 - ry1;
        double sx = sx2 - sx1;
        double sy = sy2 - sy1;

        // cross product r x s
        double cross = (rx * sy) - (ry * sx);

        // they are parallel or colinear
        return !(cross == 0.0);
    }

    /**
     * Check whether these two line segments R and S cross
     * (if they have only one point in common).
     *
     * @param rx1 Segment R start-point.
     * @param ry1 Segment R start-point.
     * @param rx2 Segment R end-point.
     * @param ry2 Segment R end-point.
     * @param sx1 Segment S start-point.
     * @param sy1 Segment S start-point.
     * @param sx2 Segment S end-point.
     * @param sy2 Segment S end-point.
     * @return True if R and S cross each other.
     */
    public static boolean segmentsCross(
            double rx1, double ry1, double rx2, double ry2,
            double sx1, double sy1, double sx2, double sy2) {
        // vectors r and s
        double rx = rx2 - rx1;
        double ry = ry2 - ry1;
        double sx = sx2 - sx1;
        double sy = sy2 - sy1;

        // cross product r x s
        double cross = (rx * sy) - (ry * sx);

        // they are parallel or colinear
        if (cross == 0.0) return false;

        double t = (sx1 - rx1) * sy - (sy1 - ry1) * sx;
        t = t / cross;
        double u = (sx1 - rx1) * ry - (sy1 - ry1) * rx;
        u = u / cross;

        return t > 0.0 && t < 1.0 &&
                u > 0.0 && u < 1.0;
    }

    /**
     * Calculate the projection of a given spatial point p = (x,y)
     * on to the given line segment s = (x1,y1)(x2,y2).
     *
     * @param x  Point to project.
     * @param y  Point to project.
     * @param x1 Line segment start-point.
     * @param y1 Line segment start-point.
     * @param x2 Line segment end-point.
     * @param y2 Line segment end-point.
     * @return Return the projection of p = (x,y) onto s = (x1,y1)(x2,y2).
     */
    public static Point pointToSegmentProjection(double x, double y,
                                                 double x1, double y1, double x2, double y2) {
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
     * Convert this line segment to a AWT Line2D object.
     *
     * @return The Line2D representation of this line segment.
     */
    public Line2D toLine2D() {
        return new Line2D.Double(x1, y1, x2, y2);
    }

    @Override
    public String toString() {
        String s = "( ";
        s += String.format("%.5f %.5f", x1, y1) + ", ";
        s += String.format("%.5f %.5f", x2, y2);
        return s + " )";
    }

    @Override
    public void print() {
        println("SEGMENT " + toString());
    }

    @Override
    public void display() {
        Graph graph = new SingleGraph("Line Segment");
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
        if (obj instanceof Segment) {
            Segment s = (Segment) obj;
            return (s.x1 == x1 && s.y1 == y1 &&
                    s.x2 == x2 && s.y2 == y2);
        }
        return false;
    }

    @Override
    public boolean equals2D(SpatialObject obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (obj instanceof Segment) {
            Segment s = (Segment) obj;
            return (s.x1 == x1 && s.y1 == y1 &&
                    s.x2 == x2 && s.y2 == y2);
        }
        return false;
    }

    @Override
    public Segment clone() {
        Segment clone = new Segment(x1, y1, x2, y2);
        super.cloneTo(clone);
        return clone;
    }

    @Override
    public Geometry toJTSGeometry() {
        return toJTSLineSegment().toGeometry(new GeometryFactory());
    }

    /**
     * Cast this line segment to an equivalent JavaTopologicalSuit (JTS)
     * LineSegment.
     *
     * @return JTS LineSegment
     */
    private LineSegment toJTSLineSegment() {
        if (JTSLineSegment == null) {
            JTSLineSegment = new LineSegment(x1, y1, x2, y2);
        }
        return JTSLineSegment;
    }
}
