package traminer.util.trajectory.distance;

import traminer.util.spatial.distance.PointDistanceFunction;
import traminer.util.spatial.objects.st.STPoint;
import traminer.util.trajectory.Trajectory;

import java.util.ArrayList;
import java.util.List;

/**
 * EDR: Edit Distance on Real sequences.
 * 
 * </br> Uniform sampling rates only.
 * </br> Spatial dimension only.
 * </br> Cope with local time shift.
 * </br> Not sensitive to noise.
 * </br> Not a metric.
 * 
 * @author HanSu, Haozhou, uqdalves
 */
@SuppressWarnings("serial")
public class EDRDistanceCalculator extends TrajectoryDistanceFunction {
    private double matching_threshold;
    private double[][] costMatrix;

    /**
     * Set default  distance matching threshold = 0.0
     *
     * @param dist The points distance measure to use.
     */
    public EDRDistanceCalculator(PointDistanceFunction dist) {
        super(dist);
        this.matching_threshold = 0.00;
    }

    /**
     * Set distance matching threshold.
     *
     * @param dist The points distance measure to use.
     */
    public EDRDistanceCalculator(double threshold, PointDistanceFunction dist) {
        super(dist);
        this.matching_threshold = threshold;
    }

    @Override
    public double getDistance(final Trajectory t1, final Trajectory t2) {
        // normalize and make sure the original trajectories are unchanged
        List<STPoint> t1_norm = normalize(t1.clone());
        List<STPoint> t2_norm = normalize(t2.clone());

        return EDR(t1_norm, t2_norm);
    }

    /**
     * EDR dynamic.
     */
    protected double EDR(List<STPoint> t1, List<STPoint> t2) {
        // current iteration
        int size_t1 = t1.size();
        int size_t2 = t2.size();

        // initialize dynamic matrix
        costMatrix = new double[size_t1 + 1][size_t2 + 1];
        costMatrix[0][0] = 0.0;
        for (int i = 1; i <= size_t1; i++) {
            costMatrix[i][0] = i;
        }
        for (int i = 1; i <= size_t2; i++) {
            costMatrix[0][i] = i;
        }

        // dynamic EDR calculation
        double cost_a, cost_b, cost_c;
        for (int i = 1; i <= size_t1; i++) {
            for (int j = 1; j <= size_t2; j++) {
                cost_a = costMatrix[i - 1][j - 1] + subcost(t1.get(i - 1), t2.get(j - 1));
                cost_b = costMatrix[i - 1][j] + 1; // gap;
                cost_c = costMatrix[i][j - 1] + 1; // gap
                costMatrix[i][j] = min(cost_a, cost_b, cost_c);
            }
        }

        return costMatrix[size_t1][size_t2];
    }

    /**
     * Distance cost between p1 and p2.
     */
    private double subcost(STPoint p1, STPoint p2) {
        return p1.distance(p2, distFunc) <= matching_threshold ? 0.0 : 1.0;
    }

    /**
     * The minimum between a, b and c.
     */
    private double min(double a, double b, double c) {
        if (a <= b && a <= c) {
            return a;
        }
        if (b <= c) {
            return b;
        }
        return c;
    }

    /**
     * Normalize the trajectory coordinate points,
     * using the mean and the standard deviation.
     *
     * @return The new normalized list of values.
     */
    private List<STPoint> normalize(final List<STPoint> pointsList) {
        List<STPoint> normList = new ArrayList<STPoint>();
        if (pointsList.size() == 0) {
            return normList;
        }

        // get mean and std
        double mean = getMean(pointsList);
        double std = getStd(pointsList, mean);

        // normalize the y values
        double norm_y;
        for (STPoint p : pointsList) {
            norm_y = (p.y() - mean) / std;
            normList.add(new STPoint(p.x(), norm_y, 0));
        }

        return normList;
    }

    /**
     * The mean of the y coordinates in this points list.
     */
    private double getMean(List<STPoint> pointsList) {
        int size = pointsList.size();
        double sum = 0;
        for (STPoint p : pointsList) {
            sum += p.y();
        }
        return (sum / size);
    }

    /**
     * The standard deviation of the y coordinates
     * in this points list.
     */
    private double getStd(List<STPoint> pointsList, double mean) {
        double size = pointsList.size();
        double dif_sum2 = 0;
        for (STPoint p : pointsList) {
            dif_sum2 += (p.y() - mean) * (p.y() - mean);
        }

        return Math.sqrt(dif_sum2 / (size - 1));
    }

}
