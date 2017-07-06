package traminer.util.spatial;

import java.io.Serializable;

/**
 * A list of geographic location on Earth. For referential purposes.
 *
 * @author uqdalves
 */
public enum GeographicLocation implements Serializable {
    /**
     * South hemisphere
     */
    SOUTH,
    /**
     * North hemisphere
     */
    NORTH,
    /**
     * e.g. South America
     */
    SOUTH_WEST,
    /**
     * e.g. North America
     */
    NORTH_WEST,
    /**
     * e.g. Australia
     */
    SOUTH_EAST,
    /**
     * e.g. Asia
     */
    NORTH_EAST,
    /**
     * e.g. Europe
     */
    MIDDLE_NORTH,
    /**
     * e.g. Africa
     */
    MIDDLE_SOUTH,
    /**
     * Equator line
     */
    EQUATOR,
    /**
     * Tropic lines
     */
    TROPICS,
    /**
     * Near the poles
     */
    POLES
}
