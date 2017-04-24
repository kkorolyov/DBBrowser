package dev.kkorolyov.sqlob.service;

import static dev.kkorolyov.sqlob.service.Constants.ID_NAME;

import java.lang.reflect.Field;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import dev.kkorolyov.sqlob.logging.Logger;
import dev.kkorolyov.sqlob.persistence.NonPersistableException;
import dev.kkorolyov.sqlob.utility.Condition;

/**
 * Executes statements on a {@link Connection}.
 */
public class StatementExecutor implements AutoCloseable {
	private static final Logger log = Logger.getLogger(StatementExecutor.class.getName());

	private final Mapper mapper;
	private final StatementGenerator generator;
	private Connection conn;

	private final Map<Class<?>, PreparedStatement> inserts = new HashMap<>();
	private final Map<Class<?>, PreparedStatement> updates = new HashMap<>();

	/**
	 * Constructs a new evaluator using the default {@code Mapper}.
	 * @param conn connection on which to execute statements
	 */
	public StatementExecutor(Connection conn) {
		this(conn, new Mapper());
	}
	/**
	 * Constructs a new statement executor.
	 * @param conn connection on which to execute statements
	 * @param mapper Java-SQL mapper
	 */
	public StatementExecutor(Connection conn, Mapper mapper) {
		this.conn = conn;
		this.mapper = mapper;

		generator = new StatementGenerator(mapper);
	}

	/**
	 * Executes a batch of all CREATE statements needed to create a relational table representation of a Java class.
	 * @param c class to create table for
	 */
	public void create(Class<?> c) {
		try (Statement statement = conn.createStatement();) {
			for (String create : generator.generateCreate(c)) {
				statement.addBatch(create);
				log.debug(() -> "Added batch statement: " + create);
			}
			statement.executeBatch();
			log.debug(() -> "Executed batch statements");
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Executes a SELECT statement retrieving instances of {@code c} matching {@code where}.
	 * @param c type to retrieve
	 * @param where condition to match, {@code null} implies no condition
	 * @return matching {@code c} instances mapped to their respective IDs
	 */
	public <T> Map<UUID, T> select(Class<T> c, Condition where) {
		try (PreparedStatement statement = conn.prepareStatement(generator.generateSelect(c, where))) {
			applyWhere(statement, where, 1);
			ResultSet rs = statement.executeQuery();

			Map<UUID, T> results = new HashMap<>();

			while (rs.next()) {
				UUID id = mapper.extract(UUID.class, rs, ID_NAME);

				T o = c.newInstance();
				for (Field f : mapper.getPersistableFields(c)) f.set(o, mapper.extract(f, rs));

				results.put(id, o);
				log.debug(() -> "Found instance: " + id + "->" + o);
			}
			return results;
		} catch (InstantiationException | IllegalAccessException e) {
			throw new NonPersistableException(c + " does not specify a public, no-arg constructor", e);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Executes a SELECT statement retrieving the ID of an instance.
	 * @param o instance to match
	 * @return ID of {@code o}, or {@code null} if not available
	 */
	public UUID selectId(Object o) {
		Condition where = whereInstance(o);
		try (PreparedStatement statement = conn.prepareStatement(generator.generateSelectId(o.getClass(), where))) {
			applyWhere(statement, where, 1);
			ResultSet rs = statement.executeQuery();

			return (rs.next()) ? mapper.extract(UUID.class, rs, ID_NAME) : null;
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	private Condition whereInstance(Object o) {
		Condition where = new Condition();

		try {
			for (Field f : mapper.getPersistableFields(o.getClass())) {
				Object value = mapper.convert(f.get(o));
				String operator = (value == null) ? "IS" : "=";

				where.and(mapper.getName(f), operator, value);
			}
		} catch (IllegalAccessException e) {
			throw new NonPersistableException("Unable to get field value from " + o, e);
		}
		return where;
	}

	/**
	 * Executes an INSERT statement persisting {@code o}.
	 * @param o object to persist
	 * @return ID of relational representation of {@code o}
	 */
	public UUID insert(Object o) {
		try {
			PreparedStatement statement = inserts.computeIfAbsent(o.getClass(), k -> {
				try {
					return conn.prepareStatement(generator.generateInsert(k));
				} catch (SQLException e) {
					throw new RuntimeException(e);
				}
			});
			UUID id = UUID.randomUUID();

			statement.setObject(1, mapper.convert(id));	// Apply ID to 1st param
			applyInstance(statement, o, 2);

			return id;
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Executes an UPDATE statement updating the instance at {@code id} with {@code o}.
	 * @param id ID of persisted instance to update
	 * @param o updated instance
	 * @return {@code true} if instance at {@code id} updated successfully
	 */
	public boolean update(UUID id, Object o) {
		Condition where = new Condition(ID_NAME, "=", id);
		try {
			PreparedStatement statement = updates.computeIfAbsent(o.getClass(), k -> {
				try {
					return conn.prepareStatement(generator.generateUpdate(k, where));	// Update where ID matches
				} catch (SQLException e) {
					throw new RuntimeException(e);
				}
			});
			int nextI = applyInstance(statement, o, 1);
			applyWhere(statement, where, nextI);

			return statement.executeUpdate() > 0;
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public int delete(Class<?> c, Condition where) {
		try (PreparedStatement statement = conn.prepareStatement(generator.generateDelete(c, where))) {
			applyWhere(statement, where, 1);

			return statement.executeUpdate();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	/** Applies instance values to statement parameters, returns index of next unapplied parameter */
	private int applyInstance(PreparedStatement statement, Object o, int startI) {
		int i = startI;
		try {
			for (Field f : mapper.getPersistableFields(o.getClass())) {
				Object value = mapper.convert(f.get(o));
				if (mapper.isComplex(f)) {
					log.debug(() -> f + " is complex, inserting before continuing with " + o + "...");
					value = insert(value);  // Insert, store ID ref if complex
				}
				statement.setObject(i++, value);
			}
			return i;
		} catch (IllegalAccessException e) {
			throw new NonPersistableException("Unable to get field value from " + o, e);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	/** Applies condition values to statement parameters, returns index of next unapplied parameter */
	private int applyWhere(PreparedStatement statement, Condition where, int startI) {
		int i = startI;
		try {
			for (Object value : where.values()) {
				statement.setObject(i++, mapper.convert(value));
				log.debug(() -> "Applied WHERE value: " + value);
			}
			return i;
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Closes the previous connection and sets a new connection.
	 * @param conn new connection
	 * @see #close()
	 */
	void setConnection(Connection conn) {
		flush();

		this.conn = conn;
	}

	/**
	 * Commits all active transactions.
	 */
	void flush() {
		if (conn != null) {
			try {
				conn.commit();
				log.info(() -> "Flushed " + this);
			} catch (SQLException e) {
				try {
					log.warning(() -> "Exception during flush, attempting rollback...");
					conn.rollback();
				} catch (SQLException e1) {
					log.severe(() -> "Exception during rollback, crashing spectacularly...");
					throw new RuntimeException(e1);
				}
				throw new RuntimeException(e);
			}
		}
	}
	/**
	 * Flushes and closes the underlying {@link Connection}.
	 */
	@Override
	public void close() throws Exception {
		if (conn != null) {
			flush();

			inserts.clear();	// All cached statements closed anyway
			conn.close();
			conn = null;
		}
	}
}
