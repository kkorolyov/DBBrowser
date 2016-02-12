package dev.kkorolyov.ezdb.connection.concrete;
import java.sql.*;
import java.util.LinkedList;
import java.util.List;

import dev.kkorolyov.ezdb.connection.DatabaseConnection;
import dev.kkorolyov.ezdb.connection.TableConnection;
import dev.kkorolyov.ezdb.construct.Column;
import dev.kkorolyov.ezdb.construct.RowEntry;
import dev.kkorolyov.ezdb.exceptions.ClosedException;
import dev.kkorolyov.ezdb.exceptions.DuplicateTableException;
import dev.kkorolyov.ezdb.exceptions.NullTableException;
import dev.kkorolyov.ezdb.logging.DebugLogger;
import dev.kkorolyov.ezdb.statement.StatementBuilder;
import dev.kkorolyov.ezdb.strings.Strings;

/**
 * A simple {@code DBConnection} implementation.
 * @see DatabaseConnection
 */
public class SimpleDatabaseConnection implements DatabaseConnection {
	private static final DebugLogger log = DebugLogger.getLogger(SimpleDatabaseConnection.class.getName());

	private static final String jdbcDriverClassName = "org.postgresql.Driver";
	private static final String jdbcHeader = "jdbc:postgresql:";
		
	private String URL, DB;
	private Connection conn;
	private List<Statement> openStatements = new LinkedList<>();
	
	/**
	 * Opens a new connection to the specified host and database residing on it.
	 * @param host IP or hostname of host containing database
	 * @param db name of database to connect to
	 * @throws SQLException if the URL is faulty or {@code null}
	 */
	public SimpleDatabaseConnection(String host, String db) throws SQLException {
		URL = formatURL(host, db);
		DB = db;
		initDriver();
		initConnection();
			
		log.debug("Successfully initialized " + getClass().getSimpleName() + " for database at: " + URL);
	}
	private static String formatURL(String host, String db) {
		return jdbcHeader + "//" + host + "/" + db;
	}
	private static void initDriver() {
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
	public TableConnection connect(String table) throws ClosedException {
		testClosed();
		
		try {
			return new SimpleTableConnection(this, table);
		} catch (NullTableException e) {
			return null;
		}
	}
	
	@Override
	public void close() {
		if (isClosed())	// Already closed
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
	public boolean isClosed() {
		return (conn == null && openStatements == null);
	}
	
	@Override
	public ResultSet execute(String statement) throws SQLException, ClosedException {
		return execute(statement, (RowEntry[]) null);
	}
	@Override
	public ResultSet execute(String baseStatement, RowEntry[] parameters) throws SQLException, ClosedException {
		testClosed();
		
		PreparedStatement s = setupStatement(baseStatement, parameters);
		
		ResultSet rs = s.execute() ? s.getResultSet() : null;	// ResultSet if returns one, null if otherwise
		return rs;
	}
	
	@Override
	public int update(String baseStatement, RowEntry[] parameters) throws SQLException, ClosedException {
		testClosed();
		
		PreparedStatement s = setupStatement(baseStatement, parameters);
		
		int updated = !s.execute() ? s.getUpdateCount() : 0;	// If false, returns an update count instead of result set
		return updated;
	}
	
	private PreparedStatement setupStatement(String baseStatement, RowEntry[] parameters) throws SQLException {
		PreparedStatement statement = conn.prepareStatement(baseStatement);
		openStatements.add(statement);	// Add to flushable list
		
		buildParameters(statement, parameters);	// Add appropriate types
		
		return statement;
	}
	private static PreparedStatement buildParameters(PreparedStatement statement, RowEntry[] parameters) throws SQLException {	// Inserts appropriate type into statement
		if (parameters != null && parameters.length > 0) {
			for (int i = 0; i < parameters.length; i++) {	// Prepare with appropriate types
				Object currentParameter = parameters[i].getValue();
				
				if (currentParameter instanceof Boolean)
					statement.setBoolean(i + 1, (boolean) currentParameter);
				else if (currentParameter instanceof Character)
					statement.setString(i + 1, String.valueOf((char) currentParameter));
				else if (currentParameter instanceof Double)
					statement.setDouble(i + 1, (double) currentParameter);
				else if (currentParameter instanceof Float)
					statement.setFloat(i + 1, (float) currentParameter);
				else if (currentParameter instanceof Integer)
					statement.setInt(i + 1, (int) currentParameter);
				else if (currentParameter instanceof String)
					statement.setString(i + 1, (String) currentParameter);
			}
		}
		return statement;
	}
	
	@Override
	public void flush() throws ClosedException {
		testClosed();
		
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
	public TableConnection createTable(String name, Column[] columns) throws DuplicateTableException, SQLException, ClosedException {
		testClosed();
		
		if (containsTable(name))
			throw new DuplicateTableException(DB, name);
		
		execute(StatementBuilder.buildCreate(name, columns));
		
		TableConnection newTable = null;
		try {
			newTable = new SimpleTableConnection(this, name);
		} catch (NullTableException e) {	// Should not be a null table, just created it
			log.exceptionSevere(e);
		}
		return newTable;
	}
	
	@Override
	public void dropTable(String table) throws NullTableException, SQLException, ClosedException {
		testClosed();
		
		if (!containsTable(table))	// No such table to drop
			throw new NullTableException(DB, table);
		
		execute(StatementBuilder.buildDrop(table));
	}
	
	@Override
	public boolean containsTable(String table) throws ClosedException {
		testClosed();
		
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
	public String[] getTables() throws ClosedException {
		testClosed();
		
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
	
	private void testClosed() throws ClosedException {
		if (isClosed())
			throw new ClosedException();
	}
}
