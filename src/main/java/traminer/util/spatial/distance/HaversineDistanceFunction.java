package traminer.util.spatial.distance;

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
     * on a sphere. Points represented by latitude and
     * longitude on the Earth surface.
     */
    public double pointToPointDistance(Point p1, Point p2) {
        return haversineDistance(p1.x(), p1.y(), p2.x(), p2.y(), EARTH_RADIUS);
    }

    /**
     * Calculate the distance (in meters) between two points
     * on a sphere. Points represented by latitude and
     * longitude on the Earth surface.
     */
    public double pointToPointDistance(double lon1, double lat1, double lon2, double lat2) {
        return haversineDistance(lon1, lat1, lon2, lat2, EARTH_RADIUS);
    }

    /**
     * Calculate the distance (in meters) between two
     * points on a sphere. Points represented by polar
     * coordinates on a given sphere of radius R.
     */
    public double getDistance(Point p1, Point p2, double radius) {
        return haversineDistance(p1.x(), p1.y(), p2.x(), p2.y(), radius);
    }


    /**
     * Calculate the distance (in meters) between two
     * points on a sphere. Points represented by polar
     * coordinates on a given sphere of radius R.
     */
    public double getDistance(double lon1, double lat1, double lon2, double lat2, double radius) {
        return haversineDistance(lon1, lat1, lon2, lat2, radius);
    }

    /**
     * Calculate the distance (in meters) between two
     * coordinates on a sphere from their longitudes
     * and latitudes.
     *
     * @param lon1 The longitude of the point 1
     * @param lat1 The latitude of the point 1
     * @param lon2 The longitude of the point 2
     * @param lat2 The latitude of the point 2
     * @return The Haversine Distance between
     * points A and B.
     */
    private double haversineDistance(
            final double lon1, final double lat1,
            final double lon2, final double lat2,
            final double radius) {

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
