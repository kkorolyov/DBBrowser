package dev.kkorolyov.sqlob.persistence;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.*;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import dev.kkorolyov.sqlob.annotation.Transient;

class SqlobCache {
	private static final String ID_NAME = "uuid",
															ID_TYPE = "CHAR(36)";
	private static final Map<Class<?>, Class<?>> wrapMap = new HashMap<>();

	private final Map<Class<?>, SqlobClass<?>> classMap = new HashMap<>();
	private Map<Class<?>, String> typeMap = getDefaultTypeMap();
	private Map<Class<?>, Extractor> extractorMap = getDefaultExtractorMap();
	
	static {
		wrapMap.put(byte.class, Byte.class);
		wrapMap.put(short.class, Short.class);
		wrapMap.put(int.class, Integer.class);
		wrapMap.put(long.class, Long.class);
		
		wrapMap.put(float.class, Float.class);
		wrapMap.put(double.class, Double.class);
		
		wrapMap.put(boolean.class, Boolean.class);
		
		wrapMap.put(char.class, Character.class);
	}
	
	private static Class<?> wrap(Class<?> c) {
		Class<?> wrapped = wrapMap.get(c);
		
		return wrapped == null ? c : wrapped;
	}
	
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
			if (field.getAnnotation(Transient.class) == null) {
				Class<?> fieldType = wrap(field.getType());
				String sqlType = typeMap.get(fieldType);
				SqlobClass<?> reference = (sqlType == null ? get(fieldType, conn) : null);
				
				field.setAccessible(true);
				
				fields.add(new SqlobField(field, extractorMap.get(fieldType), (sqlType == null ? ID_TYPE : sqlType), reference));
			}
		}
		return fields;
	}
	
	Map<Class<?>, String> getTypeMap() {
		return new HashMap<>(typeMap);
	}
	void setTypeMap(Map<Class<?>, String> typeMap) {
		classMap.clear();
		
		this.typeMap = (typeMap == null ? getDefaultTypeMap() : typeMap);
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
		map.put(Time.class, "TIME");
		map.put(Timestamp.class, "TIMESTAMP");
		
		return map;
	}
	
	Map<Class<?>, Extractor> getExtractorMap() {
		return new HashMap<>(extractorMap);
	}
	void setExtractorMap(Map<Class<?>, Extractor> extractorMap) {
		classMap.clear();
		
		this.extractorMap = (extractorMap == null ? getDefaultExtractorMap() : extractorMap);
	}
	private static Map<Class<?>, Extractor> getDefaultExtractorMap() {
		Map<Class<?>, Extractor> map = new HashMap<>();
		
		map.put(Byte.class, (rs, column) -> rs.getByte(column));
		map.put(Short.class, (rs, column) -> rs.getShort(column));
		map.put(Integer.class, (rs, column) -> rs.getInt(column));
		map.put(Long.class, (rs, column) -> rs.getLong(column));
		map.put(Float.class, (rs, column) -> rs.getFloat(column));
		map.put(Double.class, (rs, column) -> rs.getDouble(column));
		map.put(BigDecimal.class, (rs, column) -> rs.getBigDecimal(column));

		map.put(Boolean.class, (rs, column) -> rs.getBoolean(column));

		map.put(Character.class, (rs, column) -> rs.getString(column).charAt(0));
		
		map.put(String.class, (rs, column) -> rs.getString(column));

		map.put(byte[].class, (rs, column) -> rs.getBytes(column));

		map.put(Date.class, (rs, column) -> rs.getDate(column));
		map.put(Time.class, (rs, column) -> rs.getTime(column));
		map.put(Timestamp.class, (rs, column) -> rs.getTimestamp(column));
		
		return map;
	}
}
