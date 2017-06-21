package traminer.util.spatial.structures;

import traminer.util.spatial.Axis;
import traminer.util.spatial.SpatialInterface;
import traminer.util.spatial.objects.Point;
import traminer.util.spatial.objects.SpatialObject;
import traminer.util.spatial.objects.XYObject;
import traminer.util.spatial.structures.grid.GridModel;
import traminer.util.spatial.structures.kdtree.KdTree;
import traminer.util.spatial.structures.kdtree.KdTreeModel;
import traminer.util.spatial.structures.quadtree.QuadTree;
import traminer.util.spatial.structures.quadtree.QuadTreeModel;
import traminer.util.spatial.structures.rtree.RTreeModel;
import traminer.util.spatial.structures.rtree.STRTree;
import traminer.util.spatial.structures.strips.UniformStripsModel;
import traminer.util.spatial.structures.voronoi.VoronoiDiagramModel;

import java.util.ArrayList;
import java.util.List;

/**
 * A Builder service to build spatial index models.
 * <p>
 * Builds a spatial index model for the given input dataset. For dynamic index
 * structures (e.g. KdTree, QuadTree, RTree), this service firstly partition the
 * dataset using the required data structure, and finally extracts the index
 * model from the partitioning.
 * <p>
 * The model boundaries can be set by calling the builder init() method.
 * The spatial model boundaries are initially set as:
 * lower-left corner (0,0), upper-right corner (100,100).
 *
 * @author uqdalves
 */
@SuppressWarnings("serial")
public final class SpatialModelBuilder implements SpatialInterface {
    /**
     * Spatial model boundaries
     */
    private static double minX = 0, maxX = 100;
    private static double minY = 0, maxY = 100;

    /**
     * Setup the spatial model boundaries.
     *
     * @param minX Lower-left X coordinate.
     * @param minY Lower-left Y coordinate.
     * @param maxX Upper-right X coordinate.
     * @param maxY Upper-right Y coordinate.
     */
    public static synchronized void init(
            final double minX, final double minY,
            final double maxX, final double maxY) {
        SpatialModelBuilder.minX = minX;
        SpatialModelBuilder.minY = minY;
        SpatialModelBuilder.maxX = maxX;
        SpatialModelBuilder.maxY = maxY;
    }

    /**
     * Builds a [n x m] static Grid model.
     *
     * @param n The grid X dimension, that is, the number of horizontal cells.
     * @param m The grid Y dimension, that is, the number of vertical cells.
     * @return A [n x m] Grid model.
     */
    public static GridModel buildGridModel(final int n, final int m) {
        // creates the partitioning model
        GridModel gridModel = new GridModel(n, m, minX, minY, maxX, maxY);

        return gridModel;
    }

    /**
     * Builds a Strips model with [n] uniform strips.
     *
     * @param n         The number of strips.
     * @param splitAxis The direction (X or Y axis) of the split.
     * @return A Strips model with [n] uniform strips.
     */
    public static UniformStripsModel buildUniformStripsModel(
            final int n, final Axis splitAxis) {
        // creates the partitioning model
        UniformStripsModel stripsModel = new UniformStripsModel(
                n, splitAxis, minX, minY, maxX, maxY);

        return stripsModel;
    }

    /**
     * Builds a k-d Tree model from the given list of XY Spatial Objects.
     *
     * @param xyObjList     The objects list used to build the tree.
     * @param nodesCapacity The k-d Tree nodes max capacity.
     * @return A k-d Tree model from the given list of spatial objects.
     */
    public static <T extends SpatialObject> KdTreeModel buildKdTreeModel(
            List<XYObject<T>> xyObjList, final int nodesCapacity) {
        // creates an empty k-d Tree
        KdTree<T> kdTree = new KdTree<T>(minX, minY, maxX, maxY, nodesCapacity);
        // build the partitioning
        kdTree.insertAll(xyObjList);
        // extract the model
        KdTreeModel kdModel = kdTree.getModel();

        return kdModel;
    }

    /**
     * Builds a QuadTree model from the given list of XY Spatial Objects.
     *
     * @param xyObjList     xyObjList The objects list used to build the tree.
     * @param nodesCapacity The Quadtree nodes max capacity.
     * @return A Quadtree model from the given list of spatial objects.
     */
    public static <T extends SpatialObject> QuadTreeModel buildQuadTreeModel(
            List<XYObject<T>> xyObjList, final int nodesCapacity) {
        // creates an empty QuadTree
        QuadTree<T> quadTree = new QuadTree<T>(minX, minY, maxX, maxY, nodesCapacity);
        // build the partitioning
        quadTree.insertAll(xyObjList);
        // extract the model
        QuadTreeModel quadModel = quadTree.getModel();

        return quadModel;
    }

    /**
     * Builds a R-Tree model using STR-Tree partitioning from the given list of XY
     * Spatial Objects.
     *
     * @param xyObjList     xyObjList The objects list used to build the tree.
     * @param nodesCapacity The STR-Tree nodes max capacity.
     * @return A STR-Tree model from the given list of Spatial Objects.
     */
    public static <T extends SpatialObject> RTreeModel buildSTRTreeModel(
            List<XYObject<T>> xyObjList, final int nodesCapacity) {
        // creates the STRTree
        STRTree<T> strTree = new STRTree<T>(xyObjList, nodesCapacity);
        // extract the model
        RTreeModel rtreeModel = strTree.getModel();

        return rtreeModel;
    }

    /**
     * Builds a Voronoi Diagram model using the given list of XY
     * spatial objects as pivots.
     *
     * @param xyObjList xyObjList The objects list used to build the Voronoi Diagram.
     * @return A STR-Tree model from the given list of Spatial Objects.
     */
    public static <T extends SpatialObject> VoronoiDiagramModel buildVoronoiDiagramModel(
            List<XYObject<T>> xyObjList) {
        // get the pivots
        List<Point> pivotsList = new ArrayList<>(xyObjList.size());
        for (XYObject<T> obj : xyObjList) {
            pivotsList.add(new Point(obj.x(), obj.y()));
        }
        // creates the Voronoi Diagram
        VoronoiDiagramModel voronoiDiagram = new VoronoiDiagramModel(pivotsList);

        return voronoiDiagram;
    }
}
