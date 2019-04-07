package mapupdate.mapinference.trajectoryclustering.pcurves.LinearAlgebra;

public abstract class OnlineSampleStatistics {
    abstract public void Reset();

    abstract public void AddPoint(Vektor vektor);

    abstract public void DeletePoint(Vektor vektor);

    // This can be overridden for efficiency
    public void UpdatePoint(Vektor vektorFrom, Vektor vektorTo) {
        DeletePoint(vektorFrom);
        AddPoint(vektorTo);
    }
}
