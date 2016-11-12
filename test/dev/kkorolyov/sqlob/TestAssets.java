package dev.kkorolyov.sqlob;

import java.io.File;

import dev.kkorolyov.simpleprops.Properties;

@SuppressWarnings("javadoc")
public class TestAssets {
	private static final String HOST = "HOST",
															DATABASE = "DATABASE",
															USER = "USER",
															PASSWORD = "PASSWORD";	
	private static final Properties props = new Properties(new File("test/TestSQLOb.ini"), buildDefaults());
	
	public static String host() {
		return props.get(HOST);
	}
	public static String database() {
		return props.get(DATABASE);
	}
	public static String user() {
		return props.get(USER);
	}
	public static String password() {
		return props.get(PASSWORD);
	}
	
	private static Properties buildDefaults() {
		Properties defaults = new Properties();
		defaults.put(HOST, "");
		defaults.put(DATABASE, "");
		defaults.put(USER, "");
		defaults.put(PASSWORD, "");
		
		return defaults;
	}
}
