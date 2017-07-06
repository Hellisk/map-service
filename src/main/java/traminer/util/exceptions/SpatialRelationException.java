package traminer.util.exceptions;

/**
 * Exception for unsupported spatial relations.
 * 
 * @author uqdalves
 */
@SuppressWarnings("serial")
public class SpatialRelationException extends RuntimeException {
    /**
     * Constructs a new empty exception without detail message.
     */
    public SpatialRelationException() {
    }

    /**
     * Constructs a new exception with the specified detail message.
     *
     * @param message The exception detail message.
     */
    public SpatialRelationException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the specified cause.
     *
     * @param cause The cause for the exception.
     */
    public SpatialRelationException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new exception with the specified cause
     * and a detail message.
     *
     * @param message The exception detail message.
     * @param cause   The cause for the exception.
     */
    public SpatialRelationException(String message, Throwable cause) {
        super(message, cause);
    }
}