package traminer.util.spatial.distance;

import traminer.util.spatial.SpatialUtils;
import traminer.util.spatial.objects.Point;

/**
 * Calculate the Haversine distance between two
 * points on a sphere (e.g. latitude and longitude)
 * 
 * @author uqdalves
 */
@SuppressWarnings("serial")
public class HaversineDistanceFunction implements PointDistanceFunction {

    /**
     * Calculate the distance (in meters) between two points
     * on a sphere. Points represented by latitude and longitude
     * on the Earth surface.
     */
    @Override
    public double distance(Point p1, Point p2) {
        if (p1 == null || p2 == null) {
            throw new NullPointerException("Points for distance "
                    + "calculation cannot be null.");
        }
        return haversineDistance(p1.x(), p1.y(), p2.x(), p2.y(), SpatialUtils.EARTH_RADIUS);
    }

    /**
     * Calculate the distance (in meters) between two points
     * on a sphere. Points represented by latitude and longitude
     * on the Earth surface.
     */
    @Override
    public double pointToPointDistance(double lon1, double lat1, double lon2, double lat2) {
        return haversineDistance(lon1, lat1, lon2, lat2, SpatialUtils.EARTH_RADIUS);
    }

    /**
     * Calculate the distance (in meters) between two
     * points on a sphere. Points represented by polar
     * coordinates on a given sphere of radius R.
     *
     * @param p1     Spatial point P1.
     * @param p2     Spatial point P2.
     * @param radius Sphere of radius.
     * @return Distance between P1 and P2 on a sphere surface.
     */
    public double pointToPointDistance(Point p1, Point p2, double radius) {
        if (p1 == null || p2 == null) {
            throw new NullPointerException("Points for distance "
                    + "calculation cannot be null.");
        }
        return haversineDistance(p1.x(), p1.y(), p2.x(), p2.y(), radius);
    }

    /**
     * Calculate the distance (in meters) between two points
     * on a sphere. Points represented by polar coordinates
     * on a given sphere of radius R.
     *
     * @param lon1   Longitude of the first point.
     * @param lat1   Latitude of the first point.
     * @param lon2   Longitude of the second point.
     * @param lat2   Latitude of the second point.
     * @param radius Sphere of radius.
     * @return Distance between (lon1, lat1) and (lon2, lat2)
     * on a sphere surface.
     */
    public double pointToPointDistance(double lon1, double lat1,
                                       double lon2, double lat2, double radius) {
        return haversineDistance(lon1, lat1, lon2, lat2, radius);
    }

    /**
     * Calculate the distance (in meters) between two coordinates
     * on a sphere from their longitudes and latitudes.
     *
     * @param lon1   Longitude of the first point.
     * @param lat1   Latitude of the first point.
     * @param lon2   Longitude of the second point.
     * @param lat2   Latitude of the second point.
     * @param radius Sphere of radius.
     * @return The Haversine Distance between (lon1, lat1) and (lon2, lat2).
     */
    private double haversineDistance(
            double lon1, double lat1,
            double lon2, double lat2, double radius) {
        if (radius < 0) {
            throw new IllegalArgumentException("Sphere radius for distance "
                    + "calculation must be positive.");
        }

        // convert to radians
        double radLat1 = Math.toRadians(lat1);
        double radLat2 = Math.toRadians(lat2);
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(radLat1) * Math.cos(radLat2) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double distance = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        distance = radius * distance;

        return distance;
    }
}
