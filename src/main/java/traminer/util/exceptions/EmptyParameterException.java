package traminer.util.exceptions;

/**
 * Exception thrown in case of empty input parameters.
 *
 * @author uqdalves
 */
@SuppressWarnings("serial")
public class EmptyParameterException extends IllegalArgumentException {
    /**
     * Constructs a new empty exception without detail message.
     */
    public EmptyParameterException() {
    }

    /**
     * Constructs a new exception with the specified detail message.
     *
     * @param message The exception detail message.
     */
    public EmptyParameterException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the specified cause.
     *
     * @param cause The cause for the exception.
     */
    public EmptyParameterException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new exception with the specified cause
     * and a detail message.
     *
     * @param message The exception detail message.
     * @param cause   The cause for the exception.
     */
    public EmptyParameterException(String message, Throwable cause) {
        super(message, cause);
    }
}
