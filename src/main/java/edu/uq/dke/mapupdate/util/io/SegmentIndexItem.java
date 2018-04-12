package edu.uq.dke.mapupdate.util.io;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.impl.PackedCoordinateSequence;
import traminer.util.exceptions.DistanceFunctionException;
import traminer.util.spatial.distance.GreatCircleDistanceFunction;
import traminer.util.spatial.distance.SegmentDistanceFunction;
import traminer.util.spatial.objects.Point;
import traminer.util.spatial.objects.Segment;
import traminer.util.spatial.objects.SimpleSpatialObject;
import traminer.util.spatial.objects.SpatialObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Segment element for index purpose. The index is built based on
 * the position of its endpoints or virtual middle points (if the
 * segment is too long)
 *
 * @author uqpchao
 */
public class SegmentIndexItem extends SimpleSpatialObject {

    private Segment segmentElement;
    private int selectPosition;    // -1: from node, 0: to node, >0: the i-th intermediate node
    private String roadID;          // the id of the corresponding road
    private GreatCircleDistanceFunction distFunc = new GreatCircleDistanceFunction();
    private double intervalLength;

    // auxiliary point from JTS oldversion
    private com.vividsolutions.jts.geom.
            Point JTSPoint = null;

    public SegmentIndexItem(Segment segment, int selectPosition, String roadID, double intervalLength) {
        this.segmentElement = segment;
        this.selectPosition = selectPosition;
        this.roadID = roadID;
        this.intervalLength = intervalLength;
    }

    public Point toPoint() {
        return new Point(x(), y());
    }

    public double x() {
        switch (selectPosition) {
            case -1:
                return segmentElement.x1();
            case 0:
                return segmentElement.x2();
            default:
                if (distFunc.distance(segmentElement.p1(), segmentElement.p2()) > selectPosition * intervalLength) {
                    double diffX = segmentElement.x2() - segmentElement.x1();
                    double factor = selectPosition * intervalLength / distFunc.distance(segmentElement.p1(), segmentElement.p2());
                    return segmentElement.x1() + factor * diffX;
                }
                throw new DistanceFunctionException("ERROR! The segment is not long enough to have " + selectPosition + " intermediate point(s)");
        }
    }

    public double y() {
        switch (selectPosition) {
            case -1:
                return segmentElement.y1();
            case 0:
                return segmentElement.y2();
            default:
                if (distFunc.distance(segmentElement.p1(), segmentElement.p2()) > selectPosition * intervalLength) {
                    double diffY = segmentElement.y2() - segmentElement.y1();
                    double factor = selectPosition * intervalLength / distFunc.distance(segmentElement.p1(), segmentElement.p2());
                    return segmentElement.y1() + factor * diffY;
                }
                throw new DistanceFunctionException("ERROR! The segment is not long enough to have " + selectPosition + " intermediate point(s)");
        }
    }

    public Segment getSegmentElement() {
        return segmentElement;
    }

    public void setSegmentElement(Segment segmentElement) {
        this.segmentElement = segmentElement;
    }

    public String getRoadID() {
        return roadID;
    }

    public void setRoadID(String roadID) {
        this.roadID = roadID;
    }

    public SegmentDistanceFunction getDistFunc() {
        return distFunc;
    }

    public Point getPoint() {
        return new Point(x(), y());
    }
//
//    /**
//     * Returns the distance between this point
//     * and a given point p.
//     *
//     * @param dist The point distance measure to use.
//     */
//    public double distance(SegmentIndexItem p, PointDistanceFunction dist) {
//        return dist.distance(p.getPoint(), getPoint());
//    }

    /**
     * Returns the distance between this point
     * and a given point p = (x,y).
     *
     * @param dist The point distance measure to use.
     */
    public double distance(Point p, SegmentDistanceFunction dist) {
        return dist.pointToSegmentDistance(p, segmentElement);
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
        segmentList.add(segmentElement);
        return segmentList;
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
//        for (Segment s : obj.getEdges()) {
//            if (s.touches(getPoint())) {
//                return true;
//            }
//        }
//        return false;
//    }

    @Override
    public SegmentIndexItem clone() {
        SegmentIndexItem clone = new SegmentIndexItem(this.segmentElement, this.selectPosition, this.roadID, this.intervalLength);
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
        if (obj instanceof SegmentIndexItem) {
            SegmentIndexItem p = (SegmentIndexItem) obj;
            return p.segmentElement == this.segmentElement && p.selectPosition == this.selectPosition && p.roadID.equals(this.roadID);
        }
        return true;
    }

    @Override
    public String toString() {
        return String.format("%.5f %.5f", x(), y());
    }

    @Override
    public void print() {
        System.out.println("POINT (" + toString() + ")");
    }

    @Override
    public void display() {
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
    public int compareTo(SegmentIndexItem p, Comparator<SegmentIndexItem> comparator) {
        return comparator.compare(this, p);
    }
}
