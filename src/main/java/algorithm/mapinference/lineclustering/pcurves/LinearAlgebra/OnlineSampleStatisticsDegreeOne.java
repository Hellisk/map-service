package algorithm.mapinference.lineclustering.pcurves.LinearAlgebra;

final public class OnlineSampleStatisticsDegreeOne extends OnlineSampleStatistics implements Weighted {
	private Vektor prototypeVektor;
	private double weight;
	private Vektor sum;
	private Vektor center;
	private double sigmaSquaredTimesWeight;
	
	public OnlineSampleStatisticsDegreeOne(Vektor vektor) {
		prototypeVektor = vektor.DefaultClone();
		Reset();
	}
	
	@Override
	final public void Reset() {
		weight = 0;
		sigmaSquaredTimesWeight = weight = 0;
		sum = prototypeVektor.DefaultClone();
		center = null;
	}
	
	@Override
	final public void AddPoint(Vektor vektor) {
		double w;
		try {
			w = ((Weighted) vektor).GetWeight();
		} catch (ClassCastException e) {
			w = 1;
		}
		if (weight == 0) {
			sum = vektor.Mul(w);
			center = vektor.Clone();
			sigmaSquaredTimesWeight = 0;
			weight = w;
		} else {
			Vektor oldCenter = center.Clone();
			sum.AddEqual(vektor.Mul(w));
			center = sum.Div(weight + w);
			double d = center.Dist2Squared(oldCenter);
			double dn = center.Dist2Squared(vektor);
			sigmaSquaredTimesWeight += weight * d + w * dn;
			weight += w;
		}
	}
	
	@Override
	final public void DeletePoint(Vektor vektor) {
		double w;
		try {
			w = ((Weighted) vektor).GetWeight();
		} catch (ClassCastException e) {
			w = 1;
		}
		if (weight - w == 0)
			Reset();
		else {
			Vektor oldCenter = center.Clone();
			sum.SubEqual(vektor.Mul(w));
			center = sum.Div(weight - w);
			double d = center.Dist2Squared(oldCenter);
			double dn = oldCenter.Dist2Squared(vektor);
			sigmaSquaredTimesWeight -= (weight - w) * d + w * dn;
			weight -= w;
		}
	}
	
	final public Vektor GetSum() {
		return sum;
	}
	
	final public Vektor GetCenter() {
		return center;
	}
	
	final public double GetSigmaSquaredTimesWeight() {
		return sigmaSquaredTimesWeight;
	}
	
	final public double GetSigmaSquared() {
		return sigmaSquaredTimesWeight / weight;
	}
	
	@Override
	final public double GetWeight() {
		return weight;
	}
	
	final public double GetMSETimesWeight(Vektor vektor) {
		try {
			if (center == null)
				return 0;
			return sigmaSquaredTimesWeight + weight * center.Dist2Squared(vektor);
		}
		// if center is null, there is no point in the set
		catch (NullPointerException e) {
			return 0;
		}
	}
	
}
