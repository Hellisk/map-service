package traminer.util.map.matching.hmm;

import com.graphhopper.GraphHopper;
import com.graphhopper.matching.EdgeMatch;
import com.graphhopper.matching.GPXExtension;
import com.graphhopper.matching.MapMatching;
import com.graphhopper.matching.MatchResult;
import com.graphhopper.routing.AlgorithmOptions;
import com.graphhopper.routing.util.CarFlagEncoder;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.util.GPXEntry;
import traminer.util.map.roadnetwork.RoadNode;
import traminer.util.map.roadnetwork.RoadWay;
import traminer.util.spatial.SpatialInterface;
import traminer.util.spatial.objects.st.STPoint;
import traminer.util.trajectory.Trajectory;

import java.util.ArrayList;
import java.util.List;

/**
 * Off-line map matching algorithm, using the Graphhopper library.
 * <p>
 * The map matching algorithm mainly follows the approach described in:
 * <br> "Newson, Paul; Krumm, John. Hidden Markov map matching through 
 * noise and sparseness. In: Proceedings of the 17th ACM SIGSPATIAL, 2009."
 * 
 * @author uqdalves
 */
@SuppressWarnings("serial")
public class GraphHopperMatching implements SpatialInterface {
    private MapMatching mapMatching;

    /**
     * Creates a new map-matching method with default
     * Haversine distance function.
     */
    public GraphHopperMatching(
            final String osmFilePath, final String graphHopperLocation) {
        // create hopper instance with CH enabled
        CarFlagEncoder encoder = new CarFlagEncoder();
        GraphHopper hopper = new GraphHopper();
        hopper.setOSMFile(osmFilePath);
        hopper.setGraphHopperLocation(graphHopperLocation);//"../target/mapmatchingtest-ch");
        hopper.setEncodingManager(new EncodingManager(encoder));
        hopper.importOrLoad();

        AlgorithmOptions opts = AlgorithmOptions.start().build();
        mapMatching = new MapMatching(hopper, opts);
    }

    public RoadWay doMatching(Trajectory trajectory) {
        MatchResult mr = runGraphHopperMatching(trajectory);
        RoadWay way = new RoadWay(trajectory.getId());
        for (EdgeMatch edge : mr.getEdgeMatches()) {
            for (GPXExtension gpx : edge.getGpxExtensions()) {
                GPXEntry node = gpx.getEntry();
                way.addNode(new RoadNode("" + node.ele, node.lon, node.lat, "" + node.getTime()));
            }
        }
        return way;
    }

    public List<RoadNode> doMatching(List<STPoint> pointsList) {
        MatchResult mr = runGraphHopperMatching(pointsList);
        List<RoadNode> result = new ArrayList<>();
        for (EdgeMatch edge : mr.getEdgeMatches()) {
            for (GPXExtension gpx : edge.getGpxExtensions()) {
                GPXEntry node = gpx.getEntry();
                result.add(new RoadNode("" + node.ele, node.lon, node.lat, "" + node.getTime()));
            }
        }
        return result;
    }

    private MatchResult runGraphHopperMatching(List<STPoint> pointsList) {
        List<GPXEntry> inputGPXEntries = getGPXEntries(pointsList);
        MatchResult mr = mapMatching.doWork(inputGPXEntries);

        return mr;
    }

    /**
     * Parse a list of STPoints to GPXPoints.
     */
    private List<GPXEntry> getGPXEntries(List<STPoint> pointsList) {
        List<GPXEntry> result = new ArrayList<>();
        for (STPoint p : pointsList) {
            result.add(new GPXEntry(p.y(), p.x(), p.time()));
        }
        return result;
    }
}
