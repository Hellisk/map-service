package edu.uq.dke.mapupdate.util.index;

import edu.uq.dke.mapupdate.util.object.SpatialInterface;
import edu.uq.dke.mapupdate.util.object.spatialobject.SpatialObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Superclass for spatial partitions.
 * <p>
 * Contains a list of spatial objects in a spatial partition
 * (objects that intersect with the boundaries of the partition).
 *
 * @author uqdalves
 */
@SuppressWarnings({"serial", "hiding"})
public abstract class SpatialPartition<XYObject> implements SpatialInterface {
    /**
     * The partition identifier
     */
    protected final String partitionId;
    /**
     * The list of spatial objects in the partition
     */
    protected List<XYObject> objectsList;

    /**
     * Creates a new empty spatial partition.
     *
     * @param partitionId The id/index of this partition.
     */
    public SpatialPartition(String partitionId) {
        if (partitionId == null) {
            throw new NullPointerException(
                    "Spatial partition id cannot be null.");
        }
        this.partitionId = partitionId;
        this.objectsList = new ArrayList<>(1);
    }

    /**
     * Creates a new empty spatial partition with the given initial capacity.
     *
     * @param partitionId     The id/index of this partition.
     * @param initialCapacity The partition initial capacity.
     */
    public SpatialPartition(String partitionId, SpatialObject boundary, int initialCapacity) {
        if (partitionId == null) {
            throw new NullPointerException(
                    "Spatial partition id cannot be null.");
        }
        this.partitionId = partitionId;
        this.objectsList = new ArrayList<>(initialCapacity);
    }

    /**
     * Get the identifier of this spatial partition.
     *
     * @return The id/index of this partition
     */
    public String getPartitionId() {
        return partitionId;
    }

    /**
     * Insert a spatial object to this partition.
     *
     * @param obj The spatial object to insert.
     * @return Returns False if the object does not
     * intersect with this partition. True otherwise.
     */
    public abstract boolean insert(XYObject obj);

    /**
     * Insert all spatial objects in the list to
     * this partition. Insert one by one.
     *
     * @param objList The list of spatial object to insert.
     */
    public final void insertAll(List<XYObject> objList) {
        if (objList == null) {
            throw new NullPointerException(
                    "Objects list to insert cannot be null.");
        }
        for (XYObject obj : objList) {
            insert(obj);
        }
    }

    /**
     * Remove a spatial object from this partition.
     *
     * @param obj The spatial object to remove.
     * @return Return False if the object does not
     * belong to this partition. True otherwise.
     */
    public abstract boolean remove(XYObject obj);

    /**
     * Remove all spatial objects in the list from
     * this partition. Remove one by one.
     *
     * @param objList The list with the spatial objects to remove.
     */
    public final void removeAll(List<XYObject> objList) {
        if (objList == null) {
            throw new NullPointerException(
                    "Objects list to remove cannot be null.");
        }
        for (XYObject obj : objList) {
            remove(obj);
        }
    }

    /**
     * Returns the spatial object representing the
     * boundaries of this spatial partition.
     *
     * @return The boundary of this spatial partition.
     */
    public abstract SpatialObject getBoundary();

    /**
     * Returns an immutable/unmodifiable list containing the
     * spatial objects in this partition.
     *
     * @return The list containing the spatial objects
     * in this partition.
     */
    public List<XYObject> getObjectsList() {
        // must not be modified by the client
        return Collections.unmodifiableList(objectsList);
    }

    /**
     * Check whether this spatial partition
     * intersect with the given spatial object.
     *
     * @param obj The spatial object to check.
     * @return True if this spatial partition
     * intersect with the given spatial object.
     */
    public boolean intersects(SpatialObject obj) {
        return this.getBoundary().intersects(obj);
    }

    /**
     * @return The number of spatial objects in this partition.
     */
    public int count() {
        if (objectsList != null) {
            return objectsList.size();
        }
        return 0;
    }

    /**
     * Remove all objects from this partition.
     */
    public void clear() {
        objectsList = null;
    }

    /**
     * Check whether this partition is empty.
     *
     * @return True if there is no object in this partition.
     */
    public boolean isEmpty() {
        return (objectsList == null || objectsList.isEmpty());
    }
}
