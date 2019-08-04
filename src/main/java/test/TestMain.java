package test;

import util.dijkstra.MinPriorityQueue;
import util.function.GreatCircleDistanceFunction;
import util.function.SpatialUtils;
import util.object.spatialobject.Point;
import util.object.structure.InverselyComparableObject;
import util.object.structure.Pair;

import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Random;

public class TestMain {
	public static void main(String[] args) {
//		minPriorityQueueTest();
//		converterTest();
		testProjection();
	}
	
	private static void converterTest() {
		// WGS84 point: 8.582322,49.855866, corresponding UTM:469976.89,5522689.08
		Pair<Double, Double> resultUTM = SpatialUtils.convertWGS2UTM(8.582322, 49.855866);
		Pair<Double, Double> resultWGS = SpatialUtils.convertUTM2WGS(469976.89, 5522689.08, 32, 'U');
		System.out.println(resultUTM._1() + "," + resultUTM._2() + ". Correct result: 469976.89,5522689.08");
		System.out.println(resultWGS._1() + "," + resultWGS._2() + ". Correct result: 8.582322,49.855866");
		// GCJ-02 point: 116.42513 39.96940, corresponding WGS84:116.418894,39.968005
		Pair<Double, Double> resultWGS2 = SpatialUtils.convertGCJ2WGS(116.42513, 39.96940);
		Pair<Double, Double> resultUTM2 = SpatialUtils.convertWGS2UTM(resultWGS2._1(), resultWGS2._2());
		System.out.println(resultWGS2._1() + "," + resultWGS2._2() + ". Correct result: 116.418894,39.968005");
		System.out.println(resultUTM2._1() + "," + resultUTM2._2() + ". Correct result: 469976.89,5522689.08");
	}
	
	private static void priorityQueueTest() {
		Queue<InverselyComparableObject<String, Integer>> topRankedCandidates = new PriorityQueue<>();
		topRankedCandidates.add(new InverselyComparableObject<>("Text1", 100.1, 1, 1));
		topRankedCandidates.add(new InverselyComparableObject<>("Test2", 110.1, 2, 1));
		topRankedCandidates.add(new InverselyComparableObject<>("Test3", 200.1, 3, 1));
		topRankedCandidates.add(new InverselyComparableObject<>("Test4", 10.1, 4, 1));
		topRankedCandidates.add(new InverselyComparableObject<>("Test5", 100.12, 5, 1));
		topRankedCandidates.add(new InverselyComparableObject<>("Test6", 15.1, 6, 1));
		topRankedCandidates.add(new InverselyComparableObject<>("Test7", 1.1, 7, 1));
		topRankedCandidates.add(new InverselyComparableObject<>("Test8", 1.1, 8, 1));
		topRankedCandidates.add(new InverselyComparableObject<>("Test9", 1.1, 9, 1));
		topRankedCandidates.add(new InverselyComparableObject<>("Test10", 1.1, 10, 1));
		Random randomValue = new Random();
		for (int count = 11; count < 100; count++) {
			topRankedCandidates.add(new InverselyComparableObject<>("Test" + count, randomValue.nextDouble(), count, 1));
		}
		while (!topRankedCandidates.isEmpty())
			System.out.println(topRankedCandidates.remove().getObject());
	}
	
	private static void minPriorityQueueTest() {
		MinPriorityQueue minHeap = new MinPriorityQueue();
		minHeap.decreaseKey(14, 78);
		minHeap.decreaseKey(28, 100);
		minHeap.decreaseKey(17, 23);
		System.out.println(minHeap.extractMin());
		minHeap.decreaseKey(15, 78);
		System.out.println(minHeap.extractMin());
		minHeap.decreaseKey(14, 56);
		minHeap.decreaseKey(17, 100);
		System.out.println(minHeap.extractMin());
		System.out.println(minHeap.extractMin());
		minHeap.decreaseKey(18, 48);
		minHeap.decreaseKey(19, 100);
		minHeap.decreaseKey(14, 25);
		System.out.println(minHeap.extractMin());
		minHeap.decreaseKey(21, 11);
		minHeap.decreaseKey(22, 50);
		minHeap.decreaseKey(23, 51);
		minHeap.decreaseKey(24, 70);
		System.out.println(minHeap.extractMin());
		minHeap.decreaseKey(25, 100);
		System.out.println(minHeap.extractMin());
		minHeap.decreaseKey(26, 0);
		minHeap.decreaseKey(24, 100);
		minHeap.decreaseKey(23, 100);
		minHeap.decreaseKey(29, 100);
		minHeap.decreaseKey(28, 100);
		minHeap.decreaseKey(26, 20);
		System.out.println(minHeap.extractMin());
		System.out.println(minHeap.extractMin());
		System.out.println(minHeap.extractMin());
		System.out.println(minHeap.extractMin());
		System.out.println(minHeap.extractMin());
	}
	
	private static void testProjection() {
		GreatCircleDistanceFunction df = new GreatCircleDistanceFunction();
		Point res1 = df.getProjection(116.411, 39.97, 116.4, 39.98, 116.433, 39.95);
		Point res2 = df.getProjection2(116.411, 39.97, 116.4, 39.98, 116.433, 39.95);
		System.out.println(res1.toString());
		System.out.println(res2.toString());
	}
}
