package dev.kkorolyov.ezdb.connection.concrete;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import dev.kkorolyov.ezdb.column.Column;
import dev.kkorolyov.ezdb.column.RowEntry;
import dev.kkorolyov.ezdb.column.SQLType;
import dev.kkorolyov.ezdb.connection.DBConnection;
import dev.kkorolyov.ezdb.connection.TableConnection;
import dev.kkorolyov.ezdb.exceptions.NullTableException;
import dev.kkorolyov.ezdb.logging.DBLogger;
import dev.kkorolyov.ezdb.statement.StatementBuilder;

/**
 * A simple {@code TableConnection} implementation.
 * Uses a {@code DBConnection} to execute statements formatted for its table.
 * @see TableConnection
 * @see DBConnection
 */
public class SimpleTableConnection implements TableConnection {	// TODO Return if isClosed() for every method
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
	public ResultSet select(String[] columns) throws SQLException {
		return select(columns, null);
	}
	@Override
	public ResultSet select(String[] columns, RowEntry[] criteria) throws SQLException {
		Object[] selectParameters = null;	// Parameters to use in execute call
				
		if (criteria != null && criteria.length > 0) {
			selectParameters = new Object[criteria.length];
			for (int i = 0; i < selectParameters.length; i++) {
				selectParameters[i] = criteria[i].getValue();	// Build parameters to use in execute call
			}		
		}
		return conn.execute(StatementBuilder.buildSelect(tableName, columns, extractColumns(criteria)), selectParameters);	// Execute marked statement with substituted parameters
	}
	private static Column[] extractColumns(RowEntry[] rowEntries) {
		Column[] columns = new Column[rowEntries.length];
		
		for (int i = 0; i < columns.length; i++) {
			columns[i] = rowEntries[i].getColumn();
		}
		return columns;
	}
	
	@Override
	public int insert(Column[] values) throws SQLException {		
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
	public void flush() {
		conn.flush();
	}
	
	@Override
	public ResultSetMetaData getMetaData() {	// TODO executeVolatile() in DBConnection, closes statement immediately before return
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
	public Column[] getColumns() {	// TODO Reorganize into try
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
				SQLType columnType = null;
				
				for (SQLType type : SQLType.values()) {	// Set appropriate column type
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
