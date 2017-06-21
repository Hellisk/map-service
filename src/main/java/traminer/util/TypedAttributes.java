package traminer.util;

import java.util.HashMap;

/**
 * Attributes object map. Maps an attribute object's String name (key) to the
 * attribute's value. In this implementation the value for all attributes must
 * be of the same type T.
 *
 * @author uqdalves
 */
@SuppressWarnings("serial")
public class TypedAttributes<T> extends HashMap<String, T> {
    /**
     * Creates an empty TypedAttributes map object
     */
    public TypedAttributes() {
        super(1);
    }

    /**
     * Creates a new empty TypeAttributes map with the given initial capacity.
     *
     * @param initialCapacity Attributes map initial capacity.
     */
    public TypedAttributes(int initialCapacity) {
        super(initialCapacity);
    }

    /**
     * TypedAttributes map object copy constructor.
     *
     * @param attr TypedAttributes to copy from.
     */
    public TypedAttributes(TypedAttributes<T> attr) {
        super(attr);
    }

    /**
     * Map the given attribute's name to it's value. If this Attribute's Map
     * already contains the given attribute's name, then the value for the
     * attribute will be overridden.
     *
     * @throws IllegalArgumentException if attributes's name is either empty or null.
     */
    public void putAttribute(String name, T value) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Attribute's name must not be empty nor null.");
        }
        this.put(name, value);
    }

    /**
     * Get the value of the attribute with given name.
     *
     * @throws IllegalArgumentException if attributes's name is either empty or null.
     */
    public T getAttributeValue(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Attribute's name must not be empty nor null.");
        }
        return this.get(name);
    }
}
