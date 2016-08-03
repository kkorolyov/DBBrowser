package dev.kkorolyov.sqlob.construct.statement;

import dev.kkorolyov.sqlob.connection.DatabaseConnection;
import dev.kkorolyov.sqlob.construct.RowEntry;

/**
 * A SQL statement encapsulated as an executable command.
 */
public abstract class StatementCommand {
	private static final String parameterMarker = "\\?";
	
	private final String baseStatement;
	private final RowEntry[] 	values,
														criteria;
	private final DatabaseConnection conn;

	StatementCommand(String baseStatement, RowEntry[] values, RowEntry[] criteria, DatabaseConnection conn) {
		this.baseStatement = baseStatement;
		this.values = values;
		this.criteria = criteria;
		this.conn = conn;
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
	public RowEntry[] getCriteria() {
		return criteria;
	}
	/** @return all statement parameters in the order: {@code values}, {@code criteria}, or {@code null} if this statement has no parameters */
	public RowEntry[] getParameters() {
		RowEntry[] parameters = (values != null || criteria != null) ? (new RowEntry[(values == null ? 0 : values.length) + (criteria == null ? 0 : criteria.length)]) : null;
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
	
	/** @return database connection registered to execute this statement */
	public DatabaseConnection getConn() {
		return conn;
	}
	
	@Override
	public String toString() {
		String result = baseStatement;
		
		RowEntry[] parameters = getParameters();
		if (parameters != null) {
			for (RowEntry parameter : parameters)
				result = result.replaceFirst(parameterMarker, parameter.getValue().toString());
		}
		return result;
	}
}
