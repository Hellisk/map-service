package traminer.util.exceptions;

/**
 * Exception for spatial objects invariant errors.
 *
 * @author uqdalves
 */
@SuppressWarnings("serial")
public class SpatialObjectException extends RuntimeException {
    public SpatialObjectException() {
    }

    public SpatialObjectException(String message) {
        super(message);
    }

    public SpatialObjectException(String errorName, String message) {
        super(errorName + " - " + message);
    }
}
