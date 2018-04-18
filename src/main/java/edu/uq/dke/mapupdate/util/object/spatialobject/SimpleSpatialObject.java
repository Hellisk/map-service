package edu.uq.dke.mapupdate.util.object.spatialobject;

/**
 * Base superclass for simple/basic spatial objects.
 * Simple spatial objects are by definition immutable.
 *
 * @author uqdalves
 */
@SuppressWarnings("serial")
public abstract class SimpleSpatialObject implements SpatialObject {

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

    /**
     * Creates a new empty simple spatial object.
     */
    public SimpleSpatialObject() {
    }

    /**
     * Creates a new simple spatial object with the given id.
     *
     * @param id Spatial object identifier.
     */
    public SimpleSpatialObject(String id) {
        this.oid = id;
    }

    @Override
    public void setId(String id) {
        this.oid = id;
    }

    @Override
    public String getId() {
        return this.oid;
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
        return this.dimension;
    }

    @Override
    public abstract SimpleSpatialObject clone();

    /**
     * Display this spatial object in a graphical window.
     */
    // public abstract void display();

    /**
     * The distance between these two spatial objects.
     */
    // public double distance(SimpleSpatialObject obj);
}
