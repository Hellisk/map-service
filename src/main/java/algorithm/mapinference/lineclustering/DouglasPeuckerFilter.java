package algorithm.mapinference.lineclustering;

import util.function.DistanceFunction;
import util.object.roadnetwork.RoadNode;
import util.object.roadnetwork.RoadWay;
import util.object.spatialobject.Point;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

public class DouglasPeuckerFilter {
	private double epsilon;
	private DistanceFunction distFunc;
	
	public DouglasPeuckerFilter(double epsilon, DistanceFunction distFunc) {
		this.distFunc = distFunc;
		this.epsilon = epsilon;
	}
	
	/**
	 * Remove the unnecessary points on a road way to simplify the road shape. The output <tt>RoadWay</tt> is the same object with
	 * a different <tt>RoadNode</tt> set.
	 *
	 * @param originalWay The way to be simplified.
	 * @return Same road with less nodes.
	 */
	public RoadWay dpSimplifier(RoadWay originalWay) {
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
	
	/**
	 * Remove the unnecessary points on a road way to simplify the road shape. The output <tt>RoadWay</tt> is the same object with
	 * a different <tt>RoadNode</tt> set.
	 *
	 * @param polyline The way to be simplified.
	 * @return Same road with less nodes.
	 */
	public List<Integer> dpSimplifier(List<? extends Point> polyline) {
		BitSet bitSet = new BitSet(polyline.size());
		// the end points should be kept
		bitSet.set(0);
		bitSet.set(polyline.size() - 1);
		if (!polyline.get(0).equals2D(polyline.get(polyline.size() - 1))) {
			List<Range> stack = new ArrayList<>();
			stack.add(new Range(0, polyline.size() - 1));
			
			while (!stack.isEmpty()) {
				Range range = stack.remove(stack.size() - 1);
				
				int index = -1;
				double maxDist = 0f;
				
				// find index of point with maximum square distance from first and last point
				for (int i = range.first + 1; i < range.last; ++i) {
					double currDist = distFunc.pointToSegmentProjectionDistance(polyline.get(i).x(), polyline.get(i).y(),
							polyline.get(range.first).x(), polyline.get(range.first).y(), polyline.get(range.last).x(),
							polyline.get(range.last).y());
					
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
		}
		List<Integer> remainPointIndex = new ArrayList<>(bitSet.cardinality());
		for (int index = bitSet.nextSetBit(0); index >= 0; index = bitSet.nextSetBit(index + 1)) {
			remainPointIndex.add(index);
		}
		return remainPointIndex;
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