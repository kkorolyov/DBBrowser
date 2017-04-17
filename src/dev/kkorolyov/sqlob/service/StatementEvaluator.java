package dev.kkorolyov.sqlob.service;

import java.sql.Connection;
import java.sql.SQLException;

import dev.kkorolyov.sqlob.logging.Logger;

/**
 * Evaluates and executes statements.
 */
public class Statemen
		tEvaluator {
	private static final Logger log = Logger.getLogger(StatementEvaluator.class.getName());

	private final Connection conn;
	private final Mapper mapper;
	private final StatementGenerator generator;

	/**
	 * Constructs a new evaluator using the default {@code Mapper}.
	 */
	public StatementEvaluator(Connection conn) {
		this(conn, new Mapper());
	}
	/**
	 * Constructs a new evaluator.
	 * @param conn connection providing {@code Statements}
	 * @param mapper Java-SQL mapper used by this evaluator
	 */
	public StatementEvaluator(Connection conn, Mapper mapper) {
		this.conn = conn;
		this.mapper = mapper;
		this.generator = new StatementGenerator(mapper);
	}

	/**
	 * Creates relational tables for a class and all its associated classes.
	 * @param c class to create table for
	 */
	public void create(Class<?> c) {
		try {
			for (String sql : generator.generateCreate(c)) {
				conn.createStatement().execute(sql);
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
}
