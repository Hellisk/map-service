package traminer.util.spatial.distance;

import traminer.util.exceptions.DistanceFunctionException;
import traminer.util.spatial.SpatialInterface;

/**
 * Interface for distance functions between N-dimensional vectors.
 *
 * @author uqdalves
 */
public interface VectorDistanceFunction extends SpatialInterface {

    /**
     * Distance between two N-Dimensional vectors.
     * <br>
     * Vectors should have the same dimensions.
     *
     * @param v N-dimensional vector V.
     * @param u N-dimensional vector U.
     * @return Distance between U and V.
     * @throws DistanceFunctionException If vectors U and V
     *                                   do not have the same dimensions.
     */
    double distance(double[] v, double[] u)
            throws DistanceFunctionException;
}
