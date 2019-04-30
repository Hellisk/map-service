package algorithm.mapinference.lineclustering.pcurves.PrincipalCurve.Vertex;

import algorithm.mapinference.lineclustering.pcurves.PrincipalCurve.Vertex.Elements.*;

final public class StarOfManyVertex extends Vertex implements HasOneEdge, JoinVertex {
	EndVertexOfTwo[] endVertexs;
	private MiddleVertexOfMany middleVertex;
	private int degree;
	
	// for PrincipalCurveClass.JoinVertices()
	public StarOfManyVertex(Vertex vertex1, Vertex vertex2) {
		super(vertex1);
		degree = vertex1.GetDegree() + vertex2.GetDegree();
		endVertexs = new EndVertexOfTwo[degree];
		int d = 0;
		for (int i = 0; i < vertex1.GetElementVertices().length; i++)
			if (vertex1.GetElementVertices()[i] instanceof EndVertexOfTwo)
				endVertexs[d++] = (EndVertexOfTwo) vertex1.GetElementVertices()[i];
		for (int i = 0; i < vertex2.GetElementVertices().length; i++)
			if (vertex2.GetElementVertices()[i] instanceof EndVertexOfTwo)
				endVertexs[d++] = (EndVertexOfTwo) vertex2.GetElementVertices()[i];
		middleVertex = new MiddleVertexOfMany(endVertexs);
	}
	
	// for Degrade()
	private StarOfManyVertex(StarOfManyVertex starOfManyVertex, Vertex neighbor) {
		super(starOfManyVertex);
		degree = starOfManyVertex.GetDegree() - 1;
		endVertexs = new EndVertexOfTwo[degree];
		int d = 0, i;
		for (i = 0; i < degree; i++, d++) {
			if (i == d && neighbor.equals(starOfManyVertex.GetEdge(i).GetVertex2()))
				d++;
			endVertexs[i] = starOfManyVertex.endVertexs[d];
		}
		if (d == i)
			throw (new ArithmeticException("NOT NEIGHBOR!!" + "\nStarOfManyVertex:\n" + starOfManyVertex
					+ "\nneighbor:\n" + neighbor + "\n"));
		middleVertex = new MiddleVertexOfMany(endVertexs);
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
		for (int i = 0; i < degree; i++) {
			if (neighbor.equals(endVertexs[i].GetEdge1().GetVertex2())) {
				endVertexs[i] = MaintainEndVertex(neighbor);
				return;
			}
		}
		throw (new ArithmeticException("NOT NEIGHBOR!!" + "\nStarOfManyVertex:\n" + this + "\nneighbor:\n" + neighbor
				+ "\n"));
	}
	
	@Override
	final public Vertex Degrade(Vertex neighbor) {
		if (degree > 3)
			return new StarOfManyVertex(this, neighbor);
		else
			return new LineVertex(this, neighbor);
	}
	
	final public Edge GetEdge(int i) {
		return endVertexs[i].GetEdge1();
	}
	
	final int GetEdgeIndex(int i) {
		return endVertexs[i].GetEdgeIndex1();
	}
	
	@Override
	final public int GetDegree() {
		return degree;
	}
	
	@Override
	final public Edge[] GetEdges() {
		Edge[] edges = new Edge[degree];
		for (int i = 0; i < degree; i++)
			edges[i] = GetEdge(i);
		return edges;
	}
	
	@Override
	final public Vertex[] GetNeighbors() {
		Vertex[] neighbors = new Vertex[degree];
		for (int i = 0; i < degree; i++)
			neighbors[i] = GetEdge(i).GetVertex2();
		return neighbors;
	}
	
	@Override
	final public int[] GetEdgeIndexes() {
		int[] edgeIndexes = new int[degree];
		for (int i = 0; i < degree; i++)
			edgeIndexes[i] = GetEdgeIndex(i);
		return edgeIndexes;
	}
	
	@Override
	final public int[] GetNeighborIndexes() {
		int[] neighborIndexes = new int[degree];
		for (int i = 0; i < degree; i++)
			neighborIndexes[i] = GetEdge(i).GetVertexIndex2();
		return neighborIndexes;
	}
	
	@Override
	final public ElementVertex[] GetElementVertices() {
		ElementVertex[] elementVertices = new ElementVertex[degree + 1];
		elementVertices[0] = middleVertex;
		for (int i = 0; i < degree; i++)
			elementVertices[i + 1] = endVertexs[i];
		return elementVertices;
	}
	
	@Override
	final public ElementVertex GetMainElementVertex() {
		return middleVertex;
	}
	
	@Override
	final public double GetPenalty() {
		double penalty = 0.0;
		for (int i = 0; i < degree; i++) {
			try {
				penalty += ((EndVertexOfThree) endVertexs[i]).GetAnglePenalty();
			} catch (ClassCastException e) {
			}
			penalty += endVertexs[i].GetLengthPenalty();
		}
		return penalty;
	}
	
	@Override
	final public NumeratorAndDenominator GetNumeratorAndDenominatorForPenalty() {
		NumeratorAndDenominator nd = new NumeratorAndDenominator(this);
		for (int i = 0; i < degree; i++) {
			try {
				nd.AddEqual(((EndVertexOfThree) endVertexs[i]).GetNumeratorAndDenominatorForAnglePenalty());
			} catch (ClassCastException e) {
			}
			nd.AddEqual(endVertexs[i].GetNumeratorAndDenominatorForLengthPenalty());
		}
		return nd;
	}
	
	@Override
	final public Vertex Restructure() {
		return this;
	}
	
	@Override
	final public String toString() {
		String s = "StarOfManyVertex(" + degree + "):\t";
		s += super.toString() + "\t";
		s += middleVertex.toString();
		for (int i = 0; i < degree; i++)
			s += endVertexs[i].toString();
		return s;
	}
}
