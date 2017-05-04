package dev.kkorolyov.sqlob.logging;

import java.util.Arrays;

import dev.kkorolyov.simplelogs.Level;
import dev.kkorolyov.simplelogs.format.Formatters;

/**
 * Logger implementation to be loaded when {@code SimpleLogs} found.
 */
class LoggerImplementation extends Logger {
	private dev.kkorolyov.simplelogs.Logger log;
	
	LoggerImplementation(String name) {
		log = dev.kkorolyov.simplelogs.Logger.getLogger(name, Level.DEBUG, Formatters.simple());
	}

	@Override
	public void exception(Throwable e) {
		log.exception(e);
	}
	@Override
	public void exception(int level, Throwable e) {
		log.exception(level, e);
	}

	@Override
	public void severe(String message, Object... args) {
		log.severe(message, args);
	}
	@Override
	public void warning(String message, Object... args) {
		log.warning(message, args);
	}
	@Override
	public void info(String message, Object... args) {
		log.info(message, args);
	}
	@Override
	public void debug(String message, Object... args) {
		System.out.println(Arrays.toString(args));
		log.debug(message, args);
	}
}
