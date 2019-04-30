package algorithm.mapinference.lineclustering.pcurves.PrincipalCurve.Vertex.Elements;

final public class MiddleVertexOfMany extends ElementVertex implements HasOneEdge {
	private int[] edgeIndexs;
	private boolean[] forwards;
	
	public MiddleVertexOfMany(EndVertexOfTwo[] endVertexs) {
		edgeIndexs = new int[endVertexs.length];
		forwards = new boolean[endVertexs.length];
		for (int i = 0; i < endVertexs.length; i++) {
			edgeIndexs[i] = endVertexs[i].edgeIndex1;
			forwards[i] = endVertexs[i].forward1;
		}
	}
	
	final Edge GetEdge(int i) {
		if (forwards[i])
			return GetEdgeAt(edgeIndexs[i]);
		else
			return GetEdgeAt(edgeIndexs[i]).Reverse();
	}
	
	final public int GetEdgeIndex(int i) {
		return edgeIndexs[i];
	}
	
	@Override
	final public Vertex GetVertex() {
		return GetEdge(0).GetVertex1();
	}
	
	@Override
	final public EdgeDirection GetEdgeIndex(Vertex vertex) {
		for (int i = 0; i < edgeIndexs.length; i++)
			if (GetEdge(i).GetVertex2().equals(vertex))
				return new EdgeDirection(edgeIndexs[i], forwards[i]);
		String e = "vertex:\n" + vertex;
		for (int i = 0; i < edgeIndexs.length; i++)
			e += "\nedge" + i + "Vertex2\n" + GetEdge(i).GetVertex2() + "\n";
		throw (new ArithmeticException(e));
	}
	
	// For insertion
	@Override
	final public void ReplaceEdgeIndexesForInsertion(int oei, int nei1, int nei2, Vertex vertex) {
		for (int i = 0; i < edgeIndexs.length; i++) {
			if (edgeIndexs[i] == oei) {
				int j;
				if (i == 0)
					j = 1;
				else
					j = 0;
				if (GetEdge(j).GetVertex1().equals(GetEdgeAt(nei1).GetVertex1())) {
					forwards[i] = true;
					edgeIndexs[i] = nei1;
				} else if (GetEdge(j).GetVertex1().equals(GetEdgeAt(nei1).GetVertex2())) {
					forwards[i] = false;
					edgeIndexs[i] = nei1;
				} else if (GetEdge(j).GetVertex1().equals(GetEdgeAt(nei2).GetVertex1())) {
					forwards[i] = true;
					edgeIndexs[i] = nei2;
				} else if (GetEdge(j).GetVertex1().equals(GetEdgeAt(nei2).GetVertex2())) {
					forwards[i] = false;
					edgeIndexs[i] = nei2;
				} else
					throw (new ArithmeticException("GetEdge(j).GetVertex1():\n" + GetEdge(j).GetVertex1()
							+ "\nedges[nei1].GetVertex1()\n" + GetEdgeAt(nei1).GetVertex1()
							+ "\nedges[nei1].GetVertex2()\n" + GetEdgeAt(nei1).GetVertex2()
							+ "\nedges[nei2].GetVertex1()\n" + GetEdgeAt(nei2).GetVertex1()
							+ "\nedges[nei2].GetVertex2()\n" + GetEdgeAt(nei2).GetVertex2() + "\n"));
			}
		}
	}
	
	@Override
	final public void ReplaceEdgeIndexesForDeletion(int oei, int nei) {
		for (int i = 0; i < edgeIndexs.length; i++) {
			if (edgeIndexs[i] == oei) {
				if (GetVertex().equals(GetEdgeAt(nei).GetVertex1())) {
					forwards[i] = true;
					edgeIndexs[i] = nei;
				} else if (GetVertex().equals(GetEdgeAt(nei).GetVertex2())) {
					forwards[i] = false;
					edgeIndexs[i] = nei;
				} else
					throw (new ArithmeticException("\nGetEdge(0).GetVertex1():\n" + GetEdge(0).GetVertex1()
							+ "\nedges[nei].GetVertex1()\n" + GetEdgeAt(nei).GetVertex1()
							+ "\nedges[nei].GetVertex2()\n" + GetEdgeAt(nei).GetVertex2()));
			}
		}
	}
	
	@Override
	final public void DecrementEdgeIndexes(int lowerIndex) {
		for (int i = 0; i < edgeIndexs.length; i++) {
			if (edgeIndexs[i] > lowerIndex)
				edgeIndexs[i]--;
		}
	}
	
	@Override
	final public void SetVertexIndexOfEdges(int vi) {
		for (int i = 0; i < edgeIndexs.length; i++) {
			if (forwards[i])
				GetEdgeAt(edgeIndexs[i]).SetVertexIndex1(vi);
			else
				GetEdgeAt(edgeIndexs[i]).SetVertexIndex2(vi);
		}
	}
	
	private int GetSetSizeSum() {
		int sum = GetVertex().GetSetSize();
		for (int edgeIndex : edgeIndexs)
			sum += GetEdgeAt(edgeIndex).GetSetSize();
		return sum;
	}
	
	@Override
	final public boolean EmptySet() {
		return GetSetSizeSum() == 0;
	}
	
	@Override
	final public NumeratorAndDenominator GetNumeratorAndDenominatorForMSE() {
		NumeratorAndDenominator nd = GetVertex().GetNumeratorAndDenominatorForMSETimesWeight();
		for (int i = 0; i < edgeIndexs.length; i++)
			nd.AddEqual(GetEdge(i).GetNumeratorAndDenominatorForMSETimesWeight());
		return nd;
	}
	
	@Override
	final public String toString() {
		String s = new String();
		for (int i = 0; i < edgeIndexs.length; i++)
			s += "," + edgeIndexs[i] + ":" + GetEdge(i).toString();
		return s;
	}
}
