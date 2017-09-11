package traminer.util.spatial.structures;

import traminer.util.spatial.SpatialInterface;
import traminer.util.spatial.objects.SpatialObject;

import java.util.HashSet;

/**
 * Interface for spatial indexes.
 * 
 * @author uqdalves
 */
public interface SpatialIndexModel extends SpatialInterface {
    /**
     * Search and return the index of the partition that intersects
     * with the spatial object in the position (x,y).
     *
     * @param x The X coordinate to search.
     * @param y The Y coordinate to search.
     * @return Returns null if the (x,y) position is out of the boundaries of
     * this index model.
     */
    String search(double x, double y);

    /**
     * Search and Return the indexes of all partitions intersecting
     * with the given spatial object.
     *
     * @param obj The spatial object to do the search.
     * @return The indexes of all partitions intersecting with this
     *         spatial object. An empty set if the object is out of the
     *         boundaries of this index model.
     */
    HashSet<String> rangeSearch(SpatialObject obj);

    /**
     * Returns a spatial object representing the boundary of the partition with
     * the given index.
     *
     * @param index The partition index to search.
     * @return The boundary of the partition in this model with the given index.
     */
    SpatialObject get(String index);

    /**
     * @return Returns the spatial object representing the boundaries of this
     *         spatial model.
     */
    SpatialObject getBoundary();

    /**
     * @return The number of partitions in this model.
     */
    int size();

    /**
     * Check whether this index is empty.
     *
     * @return True if this model is empty.
     */
    boolean isEmpty();

    /**
     * Print this model to the system standard output.
     */
    void print();
}
