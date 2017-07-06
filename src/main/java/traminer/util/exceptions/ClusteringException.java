package traminer.util.exceptions;

/**
 * Exception for Clustering methods runtime errors.
 * 
 * @author uqdalves
 */
@SuppressWarnings("serial")
public class ClusteringException extends RuntimeException {
    /**
     * Constructs a new Clustering exception without
     * detail message.
     */
    public ClusteringException() {
    }

    /**
     * Constructs a new Clustering exception with
     * the specified detail message.
     *
     * @param message The exception detail message.
     */
    public ClusteringException(String message) {
        super(message);
    }

    /**
     * Constructs a new Clustering exception with
     * the specified cause.
     *
     * @param cause The cause for the exception.
     */
    public ClusteringException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new Clustering exception with the specified
     * cause and a detail message.
     *
     * @param message The exception detail message.
     * @param cause   The cause for the exception.
     */
    public ClusteringException(String message, Throwable cause) {
        super(message, cause);
    }
}
