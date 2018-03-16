package dev.kkorolyov.sqlob.column;

import dev.kkorolyov.simplefuncs.throwing.ThrowingBiFunction;
import dev.kkorolyov.sqlob.request.ExecutionContext;
import dev.kkorolyov.sqlob.service.PersistenceHelper;
import dev.kkorolyov.sqlob.util.UncheckedSqlException;
import dev.kkorolyov.sqlob.util.Where;

import java.lang.reflect.Field;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.function.BiFunction;

import static dev.kkorolyov.sqlob.util.UncheckedSqlException.wrapSqlException;

/**
 * A single persistable field of a class.
 * @param <T> type retrieved from {@link ResultSet}
 */
public class Column<T> {
	/**
	 * Field-agnostic column used for ID.
	 */
	public static final Column<UUID> ID_COLUMN = new Column<>(
			"id",
			"CHAR(16)",
			(rs, column) -> {
				String idString = rs.getString(column);
				return idString != null ? UUID.fromString(idString) : null;
			}
	);

	private final Field f;
	private final String name;
	private final String sqlType;
	private final BiFunction<ResultSet, String, T> extractor;

	/**
	 * Constructs a new column not associated with an individual field.
	 * @param name column name
	 * @param sqlType associated SQL type
	 * @param extractor function extracting this column's SQL value from a result set and converting it into a Java value
	 */
	public Column(String name, String sqlType, ThrowingBiFunction<ResultSet, String, T, SQLException> extractor) {
		this(null, name, sqlType, extractor);
	}
	/**
	 * Constructs a new column.
	 * @param f associated field
	 * @param sqlType associated SQL type
	 * @param extractor function extracting this column's SQL value from a result set and converting it into a Java value
	 */
	public Column(Field f, String sqlType, ThrowingBiFunction<ResultSet, String, T, SQLException> extractor) {
		this(f, PersistenceHelper.getName(f), sqlType, extractor);
	}
	private Column(Field f, String name, String sqlType, ThrowingBiFunction<ResultSet, String, T, SQLException> extractor) {
		this.f = f;
		this.name = name;
		this.sqlType = sqlType;
		this.extractor = extractor;
	}

	/**
	 * Contribute's this column's value in an instance to a statement.
	 * @param statement statement to contribute to
	 * @param instance instance to retrieve value from
	 * @param index statement index to contribute to
	 * @param context context to work in
	 * @return {@code statement}
	 * @throws UncheckedSqlException if a SQL issue occurs
	 */
	public PreparedStatement contributeToStatement(PreparedStatement statement, Object instance, int index, ExecutionContext context) {
		wrapSqlException(() -> statement.setObject(index, getValue(instance, context)));
		return statement;
	}
	/**
	 * Contributes this column's value to its associated field on an instance.
	 * @param instance instance to contribute to
	 * @param rs result set to retrieve value from
	 * @param context context to work in
	 * @return {@code instance}
	 * @throws IllegalArgumentException if an issue occurs contributing this column's field to {@code instance}
	 */
	public Object contributeToInstance(Object instance, ResultSet rs, ExecutionContext context) {
		try {
			f.setAccessible(true);
			f.set(instance, getValue(rs, context));

			return instance;
		} catch (IllegalAccessException e) {
			throw new IllegalArgumentException("Unable to contribute " + f + " to " + instance, e);
		}
	}

	/**
	 * Resolves this column's values in {@code where}.
	 * @param where where to resolve in
	 * @param context context to work in
	 * @return {@code where}
	 */
	public Where contributeToWhere(Where where, ExecutionContext context) {
		where.setResolvedValue(name, value -> value);
		return where;
	}

	/**
	 * @param instance instance to extract from
	 * @param context context to work in
	 * @return field value extracted from the associated field in {@code instance}
	 * @throws IllegalArgumentException in an issue occurs extracting this column's field from {@code instance}
	 */
	public Object getValue(Object instance, ExecutionContext context) {
		try {
			f.setAccessible(true);
			return f.get(instance);
		} catch (IllegalAccessException e) {
			throw new IllegalArgumentException("Unable to extract " + f + " value from " + instance, e);
		}
	}
	/**
	 * @param rs result set to extract from
	 * @param context context to work in
	 * @return object value extracted from the associated column in {@code rs}
	 */
	public T getValue(ResultSet rs, ExecutionContext context) {
		return extractor.apply(rs, name);
	}

	/** @return associated field */
	public Field getField() {
		return f;
	}
	/** @return associated field's type */
	public Class<?> getType() {
		return f.getType();
	}

	/** @return column name */
	public String getName() {
		return name;
	}

	/** @return associated SQL type */
	public String getSqlType() {
		return sqlType;
	}
}
