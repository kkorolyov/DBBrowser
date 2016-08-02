package dev.kkorolyov.sqlob.statement;

import dev.kkorolyov.sqlob.connection.DatabaseConnection;
import dev.kkorolyov.sqlob.connection.TableConnection;
import dev.kkorolyov.sqlob.construct.Column;
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
	 * @param conn database connection with which to execute this statement
	 * @return number of rows updated by statement execution
	 * @throws IllegalStateException if this statement is not in an executable state
	 */
	public int execute(DatabaseConnection conn) {
		assertExecutable();
		
		int result = conn.update(getBaseStatement(), getParameters());
		setExecuted(true);
		
		return result;
	}
	/**
	 * Reverts this statement if it is in a revertible state.
	 * @param conn database connection with which to revert this statement
	 * @return number of rows updated by statement reversion
	 * @throws IllegalStateException if this statement is not in a revertible state
	 */
	public int revert(DatabaseConnection conn) {
		assertRevertible();
		
		int result = conn.execute((UpdatingStatement) getReversionStatement());
		setExecuted(false);
		
		return result;
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
	 * An executable and revertible {@code CREATE TABLE} SQL statement.
	 */
	public static class CreateTableStatement extends UpdatingStatement {
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
	/**
	 * An executable and revertible {@code DROP TABLE} SQL statement.
	 */
	public static class DropTableStatement extends UpdatingStatement {
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
		public int execute(DatabaseConnection conn) {
			TableConnection droppedTable = conn.connect(table);
			if (droppedTable != null)
				columns = droppedTable.getColumns();
				
			return super.execute(conn);
		}
	}
	
	/**
	 * An executable and revertible {@code INSERT ROW} SQL statement.
	 */
	public static class InsertRowStatement extends UpdatingStatement {
		private String table;	// TODO un-reduntify
		
		/**
		 * Constructs a new {@code INSERT ROW} statement.
		 * @param table table to insert row into
		 * @param values values to insert
		 */
		public InsertRowStatement(String table, RowEntry[] values) {
			super(StatementBuilder.buildInsert(table, values), values, null);
			this.table = table;
		}

		@Override
		public StatementCommand getReversionStatement() {
			return isRevertible() ? new DeleteRowStatement(table, getValues()) : null;
		}
	}
	/**
	 * An executable and revertible {@code DELETE ROW} SQL statement.
	 */
	public static class DeleteRowStatement extends UpdatingStatement {
		private String table;
		
		/**
		 * Constructs a new {@code DELETE ROW} statement.
		 * @param table table to delete row(s) from
		 * @param criteria criteria used by row deletion
		 */
		public DeleteRowStatement(String table, RowEntry[] criteria) {
			super(StatementBuilder.buildDelete(table, criteria), null, criteria);
			this.table = table;
		}

		@Override
		public StatementCommand getReversionStatement() {
			return isRevertible() ? new InsertRowStatement(table, getCritera()) : null;
		}
	}
	/**
	 * An executable and revertible {@code INSERT ROW} SQL statement.
	 */
	public static class UpdateRowStatement extends UpdatingStatement {
		private String table;
		
		/**
		 * Constructs a new insert row statement.
		 * @param table table to update row in
		 * @param values new values to set
		 * @param criteria criteria to match
		 */
		public UpdateRowStatement(String table, RowEntry[] values, RowEntry[] criteria) {
			super(StatementBuilder.buildUpdate(table, values, criteria), values, criteria);
			this.table = table;
		}

		@Override
		public StatementCommand getReversionStatement() {
			return isRevertible() ? new UpdateRowStatement(table, getCritera(), getValues()) : null;
		}
	}
}
