package dev.kkorolyov.sqlob.service;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import dev.kkorolyov.sqlob.logging.Logger;
import dev.kkorolyov.sqlob.persistence.NonPersistableException;

/**
 * Executes statements on a {@link Connection}.
 */
public class StatementExecutor implements AutoCloseable {
	private static final Logger log = Logger.getLogger(StatementExecutor.class.getName());

	private final Mapper mapper;
	private final StatementGenerator generator;
	private Connection conn;

	private final Map<Class<?>, PreparedStatement> inserts = new HashMap<>();

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
		try {
			Statement s = conn.createStatement();

			for (String statement : generator.generateCreate(c)) {
				s.addBatch(statement);
				log.debug(() -> "Added batch statement: " + statement);
			}
			s.executeBatch();
			log.debug(() -> "Executed batch statements");
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
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

			int indexer = 2;
			for (Field f : mapper.getPersistableFields(o.getClass())) {
				Object value = mapper.convert(f.get(o));
				if (mapper.isComplex(f)) {
					log.debug(() -> f + " is complex, inserting before continuing with " + o + "...");
					value = insert(value);  // Insert, store ID ref if complex
				}
				statement.setObject(indexer++, value);
			}
			return id;
		} catch (IllegalAccessException e) {
			throw new NonPersistableException("Unable to get field value from " + o, e);
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
