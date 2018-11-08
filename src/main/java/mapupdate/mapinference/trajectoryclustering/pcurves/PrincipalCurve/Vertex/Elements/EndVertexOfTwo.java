package mapupdate.mapinference.trajectoryclustering.pcurves.PrincipalCurve.Vertex.Elements;

public class EndVertexOfTwo extends ElementVertex implements HasOneEdge {
    int edgeIndex1;
    boolean forward1;

    public EndVertexOfTwo(Vertex endVertex, int in_edgeIndex1) {
        edgeIndex1 = in_edgeIndex1;

        if (endVertex == GetEdgeAt(edgeIndex1).GetVertex1())
            forward1 = true;
        else if (endVertex == GetEdgeAt(edgeIndex1).GetVertex2())
            forward1 = false;
        else
            throw (new ElementInitializationException("\nvertex:\n" + endVertex + "\nin_edge:\n"
                    + GetEdgeAt(edgeIndex1) + "\n"));
    }

    final public Edge GetEdge1() {
        if (forward1)
            return GetEdgeAt(edgeIndex1);
        else
            return GetEdgeAt(edgeIndex1).Reverse();
    }

    final public int GetEdgeIndex1() {
        return edgeIndex1;
    }

    @Override
    final public Vertex GetVertex() {
        return GetEdge1().GetVertex1();
    }

    @Override
    final public EdgeDirection GetEdgeIndex(Vertex vertex) {
        if (GetEdge1().GetVertex2().equals(vertex)) {
            return new EdgeDirection(edgeIndex1, forward1);
        } else
            throw (new ArithmeticException("\nvertex:\n" + vertex + "\nedge1Vertex2\n" + GetEdge1().GetVertex2() + "\n"));
    }

    @Override
    public void ReplaceEdgeIndexesForInsertion(int oei, int nei1, int nei2, Vertex vertex) {
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
            } else
                throw (new ArithmeticException("\nGetVertex():\n" + vertex + "\nedges[nei1].GetVertex1()\n"
                        + GetEdgeAt(nei1).GetVertex1() + "\nedges[nei1].GetVertex2()\n" + GetEdgeAt(nei1).GetVertex2()
                        + "\nedges[nei2].GetVertex1()\n" + GetEdgeAt(nei2).GetVertex1()
                        + "\nedges[nei2].GetVertex2()\n" + GetEdgeAt(nei2).GetVertex2() + "\n"));
        }
    }

    @Override
    public void ReplaceEdgeIndexesForDeletion(int oei, int nei) {
        if (edgeIndex1 == oei) {
            if (GetVertex().equals(GetEdgeAt(nei).GetVertex1())) {
                forward1 = true;
                edgeIndex1 = nei;
            } else if (GetVertex().equals(GetEdgeAt(nei).GetVertex2())) {
                forward1 = false;
                edgeIndex1 = nei;
            } else
                throw (new ArithmeticException("\nGetVertex():\n" + GetVertex() + "\nedges[nei].GetVertex1()\n"
                        + GetEdgeAt(nei).GetVertex1() + "\nedges[nei].GetVertex2()\n" + GetEdgeAt(nei).GetVertex2()));
        }
    }

    @Override
    public void DecrementEdgeIndexes(int lowerIndex) {
        if (edgeIndex1 > lowerIndex)
            edgeIndex1--;
    }

    @Override
    final public void SetVertexIndexOfEdges(int vi) {
        if (forward1)
            GetEdgeAt(edgeIndex1).SetVertexIndex1(vi);
        else
            GetEdgeAt(edgeIndex1).SetVertexIndex2(vi);
    }

    final public double GetLengthPenalty() {
        return GetEdgeAt(edgeIndex1).GetLengthPenalty();
    }

    final public NumeratorAndDenominator GetNumeratorAndDenominatorForLengthPenalty() {
        return GetEdge1().GetNumeratorAndDenominatorForLengthPenalty();
    }

    private int GetSetSizeSum() {
        return GetVertex().GetSetSize() + GetEdgeAt(edgeIndex1).GetSetSize();
    }

    @Override
    final public boolean EmptySet() {
        return GetSetSizeSum() == 0;
    }

    @Override
    final public NumeratorAndDenominator GetNumeratorAndDenominatorForMSE() {
        NumeratorAndDenominator nd = GetVertex().GetNumeratorAndDenominatorForMSETimesWeight();
        nd.AddEqual(GetEdge1().GetNumeratorAndDenominatorForMSETimesWeight());
        return nd;
    }

    @Override
    public String toString() {
        String s = "";
        s += "\t" + edgeIndex1 + ":" + GetEdge1().toString();
        return s;
    }
}
