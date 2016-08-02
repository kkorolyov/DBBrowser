package dev.kkorolyov.sqlob.statement;

import dev.kkorolyov.sqlob.construct.RowEntry;

/**
 * A SQL statement encapsulated as an executable command.
 */
public abstract class StatementCommand {
	private String baseStatement;
	private RowEntry[] 	values,
											criteria;
	
	StatementCommand(String baseStatement, RowEntry[] values, RowEntry[] criteria) {
		this.baseStatement = baseStatement;
		this.values = values;
		this.criteria = criteria;
	}
	
	/** @return a statement which accomplishes the opposite of this statement, or {@code null} if this statement has no inverse */
	public abstract StatementCommand getInverseStatement();
	
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
