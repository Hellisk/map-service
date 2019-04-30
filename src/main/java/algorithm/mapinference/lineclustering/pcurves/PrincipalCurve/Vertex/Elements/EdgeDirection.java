package algorithm.mapinference.lineclustering.pcurves.PrincipalCurve.Vertex.Elements;

final public class EdgeDirection {
	public int edgeIndex;
	public boolean forward;
	
	public EdgeDirection(int in_edgeIndex, boolean in_forward) {
		edgeIndex = in_edgeIndex;
		forward = in_forward;
	}
}
