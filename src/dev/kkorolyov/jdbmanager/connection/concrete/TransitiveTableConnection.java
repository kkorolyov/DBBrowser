package dev.kkorolyov.jdbmanager.connection.concrete;
import java.sql.*;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import dev.kkorolyov.jdbmanager.connection.DBConnection;
import dev.kkorolyov.jdbmanager.connection.TableConnection;
import dev.kkorolyov.jdbmanager.exceptions.InvalidTableException;
import dev.kkorolyov.jdbmanager.types.Type;

/**
 * {@code TableConnection} implementation which uses a {@code DBConnection} to execute statements.
 * @see TableConnection
 * @see DBConnection
 */
public class TransitiveTableConnection implements TableConnection {
	private static final Logger LOG = Logger.getLogger(TransitiveTableConnection.class.getName());
	
	private DBConnection conn;
	private String tableName;
	private final String metaDataStatement = "SELECT * FROM " + tableName;
	Map<String, Type> columns = new HashMap<>();
	private List<Statement> openStatements = new LinkedList<>();
	
	/**
	 * Opens a new connection to a specified table.
	 * @param conn database connection
	 * @param tableName name of table to connect to
	 * @throws InvalidTableException if such a table does not exist on the specified database
	 */
	public TransitiveTableConnection(DBConnection conn, String tableName) throws InvalidTableException {		
		if (!isValidTableName(conn, tableName))
			throw new InvalidTableException(conn.getDBName(), tableName);
		
		this.conn = conn;
		this.tableName = tableName;
		reloadColumns();
	}
	private boolean isValidTableName(DBConnection conn, String tableName) {
		return conn.containsTable(tableName);
	}
	
	public void reloadColumns() {
		try {
			ResultSetMetaData rsmd = conn.execute(metaDataStatement).getMetaData();
			for (int i = 1; i <= rsmd.getColumnCount(); i++) {
				int type = rsmd.getColumnType(i);
				switch (type) {
					case Types.INTEGER:
						columns.put(rsmd.getColumnName(i), Type.INTEGER);
						break;
					case Types.VARCHAR:
						columns.put(rsmd.getColumnName(i), Type.STRING);
						break;
					case Types.BOOLEAN:
						columns.put(rsmd.getColumnName(i), Type.BOOLEAN);
						break;
				}
			}
		} catch (SQLException e) {
			LOG.log(Level.SEVERE, e.getMessage(), e);
		}
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
	public ResultSet execute(String baseStatement, Object... parameters) throws SQLException {
		return conn.execute(baseStatement, parameters);
	}
	
	@Override
	public void flush() {
		
	}
	
	@Override
	public String[] getColumnNames() {
		reloadColumns();
		return columns.keySet().toArray(new String[columns.size()]);
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
	public int getNumColumns() {
		int numColumns = 0;
		try {
			ResultSetMetaData rsmd = conn.execute(metaDataStatement).getMetaData();
			numColumns = rsmd.getColumnCount();
		} catch (SQLException e) {
			LOG.log(Level.SEVERE, e.getMessage(), e);
		}
		return numColumns;
	}
	@Override
	public int getNumRows() {
		int numRows = 0;
		try {
			ResultSet rs = conn.execute(metaDataStatement);
			while (rs.next())
				numRows++;
		} catch (SQLException e) {
			LOG.log(Level.SEVERE, e.getMessage(), e);
		}
		return numRows;
	}
}
