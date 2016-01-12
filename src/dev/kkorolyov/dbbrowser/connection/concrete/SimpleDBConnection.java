package dev.kkorolyov.dbbrowser.connection.concrete;
import java.sql.*;
import java.util.LinkedList;
import java.util.List;

import dev.kkorolyov.dbbrowser.browser.DBLogger;
import dev.kkorolyov.dbbrowser.connection.Column;
import dev.kkorolyov.dbbrowser.connection.DBConnection;
import dev.kkorolyov.dbbrowser.connection.TableConnection;
import dev.kkorolyov.dbbrowser.exceptions.DuplicateTableException;
import dev.kkorolyov.dbbrowser.exceptions.NullTableException;
import dev.kkorolyov.dbbrowser.strings.Strings;

/**
 * A simple {@code DBConnection} implementation.
 * @see DBConnection
 */
public class SimpleDBConnection implements DBConnection {
	private static final DBLogger log = DBLogger.getLogger(SimpleDBConnection.class.getName());

	private static final String jdbcDriverClassName = "org.postgresql.Driver";
	private static final String jdbcHeader = "jdbc:postgresql:";
	
	private static final String booleanString = "boolean";
	private static final String charString = "char";
	private static final String doubleString = "double";
	private static final String floatString = "float";
	private static final String integerString = "integer";
	private static final String varcharString = "varchar";
		
	private String URL, DB;
	private Connection conn;
	private List<Statement> openStatements = new LinkedList<>();
	
	/**
	 * Opens a new connection to the specified host and database residing on it.
	 * @param host IP or hostname of host containing database
	 * @param db name of database to connect to
	 * @throws SQLException if the URL is faulty or {@code null}
	 */
	public SimpleDBConnection(String host, String db) throws SQLException {
		URL = formatURL(host, db);
		DB = db;
		initDriver();
		initConnection();
			
		log.debug("Successfully initialized " + getClass().getSimpleName() + " for database at: " + URL);
	}
	private static String formatURL(String host, String db) {
		return jdbcHeader + "//" + host + "/" + db;
	}
	private void initDriver() {
		try {
			Class.forName(jdbcDriverClassName);
		} catch (ClassNotFoundException e) {
			log.exceptionSevere(e);
		}
	}
	private void initConnection() throws SQLException {
		conn = DriverManager.getConnection(URL, Strings.USER, Strings.PASSWORD);
	}
	
	@Override
	public TableConnection connect(String table) {
		try {
			return new SimpleTableConnection(this, table);
		} catch (NullTableException e) {
			return null;
		}
	}
	
	@Override
	public void close() {
		if (conn == null && openStatements == null)	// Already closed
			return;
		
		try {
			conn.close();	// Release JDBC resources
		} catch (SQLException e) {
			log.exceptionWarning(e);
		}
		conn = null;
		openStatements = null;
		
		log.debug("Closed " + getClass().getSimpleName() + " at URL: " + URL);
	}
	
	@Override
	public ResultSet execute(String statement) throws SQLException {
		return execute(statement, (Object[]) null);
	}
	@Override
	public ResultSet execute(String baseStatement, Object[] parameters) throws SQLException {
		PreparedStatement s = conn.prepareStatement(baseStatement);
		openStatements.add(s);	// Add to flushable list
		
		if (parameters != null) {
			for (int i = 0; i < parameters.length; i++) {	// Prepare with appropriate types
				if (parameters[i] instanceof String)
					s.setString(i + 1, (String) parameters[i]);
				else if (parameters[i] instanceof Integer)
					s.setInt(i + 1, (int) parameters[i]);
				else if (parameters[i] instanceof Boolean)
					s.setBoolean(i + 1, (boolean) parameters[i]);
			}
		}
		ResultSet rs = s.executeQuery();
		return rs;
	}
	
	@Override
	public void flush() {
		int closedStatements = 0;	// Count closed statements for debugging
		for (Statement openStatement : openStatements) {
			try {
				openStatement.close();
				closedStatements++;
			} catch (SQLException e) {
				log.exceptionWarning(e);;
			}
		}
		openStatements.clear();
		
		log.debug("Closed " + closedStatements + " statements");
	}
	
	@Override
	public void createTable(String table, Column[] columns) throws DuplicateTableException {
		if (containsTable(table))
			throw new DuplicateTableException(DB, table);
		
		try {
			execute(buildCreateTableStatement(table, columns));
		} catch (SQLException e) {
			log.exceptionSevere(e);
		}
	}
	private String buildCreateTableStatement(String table, Column[] columns) {
		String createTableStatement = "CREATE TABLE " + table + " (";
		for (Column column : columns) {
			createTableStatement += "\"" + column.getName() + "\" ";	// Set column name TODO verify \" works
			
			switch (column.getType()) {	// Set column type
			case (Column.BOOLEAN):
				createTableStatement += booleanString;
				break;
			case (Column.CHAR):
				createTableStatement += charString;
				break;
			case (Column.DOUBLE):
				createTableStatement += doubleString;
				break;
			case (Column.FLOAT):
				createTableStatement += floatString;
				break;
			case (Column.INTEGER):
				createTableStatement += integerString;
				break;
			case (Column.VARCHAR):
				createTableStatement += varcharString;
				break;
			}
		}
		createTableStatement += ")";	// End columns
		return createTableStatement;
	}
	
	@Override
	public void dropTable(String table) {
		// TODO
	}
	
	@Override
	public boolean containsTable(String table) {
		boolean contains = false;
		
		for (String dbTable : getTables()) {
			if (dbTable.equalsIgnoreCase(table)) {
				contains = true;
				break;
			}
		}
		return contains;
	}
	
	@Override
	public String[] getTables() {
		List<String> tables = new LinkedList<>();
		try {
			ResultSet tableSet = conn.getMetaData().getTables(null, null, "%", new String[]{"TABLE"});
			while (tableSet.next()) {
				tables.add(tableSet.getString(3));
			}
			tableSet.close();
		} catch (SQLException e) {
			log.exceptionSevere(e);
		}
		return tables.toArray(new String[tables.size()]);
	}
	
	@Override
	public String getDBName() {
		return DB;
	}
}
