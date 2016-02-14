package dev.kkorolyov.ezdb.logging;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides a centralized logging interface for DBBrowser classes.
 */
public class DebugLogger {
	@SuppressWarnings("javadoc")
	public static final Level INFO_LEVEL = Level.INFO, DEBUG_LEVEL = Level.INFO, WARNING_LEVEL = Level.WARNING, SEVERE_LEVEL = Level.SEVERE;
	
	private static final Map<String, DebugLogger> instances = new HashMap<>();
	
	private Logger logger;
	private boolean infoEnabled = false, debugEnabled = false;
	
	/**
	 * Locates or creates a {@code DBBLogger} corresponding to the specified name.
	 * @param name logger name
	 * @return logger corresponding to name
	 */
	public static DebugLogger getLogger(String name) {
		DebugLogger instance;
		while ((instance = instances.get(name)) == null)
			instances.put(name, new DebugLogger(name));
		return instance;
	}
	
	/**
	 * Enables info and debug logging for all {@code DBLogger} instances.
	 */
	public static void enableAll() {
		for (DebugLogger instance : instances.values())
			instance.enable();
	}
	/**
	 * Disables info and debug logging for all {@code DBLogger} instances.
	 */
	public static void disableAll() {
		for (DebugLogger instance : instances.values())
			instance.disable();
	}
	
	/**
	 * Enables info logging for all {@code DBLogger} instances.
	 */
	public static void enableInfoAll() {
		for (DebugLogger instance : instances.values())
			instance.enableInfo();
	}
	/**
	 * Disables info logging for all {@code DBLogger} instances.
	 */
	public static void disableInfoAll() {
		for (DebugLogger instance : instances.values())
			instance.disableInfo();
	}
	
	/**
	 * Enables debug logging for all {@code DBLogger} instances.
	 */
	public static void enableDebugAll() {
		for (DebugLogger instance : instances.values())
			instance.enableDebug();
	}
	/**
	 * Disables debug logging for all {@code DBLogger} instances.
	 */
	public static void disableDebugAll() {
		for (DebugLogger instance : instances.values())
			instance.disableDebug();
	}
	
	private DebugLogger(String name) {
		logger = Logger.getLogger(name);
	}
	
	/**
	 * Logs an info message if info logging is enabled.
	 * @param message message to log
	 */
	public void info(String message) {
		if (infoEnabled) {
			logger.log(INFO_LEVEL, message);
		}
	}
	/**
	 * Logs a debug message if debug logging is enabled.
	 * @param message message to log
	 */
	public void debug(String message) {
		if (debugEnabled) {
			logger.log(DEBUG_LEVEL, message);
		}
	}
	
	/**
	 * Logs a warning exception.
	 * @param e warning exception to log
	 */
	public void exceptionWarning(Exception e) {
		logger.log(WARNING_LEVEL, e.getMessage(), e);
	}
	/**
	 * Logs a severe exception.
	 * @param e severe exception to log
	 */
	public void exceptionSevere(Exception e) {
		logger.log(SEVERE_LEVEL, e.getMessage(), e);
	}
	
	/**
	 * Enables info and debug logging.
	 */
	public void enable() {
		enableInfo();
		enableDebug();
	}
	/**
	 * Disables info and debug logging.
	 */
	public void disable() {
		disableInfo();
		disableDebug();
	}
	
	/**
	 * Enables info logging.
	 */
	public void enableInfo() {
		infoEnabled = true;
	}
	/**
	 * Disables info logging.
	 */
	public void disableInfo() {
		infoEnabled = false;
	}
	
	/**
	 * Enables debug logging.
	 */
	public void enableDebug() {
		debugEnabled = true;
	}
	/**
	 * Disables debug logging.
	 */
	public void disableDebug() {
		debugEnabled = false;
	}
}
