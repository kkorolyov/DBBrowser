package dev.kkorolyov.sqlob.statement;

import dev.kkorolyov.sqlob.construct.Results;
import dev.kkorolyov.sqlob.construct.RowEntry;

/**
 * A {@code StatementCommand} which returns a {@code Results} on execution.
 * @see StatementCommand
 * @see Results
 */
public abstract class ResultingStatement extends StatementCommand {
	ResultingStatement(String baseStatement, RowEntry[] values, RowEntry[] criteria) {
		super(baseStatement, values, criteria);
	}

	/**
	 * Executes this statement if it is in an executable state.
	 * @return results obtained by statement execution
	 * @throws IllegalStateException if this statement is not in an executable state
	 */
	public Results execute() {
		assertExecutable();
		
		return getConn().runStatement(this);
	}
}
