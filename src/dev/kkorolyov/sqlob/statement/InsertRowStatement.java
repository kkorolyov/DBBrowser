package dev.kkorolyov.sqlob.statement;

import dev.kkorolyov.sqlob.connection.DatabaseConnection;
import dev.kkorolyov.sqlob.construct.RowEntry;

/**
 * An executable and revertible {@code INSERT ROW} SQL statement.
 */
public class InsertRowStatement extends UpdatingStatement {
	private String table;	// TODO un-reduntify
	
	/**
	 * Constructs a new {@code INSERT ROW} statement.
	 * @param conn database connection used for statement execution
	 * @param table table to insert row into
	 * @param values values to insert
	 */
	public InsertRowStatement(DatabaseConnection conn, String table, RowEntry[] values) {
		super(conn, StatementBuilder.buildInsert(table, values), values, null);
		this.table = table;
	}

	@Override
	public StatementCommand getReversionStatement() {
		return isRevertible() ? new DeleteRowStatement(getConn(), table, getValues()) : null;
	}
}
