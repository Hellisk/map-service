package algorithm.mapinference.trajectoryclustering.pcurves.LinearAlgebra;

// This is an abstract class for all lines. The abstract functions are
// the minimum requirement for a line, the other functions in the
// Line inteface are derived from them. For efficiency reasons, these derived
// functions can be rewritten in extending classes.
abstract public class LineAbstract implements Line {
    @Override
    abstract public Vektor GetVektor1();

    @Override
    abstract public Vektor GetVektor2();

    abstract public void SetVektor1(Vektor vektor);

    abstract public void SetVektor2(Vektor vektor);

    @Override
    final public boolean equals(Object o) {
        try {
            Line line = (Line) o;
            Vektor vektor1 = GetVektor1().Sub(GetVektor2());
            Vektor vektor2 = line.GetVektor1().Sub(line.GetVektor2());
            Vektor vektor3 = GetVektor1().Sub(line.GetVektor1());
            vektor1.DivEqual(vektor1.Norm2());
            vektor2.DivEqual(vektor2.Norm2());
            vektor3.DivEqual(vektor3.Norm2());
            return ((vektor1.equals(vektor2) || vektor1.equals(vektor2.Mul(-1))) && (vektor1.equals(vektor3) || vektor1
                    .equals(vektor3.Mul(-1))));
        } catch (ClassCastException e) {
            return false;
        }
    }

    @Override
    public int hashCode() {
        // TODO Auto-generated method stub
        return super.hashCode();
    }
}
