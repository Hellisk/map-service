package mapupdate.mapinference.trajectoryclustering.pcurves.LinearAlgebra;

import mapupdate.mapinference.trajectoryclustering.pcurves.Utilities.MyMath;

import java.io.PrintStream;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;


final public class Vektor2DWeighted extends Vektor2D implements Weighted {
    private static double maxWeight = 1.0; // for painting
    private double weight;

    private Vektor2DWeighted() {
        super();
        weight = 1;
    }

    private Vektor2DWeighted(double in_coordX, double in_coordY, double in_weight) {
        super(in_coordX, in_coordY);
        weight = in_weight;
    }

    public Vektor2DWeighted(StringTokenizer t) throws NoSuchElementException, NumberFormatException {
        super(new Double(t.nextToken()), new Double(t.nextToken()));
        weight = new Double(t.nextToken());
    }

    @Override
    final public Vektor Clone() {
        return new Vektor2DWeighted(coordX, coordY, weight);
    }

    @Override
    final public Vektor DefaultClone() {
        return new Vektor2DWeighted();
    }

    @Override
    final public double GetWeight() {
        return weight;
    }

    final public void SetWeight(double in_weight) {
        weight = in_weight;
    }

    @Override
    final public String toString() {
        return super.toString() + "w = " + MyMath.RoundDouble(weight, 4);
    }

    @Override
    final public void Save(PrintStream pOut) {
        super.Save(pOut);
        pOut.print(" " + weight);
    }

    @Override
    final public String SaveToString() {
        return super.SaveToString() + " " + weight;
    }

}
