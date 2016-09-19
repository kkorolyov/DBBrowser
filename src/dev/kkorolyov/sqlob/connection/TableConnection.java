package dev.kkorolyov.sqlob.connection;

import java.util.List;

import dev.kkorolyov.sqlob.construct.Column;
import dev.kkorolyov.sqlob.construct.Results;
import dev.kkorolyov.sqlob.construct.RowEntry;

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
	 * Executes a {@code SELECT} statement querying all entries in this table.
	 * @see #select(List, List)
	 */
	public Results select() {
		return select(null);
	}
	/**
	 * Executes a {@code SELECT} statement without any constraining criteria.
	 * @see #select(List, List)
	 */
	public Results select(List<Column> columns) {
		return select(columns, null);
	}
	/**
	 * Executes a {@code SELECT} statement constrained by the specified criteria.
	 * @param columns column(s) to return; if {@code null}, empty, or any column name = "*", will return all columns
	 * @param criteria specified as columns with certain values; if {@code null} or empty, will return all rows
	 * @return results meeting the specified columns and criteria
	 * @throws UncheckedSQLException if specified parameters result in an invalid statement
	 * @throws ClosedException if called on a closed connection
	 */
	public Results select(List<Column> columns, List<RowEntry> criteria) {
		return conn.execute(conn.getStatementFactory().getSelect(tableName, columns, criteria));
	}
	
	/**
	 * Inserts a row of entries into the table.
	 * @param entries entries to insert
	 * @return number of inserted rows
	 * @throws UncheckedSQLException if specified values result in an invalid statement
	 * @throws ClosedException if called on a closed connection
	 */
	public int insert(List<RowEntry> entries) {		
		return conn.execute(conn.getStatementFactory().getInsert(tableName, entries));
	}
	
	/**
	 * Deletes rows matching the specified criteria.
	 * @param criteria specified as columns with certain values
	 * @return number of deleted rows
	 * @throws UncheckedSQLException if specified values result in an invalid statement
	 * @throws ClosedException if called on a closed connection
	 */
	public int delete(List<RowEntry> criteria) {
		return conn.execute(conn.getStatementFactory().getDelete(tableName, criteria));
	}
	
	/**
	 * Updates entries to new values.
	 * @param newEntries new entries to set
	 * @param criteria criteria to match
	 * @return number of updated rows
	 * @throws UncheckedSQLException if specified values result in an invalid statement
	 * @throws ClosedException if called on a closed connection
	 */
	public int update(List<RowEntry> newEntries, List<RowEntry> criteria) {
		return conn.execute(conn.getStatementFactory().getUpdate(tableName, newEntries, criteria));
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
	 * @return all columns in this table
	 * @throws ClosedException if called on a closed connection
	 */
	public List<Column> getColumns() {
		return select().getColumns();
	}
	
	/**
	 * @return number of columns in this table
	 * @throws ClosedException if called on a closed connection
	 */
	public int getNumColumns() {
		return select().getNumColumns();
	}
	/**
	 * @return number of rows in this table
	 * @throws ClosedException if called on a closed connection
	 */
	public int getNumRows() {
		int numRows = 0;

		Results rs = select();
		while (rs.getNextRow() != null)	// Counts rows
			numRows++;
		
		return numRows;
	}
}
