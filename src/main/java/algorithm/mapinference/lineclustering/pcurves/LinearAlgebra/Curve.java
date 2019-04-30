package algorithm.mapinference.lineclustering.pcurves.LinearAlgebra;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Vector;

public class Curve extends GraphAbstract {
	private Vector<Double> parameters;
	private int unitParametrizedUntilIndex;
	
	protected Curve() {
		super();
		parameters = new Vector<>(100);
		unitParametrizedUntilIndex = -1;
	}
	
	public Curve(Sample sample) {
		super(sample);
		parameters = new Vector<>(getSize());
		for (int i = 0; i < getSize(); i++)
			parameters.addElement(new Double(0));
		unitParametrizedUntilIndex = -1;
	}
	
	@Override
	public Sample Clone() {
		Curve curve = new Curve(super.Clone());
		curve.parameters = new Vector<>(getSize());
		for (int i = 0; i < unitParametrizedUntilIndex; i++)
			curve.parameters.setElementAt(parameters.elementAt(i), i);
		curve.unitParametrizedUntilIndex = unitParametrizedUntilIndex;
		return curve;
	}
	
	@Override
	public Sample ShallowClone() {
		Curve curve = new Curve(super.ShallowClone());
		curve.parameters = parameters;
		curve.unitParametrizedUntilIndex = unitParametrizedUntilIndex;
		return curve;
	}
	
	@Override
	public Sample DefaultClone() {
		return new Curve(new Sample());
	}
	
	@Override
	final public void Reset() {
		super.Reset();
		parameters.setSize(0);
		unitParametrizedUntilIndex = -1;
	}
	
	@Override
	final public void InsertPointAt(Vektor vektor, int index) {
		super.InsertPointAt(vektor, index);
		parameters.insertElementAt(new Double(0), index);
		if (unitParametrizedUntilIndex >= index)
			unitParametrizedUntilIndex = index - 1;
		
	}
	
	@Override
	final public void DeletePointAt(int index) {
		super.DeletePointAt(index);
		if (unitParametrizedUntilIndex >= index)
			unitParametrizedUntilIndex = index - 1;
	}
	
	@Override
	final public void UpdatePointAt(Vektor vektor, int index) {
		super.UpdatePointAt(vektor, index);
		if (unitParametrizedUntilIndex >= index)
			unitParametrizedUntilIndex = index - 1;
	}
	
	@Override
	final public void SetPointAt(Vektor vektor, int index) {
		super.SetPointAt(vektor, index);
		if (unitParametrizedUntilIndex >= index)
			unitParametrizedUntilIndex = index - 1;
	}
	
	private void SetParameterAt(double parameter, int index) {
		parameters.setElementAt(new Double(parameter), index);
	}
	
	final public double GetParameterAt(int index) {
		return parameters.elementAt(index);
	}
	
	// Unit-speed (Euclidean distance)
	final public void UnitParametrize() {
		if (unitParametrizedUntilIndex < getSize() - 1) {
			double t;
			if (unitParametrizedUntilIndex == -1)
				t = 0.0;
			else
				t = GetParameterAt(unitParametrizedUntilIndex);
			for (int i = unitParametrizedUntilIndex + 1; i < getSize(); i++) {
				SetParameterAt(t, i);
				if (i < getSize() - 1)
					t += GetPointAt(i).Dist2(GetPointAt(i + 1));
			}
			unitParametrizedUntilIndex = getSize() - 1;
		}
	}
	
	final double GetLength() {
		UnitParametrize();
		if (getSize() > 0)
			return GetParameterAt(getSize() - 1);
		else
			return 0;
	}
	
	final Vektor GetPointAtParameter(double parameter) {
		UnitParametrize();
		if (parameter < 0)
			parameter = 0;
		else if (getSize() > 0 && parameter > GetParameterAt(getSize() - 1))
			parameter = GetParameterAt(getSize() - 1);
		for (int i = 0; i < getSize(); i++) {
			if (GetParameterAt(i) == parameter)
				return GetPointAt(i).Clone();
			else if (GetParameterAt(i) > parameter)
				return GetLineSegmentAt(i - 1).GetPointAtParameter(GetParameterAt(i) - parameter);
		}
		// just for the compiler...
		return GetPointAt(getSize() - 1).Clone();
	}
	
	final public Vektor GetPointAtRandomParameter() {
		return GetPointAtParameter(GetLength() * Math.random());
	}
	
	// should go to subclass
	// final public Vektor GetPointAtRandomParameter(Functions.Function f) {
	// double y0 = f.Phi(0);
	// double y1 = f.Phi(1);
	// Functions.Function distribution =
	// new Functions.ScaleYFunction(1/(y1 - y0),new Functions.TranslateYFunction(-y0,f));
	// return GetPointAtParameter(GetLength() * distribution.Phi(Math.random()));
	// }
	
	// Overwriting GraphAbstract's Save
	@Override
	public void Save(String fileName) {
		try {
			FileOutputStream fOut = new FileOutputStream(fileName);
			PrintStream pOut = new PrintStream(fOut);
			for (int i = 0; i < getSize(); i++) {
				GetPointAt(i).Save(pOut);
				pOut.println();
			}
			pOut.close();
			fOut.close();
		} catch (IOException e) {
			System.out.println("Can't open file " + fileName);
		}
	}
	
	// Overwriting GraphAbstract's SaveToString
	@Override
	final public String SaveToString() {
		String str = new String();
		for (int i = 0; i < getSize(); i++)
			str += GetPointAt(i).SaveToString() + "\n";
		return str;
	}
	
	// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// abstract functions from GraphAbstract BEGIN
	// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	final public int GetVektorIndex1OfEdgeAt(int index) {
		return index;
	}
	
	@Override
	final public int GetVektorIndex2OfEdgeAt(int index) {
		return index + 1;
	}
	
	@Override
	final public int GetNumOfLineSegments() {
		return getSize() - 1;
	}
	
	// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// abstract functions from GraphAbstract END
	// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
}
