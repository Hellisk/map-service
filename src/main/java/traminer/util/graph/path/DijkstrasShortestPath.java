package traminer.util.graph.path;

/**
 * Implementation of Dijkstras shortest path algorithm, found in
 * https://github.com/mburst/dijkstras-algorithm/blob/master/Dijkstras.java
 */

import java.util.Arrays;

public class DijkstrasShortestPath {

    public static void main(String[] args) {
        Graph g = new Graph();
        g.addVertex('A', Arrays.asList(new Vertex('B', 7), new Vertex('C', 8)));
        g.addVertex('B', Arrays.asList(new Vertex('A', 7), new Vertex('F', 2)));
        g.addVertex('C', Arrays.asList(new Vertex('A', 8), new Vertex('F', 6), new Vertex('G', 4)));
        g.addVertex('D', Arrays.asList(new Vertex('F', 8)));
        g.addVertex('E', Arrays.asList(new Vertex('H', 1)));
        g.addVertex('F', Arrays.asList(new Vertex('B', 2), new Vertex('C', 6), new Vertex('D', 8), new Vertex('G', 9), new Vertex('H', 3)));
        g.addVertex('G', Arrays.asList(new Vertex('C', 4), new Vertex('F', 9)));
        g.addVertex('H', Arrays.asList(new Vertex('E', 1), new Vertex('F', 3)));
        System.out.println(g.getShortestPath('A', 'H'));
    }

}

