package traminer.util.map.matching;

import traminer.util.Pair;
import traminer.util.map.MapInterface;
import traminer.util.map.roadnetwork.RoadNode;
import traminer.util.spatial.objects.st.STPoint;

/**
 * An immutable Point-to-Node match object for map-matching algorithms.
 *
 * @author uqdalves
 */
@SuppressWarnings("serial")
public final class MatchPair extends Pair<STPoint, RoadNode> implements MapInterface {
    /**
     * Creates a Point-to-Node match pair object.
     *
     * @param point The match point.
     * @param node  The match node.
     */
    public MatchPair(STPoint point, RoadNode node) {
        super(point, node);
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
}
