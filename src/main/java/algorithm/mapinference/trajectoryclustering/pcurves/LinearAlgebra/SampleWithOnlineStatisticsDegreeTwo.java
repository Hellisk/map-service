package algorithm.mapinference.trajectoryclustering.pcurves.LinearAlgebra;

final public class SampleWithOnlineStatisticsDegreeTwo extends SampleWithOnlineStatistics {
    private OnlineSampleStatisticsDegreeTwo statisticsDegreeTwo;

    public SampleWithOnlineStatisticsDegreeTwo(Sample in_sample) {
        super(1);
        statisticsDegreeTwo = new OnlineSampleStatisticsDegreeTwo(in_sample.GetPointAt(0));
        statistics[0] = statisticsDegreeTwo;
        ShallowCopy(in_sample);
    }

    final public double GetMSE(Line line) {
        return statisticsDegreeTwo.GetMSETimesWeightFromLine(line.GetVektor1(), line.GetVektor2())
                / statisticsDegreeTwo.GetWeight();
    }

    final public double GetRMSE(Line line) {
        return Math.sqrt(statisticsDegreeTwo.GetMSETimesWeightFromLine(line.GetVektor1(), line.GetVektor2())
                / statisticsDegreeTwo.GetWeight());
    }

    final public LineObject FirstPrincipalComponent() {
        return statisticsDegreeTwo.FirstPrincipalComponent();
    }

    final public Sample GetProjectionResiduals(Line line) {
        return sample.GetProjectionResiduals(line);
    }
}
