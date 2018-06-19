package dev.kkorolyov.sqlob.statement;

import dev.kkorolyov.sqlob.util.Where;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.function.Function;

/**
 * Builds {@code DELETE} statements.
 */
public class DeleteStatementBuilder implements StatementBuilder<PreparedStatement> {
	private final Function<String, PreparedStatement> statementSupplier;
	private final String table;
	private final Where where;

	/**
	 * Constructs a new {@code DELETE} statement builder.
	 * @param statementSupplier provides a prepared statement from a SQL string supplied to it
	 * @param table name table to delete from
	 * @param where WHERE clause limiting deletion
	 */
	public DeleteStatementBuilder(Function<String, PreparedStatement> statementSupplier, String table, Where where) {
		this.statementSupplier = statementSupplier;
		this.table = table;
		this.where = where;
	}

	@Override
	public PreparedStatement build() throws SQLException {
		PreparedStatement statement = statementSupplier.apply("DELETE FROM " + table + " WHERE " + where.getSql());

		where.forEach((i, value) -> statement.setObject(i + 1, value));

		return statement;
	}
}
