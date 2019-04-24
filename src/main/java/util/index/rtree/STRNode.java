package util.index.rtree;

import util.function.DistanceFunction;
import util.index.SpatialPartition;
import util.object.spatialobject.Rect;
import util.object.spatialobject.SpatialObject;
import util.object.structure.XYObject;

/**
 * A spatial partition representing a node in a STRTree.
 * <p>
 * Each node is represented by its boundary rectangle,
 * and contains the list of spatial objects in the node.
 *
 * @param <T> Type of spatial object to store in this node.
 * @author uqdalves
 */
@SuppressWarnings("serial")
public class STRNode<T extends SpatialObject> extends SpatialPartition<XYObject<T>> {
	/**
	 * The spatial boundaries of this node/partition
	 */
	private final Rect boundary;
	
	/**
	 * Creates a new RTree cell with the given rectangle dimensions.
	 *
	 * @param partitionId  The identifier/index of this node.
	 * @param nodeBoundary The spatial boundary of this node.
	 */
	public STRNode(String partitionId, Rect nodeBoundary) {
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
	public STRNode(String partitionId, double minX, double minY, double maxX, double maxY, DistanceFunction df) {
		super(partitionId);
		this.boundary = new Rect(minX, minY, maxX, maxY, df);
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
	public Rect getBoundary() {
		return boundary;
	}
}
