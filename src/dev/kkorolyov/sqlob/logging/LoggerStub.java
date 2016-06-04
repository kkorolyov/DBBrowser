package dev.kkorolyov.sqlob.logging;

/**
 * Stub logger class to be loaded when {@code SimpleLogs} not found.
 */
public class LoggerStub implements LoggerInterface {
	@Override
	public void exception(Exception e) {
		// No-op		
	}
	@Override
	public void info(String message) {
		// No-op
	}
	@Override
	public void debug(String message) {
		// No-op
	}
}
