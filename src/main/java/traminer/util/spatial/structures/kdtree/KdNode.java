package traminer.util.spatial.structures.kdtree;

import traminer.util.spatial.objects.Rectangle;
import traminer.util.spatial.objects.SpatialObject;
import traminer.util.spatial.objects.XYObject;
import traminer.util.spatial.structures.SpatialPartition;

import java.util.ArrayList;
import java.util.Collections;

/**
 * A spatial partition representing a node in a k-d Tree.
 * <p>
 * Each node is represented by its boundary rectangle, the node 
 * capacity, depth, and two references to its children nodes (if any).
 * <p>
 * Each node contains the list of objects in the partition.
 * 
 * @author uqdalves
 *
 * @param <T> Type of spatial object to store in this node.
 */
@SuppressWarnings("serial")
public class KdNode<T extends SpatialObject> extends SpatialPartition<XYObject<T>> {
    /**
     * Spatial dimension of this k-d tree
     */
    private static final int K = 2;    // 2D by default
    /**
     * Used to find in which axis this node is divided
     */
    private final int depth; // (even = X or odd = Y)
    /**
     * Used to find in which axis to divide the children nodes
     */
    private static final int X_AXIS = 0;
    private static final int Y_AXIS = 1;
    /**
     * Maximum objects capacity of all nodes
     */
    private static int maxCapacity = 10;
    /**
     * The spatial boundaries of this partition
     */
    private final Rectangle boundary;
    /**
     * The partition that originated this partition/node
     */
    private KdNode<T> parentNode = null;
    /** Left|bottom child partition/node */
    private KdNode<T> nodeInferior = null;
    /** Right|top child partition/node */
    private KdNode<T> nodeSuperior = null;

    /**
     * Creates a new node in the given tree depth, 
     * and given initial boundary.
     *
     * @param nodeId The id of this partition/node.
     * @param depth The depth of this node.
     * @param nodeBoundary The spatial boundaries of this partition/node.
     */
    public KdNode(String nodeId, int depth, Rectangle nodeBoundary) {
        super(nodeId);
        if (nodeBoundary == null) {
            throw new NullPointerException(
                    "k-d Tree node boundary cannot be null.");
        }
        if (depth < 0) {
            throw new IllegalArgumentException(
                    "Tree depth must be positve or zero.");
        }
        this.boundary = nodeBoundary;
        this.depth = depth;
    }
        
    /**
     * @return The partition that originated this partition/node.
     */
    public KdNode<T> getParentNode() {
        return parentNode;
    }

    /**
     * @param parentNode The partition that originated this partition/node.
     */
    public void setParentNode(KdNode<T> parentNode) {
        this.parentNode = parentNode;
    }

    /**
     * @return The left-most or bottom-most child node/partition.
     */
    public KdNode<T> getNodeInferior() {
        return nodeInferior;
    }

    /**
     * @return The right-most or top-most child node/partition.
     */
    public KdNode<T> getNodeSuperior() {
        return nodeSuperior;
    }

    /**
     * @return The depth of this node in the tree.
     */
    public int getDepth() {
        return depth;
    }

    /**
     * @return The maximum objects capacity of all nodes.
     */
    public static int getMaxCapacity() {
        return maxCapacity;
    }

    /**
     * @param maxCapacity The maximum objects capacity of all nodes.
     */
    public static void setMaxCapacity(int maxCapacity) {
        KdNode.maxCapacity = maxCapacity;
    }

    /**
     * Check whether this partition is a leaf node.
     *
     * @return True if this is a leaf partition/node.
     */
    public boolean isLeaf() {
        return (nodeInferior == null || nodeSuperior==null);
    }

    /**
     * Make this node a leaf node.
     * <br> Note: This method will remove
     * all children nodes of this partition.
     */
    public void setAsLeaf() {
        // let the gc do the job
        nodeInferior = null;
        nodeSuperior = null;
    }

    @Override
    public Rectangle getBoundary() {
        return boundary;
    }

    @Override
    public boolean insert(XYObject<T> obj) {
        // ignore objects that do not belong to this node
        if (obj == null || !boundary.contains(obj.x(), obj.y())) {
            return false; // object cannot be added here
        }

        // leaf node
        if (isLeaf()) {
            // if there is space in this leaf node, add the object here
            if (count() < maxCapacity) {
                objectsList.add(obj);
                return true;
            }
            // leaf node is full, we need to divide it
            // create children nodes
            else {
                subdivide();
            }
        }

        // then add the object to whichever child node accepts it
        if (KdTree.IsReplicateBoundary) {
            // multiple assignment
            insertIntoChildren(obj);
        } else {
            // single assignment
            insertIntoChild(obj);
        }
        
        return true;
    }

    /**
     * Insert the object in whichever child node accepts it.
     * Insert into one node only, if the object is a boundary
     * point, it will be added to the 'lesser' partition.
     *
     * @param obj The spatial object to add.
     */
    private void insertIntoChild(XYObject<T> obj) {
        // check in which child to add (only one)
        if (nodeInferior.insert(obj)) return;
        if (nodeSuperior.insert(obj)) return;
    }
    
    /**
     * Insert the object in whichever children nodes accept it.
     * Use multiple assignment of boundary points.
     * 
     * @param obj The spatial object to add.
     */
    private void insertIntoChildren(XYObject<T> obj) {
        nodeInferior.insert(obj);
        nodeSuperior.insert(obj);
    } 

    /**
     * {@inheritDoc}
     * <br>
     * This method will merge children into self if it 
     * can without overflowing the maxCapacity param.
     */
    @Override
    public boolean remove(XYObject<T> obj) {
        // if not in this node, don't do anything
        if (obj == null || !boundary.contains(obj.x(), obj.y())) {
            return false;
        }

        // if this object is in this node
        if (isLeaf()) {
            return objectsList.remove(obj);
        }

        // if this node has children
        if (!isLeaf()) {
            // not found in any children
            if (!removeFromChildren(obj)) {
                return false;
            } else {
                // try to merge children of this node
                merge();
                return true;
            }
        }

        return false;
    }

    /**
     * Remove the object from whichever child contains it.
     *
     * @param obj The spatial object to remove.
     * @return True is the object could be removed.
     */
    private boolean removeFromChildren(XYObject<T> obj) {
        // may live in all children
        boolean resultl = nodeInferior.remove(obj);
        boolean resultg = nodeSuperior.remove(obj);

        return (resultl || resultg);
    }

    /**
     * Divide this node into two balanced child nodes, 
     * that is, each child node has the same quantity 
     * of objects.
     */
    private void subdivide() {
        Rectangle infBoundary = null;
        Rectangle supBoundary = null;
        double minx = boundary.minX();
        double miny = boundary.minY();
        double maxx = boundary.maxX();
        double maxy = boundary.maxY();

        // check which axis to divide now
        int medianIndex = count() / 2;
        int axis = depth % K;
        String id = partitionId;
        if (axis == X_AXIS) {
            id += "-x";
            Collections.sort(objectsList, XYObject.X_COMPARATOR);
            // get the median object
            XYObject<T> median = objectsList.get(medianIndex);
            // left partition
            infBoundary = new Rectangle(minx, miny, median.x(), maxy);
            // right partition
            supBoundary = new Rectangle(median.x(), miny, maxx, maxy);
        } else if (axis == Y_AXIS) {
            id += "-y";
            Collections.sort(objectsList, XYObject.Y_COMPARATOR);
            // get the median object
            XYObject<T> median = objectsList.get(medianIndex);
            // bottom partition
            infBoundary = new Rectangle(minx, miny, maxx, median.y());
            // upper partition
            supBoundary = new Rectangle(minx, median.y(), maxx, maxy);
        }

        // create the child nodes
        nodeInferior = new KdNode<T>(id + "0", depth + 1, infBoundary);
        nodeInferior.setParentNode(this);
        nodeSuperior = new KdNode<T>(id + "1", depth + 1, supBoundary);
        nodeSuperior.setParentNode(this);

        // objects live in leaf nodes, so distribute
        if (KdTree.IsReplicateBoundary) {
            // multiple assignment
            for (XYObject<T> obj : objectsList) {
                insertIntoChildren(obj);
            }
        } else {
            // single assignment (simply halve)
            nodeInferior.objectsList.addAll(this.objectsList.subList(0, medianIndex + 1));
            nodeSuperior.objectsList.addAll(this.objectsList.subList(medianIndex + 1, count()));
        }

        // empties this node
        clear();
    }

    /**
     * This method will merge children into self if it 
     * can without overflowing the maxCapacity of this node.
     * 
     * @return True if the partition was successfully merged
     */
    private boolean merge() {
        // if the children aren't leafs, you cannot merge
        if (!nodeInferior.isLeaf() || !nodeSuperior.isLeaf()) 
            return false;

        // children and leafs, see if you can remove 
        // point and merge into this node
        int nl = nodeInferior.count();
        int ng = nodeSuperior.count();
        int total = nl + ng;

        // if all the children's objects can be merged into this node
        if (total < maxCapacity) {
            objectsList = new ArrayList<>();
            objectsList.addAll(nodeInferior.getObjectsList());
            objectsList.addAll(nodeSuperior.getObjectsList());
            // clear and null all child nodes of this node
            setAsLeaf();
        }
        
        return true;
    }
}
