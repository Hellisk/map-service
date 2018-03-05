package edu.uq.dke.mapupdate.util;

import edu.uq.dke.mapupdate.datatype.PointProjection;
import traminer.util.spatial.objects.Point;
import traminer.util.spatial.objects.Segment;

public class SpatialFunc {
    public static final double METERS_PER_DEGREE_LATITUDE = 111070.34306591158;
    public static final double METERS_PER_DEGREE_LONGITUDE = 83044.98918812413;
    private final double EARTH_RADIUS = 6371000.0;

    private boolean sameCoordinate(double lon1, double lat1, double lon2, double lat2) {
        return lon1 == lon2 && lat1 == lat2;
    }

    public double distance(Point p1, Point p2) {
        return distance(p1.x(), p1.y(), p2.x(), p2.y());
    }

    // returns the distance in meters between two points specified in degrees, using the Haversine formula. Formula adapted from: http://www.movable-type.co.uk/scripts/latlong.html
    public double distance(double p1x, double p1y, double p2x, double p2y) {
        if (sameCoordinate(p1x, p1y, p2x, p2y))
            return 0.0;

        double dLon = Math.toRadians(p2x - p1x);
        double dLat = Math.toRadians(p2y - p1y);

        double a = Math.sin(dLat / 2.0) * Math.sin(dLat / 2.0) + Math.cos(Math.toRadians(p1y)) * Math.cos(Math.toRadians(p2y)) * Math.sin(dLon / 2.0) * Math.sin(dLon / 2.0);

        double c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS * c;
    }

    // Returns the distance in meters between two points specified in degrees, using the Spherical Law of Cosines. Formula adapted from: http://www.movable-type.co.uk/scripts/latlong.html
    public double slcDistance(double p1x, double p1y, double p2x, double p2y) {
        if (sameCoordinate(p1x, p1y, p2x, p2y)) {
            return 0.0;
        }

        p1x = Math.toRadians(p1x);
        p1y = Math.toRadians(p1y);
        p2x = Math.toRadians(p2x);
        p2y = Math.toRadians(p2y);

        double a = Math.sin(p1y) * Math.sin(p2y) + Math.cos(p1y) * Math.cos(p2y) * Math.cos(p2x - p1x);

        return Math.acos(a) * EARTH_RADIUS;
    }

    // Returns the path bearing between two points specified in degrees. Formula adapted from: http://www.movable-type.co.uk/scripts/latlong.html
    public double pathBearing(double p1x, double p1y, double p2x, double p2y) {

        p1x = Math.toRadians(p1x);
        p1y = Math.toRadians(p1y);
        p2x = Math.toRadians(p2x);
        p2y = Math.toRadians(p2y);

        double y = Math.sin(p2x - p1x) * Math.cos(p2y);
        double x = Math.cos(p1y) * Math.sin(p2y) - Math.sin(p1y) * Math.cos(p2y) * Math.cos(p2x - p1x);
        double bearing = Math.atan2(y, x);

        return Math.floorMod((int) (Math.toDegrees(bearing) + 360), 360);
    }

    // returns the destination point given distance and bearing from a start point. Formula adapted from: http://www.movable-type.co.uk/scripts/latlong.html
    public Point destinationPoint(double p1x, double p1y, double bearing, double distance) {
        double angularDistance = (distance / EARTH_RADIUS);
        bearing = Math.toRadians(bearing);

        p1x = Math.toRadians(p1x);
        p1y = Math.toRadians(p1y);

        double p2y = Math.asin(Math.sin(p1y) * Math.cos(angularDistance) + Math.cos(p1y) * Math.sin(angularDistance) * Math.cos(bearing));
        double p2x = p1x + Math.atan2(Math.sin(bearing) * Math.sin(angularDistance) * Math.cos(p1y), Math.cos(angularDistance) - Math.sin(p1y) * Math.sin(p2y));

        p2x = Math.floorMod((int) ((p2x + (3 * Math.PI))), (int) (2 * Math.PI)) - Math.PI;

        return new Point(Math.toDegrees(p2x), Math.toDegrees(p2y));
    }

    // returns the coordinates of a point some "fraction_along" the line AB.
    public Point pointAlongLine(double p1x, double p1y, double p2x, double p2y, double fractionAlong) {
        double p3x = p1x + (fractionAlong * (p2x - p1x));
        double p3y = p1y + (fractionAlong * (p2y - p1y));
        return new Point(p3x, p3y);
    }

    public double distanceToSegment(Segment s, Point p) {
        Point projection = ProjectOntoSegment(s, p);
        return distance(projection, p);
    }


    public double dotProduct(double p1x, double p1y, double p2x, double p2y) {
        return p1x * p2x + p1y * p2y;
    }

    public double vectorLen(double x, double y) {
        return Math.sqrt(x * x + y * y);
    }


    private Point ProjectOntoSegment(Segment s, Point p) {
        // version from Tomas Gerlich's match_lib

        Point projection;
        if (dotProduct(s.x2() - s.x1(), s.y2() - s.y1(), p.x() - s.x1(), p.y() - s.y1()) <= 0) {
            projection = new Point(s.x1(), s.y1());
        } else if (dotProduct(s.x1() - s.x2(), s.y1() - s.y2(), p.x() - s.x2(), p.y() - s.y2()) <= 0) {
            projection = new Point(s.x2(), s.y2());
        } else {
            double temp1 = dotProduct(s.x2() - s.x1(), s.y2() - s.y1(), p.x() - s.x1(), p.y() - s.y1());
            double b2Len = vectorLen(s.x2() - s.x1(), s.y2() - s.y1());
            double b1Len = temp1 / b2Len;
            double fraction = b1Len / b2Len;
            double x = s.x1() + fraction * (s.x2() - s.x1());
            double y = s.y1() + fraction * (s.y2() - s.y1());
            projection = new Point(x, y);
        }
        return projection;
    }

    public PointProjection projectOntoLine(double p1x, double p1y, double p2x, double p2y, double px, double py) {
        double angleAB = pathBearing(p1x, p1y, p2x, p2y);
        double angleAC = pathBearing(p1x, p1y, px, py);
        double lengthAB = distance(p1x, p1y, p2x, p2y);
        double lengthAC = distance(p1x, p1y, px, py);
        double angleDiff = angleAC - angleAB;
        double metersAlong = (lengthAC * Math.cos(Math.toRadians(angleDiff)));

        double fractionAlong;
        if (lengthAB == 0.0) {
            fractionAlong = 0.0;
        } else
            fractionAlong = (metersAlong / lengthAB);
        Point projectedPoint = pointAlongLine(p1x, p1y, p2x, p2y, fractionAlong);
        double cprojLength = distance(px, py, projectedPoint.x(), projectedPoint.y());
        double cprojAngle = pathBearing(px, py, projectedPoint.x(), projectedPoint.y());
        return new PointProjection(projectedPoint, fractionAlong, cprojLength);
    }

}
