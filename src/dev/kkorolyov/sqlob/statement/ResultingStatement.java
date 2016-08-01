package dev.kkorolyov.sqlob.statement;

import dev.kkorolyov.sqlob.construct.Column;
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
	
	/**
	 * An executable {@code SELECT} SQL statement.
	 */
	public static class SelectStatement extends ResultingStatement {
		/**
		 * Constructs a new {@code SELECT} statement.
		 * @param table table to select from
		 * @param columns columns to select
		 * @param criteria selection criteria, if {@code null} or empty, no criteria is used
		 */
		public SelectStatement(String table, Column[] columns, RowEntry[] criteria) {
			super(StatementBuilder.buildSelect(table, columns, criteria), null, criteria);
		}

		@Override
		public StatementCommand getReversionStatement() {
			return null;
		}
	}
}
