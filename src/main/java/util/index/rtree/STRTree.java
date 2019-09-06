package util.index.rtree;

import org.apache.log4j.Logger;
import util.index.SpatialDataStructure;
import util.object.spatialobject.Rect;
import util.object.spatialobject.SpatialObject;
import util.object.structure.XYObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Implements a 2D Sort-Tile-Recursive (STR) partitioning method for R-Tree packing, as proposed in:
 *
 * <br> Leutenegger, Scott T., Mario A. Lopez, and Jeffrey Edgington. "STR: A simple and efficient algorithm for R-Tree packing." ICDE,
 * 1997.
 *
 * <p> STRTree constructs the RTree partitions by bulk-loading a list of spatial object, thus, once the tree is constructed the
 * insertion or removal of additional object will have no affect to the tree model.
 *
 * <p> Each node of the STRTree stores a maximum number of entries. For nodes at the leaf level, R is the bounding box of the objects in
 * the node. Leaf nodes may also have a maximum "fill rate" to ensure the nodes will no be filled with more than a rate of the maximum node
 * capacity when the tree is constructed. This is particularly useful if later insertions are intended.
 *
 * @param <T> Type of spatial object to store in this tree. Objects must be inserted in a container object, XYObject<T>.
 * @author uqdalves, Hellisk
 */
public class STRTree<T extends SpatialObject> implements SpatialDataStructure<T> {
	
	private static final Logger LOG = Logger.getLogger(STRTree.class);
	/**
	 * Whether or not to replicate boundary objects
	 */
	private static boolean IsReplicateBoundary = false;
	/**
	 * STR-Tree parameters
	 */
	private final int count;        /*r*/
	private final int maxCapacity;    /*b*/
	private final int numSlices;    /*S*/
	/**
	 * Nodes fill rate
	 */
	private final double nodesFillRate;
	/**
	 * STR list of partitions (nodes)
	 */
	private final List<STRNode<T>> strNodes;
	/**
	 * The RTree index model of this STR Tree
	 */
	private RTreeModel<T> rtreeModel = null;
	
	/**
	 * Constructs a STR-Tree with default nodesFillRate = 1.0 = 100%.
	 *
	 * @param objectsList      The list of objects to build the STR-tree.
	 * @param nodesMaxCapacity The maximum number of objects in each node/partition of this tree.
	 */
	public STRTree(List<XYObject<T>> objectsList, int nodesMaxCapacity) {
		if (objectsList == null || objectsList.isEmpty()) {
			throw new IllegalArgumentException("Object list for STR packing cannot be empty.");
		}
		if (nodesMaxCapacity <= 0) {
			throw new IllegalArgumentException("Nodes max capacity must be positive.");
		}
		this.count = objectsList.size();
		this.maxCapacity = Math.min(nodesMaxCapacity, count);
		this.numSlices = (int) Math.sqrt((float) count / maxCapacity);
		this.nodesFillRate = 1.0; // 100%
		this.strNodes = new ArrayList<>();
		// make a copy of the input list, and call build
		build(new ArrayList<>(objectsList));
	}
	
	/**
	 * Creates a new STR-Tree with the given parameters.
	 *
	 * @param objectsList      The list of objects to build the STR-tree.
	 * @param nodesMaxCapacity The maximum number of objects
	 *                         in each node/partition of this tree.
	 * @param nodesFillRate    The nodes initial filling rate ]0.0,..,1.0]. The Tree will be initially constructed with a maximum of
	 *                         (nodesMaxCapacity * nodesFillRate) objects in each leaf node.
	 */
	public STRTree(List<XYObject<T>> objectsList, int nodesMaxCapacity, double nodesFillRate) {
		if (objectsList == null || objectsList.isEmpty()) {
			throw new IllegalArgumentException("Object list for STR packing cannot be empty.");
		}
		if (nodesMaxCapacity <= 0) {
			throw new IllegalArgumentException("Nodes max capacity must be positive.");
		}
		if (nodesFillRate <= 0 || nodesFillRate > 1.0) {
			throw new IllegalArgumentException("Nodes fill rate must be between ]0.0,...,1.0].");
		}
		this.count = objectsList.size();
		this.maxCapacity = Math.min(nodesMaxCapacity, count);
		this.numSlices = (int) Math.sqrt((float) count / maxCapacity);
		this.nodesFillRate = nodesFillRate;
		this.strNodes = new ArrayList<>();
		// make a copy of the input list, and call build
		build(new ArrayList<>(objectsList));
	}
	
	/**
	 * Build the STRTree for the given list of spatial objects.
	 *
	 * @param objectsList The list of objects to build the STR-tree.
	 */
	private void build(List<XYObject<T>> objectsList) {
		// sort the objects by x-coordinate
		objectsList.sort(XYObject.X_COMPARATOR);
		// pack the point into vertical slices
		double minX = objectsList.get(0).x();
		double maxX;
		List<Slice> verticalSlices = new ArrayList<>();
		List<XYObject<T>> sliceObjects;
		int index;
		for (int i = 0; i < count; ) {
			index = i + numSlices * maxCapacity;
			index = Math.min(index, count);
			// create a slice for this objects
			sliceObjects = objectsList.subList(i, index);
			maxX = sliceObjects.get(sliceObjects.size() - 1).x();
			verticalSlices.add(new Slice(sliceObjects, minX, maxX));
			minX = maxX;
			i = index;
		}
		
		// split the slices into horizontal slices (nodes), then fill the nodes
		List<XYObject<T>> nodeObjects;
		//String nodeId;
		int countId = 0;
		int nodesCapacity = (int) (maxCapacity * nodesFillRate);
		for (Slice slice : verticalSlices) {
			minX = slice.minX;
			maxX = slice.maxX;
			// sort the objects in every slice by y coordinate
			slice.sort(XYObject.Y_COMPARATOR);
			// pack the point into vertical slices
			double minY = slice.get(0).y();
			double maxY;
			// pack them into nodes of size (maxCapacity)
			for (int j = 0; j < slice.size(); ) {
				index = j + nodesCapacity;
				index = Math.min(index, slice.size());
				// build the node
				nodeObjects = slice.subList(j, index);
				maxY = nodeObjects.get(nodeObjects.size() - 1).y();
				STRNode<T> node = new STRNode<>("" + countId++, minX, minY, maxX, maxY, objectsList.get(0).getSpatialObject().getDistanceFunction());
				node.insertAll(nodeObjects);
				strNodes.add(node);
				minY = maxY;
				j = index;
			}
		}
		
		// build the index model (RTree model)
		rtreeModel = new RTreeModel<>(this);
	}
	
	/**
	 * {@inheritDoc}
	 * <p>
	 * Insert the XY spatial object into the appropriate spatial partition (leaf node) of this tree.
	 * <br> Note that adding objects to a STR tree after the tree has been constructed will have no effect on its partitioning model.
	 * This method will simply insert the object into an existing partition only if the number of objects in the partition is smaller
	 * than its maximum capacity.
	 *
	 * @return True if the object was successfully inserted in the tree. False if this tree does not contain the object in its
	 * boundaries, or if the node to insert this object is already full.
	 */
	@Override
	public boolean insert(XYObject<T> obj) {
		// ignore objects not in this tree
		if (obj == null || !rtreeModel.getBoundary().contains(obj.x(), obj.y())) {
			return false; // object cannot be added
		}
		// find the node to add this object
		String index = rtreeModel.search(obj.x(), obj.y());
		int i = Integer.parseInt(index.replaceFirst("r", ""));
		STRNode<T> node = strNodes.get(i);
		// try to insert
		if (node.count() >= maxCapacity) {
			return false; // no more space in this node
		}
		return node.insert(obj);
	}
	
	/**
	 * {@inheritDoc}
	 * <p>
	 * Remove the XY spatial object from a leaf node of this tree.
	 * <br> Note that removing objects from a STR tree after the tree has been constructed will have no effect on its partitioning model.
	 * This method will simply remove the object from an existing partition (leaf) containing this object.
	 *
	 * @return True if the object was successfully removed. False if this spatial data structure does not contain the given object to
	 * remove.
	 */
	@Override
	public boolean remove(XYObject<T> obj) {
		// ignore objects not in this tree
		if (obj == null || !rtreeModel.getBoundary().contains(obj.x(), obj.y())) {
			return false; // object is not here
		}
		// find the node with this object
		String index = rtreeModel.search(obj.x(), obj.y());
		int i = Integer.parseInt(index.replaceFirst("r", ""));
		// try to remove
		return strNodes.get(i).remove(obj);
	}
	
	@Override
	public List<STRNode<T>> getPartitions() {
		return strNodes;
	}
	
	@Override
	public STRNode<T> partitionSearch(double x, double y) {
		if (rtreeModel.getBoundary().contains(x, y)) {
			// find the cell containing (x,y)
			String index = rtreeModel.search(x, y);
			int i = Integer.parseInt(index.replaceFirst("r", ""));
			return strNodes.get(i);
		}
		return null; // didn't find anything
	}
	
	@Override
	public List<STRNode<T>> rangePartitionSearch(SpatialObject obj) {
		if (obj == null) {
			throw new NullPointerException("Spatial object cannot be null.");
		}
		HashSet<String> indexList = rtreeModel.rangeSearch(obj);
		List<STRNode<T>> result = new ArrayList<>(indexList == null ? 0 : indexList.size());
		if (indexList != null)
			for (String index : indexList) {
				// find the cell containing this partition
				int i = Integer.parseInt(index.replaceFirst("r", ""));
				result.add(strNodes.get(i));
			}
		return result;
	}
	
	@Override
	public boolean isReplicateBoundary() {
		return IsReplicateBoundary;
	}
	
	@Override
	public void setReplicateBoundary(boolean replica) {
		STRTree.IsReplicateBoundary = replica;
	}
	
	@Override
	public RTreeModel getModel() {
		return rtreeModel;
	}
	
	@Override
	public long count() {
		return count;
	}
	
	@Override
	public boolean isEmpty() {
		return (count == 0);
	}
	
	@Override
	public void print() {
		for (STRNode<T> node : getPartitions()) {
			String id = node.getPartitionId();
			LOG.info("[" + id + "]: " + node.count());
			Rect boundary = node.getBoundary();
			LOG.info("BOUNDARY " + boundary.toString());
			// print content
			for (XYObject<T> obj : node.getObjectsList()) {
				obj.print();
			}
		}
	}
	
	/**
	 * Auxiliary class representing a slice/partition in the STR-Tree. Slices are vertical partitions along the X axis.
	 */
	private class Slice extends ArrayList<XYObject<T>> {
		/**
		 * Slice boundaries
		 */
		public double minX, maxX;
		
		/**
		 * Creates a new slice with the given list of objects.
		 *
		 * @param objList The list of objects in this slice.
		 * @param minX    Slice boundary.
		 * @param maxX    Slice boundary.
		 */
		public Slice(List<XYObject<T>> objList, double minX, double maxX) {
			super(objList);
			this.minX = minX;//objList.get(0).x();
			this.maxX = maxX;//objList.get(objList.size()-1).x();
		}
	}
}