package dev.kkorolyov.sqlob.connection;
import java.lang.reflect.Array;
import java.sql.SQLException;

import dev.kkorolyov.sqlob.logging.Logger;
import dev.kkorolyov.sqlob.logging.LoggerInterface;
import dev.kkorolyov.sqlob.connection.DatabaseConnection;
import dev.kkorolyov.sqlob.construct.Column;
import dev.kkorolyov.sqlob.construct.Results;
import dev.kkorolyov.sqlob.construct.RowEntry;
import dev.kkorolyov.sqlob.exceptions.ClosedException;
import dev.kkorolyov.sqlob.statement.StatementBuilder;

/**
 * A filter for a {@code DatabaseConnection} providing for table-oriented actions.
 * @see DatabaseConnection
 */
public class TableConnection implements AutoCloseable {	// TODO Single-column statements (Auto-build array of 1 column/entry)
	private static final LoggerInterface log = Logger.getLogger(TableConnection.class.getName());

	private DatabaseConnection conn;
	private String tableName;
	
	private final String metaDataStatement;

	/**
	 * Constructs a table-specific connection for a {@code DatabaseConnection}.
	 * @param conn database connection
	 * @param tableName name of table to explore
	 */
	public TableConnection(DatabaseConnection conn, String tableName) {
		this.conn = conn;
		this.tableName = tableName;		
		
		metaDataStatement = StatementBuilder.buildSelect(tableName, null, null);	// Metadata statement = "SELECT * FROM <table>"
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
	public Results select(Column[] columns) throws SQLException {
		return select(columns, null);
	}
	/**
	 * Executes a SELECT statement constrained by the specified criteria.
	 * @param columns column(s) to return; if {@code null}, empty, or any column name = "*", will return all columns
	 * @param criteria specified as columns with certain values; if {@code null} or empty, will return all rows
	 * @return results meeting the specified columns and criteria
	 * @throws SQLException if specified parameters result in an invalid statement
	 * @throws ClosedException if called on a closed connection
	 */
	public Results select(Column[] columns, RowEntry[] criteria) throws SQLException {
		return conn.execute(StatementBuilder.buildSelect(tableName, columns, criteria), criteria);	// Execute marked statement with substituted parameters
	}
	
	/**
	 * Inserts a row of entries into the table.
	 * @param entries entries to insert
	 * @return number of inserted rows
	 * @throws SQLException if specified values result in an invalid statement
	 * @throws ClosedException if called on a closed connection
	 */
	public int insert(RowEntry[] entries) throws SQLException {		
		return conn.update(StatementBuilder.buildInsert(tableName, entries), entries);
	}
	
	/**
	 * Deletes rows matching the specified criteria.
	 * @param criteria specified as columns with certain values
	 * @return number of deleted rows
	 * @throws SQLException if specified values result in an invalid statement
	 * @throws ClosedException if called on a closed connection
	 */
	public int delete(RowEntry[] criteria) throws SQLException {
		return conn.update(StatementBuilder.buildDelete(tableName, criteria), criteria);
	}
	
	/**
	 * Updates entries to new values.
	 * @param newEntries new entries to set
	 * @param criteria criteria to match
	 * @return number of updated rows
	 * @throws SQLException if specified values result in an invalid statement
	 * @throws ClosedException if called on a closed connection
	 */
	public int update(RowEntry[] newEntries, RowEntry[] criteria) throws SQLException {
		RowEntry[] combinedParameters = combineArrays(RowEntry.class, newEntries, criteria);
		
		return conn.update(StatementBuilder.buildUpdate(tableName, newEntries, criteria), combinedParameters);
	}
	@SafeVarargs
	private static <T> T[] combineArrays(Class<T> finalClass, T[]... arrays) {
		int totalLength = 0;
		for (T[] array : arrays)	// Add lengths
			totalLength += array.length;
		
		@SuppressWarnings("unchecked")
		T[] combined = (T[]) Array.newInstance(finalClass, totalLength);	// Create empty combined array

		int cursor = 0;
		for (T[] array : arrays) {	// Fill combined array
			for (T element : array) {
				combined[cursor] = element;
				cursor++;
			}
		}
		return combined;
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
		Column[] columns = null;
		try {
			columns = conn.execute(metaDataStatement).getColumns();
		} catch (SQLException e) {
			log.exception(e);
		}
		return columns;
	}
	
	/**
	 * Returns the number of columns in this table.
	 * @return total number of columns in this table
	 * @throws ClosedException if called on a closed connection
	 */
	public int getNumColumns() {
		int numColumns = 0;
		try {
			numColumns = conn.execute(metaDataStatement).getNumColumns();
		} catch (SQLException e) {
			log.exception(e);
		}
		return numColumns;
	}
	/**
	 * Returns the number of rows in this table.
	 * May take a while for large tables.
	 * @return total number of rows in this table
	 * @throws ClosedException if called on a closed connection
	 */
	public int getNumRows() {
		int numRows = 0;
		try {
			Results rs = conn.execute(metaDataStatement);
			while (rs.getNextRow() != null)	// Counts rows
				numRows++;
		} catch (SQLException e) {
			log.exception(e);
		}
		return numRows;
	}
}
