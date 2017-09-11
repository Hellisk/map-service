package traminer.util.spatial.comparators;

import traminer.util.spatial.objects.Point;

/**
 * Comparator to compare point objects (2D points) in clockwise order
 * based on a referential point (clock center).
 * <p>
 * Order starts from the Y+ X+ axis.
 * 
 * @author uqdalves
 */
@SuppressWarnings("serial")
public class ClockwiseComparator<T extends Point> implements SpatialComparator<T> {
    /**
     * Referential point - "clock center"
     */
    private final Point center;

    /**
     * Creates an empty comparator with default
     * 'clock center' set as the origin point (0, 0).
     */
    public ClockwiseComparator() {
        this.center = new Point(0.0, 0.0);
    }

    /**
     * Creates a Clockwise Comparator with the given
     * point as referential.
     *
     * @param center The 'clock center' point as referential.
     */
    public ClockwiseComparator(Point center) {
        this.center = center;
    }

    /**
     * Creates a Clockwise Comparator with the given
     * (x,y) coordinate as referential.
     *
     * @param x X coordinate referential.
     * @param y Y coordinate referential.
     */
    public ClockwiseComparator(double x, double y) {
        this.center = new Point(x, y);
    }

    @Override
    public int compare(Point p1, Point p2) {
        return compare(p1.x(), p1.y(), p2.x(), p2.y());
    }

    /**
     * Compare points in clockwise order from the center.
     * Points given by X and Y coordinates. Order starts
     * from the Y+ X+ axis.
     *
     * @param x1
     * @param y1
     * @param x2
     * @param y2
     * @return a negative integer, zero, or a positive integer as the first
     * argument is less than, equal to, or greater than the second.
     */
    public int compare(double x1, double y1, double x2, double y2) {
        if (x1 - center.x() >= 0 && x2 - center.x() < 0)
            return -1;
        if (x1 - center.x() < 0 && x2 - center.x() >= 0)
            return 1;
        if (x1 - center.x() == 0 && x2 - center.x() == 0) {
            if (y1 - center.y() >= 0 || y2 - center.y() >= 0)
                return (y1 > y2 ? -1 : 1);
            return (y2 > y1 ? 1 : -1);
        }

        // compute the cross product of vectors (center -> p1) x (center -> p2)
        double det = (x1 - center.x()) * (y2 - center.y()) -
                (x2 - center.x()) * (y1 - center.y());

        if (det < 0.0) return -1;
        if (det > 0.0) return 1;

        // points a and b are on the same line from the center
        // check which point is closer to the center
        double d1 = (x1 - center.x()) * (x1 - center.x()) +
                (y1 - center.y()) * (y1 - center.y());
        double d2 = (x2 - center.x()) * (x2 - center.x()) +
                (y2 - center.y()) * (y2 - center.y());

        return (d1 > d2 ? -1 : 1);
    }
}
