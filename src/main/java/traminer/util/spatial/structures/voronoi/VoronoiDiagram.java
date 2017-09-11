package traminer.util.spatial.structures.voronoi;

import traminer.util.spatial.objects.*;
import traminer.util.spatial.structures.SpatialDataStructure;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * A Voronoi Diagram spatial partitioning data structure.
 * This Voronoi Diagram can hold any type of spatial 
 * object in 2D.
 * 
 * @author uqdalves
 * 
 * @param <T> Type of spatial object to store in this partitioning. 
 * Objects must be inserted in a container object, XYObject<T>.
 */
@SuppressWarnings("serial")
public class VoronoiDiagram<T extends SpatialObject> implements SpatialDataStructure<T> {
    /**
     * Whether or not to replicate boundary objects
     */
    protected static boolean IsReplicateBoundary = false;
    /**
     * List of Voronoi partitions
     */
    private final List<VoronoiPartition<T>> partitionList;
    /**
     * The spatial index model of this diagram
     */
    private final VoronoiDiagramModel voronoiModel;

    /**
     * Creates a new a Voronoi Diagram data structure.
     *
     * @param generatorPivots The list of generator pivot points.
     */
    public VoronoiDiagram(List<Point> generatorPivots) {
        if (generatorPivots == null || generatorPivots.isEmpty()) {
            throw new IllegalArgumentException("List of pivots for "
                    + "Voronoi diagram construction cannot be empty nor null.");
        }
        // create the index model of this partitioning structure
        this.voronoiModel = new VoronoiDiagramModel(generatorPivots);
        this.partitionList = new ArrayList<>(voronoiModel.size());
    }

    @Override
    public boolean insert(XYObject<T> obj) {
        // ignore object not in this diagram
        if (obj == null || !voronoiModel.getBoundary().contains(obj.toPoint())) {
            return false; // object cannot be added
        }
        int index = Integer.parseInt(voronoiModel.search(obj.x(), obj.y()));
        partitionList.get(index).insert(obj);
        return true;
    }

    @Override
    public boolean remove(XYObject<T> obj) {
        // ignore object not in this diagram
        if (obj == null || !voronoiModel.getBoundary().contains(obj.toPoint())) {
            return false; // object is not here
        }
        int index = Integer.parseInt(voronoiModel.search(obj.x(), obj.y()));
        return partitionList.get(index).remove(obj);
    }

    /**
     * @param index
     * @return The Voronoi partition with the given index.
     */
    public VoronoiPartition<T> get(final int index) {
        if (index < 0 || index >= partitionList.size()) {
            throw new IllegalArgumentException(
                    "Voronoi Diagram index out of bounds.");
        }
        return partitionList.get(index);
    }

    /**
     * @param index
     * @return The Voronoi partition with the given index.
     */
    public VoronoiPartition<T> get(final String index) {
        if (index == null) {
            throw new NullPointerException(
                    "Voronoi Diagram index cannot be null.");
        }
        try {
            int i = Integer.parseInt(index);
            return get(i);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Voronoi diagram index "
                    + "format is not valid.");
        }
    }

    @Override
    public List<VoronoiPartition<T>> getPartitions() {
        return partitionList;
    }

    @Override
    public VoronoiPartition<T> partitionSearch(double x, double y) {
        String index = voronoiModel.search(x, y);
        if (index == null) {
            return null;
        }
        return partitionList.get(Integer.parseInt(index));
    }

    /**
     * Retrieve all Voronoi partitions that intersect with this
     * point object (object may be a border point).
     *
     * @param p The point to do the search.
     * @return List of Voronoi partitions intersecting with the
     * given point.
     */
    public List<VoronoiPartition<T>> rangePartitionSearch(Point p) {
        if (p == null) {
            throw new NullPointerException("Point for Voronoi Diagram "
                    + "search cannot be null.");
        }
        HashSet<String> indexList = voronoiModel.rangeSearch(p);
        List<VoronoiPartition<T>> result = new
                ArrayList<>(indexList.size());
        for (String index : indexList) {
            result.add(partitionList.get(Integer.parseInt(index)));
        }
        return result;
    }

    /**
     * Retrieve all Voronoi partitions that intersect with this
     * rectangle region.
     *
     * @param r The rectangle region to do the search.
     * @return List of Voronoi partitions intersecting with the
     * given rectangle region.
     */
    public List<VoronoiPartition<T>> rangePartitionSearch(Rectangle r) {
        if (r == null) {
            throw new NullPointerException("Rectangle region for "
                    + "Voronoi Diagram search cannot be null.");
        }
        HashSet<String> indexList = voronoiModel.rangeSearch(r);
        List<VoronoiPartition<T>> result = new
                ArrayList<>(indexList.size());
        for (String index : indexList) {
            result.add(partitionList.get(Integer.parseInt(index)));
        }
        return result;
    }

    /**
     * Retrieve all Voronoi partitions that intersect with this
     * circle region.
     *
     * @param c The circle region to do the search.
     * @return List of Voronoi partitions intersecting with the
     * given circle region.
     */
    public List<VoronoiPartition<T>> rangePartitionSearch(Circle c) {
        if (c == null) {
            throw new NullPointerException("Circle region for "
                    + "Voronoi Diagram search cannot be null.");
        }
        HashSet<String> indexList = voronoiModel.rangeSearch(c);
        List<VoronoiPartition<T>> result = new
                ArrayList<>(indexList.size());
        for (String index : indexList) {
            result.add(partitionList.get(Integer.parseInt(index)));
        }
        return result;
    }

    /**
     * Retrieve all Voronoi partitions that intersect with this
     * line-string edges.
     *
     * @param ls The line-string to do the search.
     * @return List of Voronoi partitions intersecting with the
     * given line-string.
     */
    public List<VoronoiPartition<T>> rangePartitionSearch(LineString ls) {
        if (ls == null) {
            throw new NullPointerException("Line-String for "
                    + "Voronoi Diagram search cannot be null.");
        }
        HashSet<String> indexList = voronoiModel.rangeSearch(ls);
        List<VoronoiPartition<T>> result = new
                ArrayList<>(indexList.size());
        for (String index : indexList) {
            result.add(partitionList.get(Integer.parseInt(index)));
        }
        return result;
    }

    @Override
    public List<VoronoiPartition<T>> rangePartitionSearch(SpatialObject obj) {
        if (obj == null) {
            throw new NullPointerException("Spatial object for "
                    + "Voronoi Diagram search cannot be null.");
        }
        HashSet<String> indexList = voronoiModel.rangeSearch(obj);
        List<VoronoiPartition<T>> result = new
                ArrayList<>(indexList.size());
        for (String index : indexList) {
            result.add(partitionList.get(Integer.parseInt(index)));
        }
        return result;
    }

    /**
     * Retrieve all partitions adjacent from the Voronoi polygon
     * containing the object in the (x,y) position (if any).
     *
     * @param x Object's X coordinate.
     * @param y Object's Y coordinate.
     * @return A list of adjacent partitions, or an empty list
     * if the object is out of the boundaries of this Voronoi
     * diagram, or if there is no adjacent partitions.
     */
    public List<VoronoiPartition<T>> adjacentPartitionSearch(double x, double y) {
        HashSet<String> indexList = voronoiModel.adjacentSearch(x, y);
        List<VoronoiPartition<T>> result = new
                ArrayList<>(indexList.size());
        for (String index : indexList) {
            result.add(partitionList.get(Integer.parseInt(index)));
        }
        return result;
    }

    @Override
    public boolean isReplicateBoundary() {
        return IsReplicateBoundary;
    }

    @Override
    public void setReplicateBoundary(boolean replicate) {
        VoronoiDiagram.IsReplicateBoundary = replicate;
    }

    @Override
    public VoronoiDiagramModel getModel() {
        return voronoiModel;
    }

    @Override
    public long count() {
        long count = 0;
        for (VoronoiPartition<T> partition : partitionList) {
            count += partition.count();
        }
        return count;
    }

    @Override
    public boolean isEmpty() {
        if (partitionList == null) {
            return true;
        }
        return partitionList.isEmpty();
    }

    @Override
    public void print() {
        for (VoronoiPartition<T> node : getPartitions()) {
            String id = node.getPartitionId();
            println("[" + id + "]: " + count());
            VoronoiPolygon boundary = node.getBoundary();
            println("BOUNDARY " + boundary.toString());
            // print content
            for (XYObject<T> obj : node.getObjectsList()) {
                obj.print();
            }
            println("");
        }
    }

}
