package dev.kkorolyov.sqlob.service;

import static dev.kkorolyov.sqlob.service.Constants.ID_TYPE;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import dev.kkorolyov.sqlob.annotation.Transient;
import dev.kkorolyov.sqlob.persistence.Extractor;

/**
 * Provides for data mapping between Java and SQL.
 */
public final class Mapper {
	private final Map<Class<?>, String> typeMap = new HashMap<>();
	private final Map<Class<?>, Extractor> extractorMap = new HashMap<>();

	/**
	 * Constructs a new mapper with default mappings.
	 */
	public Mapper() {
		put(Byte.class, "TINYINT", ResultSet::getByte);
		put(Short.class, "SMALLINT", ResultSet::getShort);
		put(Integer.class, "INTEGER", ResultSet::getInt);
		put(Long.class, "BIGINT", ResultSet::getLong);
		put(Float.class, "REAL", ResultSet::getFloat);
		put(Double.class, "DOUBLE PRECISION", ResultSet::getDouble);
		put(BigDecimal.class, "NUMERIC", ResultSet::getBigDecimal);

		put(Boolean.class, "BOOLEAN", ResultSet::getBoolean);

		put(Character.class, "CHAR(1)", (rs, column) -> {
			String string = rs.getString(column);
			return string == null ? null : string.charAt(0);
		});
		put(String.class, "VARCHAR(1024)", ResultSet::getString);

		put(byte[].class, "VARBINARY(1024)", ResultSet::getBytes);

		put(Date.class, "DATE", ResultSet::getDate);
		put(Time.class, "TIME(6)", ResultSet::getTime);
		put(Timestamp.class, "TIMESTAMP(6)", ResultSet::getTimestamp);

		put(UUID.class, ID_TYPE, (rs, column) -> UUID.fromString(rs.getString(column)));
	}

	/**
	 * Creates a new mapping, replacing any old mapping.
	 * @param c Java class
	 * @param sqlType associated SQL type
	 * @param extractor function transforming a SQL column of type {@code sqlType} to an instance of {@code c}
	 */
	public <T> void put(Class<T> c, String sqlType, Extractor<T> extractor) {
		typeMap.put(c, sqlType);
		extractorMap.put(c, extractor);
	}

	Iterable<Field> getPersistableFields(Class<?> c) {
		return StreamSupport.stream(Arrays.spliterator(c.getFields()), true)
												.filter(Mapper::isPersistable).collect(Collectors.toSet());
	}
	private static boolean isPersistable(Field f) {
		int modifiers = f.getModifiers();
		return !(Modifier.isStatic(modifiers) ||
						 Modifier.isTransient(modifiers)) &&
					 f.getAnnotation(Transient.class) == null;
	}

	Iterable<Class<?>> getDependencies(Class<?> c) {
		return StreamSupport.stream(getPersistableFields(c).spliterator(), true)
				.filter(field -> !isPrimitive(field))
				.map(Field::getClass)
				.collect(Collectors.toSet());
	}

	/** @return {@code true} if a primitive SQL type is associated with {@code f}'s class */
	boolean isPrimitive(Field f) {
		return getSql(f) == null;
	}

	/** @return SQL type associated with {@code f}'s class */
	String getSql(Field f) {
		return typeMap.get(f.getClass());
	}
	/** @return extractor associated with {@code f}'s class */
	Extractor<?> getExtractor(Field f) {
		return extractorMap.get(f.getClass());
	}

}
