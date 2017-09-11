package traminer.util.map.matching;

import traminer.util.Pair;
import traminer.util.map.roadnetwork.RoadNode;
import traminer.util.spatial.objects.st.STPoint;

import java.util.Comparator;

/** 
 * An immutable Point-to-Node match object for map-matching algorithms.
 * 
 * @author uqdalves
 */
@SuppressWarnings("serial")
public class PointNodePair extends Pair<STPoint, RoadNode> {
    /**
     * The distance between the point and the node.
     */
    private final double distance;

    /**
     * Creates a Point-to-Node match pair object.
     * Set default distance as zero.
     *
     * @param point The match point.
     * @param node  The match node.
     */
    public PointNodePair(STPoint point, RoadNode node) {
        super(point, node);
        this.distance = 0.0;
    }

    /**
     * Creates a Point-to-Node match pair object.
     *
     * @param point    The match point.
     * @param node     The match node.
     * @param distance The distance between the point and the node.
     */
    public PointNodePair(STPoint point, RoadNode node, double distance) {
        super(point, node);
        this.distance = distance;
    }

    /**
     * @return The match point.
     */
    public STPoint getPoint() {
        return _1();
    }

    /**
     * @return The match node.
     */
    public RoadNode getNode() {
        return _2();
    }

    /**
     * @return The distance between the point and the node.
     */
    public double getDistance() {
        return distance;
    }

    /**
     * A comparator to compare point-to-node pairs by point time-stamp
     * in ascending order.
     */
    public static final Comparator<PointNodePair> TIME_COMPARATOR =
            new Comparator<PointNodePair>() {
                @Override
                public int compare(PointNodePair o1, PointNodePair o2) {
                    long t1 = o1.getPoint().time();
                    long t2 = o2.getPoint().time();
                    if (t1 < t2) return -1;
                    if (t1 > t2) return 1;
                    return 0;
                }
            };

    /**
     * A comparator to compare point-to-node pairs by their distances
     * in ascending order.
     */
    public static final Comparator<PointNodePair> DISTANCE_COMPARATOR =
            new Comparator<PointNodePair>() {
                @Override
                public int compare(PointNodePair o1, PointNodePair o2) {
                    double d1 = o1.distance;
                    double d2 = o2.distance;
                    if (d1 < d2) return -1;
                    if (d1 > d2) return 1;
                    return 0;
                }
            };
}
