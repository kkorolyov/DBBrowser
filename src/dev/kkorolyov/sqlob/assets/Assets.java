package dev.kkorolyov.sqlob.assets;

import java.io.IOException;
import java.io.UncheckedIOException;

import dev.kkorolyov.simpleprops.Properties;

/**
 * Centralized access to all assets.
 */
public class Assets {
	@SuppressWarnings("javadoc")
	public static final String PROPERTIES_NAME = "SQLOb.ini";
	private static final String HOST = "HOST",
															DATABASE = "DATABASE",
															USER = "USER",
															PASSWORD = "PASSWORD";

	private static Properties props;

	/**
	 * Initializes all assets.
	 * @return {@code true} if properties were just initialized and must be set.
	 */
	public static boolean init() {
		boolean firstInit = false;
		
		props = Properties.getInstance((PROPERTIES_NAME));
		if (props.size() < 4) {
			initProperties();
			
			firstInit = true;
		}
		return firstInit;
	}
	private static void initProperties() {
		props.clear();
		
		props.addProperty(HOST, "");
		props.addProperty(DATABASE, "");
		props.addProperty(USER, "");
		props.addProperty(PASSWORD, "");
		
		try {
			props.saveToFile();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
	
	/** @return hostname value */
	public static String hostname() {
		return props.getValue(HOST);
	}
	/** @return database value */
	public static String database() {
		return props.getValue(DATABASE);
	}
	/** @return user value */
	public static String user() {
		return props.getValue(USER);
	}
	/** @return password value */
	public static String password() {
		return props.getValue(PASSWORD);
	}
}
