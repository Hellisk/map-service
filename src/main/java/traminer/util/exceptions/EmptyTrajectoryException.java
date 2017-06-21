package traminer.util.exceptions;

/**
 * @author uqdalves
 */
@SuppressWarnings("serial")
public class EmptyTrajectoryException extends RuntimeException {
    public EmptyTrajectoryException() {
    }

    public EmptyTrajectoryException(String message) {
        super(message);
    }

    public EmptyTrajectoryException(String errorName, String message) {
        super(errorName + " - " + message);
    }
}
