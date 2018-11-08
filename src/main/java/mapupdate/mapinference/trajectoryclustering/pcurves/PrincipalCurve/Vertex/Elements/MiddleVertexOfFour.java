package mapupdate.mapinference.trajectoryclustering.pcurves.PrincipalCurve.Vertex.Elements;

final public class MiddleVertexOfFour extends ElementVertex implements HasOneEdge {
    private int edgeIndex1;
    private boolean forward1;
    private int edgeIndex2;
    private boolean forward2;
    private int edgeIndex3;
    private boolean forward3;

    public MiddleVertexOfFour(int in_edgeIndex1, int in_edgeIndex2, int in_edgeIndex3) {
        edgeIndex1 = in_edgeIndex1;
        edgeIndex2 = in_edgeIndex2;
        edgeIndex3 = in_edgeIndex3;

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
        if (forward1) {
            if (GetEdgeVertexIndex1At(edgeIndex1) == GetEdgeVertexIndex1At(edgeIndex3))
                forward3 = true;
            else if (GetEdgeVertexIndex1At(edgeIndex1) == GetEdgeVertexIndex2At(edgeIndex3))
                forward3 = false;
            else
                throw (new ElementInitializationException("\nedge1:\n" + GetEdgeAt(edgeIndex1) + "\nedge3:\n"
                        + GetEdgeAt(edgeIndex3) + "\n"));
        } else {
            if (GetEdgeVertexIndex2At(edgeIndex1) == GetEdgeVertexIndex1At(edgeIndex3))
                forward3 = true;
            else if (GetEdgeVertexIndex2At(edgeIndex1) == GetEdgeVertexIndex2At(edgeIndex3))
                forward3 = false;
            else
                throw (new ElementInitializationException("\nedge1:\n" + GetEdgeAt(edgeIndex1) + "\nedge3:\n"
                        + GetEdgeAt(edgeIndex3) + "\n"));
        }
    }

    public MiddleVertexOfFour(EndVertexOfTwo endVertex1, EndVertexOfTwo endVertex2, EndVertexOfTwo endVertex3) {
        edgeIndex1 = endVertex1.edgeIndex1;
        forward1 = endVertex1.forward1;
        edgeIndex2 = endVertex2.edgeIndex1;
        forward2 = endVertex2.forward1;
        edgeIndex3 = endVertex3.edgeIndex1;
        forward3 = endVertex3.forward1;
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

    final Edge GetEdge3() {
        if (forward3)
            return GetEdgeAt(edgeIndex3);
        else
            return GetEdgeAt(edgeIndex3).Reverse();
    }

    final public int GetEdgeIndex1() {
        return edgeIndex1;
    }

    final public int GetEdgeIndex2() {
        return edgeIndex2;
    }

    final public int GetEdgeIndex3() {
        return edgeIndex3;
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
        } else if (GetEdge3().GetVertex2().equals(vertex)) {
            return new EdgeDirection(edgeIndex3, forward3);
        } else
            throw (new ArithmeticException("vertex:\n" + vertex + "\nedge1Vertex2\n" + GetEdge1().GetVertex2() + "\n"
                    + "\nedge2Vertex2\n" + GetEdge2().GetVertex2() + "\n" + "\nedge3Vertex2\n"
                    + GetEdge3().GetVertex2() + "\n"));
    }

    // For insertion
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
                throw (new ArithmeticException("GetEdge2().GetVertex1():\n" + GetEdge2().GetVertex1()
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
                throw (new ArithmeticException("GetEdge1().GetVertex1():\n" + GetEdge1().GetVertex1()
                        + "edges[nei1].GetVertex1()\n" + GetEdgeAt(nei1).GetVertex1() + "edges[nei1].GetVertex2()\n"
                        + GetEdgeAt(nei1).GetVertex2() + "edges[nei2].GetVertex1()\n" + GetEdgeAt(nei2).GetVertex1()
                        + "edges[nei2].GetVertex2()\n" + GetEdgeAt(nei2).GetVertex2() + "\n"));
        }
        if (edgeIndex3 == oei) {
            if (GetEdge1().GetVertex1().equals(GetEdgeAt(nei1).GetVertex1())) {
                forward3 = true;
                edgeIndex3 = nei1;
            } else if (GetEdge1().GetVertex1().equals(GetEdgeAt(nei1).GetVertex2())) {
                forward3 = false;
                edgeIndex3 = nei1;
            } else if (GetEdge1().GetVertex1().equals(GetEdgeAt(nei2).GetVertex1())) {
                forward3 = true;
                edgeIndex3 = nei2;
            } else if (GetEdge1().GetVertex1().equals(GetEdgeAt(nei2).GetVertex2())) {
                forward3 = false;
                edgeIndex3 = nei2;
            } else
                throw (new ArithmeticException("GetEdge1().GetVertex1():\n" + GetEdge1().GetVertex1()
                        + "edges[nei1].GetVertex1()\n" + GetEdgeAt(nei1).GetVertex1() + "edges[nei1].GetVertex2()\n"
                        + GetEdgeAt(nei1).GetVertex2() + "edges[nei2].GetVertex1()\n" + GetEdgeAt(nei2).GetVertex1()
                        + "edges[nei2].GetVertex2()\n" + GetEdgeAt(nei2).GetVertex2() + "\n"));
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
        } else if (edgeIndex3 == oei) {
            if (GetVertex().equals(GetEdgeAt(nei).GetVertex1())) {
                forward3 = true;
                edgeIndex3 = nei;
            } else if (GetVertex().equals(GetEdgeAt(nei).GetVertex2())) {
                forward3 = false;
                edgeIndex3 = nei;
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
        if (edgeIndex3 > lowerIndex)
            edgeIndex3--;
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
        if (forward3)
            GetEdgeAt(edgeIndex3).SetVertexIndex1(vi);
        else
            GetEdgeAt(edgeIndex3).SetVertexIndex2(vi);
    }

    private int GetSetSizeSum() {
        return GetVertex().GetSetSize() + GetEdgeAt(edgeIndex1).GetSetSize() + GetEdgeAt(edgeIndex2).GetSetSize()
                + GetEdgeAt(edgeIndex3).GetSetSize();
    }

    @Override
    final public boolean EmptySet() {
        return GetSetSizeSum() == 0;
    }

    @Override
    final public NumeratorAndDenominator GetNumeratorAndDenominatorForMSE() {
        NumeratorAndDenominator nd = GetEdge1().GetNumeratorAndDenominatorForMSETimesWeight();
        nd.AddEqual(GetVertex().GetNumeratorAndDenominatorForMSETimesWeight());
        nd.AddEqual(GetEdge2().GetNumeratorAndDenominatorForMSETimesWeight());
        nd.AddEqual(GetEdge3().GetNumeratorAndDenominatorForMSETimesWeight());
        return nd;
    }

    @Override
    final public String toString() {
        String s = new String();
        s += "\t" + edgeIndex1 + ":" + GetEdge1().toString();
        s += "," + edgeIndex2 + ":" + GetEdge2().toString();
        s += "," + edgeIndex3 + ":" + GetEdge3().toString();
        return s;
    }
}
