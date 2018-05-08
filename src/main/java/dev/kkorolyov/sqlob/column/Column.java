package dev.kkorolyov.sqlob.column;

import dev.kkorolyov.sqlob.ExecutionContext;
import dev.kkorolyov.sqlob.contributor.WhereStatementContributor;
import dev.kkorolyov.sqlob.type.SqlobType;
import dev.kkorolyov.sqlob.util.Where;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Objects;

/**
 * Performs operations involving a single SQL column.
 * @param <T> column value type
 */
public class Column<T> implements WhereStatementContributor {
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

	@Override
	public PreparedStatement contribute(PreparedStatement statement, Where where, ExecutionContext context) {
		where.consumeValues(name, (index, value) -> sqlobType.set(context.getMetadata(), statement, index, resolveCriterion(value, context)));
		return statement;
	}
	/**
	 * @param value {@link Where} criterion value to resolve
	 * @param context context to work in
	 * @return resolved form of {@code value} ready for persistence
	 */
	protected T resolveCriterion(Object value, ExecutionContext context) {
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
