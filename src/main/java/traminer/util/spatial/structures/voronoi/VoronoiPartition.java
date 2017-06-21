package traminer.util.spatial.structures.voronoi;

import traminer.util.spatial.objects.SpatialObject;
import traminer.util.spatial.objects.XYObject;
import traminer.util.spatial.structures.SpatialPartition;

/**
 * Object representing a partition in a voronoi diagram.
 * <p>
 * Each partition is represented by its Voronoi Polygon
 * boundaries, and contains the list of spatial objects
 * in the partition.
 *
 * @param <T> Type of spatial object to store in this partition.
 * @author uqdalves
 */
@SuppressWarnings("serial")
public class VoronoiPartition<T extends SpatialObject> extends SpatialPartition<XYObject<T>> {
    /**
     * The boundaries of this Voronoi partition
     */
    private VoronoiPolygon boundary = null;

    /**
     * Creates a new empty Voronoi partition with the given
     * polygon boundary.
     *
     * @param partitionId The id/index of this partition.
     * @param boundary    The boundaries of this Voronoi partition,
     */
    public VoronoiPartition(String partitionId, VoronoiPolygon boundary) {
        super(partitionId);
        if (boundary == null) {
            throw new NullPointerException("Voronoi partition "
                    + "boundary cannot be null.");
        }
        this.boundary = boundary;
    }

    @Override
    public VoronoiPolygon getBoundary() {
        return boundary;
    }

    @Override
    public boolean insert(XYObject<T> obj) {
        // Ignore objects that do not belong in this cell
        if (obj == null || !boundary.contains(obj.toPoint())) {
            return false; // object cannot be added
        }
        return objectsList.add(obj);
    }

    @Override
    public boolean remove(XYObject<T> obj) {
        // object is not here
        if (obj == null || !boundary.contains(obj.toPoint())) {
            return false;
        }
        return objectsList.remove(obj);
    }

}
