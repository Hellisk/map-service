package traminer.util.spatial.clustering;

import traminer.util.spatial.SpatialInterface;
import traminer.util.spatial.objects.Point;

import java.util.ArrayList;
import java.util.List;

/**
 * A spatial cluster entity. Cluster of spatial Points
 * returned from the clustering algorithms.
 *  
 * @author uqdalves
 */
@SuppressWarnings("serial")
public class SpatialCluster extends ArrayList<Point> implements SpatialInterface {
    /**
     * Cluster id
     */
    private final String id;
    /**
     * Flag if this cluster is a noise
     */
    private boolean isNoise = false;
    /**
     * The center/centroid of this cluster
     */
    private Point center;

    /**
     * Creates a new empty cluster with the given id.
     *
     * @param id The cluster id.
     */
    public SpatialCluster(String id) {
        super(1);
        this.id = id;
    }

    /**
     * Creates a new cluster with the given id
     * and initial list of points.
     *
     * @param id         The cluster id.
     * @param pointsList Initial list of points in this cluster.
     */
    public SpatialCluster(String id, List<? extends Point> pointsList) {
        super(pointsList);
        this.id = id;
    }

    /**
     * @return The id of this cluster.
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the center/centroid point of this cluster.
     * If the center was not already set, this method will
     * calculate and return the centroid of this cluster.
     *
     * @return The center point of this cluster.
     */
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

    /**
     * @param center The center/centroid point of this cluster.
     */
    public void setCenter(Point center) {
        this.center = center;
    }

    /**
     * Set the coordinates of the center/centroid point of this cluster.
     *
     * @param x Center X coordinate.
     * @param y Center Y coordinate.
     */
    public void setCenter(double x, double y) {
        this.center = new Point(x, y);
    }

    /**
     * Check whether this cluster is a noise.
     *
     * @return True if this cluster is a noise,
     * false otherwise.
     */
    public boolean isNoise() {
        return isNoise;
    }

    /**
     * @param noise Whether this cluster is a noise.
     */
    public void setNoise(boolean noise) {
        this.isNoise = noise;
    }
}
