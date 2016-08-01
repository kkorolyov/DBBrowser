package dev.kkorolyov.sqlob.statement;

import dev.kkorolyov.sqlob.construct.RowEntry;

/**
 * A {@code StatementCommand} which returns an {@code int} result on execution.
 * @see StatementCommand
 */
public abstract class UpdatingStatement extends StatementCommand {
	UpdatingStatement(String baseStatement, RowEntry[] values, RowEntry[] criteria) {
		super(baseStatement, values, criteria);
	}
	
	/**
	 * Executes this statement if it is in an executable state.
	 * @return number of rows updated by statement execution
	 * @throws IllegalStateException if this statement is not in an executable state
	 */
	public int execute() {
		assertExecutable();
		
		int result = getConn().runStatement(this);
		setExecuted(true);
		
		return result;
	}
	/**
	 * Reverts this statement if it is in a revertible state.
	 * @return number of rows updated by statement reversion
	 * @throws IllegalStateException if this statement is not in a revertible state
	 */
	public int revert() {
		assertRevertible();
		
		int result = getConn().execute((UpdatingStatement) getReversionStatement());
		setExecuted(false);
		
		return result;
	}
}
