package algorithm.mapinference.lineclustering.pcurves.LinearAlgebra;

// This is an abstract class for all line segments. The abstract functions are
// the minimum requirement for a line segment, the other functions in the
// LineSegment inteface are derived from them. For efficiency reasons, these derived
// functions can be rewritten in extending classes.
abstract public class LineSegmentAbstract implements LineSegment {
	@Override
	abstract public Vektor GetVektor1();
	
	@Override
	abstract public Vektor GetVektor2();
	
	@Override
	abstract public void SetVektor1(Vektor vektor);
	
	@Override
	abstract public void SetVektor2(Vektor vektor);
	
	final public LineSegmentObject GetEdgeAsLineSegmentObject() {
		return new LineSegmentObject(GetVektor1(), GetVektor2());
	}
	
	@Override
	final public double GetLength() {
		return GetVektor1().Dist2(GetVektor2());
	}
	
	@Override
	final public double GetLengthSquared() {
		return GetVektor1().Dist2Squared(GetVektor2());
	}
	
	@Override
	final public Vektor GetMidVektor() {
		return GetVektor1().Add(GetVektor2()).Div(2);
	}
	
	@Override
	final public Vektor GetPointAtParameter(double parameter) {
		Vektor vektor = GetVektor2().Sub(GetVektor1());
		vektor.MulEqual(parameter / GetLength());
		vektor.AddEqual(GetVektor1());
		return vektor;
	}
	
	@Override
	final public boolean equals(LineSegment lineSegment) {
		return ((lineSegment.GetVektor1().equals(GetVektor1()) && lineSegment.GetVektor2().equals(GetVektor2())) || (lineSegment
				.GetVektor1().equals(GetVektor2()) && lineSegment.GetVektor2().equals(GetVektor1())));
	}
}
