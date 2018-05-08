package dev.kkorolyov.sqlob.column;

import dev.kkorolyov.sqlob.ExecutionContext;
import dev.kkorolyov.sqlob.contributor.RecordStatementContributor;
import dev.kkorolyov.sqlob.contributor.ResultInstanceContributor;
import dev.kkorolyov.sqlob.result.Record;
import dev.kkorolyov.sqlob.type.SqlobType;
import dev.kkorolyov.sqlob.util.PersistenceHelper;

import java.lang.reflect.Field;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;

/**
 * A {@link Column} backed by a field on an object.
 * @param <T> column value type
 */
public class FieldBackedColumn<T> extends Column<T> implements RecordStatementContributor, ResultInstanceContributor {
	private final Field f;

	/**
	 * Constructs a new field-backed column.
	 * @param f associated field
	 * @param sqlobType column SQLOb type
	 */
	public FieldBackedColumn(Field f, SqlobType<T> sqlobType) {
		super(PersistenceHelper.getName(f), sqlobType);
		this.f = f;
	}

	@Override
	public <O> PreparedStatement contribute(PreparedStatement statement, Record<UUID, O> record, int index, ExecutionContext context) {
		getSqlobType().set(context.getMetadata(), statement, index, getValue(record.getObject(), context));
		return statement;
	}
	/**
	 * @param instance instance to extract from
	 * @param context context to work in
	 * @return field value extracted from the associated field in {@code instance}
	 * @throws IllegalArgumentException if an issue occurs extracting this column's field from {@code instance}
	 * @throws ClassCastException if this column's field type is not assignable to {@code T}
	 */
	protected T getValue(Object instance, ExecutionContext context) {
		try {
			f.setAccessible(true);
			return (T) f.get(instance);
		} catch (IllegalAccessException e) {
			throw new IllegalArgumentException("Unable to extract " + f + " value from " + instance, e);
		}
	}

	@Override
	public Object contribute(Object instance, ResultSet rs, ExecutionContext context) {
		try {
			f.setAccessible(true);
			f.set(instance, getValue(rs, context));

			return instance;
		} catch (IllegalAccessException e) {
			throw new IllegalArgumentException("Unable to contribute " + f + " to " + instance, e);
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
