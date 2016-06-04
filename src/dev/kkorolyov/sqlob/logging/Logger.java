package dev.kkorolyov.sqlob.logging;

/**
 * Provides the appropriate logger or a stub depending on the existence of the {@code SimpleLogs} API during runtime.
 */
public class Logger {
	private static final boolean SIMPLE_LOGS_FOUND; 
		
	static {
		boolean simpleLogsFound = true;
		try {
			Class.forName("dev.kkorolyov.simplelogs.Logger");
		} catch (ClassNotFoundException e) {
			simpleLogsFound = false;
		}
		SIMPLE_LOGS_FOUND = simpleLogsFound;
	}
	
	/**
	 * Returns the appropriate logger for a specified name or a stub if the {@code SimpleLogs} API is not found.
	 * @param name name of logger to get
	 * @return appropriate logger
	 */
	public static LoggerInterface getLogger(String name) {
		LoggerInterface toReturn = new LoggerStub();
		
		if (SIMPLE_LOGS_FOUND) {
			try {
				toReturn = (LoggerInterface) Class.forName("dev.kkorolyov.sqlob.logging.LoggerImplementation").getDeclaredConstructor(new Class[]{String.class}).newInstance(name);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return toReturn;
	}
}
