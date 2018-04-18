package edu.uq.dke.mapupdate.util.exceptions;

/**
 * Exception for spatial queries processing errors.
 *
 * @author uqdalves
 */
@SuppressWarnings("serial")
public class SpatialQueryException extends RuntimeException {
    /**
     * Constructs a new empty exception without detail message.
     */
    public SpatialQueryException() {
    }

    /**
     * Constructs a new exception with the specified detail message.
     *
     * @param message The exception detail message.
     */
    public SpatialQueryException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the specified cause.
     *
     * @param cause The cause for the exception.
     */
    public SpatialQueryException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new exception with the specified cause
     * and a detail message.
     *
     * @param message The exception detail message.
     * @param cause   The cause for the exception.
     */
    public SpatialQueryException(String message, Throwable cause) {
        super(message, cause);
    }
}
