package algorithm.mapinference.trajectoryclustering.pcurves.PrincipalCurve.Vertex.Elements;

import algorithm.mapinference.trajectoryclustering.pcurves.LinearAlgebra.Vektor;

final public class MiddleVertexOfThree extends ElementVertex implements HasTwoSymmetricEdges {
    private int edgeIndex1;
    private boolean forward1;
    private int edgeIndex2;
    private boolean forward2;

    public MiddleVertexOfThree(int in_edgeIndex1, int in_edgeIndex2) {
        edgeIndex1 = in_edgeIndex1;
        edgeIndex2 = in_edgeIndex2;

        if (GetEdgeVertexIndex1At(edgeIndex1) == GetEdgeVertexIndex1At(edgeIndex2)) {
            forward1 = true;
            forward2 = true;
        } else if (GetEdgeVertexIndex1At(edgeIndex1) == GetEdgeVertexIndex2At(edgeIndex2)) {
            forward1 = true;
            forward2 = false;
        } else if (GetEdgeVertexIndex2At(edgeIndex1) == GetEdgeVertexIndex1At(edgeIndex2)) {
            forward1 = false;
            forward2 = true;
        } else if (GetEdgeVertexIndex2At(edgeIndex1) == GetEdgeVertexIndex2At(edgeIndex2)) {
            forward1 = false;
            forward2 = false;
        } else
            throw (new ElementInitializationException("\nedge1:\n" + GetEdgeAt(edgeIndex1) + "\nedge2:\n"
                    + GetEdgeAt(edgeIndex2) + "\n"));
    }

    public MiddleVertexOfThree(EndVertexOfTwo endVertex1, EndVertexOfTwo endVertex2) {
        edgeIndex1 = endVertex1.edgeIndex1;
        forward1 = endVertex1.forward1;
        edgeIndex2 = endVertex2.edgeIndex1;
        forward2 = endVertex2.forward1;
    }

    final Edge GetEdge1() {
        if (forward1)
            return GetEdgeAt(edgeIndex1);
        else
            return GetEdgeAt(edgeIndex1).Reverse();
    }

    final Edge GetEdge2() {
        if (forward2)
            return GetEdgeAt(edgeIndex2);
        else
            return GetEdgeAt(edgeIndex2).Reverse();
    }

    final public int GetEdgeIndex1() {
        return edgeIndex1;
    }

    final public int GetEdgeIndex2() {
        return edgeIndex2;
    }

    @Override
    final public Vertex GetVertex() {
        return GetEdge1().GetVertex1();
    }

    @Override
    final public EdgeDirection GetEdgeIndex(Vertex vertex) {
        if (GetEdge1().GetVertex2().equals(vertex)) {
            return new EdgeDirection(edgeIndex1, forward1);
        } else if (GetEdge2().GetVertex2().equals(vertex)) {
            return new EdgeDirection(edgeIndex2, forward2);
        } else
            throw (new ArithmeticException("\nvertex:\n" + vertex + "\nedge1Vertex2\n" + GetEdge1().GetVertex2()
                    + "\nedge2Vertex2\n" + GetEdge2().GetVertex2() + "\n"));
    }

    @Override
    final public EdgeDirection GetOppositeEdgeIndex(Vertex vertex) {
        if (GetEdge1().GetVertex2().equals(vertex)) {
            return new EdgeDirection(edgeIndex2, forward2);
        } else if (GetEdge2().GetVertex2().equals(vertex)) {
            return new EdgeDirection(edgeIndex1, forward1);
        } else
            throw (new ArithmeticException("\nvertex:\n" + vertex + "\nedge1Vertex2\n" + GetEdge1().GetVertex2()
                    + "\nedge2Vertex2\n" + GetEdge2().GetVertex2() + "\n"));
    }

    @Override
    final public Vertex GetOppositeVertex(Vertex vertex) {
        if (GetEdge1().GetVertex2().equals(vertex)) {
            return GetEdge2().GetVertex2();
        } else if (GetEdge2().GetVertex2().equals(vertex)) {
            return GetEdge1().GetVertex2();
        } else
            throw (new ArithmeticException("\nvertex:\n" + vertex + "\nedge1Vertex2\n" + GetEdge1().GetVertex2()
                    + "\nedge2Vertex2\n" + GetEdge2().GetVertex2() + "\n"));
    }

    @Override
    final public void ReplaceEdgeIndexesForInsertion(int oei, int nei1, int nei2, Vertex vertex) {
        if (edgeIndex1 == oei) {
            if (GetEdge2().GetVertex1().equals(GetEdgeAt(nei1).GetVertex1())) {
                forward1 = true;
                edgeIndex1 = nei1;
            } else if (GetEdge2().GetVertex1().equals(GetEdgeAt(nei1).GetVertex2())) {
                forward1 = false;
                edgeIndex1 = nei1;
            } else if (GetEdge2().GetVertex1().equals(GetEdgeAt(nei2).GetVertex1())) {
                forward1 = true;
                edgeIndex1 = nei2;
            } else if (GetEdge2().GetVertex1().equals(GetEdgeAt(nei2).GetVertex2())) {
                forward1 = false;
                edgeIndex1 = nei2;
            } else
                throw (new ArithmeticException("\nGetEdge2().GetVertex1():\n" + GetEdge2().GetVertex1()
                        + "\nedges[nei1].GetVertex1()\n" + GetEdgeAt(nei1).GetVertex1()
                        + "\nedges[nei1].GetVertex2()\n" + GetEdgeAt(nei1).GetVertex2()
                        + "\nedges[nei2].GetVertex1()\n" + GetEdgeAt(nei2).GetVertex1()
                        + "\nedges[nei2].GetVertex2()\n" + GetEdgeAt(nei2).GetVertex2() + "\n"));
        }
        if (edgeIndex2 == oei) {
            if (GetEdge1().GetVertex1().equals(GetEdgeAt(nei1).GetVertex1())) {
                forward2 = true;
                edgeIndex2 = nei1;
            } else if (GetEdge1().GetVertex1().equals(GetEdgeAt(nei1).GetVertex2())) {
                forward2 = false;
                edgeIndex2 = nei1;
            } else if (GetEdge1().GetVertex1().equals(GetEdgeAt(nei2).GetVertex1())) {
                forward2 = true;
                edgeIndex2 = nei2;
            } else if (GetEdge1().GetVertex1().equals(GetEdgeAt(nei2).GetVertex2())) {
                forward2 = false;
                edgeIndex2 = nei2;
            } else
                throw (new ArithmeticException("\nGetEdge1().GetVertex1():\n" + GetEdge1().GetVertex1()
                        + "\nedges[nei1].GetVertex1()\n" + GetEdgeAt(nei1).GetVertex1()
                        + "\nedges[nei1].GetVertex2()\n" + GetEdgeAt(nei1).GetVertex2()
                        + "\nedges[nei2].GetVertex1()\n" + GetEdgeAt(nei2).GetVertex1()
                        + "\nedges[nei2].GetVertex2()\n" + GetEdgeAt(nei2).GetVertex2() + "\n"));
        }
    }

    @Override
    final public void ReplaceEdgeIndexesForDeletion(int oei, int nei) {
        if (edgeIndex1 == oei) {
            if (GetVertex().equals(GetEdgeAt(nei).GetVertex1())) {
                forward1 = true;
                edgeIndex1 = nei;
            } else if (GetVertex().equals(GetEdgeAt(nei).GetVertex2())) {
                forward1 = false;
                edgeIndex1 = nei;
            } else
                throw (new ArithmeticException("\nGetEdge1().GetVertex1():\n" + GetEdge1().GetVertex1()
                        + "\nedges[nei].GetVertex1()\n" + GetEdgeAt(nei).GetVertex1() + "\nedges[nei].GetVertex2()\n"
                        + GetEdgeAt(nei).GetVertex2()));
        } else if (edgeIndex2 == oei) {
            if (GetVertex().equals(GetEdgeAt(nei).GetVertex1())) {
                forward2 = true;
                edgeIndex2 = nei;
            } else if (GetVertex().equals(GetEdgeAt(nei).GetVertex2())) {
                forward2 = false;
                edgeIndex2 = nei;
            } else
                throw (new ArithmeticException("\nGetVertex():\n" + GetVertex() + "\nedges[nei].GetVertex1()\n"
                        + GetEdgeAt(nei).GetVertex1() + "\nedges[nei].GetVertex2()\n" + GetEdgeAt(nei).GetVertex2()));
        }
    }

    @Override
    final public void DecrementEdgeIndexes(int lowerIndex) {
        if (edgeIndex1 > lowerIndex)
            edgeIndex1--;
        if (edgeIndex2 > lowerIndex)
            edgeIndex2--;
    }

    @Override
    final public void SetVertexIndexOfEdges(int vi) {
        if (forward1)
            GetEdgeAt(edgeIndex1).SetVertexIndex1(vi);
        else
            GetEdgeAt(edgeIndex1).SetVertexIndex2(vi);
        if (forward2)
            GetEdgeAt(edgeIndex2).SetVertexIndex1(vi);
        else
            GetEdgeAt(edgeIndex2).SetVertexIndex2(vi);
    }

    final public double GetAnglePenalty() {
        return (PenaltyCoefficients.ANGLE_PENALTY_COEFFICIENT * (1 + GetVertex().CosAngle(GetEdge1().GetVertex2(),
                GetEdge2().GetVertex2())));
    }

    // for CornerVertex
    final public double GetAnglePenaltyForRectangle() {
        double cosAngle = GetVertex().CosAngle(GetEdge1().GetVertex2(), GetEdge2().GetVertex2());
        return 2 * cosAngle * cosAngle;
    }

    final public NumeratorAndDenominator GetNumeratorAndDenominatorForAnglePenalty() {
        NumeratorAndDenominator nd = new NumeratorAndDenominator(Vertex.prototypeVektor);

        Vektor c = GetVertex();
        Vektor a = GetEdge1().GetVertex2();
        Vektor b = GetEdge2().GetVertex2();

        Vektor Aa = c.Sub(a);
        Vektor Ab = c.Sub(b);
        double Ba = Aa.Mul(Aa);
        double Bb = Ab.Mul(Ab);
        double Fa = Aa.Mul(Ab) / Ba;
        double Fb = Aa.Mul(Ab) / Bb;
        double J = 1 / Math.sqrt(Ba * Bb);

        nd.numerator.AddEqual(a.Mul(1 - Fa).Add(b.Mul(1 - Fb)).Mul(PenaltyCoefficients.ANGLE_PENALTY_COEFFICIENT * J));
        nd.denominator += PenaltyCoefficients.ANGLE_PENALTY_COEFFICIENT * J * (2 - Fa - Fb);

        return nd;
    }

    // for CornerVertex
    final public NumeratorAndDenominator GetNumeratorAndDenominatorForAnglePenaltyForRectangle() {
        NumeratorAndDenominator nd = GetNumeratorAndDenominatorForAnglePenalty();
        nd.MulEqual(0.5 * GetVertex().CosAngle(GetEdge1().GetVertex2(), GetEdge2().GetVertex2()));
        return nd;
    }

    private int GetSetSizeSum() {
        return GetVertex().GetSetSize() + GetEdgeAt(edgeIndex1).GetSetSize() + GetEdgeAt(edgeIndex2).GetSetSize();
    }

    private double GetSetWeightSum() {
        return GetVertex().GetSetWeight() + GetEdgeAt(edgeIndex1).GetSetWeight() + GetEdgeAt(edgeIndex2).GetSetWeight();
    }

    @Override
    final public boolean EmptySet() {
        return GetSetSizeSum() == 0;
    }

    final public NumeratorAndDenominator GetNumeratorAndDenominatorForWeightDifferencePenalty() {
        NumeratorAndDenominator nd = new NumeratorAndDenominator(Vertex.prototypeVektor);

        if (GetSetSizeSum() == 0 || GetSetWeightSum() == 0)
            return nd;

        double num_1 =
                PenaltyCoefficients.WEIGHT_DIFFERENCE_PENALTY_COEFFICIENT
                        * (GetEdgeAt(edgeIndex1).GetSetWeight() - GetEdgeAt(edgeIndex2).GetSetWeight())
                        / (GetSetWeightSum());
        nd.numerator.AddEqual(GetEdge1().GetVertex2().Sub(GetEdge2().GetVertex2()).Mul(num_1));

        return nd;
    }

    @Override
    final public NumeratorAndDenominator GetNumeratorAndDenominatorForMSE() {
        NumeratorAndDenominator nd = GetEdge1().GetNumeratorAndDenominatorForMSETimesWeight();
        nd.AddEqual(GetVertex().GetNumeratorAndDenominatorForMSETimesWeight());
        nd.AddEqual(GetEdge2().GetNumeratorAndDenominatorForMSETimesWeight());
        return nd;
    }

    @Override
    final public String toString() {
        String s = new String();
        s += "\t" + edgeIndex1 + ":" + GetEdge1().toString();
        s += "," + edgeIndex2 + ":" + GetEdge2().toString();
        return s;
    }
}
