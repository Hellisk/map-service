package util.object.spatialobject;

/**
 * Base superclass for simple/basic spatial objects.
 * Simple spatial objects are by definition immutable.
 *
 * @author uqdalves
 */
public abstract class SimpleSpatialObject implements SpatialObject {

    /**
     * Spatial object identifier.
     */
    private String oid = null;

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
    public String getID() {
        return this.oid;
    }

    @Override
    public void setID(String id) {
        this.oid = id;
    }

    @Override
    public void setDimension(byte dim) {
        if (dim < 1) {
			throw new IllegalArgumentException("Number of spatial dimensions must be positive [1..127].");
        }
        this.dimension = dim;
    }

    @Override
    public byte getDimension() {
        return this.dimension;
    }

    @Override
    public abstract SimpleSpatialObject clone();
}
