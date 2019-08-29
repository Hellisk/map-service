package util.object.structure;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.impl.PackedCoordinateSequence;
import org.apache.log4j.Logger;
import org.checkerframework.checker.nullness.qual.NonNull;
import util.exceptions.DistanceFunctionException;
import util.function.DistanceFunction;
import util.object.spatialobject.Point;
import util.object.spatialobject.Segment;
import util.object.spatialobject.SimpleSpatialObject;
import util.object.spatialobject.SpatialObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Segment element for index purpose. The index is built based on the position of its endpoints or virtual middle points (if the segment
 * is too long)
 *
 * @author Hellisk
 */
public class SegmentWithIndex extends SimpleSpatialObject {
	
	private static final Logger LOG = Logger.getLogger(SegmentWithIndex.class);
	private final DistanceFunction distFunc;
	private Segment segment;
	private int selectPosition;    // -1: from node, 0: to node, >0: the i-th intermediate node
	private String roadID;          // the id of the corresponding road
	private double intervalLength;        // the threshold for extra indexing point, segments that exceed such threshold will generate extra indexing point(s)
	
	// auxiliary point from JTS old version
	private com.vividsolutions.jts.geom.Point JTSPoint = null;
	
	public SegmentWithIndex(Segment segment, int selectPosition, String roadID, double intervalLength, DistanceFunction distFunc) {
		this.segment = segment;
		this.selectPosition = selectPosition;
		this.roadID = roadID;
		this.intervalLength = intervalLength;
		this.distFunc = distFunc;
	}
	
	/**
	 * Return the x axis of the index point.
	 *
	 * @return The x axis.
	 */
	public double x() {
		switch (selectPosition) {
			case -1:
				return segment.x1();
			case 0:
				return segment.x2();
			default:
				if (distFunc.distance(segment.p1(), segment.p2()) > selectPosition * intervalLength) {
					double diffX = segment.x2() - segment.x1();
					double factor = selectPosition * intervalLength / distFunc.distance(segment.p1(), segment.p2());
					return segment.x1() + factor * diffX;
				}
				throw new DistanceFunctionException("ERROR! The segment is not long enough to have " + selectPosition + " intermediate point(s)");
		}
	}
	
	/**
	 * Return the y axis of the index point.
	 *
	 * @return The y axis.
	 */
	public double y() {
		switch (selectPosition) {
			case -1:
				return segment.y1();
			case 0:
				return segment.y2();
			default:    // other serial number
				if (distFunc.distance(segment.p1(), segment.p2()) > selectPosition * intervalLength) {
					double diffY = segment.y2() - segment.y1();
					double factor = selectPosition * intervalLength / distFunc.distance(segment.p1(), segment.p2());
					return segment.y1() + factor * diffY;
				}
				throw new DistanceFunctionException("ERROR! The segment is not long enough to have " + selectPosition + " intermediate point(s)");
		}
	}
	
	public Segment getSegment() {
		return segment;
	}
	
	public void setSegment(Segment segment) {
		this.segment = segment;
	}
	
	public String getRoadID() {
		return roadID;
	}
	
	public void setRoadID(String roadID) {
		this.roadID = roadID;
	}
	
	public Point getPoint() {
		return new Point(x(), y(), distFunc);
	}
	
	/**
	 * Get the (x,y,z) coordinates of this Point as an array of doubles.
	 */
	public double[] getCoordinate() {
		return new double[]{x(), y()};
	}
	
	@Override
	public List<Point> getCoordinates() {
		ArrayList<Point> list = new ArrayList<>();
		list.add(getPoint());
		return list;
	}
	
	@Override
	public List<Segment> getEdges() {
		List<Segment> segmentList = new ArrayList<>();
		segmentList.add(segment);
		return segmentList;
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

//    //@Override
//    public boolean touches(SpatialObject obj) {
//        if (obj == null) {
//            return false;
//        }
//        if (obj instanceof Point) {
//            return false;
//        }
//        if (obj instanceof Segment) {
//            return ((Segment) obj).touches(getPoint());
//        }
//        if (obj instanceof Circle) {
//            return ((Circle) obj).touches(this);
//        }
//        for (Segment s : obj.getOutGoingRoutingEdges()) {
//            if (s.touches(getPoint())) {
//                return true;
//            }
//        }
//        return false;
//    }
	
	@Override
	public SegmentWithIndex clone() {
		SegmentWithIndex clone = new SegmentWithIndex(this.segment, this.selectPosition, this.roadID, this.intervalLength, this.distFunc);
		super.cloneTo(clone); // clone semantics
		return clone;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(x());
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(y());
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}
	
	@Override
	public boolean equals2D(SpatialObject obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (obj instanceof SegmentWithIndex) {
			SegmentWithIndex p = (SegmentWithIndex) obj;
			return p.segment.equals2D(this.segment) && p.selectPosition == this.selectPosition && p.roadID.equals(this.roadID);
		}
		return true;
	}
	
	@Override
	public String toString() {
		return String.format("%.5f %.5f", x(), y());
	}
	
	@Override
	public void print() {
		LOG.info("Segment (" + toString() + ")");
	}
	
	@Override
	public Geometry toJTSGeometry() {
		if (JTSPoint == null) {
			PackedCoordinateSequence.Double coord =
					new PackedCoordinateSequence.Double(this.getCoordinate(), 2);
			JTSPoint = new com.vividsolutions.jts.geom.
					Point(coord, new GeometryFactory());
		}
		return JTSPoint;
	}
	
	/**
	 * Compare two points by using the given comparator.
	 */
	public int compareTo(SegmentWithIndex p, Comparator<SegmentWithIndex> comparator) {
		return comparator.compare(this, p);
	}
}
