package dev.kkorolyov.sqlob.service;

import static dev.kkorolyov.sqlob.service.Constants.ID_TYPE;
import static dev.kkorolyov.sqlob.service.Constants.sanitize;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.sql.*;
import java.sql.Date;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import dev.kkorolyov.sqlob.annotation.Column;
import dev.kkorolyov.sqlob.annotation.Table;
import dev.kkorolyov.sqlob.annotation.Transient;
import dev.kkorolyov.sqlob.logging.Logger;
import dev.kkorolyov.sqlob.utility.Converter;
import dev.kkorolyov.sqlob.utility.Extractor;
import dev.kkorolyov.sqlob.persistence.NonPersistableException;

/**
 * Manages data mapping between Java and relational domains.
 */
public class Mapper {
	private static final Logger log = Logger.getLogger(Mapper.class.getName());

	private final Map<Class<?>, String> typeMap = new HashMap<>();
	private final Map<Class<?>, Converter> converterMap = new HashMap<>();
	private final Map<Class<?>, Extractor> extractorMap = new HashMap<>();

	private final Map<Class<?>, Iterable<Field>> persistableFields = new HashMap<>();	// Cache

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

		put(UUID.class, ID_TYPE, UUID::toString, (rs, column) -> UUID.fromString(rs.getString(column)));	// Store as string
	}

	/**
	 * Applies a mapping without extra conversion
	 * @param c Java class
	 * @param sqlType associated SQL type
	 * @param extractor function transforming a SQL column of type {@code sqlType} to an instance of {@code c}
	 */
	public <T> void put(Class<T> c, String sqlType, Extractor<T> extractor) {
		put(c, sqlType, null, extractor);
	}

	/**
	 * Applies a mapping.
	 * @param c Java class
	 * @param sqlType associated SQL type
	 * @param converter function transforming an instance of {@code c} to another type before persisting as {@code sqlType}
	 * @param extractor function transforming a SQL column of type {@code sqlType} to an instance of {@code c}
	 */
	public <T, R> void put(Class<T> c, String sqlType, Converter<T, R> converter, Extractor<T> extractor) {
		String sanitizedSqlType = sanitize(sqlType);

		typeMap.put(c, sanitizedSqlType);
		converterMap.put(c, converter);
		extractorMap.put(c, extractor);

		log.debug(() -> "Put mapping: " + c + "->" + sanitizedSqlType);
	}

	/** @return SQL type associated with {@code c}, or {@code null} if no associated SQL type */
	String sql(Class<?> c) {
		return typeMap.get(c);
	}
	/** @return SQL type associated with {@code f}'s type, or {@code null} if no associated SQL type */
	String sql(Field f) {
		return sql(f.getType());
	}

	/** @return converted representation of {@code o}, or {@code o} if no converter set */
	public Object convert(Object o) {
		Converter converter = converterMap.get(o.getClass());

		return (converter == null) ? o : converter.execute(o);
	}

	/** @return column {@code columnName} of result set {@code rs} as an instance of {@code c} */
	public <T> T extract(Class<T> c, ResultSet rs, String columnName) {
		try {
			return (T) extractorMap.get(c).execute(rs, columnName);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	/** @return column mapped to {@code f} of result set {@code rs} as an instance of {@code f}'s type */
	public Object extract(Field f, ResultSet rs) {
		return extract(f.getType(), rs, getName(f));
	}

	/** @return all persistable fields of a class with accessibility restrictions disabled */
	Iterable<Field> getPersistableFields(Class<?> c) {
		return persistableFields.computeIfAbsent(c, k -> StreamSupport.stream(Arrays.spliterator(k.getDeclaredFields()), true)
																																	.filter(this::isPersistable)
																																	.map(f -> {
																																		f.setAccessible(true);
																																		return f;
																																	})
																																	.collect(Collectors.toCollection(LinkedHashSet::new)));
	}
	private boolean isPersistable(Field f) {
		int modifiers = f.getModifiers();
		return !(Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers)) &&
					 f.getAnnotation(Transient.class) == null &&
					 !f.isSynthetic();
	}

	/** @return types of all persistable fields of a class */
	Iterable<Class<?>> getPersistableFieldTypes(Class<?> c) {
		return StreamSupport.stream(getPersistableFields(c).spliterator(), true)
												.map(Field::getType)
												.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	/** @return {@code c} and all persisted non-primitive classes used both directly and indirectly by {@code c} */
	Iterable<Class<?>> getAssociatedClasses(Class<?> c) {
		Deque<Class<?>> associateds = new ArrayDeque<>();	// Results ordered by dependency
		Deque<Class<?>> untouched = new ArrayDeque<>();	// DFS stack

		if (!isPrimitive(c)) untouched.push(c);

		while (!untouched.isEmpty()) {	// DFS until primitive classes reached or all classes seen
			associateds.push(untouched.pop());

			for (Class<?> associated : getPersistableFieldTypes(associateds.peek())) {
				if (isComplex(associated) && !associateds.contains(associated)) untouched.push(associated);
			}
		}
		return associateds;
	}

	/** @return {@code true} if a primitive SQL type is associated with {@code c} */
	boolean isPrimitive(Class<?> c) {
		return sql(c) != null;
	}
	/** @return {@code true} if a primitive SQL type is associated with {@code f}'s type */
	boolean isPrimitive(Field f) {
		return isPrimitive(f.getType());
	}

	/** @return {@code true} if no primitive SQL type is associated with {@code c} */
	boolean isComplex(Class<?> c) {
		return sql(c) == null;
	}
	/** @return {@code true} if no primitive SQL type is associated with {@code f}'s type */
	boolean isComplex(Field f) {
		return isComplex(f.getType());
	}

	/** @return equivalent relational table name for a Java class */
	String getName(Class<?> c) {
		Table override = c.getAnnotation(Table.class);
		if (override != null && override.value().length() <= 0) throw new NonPersistableException(c + " has a Table annotation with an empty name");

		return (override == null) ? c.getSimpleName() : override.value();
	}
	/** @return equivalent relational column name for a Java field */
	String getName(Field f) {
		Column override = f.getAnnotation(Column.class);
		if (override != null && override.value().length() <= 0) throw new NonPersistableException(f + " has a Column annotation with an empty name");

		return (override == null) ? f.getName() : override.value();
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
					 ", persistableFields=" + persistableFields +
					 '}';
	}
}
