package dev.kkorolyov.sqlob.service;

import static dev.kkorolyov.sqlob.service.Constants.ID_TYPE;
import static dev.kkorolyov.sqlob.service.Constants.sanitize;

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
import dev.kkorolyov.sqlob.logging.Logger;
import dev.kkorolyov.sqlob.persistence.Extractor;

/**
 * Provides for data mapping between Java and SQL.
 */
public final class Mapper {
	private static final Logger log = Logger.getLogger(Mapper.class.getName());

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
		put(Double.class, "DOUBLE", ResultSet::getDouble);
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
		String sanitizedSqlType = sanitize(sqlType);

		typeMap.put(c, sanitizedSqlType);
		extractorMap.put(c, extractor);

		log.debug(() -> "Put mapping: " + c + "->" + sanitizedSqlType);
	}

	static Iterable<Class<?>> getPersistableClasses(Class<?> c) {
		return StreamSupport.stream(getPersistableFields(c).spliterator(), true)
												.map(Field::getType)
												.collect(Collectors.toSet());
	}
	static Iterable<Field> getPersistableFields(Class<?> c) {
		return StreamSupport.stream(Arrays.spliterator(c.getDeclaredFields()), true)
												.filter(Mapper::isPersistable)
												.collect(Collectors.toSet());
	}
	private static boolean isPersistable(Field f) {
		int modifiers = f.getModifiers();
		return !(Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers)) &&
					 f.getAnnotation(Transient.class) == null &&
					 !f.isSynthetic();
	}

	/** @return {@code c} and all persisted non-primitive classes used both directly and indirectly by {@code c} */
	Iterable<Class<?>> getAssociatedClasses(Class<?> c) {
		Deque<Class<?>> associateds = new ArrayDeque<>();	// Results ordered by dependency
		Deque<Class<?>> untouched = new ArrayDeque<>();	// DFS stack

		if (!isPrimitive(c)) untouched.push(c);

		while (!untouched.isEmpty()) {	// DFS until primitive classes reached or all classes seen
			associateds.push(untouched.pop());

			for (Class<?> associated : getPersistableClasses(associateds.peek())) {
				if (!isPrimitive(associated) && !associateds.contains(associated)) untouched.push(associated);
			}
		}
		return associateds;
	}

	/** @return {@code true} if a primitive SQL type is associated with {@code c} */
	boolean isPrimitive(Class<?> c) {
		return getSql(c) != null;
	}
	/** @return {@code true} if a primitive SQL type is associated with {@code f}'s class */
	boolean isPrimitive(Field f) {
		return isPrimitive(f.getClass());
	}

	/** @return SQL type associated with {@code c} */
	String getSql(Class<?> c) {
		return typeMap.get(c);
	}
	/** @return SQL type associated with {@code f}'s class */
	String getSql(Field f) {
		return getSql(f.getClass());
	}

	/** @return extractor associated with {@code c} */
	Extractor getExtractor(Class<?> c) {
		return extractorMap.get(c);
	}
	/** @return extractor associated with {@code f}'s class */
	Extractor<?> getExtractor(Field f) {
		return getExtractor(f.getClass());
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		Mapper mapper = (Mapper) o;
		return Objects.equals(typeMap, mapper.typeMap) &&
					 Objects.equals(extractorMap, mapper.extractorMap);
	}

	@Override
	public int hashCode() {
		return Objects.hash(typeMap, extractorMap);
	}

	@Override
	public String toString() {
		return "Mapper{" +
					 "typeMap=" + typeMap +
					 ", extractorMap=" + extractorMap +
					 '}';
	}
}
