package dev.kkorolyov.sqlob.util;

import dev.kkorolyov.simplefuncs.function.ThrowingFunction;
import dev.kkorolyov.sqlob.column.KeyColumn;
import dev.kkorolyov.sqlob.logging.Logger;

import java.lang.reflect.Field;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import static dev.kkorolyov.sqlob.util.PersistenceHelper.getName;
import static dev.kkorolyov.sqlob.util.PersistenceHelper.getPersistableFields;

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
	private static final Logger LOG = Logger.getLogger(Where.class.getName());

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
		return eq(KeyColumn.PRIMARY.getName(), id);
	}
	/** @return where matching {@code o}'s individual attributes */
	public static Where eqObject(Object o) {
		return getPersistableFields(o.getClass())
				.map((ThrowingFunction<Field, Where, IllegalAccessException>) f -> eq(getName(f), f.get(o)))
				.reduce(Where::and)
				.orElseThrow(() -> new IllegalArgumentException("Object 'o' has no persistable fields"));
	}

	/**
	 * Constructs a new, empty where.
	 * This is useful for dynamic construction, such as in loops or as a collector.
	 */
	public Where() {}
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
		if (sql.length() > 0) sql.append(" ").append(joiner).append(" ");
		sql.append("(").append(where.getSql()).append(")");

		nodes.addAll(where.nodes);

		return this;
	}
	private void append(WhereNode node) {
		sql.append(node.getSql());
		nodes.add(node);
	}

	/** @return whether all criteria in this where clause have been resolved */
	public boolean isResolved() {
		return nodes.stream()
				.allMatch(WhereNode::isResolved);
	}
	/** @return all attributes in this where clause which have not been resolved */
	public Collection<String> getUnresolvedAttributes() {
		return nodes.stream()
				.filter(node -> !node.isResolved())
				.map(node -> node.attribute)
				.collect(Collectors.toSet());
	}

	/**
	 * Resolves values for all criteria for an attribute in this where clause.
	 * @param attribute attribute name
	 * @param resolver function resolving a criterion's value
	 */
	public void resolve(String attribute, UnaryOperator<Object> resolver) {
		nodes.stream()
				.filter(node -> node.attribute.equals(attribute))
				.forEach(node -> node.resolve(resolver));
	}

	/**
	 * Prepares a prepared statement with this where clause's values.
	 * @param statement statement to prepare
	 * @return {@code statement}
	 */
	public PreparedStatement contributeToStatement(PreparedStatement statement) {
		if (!isResolved()) throw new IllegalStateException("Contains unresolved attributes: " + getUnresolvedAttributes());

		for (int i = 0; i < nodes.size(); i++) {
			try {
				statement.setObject(i + 1, nodes.get(i).resolvedValue);
			} catch (SQLException e) {
				LOG.exception(e);
				throw new UncheckedSqlException(e);
			}
		}
		LOG.debug("Contributed {} to {}", this, statement);

		return statement;
	}

	/** @return SQL representation of this where clause */
	public String getSql() {
		return sql.toString();
	}

	@Override
	public String toString() {
		return nodes.stream()
				.reduce(sql.toString(),
						(sql, node) -> sql.replaceFirst("\\?", String.valueOf(node.resolvedValue)),
						(sql1, sql2) -> sql1);
	}

	private static class WhereNode {
		private final String attribute;
		private final String operator;
		private final Object value;
		private Object resolvedValue;

		WhereNode(String attribute, String operator, Object value) {
			this.attribute = attribute;
			this.operator = operator;
			this.value = value;
		}

		boolean isResolved() {
			return value == null || resolvedValue != null;
		}
		void resolve(UnaryOperator<Object> resolver) {
			resolvedValue = resolver.apply(value);
		}

		String getSql() {
			return attribute + " " + operator + " ?";
		}

		@Override
		public String toString() {
			return attribute + " " + operator + " " + resolvedValue;
		}
	}
}
