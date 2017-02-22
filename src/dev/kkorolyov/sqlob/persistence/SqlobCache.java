package dev.kkorolyov.sqlob.persistence;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.sql.*;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import dev.kkorolyov.sqlob.annotation.Transient;

/**
 * Operates on a dynamically-cached ({@link Class} -> {@link SqlobClass}) map.
 */
final class SqlobCache {
	private static final String ID_NAME = "uuid",
															ID_TYPE = "CHAR(36)";

	private final Map<Class<?>, SqlobClass<?>> classMap = new HashMap<>();
	private final Map<Class<?>, String> typeMap = new HashMap<>();
	private final Map<Class<?>, Extractor> extractorMap = new HashMap<>();
	

	<T> SqlobClass<T> get(Class<T> type, Connection conn) throws SQLException {
		@SuppressWarnings("unchecked")
		SqlobClass<T> result = (SqlobClass<T>) classMap.get(type);
		
		if (result == null) {
			result = new SqlobClass<>(type, buildFields(type, conn), ID_NAME, ID_TYPE).init(conn);	// Create + init
			classMap.put(type, result);
		}
		return result;
	}
	private <T> List<SqlobField> buildFields(Class<T> type, Connection conn) throws SQLException {
		List<SqlobField> fields = new LinkedList<>();
		
		for (Field field : type.getDeclaredFields()) {
			if (isPersistable(field)) {
				Class<?> fieldType = wrap(field.getType());
				String sqlType = typeMap.get(fieldType);
				SqlobClass<?> reference = (sqlType == null ? get(fieldType, conn) : null);
				
				fields.add(new SqlobField(field, extractorMap.get(fieldType), (sqlType == null ? ID_TYPE : sqlType), reference));
			}
		}
		return fields;
	}

	private static Class<?> wrap(Class<?> c) {
		switch (c.getSimpleName()) {
			case "byte": return Byte.class;
			case "short": return Short.class;
			case "int": return Integer.class;
			case "long": return Long.class;
			case "float": return Float.class;
			case "double": return Double.class;
			case "boolean": return Boolean.class;
			case "char": return Character.class;

			default: return c;
		}
	}

	private static boolean isPersistable(Field field) {
		int modifiers = field.getModifiers();

		return !Modifier.isStatic(modifiers) &&
					 !Modifier.isTransient(modifiers) &&
					 field.getAnnotation(Transient.class) == null;
	}
	
	void mapType(Class<?> c, String sql) {
		typeMap.put(c, sql);
	}
	private static Map<Class<?>, String> getDefaultTypeMap() {
		Map<Class<?>, String> map = new HashMap<>();
		
		map.put(Byte.class, "TINYINT");
		map.put(Short.class, "SMALLINT");
		map.put(Integer.class, "INTEGER");
		map.put(Long.class, "BIGINT");
		map.put(Float.class, "REAL");
		map.put(Double.class, "DOUBLE PRECISION");
		map.put(BigDecimal.class, "NUMERIC");
		
		map.put(Boolean.class, "BOOLEAN");
		
		map.put(Character.class, "CHAR(1)");
		
		map.put(String.class, "VARCHAR(1024)");
		
		map.put(byte[].class, "VARBINARY(1024)");
		
		map.put(Date.class, "DATE");
		map.put(Time.class, "TIME(6)");
		map.put(Timestamp.class, "TIMESTAMP(6)");
		
		return map;
	}
	
	void mapExtractor(Class<?> c, Extractor extractor) {
		extractorMap.put(c, extractor);
	}
	private static Map<Class<?>, Extractor> getDefaultExtractorMap() {
		Map<Class<?>, Extractor> map = new HashMap<>();
		
		map.put(Byte.class, ResultSet::getByte);
		map.put(Short.class, ResultSet::getShort);
		map.put(Integer.class, ResultSet::getInt);
		map.put(Long.class, ResultSet::getLong);
		map.put(Float.class, ResultSet::getFloat);
		map.put(Double.class, ResultSet::getDouble);
		map.put(BigDecimal.class, ResultSet::getBigDecimal);

		map.put(Boolean.class, ResultSet::getBoolean);

		map.put(Character.class, (rs, column) -> {
			String string = rs.getString(column);
			return string == null ? null : string.charAt(0);
		});
		map.put(String.class, ResultSet::getString);

		map.put(byte[].class, ResultSet::getBytes);

		map.put(Date.class, ResultSet::getDate);
		map.put(Time.class, ResultSet::getTime);
		map.put(Timestamp.class, ResultSet::getTimestamp);
		
		return map;
	}
}
