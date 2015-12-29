package dev.kkorolyov.jdbmanager.connection;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Opens a connection to a single table on a database and provides an interface for table-oriented SQL statement execution.
 */
public interface TableConnection {
	
	/**
	 * Closes the connection and releases all resources.
	 */
	void close();
	
	/**
	 * Executes a complete SQL statement.
	 * @param statement statement to execute
	 * @return results from statement execution
	 * @throws SQLException
	 */
	ResultSet execute(String statement) throws SQLException;
	/**
	 * Executes a partial SQL statement with parameters declared separately.
	 * @param baseStatement statement without parameters, with {@code ?} denoting an area where a parameter should be substituted in
	 * @param parameters parameters to use, will be substituted into the base statement in the order of appearance
	 * @return results from statement execution
	 * @throws SQLException
	 */
	ResultSet execute(String baseStatement, Object... parameters) throws SQLException;
	
	/**
	 * Closes all open statements.
	 */
	void flush();
	
	/**
	 * @return name of this table
	 */
	String getTableName();
	/**
	 * @return name of the database this table is located in
	 */
	String getDBName();
	
	/**
	 * @return names of all columns
	 */
	String[] getColumnNames();
	
	/**
	 * @return total number of columns in this table
	 */
	int getNumColumns();
	/**
	 * @return total number of rows in this table
	 */
	int getNumRows();
}
