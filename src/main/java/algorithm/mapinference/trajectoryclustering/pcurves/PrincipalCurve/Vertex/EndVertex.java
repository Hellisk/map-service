package algorithm.mapinference.trajectoryclustering.pcurves.PrincipalCurve.Vertex;

import algorithm.mapinference.trajectoryclustering.pcurves.PrincipalCurve.Vertex.Elements.*;

final public class EndVertex extends Vertex implements HasOneEdge, RegularVertex {
    EndVertexOfTwo endVertex;

    public EndVertex(Vertex in_endVertex, int edgeIndex1) {
        super(in_endVertex);
        endVertex = new EndVertexOfTwo(in_endVertex, edgeIndex1);
    }

    public EndVertex(Vertex in_endVertex, int edgeIndex1, int edgeIndex2) {
        super(in_endVertex);
        endVertex = new EndVertexOfThree(in_endVertex, edgeIndex1, edgeIndex2);
    }

    public EndVertex(Vertex in_endVertex, int edgeIndex1, int edgeIndex2, int edgeIndex3) {
        super(in_endVertex);
        endVertex = new EndVertexOfY(in_endVertex, edgeIndex1, edgeIndex2, edgeIndex3);
    }

    // For LineVertex.Degrade()
    public EndVertex(LineVertex lineVertex, Vertex neighbor) {
        super(lineVertex);
        if (neighbor.equals(lineVertex.GetEdge1().GetVertex2()))
            endVertex = lineVertex.endVertex2;
        else if (neighbor.equals(lineVertex.GetEdge2().GetVertex2()))
            endVertex = lineVertex.endVertex1;
        else
            throw (new ArithmeticException("NOT NEIGHBOR!!" + "\nlineVertex:\n" + lineVertex + "\nneighbor:\n"
                    + neighbor + "\n"));
        MaintainNeighbors();
    }

    // For CornerVertex.Degrade()
    public EndVertex(CornerVertex cornerVertex, Vertex neighbor) {
        super(cornerVertex);
        if (neighbor.equals(cornerVertex.GetEdge1().GetVertex2()))
            endVertex = cornerVertex.endVertex2;
        else if (neighbor.equals(cornerVertex.GetEdge2().GetVertex2()))
            endVertex = cornerVertex.endVertex1;
        else
            throw (new ArithmeticException("NOT NEIGHBOR!!" + "\ncornerVertex:\n" + cornerVertex + "\nneighbor:\n"
                    + neighbor + "\n"));
        MaintainNeighbors();
    }

    @Override
    final public Vertex GetVertex() {
        return this;
    }

    @Override
    final public EdgeDirection GetEdgeIndex(Vertex vertex) {
        return endVertex.GetEdgeIndex(vertex);
    }

    @Override
    final public void Maintain(Vertex neighbor) {
        endVertex = MaintainEndVertex(neighbor);
    }

    @Override
    final public Vertex Degrade(Vertex neighbor) {
        throw (new ArithmeticException("CAN'T DEGRADE AN EndVertex" + "\nthis\n" + this + "\n"));
    }

    final Edge GetEdge1() {
        return endVertex.GetEdge1();
    }

    final public int GetEdgeIndex1() {
        return endVertex.GetEdgeIndex1();
    }

    @Override
    final public int GetDegree() {
        return 1;
    }

    @Override
    final public Edge[] GetEdges() {
        Edge[] edges = {GetEdge1()};
        return edges;
    }

    @Override
    final public Vertex[] GetNeighbors() {
        Vertex[] neighbors = {GetEdge1().GetVertex2()};
        return neighbors;
    }

    @Override
    final public int[] GetEdgeIndexes() {
        int[] edgeIndexes = {GetEdgeIndex1()};
        return edgeIndexes;
    }

    @Override
    final public int[] GetNeighborIndexes() {
        int[] neighborIndexes = {GetEdge1().GetVertexIndex2()};
        return neighborIndexes;
    }

    @Override
    final public ElementVertex[] GetElementVertices() {
        ElementVertex[] elementVertices = {endVertex};
        return elementVertices;
    }

    @Override
    final public ElementVertex GetMainElementVertex() {
        return endVertex;
    }

    @Override
    final public double GetPenalty() {
        if (endVertex instanceof EndVertexOfThree)
            return ((EndVertexOfThree) endVertex).GetAnglePenalty() + 2 * endVertex.GetLengthPenalty();
        else
            return 3 * endVertex.GetLengthPenalty();
    }

    @Override
    final public NumeratorAndDenominator GetNumeratorAndDenominatorForPenalty() {
        NumeratorAndDenominator nd = endVertex.GetNumeratorAndDenominatorForLengthPenalty();
        nd.MulEqual(2);
        if (endVertex instanceof EndVertexOfThree)
            nd.AddEqual(((EndVertexOfThree) endVertex).GetNumeratorAndDenominatorForAnglePenalty());
        else
            nd.MulEqual(1.5);
        return nd;
    }

    @Override
    final public Vertex Restructure() {
        return this;
    }

    // final public Vektor GetTangent(Vertex neighbor) {
    // return GetTangentWithSameAngleAsAtNeighbor(neighbor);
    // }

    @Override
    final public String toString() {
        String s = "EndVertex:\t";
        s += super.toString() + "\t";
        s += endVertex.toString();
        return s;
    }
}
