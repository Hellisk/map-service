package algorithm.mapinference.lineclustering;

import util.function.GreatCircleDistanceFunction;
import util.object.roadnetwork.RoadNode;
import util.object.roadnetwork.RoadWay;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

class DouglasPeuckerFilter {
	private double epsilon;
	private GreatCircleDistanceFunction distFunc = new GreatCircleDistanceFunction();
	
	public DouglasPeuckerFilter(double epsilon) {
		this.epsilon = epsilon;
	}
	
	protected RoadWay dpSimplifier(RoadWay originalWay) {
		BitSet bitSet = new BitSet(originalWay.size());
		bitSet.set(0);
		bitSet.set(originalWay.size() - 1);
		
		List<Range> stack = new ArrayList<>();
		stack.add(new Range(0, originalWay.size() - 1));
		
		while (!stack.isEmpty()) {
			Range range = stack.remove(stack.size() - 1);
			
			int index = -1;
			double maxDist = 0f;
			
			// find index of point with maximum square distance from first and last point
			for (int i = range.first + 1; i < range.last; ++i) {
				double currDist = distFunc.pointToSegmentProjectionDistance(originalWay.getNode(i).lon(), originalWay.getNode(i).lat(),
						originalWay.getNode(range.first).lon(), originalWay.getNode(range.first).lat(),
						originalWay.getNode(range.last).lon(), originalWay.getNode(range.last).lat());
				
				if (currDist > maxDist) {
					index = i;
					maxDist = currDist;
				}
			}
			
			if (maxDist > epsilon) {
				bitSet.set(index);
				
				stack.add(new Range(range.first, index));
				stack.add(new Range(index, range.last));
			}
		}
		
		List<RoadNode> newPoints = new ArrayList<>(bitSet.cardinality());
		for (int index = bitSet.nextSetBit(0); index >= 0; index = bitSet.nextSetBit(index + 1)) {
			newPoints.add(originalWay.getNode(index));
		}
		originalWay.setNodes(newPoints);
		
		return originalWay;
	}
	
	private static class Range {
		int first;
		int last;
		
		private Range(int first, int last) {
			this.first = first;
			this.last = last;
		}
	}
}