package mapupdate.mapinference.trajectoryclustering.pcurves.PrincipalCurve.Vertex;

import mapupdate.mapinference.trajectoryclustering.pcurves.PrincipalCurve.Vertex.Elements.*;

final public class TVertex extends Vertex implements HasTwoSymmetricEdges, JoinVertex {
    MiddleVertexOfThree lineVertex;
    EndVertexOfTwo endVertex;
    EndVertexOfTwo endVertex1;
    EndVertexOfTwo endVertex2;
    private MiddleVertexOfFour middleVertex;
    private MiddleVertexOfThree lineVertex1;
    private MiddleVertexOfThree lineVertex2;

    // For PrincipalCurveOfTemplate.JoinVertices()
    public TVertex(LineVertex in_lineVertex, EndVertex in_endVertex) {
        super(in_lineVertex);

        lineVertex = in_lineVertex.middleVertex;
        endVertex = in_endVertex.endVertex;
        endVertex1 = in_lineVertex.endVertex1;
        endVertex2 = in_lineVertex.endVertex2;
        lineVertex1 = new MiddleVertexOfThree(endVertex, endVertex1);
        lineVertex2 = new MiddleVertexOfThree(endVertex, endVertex2);
        middleVertex = new MiddleVertexOfFour(endVertex, endVertex1, endVertex2);
    }

    // For StarOfThreeVertex.Restructure(),XVertex.Degrade()
    public TVertex(EndVertexOfTwo in_endVertex, EndVertexOfTwo in_endVertex1, EndVertexOfTwo in_endVertex2) {
        super(in_endVertex.GetVertex());

        endVertex = in_endVertex;
        endVertex1 = in_endVertex1;
        endVertex2 = in_endVertex2;
        lineVertex = new MiddleVertexOfThree(endVertex1, endVertex2);
        lineVertex1 = new MiddleVertexOfThree(endVertex, endVertex1);
        lineVertex2 = new MiddleVertexOfThree(endVertex, endVertex2);
        middleVertex = new MiddleVertexOfFour(endVertex, endVertex1, endVertex2);

        MaintainNeighbors();
        System.out.println("Tvertex");
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
    final public EdgeDirection GetOppositeEdgeIndex(Vertex vertex) {
        if (vertex.equals(endVertex.GetEdge1().GetVertex2()))
            throw (new TwoOppositeEdgeIndexesException());
        return lineVertex.GetOppositeEdgeIndex(vertex);
    }

    @Override
    final public Vertex GetOppositeVertex(Vertex vertex) {
        if (vertex.equals(endVertex.GetEdge1().GetVertex2()))
            throw (new TwoOppositeEdgeIndexesException());
        return lineVertex.GetOppositeVertex(vertex);
    }

    @Override
    final public void Maintain(Vertex neighbor) {
        if (neighbor.equals(endVertex1.GetEdge1().GetVertex2()))
            endVertex1 = MaintainEndVertex(neighbor);
        else if (neighbor.equals(endVertex2.GetEdge1().GetVertex2()))
            endVertex2 = MaintainEndVertex(neighbor);
        else
            endVertex = MaintainEndVertex(neighbor);
    }

    @Override
    final public Vertex Degrade(Vertex neighbor) {
        if (neighbor.equals(endVertex.GetEdge1().GetVertex2()))
            return new LineVertex(this);
        else {
            CornerVertex cornerVertex = new CornerVertex(this, neighbor);
            double angle =
                    180
                            * Math.acos(CosAngle(cornerVertex.GetEdge1().GetVertex2(), cornerVertex.GetEdge2()
                            .GetVertex2())) / Math.PI;
            if (CornerAngle(angle))
                return cornerVertex;
            else
                return new LineVertex(cornerVertex);
        }
    }

    final public Edge GetEdge1() {
        return endVertex1.GetEdge1();
    }

    final public Edge GetEdge2() {
        return endVertex2.GetEdge1();
    }

    final Edge GetEdge3() {
        return endVertex.GetEdge1();
    }

    final int GetEdgeIndex1() {
        return endVertex1.GetEdgeIndex1();
    }

    final int GetEdgeIndex2() {
        return endVertex2.GetEdgeIndex1();
    }

    final int GetEdgeIndex3() {
        return endVertex.GetEdgeIndex1();
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
                {middleVertex, lineVertex, lineVertex1, lineVertex2, endVertex, endVertex1, endVertex2};
        return elementVertices;
    }

    @Override
    final public ElementVertex GetMainElementVertex() {
        return middleVertex;
    }

    @Override
    final public double GetPenalty() {
        double penalty = lineVertex.GetAnglePenalty();
        penalty += lineVertex1.GetAnglePenaltyForRectangle();
        penalty += lineVertex2.GetAnglePenaltyForRectangle();

        try {
            penalty += ((EndVertexOfThree) endVertex).GetAnglePenalty();
        } catch (ClassCastException e) {
            penalty += endVertex.GetLengthPenalty();
        }

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

        return penalty;
    }

    @Override
    final public NumeratorAndDenominator GetNumeratorAndDenominatorForPenalty() {
        NumeratorAndDenominator nd = new NumeratorAndDenominator(this);
        nd.AddEqual(lineVertex.GetNumeratorAndDenominatorForAnglePenalty());
        nd.AddEqual(lineVertex1.GetNumeratorAndDenominatorForAnglePenaltyForRectangle());
        nd.AddEqual(lineVertex2.GetNumeratorAndDenominatorForAnglePenaltyForRectangle());

        try {
            nd.AddEqual(((EndVertexOfThree) endVertex).GetNumeratorAndDenominatorForAnglePenalty());
        } catch (ClassCastException e) {
            nd.AddEqual(endVertex.GetNumeratorAndDenominatorForLengthPenalty());
        }

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

        nd.AddEqual(lineVertex.GetNumeratorAndDenominatorForWeightDifferencePenalty());

        return nd;
    }

    @Override
    final public Vertex Restructure() {
        return this;
    }

    // final public Vektor GetTangent(Vertex neighbor) {
    // Vertex [] neighbors = GetNeighbors();
    // if (neighbor.equals(neighbors[2]))
    // return GetTangentWithSameAngleAsAtNeighbor(neighbor);
    // else
    // return GetTangentOfCenterOfCircleAround(neighbor,GetOppositeVertex(neighbor));
    // }

    @Override
    final public String toString() {
        String s = "TVertex:\t";
        s += super.toString() + "\t";
        s += middleVertex.toString();
        s += endVertex.toString();
        s += endVertex1.toString();
        s += endVertex2.toString();
        return s;
    }
}
