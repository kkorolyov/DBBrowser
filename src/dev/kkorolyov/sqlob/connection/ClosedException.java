package dev.kkorolyov.sqlob.connection;

/**
 * Exception thrown when attempting to access a closed resource.
 */
public class ClosedException extends RuntimeException {
	private static final long serialVersionUID = -6584782701000159725L;
	private static final String message = "The resource is closed";
	
	/**
	 * Constructs an instance of this exception.
	 */
	public ClosedException() {
		super(message);
	}
}
