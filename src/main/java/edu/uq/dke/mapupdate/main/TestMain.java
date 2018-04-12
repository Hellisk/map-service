package edu.uq.dke.mapupdate.main;

import edu.uq.dke.mapupdate.util.dijkstra.MinPriorityQueue;
import edu.uq.dke.mapupdate.util.dijkstra.Node;

import java.io.IOException;
import java.util.Random;

/**
 * Created by uqpchao on 4/07/2017.
 * Raw trajectory file format: longitude latitude time
 * Ground truth matched result format: longitude latitude time matchedLineNum
 */
public class TestMain {
    public static void main(String[] arg) throws IOException {
        Node[] nodeList = new Node[100];
        Random random = new Random();
        for (int i = 0; i < 100; i++) {
            Node newNode = new Node();
            newNode.setDistanceFromSource(random.nextInt(1000));
            newNode.setIndex(i);
            nodeList[i] = newNode;
        }
        MinPriorityQueue minHeap = new MinPriorityQueue(nodeList[0]);
        for (int i = 0; i < 100; i++) {
            minHeap.decreaseKey(nodeList[i].getIndex(), nodeList[i].getDistanceFromSource());
        }
        for (int i = 0; i < 50; i++) {
            System.out.println(minHeap.extractMin());
        }
        for (int i = 50; i < 100; i++) {
            minHeap.decreaseKey(nodeList[i].getIndex(), random.nextInt(100));
        }
        for (int i = 0; i < 50; i++) {
            System.out.println(minHeap.extractMin());
        }
    }
}