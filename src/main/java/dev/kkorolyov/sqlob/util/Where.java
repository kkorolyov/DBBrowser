package dev.kkorolyov.sqlob.util;

import dev.kkorolyov.simplefuncs.function.ThrowingBiConsumer;
import dev.kkorolyov.sqlob.logging.Logger;

import java.lang.reflect.Field;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import static dev.kkorolyov.sqlob.column.Column.ID_COLUMN;
import static dev.kkorolyov.sqlob.util.PersistenceHelper.getName;
import static dev.kkorolyov.sqlob.util.PersistenceHelper.getPersistableFields;

/**
 * A criterion usable in requests.
 * Multiple criteria may be chained together using {@link #and(Where)} and {@link #or(Where)}.
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
		// TODO Abstract this id.toString() as a converter in Column
		return eq(ID_COLUMN.getName(), id.toString());
	}
	/** @return where matching {@code o}'s individual attributes */
	public static Where eqObject(Object o) {
		return getPersistableFields(o.getClass())
				.collect(Where::new,
						((ThrowingBiConsumer<Where, Field, IllegalAccessException>) (where, f) -> where.and(eq(getName(f), f.get(o)))),
						// Wheres not easily compose-able
						(where1, where2) -> {});
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
		sql.append("(").append(where).append(")");

		nodes.addAll(where.nodes);

		return this;
	}
	private void append(WhereNode node) {
		sql.append(node);
		nodes.add(node);
	}

	/**
	 * Sets the resolved value for all criteria for an attribute in this where clause.
	 * @param attribute attribute name
	 * @param resolver function resolving a criterion's value
	 */
	public void setResolvedValue(String attribute, Function<Object, Object> resolver) {
		nodes.stream()
				.filter(node -> node.attribute.equals(attribute))
				.forEach(node -> node.setResolvedValue(resolver.apply(node.value)));
	}

	/**
	 * Prepares a prepared statement with this where clause's values.
	 * @param statement statement to prepare
	 * @return {@code statement}
	 */
	public PreparedStatement contributeToStatement(PreparedStatement statement) {
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

	/** @return SQL WHERE clause represented by this where */
	@Override
	public String toString() {
		return sql.toString();
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

		void setResolvedValue(Object resolvedValue) {
			this.resolvedValue = resolvedValue;
		}

		@Override
		public String toString() {
			return attribute + " " + operator + " ?";
		}
	}
}
