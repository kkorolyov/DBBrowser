package dev.kkorolyov.ezdb.connection.concrete;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import dev.kkorolyov.ezdb.column.Column;
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
	public ResultSet select(String[] columns, Column[] criteria) throws SQLException {
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
				String columnName = rsmd.getColumnName(i + 1);	// RSMD column names start from 1
				Column.Type columnType = null;
				
				switch (rsmd.getColumnType(i + 1)) {	// Set correct column type
				case (java.sql.Types.BOOLEAN):
					columnType = Column.Type.BOOLEAN;
					break;
				case (java.sql.Types.CHAR):
					columnType = Column.Type.CHAR;
					break;
				case (java.sql.Types.DOUBLE):
					columnType = Column.Type.DOUBLE;
					break;
				case (java.sql.Types.INTEGER):
					columnType = Column.Type.INTEGER;
					break;
				case (java.sql.Types.REAL):
					columnType = Column.Type.REAL;
					break;
				case (java.sql.Types.VARCHAR):
					columnType = Column.Type.VARCHAR;
					break;
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
