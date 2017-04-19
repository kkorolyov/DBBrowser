package dev.kkorolyov.sqlob.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import dev.kkorolyov.sqlob.logging.Logger;

/**
 * Executes statements on a {@link Connection}.
 */
public class StatementExecutor {
	private static final Logger log = Logger.getLogger(StatementExecutor.class.getName());

	private final Connection conn;
	private final Mapper mapper;

	/**
	 * Constructs a new evaluator using the default {@code Mapper}.
	 */
	public StatementExecutor(Connection conn) {
		this(conn, new Mapper());
	}
	/**
	 * Constructs a new evaluator.
	 * @param conn connection providing {@code Statements}
	 * @param mapper Java-SQL mapper used by this evaluator
	 */
	public StatementExecutor(Connection conn, Mapper mapper) {
		this.conn = conn;
		this.mapper = mapper;
	}

	/**
	 * Executes a batch of CREATE statements.
	 * @param statements statements to execute
	 */
	public void create(Iterable<String> statements) {
		try {
			Statement s = conn.createStatement();

			for (String statement : statements) {
				s.addBatch(statement);
				log.debug(() -> "Added batch statement: " + statement);
			}
			s.executeBatch();
			log.debug(() -> "Executed batch statements");
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
}
