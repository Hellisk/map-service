package traminer.util.spatial.clustering;

import traminer.util.spatial.SpatialInterface;
import traminer.util.spatial.objects.Point;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Base interface for clustering methods/services.
 *
 * @author uqdalves
 */
public interface ClusteringInterface extends SpatialInterface {
    //public abstract Clustering<? extends Model> run();

    /**
     * Randomly select and return n points from the given list.
     */
    default List<Point> getRandomPoints(List<? extends Point> list, final int n) {
        // make sure the original list is unchanged
        List<Point> result = new ArrayList<Point>(list);
        Collections.shuffle(result);
        return result.subList(0, n);
    }
}
