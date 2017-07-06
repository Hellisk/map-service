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
     * Creates a Point-to-Edge match pair object.
     *
     * @param point The match point.
     * @param edge  The match edge.
     */
    public PointEdgePair(STPoint point, Segment edge) {
        super(point, edge);
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
}
