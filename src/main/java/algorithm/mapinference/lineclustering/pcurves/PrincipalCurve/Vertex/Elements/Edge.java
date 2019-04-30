package algorithm.mapinference.lineclustering.pcurves.PrincipalCurve.Vertex.Elements;

import algorithm.mapinference.lineclustering.pcurves.LinearAlgebra.*;
import algorithm.mapinference.lineclustering.pcurves.PrincipalCurve.PrincipalCurveSampleVektor;

final public class Edge extends LineSegmentAbstract {
	private Sample curve;
	private int pointIndex1;
	private int pointIndex2;
	private EdgeCluster set;
	
	public Edge(Sample in_curve, int in_pointIndex1, int in_pointIndex2) {
		curve = in_curve;
		pointIndex1 = in_pointIndex1;
		pointIndex2 = in_pointIndex2;
		set = new EdgeCluster(PrincipalCurveSampleVektor.prototypeVektor);
	}
	
	final public void IncrementPointIndexes(int lowerIndex) {
		if (pointIndex1 >= lowerIndex)
			pointIndex1++;
		if (pointIndex2 >= lowerIndex)
			pointIndex2++;
	}
	
	final public void DecrementPointIndexes(int lowerIndex) {
		if (pointIndex1 > lowerIndex)
			pointIndex1--;
		if (pointIndex2 > lowerIndex)
			pointIndex2--;
	}
	
	final public Edge Reverse() {
		Edge edge = new Edge(curve, pointIndex2, pointIndex1);
		edge.set = set;
		return edge;
	}
	
	@Override
	final public Vektor GetVektor1() {
		return curve.GetPointAt(pointIndex1);
	}
	
	@Override
	final public Vektor GetVektor2() {
		return curve.GetPointAt(pointIndex2);
	}
	
	@Override
	final public void SetVektor1(Vektor vektor) {
		curve.SetPointAt(vektor, pointIndex1);
	}
	
	@Override
	final public void SetVektor2(Vektor vektor) {
		curve.SetPointAt(vektor, pointIndex2);
	}
	
	final public Vertex GetVertex1() {
		return (Vertex) curve.GetPointAt(pointIndex1);
	}
	
	final public Vertex GetVertex2() {
		return (Vertex) curve.GetPointAt(pointIndex2);
	}
	
	final public int GetVertexIndex1() {
		return pointIndex1;
	}
	
	final public int GetVertexIndex2() {
		return pointIndex2;
	}
	
	final public void SetVertexIndex1(int vi) {
		pointIndex1 = vi;
	}
	
	final public void SetVertexIndex2(int vi) {
		pointIndex2 = vi;
	}
	
	final public int GetSetSize() {
		return set.GetSize();
	}
	
	final public PrincipalCurveSampleVektor GetSetPointAt(int index) {
		return (PrincipalCurveSampleVektor) set.GetSetPointAt(index);
	}
	
	final public double GetSetWeight() {
		return set.GetWeight();
	}
	
	// Add samplePoint to set of edge or endpoint, and return MSE
	final public double AddPointToSet(PrincipalCurveSampleVektor samplePoint, double smin) {
		// Find the closest endpoint
		double d1 = samplePoint.Dist2(GetVektor1());
		double d2 = samplePoint.Dist2(GetVektor2());
		double pmin;
		int pmindex;
		if (d1 > d2) {
			pmindex = pointIndex2;
			pmin = d2;
		} else {
			pmindex = pointIndex1;
			pmin = d1;
		}
		
		// Place the point in the corresponding set
		if (smin < pmin) {
			AddPointToSet(samplePoint);
			return smin * smin;
		} else {
			((Vertex) curve.GetPointAt(pmindex)).AddPointToSet(samplePoint);
			return pmin * pmin;
		}
	}
	
	final void AddPointToSet(PrincipalCurveSampleVektor samplePoint) {
		if (samplePoint.cluster != this) {
			set.AddPoint(samplePoint);
			samplePoint.cluster = this;
		}
	}
	
	final public void DeleteMovedPointsFromSet() {
		set.DeleteMovedPoints(this);
	}
	
	final public void DeletPointFromSetAt(int index) {
		set.DeletePointAt(index);
	}
	
	final public double GetMSETimesWeight() {
		return set.GetMSETimesWeightFromLine(GetVektor1(), GetVektor2());
	}
	
	final public NumeratorAndDenominator GetNumeratorAndDenominatorForMSETimesWeight() {
		return set.GetMSEGradientTimesWeightFromLine(GetVektor1(), GetVektor2());
	}
	
	final public double GetLengthPenalty() {
		return PenaltyCoefficients.LENGTH_PENALTY_COEFFICIENT * GetEdgeAsLineSegmentObject().GetLengthSquared();
	}
	
	final public NumeratorAndDenominator GetNumeratorAndDenominatorForLengthPenalty() {
		NumeratorAndDenominator nd = new NumeratorAndDenominator(Vertex.prototypeVektor);
		nd.numerator.AddEqual(GetVektor2().Mul(PenaltyCoefficients.LENGTH_PENALTY_COEFFICIENT * 2));
		nd.denominator += PenaltyCoefficients.LENGTH_PENALTY_COEFFICIENT * 2;
		return nd;
	}
	
	@Override
	final public String toString() {
		String s = pointIndex1 + "-" + pointIndex2;
		return s;
	}
	
	final class EdgeStatistics extends OnlineSampleStatisticsDegreeTwo {
		public EdgeStatistics(Vektor in_prototypeVektor) {
			super(in_prototypeVektor);
		}
		
		// see /PrincipalCurves/Article/gradient3.0.tex, p.3.
		final public NumeratorAndDenominator GetMSEGradientTimesWeightFromLine(Vektor c, Vektor a) {
			NumeratorAndDenominator nd = new NumeratorAndDenominator(prototypeVektor);
			
			Vektor A = c.Sub(a);
			Vektor L = A.Div(A.Norm2Squared());
			double K = a.Mul(L);
			
			Vektor S_1 = sum; // \sum_i=1^n y_i
			Vektor S_2 = cov.Mul(L); // \sum_i=1^n (y_i^tL)y_i
			double S_3 = cov.MulSquared(L); // \sum_i=1^n (y_i^tL)^2
			
			double L_S_1 = L.Mul(S_1);
			Vektor num_1 = a.Mul(weight * K * (1 + K) - L_S_1 + S_3 - L_S_1 * 2 * K);
			Vektor num_2 = S_1.Mul(-K);
			
			nd.denominator += weight * K * K + S_3 - L_S_1 * 2 * K;
			nd.numerator.AddEqual(num_1.Add(num_2).Add(S_2));
			return nd;
		}
	}
	
	final class EdgeCluster extends SampleWithOnlineStatistics implements Weighted {
		EdgeStatistics edgeStatistics;
		
		public EdgeCluster(Vektor prototypeVektor) {
			super(1);
			edgeStatistics = new EdgeStatistics(prototypeVektor);
			statistics[0] = edgeStatistics;
		}
		
		final public Vektor GetSetPointAt(int index) {
			return sample.GetPointAt(index);
		}
		
		@Override
		final public double GetWeight() {
			return edgeStatistics.GetWeight();
		}
		
		final public Vektor GetSum() {
			return edgeStatistics.GetSum();
		}
		
		final public double GetSumNorm2Squared() {
			return edgeStatistics.GetSumNorm2Squared();
		}
		
		final public double GetMSETimesWeightFromLine(Vektor c, Vektor a) {
			return edgeStatistics.GetMSETimesWeightFromLine(c, a);
		}
		
		final public NumeratorAndDenominator GetMSEGradientTimesWeightFromLine(Vektor c, Vektor a) {
			return edgeStatistics.GetMSEGradientTimesWeightFromLine(c, a);
			
		}
		
		final public void DeleteMovedPoints(Object cluster) {
			boolean[] toBeDeleted = new boolean[GetSize()];
			PrincipalCurveSampleVektor samplePoint;
			for (int i = 0; i < sample.getSize(); i++) {
				samplePoint = (PrincipalCurveSampleVektor) sample.GetPointAt(i);
				if (samplePoint.cluster != cluster) {
					edgeStatistics.DeletePoint(samplePoint);
					toBeDeleted[i] = true;
				} else
					toBeDeleted[i] = false;
			}
			sample.DeletePoints(toBeDeleted);
		}
		
	}
}
