package traminer.util.trajectory.distance;

import traminer.util.spatial.Axis;
import traminer.util.spatial.objects.st.STPoint;
import traminer.util.trajectory.Trajectory;

import java.util.ArrayList;
import java.util.List;

/**
 * ERP: Edit distance with Real Penalty.
 *
 * </br> Discrete 1D time series only.  
 * </br> Uniform sampling rates only.
 * </br> Sensitive to noise.
 * </br> Cope with local time shift.
 * </br> Metric function.
 * 
 * @author uqhsu1, Haozhou, uqdalves
 */
@SuppressWarnings("serial")
public class ERPDistanceCalculator extends TrajectoryDistanceFunction {
    private double g_threshold;
    private double[][] costMatrix;

    /**
     * Set default g distance threshold = 0.0
     */
    public ERPDistanceCalculator() {
        g_threshold = 0.00;
    }

    /**
     * Set distance matching threshold.
     */
    public ERPDistanceCalculator(double threshold) {
        g_threshold = threshold;
    }

    /**
     * ERP distance between two trajectories. ERD is a 1D distance measure,
     * calculates the distance on the given dimension.
     */
    public double getDistance(final Trajectory t1, final Trajectory t2, Axis dim) {
        // get only the X attributes of trajectory points
        List<Double> t1_val = new ArrayList<Double>(t1.size());
        List<Double> t2_val = new ArrayList<Double>(t2.size());
        for (STPoint p : t1) {
            switch (dim) {
                case X:
                    t1_val.add(p.x());
                case Y:
                    t1_val.add(p.y());
                default:
            }
        }
        for (STPoint p : t2) {
            switch (dim) {
                case X:
                    t2_val.add(p.x());
                case Y:
                    t2_val.add(p.y());
                default:
            }
        }
        // normalize and make sure the original time series will not be changed
        t1_val = normalize(t1_val);
        t2_val = normalize(t2_val);

        return ERP(t1_val, t2_val);
    }

    /**
     * ERP distance between two trajectories. ERD is a 1D distance measure,
     * uses the points X coordinate by default.
     */
    @Override
    public double getDistance(final Trajectory t1, final Trajectory t2) {
        // get only the X attributes of trajectory points
        List<Double> t1_val = new ArrayList<Double>(t1.size());
        for (STPoint p : t1) {
            t1_val.add(p.x());
        }
        List<Double> t2_val = new ArrayList<Double>(t2.size());
        for (STPoint p : t2) {
            t2_val.add(p.x());
        }
        // normalize and make sure the original time series will not be changed
        t1_val = normalize(t1_val);
        t2_val = normalize(t2_val);

        return ERP(t1_val, t2_val);
    }

    /**
     * The ERP distance between two arbitrary lists of values.
     * ERD is a 1D distance measure.
     */
    public double getDistance(final List<Double> t1, final List<Double> t2) {
        // normalize and make sure the original time series will not be changed
        List<Double> t1_norm = normalize(t1);
        List<Double> t2_norm = normalize(t2);

        return ERP(t1_norm, t2_norm);
    }

    /**
     * ERP dynamic.
     */
    protected double ERP(List<Double> t1, List<Double> t2) {
        // current iteration
        int size_t1 = t1.size();
        int size_t2 = t2.size();

        // initialize dynamic matrix
        costMatrix = new double[size_t1 + 1][size_t2 + 1];
        costMatrix[0][0] = 0.0;
        for (int i = 1; i <= size_t1; i++) {
            costMatrix[i][0] = costMatrix[i - 1][0] + t1.get(i - 1);
        }
        for (int i = 1; i <= size_t2; i++) {
            costMatrix[0][i] = costMatrix[0][i - 1] + t2.get(i - 1);
        }

        // dynamic ERP calculation
        double cost_a, cost_b, cost_c;
        for (int i = 1; i <= size_t1; i++) {
            for (int j = 1; j <= size_t2; j++) {
                cost_a = costMatrix[i - 1][j - 1] + cost(t1.get(i - 1), t2.get(j - 1));
                cost_b = costMatrix[i - 1][j] + cost(t1.get(i - 1)); // gap
                cost_c = costMatrix[i][j - 1] + cost(t2.get(j - 1)); // gap
                costMatrix[i][j] = min(cost_a, cost_b, cost_c);
            }
        }

        return costMatrix[size_t1][size_t2];
    }

    /**
     * Normalize the values in this list
     * using the mean and the standard deviation.
     *
     * @return The new normalized list of values.
     */
    private List<Double> normalize(List<Double> list) {
        List<Double> normList = new ArrayList<Double>();
        if (list.size() == 0) {
            return normList;
        }

        // get mean and std
        double mean = getMean(list);
        double std = getStd(list, mean);

        // normalize values
        double norm;
        for (Double value : list) {
            norm = (value - mean) / std;
            normList.add(norm);
        }

        return normList;
    }

    /**
     * The mean of the values in this list.
     */
    private double getMean(List<Double> list) {
        int size = list.size();
        double sum = 0.0;
        for (Double value : list) {
            sum += value;
        }
        return (sum / size);
    }

    /**
     * The standard deviation of values in this list.
     */
    private double getStd(List<Double> list, double mean) {
        double size = list.size();
        double dif_sum2 = 0.0;
        for (Double value : list) {
            dif_sum2 += (value - mean) * (value - mean);
        }
        return Math.sqrt(dif_sum2 / size);
    }

    /**
     * Edit cost (distance).
     */
    private double cost(double v1, double v2) {
        return Math.abs(v1 - v2);
    }

    /**
     * Edit cost - gap (distance).
     */
    private double cost(double v) {
        return Math.abs(v - g_threshold);
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

}
