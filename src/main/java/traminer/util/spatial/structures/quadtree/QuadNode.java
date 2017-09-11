package traminer.util.spatial.structures.quadtree;

import traminer.util.spatial.objects.Rectangle;
import traminer.util.spatial.objects.SpatialObject;
import traminer.util.spatial.objects.XYObject;
import traminer.util.spatial.structures.SpatialPartition;

import java.util.ArrayList;

/**
 * A spatial partition representing a node in a QuadTree.
 * <p>
 * Each node is represented by its boundary rectangle, the node 
 * capacity, and four references to its children nodes (if any).
 * <p>
 * Each node contains the list of objects in the partition.
 * 
 * @author uqdalves
 *
 * @param <T> Type of spatial object to store in this node.
 */
@SuppressWarnings("serial")
public class QuadNode<T extends SpatialObject> extends SpatialPartition<XYObject<T>> {
    /**
     * Maximum objects capacity of all nodes
     */
    private static int maxCapacity = 10;
    /**
     * The spatial boundaries of this node/partition
     */
    private final Rectangle boundary;
    /**
     * The parent node/partition of this node
     */
    private QuadNode<T> parentNode = null;
    /**
     * North-West child node
     */
    private QuadNode<T> nodeNW = null;
    /**
     * North-East child node
     */
    private QuadNode<T> nodeNE = null;
    /** South-West child node */
    private QuadNode<T> nodeSW = null;
    /** South-East child node */
    private QuadNode<T> nodeSE = null;
    
    /**
     * Creates a new node with the given initial boundary,
     * and default max objects capacity = 10.
     *
     * @param nodeId The identifier/index of this node.
     * @param nodeBoundary The spatial boundary of this node.
     */
    public QuadNode(String nodeId, Rectangle nodeBoundary) {
        super(nodeId);
        if (nodeBoundary == null) {
            throw new NullPointerException("QuadTree node boundary cannot be null.");
        }
        this.boundary = nodeBoundary;
    }
    
    /**
     * @return The maximum objects capacity of the nodes.
     */
    public static int getMaxCapacity() {
        return maxCapacity;
    }

    /**
     * Set the maximum objects capacity of the nodes.
     *
     * @param maxCapacity
     */
    public static void setMaxCapacity(int maxCapacity) {
        QuadNode.maxCapacity = maxCapacity;
    }

    @Override
    public Rectangle getBoundary() {
        return boundary;
    }

    /**
     * @return The parent node/partition of this node.
     */
    public QuadNode<T> getParentNode() {
        return parentNode;
    }

    /**
     * Set the parent node/partition of this node.
     *
     * @param parentNode
     */
    public void setParentNode(QuadNode<T> parentNode) {
        this.parentNode = parentNode;
    }

    /**
     * @return The North-West child node of this node.
     */
    public QuadNode<T> getNodeNW() {
        return nodeNW;
    }

    /**
     * @return The North-East child node of this node.
     */
    public QuadNode<T> getNodeNE() {
        return nodeNE;
    }

    /**
     * @return The South-West child node of this node.
     */
    public QuadNode<T> getNodeSW() {
        return nodeSW;
    }

    /**
     * @return The South-East child node of this node.
     */
    public QuadNode<T> getNodeSE() {
        return nodeSE;
    }

    /**
     * Check whether this node is a leaf node.
     *
     * @return True if this node is a leaf node.
     */
    public boolean isLeaf() {
        return (nodeNW == null || nodeNE == null ||
                nodeSE == null || nodeSW ==null);
    }

    /**
     * Make this node a leaf node.
     * <br> Note: This method will remove 
     * all children nodes of this node.
     */
    public void setAsLeaf() {
        // let the gc do the job
        nodeNW = null;
        nodeNE = null;
        nodeSE = null;
        nodeSW = null;
    }

    @Override
    public boolean insert(XYObject<T> obj) {
        // Ignore objects that do not belong in this node
        if (obj == null || !boundary.contains(obj.x(), obj.y())) {
            return false; // object cannot be added
        }

        // leaf node
        if (isLeaf()) {
            // If there is space in this leaf node, add the object here
            if (count() < maxCapacity) {
                objectsList.add(obj);
                return true;
            }
            // leaf node is full, we need to subdivide it,
            // create children nodes
            else {
                subdivide();
            }
        }
        
        // then add the object to whichever node will accept it
        if (QuadTree.IsReplicateBoundary) {
            // multiple assignment
            insertIntoChildren(obj);
        } else{
            // single assignment
            insertIntoChild(obj);
        }
        
        return true;
    }
    /**
     * Insert the object in whichever child node accepts it.
     * Insert into one node only, if the object is a boundary
     * point, it will be added to the it first partition occurrence.
     * 
     * @param obj
     */
    private void insertIntoChild(XYObject<T> obj) {
        // try to insert into a child of this node
        if (nodeNW.insert(obj)) return;
        if (nodeNE.insert(obj)) return;
        if (nodeSW.insert(obj)) return;
        if (nodeSE.insert(obj)) return;
    }
    /**
     * Insert the object in whichever children nodes accept it.
     * Use multiple assignment of boundary points.
     * 
     * @param obj
     */
    private void insertIntoChildren(XYObject<T> obj) {
        nodeNW.insert(obj);
        nodeNE.insert(obj);
        nodeSW.insert(obj);
        nodeSE.insert(obj);
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
        if (obj == null || !boundary.contains(obj.x(), obj.y())){
            return false;
        }
        
        // if this object is in this node
        if (isLeaf() && objectsList.remove(obj)) return true;

        // if this node has children
        if (!isLeaf()) {
            // not found in any child
            if (!removeFromChildren(obj)) return false;

            // try to merge children of this node
            merge();

            return true;
        }

        return false;
    }
    /**
     * Remove the object from whichever child contains it.
     * 
     * @param obj
     * @return True if the object could be removed.
     */
    private boolean removeFromChildren(XYObject<T> obj) {
        // may live in multiple children.
        boolean resultNW = nodeNW.remove(obj);
        boolean resultNE = nodeNE.remove(obj);
        boolean resultSW = nodeSW.remove(obj);
        boolean resultSE = nodeSE.remove(obj);
        return (resultNW || resultNE || resultSW || resultSE);
    }

    /**
     * Subdivide this node partition into 4 children nodes.
     */
    private void subdivide() {
        double w = boundary.width()  / 2d;
        double h = boundary.height() / 2d;
        double minx = boundary.minX();
        double miny = boundary.minY();
        double maxx = boundary.maxX();
        double maxy = boundary.maxY();

        Rectangle xyNW = new Rectangle(minx, miny + h, minx+w, maxy);
        nodeNW = new QuadNode<T>(partitionId + "-nw", xyNW);
        nodeNW.setParentNode(this);

        Rectangle xyNE = new Rectangle(minx + w, miny + h, maxx, maxy);
        nodeNE = new QuadNode<T>(partitionId + "-ne", xyNE);
        nodeNE.setParentNode(this);

        Rectangle xySW = new Rectangle(minx, miny, minx+w, miny+h);
        nodeSW = new QuadNode<T>(partitionId + "-sw", xySW);
        nodeSW.setParentNode(this);

        Rectangle xySE = new Rectangle(minx + w, miny, maxx, miny+h);
        nodeSE = new QuadNode<T>(partitionId + "-se", xySE);
        nodeSE.setParentNode(this);

        // objects live in leaf nodes, so distribute
        if (QuadTree.IsReplicateBoundary) {
            // multiple assignment
            for (XYObject<T> obj : objectsList) {
                insertIntoChildren(obj);
            }
        } else {
            // single assignment
            for (XYObject<T> obj : objectsList) {
                insertIntoChild(obj);
            }
        }

        // empties this node
        clear();
    }

    /**
     * This method will merge children into self if it 
     * can without overflowing the maxCapacity of this node.
     * 
     * @return True if the children were successfully merged.
     */
    private boolean merge() {
        // if the children aren't leafs, you cannot merge
        if (!nodeNW.isLeaf() || !nodeNE.isLeaf() ||
                !nodeSW.isLeaf() || !nodeSE.isLeaf())
            return false;

        // children and leafs, see if you can remove point and merge into this node
        int nw = nodeNW.count();
        int ne = nodeNE.count();
        int sw = nodeSW.count();
        int se = nodeSE.count();
        int total = nw+ne+sw+se;

        // if all the children's objects can be merged into this node
        if (total < maxCapacity) {
            objectsList = new ArrayList<>();
            objectsList.addAll(nodeNW.getObjectsList());
            objectsList.addAll(nodeNE.getObjectsList());
            objectsList.addAll(nodeSW.getObjectsList());
            objectsList.addAll(nodeSE.getObjectsList());
            // remove all children nodes
            setAsLeaf();
        }
        
        return true;
    }
}
