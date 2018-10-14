package mapupdate.util.function;

import mapupdate.util.exceptions.DistanceFunctionException;
import mapupdate.util.object.SpatialInterface;

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