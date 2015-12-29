package dev.kkorolyov.jdbmanager.connection;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Opens a connection to a single database and allows for SQL statement execution.
 */
public interface DBConnection {
	
	/**
	 * Closes the connection and releases all resources.
	 */
	void close();
	
	/**
	 * Executes a complete SQL statement.
	 * @param statement statement to execute
	 * @return results from statement execution
	 * @throws SQLException if attempting to execute an invalid statement
	 */
	ResultSet execute(String statement) throws SQLException;
	/**
	 * Executes a partial SQL statement with parameters declared separately.
	 * @param baseStatement statement without parameters, with {@code ?} denoting an area where a parameter should be substituted in
	 * @param parameters parameters to use, will be substituted into the base statement in the order of appearance
	 * @return results from statement execution
	 * @throws SQLException if attempting to execute an invalid statement
	 */
	ResultSet execute(String baseStatement, Object... parameters) throws SQLException;
	
	/**
	 * Closes all opened statements.
	 */
	void flush();
	
	/**
	 * @param table name of table to search for
	 * @return {@code true} if this database contains a table of the specified name, {@code false} if otherwise 
	 */
	boolean containsTable(String table);
	
	/**
	 * @return names of all tables in this database.
	 */
	String[] getTables();
	
	/**
	 * @return name of this database
	 */
	String getDBName();
}
