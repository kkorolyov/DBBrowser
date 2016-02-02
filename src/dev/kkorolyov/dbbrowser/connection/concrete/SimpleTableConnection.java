package dev.kkorolyov.dbbrowser.connection.concrete;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import dev.kkorolyov.dbbrowser.column.PGColumn;
import dev.kkorolyov.dbbrowser.connection.DBConnection;
import dev.kkorolyov.dbbrowser.connection.TableConnection;
import dev.kkorolyov.dbbrowser.exceptions.NullTableException;
import dev.kkorolyov.dbbrowser.logging.DBLogger;
import dev.kkorolyov.dbbrowser.statement.StatementBuilder;

/**
 * A simple {@code TableConnection} implementation.
 * Uses a {@code DBConnection} to execute statements formatted for its table.
 * @see TableConnection
 * @see DBConnection
 */
public class SimpleTableConnection implements TableConnection {
	private static final DBLogger log = DBLogger.getLogger(SimpleTableConnection.class.getName());

	private DBConnection conn;
	private String tableName;
	
	private final String metaDataStatement;

	/**
	 * Opens a new connection to a specified table on a database.
	 * @param conn database connection
	 * @param tableName name of table to connect to
	 * @throws NullTableException if such a table does not exist on the specified database
	 */
	public SimpleTableConnection(DBConnection conn, String tableName) throws NullTableException {		
		if (!conn.containsTable(tableName))
			throw new NullTableException(conn.getDBName(), tableName);
		
		this.conn = conn;
		this.tableName = tableName;		
		
		metaDataStatement = StatementBuilder.buildSelect(tableName, null, null);
	}
	
	@Override
	public void close() {
		if (conn == null)	// Already closed
			return;
		
		conn.close();
		conn = null;
	}
	
	@Override
	public ResultSet select(String[] columns) throws SQLException {
		return select(columns, null);
	}
	@Override
	public ResultSet select(String[] columns, PGColumn[] criteria) throws SQLException {
		Object[] selectParameters = null;	// Parameters to use in execute call
				
		if (criteria != null && criteria.length > 0) {
			selectParameters = new Object[criteria.length];
			for (int i = 0; i < selectParameters.length; i++) {
				selectParameters[i] = criteria[i].getValue();	// Build parameters to use in execute call
			}		
		}
		return conn.execute(StatementBuilder.buildSelect(tableName, columns, criteria), selectParameters);	// Execute marked statement with substituted parameters
	}
	
	@Override
	public void insert(Object[] values) throws SQLException {		
		conn.execute(StatementBuilder.buildInsert(tableName, values.length), values);
	}
	
	@Override
	public void flush() {
		conn.flush();
	}
	
	@Override
	public ResultSetMetaData getMetaData() {	// TODO executeVolatile(), closes statement immediately before return
		ResultSetMetaData rsmd = null;
		try {
			rsmd = conn.execute(metaDataStatement).getMetaData();
		} catch (SQLException e) {
			log.exceptionSevere(e);	// Metadata statement should not cause exception
		}
		return rsmd;
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
	public PGColumn[] getColumns() {	// TODO Reorganize into try
		ResultSetMetaData rsmd = getMetaData();
		int columnCount = 0;
		try {
			columnCount = rsmd.getColumnCount();
		} catch (SQLException e) {
			log.exceptionSevere(e);
		}
		PGColumn[] columns = new PGColumn[columnCount];
		
		for (int i = 0; i < columns.length; i++) {	// Build columns
			try {
				String columnName = rsmd.getColumnName(i + 1);	// RSMD column names start from 1
				PGColumn.Type columnType = null;
				
				switch (rsmd.getColumnType(i + 1)) {	// Set correct column type
				case (java.sql.Types.BOOLEAN):
					columnType = PGColumn.Type.BOOLEAN;
					break;
				case (java.sql.Types.CHAR):
					columnType = PGColumn.Type.CHAR;
					break;
				case (java.sql.Types.DOUBLE):
					columnType = PGColumn.Type.DOUBLE;
					break;
				case (java.sql.Types.INTEGER):
					columnType = PGColumn.Type.INTEGER;
					break;
				case (java.sql.Types.REAL):
					columnType = PGColumn.Type.REAL;
					break;
				case (java.sql.Types.VARCHAR):
					columnType = PGColumn.Type.VARCHAR;
					break;
				}
				columns[i] = new PGColumn(columnName, columnType);
			} catch (SQLException e) {
				log.exceptionSevere(e);
			}
		}
		return columns;
	}
	
	@Override
	public int getNumColumns() {
		int numColumns = 0;
		try {
			numColumns = getMetaData().getColumnCount();
		} catch (SQLException e) {
			log.exceptionSevere(e);
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
			ResultSet rs = conn.execute(metaDataStatement);
			while (rs.next())	// Counts rows
				numRows++;
		} catch (SQLException e) {
			log.exceptionSevere(e);
		}
		return numRows;
	}
}
