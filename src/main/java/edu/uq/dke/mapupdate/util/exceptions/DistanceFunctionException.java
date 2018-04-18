package edu.uq.dke.mapupdate.util.exceptions;

/**
 * Exception for Distance functions runtime errors.
 *
 * @author uqdalves
 */
@SuppressWarnings("serial")
public class
DistanceFunctionException extends RuntimeException {
    /**
     * Constructs a new Distance function exception without
     * detail message.
     */
    public DistanceFunctionException() {
    }

    /**
     * Constructs a new Distance function exception with
     * the specified detail message.
     *
     * @param message The exception detail message.
     */
    public DistanceFunctionException(String message) {
        super(message);
    }

    /**
     * Constructs a new Distance function exception with
     * the specified cause.
     *
     * @param cause The cause for the exception.
     */
    public DistanceFunctionException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new Distance function exception with the specified
     * cause and a detail message.
     *
     * @param message The exception detail message.
     * @param cause   The cause for the exception.
     */
    public DistanceFunctionException(String message, Throwable cause) {
        super(message, cause);
    }
}

