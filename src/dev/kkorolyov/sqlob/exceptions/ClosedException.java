package dev.kkorolyov.sqlob.exceptions;

/**
 * Exception thrown when attempting to access a closed resource.
 */
public class ClosedException extends Exception {
	private static final long serialVersionUID = -6584782701000159725L;
	private static final String message = "The resource is closed";
	
	/**
	 * Constructs an instance of this exception.
	 */
	public ClosedException() {
		super(message);
	}
}
