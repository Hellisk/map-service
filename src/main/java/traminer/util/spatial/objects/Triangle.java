package traminer.util.spatial.objects;

import com.vividsolutions.jts.geom.Geometry;
import org.graphstream.graph.Graph;
import org.graphstream.graph.implementations.SingleGraph;
import traminer.util.spatial.distance.EuclideanDistanceFunction;

import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;

/**
 * A 2D triangle entity, defined by its three
 * vertices coordinate points.
 * <p>
 * Triangle objects may contain both spatial and semantic 
 * attributes. Spatial attributes of simple objects, however, 
 * are immutable, that means once a Triangle object is 
 * created, its spatial attributes cannot be changed. 
 * 
 * @author uqdalves
 */
@SuppressWarnings("serial")
public class Triangle extends SimpleSpatialObject {
    /**
     * Triangle vertices
     */
    private final double v1x, v1y;
    private final double v2x, v2y;
    private final double v3x, v3y;
    /**
     * The list of edges of this triangle
     */
    private List<Segment> edgeList = null;

    /**
     * Creates a new empty triangle.
     */
    public Triangle() {
        this.v1x = 0.0;
        this.v1y = 0.0;
        this.v2x = 0.0;
        this.v2y = 0.0;
        this.v3x = 0.0;
        this.v3y = 0.0;
    }

    /**
     * Creates a new triangle with the given vertices.
     *
     * @param v1x Vertex 1x coordinate.
     * @param v1y Vertex 1y coordinate.
     * @param v2x Vertex 2x coordinate.
     * @param v2y Vertex 2y coordinate.
     * @param v3x Vertex 3x coordinate.
     * @param v3y Vertex 3y coordinate.
     */
    public Triangle(double v1x, double v1y,
                    double v2x, double v2y,
                    double v3x, double v3y) {
        this.v1x = v1x;
        this.v1y = v1y;
        this.v2x = v2x;
        this.v2y = v2y;
        this.v3x = v3x;
        this.v3y = v3y;
    }

    /**
     * @return Vertex V1.
     */
    public Point v1() {
        return new Point(v1x, v1y);
    }

    /**
     * @return Vertex V2.
     */
    public Point v2() {
        return new Point(v2x, v2y);
    }

    /**
     * @return Vertex V3.
     */
    public Point v3() {
        return new Point(v3x, v3y);
    }

    @Override
    public List<Point> getCoordinates() {
        List<Point> vertexes = new ArrayList<Point>(3);
        vertexes.add(v1());
        vertexes.add(v2());
        vertexes.add(v3());
        return vertexes;
    }

    @Override
    public List<Segment> getEdges() {
        if (edgeList == null) {
            edgeList = new ArrayList<Segment>(3);
            edgeList.add(new Segment(v1x, v1y, v2x, v2y));
            edgeList.add(new Segment(v1x, v1y, v3x, v3y));
            edgeList.add(new Segment(v2x, v2y, v3x, v3y));
        }
        return edgeList;
    }

    @Override
    public boolean isClosed() {
        return true;
    }

    /**
     * @return The coordinates of the circumcenter of this triangle.
     */
    public Point circumcenter() {
        double ax = v1x;
        double ay = v1y;
        double bx = v2x;
        double by = v2y;
        double cx = v3x;
        double cy = v3y;

        double d = 2.0 * (ax * (by - cy) + bx * (cy - ay) + cx * (ay - by));

        double centerX = (ax * ax + ay * ay) * (by - cy) +
                (bx * bx + by * by) * (cy - ay) +
                (cx * cx + cy * cy) * (ay - by);
        centerX = centerX / d;

        double centerY = (ax * ax + ay * ay) * (cx - bx) +
                (bx * bx + by * by) * (ax - cx) +
                (cx * cx + cy * cy) * (bx - ax);
        centerY = centerY / d;

        return new Point(centerX, centerY);
    }

    /**
     * @return The radius of the circumcircle of this triangle.
     */
    public double circumradius() {
        // triangle sides length
        double a = baseV1V2(); // |p1--p2|
        double b = baseV1V3(); // |p1--p3|
        double c = baseV2V3(); // |p2--p3|
        double radius = (a + b + c) * (b + c - a) *
                (c + a - b) * (a + b - c);
        radius = Math.sqrt(radius);
        radius = (a * b * c) / radius;

        return radius;
    }

    /**
     * @return The coordinates of the incenter of this triangle.
     */
    public Point incenter() {
        double a = baseV1V2();
        double b = baseV1V3();
        double c = baseV2V3();
        double x = (c * v1x + b * v2x + a * v3x) /
                (a + b + c);
        double y = (c * v1y + b * v2y + a * v3y) /
                (a + b + c);

        return new Point(x, y);
    }

    /**
     * @return The radius of the incircle of this triangle.
     */
    public double inradius() {
        double s = perimeter() / 2.0; // semiperimeter
        double a = baseV1V2();
        double b = baseV1V3();
        double c = baseV2V3();
        double radius = (s - a) * (s - b) * (s - c);
        radius = radius / s;
        radius = Math.sqrt(radius);
        return radius;
    }

    /**
     * @return The centroid (i.e. barycenter) of this triangle.
     */
    public Point centroid() {
        double x = v1x + v2x + v3x;
        double y = v1y + v2y + v3y;
        return new Point(x / 3.0, y / 3.0);
    }

	/*public void orthocenter(){
        double slopeAB = (vertex1.y - vertex2.y) / (vertex1.x - vertex2.x);
		double bAB = vertex1.y - (slopeAB*vertex1.x);
		
		double slopeBC = (vertex2.y - vertex3.y) / (vertex2.x - vertex3.x);
		double bBC = vertex2.y - (slopeBC*vertex2.x);
	}*/

    /**
     * @return The perimeter of this triangle.
     */
    public double perimeter() {
        return (baseV1V2() + baseV1V3() + baseV2V3());
    }

    /**
     * @return The area of this triangle.
     */
    public double area() {
        double area = (baseV1V2() * heightV3()) / 2.0;
        return area;
    }

    /**
     * Get the length of the edge V1V2 of this triangle.
     *
     * @return Triangle's base from vertex V1 to vertex V2.
     */
    public double baseV1V2() {
        return new EuclideanDistanceFunction()
                .pointToPointDistance(v1x, v1y, v2x, v2y);
    }

    /**
     * Get the length of the edge V1V3 of this triangle.
     *
     * @return Triangle's base from vertex V1 to vertex V3.
     */
    public double baseV1V3() {
        return new EuclideanDistanceFunction()
                .pointToPointDistance(v1x, v1y, v3x, v3y);
    }

    /**
     * Get the length of the edge V2V3 of this triangle.
     *
     * @return Triangle's base from vertex V2 to vertex V3.
     */
    public double baseV2V3() {
        return new EuclideanDistanceFunction()
                .pointToPointDistance(v2x, v2y, v3x, v3y);
    }

    /**
     * @return The triangle's height over base V2V3 to V1.
     */
    public double heightV1() {
        double height = (2.0 / baseV2V3()) * heightCoefficient();
        return height;
    }

    /**
     * @return The triangle's height over base V1V3 to V2.
     */
    public double heightV2() {
        double height = (2.0 / baseV1V3()) * heightCoefficient();
        return height;
    }

    /**
     * @return The triangle's height over base V1V2 to V3.
     */
    public double heightV3() {
        double height = (2.0 / baseV1V2()) * heightCoefficient();
        return height;
    }

    /**
     * @return Coefficient of Heron's formula.
     */
    private double heightCoefficient() {
        double s = perimeter() / 2.0; // semi-perimeter
        double base_a = baseV1V2();
        double base_b = baseV1V3();
        double base_c = baseV2V3();
        double coef = s * (s - base_a) * (s - base_b) * (s - base_c);

        return Math.sqrt(coef);
    }

    /**
     * Get the length of the segment connecting the vertex V1
     * to its opposite edge V2V3.
     *
     * @return The median of this triangle from the vertex V1.
     */
    public double medianV1() {
        double median = Math.pow(baseV1V2(), 2) +
                Math.pow(baseV1V3(), 2);
        median = (2.0 * median) -
                Math.pow(baseV2V3(), 2);
        median = Math.sqrt(median) / 2.0;
        return median;
    }

    /**
     * Get the length of the segment connecting the vertex V2
     * to its opposite edge V1V3.
     *
     * @return The median of this triangle from the vertex V2.
     */
    public double medianV2() {
        double median = Math.pow(baseV1V2(), 2) +
                Math.pow(baseV2V3(), 2);
        median = (2.0 * median) -
                Math.pow(baseV1V3(), 2);
        median = Math.sqrt(median) / 2.0;
        return median;
    }

    /**
     * Get the length of the segment connecting the vertex V3
     * to its opposite edge V1V2.
     *
     * @return The median of this triangle from the vertex V3.
     */
    public double medianV3() {
        double median = Math.pow(baseV1V3(), 2) +
                Math.pow(baseV2V3(), 2);
        median = (2.0 * median) -
                Math.pow(baseV1V2(), 2);
        median = Math.sqrt(median) / 2.0;
        return median;
    }

    /**
     * Get the length of the segment connecting the vertex V1
     * to its opposite edge that divides the vertex V1 into
     * two angles of same size.
     *
     * @return The bisector of this triangle from the vertex V1.
     */
    public double bisectorV1() {
        double s = perimeter() / 2.0; // semiperimeter
        double a = baseV1V2();
        double b = baseV1V3();
        double c = baseV2V3();
        double bisector = 2.0 / (a + b);
        bisector = bisector * Math.sqrt(a * b * s * (s - c));
        return bisector;
    }

    /**
     * Get the length of the segment connecting the vertex V2
     * to its opposite edge that divides the vertex V2 into
     * two angles of same size.
     *
     * @return The bisector of this triangle from the vertex V2.
     */
    public double bisectorV2() {
        double s = perimeter() / 2.0; // semiperimeter
        double a = baseV1V2();
        double b = baseV1V3();
        double c = baseV2V3();
        double bisector = 2.0 / (a + c);
        bisector = bisector * Math.sqrt(a * c * s * (s - b));
        return bisector;
    }

    /**
     * Get the length of the segment connecting the vertex V3
     * to its opposite edge that divides the vertex V3 into
     * two angles of same size.
     *
     * @return The bisector of this triangle from the vertex V3.
     */
    public double bisectorV3() {
        double s = perimeter() / 2.0; // semiperimeter
        double a = baseV1V2();
        double b = baseV1V3();
        double c = baseV2V3();
        double bisector = 2.0 / (b + c);
        bisector = bisector * Math.sqrt(b * c * s * (s - a));
        return bisector;
    }

    /**
     * @return The intern angle of this triangle under
     * the vertex V1.
     */
    public double angleV1() {
        double ux = v2x - v1x;
        double uy = v2y - v1y;
        double vx = v3x - v1x;
        double vy = v3y - v1y;

        return Vector2D.angleBetweenVectors(ux, uy, vx, vy);
    }

    /**
     * @return The intern angle of this triangle under
     * the vertex V2.
     */
    public double angleV2() {
        double ux = v1x - v2x;
        double uy = v1y - v2y;
        double vx = v3x - v2x;
        double vy = v3y - v2y;

        return Vector2D.angleBetweenVectors(ux, uy, vx, vy);
    }

    /**
     * @return The intern angle of this triangle under
     * the vertex V3.
     */
    public double angleV3() {
        double ux = v1x - v3x;
        double uy = v1y - v3y;
        double vx = v2x - v3x;
        double vy = v2y - v3y;

        return Vector2D.angleBetweenVectors(ux, uy, vx, vy);
    }

    /**
     * Check whether this triangle is scalene (has three edges
     * of different lengths).
     *
     * @return True if this triangle is scalene.
     */
    public boolean isScalene() {
        double a = baseV1V2();
        double b = baseV1V3();
        double c = baseV2V3();
        return (a != b && a != c && b != c);
    }

    /**
     * Check whether this triangle is isosceles (has at least
     * two edges of same lengths).
     *
     * @return True if this triangle is isosceles.
     */
    public boolean isIsosceles() {
        double a = baseV1V2();
        double b = baseV1V3();
        double c = baseV2V3();
        return a == b || (a == c || b == c);
    }

    /**
     * Check whether this triangle is equilateral (has the three
     * edges of same length).
     *
     * @return True if this triangle is equilateral.
     */
    public boolean isEquilateral() {
        return (baseV1V2() == baseV1V3() && baseV1V2() == baseV2V3());
    }

    /**
     * Check whether this triangle is rectangle (has a 90* intern angle).
     *
     * @return True if this triangle is rectangle.
     */
    public boolean isRectangle() {
        double a2 = Math.pow(baseV1V2(), 2);
        double b2 = Math.pow(baseV1V3(), 2);
        double c2 = Math.pow(baseV2V3(), 2);
        return a2 == (b2 + c2) || (b2 == (a2 + c2) || c2 == (a2 + b2));
    }

    /**
     * Check whether these two triangles are adjacent,
     * i.e. If they share any edge.
     *
     * @param tri The other triangle to check.
     * @return True if these two triangles are adjacent.
     */
    public boolean isAdjacent(Triangle tri) {
        if (tri == null) return false;
        for (Segment e1 : this.getEdges()) {
            for (Segment e2 : tri.getEdges()) {
                if (e1.equals(e2)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Check whether the point p = (x,y) is a vertex
     * of this triangle.
     *
     * @param x
     * @param y
     * @return True if the p = (x,y) is a vertex
     * of this triangle.
     */
    public boolean hasVertex(double x, double y) {
        return hasVertex(new Point(x, y));
    }

    /**
     * Check whether the point p is a vertex
     * of this triangle.
     *
     * @param p The point to check.
     * @return True if the p is a vertex of this triangle.
     */
    public boolean hasVertex(Point p) {
        if (p == null) return false;
        if (p.equals(this.v1())) return true;
        if (p.equals(this.v2())) return true;
        return p.equals(this.v3());
    }

    /**
     * Check whether the segment s = (x1,y1)(x2,y2)
     * is an edge of this triangle.
     *
     * @param x1
     * @param y1
     * @param x2
     * @param y2
     * @return True if s = (x1,y1)(x2,y2) is an edge of this triangle.
     */
    public boolean hasEdge(double x1, double y1, double x2, double y2) {
        return hasEdge(new Segment(x1, y1, x2, y2));
    }

    /**
     * Check whether the segment s is an edge of this triangle.
     *
     * @param e The segment to check.
     * @return True if e is an edge of this triangle.
     */
    public boolean hasEdge(Segment e) {
        if (e == null) return false;
        for (Segment edge : this.getEdges()) {
            if (e.equals(edge))
                return true;
        }
        return false;
    }

    /**
     * Convert this triangle to a Polygon object.
     *
     * @return The equivalent Polygon object of this Triangle.
     */
    public Polygon toPolygon() {
        return new Polygon(this.getCoordinates());
    }

    /**
     * Convert this point object to a AWT Triangle2D object.
     *
     * @return The Triangle2D representation of this triangle.
     */
    public Triangle2D toTriangle2D() {
        return new Triangle2D(
                v1x, v1y, v2x, v2y, v3x, v3y);
    }

    @Override
    public String toString() {
        String s = "( ";
        s += String.format("%.3f %.3f", v1x, v1y) + ", ";
        s += String.format("%.3f %.3f", v2x, v2y) + ", ";
        s += String.format("%.3f %.3f", v3x, v3y);
        return s + " )";
    }

    @Override
    public void print() {
        System.out.println("TRIANGLE " + toString());
    }

    @Override
    public void display() {
        Graph graph = new SingleGraph("Triangle");
        graph.display(false);
        // create one node per triangle vertex
        graph.addNode("N1").setAttribute("xy", v1x, v1y);
        graph.addNode("N2").setAttribute("xy", v2x, v2y);
        graph.addNode("N3").setAttribute("xy", v3x, v3y);
        graph.addEdge("E12", "N1", "N2");
        graph.addEdge("E23", "N2", "N3");
        graph.addEdge("E31", "N3", "N1");
    }

    @Override
    public Triangle clone() {
        Triangle clone = new Triangle(
                v1x, v1y, v2x, v2y, v3x, v3y);
        super.cloneTo(clone);
        return clone;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        long temp;
        temp = Double.doubleToLongBits(v1x);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(v1y);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(v2x);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(v2y);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(v3x);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(v3y);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public boolean equals2D(SpatialObject obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (obj instanceof Triangle) {
            Triangle t = (Triangle) obj;
            if (v1x == t.v1x && v1y == t.v1y &&
                    v2x == t.v2x && v2y == t.v2y &&
                    v3x == t.v3x && v3y == t.v3y) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Geometry toJTSGeometry() {
        return this.toPolygon().toJTSGeometry();
    }

    /**
     * Auxiliary Triangle2D object. Adaptation of a
     * closed Path2D from the java.awt.geom library.
     */
    public static class Triangle2D extends Path2D.Double {
        /**
         * Creates a AWT Triangle2D from the given vertices.
         *
         * @param v1x Vertex 1x coordinate.
         * @param v1y Vertex 1y coordinate.
         * @param v2x Vertex 2x coordinate.
         * @param v2y Vertex 2y coordinate.
         * @param v3x Vertex 3x coordinate.
         * @param v3y Vertex 3y coordinate.
         */
        public Triangle2D(
                double v1x, double v1y,
                double v2x, double v2y,
                double v3x, double v3y) {
            this.moveTo(v1x, v1y);
            this.lineTo(v2x, v2y);
            this.lineTo(v3x, v3y);
            this.closePath();
        }
    }
}
