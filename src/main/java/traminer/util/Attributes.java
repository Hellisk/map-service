package traminer.util;

import java.util.HashMap;

/**
 * Attributes object map. Maps an attribute object's String name (key) to the
 * attribute's value. The value for a single attribute can be of any Object
 * type.
 * 
 * @author uqdalves
 */
@SuppressWarnings("serial")
public class Attributes extends HashMap<String, Object> {
    /**
     * Creates an empty Attributes map object
     */
    public Attributes() {
        super(1);
    }

    /**
     * Creates a new empty Attributes map with the given initial capacity.
     *
     * @param initialCapacity Attributes map initial capacity.
     */
    public Attributes(int initialCapacity) {
        super(initialCapacity);
    }

    /**
     * Attributes map object copy constructor.
     *
     * @param attr Attributes to copy from.
     */
    public Attributes(Attributes attr) {
        super(attr);
    }

    /**
     * Map the given attribute's name to it's value. If this Attribute's Map
     * already contains the given attribute's name, then the value for the
     * attribute will be overridden.
     *
     * @throws IllegalArgumentException if attributes's name is either empty or null.
     */
    public void putAttribute(String name, Object value) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Attribute's name " + "must not be empty nor null.");
        }
        this.put(name, value);
    }

    /**
     * Get the value of the attribute with given name.
     *
     * @throws IllegalArgumentException if attributes's name is either empty or null.
     */
    public Object getAttributeValue(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Attribute's name " + "must not be empty nor null.");
        }
        return this.get(name);
    }
}
