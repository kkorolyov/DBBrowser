package dev.kkorolyov.sqlob.statement;

import dev.kkorolyov.sqlob.construct.Column;
import dev.kkorolyov.sqlob.construct.RowEntry;

/**
 * An executable {@code SELECT} SQL statement.
 */
public class SelectStatement extends ResultingStatement {
	/**
	 * Constructs a new {@code SELECT} statement.
	 * @param table table to select from
	 * @param columns columns to select
	 * @param criteria selection criteria, if {@code null} or empty, no criteria is used
	 */
	public SelectStatement(String table, Column[] columns, RowEntry[] criteria) {
		super(StatementBuilder.buildSelect(table, columns, criteria), null, criteria);
	}

	@Override
	public StatementCommand getReversionStatement() {
		return null;
	}
}
