package traminer.util.spatial.objects;

import com.vividsolutions.jts.geom.Geometry;
import traminer.util.Attributes;
import traminer.util.exceptions.SpatialObjectException;
import traminer.util.exceptions.SpatialRelationException;
import traminer.util.spatial.SpatialInterface;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Base interface for all spatial objects
 * (i.e. simple, complex, and collections).
 *
 * @author uqdalves
 */
public interface SpatialObject extends SpatialInterface, Cloneable {
    /* **************
     * Set and Get functions for attributes common to all spatial objects
	 * **************/

    /**
     * Set the identifier of this spatial object.
     */
    void setId(String id);

    /**
     * The identifier of this spatial object.
     */
    String getId();

    /**
     * Set the identifier of the parent of this spatial object (if any).
     */
    void setParentId(String parentId);

    /**
     * The identifier of the parent of this  spatial object (if any).
     */
    String getParentId();

    /**
     * Set the number of spatial dimensions
     * of this spatial object [1..127].
     */
    void setDimension(byte dim);

    /**
     * The number of spatial dimensions
     * of this spatial object [1..127].
     */
    byte getDimension();

    /**
     * The semantic attributes in this spatial object.
     */
    Attributes getAttributes();

    /**
     * Set the semantic attributes in this spatial object.
     */
    void setAttributes(Attributes attr);

    /**
     * Associates the specified value with the specified attribute name,
     * specified as a String. The attributes name is case-insensitive.
     * If the Map previously contained a mapping for the attribute name,
     * the old value is replaced.
     */
    void putAttribute(String attrName, Object attrValue);

    /**
     * Returns the value of the specified attribute name, specified
     * as a string, or null if the attribute was not found.
     */
    Object getAttribute(String attrName);

    /**
     * The list of attribute names in this spatial object.
     */
    List<String> getAttributeNames();
	
	/* ************** 
	 * Declaration of methods common to all spatial objects.
	 * **************/

    /**
     * The list of points/vertexes of this spatial object.
     */
    List<Point> getCoordinates();

    /**
     * The list of edges/segments of this spatial object (if any).
     */
    List<Edges> getEdges();

    /**
     * Return the Minimun Bounding Rectangle (MBR) of
     * this spatial object.
     *
     * @throws SpatialObjectException if object is empty or null.
     */
    default Rectangle mbr() {
        List<Point> coordList = getCoordinates();
        if (coordList == null || coordList.isEmpty()) {
            throw new SpatialObjectException(
                    "Cannot calculate MBR. Object is empty.");
        }

        int size = coordList.size();
        Collections.sort(coordList, Point.X_COMPARATOR);
        double minx = coordList.get(0).x();
        double maxx = coordList.get(size - 1).x();
        Collections.sort(coordList, Point.Y_COMPARATOR);
        double miny = coordList.get(0).y();
        double maxy = coordList.get(size - 1).y();

        return new Rectangle(minx, miny, maxx, maxy);
    }

    /**
     * Check is this spatial object is a closed object, that is,
     * return true if the object is composed by edges (or a circle)
     * and its first and final edge points are the same.
     */
    boolean isClosed();

    /**
     * Makes an exact copy of this spatial object.
     */
    SpatialObject clone();

    /**
     * Clone the semantic attributes (super-class attributes) of this spatial
     * object into the given spatial object (auxiliary clone method).
     *
     * @param obj The spatial object to receive the cloned attributes.
     */
    default void cloneTo(SpatialObject obj) {
        if (this.getAttributes() != null) {
            obj.setAttributes(new Attributes(this.getAttributes()));
        }
        if (this.getId() != null) {
            obj.setId(this.getId());
        }
        obj.setDimension(this.getDimension());
    }

    /**
     * Print this spatial object to system console.
     */
    void print();

    /***
     * Displays this spatial object in a GUI window.
     */
    void display();

    /**
     * Check whether these two spatial objects are of the same kind,
     * and whether they have the same 2D spatial coordinates.
     * <p>
     * For ComplexSpatialObjects this method verifies if every
     * coordinate of the two objects are the same in the exact order
     * they are declared in the object.
     */
    boolean equals2D(SpatialObject obj);

    /**
     * String representation of this spatial object.
     */
    @Override
    String toString();

    /**
     * Compare this spatial object using the given comparator.
     *
     * @param obj        The spatial object to compare.
     * @param comparator The comparator to use.
     * @return The result of this method is the as same as
     * comparator.compare(this, obj).
     */
    default int compareTo(SpatialObject obj, Comparator<SpatialObject> comparator) {
        return comparator.compare(this, obj);
    }

    /**
     * Returns the JTS equivalent Geometry object of
     * this Spatial Object.
     */
    Geometry toJTSGeometry();
    
	/* ************** 
	 * Topological operations (spatial relations)
	 * **************/

    /**
     * Returns TRUE if the given spatial object 'obj' is completely contained
     * by this spatial object.
     * <p>
     * The contains predicate returns the exact opposite result of the Within predicate.
     * <br> The boundary and interior of the second geometry are not allowed to intersect
     * the exterior of the first geometry and the geometries may not be equal.
     * <br> The interiors of both geometries must intersect and that the interior and
     * boundary of the secondary (geometry b) must not intersect the exterior of
     * the primary (geometry a).
     */
    default boolean contains(SpatialObject obj) throws SpatialRelationException {
        try {
            if (obj instanceof Circle) {
                return ((Circle) obj).within(this);
            }
            return this.toJTSGeometry().contains(obj.toJTSGeometry());
        } catch (Exception e) {
            throw new SpatialRelationException("'Contains' operation not supported "
                    + "for these spatial objects.");
        }
    }

    /**
     * Returns TRUE if this spatial object is completely within the
     * given spatial object 'obj'.
     * <p>
     * Within tests for the exact opposite result of Contains.
     * <br> The boundary and interior of the first geometry are not allowed
     * to intersect the exterior of the second geometry and the first geometry
     * may not equal the second geometry.
     * <br> The interiors of both geometries must intersect and that the interior
     * and boundary of the primary geometry (geometry a) must not intersect the
     * exterior of the secondary (geometry b).
     */
    default boolean within(SpatialObject obj) throws SpatialRelationException {
        try {
            return this.toJTSGeometry().within(obj.toJTSGeometry());
        } catch (Exception e) {
            throw new SpatialRelationException("'Within' operation not supported "
                    + "for these spatial objects.");
        }
    }

    /**
     * Returns TRUE is this spatial object lies in this spatial object 'obj' .
     * <p>
     * ''a'' is covered by ''b'' (extends Within) if every point of ''a'' is a point of ''b'',
     * and the interiors of the two geometries have at least one point in common.
     */
    default boolean coveredBy(SpatialObject obj) throws SpatialRelationException {
        try {
            return this.toJTSGeometry().coveredBy(obj.toJTSGeometry());
        } catch (Exception e) {
            throw new SpatialRelationException("'CoveredBy' operation not supported "
                    + "for these spatial objects.");
        }
    }

    /**
     * Returns TRUE is the given spatial object 'obj' lies in this spatial object.
     * <p>
     * ''a'' covers ''b'' if no points of ''b'' lie in the exterior of ''a'', or every
     * point of ''b'' is a point of (the interior or boundary of) ''a''.
     */
    default boolean covers(SpatialObject obj) throws SpatialRelationException {
        try {
            if (obj instanceof Circle) {
                return this.contains(obj);
            }
            return this.toJTSGeometry().covers(obj.toJTSGeometry());
        } catch (Exception e) {
            throw new SpatialRelationException("'Covers' operation not supported "
                    + "for these spatial objects.");
        }
    }

    /**
     * Returns TRUE if these two spatial objects cross.
     * <p>
     * Returns TRUE if the intersection results in a geometry whose dimension is one
     * less than the maximum dimension of the two source geometries, and the intersection
     * set is interior to both source geometries.
     * <br> The interiors must intersect and at least the interior of the primary (geometry a)
     * must intersect the exterior of the secondary (geometry b).
     * <br> The dimension of the intersection of the interiors must be 0 (intersect at a point).
     */
    default boolean crosses(SpatialObject obj) throws SpatialRelationException {
        try {
            if (obj instanceof Circle) {
                return ((Circle) obj).overlaps(this);
            }
            return this.toJTSGeometry().crosses(obj.toJTSGeometry());
        } catch (Exception e) {
            throw new SpatialRelationException("'Crosses' operation not supported "
                    + "for these spatial objects.");
        }
    }

    /**
     * Returns TRUE if the intersection of these two spatial objects is an empty set.
     * <p>
     * Geometries are disjoint if they do not intersect one another in any way.
     * <br> Disjoint returns the exact opposite result of Intersects.
     */
    default boolean disjoint(SpatialObject obj) throws SpatialRelationException {
        try {
            if (obj instanceof Circle) {
                return !((Circle) obj).intersects(this);
            }
            return this.toJTSGeometry().disjoint(obj.toJTSGeometry());
        } catch (Exception e) {
            throw new SpatialRelationException("'Disjoint' operation not supported "
                    + "for these spatial objects.");
        }
    }

    /**
     * Returns TRUE if the intersection of these tow spatial objects does not
     * result in an empty set.
     * <p>
     * Intersects returns the exact opposite result of Disjoint.
     * <br> The intersects predicate returns TRUE if the interiors of both geometries intersect.
     * <br> The intersects predicate returns TRUE if the boundary of the first geometry intersects
     * the boundary of the second geometry.
     * <br> The intersects predicate returns TRUE if the boundary of the first geometry intersects
     * the interior of the second.
     * <br> The intersects predicate returns TRUE if the boundaries of either geometry intersect.
     */
    default boolean intersects(SpatialObject obj) throws SpatialRelationException {
        try {
            if (obj instanceof Circle) {
                return ((Circle) obj).intersects(this);
            }
            return this.toJTSGeometry().intersects(obj.toJTSGeometry());
        } catch (Exception e) {
            throw new SpatialRelationException("'Intersects' operation not supported "
                    + "for these spatial objects.");
        }
    }

    /**
     * Returns TRUE if these two spatial objects overlap.
     * <p>
     * Compares two geometries of the same dimension and returns TRUE if their intersection set
     * results in a geometry different from both, but of the same dimension.
     * <br> Overlap returns TRUE only for geometries of the same dimension, and only when their
     * intersection set results in a geometry of the same dimension. In other words, if the
     * intersection of two polygons results in polygon, then overlap returns TRUE.
     * <br> The overlap predicate returns TRUE if the interior of both geometries intersects
     * the others interior and exterior.
     */
    default boolean overlaps(SpatialObject obj) throws SpatialRelationException {
        try {
            if (obj instanceof Circle) {
                return ((Circle) obj).overlaps(this);
            }
            return this.toJTSGeometry().overlaps(obj.toJTSGeometry());
        } catch (Exception e) {
            throw new SpatialRelationException("'Overlaps' operation not supported "
                    + "for these spatial objects.");
        }
    }

// TODO
/**
 * Returns TRUE if these two spatial object touch.
 * <p>
 * Returns TRUE if none of the points common to both geometries intersect 
 * the interiors of both geometries.
 * <br> The touch predicate returns TRUE if the boundary of one geometry intersects 
 * the interior of the other but the interiors do not intersect.
 * <br> The touch predicate returns TRUE if the boundary of one geometry intersects 
 * the interior of the other but the interiors do not intersect.
 * <br> The touch predicate returns TRUE if the boundaries of both geometries intersect 
 * but the interiors do not.
 */
//boolean touches(SpatialObject obj) throws SpatialRelationException;// {return false;}

	
	/* ************** 
	 * Static methods
	 * **************/

    /**
     * Check whether a -- b -- c is a counter-clockwise turn.
     * <p>
     * +1 if counter-clockwise, -1 if clockwise, 0 if collinear.
     */
    static int isCCW(Point a, Point b, Point c) {
        double area2 = (b.x() - a.x()) * (c.y() - a.y()) -
                (c.x() - a.x()) * (b.y() - a.y());
        if (area2 < 0) return -1;
        else if (area2 > 0) return +1;
        else return 0;
    }

    /**
     * Check whether a--b--c are collinear.
     */
    static boolean isCollinear(Point a, Point b, Point c) {
        return isCCW(a, b, c) == 0;
    }
}
