package dev.kkorolyov.sqlob.statement;

import dev.kkorolyov.sqlob.construct.RowEntry;

/**
 * An executable and revertible {@code INSERT ROW} SQL statement.
 */
public class UpdateRowStatement extends UpdatingStatement {
	private String table;
	
	/**
	 * Constructs a new insert row statement.
	 * @param table table to update row in
	 * @param values new values to set
	 * @param criteria criteria to match
	 */
	public UpdateRowStatement(String table, RowEntry[] values, RowEntry[] criteria) {
		super(StatementBuilder.buildUpdate(table, values, criteria), values, criteria);
		this.table = table;
	}

	@Override
	public StatementCommand getReversionStatement() {
		return isRevertible() ? new UpdateRowStatement(table, getCritera(), getValues()) : null;
	}
}
