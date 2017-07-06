package traminer.util.spatial.distance;

import traminer.util.exceptions.DistanceFunctionException;

/**
 * The Chi-Square distance between two N-Dimensional vectors.
 *
 * @author uqdalves
 */
@SuppressWarnings("serial")
public class ChiSquareDistanceFunction implements VectorDistanceFunction {

    /**
     * The Chi-Square distance between two N-Dimensional vectors.
     */
    @Override
    public double distance(double[] v, double[] u)
            throws DistanceFunctionException {
        if (v == null || u == null) {
            throw new NullPointerException("Vectors for Chi-Square "
                    + "distance calculation cannot be null.");
        }
        if (v.length != u.length) {
            throw new DistanceFunctionException("Vectors should be "
                    + "of same size for Chi-Square distance calculation.");
        }
        if (v.length == 0) {
            return 0;
        }

        double chiSquare = 0;
        double vi, ui;
        for (int i = 0; i < v.length; i++) {
            vi = v[i];
            ui = u[i];
            chiSquare += (vi - ui) * (vi - ui) / (vi + ui);
        }
        chiSquare = chiSquare / 2;

        return chiSquare;
    }

}
