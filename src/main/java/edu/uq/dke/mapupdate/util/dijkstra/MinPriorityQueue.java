package edu.uq.dke.mapupdate.util.dijkstra;

import java.util.HashMap;
import java.util.Map;

public class MinPriorityQueue {

    private Node[] array;
    private int heapSize;
    private Map<Integer, Integer> actualIndex2ArrayIndex;

    MinPriorityQueue(Node[] array) {
        this.array = array;
        this.heapSize = this.array.length;
        this.actualIndex2ArrayIndex = new HashMap<>();
    }

    Node extractMin() {
        Node temp = array[0];
        swap(0, heapSize - 1, array);
        heapSize--;
        sink(0);
        return temp;
    }

    public boolean isEmpty() {
        return heapSize == 0;
    }

    void buildMinHeap() {
        for (int i = heapSize / 2 - 1; i >= 0; i--) {
            sink(i);
        }
        for (int i = 0; i < heapSize; i++)
            actualIndex2ArrayIndex.put(array[i].getIndex(), i);
    }

    public void decreaseKey(int index, double distance) {
        int arrayIndex = actualIndex2ArrayIndex.get(index);
        if (distance >= array[arrayIndex].getDistanceFromSource()) {
            throw new IllegalArgumentException("the new key must be greater than the current key");
        }
        array[arrayIndex].setDistanceFromSource(distance);
        while (arrayIndex > 0 && array[arrayIndex].compareTo(array[parentIndex(arrayIndex)]) < 0) {
            swap(arrayIndex, parentIndex(arrayIndex), array);
            arrayIndex = parentIndex(arrayIndex);
        }
    }

    public Node getNode(int index) {
        return array[actualIndex2ArrayIndex.get(index)];
    }

    private int parentIndex(int index) {
        return (index - 1) / 2;
    }

    private int left(int index) {
        return 2 * index + 1;
    }

    private int right(int index) {
        return 2 * index + 2;
    }

    private void sink(int index) {
        int smallestIndex = index;
        int left = left(index);
        int right = right(index);
        if (left < heapSize && array[left].compareTo(array[smallestIndex]) < 0) {
            smallestIndex = left;
        }
        if (right < heapSize && array[right].compareTo(array[smallestIndex]) < 0) {
            smallestIndex = right;
        }
        if (index != smallestIndex) {
            swap(smallestIndex, index, array);
            sink(smallestIndex);
        }
    }

    public Node min() {
        return array[0];
    }

    private void swap(int i, int j, Node[] array) {
        Node temp = array[i];
        array[i] = array[j];
        array[j] = temp;
        actualIndex2ArrayIndex.replace(array[i].getIndex(), i);
        actualIndex2ArrayIndex.replace(array[j].getIndex(), j);
    }

}
