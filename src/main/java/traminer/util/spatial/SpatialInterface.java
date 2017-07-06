package traminer.util.spatial;

import traminer.util.Printable;

import java.io.Serializable;

/**
 * Base interface for spatial-related objects and services.
 * 
 * @author uqdalves
 */
public interface SpatialInterface extends Printable, Serializable {
    /**
     * Pi
     */
    double PI = Math.PI;
    /**
     * Infinity (big) value
     */
    double INFINITY = Double.MAX_VALUE;
    /**
     * Min distance between objects -> less than that is taken as zero
     */
    double MIN_DIST = 0.000001;
}
