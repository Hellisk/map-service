package traminer.util.spatial.distance;

import traminer.util.exceptions.DistanceFunctionException;
import traminer.util.spatial.SpatialUtils;

/**
 * The Cosine distance between two N-Dimensional vectors.
 * 
 * @author uqdalves
 */
@SuppressWarnings("serial")
public class CosineDistanceFunction implements VectorDistanceFunction {

    /**
     * The Cosine distance between two N-Dimensional vectors.
     */
    @Override
    public double distance(double[] v, double[] u)
            throws DistanceFunctionException {
        if (v == null || u == null) {
            throw new NullPointerException("Vectors for Cosine distance "
                    + "calculation cannot be null.");
        }
        if (v.length != u.length) {
            throw new DistanceFunctionException("Vectors should be "
                    + "of same size for Cosine distance calculation.");
        }
        if (v.length == 0) {
            return 0;
        }

        double dotProduct = SpatialUtils.dotProduct(v, u);
        double normV = SpatialUtils.norm(v);
        double normU = SpatialUtils.norm(u);
        // cosine of the angle
        double cosine = dotProduct / (normV * normU);

        return cosine;
    }

}
