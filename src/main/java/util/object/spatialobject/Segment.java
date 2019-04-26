package util.object.spatialobject;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineSegment;
import org.apache.log4j.Logger;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.graphstream.graph.Graph;
import org.graphstream.graph.implementations.SingleGraph;
import util.function.DistanceFunction;

import java.util.ArrayList;
import java.util.List;

/**
 * A 2D line segment object. Line segment from coordinate points (x1,y1) to (x2,y2).
 * <p>
 * Segment objects may contain both spatial and semantic attributes. Spatial attributes of simple objects, however, are immutable, that
 * means once a Segment object is created its spatial attributes cannot be changed.
 *
 * @author uqdalves, Hellisk
 */
public class Segment extends SimpleSpatialObject {
	
	private static final Logger LOG = Logger.getLogger(Segment.class);
	/**
	 * Start-point coordinates
	 */
	private final double x1, y1;
	/**
	 * End-point coordinates
	 */
	private final double x2, y2;
	private final DistanceFunction distFunc;
	/**
	 * Auxiliary LineSegment from JTS old version
	 */
	private LineSegment JTSLineSegment = null;
	
	/**
	 * Creates a new empty line segment.
	 */
	public Segment(DistanceFunction df) {
		this.x1 = 0.0;
		this.y1 = 0.0;
		this.x2 = 0.0;
		this.y2 = 0.0;
		this.distFunc = df;
	}
	
	/**
	 * Creates a new line segment with the given start and end coordinates.
	 *
	 * @param x1 Start-point X coordinate.
	 * @param y1 Start-point Y coordinate.
	 * @param x2 End-point X coordinate.
	 * @param y2 End-point Y coordinate.
	 */
	public Segment(double x1, double y1, double x2, double y2, DistanceFunction df) {
		if (x1 == x2 && y1 == y2)
			LOG.debug("Segment has the same endpoints: " + x1 + "," + y1 + "_" + x2 + "," + y2);
		this.x1 = x1;
		this.y1 = y1;
		this.x2 = x2;
		this.y2 = y2;
		this.distFunc = df;
	}
	
	/**
	 * Check whether these two line segments R and S intersect (if they have at least one point in common).
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
	 * Check whether these two line segments R and S cross (if they have only one point in common).
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
	public static boolean segmentsCross(double rx1, double ry1, double rx2, double ry2, double sx1, double sy1, double sx2, double sy2) {
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
		
		return t > 0.0 && t < 1.0 && u > 0.0 && u < 1.0;
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
	
	@NonNull
	@Override
	public DistanceFunction getDistanceFunction() {
		return this.distFunc;
	}
	
	@Override
	public boolean isClosed() {
		return false;
	}
	
	/**
	 * @return The length of this line segment.
	 */
	public double length() {
		return distFunc.distance(p1(), p2());
	}
	
	/**
	 * Check whether this segment is vertical, i.e. parallel to the Y axis.
	 *
	 * @return True is this segment is vertical.
	 */
	public boolean isVertical() {
		return (Double.doubleToLongBits(x1) == Double.doubleToLongBits(x2));
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
			for(Segment s : obj.getOutGoingRoutingEdges()){
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
	 * Check whether this segment is horizontal, i.e. parallel to the X axis.
	 *
	 * @return True is this segment is vertical.
	 */
	public boolean isHorizontal() {
		return (Double.doubleToLongBits(y1) == Double.doubleToLongBits(y2));
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
	 * Check whether this segment and the segment s = (x1,y1)(x2,y2) cross, i.e. have only one point in common.
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
	 * @param x X coordinate.
	 * @param y Y coordinate.
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
	 * Find and return the point where these two line segments intersect (if any).
	 *
	 * @param s Query segment.
	 * @return The intersection point between this segment and s. Return null if they do not cross.
	 */
	public Point getIntersection(Segment s) {
		if (s == null) return null;
		Coordinate c = this.toJTSLineSegment().
				intersection(s.toJTSLineSegment());
		if (c == null) {
			return null;
		}
		return new Point(c.x, c.y, distFunc);
	}
	
	/**
	 * Get the first point of this segment.
	 *
	 * @return The segment's start-point (x1,y1).
	 */
	public Point p1() {
		return new Point(x1, y1, distFunc);
	}
	
	/**
	 * Get the second point of this segment.
	 *
	 * @return The segment's end-point (x2,y2).
	 */
	public Point p2() {
		return new Point(x2, y2, distFunc);
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
		LOG.info("SEGMENT " + toString());
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
			return (s.x1 == x1 && s.y1 == y1 && s.x2 == x2 && s.y2 == y2);
		}
		return false;
	}
	
	@Override
	public boolean equals2D(SpatialObject obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (obj instanceof Segment) {
			Segment s = (Segment) obj;
			return (s.x1 == x1 && s.y1 == y1 && s.x2 == x2 && s.y2 == y2);
		}
		return false;
	}
	
	@Override
	public Segment clone() {
		Segment clone = new Segment(x1, y1, x2, y2, distFunc);
		super.cloneTo(clone);
		return clone;
	}
	
	@Override
	public Geometry toJTSGeometry() {
		return toJTSLineSegment().toGeometry(new GeometryFactory());
	}
	
	/**
	 * Cast this line segment to an equivalent JavaTopologicalSuit (JTS) LineSegment.
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
