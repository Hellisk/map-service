package traminer.util.spatial;

/**
 * Convert latitude and longitude coordinates
 * to cartesian coordinates.
 *
 * @author uqdalves
 */
@SuppressWarnings("serial")
public class CoordinateProjectionService implements SpatialInterface {
    // referential longitude and latitude
    private double lon_0 = 70.0; // origin - North East
    private double lat_0 = 20.0;
    // map area scale
    private double scale_x = 100000;
    private double scale_y = 100000;

    /**
     * @param lon_0   Referential longitude - origin
     * @param lat_0   Referential latitude - origin
     * @param scale_x Map scale X axis
     * @param scale_y Map scale Y axis
     */
    public CoordinateProjectionService(double lon_0, double lat_0,
                                       double scale_x, double scale_y) {
        super();
        this.lon_0 = lon_0;
        this.lat_0 = lat_0;
        this.scale_x = scale_x;
        this.scale_y = scale_y;
    }

    /**
     * Get the Mercator projection of this Longitude and Latitude
     * coordinates on a map. The map scale is given by the application
     * paramenters.
     *
     * @return Return the projected coordinates as a double vector
     * with x = vec[0] and y = vec[1]
     */
    public double[] getMercatorProjection(double lon, double lat) {
        double lonRad = (lon - lon_0) * (PI / 180);
        double latRad = (lat - lat_0) * (PI / 180);

        double x = EARTH_RADIUS * lonRad;
        double y = EARTH_RADIUS * Math.log(Math.tan((PI / 4) + (latRad / 2)));

        double[] res = new double[2];
        res[0] = x / scale_x;
        res[1] = y / scale_y;

        return res;
    }

    /**
     * Get the projection of this Longitude and Latitude
     * coordinates into Cartesian coodinates (x,y,z).
     * <p>
     * The x-axis goes through long,lat (0,0), so longitude 0 meets the equator.
     * <br>
     * The y-axis goes through (0,90);
     * <br>
     * The z-axis goes through the poles.
     *
     * @return Return the cartesian coordinates as a double vector
     * with x = vec[0], y = vec[1] and z = vec[2]
     */
    public double[] getCartesianProjection(double lon, double lat) {
        double x = EARTH_RADIUS * Math.cos(Math.toRadians(lat)) * Math.cos(Math.toRadians(lon));
        double y = EARTH_RADIUS * Math.cos(Math.toRadians(lat)) * Math.sin(Math.toRadians(lon));
        double z = EARTH_RADIUS * Math.sin(Math.toRadians(lat));

        double[] res = new double[3];
        res[0] = x;
        res[1] = y;
        res[2] = z;

        return res;
    }

    public void print() {
        System.out.println("ProjectionTransformation.");
    }
}
