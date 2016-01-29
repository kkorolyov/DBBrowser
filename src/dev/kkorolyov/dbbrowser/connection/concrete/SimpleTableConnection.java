package dev.kkorolyov.dbbrowser.connection.concrete;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;

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
	
	private static final String tableMarker = "<TABLE>", columnsMarker = "<COLUMNS>", criteriaMarker = "<CRITERIA>", valuesMarker = "<VALUES>";	// To easily replace statement segments in functions
	
	private static final String wildcard = "*";	// Selects all columns
	
	/* 
	 * Statements without set table name, same for all tables 
	 */
	private static final String selectStatementStatic = "SELECT " + columnsMarker + " FROM " + tableMarker;	// No criteria	TODO StatementBuilder class?
	private static final String selectStatementCriteriaStatic = selectStatementStatic + " WHERE " + criteriaMarker;	// With criteria
	private static final String insertStatementStatic = "INSERT INTO " + tableMarker + " VALUES " + valuesMarker;

	private DBConnection conn;
	private String tableName;
	private List<Statement> openStatements = new LinkedList<>();

	/*
	 * Statements with this table's name
	 */
	private final String selectStatementBase, selectStatementCriteriaBase;
	private final String insertStatementBase;
	
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
		
		/*
		 * Common statements substituted with table name, ready for use in functions
		 */
		selectStatementBase = selectStatementStatic.replaceFirst(tableMarker, tableName);
		selectStatementCriteriaBase = selectStatementCriteriaStatic.replaceFirst(tableMarker, tableName);
		insertStatementBase = insertStatementStatic.replaceFirst(tableMarker, tableName);
		
		metaDataStatement = selectStatementBase.replaceFirst(columnsMarker, wildcard);	// "Select all" to get metadata
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
	private static String buildSelectColumns(String[] columns) {
		StringBuilder selectColumns = new StringBuilder();
		String delimeter = ",";
		
		for (int i = 0; i < columns.length; i++) {
			if (columns[i].equals(wildcard))
				return wildcard;	// If any column is a wildcard, use a wildcard for statement
			
			selectColumns.append(columns[i]).append(delimeter);	// Append "<column>," to delimit columns
		}
		selectColumns.replace(selectColumns.length() - delimeter.length(), selectColumns.length(), "");	// Remove final delimiter
		
		return selectColumns.toString();
	}
	private static String buildSelectCriteriaMarkers(PGColumn[] criteria) {
		StringBuilder selectCriteria = new StringBuilder();
		String marker = "=?", delimeter = " AND ";
		
		for (int i = 0; i < criteria.length; i++) {
			selectCriteria.append(criteria[i].getName()).append(marker).append(delimeter);	// Append "<criteria>=? AND " to delimit criteria
		}
		selectCriteria.replace(selectCriteria.length() - delimeter.length(), selectCriteria.length(), "");	// Remove final delimiter
		
		return selectCriteria.toString();
	}
	
	@Override
	public void insert(Object[] values) throws SQLException {
		String insertStatement = insertStatementBase.replaceFirst(valuesMarker, buildInsertValuesMarkers(values.length));	// Set values (markers)
		
		conn.execute(insertStatement, values);
	}
	private static String buildInsertValuesMarkers(int numMarkers) {
		StringBuilder insertValues = new StringBuilder("(");	// Values declared within parentheses
		String marker = "?", delimeter = ",";
		
		for (int i = 0; i < numMarkers; i++) {
			insertValues.append(marker).append(delimeter);	// Append "?," to delimit values
		}
		insertValues.replace(insertValues.length() - delimeter.length(), insertValues.length(), ")");	// Replace final delimiter with closing parenthesis
		
		return insertValues.toString();
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
