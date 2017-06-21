package traminer.util.spatial;

import java.io.Serializable;

/**
 * Base interface for spatial-related objects and services.
 *
 * @author uqdalves
 */
public interface SpatialInterface extends Serializable {
    double PI = Math.PI;
    /**
     * Earth radius (average) in meters
     */
    int EARTH_RADIUS = 6371000;
    /**
     * Infinity (big) value
     */
    double INFINITY = Double.MAX_VALUE;
    /**
     * Min distance between objects -> less than that is taken as zero
     */
    double MIN_DIST = 0.000001;

    default void print(Object s) {
        System.out.print(s.toString());
    }

    default void println(Object s) {
        System.out.println(s.toString());
    }

    default void println() {
        System.out.println();
    }

    default void printerr(Object s) {
        System.err.println(s.toString());
    }
}
