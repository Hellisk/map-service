package traminer.util.spatial.objects;

import com.vividsolutions.jts.geom.Geometry;
import org.graphstream.graph.Graph;
import org.graphstream.graph.implementations.SingleGraph;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.List;

/**
 * A 2D circle, defined by its center (x,y) coordinates 
 * and radius. 
 * <p>
 * Circle objects may contain both spatial and semantic 
 * attributes. Spatial attributes of simple objects,
 * however, are immutable, that means once a Circle object
 * is created its spatial attributes cannot be changed.
 * 
 * @author uqdalves
 */
@SuppressWarnings("serial")
public class Circle extends SimpleSpatialObject {
    /**
     * Circle center X coordinate
     */
    private final double centerX;
    /**
     * Circle center Y coordinate
     */
    private final double centerY;
    /**
     * The radius of this circle
     */
    private final double radius;

    /**
     * Creates an empty circle, with center (0,0)
     * and zero radius.
     */
    public Circle() {
        this.centerX = 0.0;
        this.centerY = 0.0;
        this.radius = 0.0;
    }

    /**
     * Creates a new circle with the given center and radius.
     *
     * @param x      Circle's center X coordinate.
     * @param y      Circle's center Y coordinate.
     * @param radius Circle's radius.
     */
    public Circle(double x, double y, double radius) {
        this.centerX = x;
        this.centerY = y;
        this.radius = radius;
    }

    /**
     * @return The X coordinates of the center of this circle.
     */
    public double x() {
        return centerX;
    }

    /**
     * @return The Y coordinates of the center of this circle.
     */
    public double y() {
        return centerY;
    }

    /**
     * @return The radius of this circle.
     */
    public double radius() {
        return radius;
    }

    @Override
    public List<Point> getCoordinates() {
        ArrayList<Point> list = new ArrayList<>(1);
        list.add(center()); // only the center
        return list;
    }

    @Override
    public List<Segment> getEdges() {
        // no edges in circle
        return new ArrayList<Segment>(0);
    }

    @Override
    public boolean isClosed() {
        return true;
    }

    /**
     * @return The perimeter of the circle.
     */
    public double perimeter() {
        return (2 * PI * radius);
    }

    /**
     * @return The area of the circle.
     */
    public double area() {
        return (PI * radius * radius);
    }

    /**
     * @return The center of this circle as a coordinate point.
     */
    public Point center() {
        return new Point(centerX, centerY);
    }

    @Override
    public Rectangle mbr() {
        double minX = centerX - radius;
        double minY = centerY - radius;
        double maxX = centerX + radius;
        double maxY = centerY + radius;
        return new Rectangle(minX, minY, maxX, maxY);
    }

    @Override
    public boolean equals2D(SpatialObject obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (obj instanceof Circle) {
            Circle c = (Circle) obj;
            return (c.centerX == centerX &&
                    c.centerY == centerY &&
                    c.radius == radius);
        }
        return false;
    }

    @Override
    public Circle clone() {
        Circle clone = new Circle(centerX, centerY, radius);
        super.cloneTo(clone);
        return clone;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        long temp;
        temp = Double.doubleToLongBits(centerX);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(centerY);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(radius);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public boolean contains(SpatialObject obj) {
        if (obj == null) {
            return false;
        }
        if (obj instanceof Point)
            return contains((Point) obj);
        if (obj instanceof Segment)
            return contains((Segment) obj);
        if (obj instanceof Circle)
            return contains((Circle) obj);

        // any other spatial object
        for (Segment s : obj.getEdges()) {
            if (!this.contains(s)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean within(SpatialObject obj) {
        if (obj == null) {
            return false;
        }
        if (obj instanceof Point)
            return false;
        if (obj instanceof Segment)
            return false;
        if (obj instanceof Circle)
            return obj.contains(this);

        // any other spatial object
        if (!obj.isClosed()) {
            return false;
        }
        Point center = this.center();
        if (!obj.contains(center)) {
            return false;
        }
        for (Point p : this.getCoordinates()) {
            if (p.distance(center) < this.radius) {
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean disjoint(SpatialObject obj) {
        if (obj == null) {
            return false;
        }
        if (obj instanceof Point)
            return disjoint((Point) obj);
        if (obj instanceof Segment)
            return disjoint((Segment) obj);
        if (obj instanceof Circle)
            return disjoint((Circle) obj);

        // any other spatial object
        for (Segment s : obj.getEdges()) {
            if (!this.disjoint(s)) {
                return false;
            }
        }

        // TODO report change from ture to !obj.covers(new Point(centerX, centerY))
        return !obj.covers(new Point(centerX, centerY));
    }

    @Override
    public boolean intersects(SpatialObject obj) {
        if (obj == null) {
            return false;
        }
        return !disjoint(obj);
    }

    //@Override
    public boolean touches(SpatialObject obj) {
        if (obj == null) {
            return false;
        }
        if (obj instanceof Point)
            return touches((Point) obj);
        if (obj instanceof Segment)
            return touches((Segment) obj);
        if (obj instanceof Circle)
            return touches((Circle) obj);

        // any other spatial object
        for (Segment s : obj.getEdges()) {
            if (this.touches(s)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean crosses(SpatialObject obj) {
        if (obj == null) {
            return false;
        }
        if (obj instanceof Point)
            return crosses((Point) obj);
        if (obj instanceof Segment)
            return crosses((Segment) obj);
        if (obj instanceof Circle)
            return crosses((Circle) obj);

        // any other spatial object
        for (Segment s : obj.getEdges()) {
            if (this.crosses(s)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean overlaps(SpatialObject obj) {
        if (obj == null) {
            return false;
        }
        if (obj instanceof Circle)
            return overlaps((Circle) obj);

        // any other spatial object
        if (!obj.isClosed()) return false;

        return this.crosses(obj);
    }

    @Override
    public boolean covers(SpatialObject obj) {
        return contains(obj);
    }

    @Override
    public boolean coveredBy(SpatialObject obj) {
        if (obj == null) {
            return false;
        }
        return obj.covers(this);
    }

    /**
     * Check whether the point lies inside the circle area.
     *
     * @param p The point to check.
     * @return True is this circle contains the given point
     * inside its perimeter.
     */
    private boolean contains(Point p) {
        double dist = p.distance(centerX, centerY);
        return (dist <= radius);
    }

    /**
     * Check whether the segment lies inside the circle area.
     *
     * @param s The segment to check.
     * @return True is this circle contains the given segment
     * inside its perimeter.
     */
    private boolean contains(Segment s) {
        double distP1 = s.p1().distance(centerX, centerY);
        double distP2 = s.p2().distance(centerX, centerY);
        return (distP1 <= radius && distP2 <= radius);
    }

    /**
     * Check whether the circle c lies inside this circle area.
     *
     * @param c The circle to check.
     * @return True is this circle contains the given circle c
     * inside its perimeter.
     */
    private boolean contains(Circle c) {
        double dist = c.center().distance(centerX, centerY);
        return ((dist + c.radius) <= radius);
    }

    /**
     * Check whether this circle and the given point are disjoint.
     *
     * @param p The point to check.
     * @return True if this circle and the point are disjoint.
     */
    private boolean disjoint(Point p) {
        double dist = p.distance(centerX, centerY);
        return (dist > radius);
    }

    /**
     * Check whether this circle and the given segment are disjoint.
     *
     * @param s The segment to check.
     * @return True if this circle and the segment are disjoint.
     */
    private boolean disjoint(Segment s) {
        // shortest distance
        double dist = s.distance(centerX, centerY);
        return (dist > radius);
    }

    /**
     * Check whether these two circles are disjoint.
     *
     * @param c The circle to check.
     * @return True if these two circles are disjoint.
     */
    private boolean disjoint(Circle c) {
        double dist = c.center().distance(centerX, centerY);
        return (dist > (c.radius + this.radius));
    }

    /**
     * Check whether this circle touches the given point.
     *
     * @param p The point to check.
     * @return True if the perimeter of this circle touches
     * the given point.
     */
    private boolean touches(Point p) {
        double dist = p.distance(centerX, centerY);
        return (dist == radius);
    }

    /**
     * Check whether this circle touches the given segment.
     *
     * @param s The segment to check.
     * @return True if the perimeter of this circle touches
     * the given segment.
     */
    private boolean touches(Segment s) {
        // shortest distance
        double dist = s.distance(centerX, centerY);
        return (dist == radius);
    }

    /**
     * Check whether this circle touches the given circle.
     *
     * @param c The circle to check.
     * @return True if the perimeters of these two circles touch.
     */
    private boolean touches(Circle c) {
        double dist = c.center().distance(centerX, centerY);
        return (dist == (c.radius + this.radius));
    }

    /**
     * Check whether this circle crosses with this point.
     *
     * @param p The point to check.
     * @return Always false y definition.
     */
    private boolean crosses(Point p) {
        return false;
    }

    /**
     * Check whether this circle crosses with the given segment.
     *
     * @param s The segment to check.
     * @return True if this circle crosses with this segment.
     */
    private boolean crosses(Segment s) {
        if (this.disjoint(s)) return false;
        return !this.contains(s.p1()) ||
                !this.contains(s.p2());
    }

    /**
     * Check whether these circles cross each other.
     *
     * @param c The circle to check.
     * @return True if these two circles cross each other.
     */
    private boolean crosses(Circle c) {
        return this.overlaps(c);
    }

    /**
     * Check whether these circles overlap.
     *
     * @param c The circle to check.
     * @return True if these two circles overlap.
     */
    private boolean overlaps(Circle c) {
        //return (this.intersects(c) && !this.equals2D(c));
        double dist = this.center().distance(c.center());
        // contains
        if (radius >= c.radius && dist <= (radius - c.radius))
            return false;
        // within
        if (c.radius >= radius && dist <= (c.radius - radius))
            return false;
        // disjoint
        return !(dist > (radius + c.radius));
    }

    /**
     * Convert this circle object to a AWT Circle2D object.
     *
     * @return The Circle2D (AWT) representation of this circle.
     */
    public Circle2D toCircle2D() {
        return new Circle2D(centerX, centerY, radius);
    }

    @Override
    public Geometry toJTSGeometry() {
        return null; // no accurate implementation in JTS
    }

    @Override
    public String toString() {
        String s = "( " + center().toString() + ", " + radius + " )";
        return s;
    }

    @Override
    public void print() {
        println("CIRCLE " + toString());
    }

    @Override
    public void display() {
        Graph graph = new SingleGraph("Point");
        graph.display(false);
        graph.addNode("N0").setAttribute("xy", centerX, centerY);
        Graphics2D g = (Graphics2D) graph;
        int x = (int) (centerX - (radius / 2));
        int y = (int) (centerY - (radius / 2));
        int r = (int) radius;
        g.fillOval(x, y, r, r);
    }

    /**
     * Auxiliary Circle2D object. Adaptation of an
     * Ellipse2D from the java.awt.geom geometry library.
     */
    private static class Circle2D extends Ellipse2D.Double {
        /**
         * Creates a new Circle2D with the given center and radius.
         *
         * @param x      Circle's center X coordinate.
         * @param y      Circle's center Y coordinate.
         * @param radius Circle's radius.
         */
        public Circle2D(double x, double y, double radius) {
            super(x, y, radius, radius);
        }
    }
}
