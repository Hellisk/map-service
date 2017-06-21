package traminer.util.spatial.structures.kdtree;

import traminer.util.spatial.objects.Rectangle;
import traminer.util.spatial.objects.SpatialObject;
import traminer.util.spatial.structures.SpatialIndexModel;

import java.util.HashSet;

/**
 * A spatial index model representing a k-d Tree partitioning structure.
 * <p>
 * A k-d Tree (short for k-dimensional tree) is a space-partitioning data
 * structure for organizing points in a k-dimensional space. k-d trees are a
 * useful data structure for several applications, such as searches involving a
 * multidimensional search key (e.g. range searches and nearest neighbor
 * searches). k-d trees are a special case of binary space partitioning trees.
 *
 * @author uqdalves
 */
@SuppressWarnings("serial")
public class KdTreeModel implements SpatialIndexModel {
    /**
     * The boundaries of this tree/node
     */
    private Rectangle boundary = null;
    /**
     * The id of this tree/node
     */
    private String nodeId = null;
    /**
     * The tree that originated this tree/node
     */
    private KdTreeModel parentNode = null;
    /**
     * Left|bottom child node
     */
    private KdTreeModel nodeInferior = null;
    /**
     * Right|top child node
     */
    private KdTreeModel nodeSuperior = null;

    /**
     * Creates an empty k-d Tree.
     */
    public KdTreeModel() {
    }

    /**
     * Creates a k-d Tree model from the given
     * k-d Tree partitioning.
     *
     * @param tree The k-d Tree partitioning to build
     *             the index from.
     */
    @SuppressWarnings("rawtypes")
    public KdTreeModel(KdTree tree) {
        if (tree == null) {
            throw new NullPointerException("Spatial data structure for "
                    + "k-d Tree model building cannot be null.");
        }
        if (tree.isEmpty()) {
            throw new IllegalArgumentException("Spatial data structure for "
                    + "k-d Tree model building cannot be empty.");
        }
        build(tree.getRoot());
    }

    /**
     * The spatial boundary of this node.
     */
    @Override
    public Rectangle getBoundary() {
        return boundary;
    }

    /**
     * @return The id/index of this node/tree.
     */
    public String getNodeId() {
        return nodeId;
    }

    /**
     * @return The tree that originates this tree/node.
     */
    public KdTreeModel getParentNode() {
        return parentNode;
    }

    /**
     * @return Left | bottom child of this tree/node.
     */
    public KdTreeModel getNodeInferior() {
        return nodeInferior;
    }

    /**
     * @return Right | top child of this tree/node.
     */
    public KdTreeModel getNodeSuperior() {
        return nodeSuperior;
    }

    /**
     * Check whether this node is a leaf node.
     *
     * @return True if this is a leaf node.
     */
    public boolean isLeaf() {
        return (nodeInferior == null && nodeSuperior == null);
    }

    @SuppressWarnings("rawtypes")
    private void build(KdNode node) {
        boundary = node.getBoundary();
        nodeId = node.getPartitionId();

        if (node.getNodeInferior() != null) {
            nodeInferior = new KdTreeModel();
            nodeInferior.parentNode = this;
            nodeInferior.build(node.getNodeInferior());
        }
        if (node.getNodeSuperior() != null) {
            nodeSuperior = new KdTreeModel();
            nodeSuperior.parentNode = this;
            nodeSuperior.build(node.getNodeSuperior());
        }
    }

    @Override
    public String search(double x, double y) {
        if (boundary.contains(x, y)) {
            return recursiveSearch(this, x, y);
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
     * @return The id/index of the partition containing the object in the (x,y) position.
     */
    private String recursiveSearch(KdTreeModel node, double x, double y) {
        // found the partition
        if (node.isLeaf()) {
            return node.getNodeId();
        }
        // tree search (point can only reside in one leaf node)
        if (node.getNodeInferior().getBoundary().contains(x, y)) {
            return recursiveSearch(node.getNodeInferior(), x, y);
        } else {
            return recursiveSearch(node.getNodeSuperior(), x, y);
        }
    }

    @Override
    public HashSet<String> rangeSearch(SpatialObject obj) {
        HashSet<String> result = new HashSet<>();
        if (obj != null && boundary.intersects(obj)) {
            recursiveRangeSearch(this, obj, result);
        }
        return result;
    }

    /**
     * Recursively search the id of all leaf partitions intersecting with
     * the given spatial object. Put the result into the partitions id list.
     *
     * @param node   The current node to search (recursive).
     * @param obj    The object to search the intersection.
     * @param idList The list to add the results.
     */
    private void recursiveRangeSearch(KdTreeModel node, SpatialObject obj, HashSet<String> idList) {
        // found one partition
        if (node.isLeaf()) {
            idList.add(node.getNodeId());
        }
        // tree search
        if (node.getNodeInferior().getBoundary().intersects(obj)) {
            recursiveRangeSearch(node.getNodeInferior(), obj, idList);
        }
        if (node.getNodeSuperior().getBoundary().intersects(obj)) {
            recursiveRangeSearch(node.getNodeSuperior(), obj, idList);
        }
    }

    @Override
    public Rectangle get(final String index) {
        if (index == null || index.isEmpty()) {
            throw new IllegalArgumentException(
                    "k-d Tree index must not be empty.");
        }
        // get nodes (first node is the root)
        String nodes[] = index.replaceAll("x|y", "").split("-");
        KdTreeModel node = this;
        if (nodes.length == 1 && "r".equals(nodes[0])) {
            return node.getBoundary();
        }
        for (int i = 1; i < nodes.length; i++) {
            if (node.isLeaf()) {
                throw new IndexOutOfBoundsException(
                        "k-d Tree index out of bounds.");
            }
            String bin = nodes[i];
            // binary search
            if ("0".equals(bin)) {
                node = node.nodeInferior;
            } else if ("1".equals(bin)) {
                node = node.nodeSuperior;
            } else {
                throw new IllegalArgumentException(
                        "Invalid k-d Tree index.");
            }
        }

        return node.getBoundary();
    }

    @Override
    public int size() {
        return sizeRecursive(this);
    }

    /**
     * Recursively count the number of leaf nodes in this tree.
     *
     * @param node The current node to search (recursive).
     * @return number of partition in the sub-tree
     */
    private int sizeRecursive(KdTreeModel node) {
        if (node == null) {
            return 0;
        }
        if (node.isLeaf()) {
            return 1;
        }
        int sizeInf = sizeRecursive(node.getNodeInferior());
        int sizeSup = sizeRecursive(node.getNodeSuperior());

        return (sizeInf + sizeSup);
    }

    @Override
    public boolean isEmpty() {
        return isLeaf();
    }

    @Override
    public void print() {
        println("[K-D TREE]");
        printRecursive(this);
    }

    /**
     * Recursively print the nodes of this tree.
     *
     * @param node Current node (recursive).
     */
    private void printRecursive(KdTreeModel node) {
        if (node == null) return;
        // print this node
        println(node.getNodeId());
        // print children, if any
        if (node.isLeaf()) return;
        printRecursive(node.nodeInferior);
        printRecursive(node.nodeSuperior);
    }
}