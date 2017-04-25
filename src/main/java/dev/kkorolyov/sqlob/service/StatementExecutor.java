package dev.kkorolyov.sqlob.service;

import static dev.kkorolyov.sqlob.service.Constants.ID_NAME;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
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
	private final Map<Class<?>, Constructor<?>> constructors = new HashMap<>();

	/**
	 * Constructs a new evaluator using the default {@code Mapper}.
	 */
	public StatementExecutor() {
		this(new Mapper());
	}
	/**
	 * Constructs a new statement executor.
	 * @param mapper Java-SQL mapper
	 */
	public StatementExecutor(Mapper mapper) {
		this.mapper = mapper;

		generator = new StatementGenerator(mapper);
	}

	/**
	 * Executes a batch of all CREATE statements needed to create a relational table representation of a Java class.
	 * @param c class to create table for
	 */
	public void create(Class<?> c) {
		try (Statement statement = conn.createStatement();) {
			for (String create : generator.generateCreate(c))	statement.addBatch(create);

			statement.executeBatch();
			log.debug(() -> "Executed batch statements");
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Executes a SELECT statement retrieving an instance of {@code c} mapped to {@code id}.
	 * @param c type to retrieve
	 * @param id ID to match
	 * @return instance mapped to {@code id}, or {@code null} if no such instance
	 */
	public <T> T select(Class<T> c, UUID id) {
		Map<UUID, T> results = select(c, new Condition(ID_NAME, "=", mapper.convert(id)));

		return (results.isEmpty()) ? null : results.values().iterator().next();
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

				@SuppressWarnings("unchecked")
				T o = (T) constructors.computeIfAbsent(c, k -> {
					try {
						Constructor<?> constructor = k.getDeclaredConstructor();
						constructor.setAccessible(true);

						return constructor;
					} catch (NoSuchMethodException e) {
						throw new NonPersistableException(k + " does not specify a no-arg constructor", e);
					}
				}).newInstance();

				for (Field f : mapper.getPersistableFields(c)) {
					Object value = extract(f, rs);
					f.set(o, value);
				}
				results.put(id, o);
				log.debug(() -> "Found instance: " + id + "->" + o);
			}
			return results;
		} catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
			throw new NonPersistableException("");
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

			assert statement.executeUpdate() == 1;
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
		Condition where = new Condition(ID_NAME, "=", mapper.convert(id));
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

	/**
	 * Executes a DELETE statement deleting the instance of {@code c} mapped to {code id}.
	 * @param c type to delete
	 * @param id ID of instance to delete
	 * @return {@code true} if instance at {@code id} deleted successfully
	 */
	public boolean delete(Class<?> c, UUID id) {
		return delete(c, new Condition(ID_NAME, "=", mapper.convert(id))) == 1;
	}
	/**
	 * Executes a DELETE statement deleting instances of {@code c} matching {@code where}.
	 * @param c type to delete
	 * @param where condition to match, {@code null} implies no condition
	 * @return number of deleted instances
	 */
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
				statement.setObject(i++, resolve(f.get(o)));
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
				statement.setObject(i++, resolve(value));
			}
			return i;
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	private Object extract(Field f, ResultSet rs) {
		Object resolved;

		if (mapper.isPrimitive(f)) {
			resolved = mapper.extract(f, rs);
			log.debug(() -> f + " is primitive, extracted trivially");
		} else {
			resolved = select(f.getType(), mapper.extract(UUID.class, rs, mapper.getName(f)));
			log.debug(() -> f + " is complex, extracted ID and deserialized");
		}
		return resolved;
	}
	/** Resolves primitive types to themselves, and complex types to their persistable representations */
	private Object resolve(Object o) {
		Object resolved;

		if (mapper.isPrimitive(o.getClass())) {
			resolved = o;
			log.debug(() -> o + " is primitive, resolved trivially");
		} else {
			resolved = selectId(o);

			if (resolved == null) {
				resolved = insert(o);
				log.debug(() -> o + " is complex, persisted and resolved");
			} else {
				log.debug(() -> o + " is complex, found existing persisted instance");
			}
		}
		return mapper.convert(resolved);
	}

	/**
	 * Closes the previous connection and sets a new connection.
	 * @param conn new connection
	 * @see #close()
	 */
	public void setConnection(Connection conn) {
		flush();
		inserts.clear();
		updates.clear();

		this.conn = ready(conn);
	}
	private Connection ready(Connection conn) {
		try {
			conn.setAutoCommit(false);

			return conn;
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Commits all active transactions.
	 */
	public void flush() {
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

	/** @return {@code true} if this executor does not have an active {@link Connection} */
	public boolean isClosed() {
		return conn == null;
	}
	/**
	 * Flushes and closes the underlying {@link Connection}.
	 */
	@Override
	public void close() throws Exception {
		if (conn != null) {
			flush();

			inserts.clear();	// All cached statements closed anyway
			updates.clear();

			conn.close();
			conn = null;

			log.info(() -> "Closed " + this);
		}
	}
}
