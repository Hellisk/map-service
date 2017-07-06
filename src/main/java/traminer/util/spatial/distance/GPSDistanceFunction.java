package traminer.util.spatial.distance;

import traminer.util.exceptions.DistanceFunctionException;
import traminer.util.spatial.objects.Point;
import traminer.util.spatial.objects.Segment;

/**
 * Created by Hellisk on 11/06/2017.
 */
public class GPSDistanceFunction implements PointDistanceFunction, SegmentDistanceFunction, VectorDistanceFunction {
    private static final double EARTH_RADIUS = 6378.137;

    private static double rad(double d) {
        return d * Math.PI / 180.0;
    }

    @Override
    public double distance(Point p1, Point p2) throws DistanceFunctionException {
        double radLat1 = rad(p1.x());
        double radLat2 = rad(p2.x());
        double a = radLat1 - radLat2;
        double b = rad(p1.y()) - rad(p2.y());
        double s = 2 * Math.asin(Math.sqrt(Math.pow(Math.sin(a / 2), 2) +
                Math.cos(radLat1) * Math.cos(radLat2) * Math.pow(Math.sin(b / 2), 2)));
        s = s * EARTH_RADIUS;
        s = s * 10000 / 10000000;
        return s;
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
        double radLat1 = rad(x1);
        double radLat2 = rad(x2);
        double a = radLat1 - radLat2;
        double b = rad(y1) - rad(y2);
        double s = 2 * Math.asin(Math.sqrt(Math.pow(Math.sin(a / 2), 2) +
                Math.cos(radLat1) * Math.cos(radLat2) * Math.pow(Math.sin(b / 2), 2)));
        s = s * EARTH_RADIUS;
        s = s * 10000 / 10000000;
        return s;
    }

    @Override
    public double distance(Segment s, Segment r) throws DistanceFunctionException {
        return 0;
    }

    @Override
    public double pointToSegmentDistance(double x, double y, double sx1, double sy1, double sx2, double sy2) {
        // TODO confirm the correctness of this method
        // find the perpendicular point pp.
        // the segment is represented as y= ax + b, while the perpendicular line is x= -ay + m
        double a = (sy1 - sy2) / (sx1 - sx2);
        double b = (sy1 - a * sy2);
        double m = x + a * y;

        double ppx = (m - a * b) / (a * a + 1);
        double ppy = a * ppx + b;

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
        return pointToPointDistance(x, y, ppx, ppy);
    }

    @Override
    public double pointToSegmentDistance(Point p, Segment s) throws DistanceFunctionException {

        return pointToSegmentDistance(p.x(), p.y(), s.x1(), s.y1(), s.x2(), s.y2());
    }

    // TODO to be implemented
    @Override
    public double segmentToSegmentDistance(double sx1, double sy1, double sx2, double sy2, double rx1, double ry1, double rx2, double ry2) {
        return 0;
    }


    @Override
    public double distance(double[] v, double[] u) throws DistanceFunctionException {
        return 0;
    }
}
