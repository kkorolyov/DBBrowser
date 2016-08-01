package dev.kkorolyov.sqlob.statement;

import dev.kkorolyov.sqlob.connection.TableConnection;
import dev.kkorolyov.sqlob.construct.Column;

/**
 * An executable and revertible {@code DROP TABLE} SQL statement.
 */
public class DropTableStatement extends UpdatingStatement {
	private String table;
	private Column[] columns;
	
	/**
	 * Constructs a new {@code DROP TABLE} statement.
	 * @param table table to drop
	 */
	public DropTableStatement(String table) {
		super(StatementBuilder.buildDrop(table), null, null);
		this.table = table;
	}

	@Override
	public StatementCommand getReversionStatement() {
		return isRevertible() ? new CreateTableStatement(table, columns) : null;
	}
	
	@Override
	public int execute() {
		TableConnection droppedTable = getConn().connect(table);
		if (droppedTable != null)
			columns = droppedTable.getColumns();
			
		return super.execute();
	}
}
