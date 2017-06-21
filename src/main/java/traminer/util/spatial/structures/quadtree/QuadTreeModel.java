package traminer.util.spatial.structures.quadtree;

import traminer.util.spatial.objects.Rectangle;
import traminer.util.spatial.objects.SpatialObject;
import traminer.util.spatial.structures.SpatialIndexModel;

import java.util.HashSet;

/**
 * A spatial index model representing a QuadTree partitioning structure.
 * <p>
 * A Quadtree is a tree data structure in which each internal node
 * has exactly four children. Quadtrees partition a two-dimensional
 * space by recursively subdividing it into four quadrants or regions.
 *
 * @author uqdalves
 */
@SuppressWarnings("serial")
public class QuadTreeModel implements SpatialIndexModel {
    /**
     * The boundaries of this tree/node
     */
    private Rectangle boundary = null;
    /**
     * The id of this tree/node
     */
    private String nodeId = null;
    /**
     * The parent node of this node
     */
    private QuadTreeModel parentNode = null;
    /**
     * North-West child node
     */
    private QuadTreeModel nodeNW = null;
    /**
     * North-East child node
     */
    private QuadTreeModel nodeNE = null;
    /**
     * South-West child node
     */
    private QuadTreeModel nodeSW = null;
    /**
     * South-East child node
     */
    private QuadTreeModel nodeSE = null;

    /**
     * Creates an empty Quadtree model.
     */
    public QuadTreeModel() {
    }

    /**
     * Builds a QuadTree model from the given QuadTree partitioning.
     *
     * @param quadtree The Quadtree partitioning used to build this model.
     */
    @SuppressWarnings("rawtypes")
    public QuadTreeModel(QuadTree quadtree) {
        if (quadtree == null) {
            throw new NullPointerException("Spatial data structure for "
                    + "QuadTree model building cannot be null.");
        }
        if (quadtree.isEmpty()) {
            throw new IllegalArgumentException("Spatial data structure for "
                    + "QuadTree model building cannot be empty.");
        }
        build(quadtree.getRoot());
    }

    @Override
    public Rectangle getBoundary() {
        return boundary;
    }

    /**
     * @return The index/id of this node.
     */
    public String getNodeId() {
        return nodeId;
    }

    /**
     * @return The parent node of this node.
     */
    public QuadTreeModel getParentNode() {
        return parentNode;
    }

    /**
     * @return The North-West child node of this node.
     */
    public QuadTreeModel getNodeNW() {
        return nodeNW;
    }

    /**
     * @return The North-East child node of this node.
     */
    public QuadTreeModel getNodeNE() {
        return nodeNE;
    }

    /**
     * @return The South-West child node of this node.
     */
    public QuadTreeModel getNodeSW() {
        return nodeSW;
    }

    /**
     * @return The South-East child node of this node.
     */
    public QuadTreeModel getNodeSE() {
        return nodeSE;
    }

    /**
     * @return True if this node is a leaf node.
     */
    public boolean isLeaf() {
        return (nodeNW == null && nodeNE == null &&
                nodeSE == null && nodeSW == null);
    }

    @SuppressWarnings("rawtypes")
    private void build(QuadNode node) {
        this.boundary = node.getBoundary();
        this.nodeId = node.getPartitionId();

        if (node.getNodeNW() != null) {
            nodeNW = new QuadTreeModel();
            nodeNW.parentNode = this;
            nodeNW.build(node.getNodeNW());
        }
        if (node.getNodeNE() != null) {
            nodeNE = new QuadTreeModel();
            nodeNE.parentNode = this;
            nodeNE.build(node.getNodeNE());
        }
        if (node.getNodeSW() != null) {
            nodeSW = new QuadTreeModel();
            nodeSW.parentNode = this;
            nodeSW.build(node.getNodeSW());
        }
        if (node.getNodeSE() != null) {
            nodeSE = new QuadTreeModel();
            nodeSE.parentNode = this;
            nodeSE.build(node.getNodeSE());
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
    private String recursiveSearch(QuadTreeModel node, double x, double y) {
        // found the partition
        if (node.isLeaf()) {
            return node.getNodeId();
        }
        // tree search (point can only reside in one leaf node)
        if (node.getNodeNW().getBoundary().contains(x, y)) { // NW
            return recursiveSearch(node.getNodeNW(), x, y);
        } else if (node.getNodeNE().getBoundary().contains(x, y)) { // NE
            return recursiveSearch(node.getNodeNE(), x, y);
        } else if (node.getNodeSW().getBoundary().contains(x, y)) { // SW
            return recursiveSearch(node.getNodeSW(), x, y);
        } else {
            return recursiveSearch(node.getNodeSE(), x, y);  // SE
        }
    }

    @Override
    public HashSet<String> rangeSearch(SpatialObject obj) {
        HashSet<String> result = new HashSet<>();
        if (boundary.intersects(obj)) {
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
    private void recursiveRangeSearch(QuadTreeModel node, SpatialObject obj, HashSet<String> idList) {
        // found one partition
        if (node.isLeaf()) {
            idList.add(node.getNodeId());
        }
        // tree search
        if (node.getNodeNW().getBoundary().intersects(obj)) { // NW
            recursiveRangeSearch(node.getNodeNW(), obj, idList);
        }
        if (node.getNodeNE().getBoundary().intersects(obj)) { // NE
            recursiveRangeSearch(node.getNodeNE(), obj, idList);
        }
        if (node.getNodeSW().getBoundary().intersects(obj)) { // SW
            recursiveRangeSearch(node.getNodeSW(), obj, idList);
        }
        if (node.getNodeSE().getBoundary().intersects(obj)) { // SE
            recursiveRangeSearch(node.getNodeSE(), obj, idList);
        }
    }

    @Override
    public boolean isEmpty() {
        return isLeaf();
    }

    @Override
    public Rectangle get(final String index) {
        if (index == null || index.isEmpty()) {
            throw new IllegalArgumentException(
                    "QuadTree index must not be empty.");
        }
        // get nodes (first node is the root)
        String nodes[] = index.split("-");
        QuadTreeModel node = this;
        if (nodes.length == 1 && "r".equals(nodes[0])) {
            return node.getBoundary();
        }
        for (int i = 1; i < nodes.length; i++) {
            if (node.isLeaf()) {
                throw new IndexOutOfBoundsException(
                        "QuadTree index out of bounds.");
            }
            String dir = nodes[i];
            // quad search
            if ("nw".equals(dir)) {
                node = node.nodeNW;
            } else if ("ne".equals(dir)) {
                node = node.nodeNE;
            } else if ("sw".equals(dir)) {
                node = node.nodeSW;
            } else if ("se".equals(dir)) {
                node = node.nodeSE;
            } else {
                throw new IllegalArgumentException(
                        "Invalid QuadTree index.");
            }
        }

        return node.getBoundary();
    }

    /**
     * Number of leaf nodes in this tree.
     * <br> {@inheritDoc}}
     */
    @Override
    public int size() {
        return sizeRecursive(this);
    }

    /**
     * Recursively count the number of leaf nodes in this tree.
     *
     * @param node The current node to search (recursive).
     * @return Number of leaf nodes in the given node.
     */
    private int sizeRecursive(QuadTreeModel node) {
        if (node == null) {
            return 0;
        }
        if (node.isLeaf()) {
            return 1;
        }
        int sizenw = sizeRecursive(node.getNodeNW());
        int sizene = sizeRecursive(node.getNodeNE());
        int sizesw = sizeRecursive(node.getNodeSW());
        int sizese = sizeRecursive(node.getNodeSE());

        return (sizenw + sizene + sizesw + sizese);
    }

    @Override
    public void print() {
        println("[QuadTree]");
        printRecursive(this);
    }

    /**
     * Recursively print the nodes of this tree.
     *
     * @param node Current node (recursive).
     */
    private void printRecursive(QuadTreeModel node) {
        if (node == null) return;
        // print this node
        println(node.getNodeId());
        // print children, if any
        if (node.isLeaf()) return;
        printRecursive(node.nodeNW);
        printRecursive(node.nodeNE);
        printRecursive(node.nodeSW);
        printRecursive(node.nodeSE);
    }
}
