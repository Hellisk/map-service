package mapupdate.mapinference.trajectoryclustering.pcurves.PrincipalCurve;

import mapupdate.mapinference.trajectoryclustering.pcurves.LinearAlgebra.Curve;
import mapupdate.mapinference.trajectoryclustering.pcurves.LinearAlgebra.Sample;
import mapupdate.mapinference.trajectoryclustering.pcurves.LinearAlgebra.Vektor;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Vector;

final public class SetOfCurves {
    Vector<Sample> curves;

    public SetOfCurves() {
        curves = new Vector<>(1);
    }

    final public SetOfCurves Clone() {
        SetOfCurves setOfCurves = new SetOfCurves();
        for (int i = 0; i < GetNumOfCurves(); i++)
            setOfCurves.curves.addElement(GetCurveAt(i).Clone());
        return setOfCurves;
    }

    final public void AddPoint(Vektor vektor) {
        curves.lastElement().AddPoint(vektor);
    }

    private Curve GetLastCurve() {
        return (Curve) curves.lastElement();
    }

    final public Curve GetCurveAt(int index) {
        return (Curve) curves.elementAt(index);
    }

    final public int GetNumOfCurves() {
        return curves.size();
    }

    final public void StartNewCurve() {
        curves.addElement(new Curve(new Sample()));
    }

    final public void Reset() {
        curves = new Vector<>(1);
        curves.addElement(new Curve(new Sample()));
    }

    final public void UpdateLastPoint(Vektor vektor) {
        GetLastCurve().UpdatePointAt(vektor, GetLastCurve().getSize() - 1);
    }

    final public boolean Valid() {
        boolean valid = true;
        for (int i = 0; i < GetNumOfCurves(); i++)
            valid = valid && GetCurveAt(i).getSize() >= 2;
        return valid;
    }

    final public void Save(String fileName) {
        try {
            FileOutputStream fOut = new FileOutputStream(fileName);
            PrintStream pOut = new PrintStream(fOut);

            pOut.print("y x z\n");

            for (int i = 0; i < GetNumOfCurves(); i++) {
                for (int j = 0; j < GetCurveAt(i).getSize(); j++) {
                    GetCurveAt(i).GetPointAt(j).Save(pOut);
                    pOut.println();
                }
                // pOut.println();
            }
            pOut.close();
            fOut.close();
        } catch (IOException e) {
            System.out.println("Can't open file " + fileName);
        }
    }

    final public String SaveToString() {
        String str = new String();
        for (int i = 0; i < GetNumOfCurves(); i++) {
            for (int j = 0; j < GetCurveAt(i).getSize(); j++)
                str += GetCurveAt(i).GetPointAt(j).SaveToString() + "\n";
            str += "\n";
        }
        return str;
    }

    @Override
    final public String toString() {
        String s = new String();
        for (int i = 0; i < GetNumOfCurves(); i++) {
            s += "Curve " + i + ":\n";
            s += GetCurveAt(i).toString();
            s += "\n";
        }
        return s;
    }
}
