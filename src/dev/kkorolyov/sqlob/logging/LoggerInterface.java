package dev.kkorolyov.sqlob.logging;

/**
 * Logger interface to support optional {@code SimpleLogs} dependency.
 */
public interface LoggerInterface {
	/**
	 * Logs an exception.
	 * @param e exception to log
	 */
	void exception(Exception e);
	/**
	 * Logs an {@code INFO}-level message.
	 * @param message message to log
	 */
	void info(String message);
	/**
	 * Logs a {@code DEBUG}-level message.
	 * @param message message to log
	 */
	void debug(String message);
}
