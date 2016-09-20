package dev.kkorolyov.sqlob.construct.statement;

import java.util.List;

import dev.kkorolyov.sqlob.connection.DatabaseConnection;
import dev.kkorolyov.sqlob.construct.Results;
import dev.kkorolyov.sqlob.construct.RowEntry;

/**
 * A {@code StatementCommand} which returns a {@code Results} on execution.
 * @see StatementCommand
 * @see Results
 */
public class QueryStatement extends StatementCommand {
	QueryStatement(String baseStatement, List<RowEntry> values, List<RowEntry> criteria, DatabaseConnection conn) {
		super(baseStatement, values, criteria, conn);
	}

	/**
	 * Executes this statement.
	 * @return results obtained by statement execution
	 */
	public Results execute() {
		return getConn().execute(getBaseStatement(), getParameters());
	}
}
