package dev.kkorolyov.sqlob.logging;

import dev.kkorolyov.simplelogs.Logger.Level;

/**
 * Logger implementation to be loaded when {@code SimpleLogs} found.
 */
public class LoggerImplementation implements LoggerInterface {
	private dev.kkorolyov.simplelogs.Logger log;
	
	LoggerImplementation(String name) {
		log = dev.kkorolyov.simplelogs.Logger.getLogger(name);		
	}

	@Override
	public void exception(Exception e) {
		log.exception(e);
	}
	@Override
	public void info(String message) {
		log.info(message);
	}
	@Override
	public void debug(String message) {
		log.debug(message);
	}
}
