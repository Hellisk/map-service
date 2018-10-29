package mapupdate.util.object.spatialobject;

import com.vividsolutions.jts.geom.Geometry;
import mapupdate.util.exceptions.SpatialObjectConstructionException;
import mapupdate.util.exceptions.SpatialRelationException;
import mapupdate.util.object.SpatialInterface;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Base interface for all spatial objects (i.e. simple, complex, and collections).
 *
 * @author uqdalves
 */
public interface SpatialObject extends SpatialInterface, Cloneable {
    /* **************
     * Set and Get functions for attributes common to all spatial objects
     * **************/

    /**
     * Set the id of this spatial object.
     *
     * @param id Object's identifier.
     */
    void setId(String id);

    /**
     * @return The identifier of this spatial object.
     */
    String getId();

    /**
     * Set the  of the parent of this spatial object (if any).
     * Default value is NULL.
     *
     * @param parentId Parent object identifier.
     */
    void setParentId(String parentId);

    /**
     * @return The identifier of the parent of this  spatial object (if any).
     */
    String getParentId();

    /**
     * Set the number of spatial dimensions of this spatial object.
     *
     * @param dim A number in the range [1..127].
     */
    void setDimension(byte dim);

    /**
     * @return The number of spatial dimensions of this spatial object [1..127].
     */
    byte getDimension();

    /**
     * @return The list of points/vertexes of this spatial object.
     * @see Point
     */
    List<Point> getCoordinates();

    /**
     * @return The list of edges/segments of this spatial object (if any).
     * @see Segment
     */
    List<Segment> getEdges();

    /**
     * @return Return the Minimum Bounding Rectangle (MBR) of
     * this spatial object.
     * @throws SpatialObjectConstructionException if object is empty or null.
     */
    default Rectangle mbr() {
        List<Point> coordList = getCoordinates();
        if (coordList == null || coordList.isEmpty()) {
            throw new SpatialObjectConstructionException(
                    "Cannot calculate MBR. Object is empty or null.");
        }

        int size = coordList.size();
        coordList.sort(Point.X_COMPARATOR);
        double minx = coordList.get(0).x();
        double maxx = coordList.get(size - 1).x();
        coordList.sort(Point.Y_COMPARATOR);
        double miny = coordList.get(0).y();
        double maxy = coordList.get(size - 1).y();

        return new Rectangle(minx, miny, maxx, maxy);
    }

    /**
     * Check whether this spatial object is a closed object.
     *
     * @return True if the object is composed by edges (or a circle)
     * and its first and final edge points are the same.
     */
    boolean isClosed();

    /**
     * Makes an exact copy of this spatial object.
     *
     * @return A copy of this object.
     */
    SpatialObject clone();

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
     *
     * @param obj The spatial object to compare.
     * @return True if these two spatial objects are spatially equivalent.
     */
    boolean equals2D(SpatialObject obj);

    /**
     * @return A String representation of this spatial object.
     */
    @Override
    String toString();

    /**
     * Compare this spatial object using the given comparator.
     * The result of this method is the same as comparator.compare(this, obj).
     *
     * @param obj        The spatial object to compare.
     * @param comparator The comparator to use.
     * @return A negative integer, zero, or a positive integer as the first
     * argument is less than, equal to, or greater than the second.
     */
    default int compareTo(SpatialObject obj, Comparator<SpatialObject> comparator) {
        if (obj == null) {
            throw new NullPointerException("Spatial object to compare cannot be null.");
        }
        if (comparator == null) {
            throw new NullPointerException("Spatial object's comparator cannot be null.");
        }
        return comparator.compare(this, obj);
    }

    /**
     * Cast this spatial object to an equivalent JavaTopologicalSuit (JTS)
     * geometry object.
     *
     * @return Returns the JTS equivalent Geometry object of this Spatial Object.
     * @see Geometry
     */
    Geometry toJTSGeometry();

    /* **************
     * Topological operations (spatial relations)
     * **************/

    /**
     * Check whether the given spatial object is completely contained by this spatial object.
     * <p>
     * The contains predicate returns the exact opposite result of the Within predicate.
     * <br> The boundary and interior of the second geometry are not allowed to intersect
     * the exterior of the first geometry and the geometries may not be equal.
     * <br> The interiors of both geometries must intersect and that the interior and
     * boundary of the secondary (geometry b) must not intersect the exterior of
     * the primary (geometry a).
     *
     * @param obj The spatial object to check.
     * @return Returns TRUE if the given spatial object 'obj' is completely contained
     * by this spatial object.
     * @throws SpatialRelationException If the operation is not supported by these spatial objects.
     */
    default boolean contains(SpatialObject obj) throws SpatialRelationException {
        if (obj == null) {
            throw new NullPointerException("Spatial object cannot be null.");
        }
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
     * Check whether this spatial object is completely within the given spatial object 'obj'.
     * <p>
     * Within tests for the exact opposite result of Contains.
     * <br> The boundary and interior of the first geometry are not allowed
     * to intersect the exterior of the second geometry and the first geometry
     * may not equal the second geometry.
     * <br> The interiors of both geometries must intersect and that the interior
     * and boundary of the primary geometry (geometry a) must not intersect the
     * exterior of the secondary (geometry b).
     *
     * @param obj The spatial object to check.
     * @return Returns TRUE if this spatial object is completely within the
     * given spatial object 'obj'.
     * @throws SpatialRelationException If the operation is not supported by these spatial objects.
     */
    default boolean within(SpatialObject obj) throws SpatialRelationException {
        if (obj == null) {
            throw new NullPointerException("Spatial object cannot be null.");
        }
        try {
            if (obj instanceof Circle) {
                return ((Circle) obj).contains(this);
            }
            return this.toJTSGeometry().within(obj.toJTSGeometry());
        } catch (Exception e) {
            throw new SpatialRelationException("'Within' operation not supported "
                    + "for these spatial objects.");
        }
    }

    /**
     * Check whether the given spatial object is completely contained by this spatial object.
     * <p>
     * ''a'' is covered by ''b'' (extends Within) if every point of ''a'' is a point of ''b'',
     * and the interiors of the two geometries have at least one point in common.
     *
     * @param obj The spatial object to check.
     * @return Returns TRUE if the given spatial object 'obj' is completely contained
     * by this spatial object.
     * @throws SpatialRelationException If the operation is not supported by these spatial objects.
     */
    default boolean coveredBy(SpatialObject obj) throws SpatialRelationException {
        if (obj == null) {
            throw new NullPointerException("Spatial object cannot be null.");
        }
        try {
            if (obj instanceof Circle) {
                return ((Circle) obj).covers(this);
            }
            return this.toJTSGeometry().coveredBy(obj.toJTSGeometry());
        } catch (Exception e) {
            throw new SpatialRelationException("'CoveredBy' operation not supported "
                    + "for these spatial objects.");
        }
    }

    /**
     * Check whether the given spatial object lies in this spatial object.
     * <p>
     * ''a'' covers ''b'' if no points of ''b'' lie in the exterior of ''a'', or every
     * point of ''b'' is a point of (the interior or boundary of) ''a''.
     *
     * @param obj The spatial object to check.
     * @return Returns TRUE is the given spatial object 'obj' lies in this spatial object.
     * @throws SpatialRelationException If the operation is not supported by these spatial objects.
     */
    default boolean covers(SpatialObject obj) throws SpatialRelationException {
        if (obj == null) {
            throw new NullPointerException("Spatial object cannot be null.");
        }
        try {
            if (obj instanceof Circle) {
                return ((Circle) obj).coveredBy(this);
            }
            return this.toJTSGeometry().covers(obj.toJTSGeometry());
        } catch (Exception e) {
            throw new SpatialRelationException("'Covers' operation not supported "
                    + "for these spatial objects.");
        }
    }

    /**
     * Check whether these two spatial objects cross each other.
     * <p>
     * Returns TRUE if the intersection results in a geometry whose dimension is one
     * less than the maximum dimension of the two source geometries, and the intersection
     * set is interior to both source geometries.
     * <br> The interiors must intersect and at least the interior of the primary (geometry a)
     * must intersect the exterior of the secondary (geometry b).
     * <br> The dimension of the intersection of the interiors must be 0 (intersect at a point).
     *
     * @param obj The spatial object to check.
     * @return Returns TRUE if these two spatial objects cross.
     * @throws SpatialRelationException If the operation is not supported by these spatial objects.
     */
    default boolean crosses(SpatialObject obj) throws SpatialRelationException {
        if (obj == null) {
            throw new NullPointerException("Spatial object cannot be null.");
        }
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
     * Check whether the intersection of these tow spatial objects does not result in an empty set.
     * <p>
     * Intersects returns the exact opposite result of Disjoint.
     * <br> The intersects predicate returns TRUE if the interiors of both geometries intersect.
     * <br> The intersects predicate returns TRUE if the boundary of the first geometry intersects
     * the boundary of the second geometry.
     * <br> The intersects predicate returns TRUE if the boundary of the first geometry intersects
     * the interior of the second.
     * <br> The intersects predicate returns TRUE if the boundaries of either geometry intersect.
     *
     * @param obj The spatial object to check.
     * @return Returns TRUE if the intersection of these tow spatial objects does not result in an empty set.
     * @throws SpatialRelationException If the operation is not supported by these spatial objects.
     */
    default boolean intersects(SpatialObject obj) throws SpatialRelationException {
        if (obj == null) {
            throw new NullPointerException("Spatial object cannot be null.");
        }
        try {
            if (obj instanceof Circle) {
                return obj.intersects(this);
            }
            return this.toJTSGeometry().intersects(obj.toJTSGeometry());
        } catch (Exception e) {
            throw new SpatialRelationException("'Intersects' operation not supported "
                    + "for these spatial objects.");
        }
    }

    /**
     * Check whether these two spatial objects overlap.
     * <p>
     * Compares two geometries of the same dimension and returns TRUE if their intersection set
     * results in a geometry different from both, but of the same dimension.
     * <br> Overlap returns TRUE only for geometries of the same dimension, and only when their
     * intersection set results in a geometry of the same dimension. In other words, if the
     * intersection of two polygons results in polygon, then overlap returns TRUE.
     * <br> The overlap predicate returns TRUE if the interior of both geometries intersects
     * the others interior and exterior.
     *
     * @param obj The spatial object to check.
     * @return Returns TRUE if these two spatial objects overlap.
     * @throws SpatialRelationException If the operation is not supported by these spatial objects.
     */
    default boolean overlaps(SpatialObject obj) throws SpatialRelationException {
        if (obj == null) {
            throw new NullPointerException("Spatial object cannot be null.");
        }
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

    /**
     * Clone the semantic attributes (super-class attributes) of this spatial
     * object into the given spatial object (auxiliary clone method).
     *
     * @param obj The spatial object to receive the cloned attributes.
     */
    default void cloneTo(SpatialObject obj) {
        if (obj == null) {
            throw new NullPointerException("Spatial object to receive "
                    + "cloned attributes cannot be null.");
        }
        if (this.getId() != null) {
            obj.setId(this.getId());
        }
        obj.setDimension(this.getDimension());
    }
}
