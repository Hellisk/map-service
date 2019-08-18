package algorithm.mapmatching.weightBased;

import com.github.davidmoten.grumpy.core.Position;
import com.github.davidmoten.rtree.Entry;
import com.github.davidmoten.rtree.RTree;
import com.github.davidmoten.rtree.geometry.Geometries;
import com.github.davidmoten.rtree.geometry.Line;
import com.github.davidmoten.rtree.geometry.Rectangle;
import rx.Observable;
import rx.functions.Func1;
import util.function.DistanceFunction;
import util.function.GreatCircleDistanceFunction;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * This class index road network using an R tree
 */
public class RoadNetworkIndexing {
    // load road nodes

    private static RTree<String, Line> rTree;
    private static String path = "/Users/macbookpro/Desktop/capstone/Beijing-S/input/map/edges_0.txt";

    /**
     * Initialize rTree
     *
     * @throws IOException file not found
     */
    public static void initialize() throws IOException {
        rTree = buildTree();
    }

    /**
     * Add polylines to rtree
     * One polyline contains multiple simple lines,
     * Each simple line is an geometric object in rtree,
     * with an unique id in the tree: polylineID + startNodeId + endNodeID
     *
     * @return RTree
     * @throws IOException file not found
     */
    private static RTree<String, Line> buildTree() throws IOException {
        RTree<String, Line> rTree = RTree.star().create();
        BufferedReader br = new BufferedReader(new FileReader(path));

        String str = br.readLine();
        while (str != null) {
            List<String> polyline = Arrays.asList(str.split("\\|"));
            String polylineID = polyline.get(0);
            int len = polyline.size();
            polyline = polyline.subList(7, len);

            len = polyline.size();
            for (int i = 0; i < len - 1; i++) {
                List<String> curTrip = Arrays.asList(polyline.get(i).split(","));
                double[] curCoord = new double[]{Double.parseDouble(curTrip.get(1)), Double.parseDouble(curTrip.get(2))};

                List<String> nextTrip = Arrays.asList(polyline.get(i + 1).split(","));
                double[] nextCoord = new double[]{Double.parseDouble(nextTrip.get(1)), Double.parseDouble(nextTrip.get(2))};

                String lineID = polylineID + "|" + curTrip.get(0) + "|" + nextTrip.get(0);
                rTree = rTree.add(lineID, Geometries.line(curCoord[0], curCoord[1], nextCoord[0], nextCoord[1]));
            }
            str = br.readLine();
        }
        return rTree;
    }

    /**
     * Get the road network rTree
     *
     * @return loaded rtree
     */
    public static RTree<String, Line> getTree() {
        return rTree;
    }

    /**
     * Point query the rtree
     *
     * @param lon       longitude of the query point
     * @param lat       latitude of the query point
     * @param distanceM search radius (m)
     * @return list of line objects that intersect the search box
     */
    public static List<Entry<String, Line>> search(double lon, double lat, final double distanceM) {
        return priSearch(lon, lat, distanceM).toList().toBlocking().single();
    }

    /**
     * Point query the rtree
     *
     * @param lon       longitude of the query point
     * @param lat       latitude of the query point
     * @param distanceM search radius (m)
     * @return Observations of lines
     */
    private static Observable<Entry<String, Line>> priSearch(double lon, double lat, final double distanceM) {
        // First we need to calculate an enclosing lat long rectangle for this
        // distance then we refine on the exact distance
        final Position from = Position.create(lat, lon);
        Rectangle bounds = createBounds(from, distanceM * 1.5 / 1000);

        return rTree
                // do the first search using the bounds (using L2 distance)
                .search(bounds)
                // refine using the exact distance
                .filter(new Func1<Entry<String, Line>, Boolean>() {
                    @Override
                    public Boolean call(Entry<String, Line> entry) {
                        Line line = entry.geometry();

                        DistanceFunction df = new GreatCircleDistanceFunction();
                        return df.pointToSegmentProjectionDistance(
                                lon, lat,
                                line.x1(), line.y1(), line.x2(), line.y2()) < distanceM;
                    }
                });
    }

    /**
     * create searching box around the query point
     *
     * @param from       Position of the query point
     * @param distanceKm length of the searching radius (km)
     * @return searching box
     */
    private static Rectangle createBounds(final Position from, final double distanceKm) {
        // this calculates a pretty accurate bounding box. Depending on the
        // performance you require you wouldn't have to be this accurate because
        // accuracy is enforced later
        Position north = from.predict(distanceKm, 0);
        Position east = from.predict(distanceKm, 90);
        Position south = from.predict(distanceKm, 180);
        Position west = from.predict(distanceKm, 270);

        return Geometries.rectangle(west.getLon(), south.getLat(), east.getLon(), north.getLat());
    }
}
