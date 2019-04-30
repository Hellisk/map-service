package algorithm.mapinference.lineclustering.pcurves.LinearAlgebra;

final public class CovarianceMatrix2D implements CovarianceMatrix {
	double xx;
	private double xy;
	private double yy;
	
	protected CovarianceMatrix2D() {
		xx = 0;
		xy = 0;
		yy = 0;
	}
	
	@Override
	final public void AddEqual(Vektor y_i, double weight) {
		double coordX = ((Vektor2D) y_i).coordX;
		double coordY = ((Vektor2D) y_i).coordY;
		xx += weight * coordX * coordX;
		xy += weight * coordX * coordY;
		yy += weight * coordY * coordY;
	}
	
	@Override
	final public void SubEqual(Vektor y_i, double weight) {
		double coordX = ((Vektor2D) y_i).coordX;
		double coordY = ((Vektor2D) y_i).coordY;
		xx -= weight * coordX * coordX;
		xy -= weight * coordX * coordY;
		yy -= weight * coordY * coordY;
	}
	
	// \sum_i=1^n (y_i^tA)y_i
	@Override
	final public Vektor Mul(Vektor A) {
		double coordX = ((Vektor2D) A).coordX;
		double coordY = ((Vektor2D) A).coordY;
		return new Vektor2D(coordX * xx + coordY * xy, coordX * xy + coordY * yy);
	}
	
	// \sum_i=1^n (y_i^tA)^2
	@Override
	final public double MulSquared(Vektor A) {
		double coordX = ((Vektor2D) A).coordX;
		double coordY = ((Vektor2D) A).coordY;
		return coordX * coordX * xx + coordY * coordY * yy + 2 * coordX * coordY * xy;
	}
	
	// \sum_i=1^n y_i^ty_i
	@Override
	final public double GetNorm2Squared() {
		return xx + yy;
	}
}
