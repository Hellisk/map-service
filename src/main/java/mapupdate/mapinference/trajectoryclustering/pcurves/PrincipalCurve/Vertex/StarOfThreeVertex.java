package mapupdate.mapinference.trajectoryclustering.pcurves.PrincipalCurve.Vertex;

import mapupdate.mapinference.trajectoryclustering.pcurves.PrincipalCurve.Vertex.Elements.*;

final public class StarOfThreeVertex extends Vertex implements HasOneEdge, JoinVertex {
    EndVertexOfTwo endVertex1;
    EndVertexOfTwo endVertex2;
    EndVertexOfTwo endVertex3;
    private MiddleVertexOfFour middleVertex;
    private MiddleVertexOfThree middleVertex1;
    private MiddleVertexOfThree middleVertex2;
    private MiddleVertexOfThree middleVertex3;

    // For PrincipalCurve2D::InitializeToCurves()
    public StarOfThreeVertex(LineVertex in_lineVertex, EndVertex in_endVertex) {
        super(in_lineVertex);

        endVertex1 = in_endVertex.endVertex;
        endVertex2 = in_lineVertex.endVertex1;
        endVertex3 = in_lineVertex.endVertex2;
        SetMiddleVertices();
    }

    public StarOfThreeVertex(CornerVertex in_cornerVertex, EndVertex in_endVertex) {
        super(in_cornerVertex);

        endVertex1 = in_endVertex.endVertex;
        endVertex2 = in_cornerVertex.endVertex1;
        endVertex3 = in_cornerVertex.endVertex2;
        SetMiddleVertices();
    }

    // For StarOfFourVertex::Degrade()
    public StarOfThreeVertex(StarOfFourVertex starOfFourVertex, Vertex neighbor) {
        super(starOfFourVertex);
        if (neighbor.equals(starOfFourVertex.GetEdge1().GetVertex2())) {
            endVertex1 = starOfFourVertex.endVertex2;
            endVertex2 = starOfFourVertex.endVertex3;
            endVertex3 = starOfFourVertex.endVertex4;
        } else if (neighbor.equals(starOfFourVertex.GetEdge2().GetVertex2())) {
            endVertex1 = starOfFourVertex.endVertex1;
            endVertex2 = starOfFourVertex.endVertex3;
            endVertex3 = starOfFourVertex.endVertex4;
        } else if (neighbor.equals(starOfFourVertex.GetEdge3().GetVertex2())) {
            endVertex1 = starOfFourVertex.endVertex1;
            endVertex2 = starOfFourVertex.endVertex2;
            endVertex3 = starOfFourVertex.endVertex4;
        } else if (neighbor.equals(starOfFourVertex.GetEdge4().GetVertex2())) {
            endVertex1 = starOfFourVertex.endVertex1;
            endVertex2 = starOfFourVertex.endVertex2;
            endVertex3 = starOfFourVertex.endVertex3;
        } else
            throw (new ArithmeticException("NOT NEIGHBOR!!" + "\nstarOfFourVertex:\n" + starOfFourVertex
                    + "\nneighbor:\n" + neighbor + "\n"));
        SetMiddleVertices();
    }

    private void SetMiddleVertices() {
        middleVertex = new MiddleVertexOfFour(endVertex1, endVertex2, endVertex3);
        middleVertex1 = new MiddleVertexOfThree(endVertex2, endVertex3);
        middleVertex2 = new MiddleVertexOfThree(endVertex1, endVertex3);
        middleVertex3 = new MiddleVertexOfThree(endVertex1, endVertex2);
    }

    @Override
    final public Vertex GetVertex() {
        return this;
    }

    @Override
    final public EdgeDirection GetEdgeIndex(Vertex vertex) {
        return middleVertex.GetEdgeIndex(vertex);
    }

    @Override
    final public void Maintain(Vertex neighbor) {
        if (neighbor.equals(endVertex1.GetEdge1().GetVertex2()))
            endVertex1 = MaintainEndVertex(neighbor);
        else if (neighbor.equals(endVertex2.GetEdge1().GetVertex2()))
            endVertex2 = MaintainEndVertex(neighbor);
        else if (neighbor.equals(endVertex3.GetEdge1().GetVertex2()))
            endVertex3 = MaintainEndVertex(neighbor);
        else
            throw (new ArithmeticException("NOT NEIGHBOR!!" + "\nstarOfThreeVertex:\n" + this + "\nneighbor:\n"
                    + neighbor + "\n"));
    }

    @Override
    final public Vertex Degrade(Vertex neighbor) {
        CornerVertex cornerVertex = new CornerVertex(this, neighbor);
        double angle =
                180 * Math.acos(CosAngle(cornerVertex.GetEdge1().GetVertex2(), cornerVertex.GetEdge2().GetVertex2()))
                        / Math.PI;
        if (CornerAngle(angle))
            return cornerVertex;
        else
            return new LineVertex(cornerVertex);
    }

    final public Edge GetEdge1() {
        return endVertex1.GetEdge1();
    }

    final public Edge GetEdge2() {
        return endVertex2.GetEdge1();
    }

    final public Edge GetEdge3() {
        return endVertex3.GetEdge1();
    }

    final int GetEdgeIndex1() {
        return endVertex1.GetEdgeIndex1();
    }

    final int GetEdgeIndex2() {
        return endVertex2.GetEdgeIndex1();
    }

    final int GetEdgeIndex3() {
        return endVertex3.GetEdgeIndex1();
    }

    @Override
    final public int GetDegree() {
        return 3;
    }

    @Override
    final public Edge[] GetEdges() {
        Edge[] edges = {GetEdge1(), GetEdge2(), GetEdge3()};
        return edges;
    }

    @Override
    final public Vertex[] GetNeighbors() {
        Vertex[] neighbors = {GetEdge1().GetVertex2(), GetEdge2().GetVertex2(), GetEdge3().GetVertex2()};
        return neighbors;
    }

    @Override
    final public int[] GetEdgeIndexes() {
        int[] edgeIndexes = {GetEdgeIndex1(), GetEdgeIndex2(), GetEdgeIndex3()};
        return edgeIndexes;
    }

    @Override
    final public int[] GetNeighborIndexes() {
        int[] neighborIndexes =
                {GetEdge1().GetVertexIndex2(), GetEdge2().GetVertexIndex2(), GetEdge3().GetVertexIndex2()};
        return neighborIndexes;
    }

    @Override
    final public ElementVertex[] GetElementVertices() {
        ElementVertex[] elementVertices =
                {middleVertex, endVertex1, endVertex2, endVertex3, middleVertex1, middleVertex2, middleVertex3};
        return elementVertices;
    }

    @Override
    final public ElementVertex GetMainElementVertex() {
        return middleVertex;
    }

    @Override
    final public double GetPenalty() {
        double penalty = 0;
        // if (180*Math.acos(CosAngle(middleVertex1.GetEdge1().GetVertex2(),
        // middleVertex1.GetEdge2().GetVertex2()))/Math.PI > 170)
        penalty += middleVertex1.GetAnglePenalty();
        // if (180*Math.acos(CosAngle(middleVertex2.GetEdge1().GetVertex2(),
        // middleVertex2.GetEdge2().GetVertex2()))/Math.PI > 170)
        penalty += middleVertex2.GetAnglePenalty();
        // if (180*Math.acos(CosAngle(middleVertex3.GetEdge1().GetVertex2(),
        // middleVertex3.GetEdge2().GetVertex2()))/Math.PI > 170)
        penalty += middleVertex3.GetAnglePenalty();

        try {
            penalty += ((EndVertexOfThree) endVertex1).GetAnglePenalty();
        } catch (ClassCastException e) {
            penalty += endVertex1.GetLengthPenalty();
        }

        try {
            penalty += ((EndVertexOfThree) endVertex2).GetAnglePenalty();
        } catch (ClassCastException e) {
            penalty += endVertex2.GetLengthPenalty();
        }

        try {
            penalty += ((EndVertexOfThree) endVertex3).GetAnglePenalty();
        } catch (ClassCastException e) {
            penalty += endVertex3.GetLengthPenalty();
        }

        return penalty * 0.1;
    }

    @Override
    final public NumeratorAndDenominator GetNumeratorAndDenominatorForPenalty() {
        NumeratorAndDenominator nd = new NumeratorAndDenominator(this);
        // if (180*Math.acos(CosAngle(middleVertex1.GetEdge1().GetVertex2(),
        // middleVertex1.GetEdge2().GetVertex2()))/Math.PI > 170)
        nd.AddEqual(middleVertex1.GetNumeratorAndDenominatorForAnglePenalty());
        // if (180*Math.acos(CosAngle(middleVertex2.GetEdge1().GetVertex2(),
        // middleVertex2.GetEdge2().GetVertex2()))/Math.PI > 170)
        nd.AddEqual(middleVertex2.GetNumeratorAndDenominatorForAnglePenalty());
        // if (180*Math.acos(CosAngle(middleVertex3.GetEdge1().GetVertex2(),
        // middleVertex3.GetEdge2().GetVertex2()))/Math.PI > 170)
        nd.AddEqual(middleVertex3.GetNumeratorAndDenominatorForAnglePenalty());

        try {
            nd.AddEqual(((EndVertexOfThree) endVertex1).GetNumeratorAndDenominatorForAnglePenalty());
        } catch (ClassCastException e) {
            nd.AddEqual(endVertex1.GetNumeratorAndDenominatorForLengthPenalty());
        }

        try {
            nd.AddEqual(((EndVertexOfThree) endVertex2).GetNumeratorAndDenominatorForAnglePenalty());
        } catch (ClassCastException e) {
            nd.AddEqual(endVertex2.GetNumeratorAndDenominatorForLengthPenalty());
        }

        try {
            nd.AddEqual(((EndVertexOfThree) endVertex3).GetNumeratorAndDenominatorForAnglePenalty());
        } catch (ClassCastException e) {
            nd.AddEqual(endVertex3.GetNumeratorAndDenominatorForLengthPenalty());
        }

        nd.MulEqual(0.1);
        return nd;
    }

    @Override
    final public Vertex Restructure() {
        double angle12 = 180 * Math.acos(CosAngle(GetEdge1().GetVertex2(), GetEdge2().GetVertex2())) / Math.PI;
        double angle13 = 180 * Math.acos(CosAngle(GetEdge1().GetVertex2(), GetEdge3().GetVertex2())) / Math.PI;
        double angle23 = 180 * Math.acos(CosAngle(GetEdge2().GetVertex2(), GetEdge3().GetVertex2())) / Math.PI;

        // if (RectAngle(angle12) && RectAngle(angle13) && LineAngle(angle23))
        // return new TVertex(endVertex1,endVertex2,endVertex3);
        // else if (RectAngle(angle12) && RectAngle(angle23) && LineAngle(angle13))
        // return new TVertex(endVertex2,endVertex1,endVertex3);
        // else if (RectAngle(angle13) && RectAngle(angle23) && LineAngle(angle12))
        // return new TVertex(endVertex3,endVertex1,endVertex2);
        // else

        if (angle12 < angle23 && angle12 < angle13)
            return new YVertex(endVertex3, endVertex1, endVertex2);
        else if (angle13 < angle23 && angle13 < angle12)
            return new YVertex(endVertex2, endVertex1, endVertex3);
        else if (angle23 < angle13 && angle23 < angle12)
            return new YVertex(endVertex1, endVertex2, endVertex3);

        // if (angle12 > angle23 && angle12 > angle13)
        // return new TVertex(endVertex3,endVertex1,endVertex2);
        // else if (angle13 > angle23 && angle13 > angle12)
        // return new TVertex(endVertex2,endVertex1,endVertex3);
        // else if (angle23 > angle13 && angle23 > angle12)
        // return new TVertex(endVertex1,endVertex2,endVertex3);

        return this;
    }

    // final public Vektor GetTangent(Vertex neighbor) {
    // return GetTangentWithSameAngleAsAtNeighbor(neighbor);
    // }

    @Override
    final public String toString() {
        String s = "StarOfThreeVertex:\t";
        s += super.toString() + "\t";
        s += middleVertex.toString();
        s += endVertex1.toString();
        s += endVertex2.toString();
        s += endVertex3.toString();
        return s;
    }

    // public void Paint(Graphics g,DataCanvas canvas,int pixelSize,String type) {
    // super.Paint(g,canvas,pixelSize,type);
    // edges[edgeIndex2].Paint(g,canvas,pixelSize,type);
    // }
}
