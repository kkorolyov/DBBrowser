package dev.kkorolyov.dbbrowser.connection.concrete;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;

import dev.kkorolyov.dbbrowser.browser.DBLogger;
import dev.kkorolyov.dbbrowser.connection.DBConnection;
import dev.kkorolyov.dbbrowser.connection.TableConnection;
import dev.kkorolyov.dbbrowser.exceptions.NullTableException;

/**
 * A simple {@code TableConnection} implementation.
 * @see TableConnection
 * @see DBConnection
 */
public class SimpleTableConnection implements TableConnection {
	private static final DBLogger log = DBLogger.getLogger(SimpleTableConnection.class.getName());
	
	private DBConnection conn;
	private String tableName;
	private List<Statement> openStatements = new LinkedList<>();

	private final String metaDataStatement = "SELECT * FROM " + tableName;	// Metadata statement for this table
	
	/**
	 * Opens a new connection to a specified table on a database.
	 * @param conn database connection
	 * @param tableName name of table to connect to
	 * @throws NullTableException if such a table does not exist on the specified database
	 */
	public SimpleTableConnection(DBConnection conn, String tableName) throws NullTableException {		
		if (!isValidTableName(conn, tableName))
			throw new NullTableException(conn.getDBName(), tableName);
		
		this.conn = conn;
		this.tableName = tableName;
	}
	private boolean isValidTableName(DBConnection conn, String tableName) {
		return conn.containsTable(tableName);
	}
	
	@Override
	public void close() {
		if (conn == null && openStatements == null)	// Already closed
			return;
		
		conn.close();
		conn = null;
		openStatements = null;
	}
	
	@Override
	public ResultSet execute(String statement) throws SQLException {
		return conn.execute(statement);
	}
	@Override
	public ResultSet execute(String baseStatement, Object[] parameters) throws SQLException {
		return conn.execute(baseStatement, parameters);
	}
	
	@Override
	public void flush() {
		conn.flush();
	}
	
	@Override
	public ResultSetMetaData getMetaData() {
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
	public String[] getColumnNames() {
		ResultSetMetaData rsmd = getMetaData();
		int columnCount = 0;
		try {
			columnCount = rsmd.getColumnCount();
		} catch (SQLException e) {
			log.exceptionSevere(e);
		}
		String[] columnNames = new String[columnCount];
		
		for (int i = 0; i < columnNames.length; i++) {	// Build column names
			try {
				columnNames[i] = rsmd.getColumnName(i + 1);	// RSMD column names start from 1
			} catch (SQLException e) {
				log.exceptionSevere(e);
			}
		}
		return columnNames;
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
