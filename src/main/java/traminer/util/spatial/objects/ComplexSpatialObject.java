package traminer.util.spatial.objects;

import com.vividsolutions.jts.geom.Coordinate;
import traminer.util.Attributes;
import traminer.util.spatial.ClockwiseComparator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Base superclass for complex spatial objects.
 * <p>
 * Complex spatial objects are made of a list of
 * simple spatial objects.
 *
 * @author uqdalves
 */
@SuppressWarnings("serial")
public abstract class ComplexSpatialObject<T extends SimpleSpatialObject>
        extends ArrayList<T> implements SpatialObject {
    /**
     * Constructs an empty complex spatial object
     * with the initial capacity equals 1.
     */
    public ComplexSpatialObject() {
        super(1);
    }

    /**
     * Constructs an empty complex spatial object
     * with the specified initial capacity.
     */
    public ComplexSpatialObject(int initialCapacity) {
        super(initialCapacity);
    }

    /**
     * Semantic attributes of this spatial object.
     */
    private Attributes attributes = null;
    /**
     * Spatial object identifier.
     */
    private String oid = null;
    /**
     * The identifier of the parent of this spatial object (if any).
     */
    private String parentOid = null;
    /**
     * Number of spatial dimension of this object (2 by default).
     */
    private byte dimension = 2;

    @Override
    public void setId(String id) {
        this.oid = id;
    }

    @Override
    public String getId() {
        return oid;
    }

    @Override
    public String getParentId() {
        return parentOid;
    }

    @Override
    public void setParentId(String parentId) {
        this.parentOid = parentId;
    }

    @Override
    public void setDimension(byte dim) {
        if (dim < 1) {
            throw new IllegalArgumentException("Number of spatial "
                    + "dimentions must be positive [1..127].");
        }
        this.dimension = dim;
    }

    @Override
    public byte getDimension() {
        return dimension;
    }

    @Override
    public Attributes getAttributes() {
        return attributes;
    }

    @Override
    public void setAttributes(Attributes attr) {
        this.attributes = attr;
    }

    @Override
    public void putAttribute(String attrName, Object attrValue) {
        if (attributes == null) {
            attributes = new Attributes();
        }
        attributes.put(attrName, attrValue);
    }

    @Override
    public Object getAttribute(String attrName) {
        return attributes.getAttributeValue(attrName);
    }

    @Override
    public List<String> getAttributeNames() {
        List<String> namesList = new ArrayList<String>();
        for (Object key : attributes.keySet()) {
            namesList.add(key.toString());
        }
        return namesList;
    }

    @Override
    public abstract ComplexSpatialObject<T> clone();

    /**
     * True if these two spatial objects are adjacent.
     * (i.e. If they share any edge).
     */
    public boolean isAdjacent(ComplexSpatialObject<SimpleSpatialObject> obj) {
        for (Edges e1 : this.getEdges()) {
            for (Edges e2 : obj.getEdges()) {
                if (e1.equals2D(e2)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * The list of points/vertexes of this complex spatial object
     * in Clockwise order.
     */
    public List<Point> getCoordinatesClockwise() {
        List<Point> vertexList = getCoordinates();

        // sort in clockwise order
        ClockwiseComparator<Point> comparator =
                new ClockwiseComparator<Point>(this.centroid());
        Collections.sort(vertexList, comparator);

        return vertexList;
    }

    /**
     * The list of edges/segments of this complex spatial object
     * in Clockwise order.
     */
    public List<Edges> getEdgesClockwise() {
        List<Edges> edgesClockwise =
                new ArrayList<Edges>();
        List<Point> vertexes = getCoordinatesClockwise();
        double x1;
        double y1;
        double x2;
        double y2;
        Point centroid = this.centroid();
        if (vertexes.size() == 2) {
            x1 = vertexes.get(0).x();
            y1 = vertexes.get(0).y();
            x2 = vertexes.get(1).x();
            y2 = vertexes.get(1).y();
            if ((x1 - centroid.x()) >= 0 && (x2 - centroid.x()) < 0 && (y2 - centroid.y()) > 0) {
                double cross = (x2 - x1) * (centroid.y() - y1) - (y2 - y1) * (centroid.x() - x1);
                if (cross > 0.0) {
                    Edges edge = new Edges(x2, y2, x1, y1);
                    edgesClockwise.add(edge);
                    return edgesClockwise;
                }
            }
        }
        for (int i = 0; i < vertexes.size() - 1; i++) {
            x1 = vertexes.get(i).x();
            y1 = vertexes.get(i).y();
            x2 = vertexes.get(i + 1).x();
            y2 = vertexes.get(i + 1).y();
            Edges edge = new Edges(x1, y1, x2, y2);
            if (this.contains(edge)) {
                edgesClockwise.add(edge);
            }
        }
        if (vertexes.size() > 2) { // only if there are more than 2 vertexes
            // the edge connection the last to the first vertex
            x1 = vertexes.get(vertexes.size() - 1).x();
            y1 = vertexes.get(vertexes.size() - 1).y();
            x2 = vertexes.get(0).x();
            y2 = vertexes.get(0).y();
            Edges edge = new Edges(x1, y1, x2, y2);
            if (this.contains(edge)) {
                edgesClockwise.add(edge);
            }
        }
        return edgesClockwise;
    }

    /**
     * The centroid of this complex spatial object.
     */
    public Point centroid() {
        double x = 0, y = 0;
        for (Point p : this.getCoordinates()) {
            x += p.x();
            y += p.y();
        }
        x = x / (size() - 1);
        y = y / (size() - 1);

        return new Point(x, y);
    }

    /**
     * The Minimum Bounding Rectangle (MBR)
     * of this Spatial Object.
     */
    public Rectangle mbr() {
        if (!this.isEmpty()) {
            double minX = INFINITY, maxX = -INFINITY;
            double minY = INFINITY, maxY = -INFINITY;
            for (Point p : this.getCoordinates()) {
                if (p.x() > maxX) maxX = p.x();
                if (p.x() < minX) minX = p.x();
                if (p.y() > maxY) maxY = p.y();
                if (p.y() < minY) minY = p.y();
            }
            return new Rectangle(minX, minY, maxX, maxY);
        }
        return new Rectangle(0.0, 0.0, 0.0, 0.0);
    }

    /**
     * Computes the smallest convex Polygon that contains
     * all the points in the Spatial Object.
     */
    public Polygon convexHull() {
        Coordinate[] coordList = this.toJTSGeometry().
                convexHull().getCoordinates();
        Polygon convexHull = new Polygon();
        for (Coordinate c : coordList) {
            convexHull.add(c.x, c.y);
        }

        return convexHull;
    }

    /**
     * Computes the convex hull of this spatial object
     * represented as a list of coordinate points.
     */
    public List<Point> convexHullCoordinates() {
        Coordinate[] coordList = this.toJTSGeometry().
                convexHull().getCoordinates();
        List<Point> convexHull = new ArrayList<Point>();
        for (Coordinate c : coordList) {
            convexHull.add(new Point(c.x, c.y));
        }

        return convexHull;
    }
}
