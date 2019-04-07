package mapupdate.util.function;

import mapupdate.util.exceptions.DistanceFunctionException;
import mapupdate.util.object.spatialobject.Point;
import mapupdate.util.object.spatialobject.Segment;
import mapupdate.util.object.spatialobject.Vector2D;

/**
 * Euclidean Distance function for spatial objects.
 *
 * @author uqdalves
 */
@SuppressWarnings("serial")
public class EuclideanDistanceFunction implements
        PointDistanceFunction, SegmentDistanceFunction, VectorDistanceFunction {

    /**
     * Euclidean distance between two spatial points.
     */
    @Override
    public double distance(Point p1, Point p2) {
        if (p1 == null || p2 == null) {
            throw new NullPointerException("Points for distance "
                    + "calculation cannot be null.");
        }
        return pointToPointDistance(p1.x(), p1.y(), p2.x(), p2.y());
    }

    /**
     * Euclidean distance between two line segments.
     */
    @Override
    public double distance(Segment s, Segment r) {
        if (s == null || r == null) {
            throw new NullPointerException("Segments for distance "
                    + "calculation cannot be null.");
        }
        return segmentToSegmentDistance(
                s.x1(), s.x2(), s.y1(), s.y2(),
                r.x1(), r.x2(), r.y1(), r.y2());
    }

    /**
     * Euclidean distance between two N-dimensional vectors.
     */
    @Override
    public double distance(double[] v, double[] u)
            throws DistanceFunctionException {
        if (v == null || u == null) {
            throw new NullPointerException("Vectors for Euclidean distance "
                    + "calculation cannot be null.");
        }
        if (v.length != u.length) {
            throw new DistanceFunctionException("Vectors should be "
                    + "of same size for Jaccard distance calculation.");
        }
        if (v.length == 0) {
            return 0;
        }

        double distance = 0;
        for (int i = 0; i < v.length; i++) {
            distance += (v[i] - u[i]) * (v[i] - u[i]);
        }

        return Math.sqrt(distance);
    }

    /**
     * Euclidean distance between 2D points.
     * <br> {@inheritDoc}
     */
    @Override
    public double pointToPointDistance(double x1, double y1, double x2, double y2) {
        double dist2 = (x1 - x2) * (x1 - x2) +
                (y1 - y2) * (y1 - y2);
        return Math.sqrt(dist2);
    }

    /**
     * Euclidean distance between 3D points.
     *
     * @param x1
     * @param y1
     * @param z1
     * @param x2
     * @param y2
     * @param z2
     * @return Euclidean distance between the 3D points
     * (x1, y1, z1) and (x2, y2, z2).
     */
    public double pointToPointDistance(
            double x1, double y1, double z1,
            double x2, double y2, double z2) {
        double dist2 = (x1 - x2) * (x1 - x2) +
                (y1 - y2) * (y1 - y2) +
                (z1 - z2) * (z1 - z2);
        return Math.sqrt(dist2);
    }

    /**
     * Euclidean distance between point and segment.
     * <br> {@inheritDoc}
     */
    @Override
    public double pointToSegmentDistance(double x, double y,
                                         double sx1, double sy1, double sx2, double sy2) {
        // triangle height
        double num = (sy2 - sy1) * x - (sx2 - sx1) * y + sx2 * sy1 - sy2 * sx1;
        double den = (sy2 - sy1) * (sy2 - sy1) + (sx2 - sx1) * (sx2 - sx1);
        double dist = Math.abs(num) / Math.sqrt(den);
        return dist;
    }

    /**
     * Euclidean distance between point and segment.
     * <br> {@inheritDoc}
     */
    @Override
    public double pointToSegmentDistance(Point p, Segment s) {
        if (p == null) {
            throw new NullPointerException("Point for distance "
                    + "calculation cannot be null.");
        }
        if (s == null) {
            throw new NullPointerException("Segment for distance "
                    + "calculation cannot be null.");
        }
        return pointToSegmentDistance(p.x(), p.y(),
                s.x1(), s.y1(), s.x2(), s.y2());
    }

    /**
     * Euclidean distance between line segments.
     * <br> {@inheritDoc}
     */
    @Override
    public double segmentToSegmentDistance(
            double sx1, double sy1, double sx2, double sy2,
            double rx1, double ry1, double rx2, double ry2) {
        // if they intersect the shortest distance is zero
        if (Segment.segmentsCross(sx1, sy1, sx2, sy2, rx1, ry1, rx2, ry2)) {
            return 0.0;
        }

        // vectors
        double sx = sx2 - sx1;
        double sy = sy2 - sy1;
        double rx = rx2 - rx1;
        double ry = ry2 - ry1;
        double wx = rx1 - sx1;
        double wy = ry1 - sy1;

        double a = Vector2D.dotProduct(rx, ry, rx, ry); // dot(u,u) always >= 0
        double b = Vector2D.dotProduct(rx, ry, sx, sy); // dot(u,v)
        double c = Vector2D.dotProduct(sx, sy, sx, sy); // dot(v,v) always >= 0
        double d = Vector2D.dotProduct(rx, ry, wx, wy); // dot(u,w);
        double e = Vector2D.dotProduct(sx, sy, wx, wy); // dot(v,w);
        double D = a * c - b * b;    // always >= 0
        double sc, sN, sD = D;   // sc = sN / sD, default sD = D >= 0
        double tc, tN, tD = D;   // tc = tN / tD, default tD = D >= 0

        // compute the line parameters of the two closest points
        if (D < MIN_DIST) {  // the lines are almost parallel
            sN = 0.0;        // force using point P0 on segment S1
            sD = 1.0;        // to prevent possible division by 0.0 later
            tN = e;
            tD = c;
        }
        // get the closest points on the infinite lines
        else {
            sN = (b * e - c * d);
            tN = (a * e - b * d);
            // sc < 0 => the s=0 edge is visible
            if (sN < 0.0) {
                sN = 0.0;
                tN = e;
                tD = c;
            }
            // sc > 1  => the s=1 edge is visible
            else if (sN > sD) {
                sN = sD;
                tN = e + b;
                tD = c;
            }
        }

        // tc < 0 => the t=0 edge is visible
        if (tN < 0.0) {
            tN = 0.0;
            // recompute sc for this edge
            if (-d < 0.0)
                sN = 0.0;
            else if (-d > a)
                sN = sD;
            else {
                sN = -d;
                sD = a;
            }
        }
        // tc > 1  => the t=1 edge is visible
        else if (tN > tD) {
            tN = tD;
            // recompute sc for this edge
            if ((-d + b) < 0.0)
                sN = 0;
            else if ((-d + b) > a)
                sN = sD;
            else {
                sN = (-d + b);
                sD = a;
            }
        }

        // finally do the division to get sc and tc
        sc = (Math.abs(sN) < MIN_DIST ? 0.0 : sN / sD);
        tc = (Math.abs(tN) < MIN_DIST ? 0.0 : tN / tD);

        // get the difference of the two closest points
        double dx = wx + sc * rx - tc * sx;
        double dy = wy + sc * ry - tc * sy;
        // vector norm
        double dist = Math.sqrt(dx * dx + dy * dy);

        return dist;
    }
}
