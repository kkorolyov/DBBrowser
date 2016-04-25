package dev.kkorolyov.sqlob.connection;
import java.sql.*;
import java.util.LinkedList;
import java.util.List;

import dev.kkorolyov.simplelogs.Logger;
import dev.kkorolyov.sqlob.connection.TableConnection;
import dev.kkorolyov.sqlob.construct.Column;
import dev.kkorolyov.sqlob.construct.Results;
import dev.kkorolyov.sqlob.construct.RowEntry;
import dev.kkorolyov.sqlob.exceptions.ClosedException;
import dev.kkorolyov.sqlob.exceptions.DuplicateTableException;
import dev.kkorolyov.sqlob.statement.StatementBuilder;

/**
 * Opens a connection to a single database and allows for SQL statement execution.
 */
public class DatabaseConnection implements AutoCloseable {
	private static final Logger log = Logger.getLogger(DatabaseConnection.class.getName());

	private static final String jdbcDriverClassName = "org.postgresql.Driver";
	private static final String jdbcHeader = "jdbc:postgresql:";
		
	private final String url, database;
	private Connection conn;
	private List<Statement> openStatements = new LinkedList<>();
	
	/**
	 * Opens a new connection to the specified host and database residing on it.
	 * @param host IP or hostname of host to connect to
	 * @param database name of database to connect to
	 * @throws SQLException if the URL specified is faulty or {@code null}
	 */
	public DatabaseConnection(String host, String database, String user, String password) throws SQLException {
		url = formatURL(host, database);
		this.database = database;
		
		initDriver();
		conn = DriverManager.getConnection(url, user, password);
			
		log.debug("Successfully initialized " + getClass().getSimpleName() + " for database at: " + url);
	}
	private static String formatURL(String host, String db) {
		return jdbcHeader + "//" + host + "/" + db;
	}
	private static void initDriver() {
		try {
			Class.forName(jdbcDriverClassName);
		} catch (ClassNotFoundException e) {
			log.exception(e);
		}
	}
	
	/**
	 * Connects to a table on this database, if it exists.
	 * @param table name of table to connect to
	 * @return connection to the table, if it exists, {@code null} if otherwise
	 * @throws ClosedException if called on a closed connection
	 */
	public TableConnection connect(String table) {
		testClosed();
		
		return containsTable(table) ? new TableConnection(this, table) : null;
	}
	
	/**
	 * Closes this connection and releases all resources.
	 * Has no effect if called on a closed connection.
	 */
	public void close() {
		if (isClosed())	// Already closed
			return;
		
		try {
			conn.close();	// Release JDBC resources
		} catch (SQLException e) {
			log.exception(e);
		}
		conn = null;
		openStatements = null;
		
		log.debug("Closed " + getClass().getSimpleName() + " at URL: " + url);
	}
	
	/** @return {@code true} if this connection is closed */
	public boolean isClosed() {
		return (conn == null && openStatements == null);
	}
	
	/**
	 * Executes a complete SQL statement.
	 * This version of {@code execute} does not accept extra parameters.
	 * @throws SQLException if the executed statement is invalid
	 * @throws ClosedException if called on a closed connection
	 * @see #execute(String, RowEntry[])
	 */
	public Results execute(String statement) throws SQLException {
		return execute(statement, (RowEntry[]) null);
	}
	/**
	 * Executes a partial SQL statement with extra parameters.
	 * @param baseStatement statement without parameters, with {@code ?} denoting an area where a parameter should be substituted in
	 * @param parameters values to use, will be substituted into the base statement in the order of appearance, if this array is empty or {@code null}, only the base statement is executed
	 * @return results from statement execution, or {@code null} if the statement does not return results
	 * @throws SQLException if the executed statement is invalid
	 * @throws ClosedException if called on a closed connection
	 */
	public Results execute(String baseStatement, RowEntry[] parameters) throws SQLException {
		testClosed();
		
		PreparedStatement s = setupStatement(baseStatement, parameters);
		
		Results results = s.execute() ? new Results(s.getResultSet()) : null;	// New Results from ResultSet if returns one, null if otherwise
		return results;
	}
	
	/**
	 * Executes a partial SQL update statement with parameters.
	 * @param baseStatement statement without parameters, with {@code ?} denoting an area where a parameter should be substituted in
	 * @param parameters parameters to use, will be substituted into the base statement in the order of appearance, if this array is empty or {@code null}, only the base statement is executed
	 * @return number of affected rows
	 * @throws SQLException if the executed statement is invalid
	 * @throws ClosedException if called on a closed connection
	 */
	public int update(String baseStatement, RowEntry[] parameters) throws SQLException {
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
				
				if (currentParameter instanceof Boolean)	// TODO Use something other than switching tree
					statement.setBoolean(i + 1, (boolean) currentParameter);
				
				else if (currentParameter instanceof Short)
					statement.setShort(i + 1, (short) currentParameter);
				else if (currentParameter instanceof Integer)
					statement.setInt(i + 1, (int) currentParameter);
				else if (currentParameter instanceof Long)
					statement.setLong(i + 1, (long) currentParameter);
				else if (currentParameter instanceof Float)
					statement.setFloat(i + 1, (float) currentParameter);
				else if (currentParameter instanceof Double)
					statement.setDouble(i + 1, (double) currentParameter);
				
				else if (currentParameter instanceof Character)
					statement.setString(i + 1, String.valueOf((char) currentParameter));
				else if (currentParameter instanceof String)
					statement.setString(i + 1, (String) currentParameter);
				
				log.debug("Adding parameter " + i + ": " + currentParameter.toString());
			}
		}
		return statement;
	}
	
	/**
	 * Closes all opened statements.
	 * @throws ClosedException if called on a closed connection
	 */
	public void flush() {
		testClosed();
		
		int closedStatements = 0;	// Count closed statements for debugging
		for (Statement openStatement : openStatements) {
			try {
				openStatement.close();
				closedStatements++;
			} catch (SQLException e) {
				log.exception(e);
			}
		}
		openStatements.clear();
		
		log.debug("Closed " + closedStatements + " statements");
	}
	
	/**
	 * Creates a table with the specifed name and columns.
	 * @param name new table name
	 * @param columns new table columns in the order they should appear
	 * @throws DuplicateTableException if a table of the specified name already exists
	 * @return connection to the newly-created table
	 * @throws ClosedException if called on a closed connection
	 */
	public TableConnection createTable(String name, Column[] columns) throws DuplicateTableException {
		testClosed();
		
		if (containsTable(name))	// Can't add a table of the same name
			throw new DuplicateTableException(database, name);
				
		TableConnection newTable = null;
		try {
			execute(StatementBuilder.buildCreate(name, columns));
			newTable = new TableConnection(this, name);
		} catch (SQLException e) {	// Should not result in bad statement
			log.exception(e);
		}
		return newTable;
	}
	
	/**
	 * Drops a table of the specified name from the database.
	 * @param table name of table to drop
	 * @return {@code true} if table dropped successfully, {@code false} if drop failed or no such table
	 * @throws ClosedException if called on a closed connection
	 */
	public boolean dropTable(String table) {
		testClosed();
		
		boolean success = false;
		
		if (containsTable(table)) {
			try {
				execute(StatementBuilder.buildDrop(table));
				
				success = true;
			} catch (SQLException e) {
				log.exception(e);
			}
		}
		return success;
	}
	
	/**
	 * @param table name of table to search for
	 * @return {@code true} if this database contains a table of the specified name (ignoring case)
	 * @throws ClosedException if called on a closed connection
	 */
	public boolean containsTable(String table) {
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
	
	/**
	 * @return names of all tables in this database.
	 * @throws ClosedException if called on a closed connection
	 */
	public String[] getTables() {
		testClosed();
		
		List<String> tables = new LinkedList<>();
		try (ResultSet tableSet = conn.getMetaData().getTables(null, null, "%", new String[]{"TABLE"})) {
			while (tableSet.next()) {
				tables.add(tableSet.getString(3));
			}
			tableSet.close();
		} catch (SQLException e) {
			log.exception(e);
		}
		return tables.toArray(new String[tables.size()]);
	}
	
	/**
	 * Returns the name of this database.
	 * May be called on a closed connection.
	 * @return name of this database
	 */
	public String getDatabaseName() {
		return database;
	}
	
	private void testClosed() {
		if (isClosed())
			throw new ClosedException();
	}
}
