package dev.kkorolyov.sqlob.connection;

import dev.kkorolyov.sqlob.construct.Column;
import dev.kkorolyov.sqlob.construct.Results;
import dev.kkorolyov.sqlob.statement.ResultingStatement;
import dev.kkorolyov.sqlob.statement.UpdatingStatement;

/**
 * A connection to a SQL database.
 * Provides methods for SQL statement execution.
 */
public interface DatabaseConnection {
	/**
	 * Connects to a table on this database, if it exists.
	 * @param table name of table to connect to
	 * @return connection to the table, if it exists, {@code null} if otherwise
	 * @throws ClosedException if called on a closed connection
	 */
	TableConnection connect(String table);
	
	/**
	 * Closes this connection and releases all resources.
	 * Has no effect if called on a closed connection.
	 */
	void close();
	
	/** @return {@code true} if this connection is closed */
	boolean isClosed();
	
	/**
	 * Registers with and executes the specified statement.
	 * @param statement statement to execute
	 * @return results from statement execution, or {@code null} if the statement does not return results
	 * @throws UncheckedSQLException if the executed statement is invalid
	 * @throws ClosedException if called on a closed connection
	 * @throws IllegalArgumentException if {@code statement} cannot be registered to this database connection
	 */
	Results execute(ResultingStatement statement);
	/**
	 * Registers with and executes the specified statement.
	 * @param statement statement to execute
	 * @return number of rows affected by statement execution
	 * @throws UncheckedSQLException if the executed statement is invalid
	 * @throws ClosedException if called on a closed connection
	 * @throws IllegalArgumentException if {@code statement} cannot be not registered to this database connection
	 */
	int execute(UpdatingStatement statement);
	
	/**
	 * Runs the SQL statement encapsulated by the specified statement.
	 * @param statement statement to run
	 * @return results from statement execution, or {@code null} if the statement does not return results
	 * @throws UncheckedSQLException if the executed statement is invalid
	 * @throws ClosedException if called on a closed connection
	 * @throws IllegalArgumentException if {@code statement} is not registered to this database connection
	 */
	Results runStatement(ResultingStatement statement);
	/**
	 * Runs the SQL statement encapsulated by the specified statement.
	 * @param statement statement to run
	 * @return number of rows affected by statement execution
	 * @throws UncheckedSQLException if the executed statement is invalid
	 * @throws ClosedException if called on a closed connection
	 * @throws IllegalArgumentException if {@code statement} is not registered to this database connection
	 */
	int runStatement(UpdatingStatement statement);
	
	/**
	 * Reverts the last statement executed by this connection, if it is revertible.
	 * @return number of reverted rows, or {@code -1} if the last statement is not revertible or there are no more statements to revert
	 */
	int revertLastStatement();
	
	/**
	 * Closes all open statements.
	 * @throws ClosedException if called on a closed connection
	 */
	void flush();
	
	/**
	 * Creates a table with the specifed name and columns.
	 * @param name new table name
	 * @param columns new table columns in the order they should appear
	 * @return connection to the created table
	 * @throws DuplicateTableException if a table of the specified name already exists
	 * @throws ClosedException if called on a closed connection
	 */
	TableConnection createTable(String name, Column[] columns) throws DuplicateTableException;
	
	/**
	 * Drops a table of the specified name from the database.
	 * @param table name of table to drop
	 * @return {@code true} if table dropped successfully, {@code false} if drop failed or no such table
	 * @throws ClosedException if called on a closed connection
	 */
	boolean dropTable(String table);
	
	/**
	 * @param table name of table to search for
	 * @return {@code true} if the database contains a table of the specified name (ignoring case)
	 * @throws ClosedException if called on a closed connection
	 */
	boolean containsTable(String table);
	
	/**
	 * @return names of all tables in the database.
	 * @throws ClosedException if called on a closed connection
	 */
	String[] getTables();
	
	/**
	 * Returns the name of the database.
	 * May be called on a closed connection.
	 * @return name of the database
	 */
	String getDatabaseName();
	
	/** @param listener statement listener to add */
	void addStatementListener(StatementListener listener);
	/** @param listener statement listener to remove */
	void removeStatementListener(StatementListener listener);
}
