package traminer.util.spatial.structures.kdtree;

import traminer.util.spatial.objects.Rectangle;
import traminer.util.spatial.objects.SpatialObject;
import traminer.util.spatial.objects.XYObject;
import traminer.util.spatial.structures.SpatialDataStructure;

import java.util.ArrayList;
import java.util.List;

/**
 * A k-d Tree (short for k-dimensional tree) is a space-partitioning data
 * structure for organizing points in a k-dimensional space. k-d trees are a
 * useful data structure for several applications, such as searches involving a
 * multidimensional search key (e.g. range searches and nearest neighbor
 * searches). k-d trees are a special case of binary space partitioning trees.
 * <p>
 * This class implements a 2D region k-d Tree, in which spatial objects
 * are stored in the leaf nodes that intersects the objects. The leaf nodes
 * of the k-d Tree store a list of XY spatial Objects that exist within the node.
 *
 * @param <T> Type of spatial object to store in this tree. Objects must be
 *            inserted in a container object, XYObject<T>.
 * @author uqdalves
 */
@SuppressWarnings("serial")
public class KdTree<T extends SpatialObject> implements SpatialDataStructure<T> {
    /**
     * Whether or not to replicate boundary objects
     */
    protected static boolean IsReplicateBoundary = false;
    /**
     * The spatial index model of this k-d Tree
     */
    private KdTreeModel kdTreeModel = null;
    /**
     * The root node/partition of this tree
     */
    private final KdNode<T> root;

    /**
     * Create a  k-d Tree  with the given dimension, and
     * default nodes max capacity of 10 elements.
     *
     * @param minX Lower-left X coordinate.
     * @param minY Lower-left Y coordinate.
     * @param maxX Upper-right X coordinate.
     * @param maxY Upper-right Y coordinate.
     */
    public KdTree(double minX, double minY, double maxX, double maxY) {
        this.root = new KdNode<T>("r", 0, new Rectangle(minX, minY, maxX, maxY));
        KdNode.setMaxCapacity(10);
    }

    /**
     * Create a k-d Tree with the given dimensions
     * and nodes capacity.
     *
     * @param minX          Lower-left X coordinate.
     * @param minY          Lower-left Y coordinate.
     * @param maxX          Upper-right X coordinate.
     * @param maxY          Upper-right Y coordinate.
     * @param nodesCapacity The maximum capacity of the nodes in this tree.
     */
    public KdTree(double minX, double minY, double maxX, double maxY, int nodesCapacity) {
        if (nodesCapacity <= 0) {
            throw new IllegalArgumentException("Nodes max capacity "
                    + "must be positive.");
        }
        this.root = new KdNode<T>("r", 0, new Rectangle(minX, minY, maxX, maxY));
        KdNode.setMaxCapacity(nodesCapacity);
    }

    /**
     * @return The root node/partition
     */
    public KdNode<T> getRoot() {
        return root;
    }

    @Override
    public boolean isReplicateBoundary() {
        return IsReplicateBoundary;
    }

    @Override
    public void setReplicateBoundary(boolean replicateBoundary) {
        KdTree.IsReplicateBoundary = replicateBoundary;
    }

    @Override
    public boolean insert(XYObject<T> obj) {
        if (obj == null || !root.getBoundary().contains(obj.x(), obj.y())) {
            return false; // object cannot be added here
        }
        return root.insert(obj);
    }

    @Override
    public boolean remove(XYObject<T> obj) {
        if (obj == null || !root.getBoundary().contains(obj.x(), obj.y())) {
            return false; // object does not belong here
        }
        return root.remove(obj);
    }

    @Override
    public KdTreeModel getModel() {
        if (kdTreeModel == null) {
            kdTreeModel = new KdTreeModel(this);
        }
        return kdTreeModel;
    }

    @Override
    public List<KdNode<T>> getPartitions() {
        List<KdNode<T>> partitions = new ArrayList<>();
        getPartitionsRecursive(root, partitions);
        return partitions;
    }

    /**
     * Recursively collect leaf nodes.
     *
     * @param node
     * @param partitions
     */
    private void getPartitionsRecursive(KdNode<T> node, List<KdNode<T>> partitions) {
        if (!node.isLeaf()) {
            getPartitionsRecursive(node.getNodeInferior(), partitions);
            getPartitionsRecursive(node.getNodeSuperior(), partitions);
        } else {
            partitions.add(node);
        }
    }

    @Override
    public long count() {
        if (root == null) {
            return 0;
        }
        return countRecursive(root);
    }

    /**
     * Recursively count the number of objects in this tree.
     *
     * @param node
     * @return The number of objects in the given node
     */
    private long countRecursive(KdNode<T> node) {
        if (node == null) {
            return 0;
        }
        if (node.isLeaf()) {
            return node.count();
        }
        long counti = countRecursive(node.getNodeInferior());
        long counts = countRecursive(node.getNodeSuperior());

        return (counti + counts);
    }

    @Override
    public boolean isEmpty() {
        return (root.isLeaf() && root.isEmpty());
    }

    @Override
    public KdNode<T> partitionSearch(double x, double y) {
        if (root.getBoundary().contains(x, y)) {
            return recursivePartitionSearch(root, x, y);
        }
        return null; // didn't find anything
    }

    /**
     * Search the leaf partition of the given node containing
     * the object in the (x,y) position.
     *
     * @param node
     * @param x
     * @param y
     * @return The leaf partition containing the object
     * in the (x,y) position.
     */
    private KdNode<T> recursivePartitionSearch(KdNode<T> node, double x, double y) {
        // found the partition
        if (node.isLeaf()) {
            return node;
        }
        if (node.getNodeInferior().getBoundary().contains(x, y)) {
            return recursivePartitionSearch(node.getNodeInferior(), x, y);
        } else {
            return recursivePartitionSearch(node.getNodeSuperior(), x, y);
        }
    }

    @Override
    public List<KdNode<T>> rangePartitionSearch(SpatialObject obj) {
        List<KdNode<T>> result = new ArrayList<>();
        if (root.getBoundary().intersects(obj)) {
            recursiveRangePartitionSearch(root, obj, result);
        }
        return result;
    }

    /**
     * Search all leaf partitions intersecting with
     * the given spatial object. Put the result into
     * the partitions list.
     *
     * @param node
     * @param obj
     * @param partitions
     */
    private void recursiveRangePartitionSearch(KdNode<T> node, SpatialObject obj, List<KdNode<T>> partitions) {
        // found one partition
        if (node.isLeaf()) {
            partitions.add(node);
        }
        // tree search
        if (node.getNodeInferior().getBoundary().intersects(obj)) {
            recursiveRangePartitionSearch(node.getNodeInferior(), obj, partitions);
        }
        if (node.getNodeSuperior().getBoundary().intersects(obj)) {
            recursiveRangePartitionSearch(node.getNodeSuperior(), obj, partitions);
        }
    }

    @Override
    public void print() {
        for (KdNode<T> node : getPartitions()) {
            String id = node.getPartitionId();
            println("[" + id + "]: " + node.count());
            Rectangle boundary = node.getBoundary();
            println("BOUNDARY " + boundary.toString());
            // print content
            for (XYObject<T> obj : node.getObjectsList()) {
                obj.print();
            }
            println("");
        }
    }
}
