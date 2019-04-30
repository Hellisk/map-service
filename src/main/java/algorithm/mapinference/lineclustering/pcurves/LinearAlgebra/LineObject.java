package algorithm.mapinference.lineclustering.pcurves.LinearAlgebra;

final public class LineObject extends LineAbstract implements Line {
	private Vektor vektor1;
	private Vektor vektor2;
	
	public LineObject(Vektor in_vektor1, Vektor in_vektor2) {
		vektor1 = in_vektor1;
		vektor2 = in_vektor2;
	}
	
	public LineObject(LineSegment lineSegment) {
		vektor1 = lineSegment.GetVektor1();
		vektor2 = lineSegment.GetVektor2();
	}
	
	private static Vektor GetIntersection(Line line1, Line line2) {
		Vektor a = line1.GetVektor1();
		Vektor b = line1.GetVektor2();
		Vektor c = b.Sub(a);
		Vektor x = line2.GetVektor1();
		Vektor y = line2.GetVektor2();
		Vektor z = y.Sub(x);
		try {
			double a1 = ((Vektor2D) a).coordX;
			double a2 = ((Vektor2D) a).coordY;
			double c1 = ((Vektor2D) c).coordX;
			double c2 = ((Vektor2D) c).coordY;
			double x1 = ((Vektor2D) x).coordX;
			double x2 = ((Vektor2D) x).coordY;
			double z1 = ((Vektor2D) z).coordX;
			double z2 = ((Vektor2D) z).coordY;
			
			double v = c2 * (a1 - x1) - c1 * (a2 - x2);
			double w = c2 * z1 - c1 * z2;
			if (algorithm.mapinference.lineclustering.pcurves.Utilities.MyMath.equals(w, 0)) // parallel lines
				return null;
			double s = v / w;
			return x.Add(z.Mul(s));
		} catch (ClassCastException e) {
			throw new ArithmeticException("MYERROR: Not yet implemented.");
		}
	}
	
	// Clockwise normal line: incident to vektor, orthogonal to normal, inPlane is for higher dimensions.
	public static Line GetNormalAt(Vektor vektor, Vektor normal, Vektor inPlane) {
		if (!((vektor instanceof Vektor2D) && (normal instanceof Vektor2D)))
			throw new ArithmeticException("MYERROR: Not yet implemented.");
		double nX = ((Vektor2D) normal).coordX;
		double nY = ((Vektor2D) normal).coordY;
		return new LineObject(vektor, vektor.Add(new Vektor2D(nY, -nX)));
	}
	
	public static Vektor GetCenterOfCircleAround(Vektor vektor1, Vektor vektor2, Vektor vektor3) {
		if (!((vektor1 instanceof Vektor2D) && (vektor2 instanceof Vektor2D) && (vektor3 instanceof Vektor2D)))
			throw new ArithmeticException("MYERROR: Not yet implemented.");
		Vektor midPoint1 = vektor1.Add(vektor2).Div(2);
		Vektor midPoint2 = vektor1.Add(vektor3).Div(2);
		Vektor difference1 = vektor1.Sub(vektor2);
		Vektor difference2 = vektor1.Sub(vektor3);
		Line oldalfelezoMeroleges1 = LineObject.GetNormalAt(midPoint1, difference1, new Vektor2D(0, 0));
		Line oldalfelezoMeroleges2 = LineObject.GetNormalAt(midPoint2, difference2, new Vektor2D(0, 0));
		return GetIntersection(oldalfelezoMeroleges1, oldalfelezoMeroleges2);
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
	
	// Length = 1, vektor1 -> vektor2
	@Override
	final public Vektor GetDirectionalVektor() {
		Vektor vektor = vektor2.Sub(vektor1);
		vektor.DivEqual(vektor.Norm2());
		return vektor;
	}
	
	@Override
	final public Vektor Reflect(Vektor vektor) {
		Vektor projection = vektor.Project(this);
		return vektor.Add(projection.Sub(vektor).Mul(2));
	}
	
	@Override
	final public String toString() {
		return vektor1 + "\n" + vektor2;
	}
}
