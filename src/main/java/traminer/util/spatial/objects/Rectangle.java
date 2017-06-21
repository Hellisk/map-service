package traminer.util.spatial.objects;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.impl.PackedCoordinateSequence;
import org.graphstream.graph.Graph;
import org.graphstream.graph.implementations.SingleGraph;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * A 2D rectangle defined by its bottom-left and
 * upper-right coordinates, and whose edges are
 * parallel to the X and Y axis.
 * <p>
 * Rectangle objects may contain both spatial and semantic
 * attributes. Spatial attributes of simple objects, however,
 * are immutable, that means once a Rectangle object is created
 * its spatial attributes cannot be changed.
 *
 * @author uqdalves
 */
@SuppressWarnings("serial")
public class Rectangle extends SimpleSpatialObject {
    // X and Y axis position
    /**
     * lower-left corner x
     **/
    private final double minX;
    /**
     * lower-left corner y
     **/
    private final double minY;
    /**
     * upper-right corner x
     **/
    private final double maxX;
    /**
     * upper-right corner y
     **/
    private final double maxY;

    // auxiliary Polygon from JTS lib
    private com.vividsolutions.jts.geom.
            Polygon JTSPolygon = null;

    /**
     * Creates an empty rectangle.
     */
    public Rectangle() {
        this.minX = 0.0;
        this.minY = 0.0;
        this.maxX = 0.0;
        this.maxY = 0.0;
    }

    public Rectangle(double minX, double minY,
                     double maxX, double maxY) {
        this.minX = minX;
        this.minY = minY;
        this.maxX = maxX;
        this.maxY = maxY;
    }

    public double minX() {
        return minX;
    }

    public double minY() {
        return minY;
    }

    public double maxX() {
        return maxX;
    }

    public double maxY() {
        return maxY;
    }

    @Override
    public List<Point> getCoordinates() {
        List<Point> corners = new ArrayList<Point>(4);
        Point p1 = new Point(minX, minY);
        Point p2 = new Point(minX, maxY);
        Point p3 = new Point(maxX, maxY);
        Point p4 = new Point(maxX, minY);
        corners.add(p1);
        corners.add(p2);
        corners.add(p3);
        corners.add(p4);

        return corners;
    }

    @Override
    public List<Edges> getEdges() {
        List<Edges> edges = new ArrayList<Edges>(4);
        edges.add(leftEdge());
        edges.add(upperEdge());
        edges.add(rightEdge());
        edges.add(lowerEdge());

        return edges;
    }

    @Override
    public boolean isClosed() {
        return true;
    }

    public double perimeter() {
        return (2 * Math.abs(maxY - minY) + 2 * Math.abs(maxX - minX));
    }

    public double area() {
        return (maxY - minY) * (maxX - minX);
    }

    public double height() {
        return Math.abs(maxY - minY);
    }

    public double width() {
        return Math.abs(maxX - minX);
    }

    /**
     * The center of this rectangle as a coordinate point.
     */
    public Point center() {
        double xCenter = minX + (maxX - minX) / 2;
        double yCenter = minY + (maxY - minY) / 2;
        return new Point(xCenter, yCenter);
    }

    public Edges leftEdge() {
        return new Edges(minX, minY, minX, maxY);
    }

    public Edges rightEdge() {
        return new Edges(maxX, minY, maxX, maxY);
    }

    public Edges lowerEdge() {
        return new Edges(minX, minY, maxX, minY);
    }

    public Edges upperEdge() {
        return new Edges(minX, maxY, maxX, maxY);
    }

    public boolean isSquare() {
        return (Double.doubleToLongBits(Math.abs(maxX - minX)) ==
                Double.doubleToLongBits(Math.abs(maxY - minY)));
    }

    /**
     * True if these two rectangles are adjacent.
     * i.e. If they share any edge.
     */
    public boolean isAdjacent(Rectangle r) {
        for (Edges e1 : this.getEdges()) {
            for (Edges e2 : r.getEdges()) {
                if (e1.equals2D(e2)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * True is this rectangle contains the given point
     * p = (x,y) inside its perimeter. Check if the point
     * lies inside the rectangle area.
     */
    public boolean contains(double x, double y) {
        return x >= minX && x <= maxX &&
                y >= minY && y <= maxY;
    }

    /**
     * Check is this rectangle contains the line segment (x1,y1)--(x2,y2),
     * that is, the line segment is inside the rectangle area.
     */
/*	private boolean contains(double x1, double y1, double x2, double y2){
        if(contains(x1, y1) && contains(x2, y2)){
			return true;
		}
		return false;
	}
	
	/**
	 * True is this rectangle touches the point p = (x,y).
	 * Check if the point touches any edge of the rectangle.
	 */
/*	private boolean touches(double x, double y){
        // check top and bottom edges
		if( x >= min_x && x <= max_x && 
		   (y == max_y || y == min_y) ){
			return true;
		}
		// check left and right edges
		if( y >= min_y && y <= max_y && 
		   (x == min_x || x == max_x) ){
			return true;
		}
		return false;
	}

	// TODO implement
	/**
	 * True is this rectangle touches the line segment (x1,y1)--(x2,y2).
	 * Check if the line segment touches any edge of the rectangle.
	 */
/*	public boolean touches(double x1, double y1, double x2, double y2){
		for(Edges edge : getEdges()){
			if(edge.){
				
			}
		}
		
		if(touches(x1,y1) || touches(x2,y2)){
			
		}
		return false;
		
		// check top and bottom edges
		if( x >= min_x && x <= max_x && 
		   (y == max_y || y == min_y) ){
			return true;
		}
		// check left and right edges
		if( y >= min_y && y <= max_y && 
		   (x == min_x || x == max_x) ){
			return true;
		}
		return false;
	}
*/

    /**
     * Check is these two rectangles overlap.
     */
    public boolean overlaps(Rectangle r) {
        if (this.maxX < r.minX) return false;
        if (this.minX > r.maxX) return false;
        if (this.maxY < r.minY) return false;
        return !(this.minY > r.maxY);
    }

/**
 * True is this rectangle overlaps with the given line segment.
 * <br> Line segment given by end points X and Y coordinates.
 */
/*private boolean overlaps(double x1, double y1, double x2, double y2){
	if(contains(x1, y1) && contains(x2, y2)){
		return true;
	}
	return false;
}
*/
    /**
     * True if the given line segment (x1,y1)--(x2,y2)
     * intersects this Rectangle.
     */
/*	private boolean intersects(
			double x1, double y1, double x2, double y2){
		if(contains(x1, y1, x2, y2)){
			return true;
		}
		for(Edges edge : getEdges()){
			if(edge.intersects(x1, y1, x2, y2)){
				return true;
			}
		}
		return false;
	}
*/

    /**
     * Return this rectangle as a Polygon object.
     */
    public Polygon toPolygon() {
        Polygon poly = new Polygon();
        poly.addAll(this.getCoordinates());

        return poly;
    }

    /**
     * Get the Rectangle2D (AWT) representation of this rectangle.
     */
    public Rectangle2D toRectangle2D() {
        return new Rectangle2D.Double(minX, maxY, width(), height());
    }

    @Override
    public String toString() {
        String s = "( ";
        s += minX + " " + minY + ", ";
        s += minX + " " + maxY + ", ";
        s += maxX + " " + maxY + ", ";
        s += maxX + " " + minY + " )";
        return s;
    }

    @Override
    public void print() {
        println("RECTANGLE " + toString());
    }

    @Override
    public void display() {
        Graph graph = new SingleGraph("Rectangle");
        graph.display(false);
        // create one node per polygon vertex
        List<Point> coords = this.getCoordinates();
        Point p = coords.get(0);
        graph.addNode("N0").setAttribute("xyz", p.x(), p.y(), 0);
        for (int i = 1; i < 4; i++) {
            p = coords.get(i);
            graph.addNode("N" + i).setAttribute("xyz", p.x(), p.y(), 0);
            graph.addEdge("E" + (i - 1) + "-" + i, "N" + (i - 1), "N" + i);
        }
        // close the rectangle
        graph.addEdge("E" + 4 + "-" + 0, "N" + (3), "N0");
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        long temp;
        temp = Double.doubleToLongBits(maxX);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(maxY);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(minX);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(minY);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public boolean equals2D(SpatialObject obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (obj instanceof Rectangle) {
            Rectangle r = (Rectangle) obj;
            return (r.minX == minX && r.minY == minY &&
                    r.maxX == maxX && r.maxY == r.maxY);
        }
        return false;
    }

    @Override
    public Rectangle clone() {
        Rectangle clone = new Rectangle(minX, minY, maxX, maxY);
        super.cloneTo(clone);
        return clone;
    }

    @Override
    public Geometry toJTSGeometry() {
        if (JTSPolygon == null) {
            Coordinate[] coords = new Coordinate[5];
            int i = 0;
            for (Point p : getCoordinates()) {
                coords[i++] = new Coordinate(p.x(), p.y());
            }
            coords[4] = coords[0];
            LinearRing shell = new LinearRing(new PackedCoordinateSequence.
                    Double(coords), new GeometryFactory());

            JTSPolygon = new com.vividsolutions.jts.geom.
                    Polygon(shell, null, new GeometryFactory());
        }
        return JTSPolygon;
    }
}
