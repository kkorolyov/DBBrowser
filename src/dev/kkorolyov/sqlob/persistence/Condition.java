package dev.kkorolyov.sqlob.persistence;

import java.util.LinkedList;
import java.util.List;

/**
 * A condition for object retrieval.
 */
public class Condition {
	private List<Object> values = new LinkedList<>();
	private StringBuilder sql;
	
	/**
	 * Constructs a new condition.
	 * @param attribute attribute to check
	 * @param operator check operation
	 * @param value value to match
	 */
	public Condition(String attribute, String operator, Object value) {
		if (value != null)
			values.add(value);
		
		sql = new StringBuilder(attribute).append(" ").append(operator).append(" ").append(value == null ? "NULL" : "?");
	}
	
	/**
	 * Appends a condition to the end of this condition using {@code AND}.
	 * @param condition condition to append
	 * @return this condition
	 */
	public Condition and(Condition condition) {
		sql.append(" AND ").append(condition);
		
		return this;
	}
	/**
	 * Appends a condition to the end of this condition using {@code OR}.
	 * @param condition condition to append
	 * @return this condition
	 */
	public Condition or(Condition condition) {
		sql.append(" OR ").append(condition);
		
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
	
	/** @return all values in this condition, in order */
	Iterable<Object> values() {
		return values;
	}
	
	/** @return SQL phrase representing this condition */
	@Override
	public String toString() {
		return sql.toString();
	}
}
