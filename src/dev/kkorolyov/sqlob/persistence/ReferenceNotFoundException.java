package dev.kkorolyov.sqlob.persistence;

/**
 * Exception thrown when an object referenced by a persisted object is not found.
 */
public class ReferenceNotFoundException extends Exception {
	private static final long serialVersionUID = 1977346876381395181L;

	/**
	 * Constructs an instance of this exception with a {@code null} message.
	 */
	public ReferenceNotFoundException() {
		this(null);
	}
	/**
	 * Constructs an instance of this exception referencing a specific object.
	 * @param o not-found object
	 */
	public ReferenceNotFoundException(Object o) {
		this(o.toString());
	}
	/**
	 * Constructs an instance of this exception with the specified message.
	 * @param message detail message
	 */
	public ReferenceNotFoundException(String message) {
		super(message);
	}
}
