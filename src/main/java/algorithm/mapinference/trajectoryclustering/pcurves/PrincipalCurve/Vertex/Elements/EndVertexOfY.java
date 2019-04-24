package algorithm.mapinference.trajectoryclustering.pcurves.PrincipalCurve.Vertex.Elements;

final public class EndVertexOfY extends EndVertexOfThree {
    private int edgeIndex3;
    private boolean forward3;

    public EndVertexOfY(Vertex endVertex, int in_edgeIndex1, int in_edgeIndex2, int in_edgeIndex3) {
        super(endVertex, in_edgeIndex1, in_edgeIndex2);

        edgeIndex3 = in_edgeIndex3;

        if (GetEdge1().GetVertex2() == GetEdgeAt(edgeIndex3).GetVertex1())
            forward3 = true;
        else if (GetEdge1().GetVertex2() == GetEdgeAt(edgeIndex3).GetVertex2())
            forward3 = false;
        else
            throw (new ElementInitializationException("\nedge1:\n" + GetEdge1() + "\nin_edge3:\n" + "\n"));
    }

    final Edge GetEdge3() {
        if (forward3)
            return GetEdgeAt(edgeIndex3);
        else
            return GetEdgeAt(edgeIndex3).Reverse();
    }

    final public int GetEdgeIndex3() {
        return edgeIndex3;
    }

    // For insertion
    @Override
    final public void ReplaceEdgeIndexesForInsertion(int oei, int nei1, int nei2, Vertex vertex) {
        if (edgeIndex1 == oei) {
            if (vertex.equals(GetEdgeAt(nei1).GetVertex1())) {
                forward1 = true;
                edgeIndex1 = nei1;
            } else if (vertex.equals(GetEdgeAt(nei1).GetVertex2())) {
                forward1 = false;
                edgeIndex1 = nei1;
            } else if (vertex.equals(GetEdgeAt(nei2).GetVertex1())) {
                forward1 = true;
                edgeIndex1 = nei2;
            } else if (vertex.equals(GetEdgeAt(nei2).GetVertex2())) {
                forward1 = false;
                edgeIndex1 = nei2;
            }
        } else if (edgeIndex2 == oei) {
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

                throw (new ArithmeticException("edgeVertex2:\n" + GetEdge1().GetVertex2()
                        + "edges[nei1].GetVertex1()\n" + GetEdgeAt(nei1).GetVertex1() + "edges[nei1].GetVertex2()\n"
                        + GetEdgeAt(nei1).GetVertex2() + "edges[nei2].GetVertex1()\n" + GetEdgeAt(nei2).GetVertex1()
                        + "edges[nei2].GetVertex2()\n" + GetEdgeAt(nei2).GetVertex2() + "\n"));
        } else if (edgeIndex3 == oei) {
            if (GetEdge1().GetVertex2().equals(GetEdgeAt(nei1).GetVertex1())
                    && !(vertex.equals(GetEdgeAt(nei1).GetVertex2()))) {
                forward3 = true;
                edgeIndex3 = nei1;
            } else if (GetEdge1().GetVertex2().equals(GetEdgeAt(nei1).GetVertex2())
                    && !(vertex.equals(GetEdgeAt(nei1).GetVertex1()))) {
                forward3 = false;
                edgeIndex3 = nei1;
            } else if (GetEdge1().GetVertex2().equals(GetEdgeAt(nei2).GetVertex1())
                    && !(vertex.equals(GetEdgeAt(nei2).GetVertex2()))) {
                forward3 = true;
                edgeIndex3 = nei2;
            } else if (GetEdge1().GetVertex2().equals(GetEdgeAt(nei2).GetVertex2())
                    && !(vertex.equals(GetEdgeAt(nei2).GetVertex1()))) {
                forward3 = false;
                edgeIndex3 = nei2;
            } else
                throw (new ArithmeticException("\nedgeVertex2:\n" + GetEdge1().GetVertex2()
                        + "\nedges[nei1].GetVertex1()\n" + GetEdgeAt(nei1).GetVertex1()
                        + "\nedges[nei1].GetVertex2()\n" + GetEdgeAt(nei1).GetVertex2()
                        + "\nedges[nei2].GetVertex1()\n" + GetEdgeAt(nei2).GetVertex1()
                        + "\nedges[nei2].GetVertex2()\n" + GetEdgeAt(nei2).GetVertex2() + "\n"));
        }
    }

    @Override
    final public void ReplaceEdgeIndexesForDeletion(int oei, int nei) {
        super.ReplaceEdgeIndexesForDeletion(oei, nei);
        if (edgeIndex3 == oei) {
            if (GetEdge1().GetVertex2().equals(GetEdgeAt(nei).GetVertex1())) {
                forward3 = true;
                edgeIndex3 = nei;
            } else if (GetEdge1().GetVertex2().equals(GetEdgeAt(nei).GetVertex2())) {
                forward3 = false;
                edgeIndex3 = nei;
            } else
                throw (new ArithmeticException("\nGetEdge1().GetVertex2():\n" + GetEdge1().GetVertex2()
                        + "\nedges[nei].GetVertex1()\n" + GetEdgeAt(nei).GetVertex1() + "\nedges[nei].GetVertex2()\n"
                        + GetEdgeAt(nei).GetVertex2()));
        }
    }

    @Override
    final public void DecrementEdgeIndexes(int lowerIndex) {
        super.DecrementEdgeIndexes(lowerIndex);
        if (edgeIndex3 > lowerIndex)
            edgeIndex3--;
    }

    @Override
    final public double GetAnglePenalty() {
        double penalty = super.GetAnglePenalty();

        Vertex c = GetVertex();
        Vertex a = GetEdge1().GetVertex2();
        Vertex d = GetEdge3().GetVertex2();

        Vertex ac = (Vertex) a.Sub(c);
        Vertex ad = (Vertex) a.Sub(d);

        penalty += PenaltyCoefficients.ANGLE_PENALTY_COEFFICIENT * (1 + ac.Mul(ad) / (ac.Norm2() * ad.Norm2()));
        return penalty;
    }

    @Override
    final public NumeratorAndDenominator GetNumeratorAndDenominatorForAnglePenalty() {
        NumeratorAndDenominator nd = super.GetNumeratorAndDenominatorForAnglePenalty();

        Vertex c = GetVertex();
        Vertex a = GetEdge1().GetVertex2();
        Vertex d = GetEdge3().GetVertex2();

        Vertex Ac = (Vertex) a.Sub(c);
        Vertex Ad = (Vertex) a.Sub(d);
        double Bc = Ac.Mul(Ac);
        double Bd = Ad.Mul(Ad);
        double Fc = Ac.Mul(Ad) / Bc;
        double J = 1 / Math.sqrt(Bc * Bd);

        nd.numerator.AddEqual(Ad.Sub(a.Mul(Fc)).Mul(PenaltyCoefficients.ANGLE_PENALTY_COEFFICIENT * J));
        nd.denominator += PenaltyCoefficients.ANGLE_PENALTY_COEFFICIENT * -J * Fc;

        return nd;
    }

    @Override
    final public String toString() {
        String s = super.toString();
        s += "," + edgeIndex3 + ":" + GetEdge3().toString();
        return s;
    }
}
