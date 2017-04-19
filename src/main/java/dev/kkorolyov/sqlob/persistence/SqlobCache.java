package dev.kkorolyov.sqlob.persistence;

import static dev.kkorolyov.sqlob.service.Constants.ID_CLASS;
import static dev.kkorolyov.sqlob.service.Constants.ID_TYPE;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.sql.*;
import java.sql.Date;
import java.util.*;

import dev.kkorolyov.sqlob.annotation.Transient;
import dev.kkorolyov.sqlob.logging.Logger;

/**
 * Operates on a dynamically-cached ({@link Class} -> {@link SqlobClass}) map.
 */
final class SqlobCache {
	private static final Logger log = Logger.getLogger(SqlobCache.class.getName());

	private final Map<Class<?>, SqlobClass<?>> classMap = new HashMap<>();
	private final Map<Class<?>, String> typeMap = new HashMap<>();
	private final Map<Class<?>, Extractor> extractorMap = new HashMap<>();

	{
		initTypeMap();
		initExtractorMap();
	}
	private void initTypeMap() {
		typeMap.put(Byte.class, "TINYINT");
		typeMap.put(Short.class, "SMALLINT");
		typeMap.put(Integer.class, "INTEGER");
		typeMap.put(Long.class, "BIGINT");
		typeMap.put(Float.class, "REAL");
		typeMap.put(Double.class, "DOUBLE PRECISION");
		typeMap.put(BigDecimal.class, "NUMERIC");

		typeMap.put(Boolean.class, "BOOLEAN");

		typeMap.put(Character.class, "CHAR(1)");

		typeMap.put(String.class, "VARCHAR(1024)");

		typeMap.put(byte[].class, "VARBINARY(1024)");

		typeMap.put(Date.class, "DATE");
		typeMap.put(Time.class, "TIME(6)");
		typeMap.put(Timestamp.class, "TIMESTAMP(6)");

		typeMap.put(ID_CLASS, ID_TYPE);
	}
	private void initExtractorMap() {
		extractorMap.put(Byte.class, ResultSet::getByte);
		extractorMap.put(Short.class, ResultSet::getShort);
		extractorMap.put(Integer.class, ResultSet::getInt);
		extractorMap.put(Long.class, ResultSet::getLong);
		extractorMap.put(Float.class, ResultSet::getFloat);
		extractorMap.put(Double.class, ResultSet::getDouble);
		extractorMap.put(BigDecimal.class, ResultSet::getBigDecimal);

		extractorMap.put(Boolean.class, ResultSet::getBoolean);

		extractorMap.put(Character.class, (rs, column) -> {
			String string = rs.getString(column);
			return string == null ? null : string.charAt(0);
		});
		extractorMap.put(String.class, ResultSet::getString);

		extractorMap.put(byte[].class, ResultSet::getBytes);

		extractorMap.put(Date.class, ResultSet::getDate);
		extractorMap.put(Time.class, ResultSet::getTime);
		extractorMap.put(Timestamp.class, ResultSet::getTimestamp);

		extractorMap.put(ID_CLASS, ResultSet::getString);
	}

	<T> SqlobClass<T> get(Class<T> type, Connection conn) throws SQLException {
		@SuppressWarnings("unchecked")
		SqlobClass<T> result = (SqlobClass<T>) classMap.get(type);

		if (result == null) {
			Statement createStatement = conn.createStatement();	// TODO Cache this thing
			result = new SqlobClass<>(type, buildFields(type, conn), createStatement);
			createStatement.close();
			classMap.put(type, result);

			log.info(() -> "Cached (Class -> SqlobClass) mapping for " + type);
		}
		return result;
	}
	private <T> Iterable<SqlobField> buildFields(Class<T> type, Connection conn) throws SQLException {
		List<SqlobField> fields = new ArrayList<>();
		Iterable<Field> persistableFields = Arrays.stream(type.getDeclaredFields())
																							.filter(SqlobCache::isPersistable)::iterator;
		
		for (Field field : persistableFields) {
			Class<?> fieldType = wrap(field.getType());
			String sqlType = typeMap.get(fieldType);
			SqlobClass<?> reference = null;

			if (sqlType == null) {	// Not a primitive SQL type
				sqlType = ID_TYPE;
				reference = get(fieldType, conn);

				log.info(() -> "Retrieved SqlobClass for referenced class " + fieldType);
			}
			fields.add(new SqlobField(field, extractorMap.get(fieldType), sqlType, reference));
		}
		return fields;
	}

	private static boolean isPersistable(Field field) {
		int modifiers = field.getModifiers();

		return !Modifier.isStatic(modifiers) &&
					 !Modifier.isTransient(modifiers) &&
					 field.getAnnotation(Transient.class) == null;
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

	void mapType(Class<?> c, String sql) {
		typeMap.put(c, sql);
	}

	void mapExtractor(Class<?> c, Extractor extractor) {
		extractorMap.put(c, extractor);
	}
}
