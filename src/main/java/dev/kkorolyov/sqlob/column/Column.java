package dev.kkorolyov.sqlob.column;

import dev.kkorolyov.sqlob.ExecutionContext;
import dev.kkorolyov.sqlob.result.ConfigurableRecord;
import dev.kkorolyov.sqlob.result.Record;
import dev.kkorolyov.sqlob.struct.Table;
import dev.kkorolyov.sqlob.type.SqlobType;

import java.sql.ResultSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.UUID;

/**
 * Performs operations involving a single SQL column.
 * @param <T> column value type
 */
public abstract class Column<T> {
	// TODO Use struct.Column
	private final String name;
	private final SqlobType<T> sqlobType;

	/**
	 * Constructs a new column.
	 * @param name column name
	 * @param sqlobType column SQLOb type
	 */
	public Column(String name, SqlobType<T> sqlobType) {
		this.name = name;
		this.sqlobType = sqlobType;
	}

	/**
	 * @param value value to resolve
	 * @param context context to work in
	 * @return resolved representation of {@code value} in the current context
	 */
	public Object resolve(Object value, ExecutionContext context) {
		return getSqlobType().get(context.getMetadata(), (T) value);
	}

	/**
	 * @param record record to get associated value from
	 * @param context context to work in
	 * @return value in {@code record} associated with this column represented in the current context
	 * @throws dev.kkorolyov.sqlob.util.UncheckedSqlException if a SQL issue occurs
	 */
	public abstract Object get(Record<UUID, ?> record, ExecutionContext context);
	/**
	 * Sets this column's associated value in a result set on a record.
	 * @param record record to update
	 * @param rs result set to get value from
	 * @param context context to work in
	 * @return {@code record}
	 */
	public abstract <O> ConfigurableRecord<UUID, O> set(ConfigurableRecord<UUID, O> record, ResultSet rs, ExecutionContext context);

	/**
	 * @param rs result set to extract from
	 * @param context context to work in
	 * @return object value extracted from the associated column in {@code rs}
	 * @throws dev.kkorolyov.sqlob.util.UncheckedSqlException if a SQL issue occurs
	 */
	public T get(ResultSet rs, ExecutionContext context) {
		return getSqlobType().get(context.getMetadata(), rs, getName());
	}

	/**
	 * @param context context to work in
	 * @return SQL representation of this column within {@code context}
	 */
	public String getSql(ExecutionContext context) {
		return getName() + " " + getSqlobType().getSqlType(context.getMetadata());
	}

	/**
	 * @param context context to work in
	 * @return all prerequisite tables within {@code context} which must exist before a table with this column may
	 */
	public Collection<Table> getPrerequisites(ExecutionContext context) {
		return Collections.emptySet();
	}

	/** @return column name */
	public final String getName() {
		return name;
	}
	/** @return column SQLOb type */
	public final SqlobType<T> getSqlobType() {
		return sqlobType;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		Column<?> column = (Column<?>) o;

		return Objects.equals(name, column.name) &&
				Objects.equals(sqlobType, column.sqlobType);
	}
	@Override
	public int hashCode() {
		return Objects.hash(name, sqlobType);
	}
}
