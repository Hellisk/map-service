package util.object.spatialobject;

import java.util.Comparator;

/**
 * Base interface for Spatial-Temporal objects. Spatial objects with temporal features.
 *
 * @author uqdalves, Hellisk
 */
public interface SpatialTemporalObject extends SpatialObject {
	/**
	 * Compare objects by their Temporal value. First by initial time,
	 * then by final time.
	 */
	Comparator<SpatialTemporalObject> TIME_COMPARATOR = (o1, o2) -> {
		if (o1.timeStart() > o2.timeStart())
			return 1;
		if (o1.timeStart() < o2.timeStart())
			return -1;
		return Long.compare(o1.timeFinal(), o2.timeFinal());
	};
	
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
}