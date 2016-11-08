package dev.kkorolyov.sqlob.construct.statement;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import dev.kkorolyov.sqlob.connection.DatabaseConnection;
import dev.kkorolyov.sqlob.construct.Entry;

/**
 * A SQL statement encapsulated as an executable command.
 */
public abstract class StatementCommand {
	private static final String PARAMETER_MARKER = "?";
	
	private final String baseStatement;
	private List<Entry> values,
											conditions;
	private final DatabaseConnection conn;

	StatementCommand(String baseStatement, List<Entry> values, List<Entry> conditions, DatabaseConnection conn) {
		this.baseStatement = baseStatement;
		this.conn = conn;
	}
	
	/** @return	base SQL statement */
	public String getSql() {
		return baseStatement;
	}

	/** @return statement values */
	public List<Entry> getValues() {
		return values;
	}
	/** @return statement conditions */
	public List<Entry> getConditions() {
		return conditions;
	}
	/** @return all statement parameters not specified in the base string in the order: {@code values}, {@code conditions}, or an empty list if this statement has no parameters not specified in the base string */
	public List<Entry> getParameters() {
		List<Entry> parameters = new ArrayList<>();
		
		addParameters(parameters, values);
		addParameters(parameters, conditions);
		
		return parameters;
	}
	private static void addParameters(List<Entry> parameters, List<Entry> entries) {
		if (entries != null) {
			for (Entry entry : entries) {
				if (entry.hasParameter())
					parameters.add(entry);
			}
		}
	}
	
	/** @return database connection registered to execute this statement */
	public DatabaseConnection getConn() {
		return conn;
	}
	
	@Override
	public String toString() {
		String result = baseStatement;
		
		for (Entry parameter : getParameters())
			result = result.replaceFirst(Pattern.quote(PARAMETER_MARKER), (parameter.getValue() == null ? "NULL" : parameter.getValue().toString()));
		
		return result;
	}
}
