package edu.uq.dke.mapupdate.main;

import edu.uq.dke.mapupdate.io.CSVMapReader;
import edu.uq.dke.mapupdate.util.dijkstra.Graph;
import traminer.util.map.roadnetwork.RoadNetworkGraph;
import traminer.util.map.roadnetwork.RoadNode;

import java.io.IOException;

/**
 * Created by uqpchao on 4/07/2017.
 * Raw trajectory file format: longitude latitude time
 * Ground truth matched result format: longitude latitude time matchedLineNo
 */
public class test {
    public static void main(String[] arg) throws IOException {
        RoadNetworkGraph initialMap;
        CSVMapReader csvMapReader = new CSVMapReader("F:/data/trajectorydata/input/map/");
        initialMap = csvMapReader.readMap(0);
        Graph g = new Graph(initialMap);
        for (RoadNode n : initialMap.getNodes()) {
            for (RoadNode m : initialMap.getNodes()) {
                System.out.println("Distance is:" + g.calculateShortestDistances(n.lon(), n.lat(), m.lon(), m.lat()));
            }
        }
    }
}
