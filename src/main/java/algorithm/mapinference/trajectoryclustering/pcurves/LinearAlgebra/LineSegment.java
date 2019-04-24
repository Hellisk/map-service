package algorithm.mapinference.trajectoryclustering.pcurves.LinearAlgebra;


public interface LineSegment {
    public Vektor GetVektor1();

    public Vektor GetVektor2();

    public void SetVektor1(Vektor vektor);

    public void SetVektor2(Vektor vektor);

    public double GetLength();

    public double GetLengthSquared();

    public Vektor GetMidVektor();

    public Vektor GetPointAtParameter(double parameter);

    public boolean equals(LineSegment lineSegment);
}
