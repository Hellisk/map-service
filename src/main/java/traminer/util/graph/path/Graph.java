package traminer.util.graph.path;

import java.util.*;

public class Graph {

    private final Map<Integer, List<Vertex>> vertices;
    private final Map<Integer, Vertex> vertexInfo;

    public Graph() {
        this.vertices = new HashMap<Integer, List<Vertex>>();
        this.vertexInfo = new HashMap<Integer, Vertex>();
    }

    public void addVertex(int id, List<Vertex> vertex) {
        this.vertices.put(id, vertex);
    }

    public void addAdjacency(int id, Vertex vertex) {
        List<Vertex> adjacentList = new ArrayList<>();
        if (this.vertices.containsKey(id)) {
            adjacentList = this.vertices.get(id);
            adjacentList.add(vertex);
            this.vertices.remove(id);
            this.vertices.put(id, adjacentList);
        } else {
            adjacentList.add(vertex);
            this.vertices.put(id, adjacentList);
        }
    }

    public void addVertexInfo(int id, Vertex vertex) {
        this.vertexInfo.put(id, vertex);
    }

    public Vertex getVertexInfo(int id) {
        return vertexInfo.get(id);
    }

    public List<Integer> getShortestPath(int start, int finish) {
        final Map<Integer, Double> distances = new HashMap<Integer, Double>();
        final Map<Integer, Vertex> previous = new HashMap<Integer, Vertex>();
        PriorityQueue<Vertex> nodes = new PriorityQueue<Vertex>();

        for (int vertex : vertices.keySet()) {
            if (vertex == start) {
                distances.put(vertex, 0d);
                nodes.add(new Vertex(vertex, 0));
            } else {
                distances.put(vertex, Double.MAX_VALUE);
                nodes.add(new Vertex(vertex, Double.MAX_VALUE));
            }
            previous.put(vertex, null);
        }

        while (!nodes.isEmpty()) {
            Vertex smallest = nodes.poll();
            if (smallest.getId() == finish) {
                final List<Integer> path = new ArrayList<Integer>();
                while (previous.get(smallest.getId()) != null) {
                    path.add(smallest.getId());
                    smallest = previous.get(smallest.getId());
                }
                return path;
            }

            if (distances.get(smallest.getId()) == Integer.MAX_VALUE) {
                break;
            }

            for (Vertex neighbor : vertices.get(smallest.getId())) {
                double alt = distances.get(smallest.getId()) + neighbor.getDistance();
                if (alt < distances.get(neighbor.getId())) {
                    distances.put(neighbor.getId(), alt);
                    previous.put(neighbor.getId(), smallest);

                    for (Vertex n : nodes) {
                        if (n.getId() == neighbor.getId()) {
                            nodes.remove(n);
                            n.setDistance(alt);
                            nodes.add(n);
                            break;
                        }
                    }
                }
            }
        }

        return new ArrayList<Integer>(distances.keySet());
    }
}
