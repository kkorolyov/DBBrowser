package dev.kkorolyov.sqlob.util;

import dev.kkorolyov.sqlob.column.KeyColumn;

import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;

import static dev.kkorolyov.sqlob.util.PersistenceHelper.getName;
import static dev.kkorolyov.sqlob.util.PersistenceHelper.getPersistableFields;
import static dev.kkorolyov.sqlob.util.ReflectionHelper.getValue;

/**
 * Criteria usable in requests as a SQL WHERE clause.
 * The standard lifecycle of a Where is
 * <pre>
 * - Build a Where using whichever combination of constructors, factory methods, concatenation methods
 * - Resolve criteria values by providing a resolver function to all attributes in the Where
 * - Append the string representation of the Where to some SQL statement
 * - Create a {@link PreparedStatement} from the string statement
 * - Contribute the Where to the prepared statement
 * </pre>
 */
public class Where {
	private final StringBuilder sql = new StringBuilder();
	private final List<WhereNode> nodes = new ArrayList<>();

	/** @return where for {@code attribute == value}; translates to {@link #isNull(String)} if {@code value} is {@code null} */
	public static Where eq(String attribute, Object value) {
		return value != null
				? new Where(attribute, "=", value)
				: isNull(attribute);
	}
	/** @return where for {@code attribute != value}; translates to {@link #isNotNull(String)} if {@code value} is {@code null} */
	public static Where neq(String attribute, Object value) {
		return value != null
				? new Where(attribute, "<>", value)
				: isNotNull(attribute);
	}

	/** @return where for {@code attribute < value} */
	public static Where lt(String attribute, Object value) {
		return new Where(attribute, "<", value);
	}
	/** @return where for {@code attribute > value} */
	public static Where gt(String attribute, Object value) {
		return new Where(attribute, ">", value);
	}
	/** @return where for {@code attribute <= value} */
	public static Where lte(String attribute, Object value) {
		return new Where(attribute, "<=", value);
	}
	/** @return where for {@code attribute >= value} */
	public static Where gte(String attribute, Object value) {
		return new Where(attribute, ">=", value);
	}

	/** @return where for {@code attribute IS NULL} */
	public static Where isNull(String attribute) {
		return new Where(attribute, "IS", null);
	}
	/** @return where for {@code attribute IS NOT NULL} */
	public static Where isNotNull(String attribute) {
		return new Where(attribute, "IS NOT", null);
	}

	/** @return where matching on {@code id} */
	public static Where eqId(UUID id) {
		return eq(KeyColumn.ID.getName(), id);
	}
	/** @return where matching {@code o}'s individual attributes */
	public static Where eqObject(Object o) {
		return getPersistableFields(o.getClass())
				.map(f -> eq(getName(f), getValue(o, f)))
				.reduce(Where::and)
				.orElseThrow(() -> new IllegalArgumentException("Object 'o' has no persistable fields"));
	}

	/**
	 * Constructs a new where.
	 * @param attribute attribute to check
	 * @param operator check operation
	 * @param value value to match
	 */
	public Where(String attribute, String operator, Object value) {
		append(new WhereNode(attribute, operator, value));
	}

	/**
	 * Appends a where to the end of this where using {@code AND}.
	 * @param attribute attribute to test
	 * @param operator test operation
	 * @param value value to match
	 * @return {@code this}
	 */
	public Where and(String attribute, String operator, Object value) {
		return and(new Where(attribute, operator, value));
	}
	/**
	 * Appends a where to the end of this where using {@code AND}.
	 * @param where where to append
	 * @return {@code this}
	 */
	public Where and(Where where) {
		return append(where, "AND");
	}

	/**
	 * Appends a where to the end of this where using {@code OR}.
	 * @param attribute attribute to test
	 * @param operator test operation
	 * @param value value to match
	 * @return {@code this}
	 */
	public Where or(String attribute, String operator, Object value) {
		return or(new Where(attribute, operator, value));
	}
	/**
	 * Appends a where to the end of this where using {@code OR}.
	 * @param where where to append
	 * @return {@code this}
	 */
	public Where or(Where where) {
		return append(where, "OR");
	}

	private Where append(Where where, String joiner) {
		sql.append(" ").append(joiner).append(" ")
				.append("(").append(where.getSql()).append(")");

		nodes.addAll(where.nodes);

		return this;
	}
	private void append(WhereNode node) {
		sql.append(node.getSql());
		nodes.add(node);
	}

	/**
	 * Consumes all {index, value} pairs of an attribute.
	 * @param attribute attribute to filter on
	 * @param action bi-consumer invoked with each {index, value} pair of {@code attribute} in this where clause
	 */
	public void consumeValues(String attribute, BiConsumer<Integer, Object> action) {
		nodes.stream()
				.filter(node -> node.attribute.equals(attribute))
				.forEach(node -> action.accept(nodes.indexOf(node), node.value));
	}

	/** @return SQL representation of this where clause */
	public String getSql() {
		return sql.toString();
	}

	@Override
	public String toString() {
		return nodes.stream()
				.reduce(sql.toString(),
						(sql, node) -> sql.replaceFirst("\\?", Matcher.quoteReplacement(String.valueOf(node.value))),
						(sql1, sql2) -> sql1);
	}

	private static class WhereNode {
		private final String attribute;
		private final String operator;
		private final Object value;

		WhereNode(String attribute, String operator, Object value) {
			this.attribute = attribute;
			this.operator = operator;
			this.value = value;
		}

		String getSql() {
			return attribute + " " + operator + " ?";
		}

		@Override
		public String toString() {
			return attribute + " " + operator + " " + value;
		}
	}
}
