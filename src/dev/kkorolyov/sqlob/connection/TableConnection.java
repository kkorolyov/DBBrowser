package dev.kkorolyov.sqlob.connection;

import dev.kkorolyov.sqlob.construct.Column;
import dev.kkorolyov.sqlob.construct.Results;
import dev.kkorolyov.sqlob.construct.RowEntry;
import dev.kkorolyov.sqlob.statement.DeleteRowStatement;
import dev.kkorolyov.sqlob.statement.InsertRowStatement;
import dev.kkorolyov.sqlob.statement.SelectStatement;
import dev.kkorolyov.sqlob.statement.UpdateRowStatement;

/**
 * A filter for a {@code DatabaseConnection} providing for table-oriented actions.
 * @see DatabaseConnection
 */
public class TableConnection implements AutoCloseable {	// TODO Single-column statements (Auto-build array of 1 column/entry)
	private DatabaseConnection conn;
	private String tableName;
	
	/**
	 * Constructs a table-specific connection for a {@code DatabaseConnection}.
	 * @param conn database connection
	 * @param tableName name of table to explore
	 */
	public TableConnection(DatabaseConnection conn, String tableName) {
		this.conn = conn;
		this.tableName = tableName;		
	}
	
	/**
	 * Closes the parent {@code DatabaseConnection} and releases all resources.
	 * Has no effect if called on a closed connection.
	 */
	@Override
	public void close() {
		if (isClosed())	// Already closed
			return;
		
		conn.close();
	}
	
	/** @return {@code true} if the parent {@code DatabaseConnection} connection is closed */
	public boolean isClosed() {
		return (conn.isClosed());
	}
	
	/**
	 * Executes a SELECT statement without any constraining criteria.
	 * @see #select(Column[], RowEntry[])
	 */
	public Results select(Column[] columns) {
		return select(columns, null);
	}
	/**
	 * Executes a SELECT statement constrained by the specified criteria.
	 * @param columns column(s) to return; if {@code null}, empty, or any column name = "*", will return all columns
	 * @param criteria specified as columns with certain values; if {@code null} or empty, will return all rows
	 * @return results meeting the specified columns and criteria
	 * @throws UncheckedSQLException if specified parameters result in an invalid statement
	 * @throws ClosedException if called on a closed connection
	 */
	public Results select(Column[] columns, RowEntry[] criteria) {
		return new SelectStatement(conn, tableName, columns, criteria).execute();
	}
	
	/**
	 * Inserts a row of entries into the table.
	 * @param entries entries to insert
	 * @return number of inserted rows
	 * @throws UncheckedSQLException if specified values result in an invalid statement
	 * @throws ClosedException if called on a closed connection
	 */
	public int insert(RowEntry[] entries) {		
		return new InsertRowStatement(conn, tableName, entries).execute();
	}
	
	/**
	 * Deletes rows matching the specified criteria.
	 * @param criteria specified as columns with certain values
	 * @return number of deleted rows
	 * @throws UncheckedSQLException if specified values result in an invalid statement
	 * @throws ClosedException if called on a closed connection
	 */
	public int delete(RowEntry[] criteria) {
		return new DeleteRowStatement(conn, tableName, criteria).execute();
	}
	
	/**
	 * Updates entries to new values.
	 * @param newEntries new entries to set
	 * @param criteria criteria to match
	 * @return number of updated rows
	 * @throws UncheckedSQLException if specified values result in an invalid statement
	 * @throws ClosedException if called on a closed connection
	 */
	public int update(RowEntry[] newEntries, RowEntry[] criteria) {
		return new UpdateRowStatement(conn, tableName, newEntries, criteria).execute();
	}
	
	/**
	 * Closes all open statements.
	 * @throws ClosedException if called on a closed connection
	 */
	public void flush() {
		conn.flush();
	}
	
	/**
	 * Returns the database connection this table connection serves as a filter for.
	 * May be called on a closed connection.
	 * @return the parent {@code DatabaseConnection}
	 */
	public DatabaseConnection getDatabase() {
		return conn;
	}
	
	/**
	 * @return name of this table
	 */
	public String getTableName() {
		return tableName;
	}
	
	/**
	 * Returns all the columns in this table.
	 * @return all table columns
	 * @throws ClosedException if called on a closed connection
	 */
	public Column[] getColumns() {
		return new SelectStatement(conn, tableName, null, null).execute().getColumns();
	}
	
	/**
	 * Returns the number of columns in this table.
	 * @return total number of columns in this table
	 * @throws ClosedException if called on a closed connection
	 */
	public int getNumColumns() {
		return new SelectStatement(conn, tableName, null, null).execute().getNumColumns();
	}
	/**
	 * Returns the number of rows in this table.
	 * May take a while for large tables.
	 * @return total number of rows in this table
	 * @throws ClosedException if called on a closed connection
	 */
	public int getNumRows() {
		int numRows = 0;

		Results rs = new SelectStatement(conn, tableName, null, null).execute();
		while (rs.getNextRow() != null)	// Counts rows
			numRows++;
		
		return numRows;
	}
}
