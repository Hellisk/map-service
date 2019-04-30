package algorithm.mapinference.lineclustering.pcurves.LinearAlgebra;

final public class LineSegmentObject extends LineSegmentAbstract {
	private Vektor vektor1;
	private Vektor vektor2;
	
	public LineSegmentObject(Vektor in_vektor1, Vektor in_vektor2) {
		vektor1 = in_vektor1;
		vektor2 = in_vektor2;
	}
	
	@Override
	final public Vektor GetVektor1() {
		return vektor1;
	}
	
	@Override
	final public Vektor GetVektor2() {
		return vektor2;
	}
	
	@Override
	final public void SetVektor1(Vektor vektor) {
		vektor1 = vektor;
	}
	
	@Override
	final public void SetVektor2(Vektor vektor) {
		vektor2 = vektor;
	}
}
