package traminer.util.exceptions;

/**
 * Exception for map-matching computation errors.
 * 
 * @author uqdalves
 */
@SuppressWarnings("serial")
public class MapMatchingException extends RuntimeException {
    /**
     * Constructs a new empty exception without detail message.
     */
    public MapMatchingException() {
    }

    /**
     * Constructs a new exception with the specified detail message.
     *
     * @param message The exception detail message.
     */
    public MapMatchingException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the specified cause.
     *
     * @param cause The cause for the exception.
     */
    public MapMatchingException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new exception with the specified cause
     * and a detail message.
     *
     * @param message The exception detail message.
     * @param cause   The cause for the exception.
     */
    public MapMatchingException(String message, Throwable cause) {
        super(message, cause);
    }
}
