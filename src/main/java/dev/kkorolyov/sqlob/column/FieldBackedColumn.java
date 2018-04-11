package dev.kkorolyov.sqlob.column;

import dev.kkorolyov.sqlob.ExecutionContext;
import dev.kkorolyov.sqlob.type.SqlobType;
import dev.kkorolyov.sqlob.util.PersistenceHelper;

import java.lang.reflect.Field;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * A {@link Column} backed by a field on an object.
 * @param <T> column value type
 */
public abstract class FieldBackedColumn<T> extends Column<T> {
	private final Field f;

	/**
	 * Constructs a new field-backed column.
	 * @param f associated field
	 * @param sqlobType column SQLOb type
	 */
	protected FieldBackedColumn(Field f, SqlobType<? super T> sqlobType) {
		super(PersistenceHelper.getName(f), sqlobType);
		this.f = f;
	}

	/**
	 * Contribute's this column's value in an instance to a statement.
	 * @param statement statement to contribute to
	 * @param instance instance to retrieve value from
	 * @param index statement index to contribute to
	 * @param context context to work in
	 * @return {@code statement}
	 * @throws dev.kkorolyov.sqlob.util.UncheckedSqlException if a SQL issue occurs
	 * @throws IllegalArgumentException in an issue occurs extracting this column's field from {@code instance}
	 */
	public PreparedStatement contributeToStatement(PreparedStatement statement, Object instance, int index, ExecutionContext context) {
		getSqlobType().set(context.getMetadata(), statement, index, getValue(instance, context));
		return statement;
	}
	/**
	 * Contributes this column's value to its associated field on an instance.
	 * @param instance instance to contribute to
	 * @param rs result set to retrieve value from
	 * @param context context to work in
	 * @return {@code instance}
	 * @throws dev.kkorolyov.sqlob.util.UncheckedSqlException if a SQL issue occurs
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
	 * @param instance instance to extract from
	 * @param context context to work in
	 * @return field value extracted from the associated field in {@code instance}
	 * @throws IllegalArgumentException in an issue occurs extracting this column's field from {@code instance}
	 */
	public T getValue(Object instance, ExecutionContext context) {
		try {
			f.setAccessible(true);
			return (T) f.get(instance);
		} catch (IllegalAccessException e) {
			throw new IllegalArgumentException("Unable to extract " + f + " value from " + instance, e);
		}
	}

	/** @return associated field */
	public final Field getField() {
		return f;
	}
	/** @return associated field's type */
	public final Class<?> getType() {
		return f.getType();
	}
}
