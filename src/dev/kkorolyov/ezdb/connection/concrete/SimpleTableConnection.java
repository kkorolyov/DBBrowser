package dev.kkorolyov.ezdb.connection.concrete;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import dev.kkorolyov.ezdb.connection.DatabaseConnection;
import dev.kkorolyov.ezdb.connection.TableConnection;
import dev.kkorolyov.ezdb.construct.Column;
import dev.kkorolyov.ezdb.construct.RowEntry;
import dev.kkorolyov.ezdb.construct.SqlType;
import dev.kkorolyov.ezdb.exceptions.ClosedException;
import dev.kkorolyov.ezdb.exceptions.NullTableException;
import dev.kkorolyov.ezdb.logging.DebugLogger;
import dev.kkorolyov.ezdb.statement.StatementBuilder;

/**
 * A simple {@code TableConnection} implementation.
 * Uses a {@code DBConnection} to execute statements formatted for its table.
 * @see TableConnection
 * @see DatabaseConnection
 */
public class SimpleTableConnection implements TableConnection {	// TODO Return if isClosed() for every method
	private static final DebugLogger log = DebugLogger.getLogger(SimpleTableConnection.class.getName());

	private DatabaseConnection conn;
	private String tableName;
	
	private final String metaDataStatement;

	/**
	 * Opens a new connection to a specified table on a database.
	 * @param conn database connection
	 * @param tableName name of table to connect to
	 * @throws NullTableException if such a table does not exist on the specified database
	 * @throws ClosedException if constructed from a closed database connection 
	 */
	public SimpleTableConnection(DatabaseConnection conn, String tableName) throws NullTableException, ClosedException {		
		if (!conn.containsTable(tableName))
			throw new NullTableException(conn.getDBName(), tableName);
		
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
	public ResultSet select(Column[] columns) throws SQLException, ClosedException {
		return select(columns, null);
	}
	@Override
	public ResultSet select(Column[] columns, RowEntry[] criteria) throws SQLException, ClosedException {
		return conn.execute(StatementBuilder.buildSelect(tableName, columns, criteria), criteria);	// Execute marked statement with substituted parameters
	}
	
	@Override
	public int insert(RowEntry[] values) throws SQLException, ClosedException {		
		return conn.update(StatementBuilder.buildInsert(tableName, values.length), values);
	}
	
	@Override
	public int delete(Column[] criteria) throws SQLException {
		return 0;	// TODO
	};
	
	@Override
	public int update(Column[] criteria) throws SQLException {
		return 0;	// TODO
	};
	
	@Override
	public void flush() throws ClosedException {
		conn.flush();
	}
	
	@Override
	public ResultSetMetaData getMetaData() throws ClosedException {	// TODO executeVolatile() in DBConnection, closes statement immediately before return
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
	public Column[] getColumns() throws ClosedException {	// TODO Reorganize into try
		ResultSetMetaData rsmd = getMetaData();
		int columnCount = 0;
		try {
			columnCount = rsmd.getColumnCount();
		} catch (SQLException e) {
			log.exceptionSevere(e);
		}
		Column[] columns = new Column[columnCount];
		
		for (int i = 0; i < columns.length; i++) {	// Build columns
			try {
				String columnName = rsmd.getColumnName(i + 1);	// RSMD columns start from 1
				int columnTypeCode = rsmd.getColumnType(i + 1);
				SqlType columnType = null;
				
				for (SqlType type : SqlType.values()) {	// Set appropriate column type
					if (type.getTypeCode() == columnTypeCode)
						columnType = type;
				}
				columns[i] = new Column(columnName, columnType);
			} catch (SQLException e) {
				log.exceptionSevere(e);
			}
		}
		return columns;
	}
	
	@Override
	public int getNumColumns() throws ClosedException {
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
	public int getNumRows() throws ClosedException {
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
