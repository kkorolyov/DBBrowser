package dev.kkorolyov.sqlob.utility;

import java.util.LinkedList;
import java.util.List;

/**
 * A condition for object retrieval.
 */
public class Condition {
	private final StringBuilder sql = new StringBuilder();
	private final List<Object> values = new LinkedList<>();

	/**
	 * Constructs a new, empty condition.
	 * This is useful for conditions created dynamically, such as by loops.
	 */
	public Condition() {}
	/**
	 * Constructs a new condition.
	 * @param attribute attribute to check
	 * @param operator check operation
	 * @param value value to match
	 */
	public Condition(String attribute, String operator, Object value) {
		sql.append(attribute).append(" ").append(operator).append(" ").append(value == null ? "NULL" : "?");
		if (value != null) values.add(value);
	}
	
	/**
	 * Appends a condition to the end of this condition using {@code AND}.
	 * @param condition condition to append
	 * @return this condition
	 */
	public Condition and(Condition condition) {
		if (sql.length() > 0) sql.append(" AND ");
		sql.append("(").append(condition).append(")");

		appendValues(condition);

		return this;
	}
	/**
	 * Appends a condition to the end of this condition using {@code OR}.
	 * @param condition condition to append
	 * @return this condition
	 */
	public Condition or(Condition condition) {
		if (sql.length() > 0) sql.append(" OR ");
		sql.append("(").append(condition).append(")");

		appendValues(condition);

		return this;
	}
	
	/**
	 * Appends a condition to the end of this condition using {@code AND}.
	 * @param attribute attribute to check
	 * @param operator check operation
	 * @param value value to match
	 * @return this condition
	 */
	public Condition and(String attribute, String operator, Object value) {
		return and(new Condition(attribute, operator, value));
	}
	/**
	 * Appends a condition to the end of this condition using {@code OR}.
	 * @param attribute attribute to check
	 * @param operator check operation
	 * @param value value to match
	 * @return this condition
	 */
	public Condition or(String attribute, String operator, Object value) {
		return or(new Condition(attribute, operator, value));
	}
	
	/** @return all values in this condition in original insertion order */
	public Iterable<Object> values() {
		return values;
	}
	
	private void appendValues(Condition condition) {
		for (Object value : condition.values()) values.add(value);
	}
	
	/** @return SQL phrase representing this condition */
	@Override
	public String toString() {
		return sql.toString();
	}
}
