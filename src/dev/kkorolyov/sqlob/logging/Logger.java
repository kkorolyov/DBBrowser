package dev.kkorolyov.sqlob.logging;

/**
 * Provides the appropriate logger or a stub depending on the existence of the {@code SimpleLogs} library during runtime.
 */
public class Logger {
	private static boolean simpleLogsFound = true;
	private static LoggerInterface stub;
	
	/**
	 * Returns the appropriate logger for a specified name or a stub if the {@code SimpleLogs} library is not found.
	 * @param name name of logger to get
	 * @return appropriate logger
	 */
	public static LoggerInterface getLogger(String name) {
		if (simpleLogsFound) {
			try {
				return (LoggerInterface) Class.forName("dev.kkorolyov.sqlob.logging.LoggerImplementation").getDeclaredConstructor(new Class[]{String.class}).newInstance(name);
			} catch (Exception e) {
				simpleLogsFound = false;	// SimpleLogs not found
			}
		}
		if (stub == null)
			stub = new LoggerStub();
		return stub;
	}
}
