package dev.kkorolyov.sqlob;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;

import dev.kkorolyov.simpleprops.Properties;
import dev.kkorolyov.sqlob.construct.SqlType;

@SuppressWarnings("javadoc")
public class TestAssets {
	private static final String HOST = "HOST",
															DATABASE = "DATABASE",
															USER = "USER",
															PASSWORD = "PASSWORD";	
	private static final Properties props = new Properties(new File("TestSQLOb.ini"), buildDefaults());
	private static final Map<SqlType, Object> matchedTypes = new HashMap<>();
	
	static {
		matchedTypes.put(SqlType.BOOLEAN, false);
		
		matchedTypes.put(SqlType.SMALLINT, (short) 0);
		matchedTypes.put(SqlType.INTEGER, 0);
		matchedTypes.put(SqlType.BIGINT, (long) 0);
		matchedTypes.put(SqlType.REAL, (float) 0.0);
		matchedTypes.put(SqlType.DOUBLE, 0.0);
		
		matchedTypes.put(SqlType.CHAR, 'A');
		matchedTypes.put(SqlType.VARCHAR, "String");
		
		assert (matchedTypes.size() == SqlType.values().length);
		
		try {
			props.saveFile();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
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
	
	public static Object getMatchedType(SqlType type) {
		return matchedTypes.get(type);
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
