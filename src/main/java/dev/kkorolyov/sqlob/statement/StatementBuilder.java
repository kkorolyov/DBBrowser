package dev.kkorolyov.sqlob.statement;

import java.sql.SQLException;
import java.sql.Statement;

/**
 * Builds {@link Statement}s.
 * @param <T> statement type
 */
public interface StatementBuilder<T extends Statement> {
	/**
	 * @return built statement
	 * @throws SQLException if a SQL issue occurs building the statement
	 */
	T build() throws SQLException;
}
