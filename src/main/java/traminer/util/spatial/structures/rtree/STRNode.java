package traminer.util.spatial.structures.rtree;

import traminer.util.spatial.objects.Rectangle;
import traminer.util.spatial.objects.SpatialObject;
import traminer.util.spatial.objects.XYObject;
import traminer.util.spatial.structures.SpatialPartition;

/**
 * A spatial partition representing a node in a STRTree.
 * <p>
 * Each node is represented by its boundary rectangle, 
 * and contains the list of spatial objects in the node.
 * 
 * @author uqdalves
 *
 * @param <T> Type of spatial object to store in this node.
 */
@SuppressWarnings("serial")
public class STRNode<T extends SpatialObject> extends SpatialPartition<XYObject<T>> {
    /**
     * The spatial boundaries of this node/partition
     */
    private final Rectangle boundary;

    /**
     * Creates a new RTree cell with the given rectangle dimensions.
     *
     * @param partitionId  The identifier/index of this node.
     * @param nodeBoundary The spatial boundary of this node.
     */
    public STRNode(String partitionId, Rectangle nodeBoundary) {
        super(partitionId);
        if (nodeBoundary == null) {
            throw new NullPointerException("STRTree node boundary cannot be null.");
        }
        this.boundary = nodeBoundary;
    }

    /**
     * Creates a new STRTree node with the given rectangle dimensions.
     *
     * @param partitionId The identifier/index of this node.
     * @param minX        Lower-left X coordinate.
     * @param minY        Lower-left Y coordinate.
     * @param maxX        Upper-right X coordinate.
     * @param maxY        Upper-right Y coordinate.
     */
    public STRNode(String partitionId,
                   double minX, double minY, double maxX, double maxY) {
        super(partitionId);
        this.boundary = new Rectangle(minX, minY, maxX, maxY);
    }

    @Override
    public boolean insert(XYObject<T> obj) {
        // Ignore objects that do not belong in this node
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

    @Override
    public Rectangle getBoundary() {
        return boundary;
    }
}
