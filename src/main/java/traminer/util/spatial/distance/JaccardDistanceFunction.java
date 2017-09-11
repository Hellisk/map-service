package traminer.util.spatial.distance;

import traminer.util.exceptions.DistanceFunctionException;

/**
 * The Jaccard distance between two N-Dimensional vectors.
 * 
 * @author uqdalves
 */
@SuppressWarnings("serial")
public class JaccardDistanceFunction implements VectorDistanceFunction {

    /**
     * The Jaccard distance between two N-Dimensional vectors.
     */
    @Override
    public double distance(double[] v, double[] u)
            throws DistanceFunctionException {
        if (v == null || u == null) {
            throw new NullPointerException("Vectors for Jaccard distance "
                    + "calculation cannot be null.");
        }
        if (v.length != u.length) {
            throw new DistanceFunctionException("Vectors should be "
                    + "of same size for Jaccard distance calculation.");
        }
        if (v.length == 0) {
            // the Jaccard coefficient in this case is equals 1,
            // but the Jaccard distance is 1 - 1 = 0
            return 0;
        }

        double minSum = 0, maxSum = 0;
        for (int i = 0; i < v.length; i++) {
            minSum += Math.min(v[i], u[i]);
            maxSum += Math.max(v[i], u[i]);
        }
        // Jaccard coefficient
        double coefficient = minSum / maxSum;
        // Jaccard distance
        double distance = 1 - coefficient;

        return distance;
    }

}
