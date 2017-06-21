package traminer.util.spatial.structures;

import traminer.util.spatial.distance.PointDistanceFunction;
import traminer.util.spatial.objects.SpatialObject;
import traminer.util.spatial.objects.XYObject;

import java.util.List;

/**
 * Base interface for spatial queries implemented by the
 * spatial data structures.
 *
 * @param <T> The type of spatial object to query, i.e. the type
 *            of spatial object stored in the spatial data structure.
 * @author uqdalves
 */
public interface SpatialQuery<T extends SpatialObject> {

    /**
     * Search the nearest-neighbor from the object in the (x,y) position.
     *
     * @param x        Query object X coordinate.
     * @param y        Query object Y coordinate.
     * @param distFunc The function to calculate the object's distance
     *                 (points distance only).
     * @return The nearest-neighbor from the object in the (x,y) position.
     */
    XYObject<T> nearestNeighborSearch(double x, double y, PointDistanceFunction distFunc);

    /**
     * Search the k-nearest-neighbors from the object in the (x,y) position.
     *
     * @param x        Query object X coordinate.
     * @param y        Query object Y coordinate.
     * @param k        The number of neighbor to return.
     * @param distFunc The function to calculate the object's distance
     *                 (points distance only).
     * @return The nearest-neighbor from the object in the (x,y) position.
     */
    List<XYObject<T>> kNearestNeighborsSearch(double x, double y, int k, PointDistanceFunction distFunc);

    /**
     * Search all spatial objects intersecting with the given query object.
     *
     * @param queryObj The query object.
     * @return A list of spatial objects intersecting with the query object.
     */
    List<XYObject<T>> rangeSearch(SpatialObject queryObj);
}
