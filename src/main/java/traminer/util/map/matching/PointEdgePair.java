package traminer.util.map.matching;

import traminer.util.Pair;
import traminer.util.spatial.objects.Segment;
import traminer.util.spatial.objects.st.STPoint;

/** 
 * An immutable Point-to-Edge match object for map-matching algorithms.
 * 
 * @author uqdalves
 */
@SuppressWarnings("serial")
public class PointEdgePair extends Pair<STPoint, Segment> {
    /**
     * The distance between the point and the edge
     */
    private final double distance;

    /**
     * Creates a Point-to-Edge match pair object.
     * Set default distance as zero.
     *
     * @param point The match point.
     * @param node  The match edge.
     */
    public PointEdgePair(STPoint point, Segment edge) {
        super(point, edge);
        this.distance = 0.0;
    }

    /**
     * Creates a Point-to-Edge match pair object.
     *
     * @param point The match point.
     * @param node  The match edge.
     */
    public PointEdgePair(STPoint point, Segment edge, double distance) {
        super(point, edge);
        this.distance = distance;
    }

    /**
     * @return The matching point.
     */
    public STPoint getPoint() {
        return _1();
    }

    /**
     * @return The matching edge.
     */
    public Segment getEdge() {
        return _2();
    }

    /**
     * @return The distance between the point and the edge.
     */
    public double getDistance() {
        return distance;
    }
}
