package dev.kkorolyov.sqlob;

import java.io.IOException;
import java.io.UncheckedIOException;

import dev.kkorolyov.simpleprops.Properties;

@SuppressWarnings("javadoc")
public class TestAssets {
	private static final String TEST_PROPERTIES_NAME = "TestSQLOb.ini";
	private static final String HOST = "HOST",
															DATABASE = "DATABASE",
															USER = "USER",
															PASSWORD = "PASSWORD";
	private static final String[][] defaults = {{HOST, ""},
																							{DATABASE, ""},
																							{USER, "postgres"},
																							{PASSWORD, ""}};
	private static final Properties props = Properties.getInstance(TEST_PROPERTIES_NAME, defaults);
	
	static {
		try {
			props.saveToFile();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
	
	public static String host() {
		return props.getValue(HOST);
	}
	public static String database() {
		return props.getValue(DATABASE);
	}
	public static String user() {
		return props.getValue(USER);
	}
	public static String password() {
		return props.getValue(PASSWORD);
	}
}
