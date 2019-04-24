package algorithm.mapinference.trajectoryclustering.pcurves.LinearAlgebra;

public interface CovarianceMatrix {
    public void AddEqual(Vektor y_i, double weight);

    public void SubEqual(Vektor y_i, double weight);

    public Vektor Mul(Vektor A); // \sum_i=1^n (y_i^tA)y_i

    public double MulSquared(Vektor A); // \sum_i=1^n (y_i^tA)^2

    public double GetNorm2Squared(); // \sum_i=1^n y_i^ty_i
}
