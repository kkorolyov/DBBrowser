package dev.kkorolyov.sqlob.logging;

/**
 * A message which is executed and used only by an enabled logger.
 */
@FunctionalInterface
public interface LazyMessage {
	/**
	 * Constructs and returns the message built by an implementation of this interface.
	 * @return constructed message
	 */
	String execute();
}
