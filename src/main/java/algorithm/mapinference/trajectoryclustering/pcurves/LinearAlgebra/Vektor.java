package algorithm.mapinference.trajectoryclustering.pcurves.LinearAlgebra;

import java.io.PrintStream;

public interface Vektor {
    public Vektor Clone();

    public Vektor DefaultClone();

    public CovarianceMatrix CovarianceMatrixClone();

    public void Update(Vektor vektor);

    public int Dimension();

    public double GetCoords(int i);

    public Vektor Add(Vektor vektor); // +

    public Vektor Sub(Vektor vektor); // -

    public Vektor Mul(double d); // *

    public Vektor Div(double d); // /

    public double Mul(Vektor vektor); // inner product

    public void AddEqual(Vektor vektor); // +=

    public void SubEqual(Vektor vektor); // -=

    public void MulEqual(double d); // *=

    public void DivEqual(double d); // /=

    @Override
    public boolean equals(Object vektor);

    public double Norm2();

    public double Norm2Squared();

    public double Dist2(Vektor vektor);

    public double Dist2Squared(Vektor vektor);

    public double Dist2(LineSegment lineSegment);

    public double Dist2Squared(LineSegment lineSegment);

    public Vektor Project(LineSegment lineSegment);

    public Vektor Project(Line line);

    public double CosAngle(Vektor vektor1, Vektor vektor2);

    @Override
    public String toString();

    public void Save(PrintStream pOut);

    public String SaveToString();
}
