package algorithm.mapinference.lineclustering.pcurves.PrincipalCurve.Vertex.Elements;

import algorithm.mapinference.lineclustering.pcurves.LinearAlgebra.Vektor;

final public class NumeratorAndDenominator {
	public Vektor numerator;
	public double denominator;
	
	public NumeratorAndDenominator(Vektor prototypeVektor) {
		numerator = prototypeVektor.DefaultClone();
		denominator = 0.0;
	}
	
	final public Vektor Resolve() {
		numerator.DivEqual(denominator);
		return numerator;
	}
	
	final public void AddEqual(NumeratorAndDenominator nd) {
		numerator.AddEqual(nd.numerator);
		denominator += nd.denominator;
	}
	
	final public void DivEqual(double d) {
		numerator.DivEqual(d);
		denominator /= d;
	}
	
	final public void MulEqual(double d) {
		numerator.MulEqual(d);
		denominator *= d;
	}
	
	@Override
	final public String toString() {
		return ("numerator: \n" + numerator + "\ndenominator:\n" + denominator);
	}
}
