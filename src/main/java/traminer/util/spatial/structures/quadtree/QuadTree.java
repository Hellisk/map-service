package traminer.util.spatial.structures.quadtree;

import traminer.util.spatial.objects.Rectangle;
import traminer.util.spatial.objects.SpatialObject;
import traminer.util.spatial.objects.XYObject;
import traminer.util.spatial.structures.SpatialDataStructure;

import java.util.ArrayList;
import java.util.List;

/**
 * A Quadtree is a tree data structure in which each internal node
 * has exactly four children. Quadtrees partition a two-dimensional
 * space by recursively subdividing it into four quadrants or regions.
 * <p>
 * This class implements a Point-region (PR) Quadtree, in which  a
 * spatial object is stored in the leaf node that intersects the object.
 * The leaf nodes of a PR quadtree store a list of XY spatial Objects that
 * exist within the node.
 *
 * @param <T> Type of spatial object to store in this tree. Objects must be
 *            inserted in a container object, XYObject<T>.
 * @author uqdalves
 */
@SuppressWarnings("serial")
public class QuadTree<T extends SpatialObject> implements SpatialDataStructure<T> {
    /**
     * Whether or not to replicate boundary objects
     */
    protected static boolean IsReplicateBoundary = false;
    /**
     * The spatial index model of this Quadtree
     */
    private QuadTreeModel quadTreeModel = null;
    /**
     * The root node/partition of this tree
     */
    private final QuadNode<T> root;

    /**
     * Creates a Quadtree with the given dimension, and
     * default nodes max capacity of 10 elements.
     *
     * @param minX Lower-left X coordinate.
     * @param minY Lower-left Y coordinate.
     * @param maxX Upper-right X coordinate.
     * @param maxY Upper-right Y coordinate.
     */
    public QuadTree(double minX, double minY, double maxX, double maxY) {
        this.root = new QuadNode<T>("r", new Rectangle(minX, minY, maxX, maxY));
        QuadNode.setMaxCapacity(10);
    }

    /**
     * Creates a Quadtree with the given dimensions
     * and nodes max capacity.
     *
     * @param minX          Lower-left X coordinate.
     * @param minY          Lower-left Y coordinate.
     * @param maxX          Upper-right X coordinate.
     * @param maxY          Upper-right Y coordinate.
     * @param nodesCapacity The maximum number of objects
     *                      in each node of this tree.
     */
    public QuadTree(double minX, double minY, double maxX, double maxY, int nodesCapacity) {
        if (nodesCapacity <= 0) {
            throw new IllegalArgumentException("Nodes max capacity "
                    + "must be positive.");
        }
        this.root = new QuadNode<T>("r", new Rectangle(minX, minY, maxX, maxY));
        QuadNode.setMaxCapacity(nodesCapacity);
    }

    /**
     * @return The Root node/partition.
     */
    public QuadNode<T> getRoot() {
        return root;
    }

    @Override
    public boolean isReplicateBoundary() {
        return IsReplicateBoundary;
    }

    @Override
    public void setReplicateBoundary(boolean replicateBoundary) {
        QuadTree.IsReplicateBoundary = replicateBoundary;
    }

    /**
     * @return The maximum objects capacity of the nodes in this tree.
     */
    public int getNodesCapacity() {
        return QuadNode.getMaxCapacity();
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
    public QuadTreeModel getModel() {
        if (quadTreeModel == null) {
            quadTreeModel = new QuadTreeModel(this);
        }
        return quadTreeModel;
    }

    @Override
    public List<QuadNode<T>> getPartitions() {
        List<QuadNode<T>> partitions = new ArrayList<>();
        getPartitionsRecursive(root, partitions);
        return partitions;
    }

    /**
     * Recursively collect leaf nodes.
     * Put the result into the partitions list.
     *
     * @param currentNode The current node to search (recursive).
     * @param partitions  The list to add the leaf nodes.
     */
    private void getPartitionsRecursive(QuadNode<T> currentNode, List<QuadNode<T>> partitions) {
        if (!currentNode.isLeaf()) {
            getPartitionsRecursive(currentNode.getNodeNW(), partitions);
            getPartitionsRecursive(currentNode.getNodeNE(), partitions);
            getPartitionsRecursive(currentNode.getNodeSW(), partitions);
            getPartitionsRecursive(currentNode.getNodeSE(), partitions);
        } else {
            partitions.add(currentNode);
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
     * @param node The current node to search (recursive).
     * @return Number of object in the given node.
     */
    private long countRecursive(QuadNode<T> node) {
        if (node == null) return 0;
        if (node.isLeaf()) return node.count();

        long countnw = countRecursive(node.getNodeNW());
        long countne = countRecursive(node.getNodeNE());
        long countsw = countRecursive(node.getNodeSW());
        long countse = countRecursive(node.getNodeSE());

        return (countnw + countne + countsw + countse);
    }

    @Override
    public boolean isEmpty() {
        return (root.isLeaf() && root.isEmpty());
    }

    @Override
    public QuadNode<T> partitionSearch(double x, double y) {
        if (root.getBoundary().contains(x, y)) {
            return recursivePartitionSearch(root, x, y);
        }
        return null; // didn't find anything
    }

    /**
     * Recursively search the leaf partition containing the object
     * in the (x,y) position.
     *
     * @param node The current node to search (recursive).
     * @param x    Object's X position.
     * @param y    Object's Y position.
     * @return The partition containing the object in the (x,y) position.
     */
    private QuadNode<T> recursivePartitionSearch(QuadNode<T> node, double x, double y) {
        // found the partition
        if (node.isLeaf()) {
            return node;
        }
        // tree search (point can only reside in one leaf node)
        if (node.getNodeNW().getBoundary().contains(x, y)) {
            return recursivePartitionSearch(node.getNodeNW(), x, y);
        } else if (node.getNodeNE().getBoundary().contains(x, y)) {
            return recursivePartitionSearch(node.getNodeNE(), x, y);
        } else if (node.getNodeSW().getBoundary().contains(x, y)) {
            return recursivePartitionSearch(node.getNodeSE(), x, y);
        } else {
            return recursivePartitionSearch(node.getNodeSE(), x, y);
        }
    }

    @Override
    public List<QuadNode<T>> rangePartitionSearch(SpatialObject obj) {
        List<QuadNode<T>> result = new ArrayList<>();
        if (root.getBoundary().intersects(obj)) {
            recursiveRangePartitionSearch(root, obj, result);
        }
        return result;
    }

    /**
     * Recursively search all leaf partitions intersecting with the
     * given spatial object. Put the result into the partitions list.
     *
     * @param node       The current node to search (recursive).
     * @param obj        The object to search the intersection.
     * @param partitions The list to add the results.
     */
    private void recursiveRangePartitionSearch(QuadNode<T> node, SpatialObject obj, List<QuadNode<T>> partitions) {
        // found one partition
        if (node.isLeaf()) {
            partitions.add(node);
        }
        // tree search
        if (node.getNodeNW().getBoundary().intersects(obj)) {
            recursiveRangePartitionSearch(node.getNodeNW(), obj, partitions);
        }
        if (node.getNodeNE().getBoundary().intersects(obj)) {
            recursiveRangePartitionSearch(node.getNodeNE(), obj, partitions);
        }
        if (node.getNodeSW().getBoundary().intersects(obj)) {
            recursiveRangePartitionSearch(node.getNodeSE(), obj, partitions);
        }
        if (node.getNodeSE().getBoundary().intersects(obj)) {
            recursiveRangePartitionSearch(node.getNodeSE(), obj, partitions);
        }
    }

    @Override
    public void print() {
        for (QuadNode<T> node : getPartitions()) {
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
