package traminer.util.spatial.structures.rtree;

import gnu.trove.procedure.TIntProcedure;
import net.sf.jsi.rtree.RTree;
import traminer.util.spatial.objects.Rectangle;
import traminer.util.spatial.objects.SpatialObject;
import traminer.util.spatial.structures.SpatialIndexModel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * A RTree index model.
 * <p>
 * This RTree model is build  either by loading a list of
 * rectangles, or by loading partitions from a RTree packing
 * algorithm (e.g. STRTree).
 * <p>
 * RTree using the JSI library based on the algorithm:
 * <br> Guttman, A. (1984). "R-Trees: A Dynamic Index Structure
 * for Spatial Searching". In ACM SIGMOD '84.
 *
 * @author uqdalves
 */
@SuppressWarnings("serial")
public class RTreeModel implements SpatialIndexModel {
    /**
     * The RTree leaf nodes
     */
    private final Rectangle[] leafNodes;
    /**
     * The boundaries of this tree
     */
    private Rectangle boundary;
    /**
     * The RTree index itself - using JSI library
     */
    private final RTree tree;

    /**
     * Build a RTree index model from a STR-Tree space partitioning.
     *
     * @param strTree The STR-Tree space partitioning to
     *                build the index from.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public RTreeModel(STRTree strTree) {
        if (strTree == null) {
            throw new NullPointerException("Spatial data structure for "
                    + "R-Tree model building cannot be null.");
        }
        if (strTree.isEmpty()) {
            throw new IllegalArgumentException("Spatial data structure for "
                    + "R-Tree model building cannot be empty.");
        }
        this.leafNodes = new Rectangle[strTree.getPartitions().size()];
        this.tree = new RTree();
        tree.init(null);
        // build the RTree model
        List<Rectangle> rectangles = new ArrayList<>();
        List<STRNode> strNodes = strTree.getPartitions();
        for (STRNode node : strNodes) {
            rectangles.add(node.getBoundary());
        }
        build(rectangles);
    }

    /**
     * Build a RTree index model from the given list of rectangles.
     *
     * @param rectangles The list of rectangles to build the RTree from.
     */
    public RTreeModel(List<Rectangle> rectangles) {
        if (rectangles == null || rectangles.isEmpty()) {
            throw new IllegalArgumentException("List of rectangles for "
                    + "RTree model building cannot be empty.");
        }
        this.leafNodes = new Rectangle[rectangles.size()];
        this.tree = new RTree();
        tree.init(null);
        // build the RTree model
        build(rectangles);
    }

    /**
     * Build the RTree index from the given rectangles.
     *
     * @param rectangles The list of rectangles to build the RTree from.
     */
    private void build(List<Rectangle> rectangles) {
        int index = 0;
        for (Rectangle r : rectangles) {
            leafNodes[index] = r;
            JSIRectangle jsiRectangle = new JSIRectangle(r);
            tree.add(jsiRectangle, index++);
        }
        // get tree boundary
        net.sf.jsi.Rectangle rec = tree.getBounds();
        this.boundary = new Rectangle(rec.minX, rec.minY, rec.maxX, rec.maxY);
    }

    @Override
    public String search(double x, double y) {
        if (!boundary.contains(x, y)) {
            return null; // didn't find anything
        }
        final int[] index = new int[1];
        tree.intersects(new JSIRectangle(x, y, x, y), new TIntProcedure() {
            @Override
            public boolean execute(int i) {
                index[0] = i;
                return false; // stop receiving results
            }
        });
        return ("r" + index[0]);
    }

    @Override
    public HashSet<String> rangeSearch(SpatialObject obj) {
        if (!boundary.contains(obj)) {
            return null; // didn't find anything
        }
        final HashSet<String> indexSet = new HashSet<>();
        Rectangle mbr = obj.mbr();
        tree.intersects(new JSIRectangle(mbr), new TIntProcedure() {
            @Override
            public boolean execute(int i) {
                indexSet.add("r" + i);
                return true; // continue receiving results
            }
        });
        return indexSet;
    }

    @Override
    public Rectangle get(final String index) {
        if (index == null || index.isEmpty()) {
            throw new IllegalArgumentException(
                    "RTree index must not be empty.");
        }
        try {
            int i = Integer.parseInt(index.replaceFirst("r", ""));
            if (i < 0 || i >= size()) {
                throw new IndexOutOfBoundsException("RTree index out of bounds.");
            }
            return leafNodes[i];
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("RTree index is in a wrong format.");
        }
    }

    @Override
    public Rectangle getBoundary() {
        return boundary;
    }

    @Override
    public int size() {
        if (tree == null) return 0;
        return tree.size();
    }

    @Override
    public boolean isEmpty() {
        if (tree == null) return true;
        return (tree.size() == 0);
    }

    @Override
    public void print() {
        println("[R-TREE]");
        int i = 0;
        for (Rectangle node : leafNodes) {
            println("NODE[" + i++ + "]: " + node.toString());
        }
    }

    /**
     * Auxiliary JSI Rectangle object.
     */
    private class JSIRectangle extends net.sf.jsi.Rectangle implements Serializable {
        /**
         * Creates an instance of a JSI rectangle from the given rectangle.
         *
         * @param r
         */
        public JSIRectangle(Rectangle r) {
            super();
            float x1 = (float) r.minX();
            float y1 = (float) r.minY();
            float x2 = (float) r.maxX();
            float y2 = (float) r.maxY();
            this.set(x1, y1, x2, y2);
        }

        /**
         * Creates a a JSI rectangle with the given dimensions.
         *
         * @param minX Lower-left X coordinate.
         * @param minY Lower-left Y coordinate.
         * @param maxX Upper-right X coordinate.
         * @param maxY Upper-right Y coordinate.
         */
        public JSIRectangle(double minX, double minY, double maxX, double maxY) {
            super((float) minX, (float) minY, (float) maxX, (float) maxY);
        }
    }

}
