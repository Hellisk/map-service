package traminer.util.exceptions;

/**
 * @author uqdalves
 */
@SuppressWarnings("serial")
public class SpatialQueryException extends RuntimeException {
    public SpatialQueryException() {
    }

    public SpatialQueryException(String message) {
        super(message);
    }

    public SpatialQueryException(String errorName, String message) {
        super(errorName + " - " + message);
    }
}
