package dev.kkorolyov.sqlob.statement;

import dev.kkorolyov.sqlob.construct.RowEntry;

/**
 * An executable and revertible {@code INSERT ROW} SQL statement.
 */
public class InsertRowStatement extends UpdatingStatement {
	private String table;	// TODO un-reduntify
	
	/**
	 * Constructs a new {@code INSERT ROW} statement.
	 * @param table table to insert row into
	 * @param values values to insert
	 */
	public InsertRowStatement(String table, RowEntry[] values) {
		super(StatementBuilder.buildInsert(table, values), values, null);
		this.table = table;
	}

	@Override
	public StatementCommand getReversionStatement() {
		return isRevertible() ? new DeleteRowStatement(table, getValues()) : null;
	}
}
