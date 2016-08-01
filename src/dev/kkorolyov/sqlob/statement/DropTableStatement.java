package dev.kkorolyov.sqlob.statement;

import dev.kkorolyov.sqlob.connection.DatabaseConnection;

/**
 * An executable and revertible {@code DROP TABLE} SQL statement.
 */
public class DropTableStatement extends UpdatingStatement {
	private String table;
	
	/**
	 * Constructs a new {@code DROP TABLE} statement.
	 * @param conn database connection used for statement execution
	 * @param table table to drop
	 */
	public DropTableStatement(DatabaseConnection conn, String table) {
		super(conn, StatementBuilder.buildDrop(table), null, null);
	}

	@Override
	public StatementCommand getReversionStatement() {
		return null;	// TODO
	}
}
