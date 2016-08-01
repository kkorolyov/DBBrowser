package dev.kkorolyov.sqlob.statement;

import dev.kkorolyov.sqlob.construct.Column;

/**
 * An executable and revertible {@code CREATE TABLE} SQL statement.
 */
public class CreateTableStatement extends UpdatingStatement {
	private String table;
	
	/**
	 * Constructs a new {@code CREATE TABLE} statement.
	 * @param table new table name
	 * @param columns new table columns
	 */
	public CreateTableStatement(String table, Column[] columns) {
		super(StatementBuilder.buildCreate(table, columns), null, null);
	}
	
	@Override
	public StatementCommand getReversionStatement() {
		return new DropTableStatement(table);
	}
}
