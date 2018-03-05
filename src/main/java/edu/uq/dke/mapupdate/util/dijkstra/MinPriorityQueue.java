package edu.uq.dke.mapupdate.util.dijkstra;

public class MinPriorityQueue {

    private Node[] array;
    private int heapSize;

    public MinPriorityQueue(Node[] array) {
        this.array = array;
        this.heapSize = this.array.length;
    }

    public Node extractMin() {
        Node temp = array[0];
        swap(0, heapSize - 1, array);
        heapSize--;
        sink(0);
        return temp;
    }

    public boolean isEmpty() {
        return heapSize == 0;
    }

    public void buildMinHeap() {
        for (int i = heapSize / 2 - 1; i >= 0; i--) {
            sink(i);
        }
    }

    public void decreaseKey(int index, Node key) {
        if (key.compareTo(array[index]) >= 0) {
            throw new IllegalArgumentException("the new key must be greater than the current key");
        }
        array[index] = key;
        while (index > 0 && array[index].compareTo(array[parentIndex(index)]) < 0) {
            swap(index, parentIndex(index), array);
            index = parentIndex(index);
        }
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
    }

}
