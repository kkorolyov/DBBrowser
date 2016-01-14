package dev.kkorolyov.dbbrowser.connection;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
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
	 * Executes a SELECT statement on the table.
	 * @param columns column(s) to return; if {@code columns[0]} = "*", will return all columns
	 * @return results meeting the specified columns
	 * @throws SQLException if specified parameters result in an invalid statement
	 */
	ResultSet select(String[] columns) throws SQLException;
	/**
	 * Executes a SELECT statement with additional criteria on the table.
	 * @param columns column(s) to return; if {@code columns[0]} = '*', will return all columns
	 * @param criteria specified as columns with certain values
	 * @return results meeting the specified columns and criteria
	 * @throws SQLException if specified parameters result in an invalid statement
	 */
	ResultSet select(String[] columns, PGColumn[] criteria) throws SQLException;
	
	/**
	 * Inserts a row into the table.
	 * @param values column values in order of column appearance
	 * @throws SQLException
	 */
	void insert(Object[] values) throws SQLException;
	
	/**
	 * Closes all open statements.
	 */
	void flush();
	
	/**
	 * @return table metadata
	 */
	ResultSetMetaData getMetaData();
	
	/**
	 * @return name of this table
	 */
	String getTableName();
	/**
	 * @return name of the database this table is located in
	 */
	String getDBName();
	
	/**
	 * @return all table columns
	 */
	PGColumn[] getColumns();
	
	/**
	 * @return total number of columns in this table
	 */
	int getNumColumns();
	/**
	 * @return total number of rows in this table
	 */
	int getNumRows();
}
