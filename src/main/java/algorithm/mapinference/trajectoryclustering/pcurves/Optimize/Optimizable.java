package algorithm.mapinference.trajectoryclustering.pcurves.Optimize;

public interface Optimizable {
    public void OptimizingStep(double step);

    public double GetCriterion();
}
