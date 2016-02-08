package dev.kkorolyov.ezdb.connection;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import dev.kkorolyov.ezdb.column.Column;

/**
 * Opens a connection to a single table on a database and provides an interface for table-oriented SQL statement execution.
 */
public interface TableConnection {
	
	/**
	 * Closes the connection and releases all resources.
	 * Has no effect if called on a closed connection.
	 */
	void close();
	
	/**
	 * Executes a SELECT statement without any criteria.
	 * @see #select(String[], Column[])
	 */
	ResultSet select(String[] columns) throws SQLException;
	/**
	 * Executes a SELECT statement with additional criteria.
	 * @param columns column(s) to return; if any column = "*", will return all columns
	 * @param criteria specified as columns with certain values; if {@code null} or empty, will return all rows
	 * @return results meeting the specified columns and criteria
	 * @throws SQLException if specified parameters result in an invalid statement
	 */
	ResultSet select(String[] columns, Column[] criteria) throws SQLException;
	
	/**
	 * Inserts a row into the table.
	 * @param values columns to insert, specified as a name+value pair
	 * @return number of inserted rows
	 * @throws SQLException if specified values result in an invalid statement
	 */
	int insert(Column[] values) throws SQLException;
	
	/**
	 * Deletes rows matching the specified criteria.
	 * @param criteria specified as columns with certain values
	 * @return number of deleted rows
	 * @throws SQLException if specified values result in an invalid statement
	 */
	int delete(Column[] criteria) throws SQLException;
	
	/**
	 * Updates columns to new values.
	 * @param criteria criteria to match
	 * @return number of updated rows
	 * @throws SQLException
	 */
	int update(Column[] criteria) throws SQLException;
	
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
	Column[] getColumns();
	
	/**
	 * @return total number of columns in this table
	 */
	int getNumColumns();
	/**
	 * @return total number of rows in this table
	 */
	int getNumRows();
}
