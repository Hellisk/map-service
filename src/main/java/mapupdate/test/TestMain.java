package mapupdate.test;

import mapupdate.util.object.datastructure.ItemWithProbability;

import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Random;

public class TestMain {
    public static void main(String[] args) {
        Queue<ItemWithProbability<String>> topRankedCandidates = new PriorityQueue<>();
        topRankedCandidates.add(new ItemWithProbability<>("Text1", 100.1));
        topRankedCandidates.add(new ItemWithProbability<>("Test2", 110.1));
        topRankedCandidates.add(new ItemWithProbability<>("Test3", 200.1));
        topRankedCandidates.add(new ItemWithProbability<>("Test4", 10.1));
        topRankedCandidates.add(new ItemWithProbability<>("Test5", 100.12));
        topRankedCandidates.add(new ItemWithProbability<>("Test6", 15.1));
        topRankedCandidates.add(new ItemWithProbability<>("Test7", 1.1));
        topRankedCandidates.add(new ItemWithProbability<>("Test8", 1.1));
        topRankedCandidates.add(new ItemWithProbability<>("Test9", 1.1));
        topRankedCandidates.add(new ItemWithProbability<>("Test10", 1.1));
        Random randomValue = new Random();
        for (int count = 11; count < 100; count++) {
            topRankedCandidates.add(new ItemWithProbability<>("Test" + count, randomValue.nextDouble()));
        }
        while (!topRankedCandidates.isEmpty())
            System.out.println(topRankedCandidates.remove().getItem());
    }
}
