package dev.kkorolyov.sqlob.construct;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import dev.kkorolyov.sqlob.annotation.Transient;

/**
 * Provides appropriate {@code SqlobClass} objects for {@code Class} objects.
 */
public class SqlobClassFactory {
	private final Map<Class<?>, SqlobClass<?>> classMap = new HashMap<>();
	private Map<Class<?>, String> typeMap = getDefaultTypeMap();
	private final String referenceType;
	
	/**
	 * Constructs a new factory with a specified reference SQL type.
	 * @param referenceType SQL type for a reference
	 */
	public SqlobClassFactory(String referenceType) {
		this.referenceType = referenceType;
	}
	
	/**
	 * Returns the appropriate {@code SqlobClass} object for a {@code Class} object.
	 * @param type class to wrap in SqlobClass
	 * @return appropriate SqlobClass
	 */
	@SuppressWarnings("unchecked")
	public <T> SqlobClass<T> get(Class<T> type) {
		SqlobClass<T> result = (SqlobClass<T>) classMap.get(type);
		
		if (result == null) {
			result = new SqlobClass<>(type, buildFields(type));
			classMap.put(type, result);
		}
		return result;
	}
	private <T> List<SqlobField> buildFields(Class<T> type) {
		List<SqlobField> fields = new LinkedList<>();
		
		for (Field field : type.getDeclaredFields()) {
			if (field.getAnnotation(Transient.class) == null) {
				Class<?> fieldType = field.getType();
				String sqlType = typeMap.get(fieldType);
				SqlobClass<?> reference = (sqlType == null ? get(fieldType) : null);
				
				fields.add(new SqlobField(field, (sqlType == null ? referenceType : sqlType), reference));
			}
		}
		return fields;
	}
	
	/** @return type map used by this factory for mapping Java classes to SQL types */
	public Map<Class<?>, String> getTypeMap() {
		return new HashMap<>(typeMap);
	}
	/** @param typeMap new Java-to-SQL type map, if {@code null}, resets to default type map */
	public void setTypeMap(Map<Class<?>, String> typeMap) {
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
