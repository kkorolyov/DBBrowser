package dev.kkorolyov.sqlob.connection;

/**
 * Listens for events concerning SQL statements.
 */
public interface StatementListener {
	/**
	 * Invoked when a SQL statement has been prepared by a {@code DatabaseConnection}.
	 * @param statement full text of the prepared statement
	 * @param source database connection preparing the statement
	 */
	void statementPrepared(String statement, DatabaseConnection source);
}
