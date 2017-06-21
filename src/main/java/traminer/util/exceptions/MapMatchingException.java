package traminer.util.exceptions;

/**
 * Exception for map-matching computation errors.
 *
 * @author uqdalves
 */
@SuppressWarnings("serial")
public class MapMatchingException extends RuntimeException {
    public MapMatchingException() {
    }

    public MapMatchingException(String message) {
        super(message);
    }

    public MapMatchingException(String errorName, String message) {
        super(errorName + " - " + message);
    }
}
