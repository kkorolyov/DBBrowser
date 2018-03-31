package dev.kkorolyov.sqlob.column;

import dev.kkorolyov.sqlob.ExecutionContext;
import dev.kkorolyov.sqlob.util.Where;

import java.sql.ResultSet;

/**
 * Performs operations involving a single SQL column.
 * @param <T> column value type
 */
public abstract class Column<T> {
	private final String name;
	private final String sqlType;

	/**
	 * Constructs a new column.
	 * @param name column name
	 * @param sqlType associated SQL type
	 */
	protected Column(String name, String sqlType) {
		this.name = name;
		this.sqlType = sqlType;
	}

	/**
	 * Resolves this column's values in {@code where}.
	 * @param where where to resolve in
	 * @param context context to work in
	 * @return {@code where}
	 */
	public abstract Where contributeToWhere(Where where, ExecutionContext context);

	/**
	 * @param rs result set to extract from
	 * @param context context to work in
	 * @return object value extracted from the associated column in {@code rs}
	 * @throws dev.kkorolyov.sqlob.util.UncheckedSqlException if a SQL issue occurs
	 */
	public abstract T getValue(ResultSet rs, ExecutionContext context);

	/** @return SQL representation of this column */
	public String getSql() {
		return getName() + " " + getSqlType();
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
