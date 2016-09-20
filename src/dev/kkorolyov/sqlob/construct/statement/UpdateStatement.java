package dev.kkorolyov.sqlob.construct.statement;

import java.util.List;

import dev.kkorolyov.sqlob.connection.DatabaseConnection;
import dev.kkorolyov.sqlob.construct.RowEntry;

/**
 * A {@code StatementCommand} which returns an {@code int} result on execution.
 * @see StatementCommand
 */
public class UpdateStatement extends StatementCommand {
	UpdateStatement(String baseStatement, List<RowEntry> values, List<RowEntry> criteria, DatabaseConnection conn) {
		super(baseStatement, values, criteria, conn);
	}
	
	/**
	 * Executes this statement.
	 * @return number of rows updated by statement execution
	 */
	public int execute() {
		return getConn().update(getBaseStatement(), getParameters());
	}
	
	/** @return a statement which accomplishes the inverse of this statement, or {@code null} if this statement has no inverse */
	public UpdateStatement getInverseStatement() {
		return null;
	}
}
