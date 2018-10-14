package mapupdate.util.dijkstra;

import java.util.HashMap;

public class MinPriorityQueue {


    private Item root;
    private int heapSize;
    private HashMap<Integer, Item> insertedNodes;     // the index and the corresponding node
    private Item lastItem;

    MinPriorityQueue() {
        this.root = null;
        this.heapSize = 0;
        this.insertedNodes = new HashMap<>();
        this.lastItem = null;
    }

    /**
     * extract the index of the unvisited node with the closest distance to source
     *
     * @return the index of the next node, returns -1 if all points are visited
     */
    int extractMin() {
//        double distance = root.distance;
        if (this.heapSize > 1) {
            int index = root.arrayIndex;
            Item tempPointer = this.lastItem;
            this.lastItem = this.lastItem.lastNode;
            this.lastItem.nextNode = null;
            swap(root, tempPointer, false);
            this.root = tempPointer;
            heapSize--;
            sink(root);
            insertedNodes.remove(index);
            return index;
        } else if (this.heapSize == 1) {
            int index = root.arrayIndex;
            this.root = null;
            this.lastItem = null;
            heapSize--;
            insertedNodes.remove(index);
            return index;
        } else return -1;
    }

    public boolean isEmpty() {
        return heapSize == 0;
    }

    /**
     * Update existing min heap item or insert a new update.
     *
     * @param index    the index of the currently updating node
     * @param distance the current calculated distance
     * @return false if the distance is not smaller than the previous one, otherwise true
     */
    boolean decreaseKey(int index, double distance) {
        if (insertedNodes.containsKey(index)) {   // the coming key already exists
            Item queryNode = insertedNodes.get(index);
            if (distance < queryNode.distance) {  // the update is valid
                queryNode.distance = distance;
                if (queryNode.distance < queryNode.parent.distance && this.lastItem == queryNode)
                    this.lastItem = queryNode.parent;
                while (queryNode.distance < queryNode.parent.distance) {
                    swap(queryNode.parent, queryNode, true);
                }
                return true;
            } else return false;
        } else {    // insert new node to the list
            Item insertPoint = new Item(distance, index);
            if (heapSize == 0) {
                insertPoint.parent = insertPoint;
                this.root = insertPoint;
                this.lastItem = insertPoint;
                this.insertedNodes.put(insertPoint.arrayIndex, insertPoint);
                this.heapSize = 1;
            } else insert(insertPoint);
            return true;
        }
    }

    private void insert(Item newPoint) {
        if (this.heapSize != 0) {
            if ((this.heapSize + 1) % 2 == 0 && isPowerOfTwo((this.heapSize + 1) / 2)) {  //the current tree is full
                Item currRoot = this.root;
                while (currRoot.leftChild != null) {
                    currRoot = currRoot.leftChild;
                }
                currRoot.leftChild = newPoint;
                newPoint.parent = currRoot;
            } else {
                insertIntoSubTree(newPoint, this.root);
            }
            // append the newPoint to the end of the tree
            newPoint.lastNode = this.lastItem;
            this.lastItem.nextNode = newPoint;
            this.lastItem = newPoint;
            // when the newPoint is at the end of the tree, the swap requires to update this.lastItem
            if (newPoint.distance < newPoint.parent.distance)
                this.lastItem = newPoint.parent;
            // the rest swaps are normal
            while (newPoint.distance < newPoint.parent.distance) {
                swap(newPoint.parent, newPoint, true);
            }
        } else {
            this.root = newPoint;
            newPoint.parent = newPoint;
            this.lastItem = newPoint;
        }
        heapSize++;
        insertedNodes.put(newPoint.arrayIndex, newPoint);
    }

    private void insertIntoSubTree(Item newPoint, Item treeNode) {
        if (treeNode.leftChild != null && treeNode.rightChild != null) {
            if (isSameDepth(treeNode.leftChild, treeNode.rightChild))
                if (!isComplete(treeNode.rightChild))
                    insertIntoSubTree(newPoint, treeNode.rightChild);
                else
                    insertIntoSubTree(newPoint, treeNode.leftChild);
            else if (isComplete(treeNode.leftChild))
                insertIntoSubTree(newPoint, treeNode.rightChild);
            else insertIntoSubTree(newPoint, treeNode.leftChild);
        } else {
            if (treeNode.leftChild == null) {
                treeNode.leftChild = newPoint;
                newPoint.parent = treeNode;
            } else {
                treeNode.rightChild = newPoint;
                newPoint.parent = treeNode;
            }
        }
    }

    private boolean isComplete(Item treeNode) {
        if (treeNode.leftChild != null && treeNode.rightChild != null) {
            return isComplete(treeNode.leftChild) && isComplete(treeNode.rightChild) && isSameDepth(treeNode.leftChild, treeNode
                    .rightChild);
        } else return treeNode.leftChild == null;
    }

    /**
     * validate the depth of each branch, only apply when both branches are balanced
     *
     * @param branch1   The first item
     * @param branch2   The second item
     * @return True if they are in the same depth, otherwise false
     */
    private boolean isSameDepth(Item branch1, Item branch2) {
        while (branch1.leftChild != null) {
            if (branch2.leftChild != null) {
                branch1 = branch1.leftChild;
                branch2 = branch2.leftChild;
            } else return false;
        }
        return branch2.leftChild == null;
    }


    /**
     * swap the nodes
     *
     * @param item1 item with higher level
     * @param item2 item with lower level
     */
    private void swap(Item item1, Item item2, boolean parent) {
        Item tempPointer;
        if (parent) {
            if (item1.leftChild == item2) {
                item1.leftChild = item2.leftChild;
                if (item1.leftChild != null)
                    item1.leftChild.parent = item1;
                item2.leftChild = item1;
                tempPointer = item1.rightChild;
                item1.rightChild = item2.rightChild;
                if (item1.rightChild != null)
                    item1.rightChild.parent = item1;
                item2.rightChild = tempPointer;
                if (item2.rightChild != null)
                    item2.rightChild.parent = item2;
                if (item1.parent != item1) {
                    item2.parent = item1.parent;
                    if (item2.parent.leftChild == item1)
                        item2.parent.leftChild = item2;
                    else item2.parent.rightChild = item2;
                } else {
                    this.root = item2;
                    item2.parent = item2;
                }
                item1.parent = item2;
            } else if (item1.rightChild == item2) {
                item1.rightChild = item2.rightChild;
                if (item1.rightChild != null)
                    item1.rightChild.parent = item1;
                item2.rightChild = item1;
                tempPointer = item1.leftChild;
                item1.leftChild = item2.leftChild;
                if (item1.leftChild != null)
                    item1.leftChild.parent = item1;
                item2.leftChild = tempPointer;
                if (item2.leftChild != null)
                    item2.leftChild.parent = item2;
                if (item1.parent != item1) {
                    item2.parent = item1.parent;
                    if (item2.parent.leftChild == item1)
                        item2.parent.leftChild = item2;
                    else item2.parent.rightChild = item2;
                } else {
                    this.root = item2;
                    item2.parent = item2;
                }
                item1.parent = item2;
            }

            if (item1.nextNode == item2) {
                item1.nextNode = item2.nextNode;
                if (item2.nextNode != null)
                    item1.nextNode.lastNode = item1;
                item2.nextNode = item1;
                item2.lastNode = item1.lastNode;
                if (item2.lastNode != null)
                    item2.lastNode.nextNode = item2;
                item1.lastNode = item2;
            } else {
                tempPointer = item1.nextNode;
                item1.nextNode = item2.nextNode;
                if (item1.nextNode != null)
                    item1.nextNode.lastNode = item1;
                item2.nextNode = tempPointer;
                if (item2.nextNode != null)
                    item2.nextNode.lastNode = item2;
                tempPointer = item1.lastNode;
                item1.lastNode = item2.lastNode;
                if (item1.lastNode != null)
                    item1.lastNode.nextNode = item1;
                item2.lastNode = tempPointer;
                if (item2.lastNode != null)
                    item2.lastNode.nextNode = item2;
            }

        } else {     // swap happens in extractMin
            // clean up the links in item2
            if (item2.parent.leftChild == item2)
                item2.parent.leftChild = null;
            else item2.parent.rightChild = null;
            item2.parent = null;
            if (item2.leftChild != null || item2.rightChild != null || item2.nextNode != null)
                throw new NullPointerException("ERROR! The swapping item should be at the end of the tree");
            if (item1.leftChild != item2 && item1.leftChild != null) {
                item2.leftChild = item1.leftChild;
                item2.leftChild.parent = item2;
            } else item2.leftChild = null;
            if (item1.rightChild != item2 && item1.rightChild != null) {
                item2.rightChild = item1.rightChild;
                item2.rightChild.parent = item2;
            } else item2.rightChild = null;

            item2.parent = item2;
            this.root = item2;

            if (item1.nextNode == item2 || item1.nextNode == null)
                item2.nextNode = null;
            else {
                item2.nextNode = item1.nextNode;
                item2.nextNode.lastNode = item2;
            }
            item1.nextNode = null;

            if (item1.lastNode != null)
                throw new NullPointerException("ERROR! Root node has previous node");

            item2.lastNode = null;
        }
    }

    private void sink(Item node) {
        Item smallestIndex = node;
        Item left = node.leftChild;
        Item right = node.rightChild;
        if (left != null && left.distance < smallestIndex.distance) {
            smallestIndex = left;
        }
        if (right != null && right.distance < smallestIndex.distance) {
            smallestIndex = right;
        }
        if (node != smallestIndex) {
            if (this.lastItem == smallestIndex)
                this.lastItem = node;
            swap(node, smallestIndex, true);
            sink(node);
        }
    }

//    public double min() {
//        return root.distance;
//    }

    private boolean isPowerOfTwo(int x) {
        return x > 0 & (x & (x - 1)) == 0;
    }

    private class Item {

        private double distance;
        private int arrayIndex;
        private Item parent;
        private Item leftChild;
        private Item rightChild;
        private Item nextNode;  // the node which located in the right of the current
        private Item lastNode;  // the node which located in the left of the current

        Item(double distance, int arrayIndex) {
            this.distance = distance;
            this.arrayIndex = arrayIndex;
            this.leftChild = null;
            this.rightChild = null;
            this.lastNode = null;
            this.nextNode = null;
        }
    }
}
