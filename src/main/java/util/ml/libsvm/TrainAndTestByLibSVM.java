package util.ml.libsvm;

public class TrainAndTestByLibSVM {
    /**
     * Build a 2d feature vector
     *
     * @param x
     * @param y
     * @return
     */
    public static svm_node[] buildPoint(double x, double y) {
        svm_node[] point = new svm_node[2];

        // x
        point[0] = new svm_node();
        point[0].index = 1;
        point[0].value = x;

        // y
        point[1] = new svm_node();
        point[1].index = 2;
        point[1].value = y;

        return point;
    }

    public static svm_model buildModel(svm_node[][] nodes) {
        // Build Parameters
        svm_parameter param = new svm_parameter();
        param.svm_type = svm_parameter.ONE_CLASS;
        param.kernel_type = svm_parameter.RBF;
        param.gamma = 0.5;
        param.C = 0.25;
        param.nu = 0.1608;
        param.cache_size = 100;

        // Build Problem
        svm_problem problem = new svm_problem();
        problem.x = nodes;
        problem.l = nodes.length;
        problem.y = prepareY(nodes.length); // y is label of the tuple

        // Build Model
        return svm.svm_train(problem, param);
    }

    private static double[] prepareY(int size) {
        double[] y = new double[size];

        for (int i = 0; i < size; i++)
            y[i] = 1;

        return y;
    }

    public static double predict(svm_model model, svm_node[] nodes) {
        double[] scores = new double[2];
        double result = svm.svm_predict(model, nodes);

        return scores[0];
    }

    public static void main(String[] args) {
        svm_node[] point = TrainAndTestByLibSVM.buildPoint(1, 2);

        svm_node[][] points = new svm_node[2][3];
        svm_model model = TrainAndTestByLibSVM.buildModel(points);
        TrainAndTestByLibSVM.predict(model, point);
    }
}
