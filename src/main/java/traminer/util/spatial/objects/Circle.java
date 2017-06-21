package traminer.util.spatial.objects;

import com.vividsolutions.jts.geom.Geometry;
import org.graphstream.graph.Graph;
import org.graphstream.graph.implementations.SingleGraph;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.List;

/**
 * A 2D circle, defined by its center (x,y)
 * coordinates and radius.
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
    private final double center_x;
    /**
     * Circle center Y coordinate
     */
    private final double center_y;
    /**
     * The radius of this circle
     */
    private final double radius;

    /**
     * Creates an empty circle.
     */
    public Circle() {
        this.center_x = 0.0;
        this.center_y = 0.0;
        this.radius = 0.0;
    }

    public Circle(double center_x, double center_y, double radius) {
        this.center_x = center_x;
        this.center_y = center_y;
        this.radius = radius;
    }

    /**
     * The X coordinates of the center of this circle.
     */
    public double x() {
        return center_x;
    }

    /**
     * The Y coordinates of the center of this circle.
     */
    public double y() {
        return center_y;
    }

    /**
     * The radius of this circle.
     */
    public double radius() {
        return radius;
    }

    @Override
    public List<Point> getCoordinates() {
        ArrayList<Point> list = new ArrayList<Point>();
        list.add(center()); // only the center
        return list;
    }

    @Override
    public List<Edges> getEdges() {
        // no edges in circle
        return new ArrayList<Edges>();
    }

    @Override
    public boolean isClosed() {
        return true;
    }

    public double perimeter() {
        return (2 * PI * radius);
    }

    public double area() {
        return (PI * radius * radius);
    }

    /**
     * The center of this circle as a coordinate point.
     */
    public Point center() {
        return new Point(center_x, center_y);
    }

    /**
     * Get the Minimum Bounding Rectangle (MBR)
     * of this circle.
     */
    public Rectangle mbr() {
        double min_x = center_x - radius;
        double min_y = center_y - radius;
        double max_x = center_x + radius;
        double max_y = center_y + radius;
        return new Rectangle(min_x, max_x, min_y, max_y);
    }

    @Override
    public boolean equals2D(SpatialObject obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (obj instanceof Circle) {
            Circle c = (Circle) obj;
            return (c.center_x == center_x &&
                    c.center_y == center_y &&
                    c.radius == radius);
        }
        return false;
    }

    @Override
    public Circle clone() {
        Circle clone = new Circle(center_x, center_y, radius);
        super.cloneTo(clone);
        return clone;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        long temp;
        temp = Double.doubleToLongBits(center_x);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(center_y);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(radius);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public boolean contains(SpatialObject obj) {
        if (obj instanceof Point)
            return contains((Point) obj);
        if (obj instanceof Edges)
            return contains((Edges) obj);
        if (obj instanceof Circle)
            return contains((Circle) obj);

        // any other spatial object
        for (Edges s : obj.getEdges()) {
            if (!this.contains(s)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean within(SpatialObject obj) {
        if (obj instanceof Point)
            return false;
        if (obj instanceof Edges)
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
        if (obj instanceof Point)
            return disjoint((Point) obj);
        if (obj instanceof Edges)
            return disjoint((Edges) obj);
        if (obj instanceof Circle)
            return disjoint((Circle) obj);

        // any other spatial object
        for (Edges s : obj.getEdges()) {
            if (!this.disjoint(s)) {
                return false;
            }
        }
        return !obj.covers(new Point(center_x, center_y));
    }

    @Override
    public boolean intersects(SpatialObject obj) {
        return !disjoint(obj);
    }

    //@Override
    public boolean touches(SpatialObject obj) {
        if (obj instanceof Point)
            return touches((Point) obj);
        if (obj instanceof Edges)
            return touches((Edges) obj);
        if (obj instanceof Circle)
            return touches((Circle) obj);

        // any other spatial object
        for (Edges s : obj.getEdges()) {
            if (this.touches(s)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean crosses(SpatialObject obj) {
        if (obj instanceof Point)
            return crosses((Point) obj);
        if (obj instanceof Edges)
            return crosses((Edges) obj);
        if (obj instanceof Circle)
            return crosses((Circle) obj);

        // any other spatial object
        for (Edges s : obj.getEdges()) {
            if (this.crosses(s)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean overlaps(SpatialObject obj) {
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
        return obj.covers(this);
    }

    /**
     * True is this circle contains the given point inside its perimeter.
     * Check if the point lies inside the circle area.
     */
    private boolean contains(Point p) {
        double dist = p.distance(center_x, center_y);
        return (dist <= radius);
    }

    /**
     * True is this circle contains the given segment inside its perimeter.
     * Check if the segment lies inside the circle area.
     */
    private boolean contains(Edges s) {
        double dist_p1 = s.p1().distance(center_x, center_y);
        double dist_p2 = s.p2().distance(center_x, center_y);
        return (dist_p1 <= radius && dist_p2 <= radius);
    }

    /**
     * True is this circle contains the given circle c inside its perimeter.
     * Check if the circle c lies inside this circle area.
     */
    private boolean contains(Circle c) {
        double dist = c.center().distance(center_x, center_y);
        return ((dist + c.radius) <= radius);
    }

    /**
     * True if this point and the circle are disjoint.
     */
    private boolean disjoint(Point p) {
        double dist = p.distance(center_x, center_y);
        return (dist > radius);
    }

    /**
     * True if this segment and the circle are disjoint.
     */
    private boolean disjoint(Edges s) {
        // shortest distance
        double dist = s.distance(center_x, center_y);
        return (dist > radius);
    }

    /**
     * True if these two circles are disjoint.
     */
    private boolean disjoint(Circle c) {
        double dist = c.center().distance(center_x, center_y);
        return (dist > (c.radius + this.radius));
    }

    /**
     * True if the given point touches this circle (circle perimeter).
     */
    private boolean touches(Point p) {
        double dist = p.distance(center_x, center_y);
        return (dist == radius);
    }

    /**
     * True if the given segment touches this circle (circle perimeter).
     */
    private boolean touches(Edges s) {
        // shortest distance
        double dist = s.distance(center_x, center_y);
        return (dist == radius);
    }

    /**
     * True if these two circles touch (circle's perimeter).
     */
    private boolean touches(Circle c) {
        double dist = c.center().distance(center_x, center_y);
        return (dist == (c.radius + this.radius));
    }

    /**
     * True if this circle crosses this point.
     */
    private boolean crosses(Point p) {
        return false;
    }

    /**
     * True if this circle crosses this segment.
     */
    private boolean crosses(Edges s) {
        if (this.disjoint(s)) return false;
        return !this.contains(s.p1()) ||
                !this.contains(s.p2());
    }

    /**
     * True if these two circles cross each other.
     */
    private boolean crosses(Circle c) {
        return this.overlaps(c);
    }

    /**
     * True if these two circles overlap.
     */
    private boolean overlaps(Circle c) {
        //return (this.intersects(c) && !this.equals2D(c));
        double dist = this.center().distance(c.center());
        // contains
        if (radius >= c.radius && dist <= (radius - c.radius)) return false;
        // within
        if (c.radius >= radius && dist <= (c.radius - radius)) return false;
        // disjoint
        return !(dist > (radius + c.radius));
    }

    /**
     * Get the Circle2D (AWT) representation of this circle.
     */
    public Circle2D toCircle2D() {
        return new Circle2D(center_x, center_y, radius);
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
        System.out.println("CIRCLE " + toString());
    }

    @Override
    public void display() {
        Graph graph = new SingleGraph("Point");
        graph.display(false);
        graph.addNode("N0").setAttribute("xy", center_x, center_y);
        Graphics2D g = (Graphics2D) graph;
        int x = (int) (center_x - (radius / 2));
        int y = (int) (center_y - (radius / 2));
        int r = (int) radius;
        g.fillOval(x, y, r, r);
    }

    /**
     * Auxiliary Circle2D object.
     * Adaptation of an Ellipse2D from the java.awt.geom geometry library.
     */
    private static class Circle2D extends Ellipse2D.Double {
        public Circle2D(double center_x, double center_y, double radius) {
            super(center_x, center_y, radius, radius);
        }
    }
}
