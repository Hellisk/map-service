package traminer.util.map;

import traminer.util.Printable;

import java.io.Serializable;

/**
 * Base interface for map objects and services.
 * 
 * @author uqdalves
 */
public interface MapInterface extends Printable, Serializable {
    /**
     * Infinity (big) value
     */
    double INFINITY = Double.MAX_VALUE;
}

