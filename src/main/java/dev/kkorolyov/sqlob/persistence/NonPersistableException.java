package dev.kkorolyov.sqlob.persistence;

/**
 * Exception thrown when a class that does not match persistence requirements is used in a persistence function.
 */
public class NonPersistableException extends RuntimeException {
	private static final long serialVersionUID = 9189881440395925878L;

	/**
	 * Constructs an instance of this exception with a {@code null} message.
	 */
	public NonPersistableException() {
		this(null);
	}
	/**
	 * Constructs an instance of this exception with the specified message.
	 * @param message detail message
	 */
	public NonPersistableException(String message) {
		super(message);
	}
	/**
	 * Constructs an instance of this exception with the specified message and cause.
	 * @param message detail message
	 * @param cause {@code Throwable} causing this exception to be thrown
	 */
	public NonPersistableException(String message, Throwable cause) {
		super(message, cause);
	}
}
