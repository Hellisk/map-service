package traminer.util.spatial;

import java.io.Serializable;

/**
 * Service to convert latitude and longitude coordinates
 * to Cartesian/Mercator coordinates, and vice-versa.
 *
 * @author uqdalves
 */
@SuppressWarnings("serial")
public class CoordinatesConverter implements Serializable {
    /**
     * Referential longitude - map origin
     */
    private final double lon0;
    /**
     * Referential latitude - map origin
     */
    private final double lat0;
    /**
     * Map area scale
     */
    private final double scaleX;
    private final double scaleY;

    /**
     * Creates a new coordinates converter with default
     * map origin at the Equator (0,0) and 1000 x 1000
     * scale.
     */
    public CoordinatesConverter() {
        this.lat0 = 0.0;
        this.lon0 = 0.0;
        this.scaleX = 1000;
        this.scaleY = 1000;
    }

    /**
     * Creates a new coordinates converter with the given
     * map referential (origin) and scale.
     *
     * @param lon0   Referential longitude - origin
     * @param lat0   Referential latitude - origin
     * @param scaleX Map scale X axis
     * @param scaleY Map scale Y axis
     */
    public CoordinatesConverter(double lon0, double lat0, double scaleX, double scaleY) {
        this.lon0 = lon0;
        this.lat0 = lat0;
        this.scaleX = scaleX;
        this.scaleY = scaleY;
    }

    /**
     * Get the Mercator projection of the given location (Longitude and Latitude)
     * coordinates on a map.
     *
     * @param lon Longitude to convert.
     * @param lat Latitude to convert.
     * @return Return the projected coordinates as a double vector
     * with x = vec[0] and y = vec[1].
     */
    public double[] getMercatorProjection(double lon, double lat) {
        double lonRad = (lon - lon0) * (Math.PI / 180);
        double latRad = (lat - lat0) * (Math.PI / 180);

        double x = SpatialUtils.EARTH_RADIUS * lonRad;
        double y = SpatialUtils.EARTH_RADIUS * Math.log(
                Math.tan((Math.PI / 4) + (latRad / 2)));

        double[] res = new double[2];
        res[0] = x / scaleX;
        res[1] = y / scaleY;

        return res;
    }

    /**
     * Get the projection of this Longitude and Latitude
     * coordinates into Cartesian coodinates (x,y,z).
     * <p>  The x-axis goes through long,lat (0,0), so longitude 0
     * meets the equator.
     * <br> The y-axis goes through (0,90);
     * <br> The z-axis goes through the poles.
     *
     * @param lon Longitude to convert.
     * @param lat Latitude to convert.
     * @return Return the Cartesian coordinates as a double vector
     * with x = vec[0], y = vec[1] and z = vec[2]
     */
    public double[] getCartesianProjection(double lon, double lat) {
        double x = SpatialUtils.EARTH_RADIUS * Math.cos(Math.toRadians(lat)) *
                Math.cos(Math.toRadians(lon));
        double y = SpatialUtils.EARTH_RADIUS * Math.cos(Math.toRadians(lat)) *
                Math.sin(Math.toRadians(lon));
        double z = SpatialUtils.EARTH_RADIUS * Math.sin(Math.toRadians(lat));

        double[] res = new double[]{x, y, z};

        return res;
    }
}
