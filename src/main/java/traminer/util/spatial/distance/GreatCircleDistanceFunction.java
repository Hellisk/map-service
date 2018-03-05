package traminer.util.spatial.distance;

import traminer.util.exceptions.DistanceFunctionException;
import traminer.util.spatial.objects.Point;
import traminer.util.spatial.objects.Segment;

import java.text.DecimalFormat;

/**
 * Created by Hellisk on 11/06/2017.
 */
public class GreatCircleDistanceFunction implements PointDistanceFunction, SegmentDistanceFunction, VectorDistanceFunction {
    private static final double EARTH_RADIUS = 6378137;
    DecimalFormat df = new DecimalFormat(".0000");

    private static double rad(double d) {
        return d * Math.PI / 180.0;
    }

    @Override
    public double distance(Point p1, Point p2) throws DistanceFunctionException {
        return pointToPointDistance(p1.x(), p1.y(), p2.x(), p2.y());
    }

    /**
     * distance calculator between points.
     *
     * @param x1
     * @param y1
     * @param x2
     * @param y2
     * @return distance at metres
     */
    @Override
    public double pointToPointDistance(double x1, double y1, double x2, double y2) {
        double radLat1 = rad(y1);
        double radLat2 = rad(y2);
        double a = radLat1 - radLat2;
        double b = rad(x1) - rad(x2);
        double s = 2 * Math.asin(Math.sqrt(Math.pow(Math.sin(a / 2), 2) +
                Math.cos(radLat1) * Math.cos(radLat2) * Math.pow(Math.sin(b / 2), 2)));
        s = s * EARTH_RADIUS;
        return Double.parseDouble(df.format(s));
    }

    @Override
    public double distance(Segment s, Segment r) throws DistanceFunctionException {
        return 0;
    }

    public Point findPerpendicularPoint(Point p, Segment s) {
        return findPerpendicularPoint(p.x(), p.y(), s.x1(), s.y1(), s.x2(), s.y2());
    }

    public Point findPerpendicularPoint(double x, double y, double sx1, double sy1, double sx2, double sy2) {

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
        return new Point(ppx, ppy);
    }

    @Override
    public double pointToSegmentDistance(double x, double y, double sx1, double sy1, double sx2, double sy2) {

        Point ppPoint = findPerpendicularPoint(x, y, sx1, sy1, sx2, sy2);

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
