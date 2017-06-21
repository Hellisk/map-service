package edu.uq.dke.mapupdate.mapmatching.io;

import org.graphstream.graph.Graph;
import org.graphstream.graph.implementations.SingleGraph;
import traminer.util.map.roadnetwork.RoadWay;
import traminer.util.spatial.objects.Point;
import traminer.util.trajectory.Trajectory;

/**
 * Created by uqpchao on 12/06/2017.
 */
public class GraphDisplayFunc {

    public static void displayTrajMatch(Trajectory traj, RoadWay matchedTraj) {
        if (traj.isEmpty()) return;

        Graph graph = new SingleGraph("Trajectory");
        graph.display(false);
        // create one node per trajectory point
        graph.addNode("N0");
        for (int i = 1; i < traj.size(); i++) {
            graph.addNode("N" + i);
            graph.addEdge("E" + (i - 1) + "-" + i, "N" + (i - 1), "N" + i);
        }
        for (int i = 0; i < traj.size(); i++) {
            Point p = traj.get(i);
            graph.getNode("N" + i).setAttribute("xyz", p.x(), p.y(), 0);
        }
        graph.addNode("A0");
        graph.addNode("A1");
        graph.addNode("A2");

        graph.addNode("NM0");
        for (int i = 1; i < matchedTraj.size(); i++) {
            graph.addNode("NM" + i);
            graph.addEdge("EM" + (i - 1) + "-" + i, "NM" + (i - 1), "NM" + i);
        }
        for (int i = 0; i < matchedTraj.size(); i++) {
            graph.getNode("NM" + i).setAttribute("xyz", matchedTraj.getNode(i).lon(), matchedTraj.getNode(i).lat(), 0);
        }

        graph.addNode("B0");
        graph.addNode("B1");
        graph.addNode("B2");
    }
}
