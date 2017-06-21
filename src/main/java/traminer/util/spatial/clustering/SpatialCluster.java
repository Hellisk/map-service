package traminer.util.spatial.clustering;

import traminer.util.spatial.SpatialInterface;
import traminer.util.spatial.objects.Point;

import java.util.ArrayList;
import java.util.Collection;

/**
 * A basic spatial cluster entity, cluster of spatial Points
 * returned from the clustering algorithms.
 *
 * @author uqdalves
 */
@SuppressWarnings("serial")
public class SpatialCluster extends ArrayList<Point> implements SpatialInterface {
    private final String id;
    private boolean noise = false;
    private Point center;

    public SpatialCluster(String id) {
        super();
        this.id = id;
    }

    public SpatialCluster(String id, Collection<? extends Point> collection) {
        super(collection);
        this.id = id;
    }

    /**
     * Initial capacity
     */
    public SpatialCluster(String id, int size) {
        super(size);
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public Point getCenter() {
        if (center == null) {
            // get mean x, y and z
            double x = 0, y = 0;
            for (Point p : this) {
                x += p.x();
                y += p.y();
            }
            x /= size();
            y /= size();

            this.center = new Point(x, y);
        }
        return center;
    }

    public void setCenter(Point center) {
        this.center = center;
    }

    public void setCenter(double x, double y) {
        this.center = new Point(x, y);
    }

    public boolean isNoise() {
        return noise;
    }

    public void setNoise(boolean noise) {
        this.noise = noise;
    }
}
