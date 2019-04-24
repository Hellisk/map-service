package algorithm.mapinference.trajectoryclustering.pcurves.PrincipalCurve.Vertex;

import algorithm.mapinference.trajectoryclustering.pcurves.PrincipalCurve.Vertex.Elements.*;

final public class CornerVertex extends Vertex implements HasOneEdge {
    MiddleVertexOfThree middleVertex;
    EndVertexOfTwo endVertex1;
    EndVertexOfTwo endVertex2;

    // For PrincipalCurveOfSkeleton.JoinVertices
    public CornerVertex(EndVertex in_endVertex1, EndVertex in_endVertex2) {
        super(in_endVertex1);

        endVertex1 = in_endVertex1.endVertex;
        endVertex2 = in_endVertex2.endVertex;
        middleVertex = new MiddleVertexOfThree(endVertex1, endVertex2);
    }

    // For TVertex::Degrade()
    public CornerVertex(TVertex tVertex, Vertex neighbor) {
        super(tVertex);
        if (neighbor.equals(tVertex.GetEdge1().GetVertex2()))
            endVertex1 = tVertex.endVertex2;
        else if (neighbor.equals(tVertex.GetEdge2().GetVertex2()))
            endVertex1 = tVertex.endVertex1;
        else
            throw (new ArithmeticException("NOT NEIGHBOR!!" + "\ntVertex:\n" + tVertex + "\nneighbor:\n" + neighbor
                    + "\n"));

        endVertex2 = tVertex.endVertex;
        middleVertex = new MiddleVertexOfThree(endVertex1, endVertex2);

        GetEdge1().GetVertex2().Maintain(this);
    }

    // For YVertex::Degrade()
    public CornerVertex(YVertex yVertex) {
        super(yVertex);
        endVertex1 = yVertex.endVertex1;
        endVertex2 = yVertex.endVertex2;
        middleVertex = new MiddleVertexOfThree(endVertex1, endVertex2);

        MaintainNeighbors();
    }

    // For StarOfThreeVertex::Degrade()
    public CornerVertex(StarOfThreeVertex starOfThreeVertex, Vertex neighbor) {
        super(starOfThreeVertex);
        if (neighbor.equals(starOfThreeVertex.GetEdge1().GetVertex2())) {
            endVertex1 = starOfThreeVertex.endVertex2;
            endVertex2 = starOfThreeVertex.endVertex3;
        } else if (neighbor.equals(starOfThreeVertex.GetEdge2().GetVertex2())) {
            endVertex1 = starOfThreeVertex.endVertex1;
            endVertex2 = starOfThreeVertex.endVertex3;
        } else if (neighbor.equals(starOfThreeVertex.GetEdge3().GetVertex2())) {
            endVertex1 = starOfThreeVertex.endVertex1;
            endVertex2 = starOfThreeVertex.endVertex2;
        } else
            throw (new ArithmeticException("NOT NEIGHBOR!!" + "\nstarOfThreeVertex:\n" + starOfThreeVertex
                    + "\nneighbor:\n" + neighbor + "\n"));

        middleVertex = new MiddleVertexOfThree(endVertex1, endVertex2);
    }

    @Override
    final public Vertex GetVertex() {
        return this;
    }

    @Override
    final public EdgeDirection GetEdgeIndex(Vertex vertex) {
        return middleVertex.GetEdgeIndex(vertex);
    }

    final public EdgeDirection GetOppositeEdgeIndex(Vertex vertex) {
        return middleVertex.GetOppositeEdgeIndex(vertex);
    }

    final public Vertex GetOppositeVertex(Vertex vertex) {
        return middleVertex.GetOppositeVertex(vertex);
    }

    @Override
    final public void Maintain(Vertex neighbor) {
        if (neighbor.equals(endVertex1.GetEdge1().GetVertex2()))
            endVertex1 = MaintainEndVertex(neighbor);
        else if (neighbor.equals(endVertex2.GetEdge1().GetVertex2()))
            endVertex2 = MaintainEndVertex(neighbor);
        else
            throw (new ArithmeticException("NOT NEIGHBOR!!" + "\ncornerVertex:\n" + this + "\nneighbor:\n" + neighbor
                    + "\n"));
    }

    @Override
    final public Vertex Degrade(Vertex neighbor) {
        return new EndVertex(this, neighbor);
    }

    final public Edge GetEdge1() {
        return endVertex1.GetEdge1();
    }

    final public Edge GetEdge2() {
        return endVertex2.GetEdge1();
    }

    final int GetEdgeIndex1() {
        return endVertex1.GetEdgeIndex1();
    }

    final int GetEdgeIndex2() {
        return endVertex2.GetEdgeIndex1();
    }

    @Override
    final public int GetDegree() {
        return 2;
    }

    @Override
    final public Edge[] GetEdges() {
        Edge[] edges = {GetEdge1(), GetEdge2()};
        return edges;
    }

    @Override
    final public Vertex[] GetNeighbors() {
        Vertex[] neighbors = {GetEdge1().GetVertex2(), GetEdge2().GetVertex2()};
        return neighbors;
    }

    @Override
    final public int[] GetEdgeIndexes() {
        int[] edgeIndexes = {GetEdgeIndex1(), GetEdgeIndex2()};
        return edgeIndexes;
    }

    @Override
    final public int[] GetNeighborIndexes() {
        int[] neighborIndexes = {GetEdge1().GetVertexIndex2(), GetEdge2().GetVertexIndex2()};
        return neighborIndexes;
    }

    @Override
    final public ElementVertex[] GetElementVertices() {
        ElementVertex[] elementVertices = {middleVertex, endVertex1, endVertex2};
        return elementVertices;
    }

    @Override
    final public ElementVertex GetMainElementVertex() {
        return middleVertex;
    }

    @Override
    final public double GetPenalty() {
        double penalty = middleVertex.GetAnglePenaltyForRectangle();

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
        NumeratorAndDenominator nd = middleVertex.GetNumeratorAndDenominatorForAnglePenaltyForRectangle();

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

        nd.AddEqual(middleVertex.GetNumeratorAndDenominatorForWeightDifferencePenalty());

        return nd;
    }

    @Override
    final public Vertex Restructure() {
        return this;
    }

    // We keep the same angle as the tangent at the neighbor and the incident line segment
    // final public Vektor GetTangent(Vertex neighbor) {
    // return GetTangentWithSameAngleAsAtNeighbor(neighbor);
    // }

    @Override
    final public String toString() {
        String s = "CornerVertex:\t";
        s += super.toString() + "\t";
        s += middleVertex.toString();
        s += endVertex1.toString();
        s += endVertex2.toString();
        return s;
    }
}
