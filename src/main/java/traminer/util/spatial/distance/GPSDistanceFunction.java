package traminer.util.spatial.distance;

import traminer.util.spatial.objects.Edges;
import traminer.util.spatial.objects.Point;

/**
 * Created by Hellisk on 11/06/2017.
 */
public class GPSDistanceFunction implements PointDistanceFunction, SegmentDistanceFunction {
    private static final double EARTH_RADIUS = 6378.137;

    private static double rad(double d) {
        return d * Math.PI / 180.0;
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
    public double pointToPointDistance(Point p1, Point p2) {
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
    public double pointToSegmentDistance(Point p, Edges s) {
        // find the perpendicular point pp.
        // the segment is represented as y= ax + b, while the perpendicular line is x= -ay + m
        double a = (s.y1() - s.y2()) / (s.x1() - s.x2());
        double b = (s.y1() - a * s.y2());
        double m = p.x() + a * p.y();

        double ppx = (m - a * b) / (a * a + 1);
        double ppy = a * ppx + b;

        // check whether the perpendicular point is outside the segment
        if (s.x1() < s.x2()) {
            if (ppx < s.x1()) {
                ppx = s.x1();
                ppy = s.y1();
            } else if (ppx > s.x2()) {
                ppx = s.x2();
                ppy = s.y2();
            }
        } else {
            if (ppx < s.x2()) {
                ppx = s.x2();
                ppy = s.y2();
            } else if (ppx > s.x1()) {
                ppx = s.x1();
                ppy = s.y1();
            }
        }

        Point pp = new Point(ppx, ppy);

        return pointToPointDistance(p, pp);
    }

    // TODO to be implemented
    @Override
    public double segmentToSegmentDistance(double sx1, double sy1, double sx2, double sy2, double rx1, double ry1, double rx2, double ry2) {
        return 0;
    }

    // TODO to be implemented
    @Override
    public double segmentToSegmentDistance(Edges s, Edges r) {
        return 0;
    }
}
