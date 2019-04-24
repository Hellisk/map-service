package algorithm.mapinference.trajectoryclustering.pcurves.PrincipalCurve.Vertex;

import algorithm.mapinference.trajectoryclustering.pcurves.PrincipalCurve.Vertex.Elements.*;

final public class LineVertex extends Vertex implements HasTwoSymmetricEdges, RegularVertex {
    MiddleVertexOfThree middleVertex;
    EndVertexOfTwo endVertex1;
    EndVertexOfTwo endVertex2;

    public LineVertex(Vertex in_middleVertex, int edgeIndex1, int edgeIndex2) {
        super(in_middleVertex);
        Constructor(in_middleVertex, edgeIndex1, edgeIndex2);
    }

    public LineVertex(Vertex in_middleVertex, int edgeIndex1, int edgeIndex2, int edgeIndex3) {
        super(in_middleVertex);
        Constructor(in_middleVertex, edgeIndex1, edgeIndex2, edgeIndex3);
    }

    public LineVertex(Vertex in_middleVertex, int edgeIndex1, int edgeIndex2, int edgeIndex3, int edgeIndex4) {
        super(in_middleVertex);
        try {
            ConstructorTwoAndTwo(in_middleVertex, edgeIndex1, edgeIndex2, edgeIndex3, edgeIndex4);
        } catch (ElementInitializationException e1) {
            try {
                ConstructorThreeAndOne(in_middleVertex, edgeIndex1, edgeIndex2, edgeIndex3, edgeIndex4);
            } catch (ElementInitializationException e2) {
                throw new ElementInitializationException(e1.getMessage() + e2.getMessage());
            }
        }
    }

    public LineVertex(Vertex in_middleVertex, int edgeIndex1, int edgeIndex2, int edgeIndex3, int edgeIndex4,
                      int edgeIndex5) {
        super(in_middleVertex);
        Constructor(in_middleVertex, edgeIndex1, edgeIndex2, edgeIndex3, edgeIndex4, edgeIndex5);
    }

    public LineVertex(Vertex in_middleVertex, int edgeIndex1, int edgeIndex2, int edgeIndex3, int edgeIndex4,
                      int edgeIndex5, int edgeIndex6) {
        super(in_middleVertex);
        Constructor(in_middleVertex, edgeIndex1, edgeIndex2, edgeIndex3, edgeIndex4, edgeIndex5, edgeIndex6);
    }

    // For PrincipalCurveClass.InsertMidPoint()
    public LineVertex(Vertex in_middleVertex, Vertex neighbor1, Vertex neighbor2) {
        super(in_middleVertex);
        InsertAsMidpoint(in_middleVertex, neighbor1, neighbor2);
    }

    // For PrincipalCurveClass.InitializeToCurves()
    public LineVertex(EndVertex in_endVertex1, EndVertex in_endVertex2) {
        super(in_endVertex1);

        endVertex1 = in_endVertex1.endVertex;
        endVertex2 = in_endVertex2.endVertex;
        middleVertex = new MiddleVertexOfThree(endVertex1, endVertex2);
    }

    // For StarOfFourVertex.MergeStarOfThreeVertices()
    public LineVertex(EndVertex endVertex, HasOneEdge neighbor) {
        super(endVertex);

        endVertex1 = endVertex.endVertex;
        endVertex2 = new EndVertexOfTwo(endVertex, neighbor.GetEdgeIndex(endVertex).edgeIndex);
        middleVertex = new MiddleVertexOfThree(endVertex1, endVertex2);
    }

    // For TVertex.Degrade()
    public LineVertex(TVertex tVertex) {
        super(tVertex);
        middleVertex = tVertex.lineVertex;
        endVertex1 = tVertex.endVertex1;
        endVertex2 = tVertex.endVertex2;
    }

    // For YVertex.Degrade()
    public LineVertex(YVertex yVertex, Vertex neighbor) {
        super(yVertex);
        if (neighbor.equals(yVertex.GetEdge1().GetVertex2()))
            endVertex1 = yVertex.endVertex2;
        else if (neighbor.equals(yVertex.GetEdge2().GetVertex2()))
            endVertex1 = yVertex.endVertex1;
        else
            throw (new ArithmeticException("NOT NEIGHBOR!!" + "\nyVertex:\n" + yVertex + "\nneighbor:\n" + neighbor
                    + "\n"));

        endVertex2 = yVertex.endVertex;
        middleVertex = new MiddleVertexOfThree(endVertex1, endVertex2);

        GetEdge2().GetVertex2().Maintain(this);
    }

    // For StarOfThreeVertex.Degrade()
    public LineVertex(StarOfThreeVertex starOfThreeVertex, Vertex neighbor) {
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

        MaintainNeighbors();
    }

    // For StarOfManyVertex.Degrade()
    public LineVertex(StarOfManyVertex starOfManyVertex, Vertex neighbor) {
        super(starOfManyVertex);
        if (neighbor.equals(starOfManyVertex.GetEdge(0).GetVertex2())) {
            endVertex1 = starOfManyVertex.endVertexs[1];
            endVertex2 = starOfManyVertex.endVertexs[2];
        } else if (neighbor.equals(starOfManyVertex.GetEdge(1).GetVertex2())) {
            endVertex1 = starOfManyVertex.endVertexs[0];
            endVertex2 = starOfManyVertex.endVertexs[2];
        } else if (neighbor.equals(starOfManyVertex.GetEdge(2).GetVertex2())) {
            endVertex1 = starOfManyVertex.endVertexs[0];
            endVertex2 = starOfManyVertex.endVertexs[1];
        } else
            throw (new ArithmeticException("NOT NEIGHBOR!!" + "\nstarOfManyVertex:\n" + starOfManyVertex
                    + "\nneighbor:\n" + neighbor + "\n"));

        middleVertex = new MiddleVertexOfThree(endVertex1, endVertex2);

        MaintainNeighbors();
    }

    // For PrincipalCurveOfTemplate.Restructure()
    public LineVertex(CornerVertex cornerVertex) {
        super(cornerVertex);
        middleVertex = cornerVertex.middleVertex;
        endVertex1 = cornerVertex.endVertex1;
        endVertex2 = cornerVertex.endVertex2;
    }

    private void Constructor(Vertex in_middleVertex, int edgeIndex1, int edgeIndex2) {
        middleVertex = new MiddleVertexOfThree(edgeIndex1, edgeIndex2);
        endVertex1 = new EndVertexOfTwo(in_middleVertex, edgeIndex1);
        endVertex2 = new EndVertexOfTwo(in_middleVertex, edgeIndex2);
    }

    private void Constructor(Vertex in_middleVertex, int edgeIndex1, int edgeIndex2, int edgeIndex3) {
        middleVertex = new MiddleVertexOfThree(edgeIndex1, edgeIndex2);
        endVertex1 = new EndVertexOfTwo(in_middleVertex, edgeIndex1);
        endVertex2 = new EndVertexOfThree(in_middleVertex, edgeIndex2, edgeIndex3);
    }

    private void ConstructorThreeAndOne(Vertex in_middleVertex, int edgeIndex1, int edgeIndex2, int edgeIndex3,
                                        int edgeIndex4) {
        middleVertex = new MiddleVertexOfThree(edgeIndex1, edgeIndex2);
        endVertex1 = new EndVertexOfTwo(in_middleVertex, edgeIndex1);
        endVertex2 = new EndVertexOfY(in_middleVertex, edgeIndex2, edgeIndex3, edgeIndex4);
    }

    private void Constructor(Vertex in_middleVertex, int edgeIndex1, int edgeIndex2, int edgeIndex3, int edgeIndex4,
                             int edgeIndex5) {
        middleVertex = new MiddleVertexOfThree(edgeIndex1, edgeIndex2);
        endVertex1 = new EndVertexOfThree(in_middleVertex, edgeIndex1, edgeIndex5);
        endVertex2 = new EndVertexOfY(in_middleVertex, edgeIndex2, edgeIndex3, edgeIndex4);
    }

    private void Constructor(Vertex in_middleVertex, int edgeIndex1, int edgeIndex2, int edgeIndex3, int edgeIndex4,
                             int edgeIndex5, int edgeIndex6) {
        middleVertex = new MiddleVertexOfThree(edgeIndex1, edgeIndex2);
        endVertex1 = new EndVertexOfY(in_middleVertex, edgeIndex1, edgeIndex5, edgeIndex6);
        endVertex2 = new EndVertexOfY(in_middleVertex, edgeIndex2, edgeIndex3, edgeIndex4);
    }

    private void ConstructorTwoAndTwo(Vertex in_middleVertex, int edgeIndex1, int edgeIndex2, int edgeIndex3,
                                      int edgeIndex4) {
        middleVertex = new MiddleVertexOfThree(edgeIndex1, edgeIndex2);
        endVertex1 = new EndVertexOfThree(in_middleVertex, edgeIndex1, edgeIndex4);
        endVertex2 = new EndVertexOfThree(in_middleVertex, edgeIndex2, edgeIndex3);
    }

    private void InsertAsMidpoint(Vertex in_middleVertex, Vertex neighbor1, Vertex neighbor2) {
        int edgeIndex1, edgeIndex2, edgeIndex3, edgeIndex4, edgeIndex5, edgeIndex6;
        try {
            edgeIndex1 = ((HasOneEdge) neighbor1).GetEdgeIndex(in_middleVertex).edgeIndex;
            edgeIndex2 = ((HasOneEdge) neighbor2).GetEdgeIndex(in_middleVertex).edgeIndex;
            try {
                edgeIndex3 = ((HasTwoSymmetricEdges) neighbor2).GetOppositeEdgeIndex(in_middleVertex).edgeIndex;
                try {
                    edgeIndex4 = ((HasTwoSymmetricEdges) neighbor1).GetOppositeEdgeIndex(in_middleVertex).edgeIndex;
                    // 2-2
                    ConstructorTwoAndTwo(in_middleVertex, edgeIndex1, edgeIndex2, edgeIndex3, edgeIndex4);
                } catch (ClassCastException e1) {
                    // 1-2
                    Constructor(in_middleVertex, edgeIndex1, edgeIndex2, edgeIndex3);
                } catch (TwoOppositeEdgeIndexesException e1) {
                    try {
                        edgeIndex4 = ((YVertex) neighbor1).GetEdgeIndex1();
                        edgeIndex5 = ((YVertex) neighbor1).GetEdgeIndex2();
                        // Y-2
                        Constructor(in_middleVertex, edgeIndex2, edgeIndex1, edgeIndex4, edgeIndex5, edgeIndex3);
                    } catch (ClassCastException e3) {
                        // T-2
                        Constructor(in_middleVertex, edgeIndex1, edgeIndex2, edgeIndex3);
                    }
                }
            } catch (ClassCastException e2) {
                try {
                    edgeIndex3 = ((HasTwoSymmetricEdges) neighbor1).GetOppositeEdgeIndex(in_middleVertex).edgeIndex;
                    // 2-1
                    Constructor(in_middleVertex, edgeIndex2, edgeIndex1, edgeIndex3);
                } catch (ClassCastException e1) {
                    // 1-1
                    Constructor(in_middleVertex, edgeIndex1, edgeIndex2);
                } catch (TwoOppositeEdgeIndexesException e1) {
                    try {
                        edgeIndex3 = ((YVertex) neighbor1).GetEdgeIndex1();
                        edgeIndex4 = ((YVertex) neighbor1).GetEdgeIndex2();
                        // Y-1
                        ConstructorThreeAndOne(in_middleVertex, edgeIndex2, edgeIndex1, edgeIndex3, edgeIndex4);
                    } catch (ClassCastException e3) {
                        // T-1
                        Constructor(in_middleVertex, edgeIndex1, edgeIndex2);
                    }
                }
            } catch (TwoOppositeEdgeIndexesException e2) {
                try {
                    edgeIndex3 = ((YVertex) neighbor2).GetEdgeIndex1();
                    edgeIndex4 = ((YVertex) neighbor2).GetEdgeIndex2();
                    try {
                        edgeIndex5 = ((HasTwoSymmetricEdges) neighbor1).GetOppositeEdgeIndex(in_middleVertex).edgeIndex;
                        // 2-Y
                        Constructor(in_middleVertex, edgeIndex1, edgeIndex2, edgeIndex3, edgeIndex4, edgeIndex5);
                    } catch (ClassCastException e1) {
                        // 1-Y
                        ConstructorThreeAndOne(in_middleVertex, edgeIndex1, edgeIndex2, edgeIndex3, edgeIndex4);
                    } catch (TwoOppositeEdgeIndexesException e1) {
                        try {
                            edgeIndex5 = ((YVertex) neighbor1).GetEdgeIndex1();
                            edgeIndex6 = ((YVertex) neighbor1).GetEdgeIndex2();
                            // Y-Y
                            Constructor(in_middleVertex, edgeIndex1, edgeIndex2, edgeIndex3, edgeIndex4, edgeIndex5,
                                    edgeIndex6);
                        } catch (ClassCastException e4) {
                            // T-Y
                            ConstructorThreeAndOne(in_middleVertex, edgeIndex1, edgeIndex2, edgeIndex3, edgeIndex4);
                        }
                    }
                } catch (ClassCastException e3) {
                    try {
                        edgeIndex3 = ((HasTwoSymmetricEdges) neighbor1).GetOppositeEdgeIndex(in_middleVertex).edgeIndex;
                        // 2-T
                        Constructor(in_middleVertex, edgeIndex2, edgeIndex1, edgeIndex3);
                    } catch (ClassCastException e1) {
                        // 1-T
                        Constructor(in_middleVertex, edgeIndex1, edgeIndex2);
                    } catch (TwoOppositeEdgeIndexesException e1) {
                        try {
                            edgeIndex3 = ((YVertex) neighbor1).GetEdgeIndex1();
                            edgeIndex4 = ((YVertex) neighbor1).GetEdgeIndex2();
                            // Y-T
                            ConstructorThreeAndOne(in_middleVertex, edgeIndex2, edgeIndex1, edgeIndex3, edgeIndex4);
                        } catch (ClassCastException e4) {
                            // T-T
                            Constructor(in_middleVertex, edgeIndex1, edgeIndex2);
                        }
                    }
                }
            }
        } catch (ClassCastException e) {
            throw (new ArithmeticException("NO EDGES!!" + "\nneighbor1:\n" + neighbor1 + "\nneighbor2:\n" + neighbor2
                    + "\n"));
        }
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
        return middleVertex.GetOppositeEdgeIndex(vertex);
    }

    @Override
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
            throw (new ArithmeticException("NOT NEIGHBOR!!" + "\nlineVertex:\n" + this + "\nneighbor:\n" + neighbor
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

    final public int GetEdgeIndex1() {
        return endVertex1.GetEdgeIndex1();
    }

    final public int GetEdgeIndex2() {
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
        double penalty = middleVertex.GetAnglePenalty();

        if (endVertex1 instanceof EndVertexOfThree) {
            penalty += ((EndVertexOfThree) endVertex1).GetAnglePenalty();
        } else {
            penalty += endVertex1.GetLengthPenalty();
        }

        if (endVertex2 instanceof EndVertexOfThree) {
            penalty += ((EndVertexOfThree) endVertex2).GetAnglePenalty();
        } else {
            penalty += endVertex2.GetLengthPenalty();
        }

        return penalty;
    }

    @Override
    final public NumeratorAndDenominator GetNumeratorAndDenominatorForPenalty() {
        NumeratorAndDenominator nd = middleVertex.GetNumeratorAndDenominatorForAnglePenalty();

        if (endVertex1 instanceof EndVertexOfThree) {
            nd.AddEqual(((EndVertexOfThree) endVertex1).GetNumeratorAndDenominatorForAnglePenalty());
        } else {
            nd.AddEqual(endVertex1.GetNumeratorAndDenominatorForLengthPenalty());
        }

        if (endVertex2 instanceof EndVertexOfThree) {
            nd.AddEqual(((EndVertexOfThree) endVertex2).GetNumeratorAndDenominatorForAnglePenalty());
        } else {
            nd.AddEqual(endVertex2.GetNumeratorAndDenominatorForLengthPenalty());
        }

        nd.AddEqual(middleVertex.GetNumeratorAndDenominatorForWeightDifferencePenalty());

        return nd;
    }

    @Override
    final public Vertex Restructure() {
        return this;
    }

    // final public Vektor GetTangent(Vertex neighbor) {
    // return GetTangentOfCenterOfCircleAround(neighbor,GetOppositeVertex(neighbor));
    // }

    @Override
    final public String toString() {
        String s = "LineVertex:\t";
        s += super.toString() + "\t";
        s += middleVertex.toString();
        s += endVertex1.toString();
        s += endVertex2.toString();
        return s;
    }
}
