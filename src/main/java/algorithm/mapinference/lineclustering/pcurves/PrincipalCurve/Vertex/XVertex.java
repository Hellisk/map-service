package algorithm.mapinference.lineclustering.pcurves.PrincipalCurve.Vertex;

import algorithm.mapinference.lineclustering.pcurves.PrincipalCurve.Vertex.Elements.*;

final public class XVertex extends Vertex implements HasTwoSymmetricEdges, JoinVertex {
	private MiddleVertexOfFive middleVertex;
	private MiddleVertexOfThree lineVertex1;
	private MiddleVertexOfThree lineVertex2;
	private EndVertexOfTwo endVertex1;
	private EndVertexOfTwo endVertex2;
	private EndVertexOfTwo endVertex3;
	private EndVertexOfTwo endVertex4;
	
	// For PrincipalCurveOfTemplate.JoinVertices()
	public XVertex(LineVertex in_lineVertex1, LineVertex in_lineVertex2) {
		super(in_lineVertex1);
		
		endVertex1 = in_lineVertex1.endVertex1;
		endVertex2 = in_lineVertex1.endVertex2;
		endVertex3 = in_lineVertex2.endVertex1;
		endVertex4 = in_lineVertex2.endVertex2;
		SetMiddleVertices();
	}
	
	// For PrincipalCurveOfTemplate.JoinVertices()
	public XVertex(CornerVertex cornerVertex1, CornerVertex cornerVertex2) {
		super(cornerVertex1);
		
		endVertex1 = cornerVertex1.endVertex1;
		endVertex2 = cornerVertex1.endVertex2;
		endVertex3 = cornerVertex2.endVertex1;
		endVertex4 = cornerVertex2.endVertex2;
		SetMiddleVertices();
	}
	
	// For PrincipalCurveOfTemplate.JoinVertices()
	public XVertex(TVertex tVertex, EndVertex endVertex) {
		super(tVertex);
		
		endVertex1 = tVertex.endVertex;
		endVertex2 = endVertex.endVertex;
		endVertex3 = tVertex.endVertex1;
		endVertex4 = tVertex.endVertex2;
		SetMiddleVertices();
	}
	
	// For PrincipalCurveOfTemplate.JoinVertices()
	public XVertex(StarOfThreeVertex starOfThreeVertex, EndVertex endVertex) {
		super(starOfThreeVertex);
		
		endVertex1 = starOfThreeVertex.endVertex1;
		endVertex2 = endVertex.endVertex;
		endVertex3 = starOfThreeVertex.endVertex2;
		endVertex4 = starOfThreeVertex.endVertex3;
		SetMiddleVertices();
	}
	
	// For StarOfFourVertex.Restructure()
	public XVertex(EndVertexOfTwo in_endVertex1, EndVertexOfTwo in_endVertex2, EndVertexOfTwo in_endVertex3,
				   EndVertexOfTwo in_endVertex4) {
		super(in_endVertex1.GetVertex());
		
		endVertex1 = in_endVertex1;
		endVertex2 = in_endVertex2;
		endVertex3 = in_endVertex3;
		endVertex4 = in_endVertex4;
		SetMiddleVertices();
		MaintainNeighbors();
	}
	
	private void SetMiddleVertices() {
		lineVertex1 = new MiddleVertexOfThree(endVertex1, endVertex2);
		lineVertex2 = new MiddleVertexOfThree(endVertex3, endVertex4);
		middleVertex = new MiddleVertexOfFive(endVertex1, endVertex2, endVertex3, endVertex4);
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
		else if (neighbor.equals(endVertex3.GetEdge1().GetVertex2()))
			endVertex3 = MaintainEndVertex(neighbor);
		else
			endVertex4 = MaintainEndVertex(neighbor);
	}
	
	@Override
	final public Vertex Degrade(Vertex neighbor) {
		EndVertexOfTwo[] endVertices = new EndVertexOfTwo[3];
		if (neighbor.equals(endVertex1.GetEdge1().GetVertex2())) {
			endVertices[0] = endVertex2;
			endVertices[1] = endVertex3;
			endVertices[2] = endVertex4;
		} else if (neighbor.equals(endVertex2.GetEdge1().GetVertex2())) {
			endVertices[0] = endVertex1;
			endVertices[1] = endVertex3;
			endVertices[2] = endVertex4;
		} else if (neighbor.equals(endVertex3.GetEdge1().GetVertex2())) {
			endVertices[0] = endVertex4;
			endVertices[1] = endVertex1;
			endVertices[2] = endVertex2;
		} else {
			endVertices[0] = endVertex3;
			endVertices[1] = endVertex1;
			endVertices[2] = endVertex2;
		}
		double angle1 =
				180
						* Math.acos(CosAngle(endVertices[0].GetEdge1().GetVertex2(), endVertices[1].GetEdge1()
						.GetVertex2())) / Math.PI;
		double angle2 =
				180
						* Math.acos(CosAngle(endVertices[0].GetEdge1().GetVertex2(), endVertices[2].GetEdge1()
						.GetVertex2())) / Math.PI;
		if (RectAngle(angle1) && RectAngle(angle2))
			return new TVertex(endVertices[0], endVertices[1], endVertices[2]);
		else if (angle1 < angle2)
			return new YVertex(endVertices[2], endVertices[0], endVertices[1]);
		else
			return new YVertex(endVertices[1], endVertices[0], endVertices[2]);
	}
	
	final Edge GetEdge1() {
		return endVertex1.GetEdge1();
	}
	
	final Edge GetEdge2() {
		return endVertex2.GetEdge1();
	}
	
	final Edge GetEdge3() {
		return endVertex3.GetEdge1();
	}
	
	final Edge GetEdge4() {
		return endVertex4.GetEdge1();
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
	
	final int GetEdgeIndex4() {
		return endVertex4.GetEdgeIndex1();
	}
	
	@Override
	final public int GetDegree() {
		return 4;
	}
	
	@Override
	final public Edge[] GetEdges() {
		Edge[] edges = {GetEdge1(), GetEdge2(), GetEdge3(), GetEdge4()};
		return edges;
	}
	
	@Override
	final public Vertex[] GetNeighbors() {
		Vertex[] neighbors =
				{GetEdge1().GetVertex2(), GetEdge2().GetVertex2(), GetEdge3().GetVertex2(), GetEdge4().GetVertex2()};
		return neighbors;
	}
	
	@Override
	final public int[] GetEdgeIndexes() {
		int[] edgeIndexes = {GetEdgeIndex1(), GetEdgeIndex2(), GetEdgeIndex3(), GetEdgeIndex4()};
		return edgeIndexes;
	}
	
	@Override
	final public int[] GetNeighborIndexes() {
		int[] neighborIndexes =
				{GetEdge1().GetVertexIndex2(), GetEdge2().GetVertexIndex2(), GetEdge3().GetVertexIndex2(),
						GetEdge4().GetVertexIndex2()};
		return neighborIndexes;
	}
	
	@Override
	final public ElementVertex[] GetElementVertices() {
		ElementVertex[] elementVertices =
				{middleVertex, lineVertex1, lineVertex2, endVertex1, endVertex2, endVertex3, endVertex4};
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
		
		try {
			penalty += ((EndVertexOfThree) endVertex4).GetAnglePenalty();
		} catch (ClassCastException e) {
			penalty += endVertex4.GetLengthPenalty();
		}
		
		return penalty;
	}
	
	@Override
	final public NumeratorAndDenominator GetNumeratorAndDenominatorForPenalty() {
		NumeratorAndDenominator nd = new NumeratorAndDenominator(this);
		nd.AddEqual(lineVertex1.GetNumeratorAndDenominatorForAnglePenalty());
		nd.AddEqual(lineVertex2.GetNumeratorAndDenominatorForAnglePenalty());
		
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
		
		try {
			nd.AddEqual(((EndVertexOfThree) endVertex4).GetNumeratorAndDenominatorForAnglePenalty());
		} catch (ClassCastException e) {
			nd.AddEqual(endVertex4.GetNumeratorAndDenominatorForLengthPenalty());
		}
		
		nd.AddEqual(lineVertex1.GetNumeratorAndDenominatorForWeightDifferencePenalty());
		nd.AddEqual(lineVertex2.GetNumeratorAndDenominatorForWeightDifferencePenalty());
		
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
		String s = "XVertex:\t";
		s += super.toString() + "\t";
		s += middleVertex.toString();
		s += endVertex1.toString();
		s += endVertex2.toString();
		s += endVertex3.toString();
		s += endVertex4.toString();
		return s;
	}
}
