package dev.kkorolyov.sqlob.connection;

import java.sql.SQLException;

import dev.kkorolyov.sqlob.construct.Column;
import dev.kkorolyov.sqlob.construct.Results;
import dev.kkorolyov.sqlob.construct.RowEntry;
import dev.kkorolyov.sqlob.exceptions.ClosedException;
import dev.kkorolyov.sqlob.exceptions.DuplicateTableException;

/**
 * Opens a connection to a single database and allows for SQL statement execution.
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
	 * Closes the connection and releases all resources.
	 * Has no effect if called on a closed connection.
	 */
	void close();
	
	/** @return {@code true} if the connection is closed */
	boolean isClosed();
	
	/**
	 * Executes a complete SQL statement without additional parameters.
	 * @see #execute(String, RowEntry[])
	 */
	Results execute(String statement) throws SQLException;
	/**
	 * Executes a partial SQL statement with parameters.
	 * @param baseStatement statement without parameters, with {@code ?} denoting an area where a parameter should be substituted in
	 * @param parameters values to use, will be substituted into the base statement in the order of appearance, if this array is empty or {@code null}, only the base statement is executed
	 * @return results from statement execution, or {@code null} if the statement does not return results
	 * @throws SQLException if the executed statement is invalid
	 * @throws ClosedException if called on a closed connection
	 */
	Results execute(String baseStatement, RowEntry[] parameters) throws SQLException;
	
	/**
	 * Executes a partial SQL update statement with parameters.
	 * @param baseStatement statement without parameters, with {@code ?} denoting an area where a parameter should be substituted in
	 * @param parameters parameters to use, will be substituted into the base statement in the order of appearance, if this array is empty or {@code null}, only the base statement is executed
	 * @return number of affected rows
	 * @throws SQLException if the executed statement is invalid
	 * @throws ClosedException if called on a closed connection
	 */
	int update(String baseStatement, RowEntry[] parameters) throws SQLException;
	
	/**
	 * Closes all opened statements.
	 * @throws ClosedException if called on a closed connection
	 */
	void flush() throws ClosedException;
	
	/**
	 * Creates a table with the specifed name and columns.
	 * @param name new table name
	 * @param columns new table columns in the order they should appear
	 * @throws DuplicateTableException if a table of the specified name already exists
	 * @return connection to the recently-created table
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
	 * @return {@code true} if this database contains a table of the specified name (ignoring case), {@code false} if otherwise 
	 * @throws ClosedException if called on a closed connection
	 */
	boolean containsTable(String table);
	
	/**
	 * @return names of all tables in this database.
	 * @throws ClosedException if called on a closed connection
	 */
	String[] getTables();
	
	/**
	 * Returns the name of the database connected to.
	 * Callable on a closed connection.
	 * @return name of this database
	 */
	String getDBName();
}
