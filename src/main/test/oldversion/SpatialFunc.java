package edu.uq.dke.mapupdate.oldversion;

/**
 * Created by Hellisk on 11/04/2017.
 */
public class SpatialFunc {
    // Global constants
    public static double METERS_PER_DEGREE_LATITUDE = 111070.34306591158;
    public static double METERS_PER_DEGREE_LONGITUDE = 83044.98918812413;
    public static double EARTH_RADIUS = 6371000.0; // meters

    public double distance(double aLat, double aLon, double bLat, double bLon) {
        return fastDistance(aLat, aLon, bLat, bLon);
    }

    private double fastDistance(double aLat, double aLon, double bLat, double bLon) {
        if (isSameCoords(aLat, aLon, bLat, bLon)) {
            return 0.0;
        }
        double yDist = METERS_PER_DEGREE_LATITUDE * (aLat - bLat);
        double xDist = METERS_PER_DEGREE_LONGITUDE * (aLon - bLon);

        return Math.sqrt((yDist * yDist) + (xDist * xDist));
    }

    private boolean isSameCoords(double aLat, double aLon, double bLat, double bLon) {
        return aLat == bLat && aLon == bLon;
    }
}
