package dev.kkorolyov.sqlob.logging;

import dev.kkorolyov.simplelogs.Logger.Level;

/**
 * Logger implementation to be loaded when {@code SimpleLogs} found.
 */
class LoggerImplementation extends Logger {
	private dev.kkorolyov.simplelogs.Logger log;
	
	LoggerImplementation(String name) {
		log = dev.kkorolyov.simplelogs.Logger.getLogger(name, Level.DEBUG);
	}

	@Override
	public void exception(Exception e) {
		log.exception(e);
	}

	@Override
	public void severe(LazyMessage message) {
		log.severe(message::execute);
	}
	@Override
	public void warning(LazyMessage message) {
		log.warning(message::execute);
	}
	@Override
	public void info(LazyMessage message) {
		log.info(message::execute);
	}
	@Override
	public void debug(LazyMessage message) {
		log.debug(message::execute);
	}
}
