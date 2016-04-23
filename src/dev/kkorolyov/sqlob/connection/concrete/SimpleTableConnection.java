package dev.kkorolyov.sqlob.connection.concrete;
import java.lang.reflect.Array;
import java.sql.SQLException;

import dev.kkorolyov.simplelogs.Logger;
import dev.kkorolyov.sqlob.connection.DatabaseConnection;
import dev.kkorolyov.sqlob.connection.TableConnection;
import dev.kkorolyov.sqlob.construct.Column;
import dev.kkorolyov.sqlob.construct.Results;
import dev.kkorolyov.sqlob.construct.RowEntry;
import dev.kkorolyov.sqlob.statement.StatementBuilder;

/**
 * A simple {@code TableConnection} implementation.
 * Uses a {@code DatabaseConnection} to execute statements on itself.
 * @see DatabaseConnection
 */
public class SimpleTableConnection implements TableConnection, AutoCloseable {
	private static final Logger log = Logger.getLogger(SimpleTableConnection.class.getName());

	private DatabaseConnection conn;
	private String tableName;
	
	private final String metaDataStatement;

	/**
	 * Constructs a table-specific connection for a {@code DatabaseConnection}.
	 * @param conn database connection
	 * @param tableName name of table to explore
	 */
	public SimpleTableConnection(DatabaseConnection conn, String tableName) {
		this.conn = conn;
		this.tableName = tableName;		
		
		metaDataStatement = StatementBuilder.buildSelect(tableName, null, null);	// Metadata statement = "SELECT * FROM <table>"
	}
	
	@Override
	public void close() {
		if (isClosed())	// Already closed
			return;
		
		conn.close();
		conn = null;
	}
	
	@Override
	public boolean isClosed() {
		return (conn == null);
	}
	
	@Override
	public Results select(Column[] columns) throws SQLException {
		return select(columns, null);
	}
	@Override
	public Results select(Column[] columns, RowEntry[] criteria) throws SQLException {
		return conn.execute(StatementBuilder.buildSelect(tableName, columns, criteria), criteria);	// Execute marked statement with substituted parameters
	}
	
	@Override
	public int insert(RowEntry[] entries) throws SQLException {		
		return conn.update(StatementBuilder.buildInsert(tableName, entries), entries);
	}
	
	@Override
	public int delete(RowEntry[] criteria) throws SQLException {
		return conn.update(StatementBuilder.buildDelete(tableName, criteria), criteria);
	}
	
	@Override
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
	
	@Override
	public void flush() {
		conn.flush();
	}
	
	@Override
	public String getTableName() {
		return tableName;
	}
	@Override
	public String getDBName() {
		return conn.getDBName();
	}
	
	@Override
	public Column[] getColumns() {
		Column[] columns = null;
		try {
			columns = conn.execute(metaDataStatement).getColumns();
		} catch (SQLException e) {
			log.exception(e);
		}
		return columns;
	}
	
	@Override
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
	 * May take a while for large tables.
	 */
	@Override
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
