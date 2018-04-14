package dev.kkorolyov.sqlob.column;

import dev.kkorolyov.sqlob.ExecutionContext;
import dev.kkorolyov.sqlob.type.SqlobType;
import dev.kkorolyov.sqlob.util.Where;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Performs operations involving a single SQL column.
 * @param <T> column value type
 */
public abstract class Column<T> {
	private final String name;
	private final SqlobType<T> sqlobType;

	/**
	 * Constructs a new column.
	 * @param name column name
	 * @param sqlobType column SQLOb type
	 */
	protected Column(String name, SqlobType<T> sqlobType) {
		this.name = name;
		this.sqlobType = sqlobType;
	}

	/**
	 * Contributes this column's resolved values in {@code where} to {@code statement}.
	 * @param statement statement to contribute to
	 * @param where where to resolve values from
	 * @param context context to work in
	 * @return {@code statement}
	 */
	public PreparedStatement contributeToStatement(PreparedStatement statement, Where where, ExecutionContext context) {
		where.consumeValues(name, (index, value) -> sqlobType.set(context.getMetadata(), statement, index, resolveCriterion(value, context)));
		return statement;
	}

	/**
	 * @param value {@link Where} criterion value to resolve
	 * @param context context to work in
	 * @return resolved form of {@code value} ready for persistence
	 */
	public T resolveCriterion(Object value, ExecutionContext context) {
		// Just let it throw a class-cast if occurs
		return (T) value;
	}
	/**
	 * @param rs result set to extract from
	 * @param context context to work in
	 * @return object value extracted from the associated column in {@code rs}
	 * @throws dev.kkorolyov.sqlob.util.UncheckedSqlException if a SQL issue occurs
	 */
	public T getValue(ResultSet rs, ExecutionContext context) {
		return sqlobType.get(context.getMetadata(), rs, name);
	}

	/**
	 * @param context context to work in
	 * @return SQL representation of this column within {@code context}
	 */
	public String getSql(ExecutionContext context) {
		return name + " " + sqlobType.getSqlType(context.getMetadata());
	}

	/** @return column name */
	public final String getName() {
		return name;
	}
	/** @return column SQLOb type */
	public final SqlobType<T> getSqlobType() {
		return sqlobType;
	}
}
