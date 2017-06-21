package traminer.util.exceptions;

/**
 * Exception for not allowed/supported spatial relations.
 *
 * @author uqdalves
 */
@SuppressWarnings("serial")
public class SpatialRelationException extends RuntimeException {
    public SpatialRelationException() {
    }

    public SpatialRelationException(String message) {
        super(message);
    }

    public SpatialRelationException(String errorName, String message) {
        super(errorName + " - " + message);
    }
}