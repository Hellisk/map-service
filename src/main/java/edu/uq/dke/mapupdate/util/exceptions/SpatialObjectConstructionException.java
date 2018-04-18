package edu.uq.dke.mapupdate.util.exceptions;

/**
 * Exception for spatial objects invariant errors.
 *
 * @author uqdalves
 */
@SuppressWarnings("serial")
public class SpatialObjectConstructionException extends RuntimeException {
    /**
     * Constructs a new empty exception without detail message.
     */
    public SpatialObjectConstructionException() {
    }

    /**
     * Constructs a new exception with the specified detail message.
     *
     * @param message The exception detail message.
     */
    public SpatialObjectConstructionException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the specified cause.
     *
     * @param cause The cause for the exception.
     */
    public SpatialObjectConstructionException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new exception with the specified cause
     * and a detail message.
     *
     * @param message The exception detail message.
     * @param cause   The cause for the exception.
     */
    public SpatialObjectConstructionException(String message, Throwable cause) {
        super(message, cause);
    }
}
