package dev.kkorolyov.sqlob.connection;

import dev.kkorolyov.sqlob.construct.Column;
import dev.kkorolyov.sqlob.construct.Results;
import dev.kkorolyov.sqlob.construct.RowEntry;
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
	 */
	Results execute(ResultingStatement statement);
	/**
	 * Registers with and executes the specified statement.
	 * @param statement statement to execute
	 * @return number of rows affected by statement execution
	 * @throws UncheckedSQLException if the executed statement is invalid
	 * @throws ClosedException if called on a closed connection
	 */
	int execute(UpdatingStatement statement);
	
	/**
	 * Executes a SQL statement with substituted parameters.
	 * @param baseStatement base SQL statement with '?' denoting an area for parameter substitution
	 * @param parameters parameters to substitute in order of declaration
	 * @return results of statement execution, or {@code null} if statement does not return results
	 */
	Results execute(String baseStatement, RowEntry... parameters);
	/**
	 * Executes a SQL statement with substituted parameters.
	 * @param baseStatement base SQL statement with '?' denoting an area for parameter substitution
	 * @param parameters parameters to substitute in order of declaration
	 * @return number of rows affected by statement execution
	 */
	int update (String baseStatement, RowEntry... parameters);
		
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
	
	/**
	 * Returns the statement log associated with this connection.
	 * May be called on a closed connection.
	 * @return associated statement log
	 */
	StatementLog getStatementLog();
}
