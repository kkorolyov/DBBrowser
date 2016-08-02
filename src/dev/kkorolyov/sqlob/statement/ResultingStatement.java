package dev.kkorolyov.sqlob.statement;

import dev.kkorolyov.sqlob.connection.DatabaseConnection;
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
	 * @param conn database connection with which to execute this statement
	 * @return results obtained by statement execution
	 * @throws IllegalStateException if this statement is not in an executable state
	 */
	public Results execute(DatabaseConnection conn) {
		assertExecutable();
		
		return conn.execute(getBaseStatement(), getParameters());
	}
	
	/**
	 * An executable custom SQL statement.
	 * Cannot be reverted.
	 */
	public static class CustomStatement extends UpdatingStatement {
		/**
		 * Constructs a new custom statement.
		 * @param baseStatement base SQL statement, with {@code ?} denoting areas of substitution with parameters
		 * @param parameters parameters to utilize in statement, will be substituted into the base statement in order of declaration
		 */
		public CustomStatement(String baseStatement, RowEntry[] parameters) {
			super(baseStatement, parameters, null);
		}
		
		@Override
		public StatementCommand getReversionStatement() {
			return null;
		}
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
