package dev.kkorolyov.sqlob.construct.statement;

import java.util.List;

import dev.kkorolyov.sqlob.connection.DatabaseConnection;
import dev.kkorolyov.sqlob.construct.Entry;

/**
 * A {@code StatementCommand} which returns an {@code int} result on execution.
 * @see StatementCommand
 */
public class UpdateStatement extends StatementCommand {
	UpdateStatement(String baseStatement, List<Entry> values, List<Entry> conditions, DatabaseConnection conn) {
		super(baseStatement, values, conditions, conn);
	}
	
	/**
	 * Executes this statement.
	 * @return number of rows updated by statement execution
	 */
	public int execute() {
		return getConn().update(getSql(), getParameters());
	}
	
	/** @return a statement which accomplishes the inverse of this statement, or {@code null} if this statement has no inverse */
	public UpdateStatement getInverseStatement() {
		return null;
	}
}
