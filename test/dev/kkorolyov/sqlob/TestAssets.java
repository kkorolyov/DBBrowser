package dev.kkorolyov.sqlob;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import dev.kkorolyov.simpleprops.Properties;

@SuppressWarnings("javadoc")
public class TestAssets {
	private static final String HOST = "HOST",
															DATABASE = "DATABASE",
															USER = "USER",
															PASSWORD = "PASSWORD";	
	private static final Properties props = new Properties(new File("TestSQLOb.ini"), buildDefaults());
	private static final Map<Class<?>, Object> matchedTypes = new HashMap<>();
	
	static {
		matchedTypes.put(Boolean.class, false);
		
		matchedTypes.put(Short.class, (short) 0);
		matchedTypes.put(Integer.class, 0);
		matchedTypes.put(Long.class, (long) 0);
		matchedTypes.put(Float.class, (float) 0.0);
		matchedTypes.put(Double.class, 0.0);
		
		matchedTypes.put(Character.class, 'A');
		matchedTypes.put(String.class, "String");
		
		try {
			props.saveFile();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
	
	public static void clean() {
		try {
			Files.delete(Paths.get(database()));	// Delete SQLite DB
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
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
	
	public static Object getMatchedType(SqlobType type) {
		return matchedTypes.get(type.getTypeClass());
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
