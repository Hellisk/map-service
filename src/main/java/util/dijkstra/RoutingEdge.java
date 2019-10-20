package util.dijkstra;

public class RoutingEdge {
	private final int index;
	private final int fromNodeIndex;
	private final int toNodeIndex;
	private final double length;
	
	RoutingEdge(int index, int fromNodeIndex, int toNodeIndex, double length) {
		this.index = index;
		this.fromNodeIndex = fromNodeIndex;
		this.toNodeIndex = toNodeIndex;
		this.length = length;
	}
	
	int getFromNodeIndex() {
		return fromNodeIndex;
	}
	
	int getToNodeIndex() {
		return toNodeIndex;
	}
	
	public double getLength() {
		return length;
	}
	
	public int getIndex() {
		return index;
	}
}