package dev.kkorolyov.sqlob.connection;

import java.sql.SQLException;

import dev.kkorolyov.sqlob.construct.Column;
import dev.kkorolyov.sqlob.construct.Results;
import dev.kkorolyov.sqlob.construct.RowEntry;
import dev.kkorolyov.sqlob.exceptions.ClosedException;

/**
 * Opens a connection to a single table on a database and provides an interface for table-oriented SQL statement execution.
 */
public interface TableConnection {
	
	/**
	 * Closes the connection and releases all resources.
	 * Has no effect if called on a closed connection.
	 */
	void close();
	
	/** @return {@code true} if the connection is closed */
	boolean isClosed();
	
	/**
	 * Executes a SELECT statement without any constraining criteria.
	 * @see #select(Column[], RowEntry[])
	 */
	Results select(Column[] columns) throws SQLException, ClosedException;
	/**
	 * Executes a SELECT statement constrained by the specified criteria.
	 * @param columns column(s) to return; if {@code null}, empty, or any column name = "*", will return all columns
	 * @param criteria specified as columns with certain values; if {@code null} or empty, will return all rows
	 * @return results meeting the specified columns and criteria
	 * @throws SQLException if specified parameters result in an invalid statement
	 * @throws ClosedException if called on a closed connection
	 */
	Results select(Column[] columns, RowEntry[] criteria) throws SQLException, ClosedException;
	
	/**
	 * Inserts a row of entries into the table.
	 * @param entries entries to insert
	 * @return number of inserted rows
	 * @throws SQLException if specified values result in an invalid statement
	 * @throws ClosedException if called on a closed connection
	 */
	int insert(RowEntry[] entries) throws SQLException, ClosedException;
	
	/**
	 * Deletes rows matching the specified criteria.
	 * @param criteria specified as columns with certain values
	 * @return number of deleted rows
	 * @throws SQLException if specified values result in an invalid statement
	 * @throws ClosedException if called on a closed connection
	 */
	int delete(RowEntry[] criteria) throws SQLException, ClosedException;
	
	/**
	 * Updates entries to new values.
	 * @param newEntries new entries to set
	 * @param criteria criteria to match
	 * @return number of updated rows
	 * @throws SQLException if specified values result in an invalid statement
	 * @throws ClosedException if called on a closed connection
	 */
	int update(RowEntry[] newEntries, RowEntry[] criteria) throws SQLException, ClosedException;
	
	/**
	 * Closes all open statements.
	 * @throws ClosedException if called on a closed connection
	 */
	void flush() throws ClosedException;
	
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
	 * @throws ClosedException if called on a closed connection
	 */
	Column[] getColumns() throws ClosedException;
	
	/**
	 * @return total number of columns in this table
	 * @throws ClosedException if called on a closed connection
	 */
	int getNumColumns() throws ClosedException;
	/**
	 * @return total number of rows in this table
	 * @throws ClosedException if called on a closed connection
	 */
	int getNumRows() throws ClosedException;
}
