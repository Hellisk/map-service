package algorithm.mapinference.lineclustering.pcurves.PrincipalCurve.Vertex;

import algorithm.mapinference.lineclustering.pcurves.PrincipalCurve.Vertex.Elements.*;
import algorithm.mapinference.lineclustering.pcurves.Utilities.MyMath;

final public class StarOfFourVertex extends Vertex implements HasOneEdge, JoinVertex {
	EndVertexOfTwo endVertex1;
	EndVertexOfTwo endVertex2;
	EndVertexOfTwo endVertex3;
	EndVertexOfTwo endVertex4;
	private MiddleVertexOfFive middleVertex;
	
	// For PrincipalCurve2D::InitializeToCurves()
	public StarOfFourVertex(LineVertex in_lineVertex1, LineVertex in_lineVertex2) {
		super(in_lineVertex1);
		
		endVertex1 = in_lineVertex1.endVertex1;
		endVertex2 = in_lineVertex1.endVertex2;
		endVertex3 = in_lineVertex2.endVertex1;
		endVertex4 = in_lineVertex2.endVertex2;
		SetMiddleVertices();
	}
	
	// For PrincipalCurve2D::InitializeToCurves()
	public StarOfFourVertex(StarOfThreeVertex starOfThreeVertex, EndVertex endVertex) {
		super(starOfThreeVertex);
		
		endVertex1 = starOfThreeVertex.endVertex1;
		endVertex2 = starOfThreeVertex.endVertex2;
		endVertex3 = starOfThreeVertex.endVertex3;
		endVertex4 = endVertex.endVertex;
		SetMiddleVertices();
	}
	
	// For PrincipalCurveOfTemplate::MergeStarOfThreeVertices()
	public StarOfFourVertex(Vertex midVertex, int edgeIndex1, int edgeIndex2, int edgeIndex3, int edgeIndex4,
							EndVertex vertex1, EndVertex vertex2, EndVertex vertex3, EndVertex vertex4) {
		super(midVertex);
		
		endVertex1 = new EndVertexOfThree(midVertex, edgeIndex1, vertex1.GetEdgeIndex1());
		endVertex2 = new EndVertexOfThree(midVertex, edgeIndex2, vertex2.GetEdgeIndex1());
		endVertex3 = new EndVertexOfThree(midVertex, edgeIndex3, vertex3.GetEdgeIndex1());
		endVertex4 = new EndVertexOfThree(midVertex, edgeIndex4, vertex4.GetEdgeIndex1());
		SetMiddleVertices();
	}
	
	private void SetMiddleVertices() {
		middleVertex = new MiddleVertexOfFive(endVertex1, endVertex2, endVertex3, endVertex4);
	}
	
	// final public void ArrangeEdges() {
	// double angle12 = 180*Math.acos(CosAngle(GetEdge1().GetVertex2(),GetEdge2().GetVertex2()))/Math.PI;
	// double angle13 = 180*Math.acos(CosAngle(GetEdge1().GetVertex2(),GetEdge3().GetVertex2()))/Math.PI;
	// double angle14 = 180*Math.acos(CosAngle(GetEdge1().GetVertex2(),GetEdge4().GetVertex2()))/Math.PI;
	// double angle23 = 180*Math.acos(CosAngle(GetEdge2().GetVertex2(),GetEdge3().GetVertex2()))/Math.PI;
	// double angle24 = 180*Math.acos(CosAngle(GetEdge2().GetVertex2(),GetEdge4().GetVertex2()))/Math.PI;
	// double angle34 = 180*Math.acos(CosAngle(GetEdge3().GetVertex2(),GetEdge4().GetVertex2()))/Math.PI;
	
	// if (Math.abs(angle12 + angle23 - angle13) < 0.1 || Math.abs(angle14 + angle34 - angle13) < 0.1) {
	// EndVertexOfTwo endVertex = endVertex2;
	// endVertex2 = endVertex3;
	// endVertex3 = endVertex;
	// }
	// else if (Math.abs(angle12 + angle24 - angle14) < 0.1 || Math.abs(angle13 + angle34 - angle14) < 0.1) {
	// EndVertexOfTwo endVertex = endVertex2;
	// endVertex2 = endVertex4;
	// endVertex4 = endVertex;
	// }
	
	// SetMiddleVertices();
	// }
	
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
		else if (neighbor.equals(endVertex4.GetEdge1().GetVertex2()))
			endVertex4 = MaintainEndVertex(neighbor);
		else
			throw (new ArithmeticException("NOT NEIGHBOR!!" + "\nstarOfFourVertex:\n" + this + "\nneighbor:\n"
					+ neighbor + "\n"));
	}
	
	@Override
	final public Vertex Degrade(Vertex neighbor) {
		return new StarOfThreeVertex(this, neighbor);
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
	
	final public Edge GetEdge4() {
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
		ElementVertex[] elementVertices = {middleVertex, endVertex1, endVertex2, endVertex3, endVertex4
				// ,middleVertex1,middleVertex2,middleVertex3,middleVertex4
		};
		return elementVertices;
	}
	
	@Override
	final public ElementVertex GetMainElementVertex() {
		return middleVertex;
	}
	
	@Override
	final public double GetPenalty() {
		double penalty = 0.0;
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
		
		return penalty * 0.1;
	}
	
	@Override
	final public NumeratorAndDenominator GetNumeratorAndDenominatorForPenalty() {
		NumeratorAndDenominator nd = new NumeratorAndDenominator(this);
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
		nd.MulEqual(0.1);
		return nd;
	}
	
	@Override
	final public Vertex Restructure() {
		// ArrangeEdges();
		double angle12 = 180 * MyMath.Acos(CosAngle(GetEdge1().GetVertex2(), GetEdge2().GetVertex2())) / Math.PI;
		double angle13 = 180 * MyMath.Acos(CosAngle(GetEdge1().GetVertex2(), GetEdge3().GetVertex2())) / Math.PI;
		double angle14 = 180 * MyMath.Acos(CosAngle(GetEdge1().GetVertex2(), GetEdge4().GetVertex2())) / Math.PI;
		double angle23 = 180 * MyMath.Acos(CosAngle(GetEdge2().GetVertex2(), GetEdge3().GetVertex2())) / Math.PI;
		double angle24 = 180 * MyMath.Acos(CosAngle(GetEdge2().GetVertex2(), GetEdge4().GetVertex2())) / Math.PI;
		double angle34 = 180 * MyMath.Acos(CosAngle(GetEdge3().GetVertex2(), GetEdge4().GetVertex2())) / Math.PI;
		
		if (Math.abs(angle12 + angle23 - angle13) < 0.0001 || Math.abs(angle14 + angle34 - angle13) < 0.0001)
			return new XVertex(endVertex1, endVertex3, endVertex2, endVertex4);
		else if (Math.abs(angle12 + angle24 - angle14) < 0.0001 || Math.abs(angle13 + angle34 - angle14) < 0.0001)
			return new XVertex(endVertex1, endVertex4, endVertex3, endVertex2);
		else
			return new XVertex(endVertex1, endVertex2, endVertex3, endVertex4);
	}
	
	// final public Vektor GetTangent(Vertex neighbor) {
	// return GetTangentWithSameAngleAsAtNeighbor(neighbor);
	// }
	
	@Override
	final public String toString() {
		String s = "StarOfFourVertex:\t";
		s += super.toString() + "\t";
		s += middleVertex.toString();
		s += endVertex1.toString();
		s += endVertex2.toString();
		s += endVertex3.toString();
		s += endVertex4.toString();
		return s;
	}
}
