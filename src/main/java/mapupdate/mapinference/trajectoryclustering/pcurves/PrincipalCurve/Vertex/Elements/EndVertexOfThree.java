package mapupdate.mapinference.trajectoryclustering.pcurves.PrincipalCurve.Vertex.Elements;

import mapupdate.mapinference.trajectoryclustering.pcurves.LinearAlgebra.Vektor;

public class EndVertexOfThree extends EndVertexOfTwo {
    int edgeIndex2;
    boolean forward2;

    public EndVertexOfThree(Vertex endVertex, int in_edgeIndex1, int in_edgeIndex2) {
        super(endVertex, in_edgeIndex1);

        edgeIndex2 = in_edgeIndex2;

        if (GetEdge1().GetVertex2() == GetEdgeAt(edgeIndex2).GetVertex1())
            forward2 = true;
        else if (GetEdge1().GetVertex2() == GetEdgeAt(edgeIndex2).GetVertex2())
            forward2 = false;
        else
            throw (new ElementInitializationException("\nedge1:\n" + GetEdge1() + "\nin_edge2:\n"
                    + GetEdgeAt(edgeIndex2) + "\n"));
    }

    final Edge GetEdge2() {
        if (forward2)
            return GetEdgeAt(edgeIndex2);
        else
            return GetEdgeAt(edgeIndex2).Reverse();
    }

    final public int GetEdgeIndex2() {
        return edgeIndex2;
    }

    @Override
    public void ReplaceEdgeIndexesForInsertion(int oei, int nei1, int nei2, Vertex vertex) {
        if (edgeIndex1 == oei || edgeIndex2 == oei) {
            super.ReplaceEdgeIndexesForInsertion(oei, nei1, nei2, vertex);
            if (GetEdge1().GetVertex2().equals(GetEdgeAt(nei1).GetVertex1())
                    && !(vertex.equals(GetEdgeAt(nei1).GetVertex2()))) {
                forward2 = true;
                edgeIndex2 = nei1;
            } else if (GetEdge1().GetVertex2().equals(GetEdgeAt(nei1).GetVertex2())
                    && !(vertex.equals(GetEdgeAt(nei1).GetVertex1()))) {
                forward2 = false;
                edgeIndex2 = nei1;
            } else if (GetEdge1().GetVertex2().equals(GetEdgeAt(nei2).GetVertex1())
                    && !(vertex.equals(GetEdgeAt(nei2).GetVertex2()))) {
                forward2 = true;
                edgeIndex2 = nei2;
            } else if (GetEdge1().GetVertex2().equals(GetEdgeAt(nei2).GetVertex2())
                    && !(vertex.equals(GetEdgeAt(nei2).GetVertex1()))) {
                forward2 = false;
                edgeIndex2 = nei2;
            } else
                throw (new ArithmeticException("\nedgeVertex2:\n" + GetEdge1().GetVertex2()
                        + "\nedges[nei1].GetVertex1()\n" + GetEdgeAt(nei1).GetVertex1()
                        + "\nedges[nei1].GetVertex2()\n" + GetEdgeAt(nei1).GetVertex2()
                        + "\nedges[nei2].GetVertex1()\n" + GetEdgeAt(nei2).GetVertex1()
                        + "\nedges[nei2].GetVertex2()\n" + GetEdgeAt(nei2).GetVertex2() + "\n"));
        }
    }

    @Override
    public void ReplaceEdgeIndexesForDeletion(int oei, int nei) {
        super.ReplaceEdgeIndexesForDeletion(oei, nei);
        if (edgeIndex2 == oei) {
            if (GetEdge1().GetVertex2().equals(GetEdgeAt(nei).GetVertex1())) {
                forward2 = true;
                edgeIndex2 = nei;
            } else if (GetEdge1().GetVertex2().equals(GetEdgeAt(nei).GetVertex2())) {
                forward2 = false;
                edgeIndex2 = nei;
            } else
                throw (new ArithmeticException("\nGetEdge1().GetVertex2():\n" + GetEdge1().GetVertex2()
                        + "\nedges[nei].GetVertex1()\n" + GetEdgeAt(nei).GetVertex1() + "\nedges[nei].GetVertex2()\n"
                        + GetEdgeAt(nei).GetVertex2()));
        }
    }

    @Override
    public void DecrementEdgeIndexes(int lowerIndex) {
        super.DecrementEdgeIndexes(lowerIndex);
        if (edgeIndex2 > lowerIndex)
            edgeIndex2--;
    }

    public double GetAnglePenalty() {
        return (PenaltyCoefficients.ANGLE_PENALTY_COEFFICIENT * (1 + GetEdge1().GetVertex2().CosAngle(GetVertex(),
                GetEdge2().GetVertex2())));
    }

    public NumeratorAndDenominator GetNumeratorAndDenominatorForAnglePenalty() {
        NumeratorAndDenominator nd = new NumeratorAndDenominator(Vertex.prototypeVektor);

        Vektor c = GetVertex();
        Vektor a = GetEdge1().GetVertex2();
        Vektor d = GetEdge2().GetVertex2();

        Vektor Ac = a.Sub(c);
        Vektor Ad = a.Sub(d);
        double Bc = Ac.Mul(Ac);
        double Bd = Ad.Mul(Ad);
        double Fc = Ac.Mul(Ad) / Bc;
        if (Fc > -0.2) {
            nd.numerator.AddEqual(Ad.Add(a).Mul(PenaltyCoefficients.ANGLE_PENALTY_COEFFICIENT));
            nd.denominator += PenaltyCoefficients.ANGLE_PENALTY_COEFFICIENT;
        } else {
            double J = 1 / Math.sqrt(Bc * Bd);
            nd.numerator.AddEqual(Ad.Sub(a.Mul(Fc)).Mul(PenaltyCoefficients.ANGLE_PENALTY_COEFFICIENT * J));
            nd.denominator += PenaltyCoefficients.ANGLE_PENALTY_COEFFICIENT * -J * Fc;
        }
        return nd;
    }

    @Override
    public String toString() {
        String s = super.toString();
        s += "," + edgeIndex2 + ":" + GetEdge2().toString();
        return s;
    }
}
