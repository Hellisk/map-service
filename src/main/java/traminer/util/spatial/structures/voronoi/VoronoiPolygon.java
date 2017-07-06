package traminer.util.spatial.structures.voronoi;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.impl.PackedCoordinateSequence;
import org.graphstream.graph.Graph;
import org.graphstream.graph.implementations.SingleGraph;
import traminer.util.spatial.objects.*;
import traminer.util.spatial.objects.Polygon.Polygon2D;

import java.util.ArrayList;
import java.util.List;

/**
 * Implements a planar Voronoi polygon object. 
 * Each polygon is composed by a list of Voronoi edges,
 * the polygon generator pivot, and a list of adjacent 
 * polygons.
 * <p>
 * Note: May not be a closed polygon, i.e. border polygons.
 * 
 * @author uqdalves
 */
@SuppressWarnings("serial")
public class VoronoiPolygon extends ComplexSpatialObject<VoronoiEdge> {
    /**
     * The generator pivot point of this polygon
     */
    private final Point pivot;
    /**
     * The list of neighbor polygons. Adjacent polygons IDs
     */
    private List<String> adjacentList;

    // auxiliary Polygon from JTS lib
    private com.vividsolutions.jts.geom.
            Polygon JTSPolygon = null;

    /**
     * Creates a new empty Voronoi polygon.
     *
     * @param pivot The generator pivot point of this polygon
     */
    public VoronoiPolygon(Point pivot) {
        this.pivot = pivot;
        this.setId(pivot.getId());
    }

    /**
     * Creates a new empty Voronoi polygon.
     *
     * @param pivot   The generator pivot point of this polygon
     * @param pivotId The id of the pivot/polygon.
     */
    public VoronoiPolygon(Point pivot, String pivotId) {
        this.pivot = pivot;
        this.pivot.setId(pivotId);
        this.setId(pivot.getId());
    }

    /**
     * Creates a new empty Voronoi polygon.
     *
     * @param x       X coordinate of the generator pivot.
     * @param y       Y coordinate of the generator pivot.
     * @param pivotId The id of the pivot/polygon.
     */
    public VoronoiPolygon(double x, double y, String pivotId) {
        this.pivot = new Point(x, y);
        this.pivot.setId(pivotId);
        this.setId(pivot.getId());
    }

    /**
     * @return The generator pivot point of this polygon.
     */
    public Point getPivot() {
        return pivot;
    }

    /**
     * Add an edge to this polygon.
     * Check if the edge is not already in the polygon.
     *
     * @param edge The edge to add.
     * @return True if the edge could be add. False if
     * the polygon already contains the edge.
     */
    public boolean addEdge(VoronoiEdge edge) {
        if (!this.contains(edge)) {
            this.add(edge);
            return true;
        }
        return false;
    }

    /**
     * @return The list of the adjacent (neighbors) polygons IDs.
     */
    public List<String> getAdjacentList() {
        return adjacentList;
    }

    /**
     * Add a adjacent (neighbor) polygon to this polygon.
     * Check if the adjacent polygon is not yet add.
     *
     * @param adjacentId Adjacent polygon id.
     * @return True if the adjacent polygon id could be add.
     * False if the polygon already contains the adjacent id.
     */
    public boolean addAdjacent(String adjacentId) {
        if (adjacentList == null) {
            adjacentList = new ArrayList<String>(1);
        }
        if (!adjacentList.contains(adjacentId)) {
            adjacentList.add(adjacentId);
            return true;
        }
        return false;
    }

    @Override
    public List<Point> getCoordinates() {
        List<Point> vertexList = new ArrayList<Point>();
        for (VoronoiEdge e : this) {
            Point p1 = new Point(e.x1(), e.y1());
            Point p2 = new Point(e.x2(), e.y2());
            if (!vertexList.contains(p1)) {
                vertexList.add(p1);
            }
            if (!vertexList.contains(p2)) {
                vertexList.add(p2);
            }
        }
        return vertexList;
    }

    @Override
    public List<Segment> getEdges() {
        List<Segment> list = new ArrayList<Segment>();
        for (Segment s : this) {
            list.add(s);
        }
        return list;
    }

    /**
     * @return The list of Voronoi edges of this polygon.
     */
    public List<VoronoiEdge> getVoronoiEdges() {
        return this;
    }

    @Override
    public boolean isClosed() {
        List<Point> vertices = getCoordinatesClockwise();
        if (vertices == null || vertices.size() < 2) return false;
        return vertices.get(0).equals2D(vertices.get(size() - 1));
    }

    /**
     * @return Returns the Polygon object representation of this
     * Voronoi Polygon.
     */
    public Polygon toPolygon() {
        return new Polygon(this.getCoordinatesClockwise());
    }

    /**
     * @return The Polygon2D (AWT) representation of this
     * Voronoi Polygon.
     */
    public Polygon2D toPolygon2D() {
        return new Polygon2D(getCoordinates());
    }

    @Override
    public String toString() {
        String s = "( ";
        for (Point p : getCoordinatesClockwise()) {
            s += ", " + p.toString();
        }
        s.replaceFirst(", ", "");
        return s + " )";
    }

    @Override
    public void print() {
        System.out.println("VORONOI POLYGON " + toString());
    }

    @Override
    public void display() {
        if (this.isEmpty()) return;

        Graph graph = new SingleGraph("VoronoiPolygon");
        graph.display(false);
        // create one node per polygon vertex
        List<Point> coordList = this.getCoordinatesClockwise();
        Point p = coordList.get(0);
        int size = coordList.size();
        graph.addNode("N0").setAttribute("xyz", p.x(), p.y(), 0);
        for (int i = 1; i < size; i++) {
            p = coordList.get(i);
            graph.addNode("N" + i).setAttribute("xyz", p.x(), p.y(), 0);
            graph.addEdge("E" + (i - 1) + "-" + i, "N" + (i - 1), "N" + i);
        }
        // close the polygon
        graph.addEdge("E" + size + "-" + 0, "N" + (size - 1), "N0");
    }

    @Override
    public VoronoiPolygon clone() {
        VoronoiPolygon clone = new VoronoiPolygon(pivot.clone());
        for (VoronoiEdge e : this) {
            clone.add(e.clone());
        }
        super.cloneTo(clone);
        return clone;
    }

    @Override
    public boolean equals2D(SpatialObject obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (obj instanceof VoronoiPolygon) {
            VoronoiPolygon vp = (VoronoiPolygon) obj;
            if (this.size() != vp.size()) {
                return false;
            }
            for (int i = 0; i < size(); i++) {
                if (!vp.get(i).equals2D(this.get(i))) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public Geometry toJTSGeometry() {
        if (JTSPolygon == null) {
            Coordinate[] coords = new Coordinate[size() + 1];
            int i = 0;
            for (Point p : getCoordinatesClockwise()) {
                coords[i++] = new Coordinate(p.x(), p.y());
            }
            coords[size()] = coords[0];

            LinearRing shell = new LinearRing(new PackedCoordinateSequence.
                    Double(coords), new GeometryFactory());

            JTSPolygon = new com.vividsolutions.jts.geom.
                    Polygon(shell, null, new GeometryFactory());
        }
        return JTSPolygon;
    }
}
