package traminer.util.spatial.structures.grid;

import traminer.util.spatial.objects.Rectangle;
import traminer.util.spatial.objects.SpatialObject;
import traminer.util.spatial.objects.XYObject;
import traminer.util.spatial.structures.SpatialDataStructure;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * A grid partition data structure made of n x m cells.
 * This Grid can hold any type of spatial object in 2D.
 * <p>
 * The grid is constructed from left to right,
 * bottom up. The first position in the grid
 * is the position index zero 0=[i,j]=[0,0].
 *
 * @param <T> Type of spatial object to store in this Grid.
 * @author uqdalves
 */
@SuppressWarnings("serial")
public class Grid<T extends SpatialObject> implements SpatialDataStructure<T> {
    /**
     * Whether or not to replicate boundary objects
     */
    protected static boolean IsReplicateBoundary = false;
    /**
     * Matrix of grid cells partitions
     */
    private final GridPartition<T>[][] partitions;
    /**
     * The spatial index model of this grid
     */
    private final GridModel gridModel;

    /**
     * Create a new static grid partitioning of [n x m] cells with
     * the given dimensions.
     *
     * @param n    The number of horizontal cells (x).
     * @param m    The number of vertical cells (y).
     * @param minX Lower-left X coordinate.
     * @param minY Lower-left Y coordinate.
     * @param maxX Upper-right X coordinate.
     * @param maxY Upper-right Y coordinate.
     */
    @SuppressWarnings("unchecked")
    public Grid(int n, int m, double minX, double minY, double maxX, double maxY) {
        if (n <= 0 || m <= 0) {
            throw new IllegalArgumentException("Grid dimensions "
                    + "must be positive.");
        }
        // create the index model of this partitioning structure
        this.gridModel = new GridModel(n, m, minX, minY, maxX, maxY);
        this.partitions = new GridPartition[n][m];
    }

    /**
     * Return the partition in the position [i,j]
     * in the grid. Grid i and j position start
     * from [0,0].
     *
     * @param i cell position in the horizontal axis (x).
     * @param j cell position in the vertical axis (y).
     * @return The partition in the position [i,j]
     * in the grid.
     */
    public GridPartition<T> get(int i, int j) {
        if (i < 0 || i >= gridModel.sizeX() ||
                j < 0 || j >= gridModel.sizeY()) {
            throw new IndexOutOfBoundsException(
                    "Grid index out of bounds.");
        }
        return partitions[i][j];
    }

    @Override
    public long count() {
        long count = 0;
        for (GridPartition<T>[] row : partitions) {
            for (GridPartition<T> cell : row) {
                if (cell != null) count += cell.count();
            }
        }
        return count;
    }

    @Override
    public boolean isEmpty() {
        if (partitions == null) return true;
        return (partitions.length == 0);
    }

    @Override
    public boolean isReplicateBoundary() {
        return IsReplicateBoundary;
    }

    @Override
    public void setReplicateBoundary(boolean replicateBoundary) {
        Grid.IsReplicateBoundary = replicateBoundary;
    }

    @Override
    public List<GridPartition<T>> getPartitions() {
        List<GridPartition<T>> result = new
                ArrayList<>(gridModel.size());
        for (GridPartition<T>[] row : partitions) {
            for (GridPartition<T> cell : row) {
                if (cell != null) result.add(cell);
            }
        }
        return result;
    }

    @Override
    public GridModel getModel() {
        return gridModel;
    }

    @Override
    public boolean insert(XYObject<T> obj) {
        // ignore object not in this grid
        if (obj == null || !gridModel.getBoundary().contains(obj.x(), obj.y())) {
            return false; // object cannot be added
        }
        // find the cell to add this object
        String index = gridModel.search(obj.x(), obj.y());
        int pos[] = gridModel.getCellPosition(index);
        int i = pos[0], j = pos[1];

        // create partition if it does not exist
        if (partitions[i][j] == null) {
            partitions[i][j] = new GridPartition<>(index, gridModel.get(i, j));
        }
        return partitions[i][j].insert(obj);
    }

    @Override
    public boolean remove(XYObject<T> obj) {
        // ignore object not in this grid
        if (obj == null || !gridModel.getBoundary().contains(obj.x(), obj.y())) {
            return false; // object is not here
        }
        // find the cell to remove this object from
        String index = gridModel.search(obj.x(), obj.y());
        int pos[] = gridModel.getCellPosition(index);
        int i = pos[0], j = pos[1];
        // no partition with the object
        if (partitions[i][j] == null) {
            return false;
        }
        return partitions[i][j].remove(obj);
    }

    @Override
    public GridPartition<T> partitionSearch(double x, double y) {
        if (gridModel.getBoundary().contains(x, y)) {
            // find the cell containing (x,y)
            String index = gridModel.search(x, y);
            int pos[] = gridModel.getCellPosition(index);

//            if (partitions[pos[0]][pos[1]] == null) {
//                System.err.println("Error partition" + pos[0] + "," + pos[1]);
//            }
            return partitions[pos[0]][pos[1]];
        }

        return null; // didn't find anything
    }

    @Override
    public List<GridPartition<T>> rangePartitionSearch(SpatialObject obj) {
        if (obj == null) {
            throw new NullPointerException("Spatial object cannot be null.");
        }
        HashSet<String> indexList = gridModel.rangeSearch(obj);
        // TODO report change, delete the size limit for result
        List<GridPartition<T>> result = new ArrayList<>();
        for (String index : indexList) {
            // find the cell containing this partition
            int pos[] = gridModel.getCellPosition(index);

            // TODO report change, partition can be null if no elements there.
            if (!(partitions[pos[0]][pos[1]] == null)) {
                result.add(partitions[pos[0]][pos[1]]);
            }
        }
        return result;
    }

    /**
     * Return the adjacent partitions from the partition
     * containing the object in the (x,y) position (if any).
     *
     * @param x Object's X coordinate.
     * @param y Object's Y coordinate.
     * @return A list of adjacent partitions, or an empty list
     * if the object is out of the boundaries of this grid,
     * or if there is no adjacent partitions.
     */
    public List<GridPartition<T>> adjacentPartitionSearch(double x, double y) {
        HashSet<String> indexList = gridModel.adjacentSearch(x, y);
        List<GridPartition<T>> result = new ArrayList<>(indexList.size());
        for (String index : indexList) {
            // find the cell containing this partition
            int pos[] = gridModel.getCellPosition(index);
            result.add(partitions[pos[0]][pos[1]]);
        }
        return result;
    }

    /**
     * Return the adjacent partitions from the given
     * grid position [i,j] (if any).
     *
     * @param i cell position in the horizontal axis (x).
     * @param j cell position in the vertical axis (y).
     * @return An adjacent partitions list, or an empty list
     * if there is no adjacent partition.
     */
    public List<GridPartition<T>> adjacentPartitionSearch(int i, int j) {
        HashSet<String> indexList = gridModel.adjacentSearch(i, j);
        List<GridPartition<T>> result = new ArrayList<>(indexList.size());
        for (String index : indexList) {
            // find the cell containing this partition
            int pos[] = gridModel.getCellPosition(index);
            result.add(partitions[pos[0]][pos[1]]);
        }
        return result;
    }

    @Override
    public void print() {
        for (GridPartition<T> cell : getPartitions()) {
            String id = cell.getPartitionId();
            println("[" + id + "]: " + cell.count());
            Rectangle boundary = cell.getBoundary();
            println("BOUNDARY " + boundary.toString());
            // print content
            for (XYObject<T> obj : cell.getObjectsList()) {
                obj.print();
            }
            println();
        }
    }
}
