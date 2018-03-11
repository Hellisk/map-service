package traminer.util.spatial.structures.strips;

import com.google.common.collect.Lists;
import traminer.util.spatial.Axis;
import traminer.util.spatial.objects.Rectangle;
import traminer.util.spatial.objects.SpatialObject;
import traminer.util.spatial.objects.XYObject;
import traminer.util.spatial.structures.SpatialDataStructure;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * A Strips partitioning data structure made of [n] uniform strips.
 * <p>
 * Strips construction can be either Horizontal (X axis)
 * or Vertical (Y axis) defined by the StripsSplitPolicy.
 * <p> 
 * Strips index position ranges from i = [0...n-1].
 *
 * @author uqdalves
 *
 * @param <T> Type of spatial object to store in this partitioning. 
 * Objects must be inserted in a container object, XYObject<T>.
 */
@SuppressWarnings("serial")
public class UniformStrips<T extends SpatialObject> implements SpatialDataStructure<T> {
    /**
     * Whether or not to replicate boundary objects
     */
    protected static boolean IsReplicateBoundary = false;
    /**
     * The direction (X or Y axis) of the split
     */
    private final Axis splitAxis;
    /**
     * The spatial index model of this strips partitioning
     */
    private final UniformStripsModel stripsModel;
    /**
     * List of strips partitions
     */
    private final StripsPartition<T>[] partitions;

    /**
     * Create a new uniform strips partitioning with
     * [n] strips for the given dimensions.
     *
     * @param n         Number of strips.
     * @param splitAxis The direction (axis) to split.
     * @param minX      Lower-left X coordinate.
     * @param minY      Lower-left Y coordinate.
     * @param maxX      Upper-right X coordinate.
     * @param maxY      Upper-right Y coordinate.
     */
    @SuppressWarnings("unchecked")
    public UniformStrips(int n, Axis splitAxis,
                         double minX, double minY, double maxX, double maxY) {
        if (n <= 0) {
            throw new IllegalArgumentException("Strips dimension "
                    + "must be positive.");
        }
        if (splitAxis == null) {
            throw new NullPointerException("Axis cannot be null.");
        }
        this.splitAxis = splitAxis;
        // build model
        this.stripsModel = new UniformStripsModel(
                n, splitAxis, minX, minY, maxX, maxY);
        this.partitions = new StripsPartition[n];
    }

    /**
     * @return The strips split axis (X,Y).
     */
    public Axis getSplitAxis() {
        return splitAxis;
    }

    @Override
    public UniformStripsModel getModel() {
        return stripsModel;
    }

    /**
     * Get the partition in the given index position.
     * <br> Strips index ranges from [0...n-1].
     *
     * @param index
     * @return The partition in the given index position.
     */
    public StripsPartition<T> get(int index) {
        if (index < 0 || index >= partitions.length) {
            throw new IndexOutOfBoundsException(
                    "Strips index out of bounds");
        }
        return partitions[index];
    }

    @Override
    public List<StripsPartition<T>> getPartitions() {
        return Lists.newArrayList(partitions);
    }

    @Override
    public long count() {
        long count = 0;
        for (StripsPartition<T> strip : partitions) {
            count += strip.count();
        }
        return count;
    }

    @Override
    public boolean isEmpty() {
        if (partitions == null) {
            return true;
        }
        return (partitions.length == 0);
    }

    @Override
    public boolean insert(XYObject<T> obj) {
        // ignore objects not in this partitioning
        if (obj == null || !stripsModel.getBoundary().contains(obj.x(), obj.y())) {
            return false; // object cannot be added
        }
        String index = stripsModel.search(obj.x(), obj.y());
        int pos = stripsModel.getStripPosition(index);
        // create partition if it does not exist
        if (partitions[pos] == null) {
            partitions[pos] = new StripsPartition<>(index, stripsModel.get(pos));
        }
        return partitions[pos].insert(obj);
    }

    @Override
    public boolean remove(XYObject<T> obj) {
        // ignore object not in this partitioning
        if (obj == null || !stripsModel.getBoundary().contains(obj.x(), obj.y())) {
            return false; // object cannot be added
        }
        String index = stripsModel.search(obj.x(), obj.y());
        int pos = stripsModel.getStripPosition(index);
        // no partition with the object
        if (partitions[pos] == null) {
            return false;
        }
        return partitions[pos].remove(obj);
    }

    @Override
    public StripsPartition<T> partitionSearch(double x, double y) {
        if (stripsModel.getBoundary().contains(x, y)) {
            String index = stripsModel.search(x, y);
            int pos = stripsModel.getStripPosition(index);

            return partitions[pos];
        }
        return null; // didn't find anything
    }

    @Override
    public List<StripsPartition<T>> rangePartitionSearch(SpatialObject obj) {
        if (obj == null) {
            throw new NullPointerException("Spatial object cannot be null.");
        }
        HashSet<String> indexList = stripsModel.rangeSearch(obj);
        List<StripsPartition<T>> result = new ArrayList<>(indexList.size());
        for (String index : indexList) {
            // find the cell containing this partition
            int pos = stripsModel.getStripPosition(index);
            result.add(partitions[pos]);
        }
        return result;
    }

    /**
     * Return the adjacent partitions from the strips cell
     * containing the object in the (x,y) position (if any).
     *
     * @param x Object's X coordinate.
     * @param y Object's Y coordinate.
     * @return An adjacent strips index set, or an empty set
     * if the object is out of the boundaries of this index model,
     * or if there is no adjacent strips.
     */
    public List<StripsPartition<T>> adjacentPartitionSearch(double x, double y) {
        HashSet<String> indexList = stripsModel.adjacentSearch(x, y);
        List<StripsPartition<T>> result = new ArrayList<>(indexList.size());
        for (String index : indexList) {
            // find the cell containing this partition
            int pos = stripsModel.getStripPosition(index);
            result.add(partitions[pos]);
        }
        return result;
    }

    /**
     * Return the adjacent strips partitions from the
     * strip in the given position [i].
     *
     * @param i Strips index position ranges from i = [0...n-1]
     * @return A list of adjacent strips, or an empty set
     * if the cell has no adjacent cells.
     */
    public List<StripsPartition<T>> adjacentPartitionSearch(int i) {
        HashSet<String> indexList = stripsModel.adjacentSearch(i);
        List<StripsPartition<T>> result = new ArrayList<>(indexList.size());
        for (String index : indexList) {
            // find the cell containing this partition
            int pos = stripsModel.getStripPosition(index);
            result.add(partitions[pos]);
        }
        return result;
    }

    @Override
    public boolean isReplicateBoundary() {
        return UniformStrips.IsReplicateBoundary;
    }

    @Override
    public void setReplicateBoundary(boolean replicate) {
        UniformStrips.IsReplicateBoundary = replicate;
    }

    @Override
    public void print() {
        for (StripsPartition<T> node : getPartitions()) {
            String id = node.getPartitionId();
            println("[" + id + "]: " + count());
            Rectangle boundary = node.getBoundary();
            println("BOUNDARY " + boundary.toString());
            // print content
            for (XYObject<T> obj : node.getObjectsList()) {
                obj.print();
            }
            println();
        }
    }
}
