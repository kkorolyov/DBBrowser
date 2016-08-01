package dev.kkorolyov.sqlob.statement;

import dev.kkorolyov.sqlob.connection.DatabaseConnection;
import dev.kkorolyov.sqlob.construct.RowEntry;

/**
 * A SQL statement encapsulated as an executable command.
 */
public abstract class StatementCommand {
	private DatabaseConnection conn;
	private String baseStatement;
	private RowEntry[] 	values,
											criteria;
	private boolean executed = false;
	
	StatementCommand(String baseStatement, RowEntry[] values, RowEntry[] criteria) {
		this.baseStatement = baseStatement;
		this.values = values;
		this.criteria = criteria;
	}
	
	/**
	 * Registers a database connection to this statement if this statement does not yet have a connection.
	 * @param conn database connection to execute this statement
	 * @return {@code true} if registration successful, {@code false} if this statement already has a registered connection
	 */
	public boolean register(DatabaseConnection conn) {
		boolean free = (this.conn == null);
		
		if (free)
			this.conn = conn;
		
		return free;
	}
	
	/** @return {@code true} if this statement is in an executable state */
	public boolean isExecutable() {
		return conn != null && !executed;
	}
	/** @return {@code true} if this statement is in a revertible state */
	public boolean isRevertible() {
		return conn != null && executed;
	}
	void setExecuted(boolean executed) {
		this.executed = executed;
	}
	
	void assertExecutable() {
		if (!isExecutable())
			throw new IllegalStateException("Statement is not in an executable state: " + baseStatement);
	}
	void assertRevertible() {
		if (!isRevertible())
			throw new IllegalStateException("Statement is not in a revertible state: " + baseStatement);
	}
	
	/** @return a statement which reverts this statement, or {@code null} if this statement is not currently revertible */
	public abstract StatementCommand getReversionStatement();
	
	/** @return database connection used for statement execution */
	public DatabaseConnection getConn() {
		return conn;
	}
	
	/** @return	base SQL statement */
	public String getBaseStatement() {
		return baseStatement;
	}
	
	/** @return statement values */
	public RowEntry[] getValues() {
		return values;
	}
	/** @return statement criteria */
	public RowEntry[] getCritera() {
		return criteria;
	}
	/** @return all statement parameters in the order: {@code values}, {@code criteria} */
	public RowEntry[] getParameters() {
		RowEntry[] parameters = new RowEntry[(values == null ? 0 : values.length) + (criteria == null ? 0 : criteria.length)];
		int i = 0;
		
		if (values != null) {
			for (RowEntry value : values)
				parameters[i++] = value;
		}
		if (criteria != null) {
			for (RowEntry criterion : criteria)
				parameters[i++] = criterion;
		}
		return parameters;
	}
}
