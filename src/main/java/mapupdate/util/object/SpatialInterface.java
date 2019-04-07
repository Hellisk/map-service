package mapupdate.util.object;

import java.io.Serializable;

/**
 * Base interface for spatial-related objects and services.
 *
 * @author uqdalves
 */
public interface SpatialInterface extends Serializable {
    /**
     * Pi
     */
    double PI = Math.PI;
    /**
     * Infinity (big) value
     */
    double INFINITY = Double.POSITIVE_INFINITY;
    /**
     * Min distance between objects -> less than that is taken as zero
     */
    double MIN_DIST = 0.000001;
}
