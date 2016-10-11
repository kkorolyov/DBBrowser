package dev.kkorolyov.sqlob.construct.statement;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import dev.kkorolyov.sqlob.connection.DatabaseConnection;
import dev.kkorolyov.sqlob.construct.Entry;

/**
 * A SQL statement encapsulated as an executable command.
 */
public abstract class StatementCommand {
	private static final String parameterMarker = "?";
	
	private final String baseStatement;
	private final List<Entry> 	values,
																criteria;
	private final DatabaseConnection conn;

	StatementCommand(String baseStatement, List<Entry> values, List<Entry> criteria, DatabaseConnection conn) {
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
	public List<Entry> getValues() {
		return values;
	}
	/** @return statement criteria */
	public List<Entry> getCriteria() {
		return criteria;
	}
	/** @return all statement parameters not specified in the base string in the order: {@code values}, {@code criteria}, or an empty list if this statement has no parameters not specified in the base string */
	public List<Entry> getParameters() {
		List<Entry> parameters = new LinkedList<>();
		
		if (values != null)
			parameters.addAll(values);
		if (criteria != null) {
			for (Entry criterion : criteria) {
				if (criterion.hasParameter())
					parameters.add(criterion);
			}
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
		
		for (Entry parameter : getParameters())
			result = result.replaceFirst(Pattern.quote(parameterMarker), (parameter.getValue() == null ? "NULL" : parameter.getValue().toString()));
		
		return result;
	}
}
