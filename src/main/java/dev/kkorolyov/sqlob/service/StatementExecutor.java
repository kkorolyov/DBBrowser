package dev.kkorolyov.sqlob.service;

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

	private final Map<Class<?>, Iterable<PreparedStatement>> insertStatements = new HashMap<>();

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

	public UUID insert(Object o) {
		PreparedStatement statement = getStatement(o);
	}

	private PreparedStatement getStatement(Object o) {

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
					conn.rollback();
				} catch (SQLException e1) {
					throw new RuntimeException(e1);
				}
				log.exception(e);
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

			conn.close();
			conn = null;
		}
	}
}
