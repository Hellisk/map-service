package mapupdate.mapinference.trajectoryclustering.pcurves.LinearAlgebra;

final public class OnlineSampleStatisticsWeight extends OnlineSampleStatistics implements Weighted {
    private double weight;

    public OnlineSampleStatisticsWeight() {
        Reset();
    }

    @Override
    final public void Reset() {
        weight = 0;
    }

    @Override
    final public void AddPoint(Vektor vektor) {
        try {
            weight += ((Weighted) vektor).GetWeight();
        } catch (ClassCastException e) {
            weight += 1;
        }
    }

    @Override
    final public void DeletePoint(Vektor vektor) {
        try {
            weight -= ((Weighted) vektor).GetWeight();
        } catch (ClassCastException e) {
            weight -= 1;
        }
    }

    @Override
    final public double GetWeight() {
        return weight;
    }
}
