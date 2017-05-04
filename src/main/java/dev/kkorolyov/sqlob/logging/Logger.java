package dev.kkorolyov.sqlob.logging;

/**
 * Provides the appropriate logger or a stub depending on the existence of the {@code SimpleLogs} library during runtime.
 */
public class Logger {
	private static boolean simpleLogsFound = true;
	private static Logger stub;
	
	/**
	 * Returns the appropriate logger for a specified name or a stub if the {@code SimpleLogs} library is not found.
	 * @param name name of logger to get
	 * @return appropriate logger
	 */
	public static Logger getLogger(String name) {
		if (simpleLogsFound) {
			try {
				return (Logger) Class.forName("dev.kkorolyov.sqlob.logging.LoggerImplementation").getDeclaredConstructor(String.class).newInstance(name);
			} catch (Exception e) {
				simpleLogsFound = false;	// SimpleLogs not found
			}
		}
		if (stub == null) stub = new Logger();
		return stub;
	}
	
	/**
	 * Logs an exception.
	 * @param e exception to log
	 */
	public void exception(Throwable e) {/*Default no-op*/}
	/**
	 * Logs an exception.
	 * @param level message level
	 * @param e exception to log
	 */
	public void exception(int level, Throwable e) { /*Default no-op*/}

	/**
	 * Logs a {@code SEVERE}-level message.
	 * @param message message to log
	 * @param args optional arguments to resolve and inject into message
	 */
	public void severe(String message, Object... args) {/*Default no-op*/}
	/**
	 * Logs a {@code WARNING}-level message.
	 * @param message message to log
	 * @param args optional arguments to resolve and inject into message
	 */
	public void warning(String message, Object... args) {/*Default no-op*/}
	/**
	 * Logs an {@code INFO}-level message.
	 * @param message message to log
	 * @param args optional arguments to resolve and inject into message
	 */
	public void info(String message, Object... args) {/*Default no-op*/}
	/**
	 * Logs a {@code DEBUG}-level message.
	 * @param message message to log
	 * @param args optional arguments to resolve and inject into message
	 */
	public void debug(String message, Object... args) {/*Default no-op*/}
}
