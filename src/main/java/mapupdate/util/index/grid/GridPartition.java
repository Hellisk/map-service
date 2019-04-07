package mapupdate.util.index.grid;

import mapupdate.util.index.SpatialPartition;
import mapupdate.util.object.datastructure.XYObject;
import mapupdate.util.object.spatialobject.Rect;
import mapupdate.util.object.spatialobject.SpatialObject;

/**
 * Spatial partition representing a cell in a Grid diagram.
 * <p>
 * Each partition is represented by its its boundary rectangle,
 * and contains the list of spatial objects in the partition.
 *
 * @param <T> Type of spatial object to store in this partition.
 * @author uqdalves
 */
@SuppressWarnings("serial")
public class GridPartition<T extends SpatialObject> extends SpatialPartition<XYObject<T>> {
    /**
     * The boundaries of this grid cell
     */
    private final Rect boundary;

    /**
     * Creates a new empty grid cell partition with
     * the given rectangle dimensions.
     *
     * @param cellId       The id/index of this cell.
     * @param cellBoundary The boundaries of this grid cell.
     */
    public GridPartition(String cellId, Rect cellBoundary) {
        super(cellId);
        if (cellBoundary == null) {
            throw new NullPointerException("Grid cell boundary cannot be null.");
        }
        this.boundary = cellBoundary;
    }
    /**
     * Creates a new grid cell with the given dimensions.
     */

    /**
     * Creates a new empty grid cell partition with
     * the given rectangle dimensions.
     *
     * @param cellId The id/index of this cell.
     * @param minX   Lower-left X coordinate.
     * @param minY   Lower-left Y coordinate.
     * @param maxX   Upper-right X coordinate.
     * @param maxY   Upper-right Y coordinate.
     */
    public GridPartition(String cellId,
                         double minX, double minY, double maxX, double maxY) {
        super(cellId);
        this.boundary = new Rect(minX, minY, maxX, maxY);
    }

    @Override
    public Rect getBoundary() {
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
