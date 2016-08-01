package dev.kkorolyov.sqlob.statement;

import dev.kkorolyov.sqlob.connection.DatabaseConnection;
import dev.kkorolyov.sqlob.construct.Column;

/**
 * An executable and revertible {@code CREATE TABLE} SQL statement.
 */
public class CreateTableStatement extends UpdatingStatement {
	private String table;
	
	/**
	 * Constructs a new {@code CREATE TABLE} statement.
	 * @param conn database connection used for statement execution
	 * @param table new table name
	 * @param columns new table columns
	 */
	public CreateTableStatement(DatabaseConnection conn, String table, Column[] columns) {
		super(conn, StatementBuilder.buildCreate(table, columns), null, null);
	}
	
	@Override
	public StatementCommand getReversionStatement() {
		return new DropTableStatement(getConn(), table);
	}
}
