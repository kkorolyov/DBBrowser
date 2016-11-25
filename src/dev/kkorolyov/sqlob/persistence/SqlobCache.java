package dev.kkorolyov.sqlob.persistence;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import dev.kkorolyov.sqlob.annotation.Transient;

class SqlobCache {
	private final String 	idName,
												idType;
	private final Map<Class<?>, SqlobClass<?>> classMap = new HashMap<>();
	private Map<Class<?>, String> typeMap = getDefaultTypeMap();
	
	SqlobCache() {
		this("uuid", "CHAR(36)");
	}
	SqlobCache(String idName, String idType) {
		this.idName = idName;
		this.idType = idType;
	}
	
	<T> SqlobClass<T> get(Class<T> type, Connection conn) throws SQLException {
		@SuppressWarnings("unchecked")
		SqlobClass<T> result = (SqlobClass<T>) classMap.get(type);
		
		if (result == null) {
			result = new SqlobClass<>(type, buildFields(type, conn), idName, idType).init(conn);	// Create + init
			classMap.put(type, result);
		}
		return result;
	}
	private <T> List<SqlobField> buildFields(Class<T> type, Connection conn) throws SQLException {
		List<SqlobField> fields = new LinkedList<>();
		
		for (Field field : type.getDeclaredFields()) {
			if (field.getAnnotation(Transient.class) == null) {
				Class<?> fieldType = field.getType();
				String sqlType = typeMap.get(fieldType);
				SqlobClass<?> reference = (sqlType == null ? get(fieldType, conn) : null);
				
				field.setAccessible(true);
				
				fields.add(new SqlobField(field, (sqlType == null ? idType : sqlType), reference));
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
		
		String shortSql = "SMALLINT";
		map.put(Short.class, shortSql);
		map.put(Short.TYPE, shortSql);
		
		String intSql = "INTEGER";
		map.put(Integer.class, intSql);
		map.put(Integer.TYPE, intSql);
		
		String longSql = "BIGINT";
		map.put(Long.class, longSql);
		map.put(Long.TYPE, longSql);
		
		String doubleSql = "DOUBLE PRECISION";
		map.put(Float.class, doubleSql);
		map.put(Float.TYPE, doubleSql);
		map.put(Double.class, doubleSql);
		map.put(Double.TYPE, doubleSql);
		
		String bigDecimalSql = "DECIMAL";
		map.put(BigDecimal.class, bigDecimalSql);
		
		String booleanSql = "BIT";
		map.put(Boolean.class, booleanSql);
		map.put(Boolean.TYPE, booleanSql);
		
		String charSql = "CHAR(1)";
		map.put(Character.class, charSql);
		map.put(Character.TYPE, charSql);
		
		String stringSql = "VARCHAR(1024)";
		map.put(String.class, stringSql);
		
		return map;
	}
}
