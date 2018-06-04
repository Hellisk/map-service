package edu.uq.dke.mapupdate.test;

import edu.uq.dke.mapupdate.util.object.datastructure.ItemWithProbability;
import edu.uq.dke.mapupdate.util.object.datastructure.Pair;
import edu.uq.dke.mapupdate.util.object.spatialobject.Point;
import org.apache.log4j.BasicConfigurator;

import java.util.PriorityQueue;
import java.util.Queue;

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
        topRankedCandidates.add(new ItemWithProbability<>("Test10", 1.1));
        topRankedCandidates.add(new ItemWithProbability<>("Test9", 1.1));
        topRankedCandidates.add(new ItemWithProbability<>("Test8", 1.1));
        System.out.println(topRankedCandidates.remove().getItem());
        System.out.println(topRankedCandidates.remove().getItem());
        System.out.println(topRankedCandidates.remove().getItem());
        System.out.println(topRankedCandidates.remove().getItem());
        System.out.println(topRankedCandidates.remove().getItem());
        System.out.println(topRankedCandidates.remove().getItem());
        System.out.println(topRankedCandidates.remove().getItem());
        System.out.println(topRankedCandidates.remove().getItem());
        System.out.println(topRankedCandidates.remove().getItem());
        System.out.println(topRankedCandidates.remove().getItem());
    }
}
