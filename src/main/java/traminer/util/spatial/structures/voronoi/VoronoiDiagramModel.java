package traminer.util.spatial.structures.voronoi;

import traminer.util.spatial.objects.*;
import traminer.util.spatial.structures.SpatialIndexModel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * A spatial index model representing a Voronoi Diagram. 
 * Contains the list of Voronoi Polygons boundaries that
 * compose the diagram.
 *  
 * @author uqdalves
 */
@SuppressWarnings("serial")
public class VoronoiDiagramModel implements SpatialIndexModel {
    /**
     * The list of Voronoi polygons in this diagram
     */
    private final List<VoronoiPolygon> polygonsList;
    /**
     * The list of generator pivot points
     */
    private List<Point> pivotsList = null; // avoid redundancy

    /**
     * Creates a new a Voronoi Diagram model for the given
     * generator pivot points.
     *
     * @param generatorPivots The list of generator pivot points.
     */
    public VoronoiDiagramModel(List<? extends Point> generatorPivots) {
        if (generatorPivots == null) {
            throw new NullPointerException("List of generator pivots for "
                    + "Voronoi diagram construction cannot be null.");
        }
        if (generatorPivots.isEmpty()) {
            throw new IllegalArgumentException("List of generator pivots for "
                    + "Voronoi diagram construction cannot be empty.");
        }
        // generate Voronoi polygon edges
        // Call Fortune's algorithm.
        this.polygonsList = new VoronoiDiagramBuilder()
                .buildVoronoiPolygons(generatorPivots);
    }

    /**
     * @return The list of Voronoi polygons in this diagram.
     */
    public List<VoronoiPolygon> getPolygons() {
        return polygonsList;
    }

    /**
     * @return The list of generator pivot points of this diagram.
     */
    public List<Point> getPivots() {
        if (pivotsList == null) {
            pivotsList = new ArrayList<Point>(polygonsList.size());
            for (VoronoiPolygon poly : polygonsList) {
                pivotsList.add(poly.getPivot());
            }
        }
        return pivotsList;
    }

    @Override
    public String search(double x, double y) {
        double dist, minDist = INFINITY;
        String id = null;
        // check the closest pivot from this point
        for (VoronoiPolygon poly : polygonsList) {
            dist = poly.getPivot().distance(x, y);
            if (dist < minDist) {
                minDist = dist;
                id = poly.getId();
            }
        }
        return id;
    }

    /**
     * Find all Voronoi polygon the intersect with this point
     * object (object may be a border point).
     *
     * @param p The point to search the polygon.
     * @return The Voronoi polygon the intersect with the given point.
     */
    public HashSet<String> rangeSearch(Point p) {
        if (p == null) {
            throw new NullPointerException(
                    "Point for range search cannot be null.");
        }
        HashSet<String> polySet = new HashSet<>();
        // check which polygons intersect with the object.
        double dist, minDist = INFINITY;
        // check the closest pivot from this point
        for (VoronoiPolygon poly : polygonsList) {
            dist = poly.getPivot().distance(p);
            if (dist < minDist) {
                minDist = dist;
                polySet = new HashSet<>();
                polySet.add(poly.getId());
            } else if (dist == minDist) {
                polySet.add(poly.getId());
            }
        }
        return polySet;
    }

    @Override
    public HashSet<String> rangeSearch(SpatialObject obj) {
        if (obj == null) {
            throw new NullPointerException(
                    "Spatial object for range search cannot be null.");
        }
        HashSet<String> polySet = new HashSet<>();
        // check which polygons intersect with the object.
        for (VoronoiPolygon poly : polygonsList) {
            if (poly.intersects(obj)) {
                polySet.add(poly.getId());
            }
        }
        return polySet;
    }

    /**
     * Search all Voronoi polygons that intersects with
     * the given  rectangular region.
     *
     * @param r The rectangular region to do the search.
     * @return A list of polygon IDs.
     */
    public HashSet<String> rangeSearch(Rectangle r) {
        if (r == null) {
            throw new NullPointerException("Rectangle region "
                    + "for range search cannot be null.");
        }
        HashSet<String> polySet = new HashSet<>();
        // check if the rectangle area intersects the polygon.
        for (VoronoiPolygon poly : polygonsList) {
            // check if the polygon's pivot is inside r.
            if (r.contains(poly.getPivot())) {
                polySet.add(poly.getId());
                continue;
            }
            // check if any of the polygon vertices are
            // inside the box, or if any edge intersect
            for (VoronoiEdge edge : poly.getVoronoiEdges()) {
                if (r.intersects(edge)) {
                    polySet.add(poly.getId());
                    break;
                }
            }
        }
        // check in which polygons the rectangle center is inside
        polySet.addAll(rangeSearch(r.center()));

        return polySet;
    }

    /**
     * Search all Voronoi polygons that intersects with
     * the given circular region.
     *
     * @param c The circular region to do the search.
     * @return A list of polygon IDs.
     */
    public HashSet<String> rangeSearch(Circle c) {
        if (c == null) {
            throw new NullPointerException("Circle region "
                    + "for range search cannot be null.");
        }
        HashSet<String> polySet = new HashSet<>();
        // check if the query area intersects the polygon.
        for (VoronoiPolygon poly : polygonsList) {
            // check if the polygon's pivot is inside the circle.
            if (c.contains(poly.getPivot())) {
                polySet.add(poly.getId());
                continue;
            }
            // check if any of the polygon edges intersects the circle
            for (VoronoiEdge edge : poly.getVoronoiEdges()) {
                if (c.intersects(edge)) {
                    polySet.add(poly.getId());
                    break;
                }
            }
        }
        // check in which polygon the circle center is inside of
        polySet.addAll(rangeSearch(c.center()));

        return polySet;
    }

    /**
     * Search all Voronoi polygons that intersects with
     * the given line string.
     *
     * @param ls The line string to do the search.
     * @return A list of polygon IDs.
     */
    public HashSet<String> rangeSearch(LineString ls) {
        if (ls == null) {
            throw new NullPointerException("Line-String "
                    + "for range search cannot be null.");
        }
        HashSet<String> polySet = new HashSet<String>();
        double dist, minDist;
        String id = "";
        for (Point p : ls.getCoordinates()) {
            minDist = INFINITY;
            for (VoronoiPolygon poly : polygonsList) {
                dist = p.distance(poly.getPivot());
                if (dist < minDist) {
                    minDist = dist;
                    id = poly.getId();
                }
            }
            polySet.add(id);
        }
        return polySet;
    }

    /**
     * Search for all polygons adjacent from the polygon
     * containing the object in the (x,y) position (if any).
     *
     * @param x Object's X coordinate.
     * @param y Object's Y coordinate.
     * @return A set of adjacent polygons IDs, or an empty set
     * if the object is out of the boundaries of this index model,
     * or if there is no adjacent polygons.
     */
    public HashSet<String> adjacentSearch(double x, double y) {
        HashSet<String> polySet = new HashSet<String>();
        // find the polygon containing the point
        String index = search(x, y);
        VoronoiPolygon poly = get(index);
        polySet.addAll(poly.getAdjacentList());

        return polySet;
    }

    /**
     * Search for all polygons adjacent from the polygon
     * with the given index (if any).
     *
     * @param index
     * @return A set of adjacent polygons IDs, or an empty set
     * if there is no adjacent polygons.
     */
    public HashSet<String> adjacentSearch(final String index) {
        if (index == null) {
            throw new NullPointerException(
                    "Voronoi Polygon index to search cannot be null.");
        }
        HashSet<String> polySet = new HashSet<String>();
        VoronoiPolygon poly = get(index);
        polySet.addAll(poly.getAdjacentList());

        return polySet;
    }

    @Override
    public VoronoiPolygon get(String index) {
        if (index == null) {
            throw new NullPointerException(
                    "Voronoi Polygon index to search cannot be null.");
        }
        for (VoronoiPolygon poly : polygonsList) {
            if (poly.getId().equals(index)) {
                return poly;
            }
        }
        return null;
    }

    @Override
    public SpatialObject getBoundary() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int size() {
        if (polygonsList == null) {
            return 0;
        }
        return polygonsList.size();
    }

    @Override
    public boolean isEmpty() {
        return (polygonsList == null || polygonsList.isEmpty());
    }

    @Override
    public void print() {
        System.out.println("VORONOI DIAGRAM " + toString());
    }

    @Override
    public String toString() {
        String s = "(";
        for (VoronoiPolygon poly : polygonsList) {
            s += " " + poly.toString();
        }
        s = s.replaceFirst(" ", "");
        return s + ")";
    }
}
