package dev.kkorolyov.sqlob.statement;

import dev.kkorolyov.sqlob.connection.DatabaseConnection;
import dev.kkorolyov.sqlob.construct.RowEntry;

/**
 * An executable and revertible {@code INSERT ROW} SQL statement.
 */
public class UpdateRowStatement extends UpdatingStatement {
	private String table;
	
	/**
	 * Constructs a new insert row statement.
	 * @param conn database connection used for statement execution
	 * @param table table to update row in
	 * @param values new values to set
	 * @param criteria criteria to match
	 */
	public UpdateRowStatement(DatabaseConnection conn, String table, RowEntry[] values, RowEntry[] criteria) {
		super(conn, StatementBuilder.buildUpdate(table, values, criteria), values, criteria);
		this.table = table;
	}

	@Override
	public StatementCommand getReversionStatement() {
		return isRevertible() ? new UpdateRowStatement(getConn(), table, getCritera(), getValues()) : null;
	}
}
