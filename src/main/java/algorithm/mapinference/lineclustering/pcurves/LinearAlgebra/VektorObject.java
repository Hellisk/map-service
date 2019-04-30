package algorithm.mapinference.lineclustering.pcurves.LinearAlgebra;

import java.io.PrintStream;

// This is an abstract class for all vectors. The abstract functions are
// the minimum requirement for a vector, the other functions in the
// Vektor inteface are derived from them. For efficiency reasons, these derived
// functions can be rewritten in extending classes.
abstract public class VektorObject implements Vektor {
	
	@Override
	abstract public Vektor Clone();
	
	@Override
	abstract public Vektor DefaultClone();
	
	@Override
	abstract public int Dimension();
	
	@Override
	abstract public double GetCoords(int i);
	
	@Override
	abstract public CovarianceMatrix CovarianceMatrixClone();
	
	@Override
	abstract public void Update(Vektor vektor);
	
	@Override
	abstract public void AddEqual(Vektor vektor); // +=
	
	@Override
	abstract public void MulEqual(double d); // +=
	
	@Override
	abstract public double Mul(Vektor vektor); // inner product
	
	@Override
	abstract public boolean equals(Object vektor);
	
	@Override
	abstract public String toString();
	
	@Override
	abstract public void Save(PrintStream pOut);
	
	@Override
	public Vektor Add(Vektor vektor) { // +
		Vektor v = Clone();
		v.AddEqual(vektor);
		return v;
	}
	
	@Override
	public Vektor Sub(Vektor vektor) { // -
		Vektor v = Clone();
		v.SubEqual(vektor);
		return v;
	}
	
	@Override
	public Vektor Mul(double d) { // *
		Vektor v = Clone();
		v.MulEqual(d);
		return v;
	}
	
	@Override
	public Vektor Div(double d) { // /
		Vektor v = Clone();
		v.DivEqual(d);
		return v;
	}
	
	@Override
	public void SubEqual(Vektor vektor) { // *=
		AddEqual(vektor.Mul(-1));
	}
	
	@Override
	public void DivEqual(double d) {// /=
		MulEqual(1 / d);
	}
	
	@Override
	public double Norm2() {
		return Math.sqrt(Norm2Squared());
	}
	
	@Override
	public double Norm2Squared() {
		return Mul(this);
	}
	
	@Override
	public double CosAngle(Vektor vektor1, Vektor vektor2) {
		Vektor v1 = vektor1.Sub(this);
		Vektor v2 = vektor2.Sub(this);
		double a = v1.Mul(v2);
		double b = v1.Norm2() * v2.Norm2();
		return a / b;
	}
	
	public double Angle(Vektor vektor1, Vektor vektor2) {
		return 180 * Math.acos(CosAngle(vektor1, vektor2)) / Math.PI;
	}
	
	@Override
	public int hashCode() {
		// TODO Auto-generated method stub
		return super.hashCode();
	}
}
