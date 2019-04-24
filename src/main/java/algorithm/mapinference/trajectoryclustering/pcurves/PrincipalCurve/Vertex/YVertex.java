package algorithm.mapinference.trajectoryclustering.pcurves.PrincipalCurve.Vertex;

import algorithm.mapinference.trajectoryclustering.pcurves.PrincipalCurve.Vertex.Elements.*;

final public class YVertex extends Vertex implements HasTwoSymmetricEdges, JoinVertex {
    EndVertexOfTwo endVertex;
    EndVertexOfTwo endVertex1;
    EndVertexOfTwo endVertex2;
    private MiddleVertexOfFour middleVertex;
    private MiddleVertexOfThree lineVertex1;
    private MiddleVertexOfThree lineVertex2;

    // For PrincipalCurveOfTemplate.JoinVertices()
    public YVertex(CornerVertex in_cornerVertex, EndVertex in_endVertex) {
        super(in_cornerVertex);

        endVertex = in_endVertex.endVertex;
        endVertex1 = in_cornerVertex.endVertex1;
        endVertex2 = in_cornerVertex.endVertex2;
        lineVertex1 = new MiddleVertexOfThree(endVertex, endVertex1);
        lineVertex2 = new MiddleVertexOfThree(endVertex, endVertex2);
        middleVertex = new MiddleVertexOfFour(endVertex, endVertex1, endVertex2);
    }

    // For StarOfThreeVertex.Restructure(),XVertex.Degrade()
    public YVertex(EndVertexOfTwo in_endVertex, EndVertexOfTwo in_endVertex1, EndVertexOfTwo in_endVertex2) {
        super(in_endVertex.GetVertex());

        endVertex = in_endVertex;
        endVertex1 = in_endVertex1;
        endVertex2 = in_endVertex2;
        lineVertex1 = new MiddleVertexOfThree(endVertex, endVertex1);
        lineVertex2 = new MiddleVertexOfThree(endVertex, endVertex2);
        middleVertex = new MiddleVertexOfFour(endVertex, endVertex1, endVertex2);

        MaintainNeighbors();
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
        if (vertex.equals(endVertex1.GetEdge1().GetVertex2()))
            return lineVertex1.GetOppositeEdgeIndex(vertex);
        else if (vertex.equals(endVertex2.GetEdge1().GetVertex2()))
            return lineVertex2.GetOppositeEdgeIndex(vertex);
        else if (vertex.equals(endVertex.GetEdge1().GetVertex2()))
            return lineVertex1.GetOppositeEdgeIndex(vertex); // it doesn't matter which path we return
            // throw(new TwoOppositeEdgeIndexesException());
        else
            throw (new ArithmeticException("vertex:\n" + vertex + "\nedge1Vertex2\n"
                    + endVertex1.GetEdge1().GetVertex2() + "\n" + "\nedge2Vertex2\n"
                    + endVertex2.GetEdge1().GetVertex2() + "\n"));
    }

    @Override
    final public Vertex GetOppositeVertex(Vertex vertex) {
        if (vertex.equals(endVertex1.GetEdge1().GetVertex2()))
            return lineVertex1.GetOppositeVertex(vertex);
        else if (vertex.equals(endVertex2.GetEdge1().GetVertex2()))
            return lineVertex2.GetOppositeVertex(vertex);
        else if (vertex.equals(endVertex.GetEdge1().GetVertex2()))
            return lineVertex1.GetOppositeVertex(vertex);
            // throw(new TwoOppositeEdgeIndexesException()); // it doesn't matter which path we return
        else
            throw (new ArithmeticException("vertex:\n" + vertex + "\nedge1Vertex2\n"
                    + endVertex1.GetEdge1().GetVertex2() + "\n" + "\nedge2Vertex2\n"
                    + endVertex2.GetEdge1().GetVertex2() + "\n"));
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
        if (neighbor.equals(endVertex.GetEdge1().GetVertex2())) {
            CornerVertex cornerVertex = new CornerVertex(this);
            double angle = 180 * Math.acos(CosAngle(GetEdge2().GetVertex2(), GetEdge3().GetVertex2())) / Math.PI;
            if (CornerAngle(angle))
                return cornerVertex;
            else
                return new LineVertex(cornerVertex);
        } else
            return new LineVertex(this, neighbor);
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

    final public int GetEdgeIndex1() {
        return endVertex1.GetEdgeIndex1();
    }

    final public int GetEdgeIndex2() {
        return endVertex2.GetEdgeIndex1();
    }

    final public int GetEdgeIndex3() {
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
        ElementVertex[] elementVertices = {middleVertex, lineVertex1, lineVertex2, endVertex, endVertex1, endVertex2};
        return elementVertices;
    }

    @Override
    final public ElementVertex GetMainElementVertex() {
        return middleVertex;
    }

    @Override
    final public double GetPenalty() {
        double penalty = 0.0;
        penalty += lineVertex1.GetAnglePenalty();
        penalty += lineVertex2.GetAnglePenalty();

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
        nd.AddEqual(lineVertex1.GetNumeratorAndDenominatorForAnglePenalty());
        nd.AddEqual(lineVertex2.GetNumeratorAndDenominatorForAnglePenalty());

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

        nd.AddEqual(lineVertex1.GetNumeratorAndDenominatorForWeightDifferencePenalty());
        nd.AddEqual(lineVertex2.GetNumeratorAndDenominatorForWeightDifferencePenalty());
        nd.MulEqual(0.2);

        return nd;
    }

    @Override
    final public Vertex Restructure() {
        return this;
    }

    // final public Vektor GetTangent(Vertex neighbor) {
    // Vertex [] neighbors = GetNeighbors();
    // if (neighbor.equals(neighbors[2]))
    // return GetTangentOfCenterOfCircleAround(neighbors[2],neighbors[0].Add(neighbors[1]).Div(2));
    // else
    // return GetTangentOfCenterOfCircleAround(neighbors[0].Add(neighbors[1]).Div(2),neighbors[2]);
    // }

    @Override
    final public String toString() {
        String s = "YVertex:\t";
        s += super.toString() + "\t";
        s += middleVertex.toString();
        s += endVertex.toString();
        s += endVertex1.toString();
        s += endVertex2.toString();
        return s;
    }
}
