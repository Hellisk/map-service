package traminer.util.trajectory.transformation;

import traminer.util.spatial.SpatialInterface;
import traminer.util.trajectory.Trajectory;

/**
 * Base interface for trajectory data transformations.
 * <p>
 * Trajectory transformations are useful for experimentation
 * visualization, and distance measures comparison.
 *
 * @author uqdalves *
 */
@SuppressWarnings("serial")
public abstract class TrajectoryTransformation implements SpatialInterface {
// TODO make sure the transformations return a copy of the trajectory	

    /**
     * Run the transformation on the given trajectory.
     *
     * @return Return a copy of this trajectory after performing
     * this transformation.
     */
    public abstract Trajectory getTransformation(Trajectory t);

    protected int[] sort(double[] list) {
        int[] result = new int[list.length];
        boolean[] mark = new boolean[list.length];

        for (int i = 0; i < mark.length; i++) {
            mark[i] = true;
            result[i] = -1;
        }
        int count = 0;
        for (int i = 0; i < list.length; i++) {
            double max = -1;
            int index = -1;
            for (int j = 0; j < list.length; j++) {
                if (mark[j]) {
                    if (max == -1) {
                        max = list[j];
                        index = j;
                    } else if (max < list[j]) {
                        max = list[j];
                        index = j;
                    }
                }
            }
            mark[index] = false;
            result[count] = index;
            count++;
        }
        return result;
    }

    protected int[] topN(int allSize, int N) {
        int[] result = new int[N];

        double[] valueList = new double[allSize];
        for (int i = 0; i < valueList.length; i++) {
            valueList[i] = Math.random();
        }
        int[] allSizeList = sort(valueList);

        for (int i = 0; i < N; i++) {
            result[i] = allSizeList[i];
        }

        for (int i = 0; i < N; i++) {
            int min = result[i];
            int minIndex = i;
            for (int j = i + 1; j < N; j++) {
                if (min > result[j]) {
                    min = result[j];
                    minIndex = j;
                }
            }
            int temp = result[i];
            result[i] = min;
            result[minIndex] = temp;
        }

        return result;
    }

    protected int[] topN(int allSize, int N, double[] valueList) {
        int[] result = new int[N];

        int[] allSizeList = sort(valueList);

        for (int i = 0; i < N; i++) {
            result[i] = allSizeList[i];
        }

        for (int i = 0; i < N; i++) {
            int min = result[i];
            int minIndex = i;
            for (int j = i + 1; j < N; j++) {
                if (min > result[j]) {
                    min = result[j];
                    minIndex = j;
                }
            }
            int temp = result[i];
            result[i] = min;
            result[minIndex] = temp;
        }

        return result;
    }
}
