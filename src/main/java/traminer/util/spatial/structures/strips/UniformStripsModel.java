package traminer.util.spatial.structures.strips;

import traminer.util.spatial.Axis;
import traminer.util.spatial.objects.Rectangle;
import traminer.util.spatial.objects.SpatialObject;
import traminer.util.spatial.structures.SpatialIndexModel;

import java.util.HashSet;

/**
 * A 2D strips index model made of n uniform strips cells.
 * This model uniformly splits the space into n strips of
 * same dimensions.
 * <p>
 * Strips construction can be either Horizontal (X axis)
 * or Vertical (Y axis) defined by the StripsSplitPolicy.
 * <p>
 * Strips index position ranges from i = [0...n-1].
 *
 * @author uqdalves
 */
@SuppressWarnings("serial")
public class UniformStripsModel implements SpatialIndexModel {
    /**
     * The strips cells list
     */
    private final Rectangle[] strips;
    /**
     * The boundaries of this strips diagram
     */
    private final Rectangle boundary;
    /**
     * The direction (X or Y axis) of the split
     */
    private final Axis splitAxis;

    /**
     * Create a new uniform strips model with
     * [n] strips for the given dimensions.
     *
     * @param n         Number of splits.
     * @param splitAxis The direction (X or Y axis) of the split.
     */
    public UniformStripsModel(int n, Axis splitAxis,
                              double minX, double minY, double maxX, double maxY) {
        if (n <= 0) {
            throw new IllegalArgumentException("Strips dimension "
                    + "must be positive.");
        }
        if (splitAxis == null) {
            throw new NullPointerException("Axis cannot be null.");
        }
        this.splitAxis = splitAxis;
        this.boundary = new Rectangle(minX, minY, maxX, maxY);
        this.strips = new Rectangle[n];
        // build the model
        build();
    }

    /**
     * Build the model for a uniform partitioning.
     * Generates the strips boundaries.
     */
    private void build() {
        // axis increments
        double incr = 0;
        double current = 0;
        String stripId = "";
        // split the X axis
        int size = strips.length;
        if (splitAxis.equals(Axis.X)) {
            incr = (boundary.maxX() - boundary.minX()) / size;
            current = boundary.minX();
            for (int i = 0; i < size; i++) {
                stripId = "sx" + i;
                Rectangle strip = new Rectangle(
                        current, boundary.minY(),
                        current + incr, boundary.maxY());
                strip.setId(stripId);
                strips[i] = strip;
                current += incr;
            }
        } else
            // split the Y axis
            if (splitAxis.equals(Axis.Y)) {
                incr = (boundary.maxY() - boundary.minY()) / size;
                current = boundary.minY();
                for (int i = 0; i < size; i++) {
                    stripId = "sy" + i;
                    Rectangle strip = new Rectangle(
                            boundary.minX(), current,
                            boundary.maxX(), current + incr);
                    strip.setId(stripId);
                    strips[i] = strip;
                    current += incr;
                }
            }
    }

    /**
     * @return The list of all strips cells in this model.
     */
    public Rectangle[] getStrips() {
        return strips;
    }

    @Override
    public String search(double x, double y) {
        if (!boundary.contains(x, y)) {
            return null;
        }
        int pos;
        double dim;
        String index = null;
        if (splitAxis.equals(Axis.X)) {
            dim = boundary.maxX() - boundary.minX(); // width
            pos = (int) ((x - boundary.minX()) / dim);
            if (x != boundary.minX() && x % dim == 0) pos--;
            index = "sx" + pos;
        } else if (splitAxis.equals(Axis.Y)) {
            dim = boundary.maxY() - boundary.minY(); // height
            pos = (int) ((y - boundary.minY()) / dim);
            if (y != boundary.minY() && y % dim == 0) pos--;
            index = "sy" + pos;
        }

        return index;
    }

    @Override
    public HashSet<String> rangeSearch(SpatialObject obj) {
        HashSet<String> posList = new HashSet<>();
        if (obj != null && boundary.intersects(obj)) {
            for (Rectangle strip : strips) {
                if (strip.intersects(obj)) {
                    posList.add(strip.getId());
                }
            }
        }
        return posList;
    }

    /**
     * Return the indexes of all strip cells adjacent from
     * the cell containing the object in the (x,y)
     * position (if any).
     *
     * @param x Object's X coordinate.
     * @param y Object's Y coordinate.
     * @return An adjacent strips index set, or an empty set
     * if the object is out of the boundaries of this index model,
     * or if there is no adjacent strips.
     */
    public HashSet<String> adjacentSearch(double x, double y) {
        String index = search(x, y);
        int pos = getStripPosition(index);
        return adjacentSearch(pos);
    }

    /**
     * Return the indexes of all strip cells adjacent
     * from the cell in the given position [i].
     *
     * @param index Strips index position ranges from i = [0...n-1]
     * @return An adjacent cells index set, or an empty set
     * if the cell has no adjacent cells.
     */
    public HashSet<String> adjacentSearch(final int i) {
        if (i < 0 || i >= strips.length) {
            throw new IndexOutOfBoundsException(
                    "Strips index out of bounds.");
        }
        HashSet<String> adjList = new HashSet<>();
        if (i > 0) {
            adjList.add("" + (i - 1));
        }
        if (i < strips.length - 1) {
            adjList.add("" + (i + 1));
        }
        return adjList;
    }

    /**
     * @param index The index of the strip cell to search.
     * @return Returns the position [i] in the Strips
     * of the cell in the given index.
     */
    public int getStripPosition(final String index) {
        try {
            // get strips cell position
            String pos = index.replaceAll("s|x|y", "");
            int i = Integer.parseInt(pos);
            return i;
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Strips index is invalid.");
        }
    }

    /**
     * @param index The index of the strip cell to return.
     * @return Return the strip boundary in the given index position.
     */
    public Rectangle get(int index) {
        if (index < 0 || index >= strips.length) {
            throw new IndexOutOfBoundsException(
                    "Strips index out of bounds.");
        }
        return strips[index];
    }

    @Override
    public Rectangle get(String index) {
        if (index == null || index.isEmpty()) {
            throw new IllegalArgumentException(
                    "Strips index must not be empty.");
        }
        int pos = getStripPosition(index);
        return get(pos);
    }

    @Override
    public Rectangle getBoundary() {
        return boundary;
    }

    @Override
    public int size() {
        if (strips == null) {
            return 0;
        }
        return strips.length;
    }

    @Override
    public boolean isEmpty() {
        return (strips == null || strips.length == 0);
    }

    @Override
    public void print() {
        int size = size();
        println("[STRIPS] [" + size + "]");
        Rectangle strip;
        for (int i = 0; i < size; i++) {
            strip = strips[i];
            System.out.format("%s:[(%.2f,%.2f)(%.2f,%.2f)]",
                    strip.getId(),
                    strip.minX(), strip.maxY(),
                    strip.maxX(), strip.maxY());
            if (splitAxis.equals(Axis.X)) {
                print(" ");
            } else if (splitAxis.equals(Axis.Y)) {
                println();
            }
        }
    }
}
