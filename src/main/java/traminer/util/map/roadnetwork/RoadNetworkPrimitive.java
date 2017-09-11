package traminer.util.map.roadnetwork;

import traminer.util.map.MapInterface;

import java.util.HashMap;
import java.util.Map;

/**
 * Superclass for road map (OSM) primitives.
 *
 * @see http://wiki.openstreetmap.org/wiki/OSM_XML
 * 
 * @author uqdalves
 */
@SuppressWarnings("serial")
public abstract class RoadNetworkPrimitive implements MapInterface, Cloneable {
    /**
     * Common attributes of OSM primitives - as in the OSM file
     */
    private String id = null;
    private String timeStamp = null;
    private Map<String, Object> tags = null;

    /**
     * Creates a new empty road network primitive.
     */
    public RoadNetworkPrimitive() {
    }

    /**
     * Creates a new empty road network primitive with the given Id.
     *
     * @param id The identifier of this road network primitive.
     */
    public RoadNetworkPrimitive(String id) {
        this.id = id;
    }

    /**
     * Creates a new empty road network primitive with the given Id
     * and time-stamp.
     *
     * @param id        The identifier of this road network primitive.
     * @param timeStamp The time-stamp of this road network primitive.
     */
    public RoadNetworkPrimitive(String id, String timeStamp) {
        this.id = id;
        this.timeStamp = timeStamp;
    }

    /**
     * @return The identifier of this road network primitive.
     */
    public String getId() {
        if (id == null) {
            return "";
        }
        return id;
    }

    /**
     * @param id The identifier of this road network primitive.
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return The time-stamp of this road network primitive.
     */
    public String getTimeStamp() {
        if (timeStamp == null) {
            return "";
        }
        return timeStamp;
    }

    /**
     * @param timeStamp The time-stamp of this road network primitive.
     */
    public void setTimeStamp(String timeStamp) {
        this.timeStamp = timeStamp;
    }

    /**
     * @return The list of tags of this road network primitive.
     */
    public Map<String, Object> getTags() {
        if (tags == null) {
            tags = new HashMap<>(1);
        }
        return tags;
    }

    /**
     * Add a tag to the list of tags of this road network primitive.
     *
     * @param key   The tag key/name.
     * @param value The tag value.
     */
    public void addTag(String key, Object value) {
        if (tags == null) {
            tags = new HashMap<String, Object>(1);
        }
        tags.put(key, value);
    }

    /**
     * Print this object: system out.
     */
    public abstract void print();

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        RoadNetworkPrimitive other = (RoadNetworkPrimitive) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }

    /**
     * Primitives are hash-coded by their Id by default.
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }
}
