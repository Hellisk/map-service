package traminer.util.spatial.structures.strips;

import traminer.util.spatial.objects.Rectangle;
import traminer.util.spatial.objects.SpatialObject;
import traminer.util.spatial.objects.XYObject;
import traminer.util.spatial.structures.SpatialPartition;

/**
 * Object representing a partition in a Strips diagram.
 * <p>
 * Each partition is represented by its its boundary rectangle,
 * and contains the list of spatial objects in the partition.
 *
 * @param <T> Type of spatial object to store in this partition.
 * @author uqdalves
 */
@SuppressWarnings("serial")
public class StripsPartition<T extends SpatialObject> extends SpatialPartition<XYObject<T>> {
    /**
     * The boundaries of this strips cell
     */
    private final Rectangle boundary;

    /**
     * Creates a new strips partition with the given rectangle dimensions.
     *
     * @param partitionId The id/index of this partition.
     * @param boundary    The boundaries of this strips cell
     */
    public StripsPartition(String partitionId, Rectangle boundary) {
        super(partitionId);
        if (boundary == null) {
            throw new NullPointerException("Strips cell boundary cannot be null.");
        }
        this.boundary = boundary;
    }

    /**
     * Creates a new grid cell with the given dimensions.
     *
     * @param partitionId The id/index of this partition.
     * @param minX        Lower-left X coordinate.
     * @param minY        Lower-left Y coordinate.
     * @param maxX        Upper-right X coordinate.
     * @param maxY        Upper-right Y coordinate.
     */
    public StripsPartition(String partitionId,
                           double minX, double minY, double maxX, double maxY) {
        super(partitionId);
        this.boundary = new Rectangle(minX, minY, maxX, maxY);
    }

    @Override
    public Rectangle getBoundary() {
        return boundary;
    }

    @Override
    public boolean insert(XYObject<T> obj) {
        // Ignore objects that do not belong in this cell
        if (obj == null || !boundary.contains(obj.x(), obj.y())) {
            return false; // object cannot be added
        }
        return objectsList.add(obj);
    }

    @Override
    public boolean remove(XYObject<T> obj) {
        // object is not here
        if (obj == null || !boundary.contains(obj.x(), obj.y())) {
            return false;
        }
        return objectsList.remove(obj);
    }
}
