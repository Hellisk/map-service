package traminer.util.exceptions;

/**
 * @author uqdalves
 */
@SuppressWarnings("serial")
public class ClusteringException extends RuntimeException {
    public ClusteringException() {
    }

    public ClusteringException(String message) {
        super(message);
    }

    public ClusteringException(String errorName, String message) {
        super(errorName + " - " + message);
    }
}
