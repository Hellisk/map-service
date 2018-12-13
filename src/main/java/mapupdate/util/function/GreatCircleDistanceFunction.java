package mapupdate.util.function;

import mapupdate.util.exceptions.DistanceFunctionException;
import mapupdate.util.object.spatialobject.Point;
import mapupdate.util.object.spatialobject.Segment;

import java.text.DecimalFormat;

import static java.lang.Math.*;

/**
 * Created by Hellisk on 11/06/2017.
 */
public class GreatCircleDistanceFunction implements PointDistanceFunction, SegmentDistanceFunction, VectorDistanceFunction {
    private static final double EARTH_RADIUS = 6371000;
    DecimalFormat df = new DecimalFormat(".00000");

    private double rad(double d) {
        return d * Math.PI / 180.0;
    }

    @Override
    public double distance(Point p1, Point p2) throws DistanceFunctionException {
        return pointToPointDistance(p1.x(), p1.y(), p2.x(), p2.y());
    }

    /**
     * distance calculator between points. Method adopted from GraphHopper
     *
     * @param x1 longitude of the start point
     * @param y1 latitude of the start point
     * @param x2 longitude of the end point
     * @param y2 latitude of the end point
     * @return distance at metres
     */
    @Override
    public double pointToPointDistance(double x1, double y1, double x2, double y2) {
        double dLat = toRadians(y2 - y1);
        double dLon = toRadians(x2 - x1);
        // use mean latitude as reference point for delta_lon
        double tmp = cos(toRadians((y1 + y2) / 2)) * dLon;
        double normedDist = dLat * dLat + tmp * tmp;
        return EARTH_RADIUS * sqrt(normedDist);
    }

    @Override
    public double distance(Segment s, Segment r) throws DistanceFunctionException {
        return 0;
    }

    /**
     * find the closest point of a given point to the segment, the closest point is either the perpendicular point or one of the endpoint
     *
     * @param p 2D point
     * @param s candidate segment
     * @return point on s that is closest to p
     */
    public Point findClosestPoint(Point p, Segment s) {
        Point returnPoint = findClosestPoint(p.x(), p.y(), s.x1(), s.y1(), s.x2(), s.y2());
        if (returnPoint.x() == s.x1())
            return s.p1();
        else if (returnPoint.x() == s.x2())
            return s.p2();
        else return returnPoint;
    }

    /**
     * Convert the actual distance into the coordinate offset of latitude/longitude
     *
     * @param distance the actual distance in meter
     * @return the coordinate offset
     */
    public double coordinateOffset(double distance) {
        double radian = distance / EARTH_RADIUS;
        return Math.toDegrees(radian);
    }

    /**
     * Return the perpendicular point on segment even it is on the extended line.
     *
     * @param x   Source point x coordinate.
     * @param y   Source point y coordinate.
     * @param sx1 Segment start point x coordinate.
     * @param sy1 Segment start point y coordinate.
     * @param sx2 Segment end point x coordinate.
     * @param sy2 Segment end point y coordinate.
     * @return Perpendicular point.
     */
    public Point findClosestPointExtendable(double x, double y, double sx1, double sy1, double sx2, double sy2) {

        // find the perpendicular point pp.
        // the segment is represented as y= ax + b, while the perpendicular line is x= -ay + m
        double a = sy2 - sy1;
        double b = sx1 - sx2;
        double c = sx2 * sy1 - sx1 * sy2;

        double ppx = (b * b * x - a * b * y - a * c) / (a * a + b * b);
        double ppy = (-a * b * x + a * a * y - b * c) / (a * a + b * b);

        return new Point(ppx, ppy);
    }

    public Point findClosestPoint(double x, double y, double sx1, double sy1, double sx2, double sy2) {

        // find the perpendicular point pp.
        // the segment is represented as y= ax + b, while the perpendicular line is x= -ay + m
        double a = sy2 - sy1;
        double b = sx1 - sx2;
        double c = sx2 * sy1 - sx1 * sy2;

        double ppx = (b * b * x - a * b * y - a * c) / (a * a + b * b);
        double ppy = (-a * b * x + a * a * y - b * c) / (a * a + b * b);

//        double a = (sy1 - sy2) / (sx1 - sx2);
//        double b = (sy1 - a * sy2);
//        double m = x + a * y;
//
//        double ppx = (m - a * b) / (a * a + 1);
//        double ppy = a * ppx + b;

        // check whether the perpendicular point is outside the segment
        if (sx1 < sx2) {
            if (ppx < sx1) {
                ppx = sx1;
                ppy = sy1;
            } else if (ppx > sx2) {
                ppx = sx2;
                ppy = sy2;
            }
        } else {
            if (ppx < sx2) {
                ppx = sx2;
                ppy = sy2;
            } else if (ppx > sx1) {
                ppx = sx1;
                ppy = sy1;
            }
        }
        double pointX = 0;
        double pointY = 0;
        try {
            pointX = Double.parseDouble(df.format(ppx));
            pointY = Double.parseDouble(df.format(ppy));
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        return new Point(pointX, pointY);
    }

    @Override
    public double pointToSegmentDistance(double x, double y, double sx1, double sy1, double sx2, double sy2) {

        Point ppPoint = findClosestPoint(x, y, sx1, sy1, sx2, sy2);

        return pointToPointDistance(x, y, ppPoint.x(), ppPoint.y());
    }

    @Override
    public double pointToSegmentDistance(Point p, Segment s) throws DistanceFunctionException {

        return pointToSegmentDistance(p.x(), p.y(), s.x1(), s.y1(), s.x2(), s.y2());
    }

    @Override
    public double segmentToSegmentDistance(double sx1, double sy1, double sx2, double sy2, double rx1, double ry1, double rx2, double ry2) {
        if (Segment.segmentsCross(sx1, sy1, sx2, sy2, rx1, ry1, rx2, ry2)) {
            return 0.0;
        }
        double distance1 = pointToSegmentDistance(sx1, sy1, rx1, ry1, rx2, ry2);
        double distance2 = pointToSegmentDistance(sx2, sy2, rx1, ry1, rx2, ry2);
        double distance3 = pointToSegmentDistance(rx1, ry1, sx1, sy1, sx2, sy2);
        double distance4 = pointToSegmentDistance(rx2, ry2, sx1, sy1, sx2, sy2);
        double dist = distance1 > distance2 ? distance1 : distance2;
        dist = dist > distance3 ? dist : distance3;
        dist = dist > distance4 ? dist : distance4;
        return dist;
    }


    @Override
    public double distance(double[] v, double[] u) throws DistanceFunctionException {
        return 0;
    }
}