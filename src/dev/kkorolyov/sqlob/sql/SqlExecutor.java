package dev.kkorolyov.sqlob.sql;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Executes SQL statements.
 */
public class SqlExecutor {
	private Connection conn;
	
	/**
	 * Constructs a new SQL executor.
	 * @param conn connection with which to execute statements
	 */
	public SqlExecutor(Connection conn) {
		this.conn = conn;
	}
	
	/**
	 * Executes a statement.
	 * @param sql statement to execute
	 * @throws SQLException if a database error occurs
	 */
	public void execute(String sql) throws SQLException {
		try (Statement s = conn.createStatement()) {
			s.executeUpdate(sql);
		}
	}
}
