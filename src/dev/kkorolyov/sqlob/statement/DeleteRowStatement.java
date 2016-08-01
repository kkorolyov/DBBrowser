package dev.kkorolyov.sqlob.statement;

import dev.kkorolyov.sqlob.connection.DatabaseConnection;
import dev.kkorolyov.sqlob.construct.RowEntry;

/**
 * An executable and revertible {@code DELETE ROW} SQL statement.
 */
public class DeleteRowStatement extends UpdatingStatement {
	private String table;
	
	/**
	 * Constructs a new {@code DELETE ROW} statement.
	 * @param conn database connection used for statement execution
	 * @param table table to delete row(s) from
	 * @param criteria criteria used by row deletion
	 */
	public DeleteRowStatement(DatabaseConnection conn, String table, RowEntry[] criteria) {
		super(conn, StatementBuilder.buildDelete(table, criteria), null, criteria);
		this.table = table;
	}

	@Override
	public StatementCommand getReversionStatement() {
		return isRevertible() ? new InsertRowStatement(getConn(), table, getCritera()) : null;
	}
}
