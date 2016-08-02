package dev.kkorolyov.sqlob.statement;

import dev.kkorolyov.sqlob.construct.RowEntry;

/**
 * A SQL statement encapsulated as an executable command.
 */
public abstract class StatementCommand {
	private String baseStatement;
	private RowEntry[] 	values,
											criteria;
	private boolean executed = false;
	
	StatementCommand(String baseStatement, RowEntry[] values, RowEntry[] criteria) {
		this.baseStatement = baseStatement;
		this.values = values;
		this.criteria = criteria;
	}
	
	/** @return {@code true} if this statement is in an executable state */
	public boolean isExecutable() {
		return !executed;
	}
	/** @return {@code true} if this statement is in a revertible state */
	public boolean isRevertible() {
		return executed;
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
