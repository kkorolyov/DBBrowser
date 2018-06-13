package dev.kkorolyov.sqlob.statement;

import dev.kkorolyov.sqlob.util.Where;

import java.sql.PreparedStatement;
import java.util.Collection;
import java.util.HashSet;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Builds {@code SELECT} statements.
 */
public class SelectStatementBuilder implements StatementBuilder<PreparedStatement> {
	private final Function<String, PreparedStatement> statementSupplier;
	private final String table;
	private final Collection<String> columns;
	private final Where where;

	/**
	 * Constructs a new {@code SELECT} statement builder.
	 * @param statementSupplier provides a prepared statement from a SQL string supplied to it
	 * @param table name of table to select from
	 * @param columns names of columns to select values from
	 * @param where WHERE clause limiting selection
	 */
	public SelectStatementBuilder(Function<String, PreparedStatement> statementSupplier, String table, Collection<String> columns, Where where) {
		this.statementSupplier = statementSupplier;
		this.table = table;
		this.columns = new HashSet<>(columns);
		this.where = where;
	}

	@Override
	public PreparedStatement build() {
		PreparedStatement statement = statementSupplier.apply(
				columns.stream()
						.collect(Collectors.joining(
								",",
								"SELECT ",
								" FROM " + table + " WHERE " + where.getSql()
						))
		);
		where.forEach((i, value) -> statement.setObject(i + 1, value));

		return statement;
	}
}
