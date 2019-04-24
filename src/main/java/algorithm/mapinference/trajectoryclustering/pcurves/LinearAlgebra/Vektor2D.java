package algorithm.mapinference.trajectoryclustering.pcurves.LinearAlgebra;

/*
 * javah -jni algorithm.mapinference.trajectoryclustering.pcurves.LinearAlgebra.Vektor2D
 * mv LinearAlgebra_Vektor2D.h ~/Java/Sources/CRoutines/
 * change <jni.h> to "jni.h"
 */

import algorithm.mapinference.trajectoryclustering.pcurves.Utilities.MyMath;
import util.function.GreatCircleDistanceFunction;
import util.object.spatialobject.Point;

import java.io.PrintStream;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

public class Vektor2D extends VektorObject {
//	static {
//		if (!algorithm.mapinference.trajectoryclustering.pcurves.Utilities.Environment.inApplet)
//			System.loadLibrary("linearAlgebra");
//	}
	
	double coordX;
	double coordY;
	private GreatCircleDistanceFunction distFunc = new GreatCircleDistanceFunction();
	
	Vektor2D() {
		coordX = 0;
		coordY = 0;
	}
	
	public Vektor2D(double in_coordX, double in_coordY) {
		coordX = in_coordX;
		coordY = in_coordY;
	}
	
	public Vektor2D(Vektor vektor) {
		coordX = ((Vektor2D) vektor).coordX;
		coordY = ((Vektor2D) vektor).coordY;
	}
	
	Vektor2D(StringTokenizer t) throws NoSuchElementException, NumberFormatException {
		coordX = new Double(t.nextToken());
		coordY = new Double(t.nextToken());
	}
	
	final public void SetCoords(double in_coordX, double in_coordY) {
		coordX = in_coordX;
		coordY = in_coordY;
	}
	
	// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// interface Vektor BEGIN
	// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	final public double GetCoords(int i) {
		if (i == 0)
			return coordX;
		else if (i == 1)
			return coordY;
		else
			throw new RuntimeException("Dimension error: d = 2, i = " + i);
	}
	
	@Override
	public Vektor Clone() {
		return new Vektor2D(coordX, coordY);
	}
	
	@Override
	public Vektor DefaultClone() {
		return new Vektor2D();
	}
	
	@Override
	final public int Dimension() {
		return 2;
	}
	
	@Override
	final public CovarianceMatrix CovarianceMatrixClone() {
		return new CovarianceMatrix2D();
	}
	
	@Override
	final public void Update(Vektor vektor) {
		coordX = ((Vektor2D) vektor).coordX;
		coordY = ((Vektor2D) vektor).coordY;
	}
	
	@Override
	final public Vektor Add(Vektor vektor) {
		return new Vektor2D(coordX + ((Vektor2D) vektor).coordX, coordY + ((Vektor2D) vektor).coordY);
	}
	
	@Override
	final public Vektor Sub(Vektor vektor) {
		return new Vektor2D(coordX - ((Vektor2D) vektor).coordX, coordY - ((Vektor2D) vektor).coordY);
	}
	
	@Override
	final public Vektor Mul(double d) {
		return new Vektor2D(d * coordX, d * coordY);
	}
	
	@Override
	final public Vektor Div(double d) {
		return new Vektor2D(coordX / d, coordY / d);
	}
	
	@Override
	final public double Mul(Vektor vektor) {
		return coordX * ((Vektor2D) vektor).coordX + coordY * ((Vektor2D) vektor).coordY;
	}
	
	@Override
	final public void AddEqual(Vektor vektor) {
		coordX += ((Vektor2D) vektor).coordX;
		coordY += ((Vektor2D) vektor).coordY;
	}
	
	@Override
	final public void SubEqual(Vektor vektor) {
		coordX -= ((Vektor2D) vektor).coordX;
		coordY -= ((Vektor2D) vektor).coordY;
	}
	
	@Override
	final public void MulEqual(double d) {
		coordX *= d;
		coordY *= d;
	}
	
	@Override
	final public void DivEqual(double d) {
		coordX /= d;
		coordY /= d;
	}
	
	@Override
	final public boolean equals(Object object) {
		try {
			Vektor vektor = (Vektor) object;
			if (2 != vektor.Dimension())
				return false;
			return MyMath.equals(coordX, vektor.GetCoords(0)) && MyMath.equals(coordY, vektor.GetCoords(1));
		} catch (ClassCastException e) {
			return false;
		}
	}
	
	@Override
	final public double Norm2() {
		return Math.sqrt(Norm2Squared());
	}
	
	@Override
	final public double Norm2Squared() {
		return coordX * coordX + coordY * coordY;
	}
	
	@Override
	final public double Dist2(Vektor vektor) {
		return distFunc.pointToPointDistance(this.coordX, this.coordY, ((Vektor2D) vektor).coordX, ((Vektor2D) vektor).coordY);
	}
	
	@Override
	final public double Dist2Squared(Vektor vektor) {
		return Math.pow(Dist2(vektor), 2);
		
	}
	
	@Override
	final public double Dist2(LineSegment lineSegment) {
		Vektor2D point1 = (Vektor2D) lineSegment.GetVektor1();
		Vektor2D point2 = (Vektor2D) lineSegment.GetVektor2();
		double coordX1 = point1.coordX;
		double coordY1 = point1.coordY;
		double coordX2 = point2.coordX;
		double coordY2 = point2.coordY;
		return distFunc.pointToSegmentProjectionDistance(coordX, coordY, coordX1, coordY1, coordX2, coordY2);
	}
	
	@Override
	final public double Dist2Squared(LineSegment lineSegment) {
		return Math.pow(Dist2(lineSegment), 2);
	}
	
	@Override
	final public Vektor Project(LineSegment lineSegment) {
		Vektor2D point1 = (Vektor2D) lineSegment.GetVektor1();
		Vektor2D point2 = (Vektor2D) lineSegment.GetVektor2();
		double coordX1 = point1.coordX;
		double coordY1 = point1.coordY;
		double coordX2 = point2.coordX;
		double coordY2 = point2.coordY;
		Point resultPoint = distFunc.getClosestPoint(coordX, coordY, coordX1, coordY1, coordX2, coordY2);
		return new Vektor2D(resultPoint.x(), resultPoint.y());
	}
	
	@Override
	final public Vektor Project(Line line) {
		Vektor2D point1 = (Vektor2D) line.GetVektor1();
		Vektor2D point2 = (Vektor2D) line.GetVektor2();
		if (point1.equals(point2))
			return point1.Clone();
		Point resultPoint = distFunc.getProjection(coordX, coordY, point1.coordX, point1.coordY, point2.coordX, point2.coordY);
		return new Vektor2D(resultPoint.x(), resultPoint.y());
	}
	
	@Override
	public String toString() {
		return "(" + MyMath.RoundDouble(coordX, 4) + "," + MyMath.RoundDouble(coordY, 4) + ")";
	}
	
	@Override
	public void Save(PrintStream pOut) {
		pOut.print(coordX + " " + coordY);
	}
	
	@Override
	public String SaveToString() {
		return (coordX + " " + coordY);
	}
	
	// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// interface Vektor END
	// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
}
