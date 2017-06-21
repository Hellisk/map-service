package traminer.util.graph;

import traminer.util.spatial.SpatialInterface;

import java.io.Serializable;

/**
 * Base interface for graph objects and services.
 *
 * @author uqdalves
 */
public interface GraphInterface extends Serializable, SpatialInterface {
    // TODO
    default String getErrorMsg(String name, String message) {
        return "[GRAPH ERROR] In '" + name + "': " + message;
    }
}
