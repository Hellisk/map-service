package traminer.util.graph;

import org.graphstream.graph.Graph;
import org.graphstream.graph.implementations.SingleGraph;

/**
 * GraphStream library demo.
 * <p>
 * </br> http://graphstream-project.org/
 *
 * @author uqdalves
 */
@SuppressWarnings("serial")
public class GraphStreamDemo implements GraphInterface {

    /**
     * GraphStream is a graph handling Java library that focuses on
     * the dynamics aspects of graphs. Its main focus is on the modeling
     * of dynamic interaction networks of various sizes.
     */
    public static void main(String args[]) {
        Graph graph = new SingleGraph("Example 1");

        graph.addNode("A");
        graph.addNode("B");
        graph.addNode("C");
        graph.addEdge("AB", "A", "B");
        graph.addEdge("BC", "B", "C");
        graph.addEdge("CA", "C", "A");

        graph.display();
    }
}
