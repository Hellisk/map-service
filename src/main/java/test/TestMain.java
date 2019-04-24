package test;

import util.dijkstra.MinPriorityQueue;
import util.object.structure.InverselyComparableObject;

import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Random;

public class TestMain {
    public static void main(String[] args) {
        minPriorityQueueTest();
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
}
