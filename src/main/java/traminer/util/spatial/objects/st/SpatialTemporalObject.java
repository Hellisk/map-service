package traminer.util.spatial.objects.st;

import traminer.util.spatial.objects.SpatialObject;

import java.util.Comparator;

/**
 * Base interface for Spatial-Temporal objects.
 * Spatial objects with temporal features.
 * 
 * @author uqdalves
 */
public interface SpatialTemporalObject extends SpatialObject {
    /**
     * Check whether these two spatial objects are of the same kind,
     * and whether they have the same 2D spatial coordinates and temporal
     * attributes. That is, check weather they are spatial-temporally
     * equivalents.
     *
     * @param obj The spatial-temporal object to compare.
     * @return True if these two spatial objects are
     * spatial-temporally equivalent.
     */
    boolean equalsST(SpatialTemporalObject obj);

    /**
     * @return The initial/start time of this spatial-temporal object.
     */
    long timeStart();

    /**
     * @return The final/end time of this spatial-temporal object.
     */
    long timeFinal();

    /**
     * The duration in time of this spatial-temporal object.
     * That is, the time duration this object was active.
     *
     * @return The temporal duration of this object.
     */
    default long duration() {
        return (this.timeFinal() - this.timeStart());
    }

    /**
     * Compare objects by their Temporal value. First by initial time,
     * then by final time.
     */
    Comparator<SpatialTemporalObject> TIME_COMPARATOR =
            new Comparator<SpatialTemporalObject>() {
                @Override
                public int compare(SpatialTemporalObject o1, SpatialTemporalObject o2) {
                    if (o1.timeStart() > o2.timeStart())
                        return 1;
                    if (o1.timeStart() < o2.timeStart())
                        return -1;
                    if (o1.timeFinal() > o2.timeFinal())
                        return 1;
                    if (o1.timeFinal() < o2.timeFinal())
                        return -1;
                    return 0;
                }
            };
}
