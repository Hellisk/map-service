package mapupdate.mapinference.trajectoryclustering.pcurves.LinearAlgebra;

// no abstract functions, but statistics[i]'s must be initialized
abstract public class SampleWithOnlineStatistics {
    protected OnlineSampleStatistics[] statistics;
    protected Sample sample;

    protected SampleWithOnlineStatistics(int numOfStatistics) {
        statistics = new OnlineSampleStatistics[numOfStatistics];
        sample = new Sample();
    }

    final protected void Copy(Sample in_sample) {
        Reset();
        sample = in_sample.Clone();
        for (int j = 0; j < sample.getSize(); j++)
            for (OnlineSampleStatistics statistic : statistics)
                statistic.AddPoint(sample.GetPointAt(j));
    }

    final protected void ShallowCopy(Sample in_sample) {
        Reset();
        sample = in_sample.ShallowClone();
        for (int j = 0; j < sample.getSize(); j++)
            for (OnlineSampleStatistics statistic : statistics)
                statistic.AddPoint(sample.GetPointAt(j));
    }

    final public void Reset() {
        sample.Reset();
        for (OnlineSampleStatistics statistic : statistics)
            statistic.Reset();
    }

    final public void UpdatePointAt(Vektor vektor, int index) {
        for (OnlineSampleStatistics statistic : statistics)
            statistic.UpdatePoint(sample.GetPointAt(index), vektor);
        sample.UpdatePointAt(vektor, index);
    }

    final public void SetPointAt(Vektor vektor, int index) {
        for (OnlineSampleStatistics statistic : statistics)
            statistic.UpdatePoint(sample.GetPointAt(index), vektor);
        sample.SetPointAt(vektor, index);
    }

    final public void AddPoint(Vektor vektor) {
        sample.AddPoint(vektor);
        for (OnlineSampleStatistics statistic : statistics)
            statistic.AddPoint(vektor);
    }

    final public void DeletePointAt(int index) {
        for (OnlineSampleStatistics statistic : statistics)
            statistic.DeletePoint(sample.GetPointAt(index));
        sample.DeletePointAt(index);
    }

    final public int GetSize() {
        return sample.getSize();
    }

}
