package traminer.util.exceptions;

/**
 * Exception thrown in case of NULL input method parameters.
 *
 * @author uqdalves
 */
@SuppressWarnings("serial")
public class NullParameterException extends NullPointerException {
    /**
     * Constructs a new exception without detail message.
     */
    public NullParameterException() {
    }

    /**
     * Constructs a new exception with the specified detail message.
     *
     * @param message The exception detail message.
     */
    public NullParameterException(String message) {
        super(message);
    }
}
