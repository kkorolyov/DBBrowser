package dev.kkorolyov.sqlob.column;

import dev.kkorolyov.sqlob.ExecutionContext;
import dev.kkorolyov.sqlob.contributor.RecordStatementContributor;
import dev.kkorolyov.sqlob.contributor.ResultInstanceContributor;
import dev.kkorolyov.sqlob.result.Record;
import dev.kkorolyov.sqlob.type.SqlobType;
import dev.kkorolyov.sqlob.util.PersistenceHelper;
import dev.kkorolyov.sqlob.util.ReflectionHelper;

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
		getSqlobType().set(context.getMetadata(), statement, index, (T) ReflectionHelper.getValue(record.getObject(), f));
		return statement;
	}
	@Override
	public Object contribute(Object instance, ResultSet rs, ExecutionContext context) {
		return ReflectionHelper.setValue(instance, getField(), getValue(rs, context));
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
