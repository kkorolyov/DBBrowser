package dev.kkorolyov.sqlob.connection;
import java.sql.*;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import dev.kkorolyov.sqlob.construct.Column;
import dev.kkorolyov.sqlob.construct.Results;
import dev.kkorolyov.sqlob.construct.RowEntry;
import dev.kkorolyov.sqlob.construct.statement.QueryStatement;
import dev.kkorolyov.sqlob.construct.statement.StatementFactory;
import dev.kkorolyov.sqlob.construct.statement.UpdateStatement;
import dev.kkorolyov.sqlob.logging.Logger;
import dev.kkorolyov.sqlob.logging.LoggerInterface;

/**
 * A connection to a SQL database.
 * Provides methods for SQL statement execution.
 */
public class DatabaseConnection implements AutoCloseable {
	private static final LoggerInterface log = Logger.getLogger(DatabaseConnection.class.getName());

	private final String database;
	private final DatabaseType databaseType;
	private Connection conn;
	private Set<Statement> openStatements = new HashSet<>();
	private StatementLog statementLog = new StatementLog();
	private StatementFactory statementFactory = new StatementFactory(this);
	
	/**
	 * Opens a new connection to the specified host and database residing on it.
	 * @param host IP or hostname of host to connect to
	 * @param database name of database to connect to
	 * @param databaseType type of database to connect to
	 * @param user user for database connection
	 * @param password password for database connection
	 * @throws SQLException if the URL specified is faulty or {@code null}
	 */
	public DatabaseConnection(String host, String database, DatabaseType databaseType, String user, String password) throws SQLException {
		this.database = database;
		this.databaseType = databaseType;
		
		initDriver(databaseType);
		String url = databaseType.getURL(host, database);
		conn = DriverManager.getConnection(url, user, password);
			
		log.debug("Successfully initialized " + databaseType + " " + getClass().getSimpleName() + ": " + hashCode() + " for database at: " + url);
	}
	private static void initDriver(DatabaseType type) {
		try {
			Class.forName(type.getDriverClassName());
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
		assertNotClosed();
		
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
			log.exception(e);	// Nothing to do for close() exception
		}
		conn = null;
		
		log.debug("Closed " + databaseType + " " + getClass().getSimpleName() + ": " + hashCode());
	}
	
	/** @return {@code true} if this connection is closed */
	public boolean isClosed() {
		return (conn == null);
	}
	private void assertNotClosed() {
		if (isClosed())
			throw new ClosedException();
	}
	
	/**
	 * Executes and logs the specified statement.
	 * @param statement statement to execute
	 * @return results from statement execution, or {@code null} if the statement does not return results
	 * @throws UncheckedSQLException if the executed statement is invalid
	 * @throws ClosedException if called on a closed connection
	 */
	public Results execute(QueryStatement statement) {
		assertNotClosed();
		
		statementLog.add(statement);
		
		return statement.execute();
	}
	/**
	 * Executes and logs the specified statement.
	 * @param statement statement to execute
	 * @return number of rows affected by statement execution
	 * @throws UncheckedSQLException if the executed statement is invalid
	 * @throws ClosedException if called on a closed connection
	 */
	public int execute(UpdateStatement statement) {
		assertNotClosed();

		statementLog.add(statement);
		
		return statement.execute();
	}
	
	/**
	 * Executes a SQL statement with substituted parameters.
	 * @param baseStatement base SQL statement with '?' denoting an area for parameter substitution
	 * @param parameters parameters to substitute in order of declaration
	 * @return results of statement execution, or {@code null} if statement does not return results
	 */
	public Results execute(String baseStatement, List<RowEntry> parameters) {
		assertNotClosed();
		
		Results results = null;
		
		try {
			PreparedStatement s = buildStatement(baseStatement, parameters);	// Remains open to not close results
			openStatements.add(s);
			
			if (s.execute())
				results = new Results(s.getResultSet());
		} catch (SQLException e) {
			throw new UncheckedSQLException(e);
		}
		return results;
	}
	/**
	 * Executes a SQL statement with substituted parameters.
	 * @param baseStatement base SQL statement with '?' denoting an area for parameter substitution
	 * @param parameters parameters to substitute in order of declaration
	 * @return number of rows affected by statement execution
	 */
	public int update(String baseStatement, List<RowEntry> parameters) {
		assertNotClosed();
		closeAllStatements();	// Close open resources before modifying
		
		int updated = 0;
		
		try (PreparedStatement s = buildStatement(baseStatement, parameters)) {
			if (!s.execute())
				updated = s.getUpdateCount();
		} catch(SQLException e) {
			throw new UncheckedSQLException(e);
		}
		return updated;
	}
		
	private PreparedStatement buildStatement(String baseStatement, List<RowEntry> parameters) throws SQLException {	// Inserts appropriate type into statement
		PreparedStatement statement = conn.prepareStatement(baseStatement);
		
		if (parameters != null && parameters.size() > 0) {
			for (int i = 0; i < parameters.size(); i++) {	// Prepare with appropriate types
				Object currentParameter = parameters.get(i).getValue();
				
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
	 * Creates a table with the specifed name and columns.
	 * @param name new table name
	 * @param columns new table columns in the order they should appear
	 * @return connection to the created table
	 * @throws DuplicateTableException if a table of the specified name already exists
	 * @throws ClosedException if called on a closed connection
	 */
	public TableConnection createTable(String name, List<Column> columns) throws DuplicateTableException {
		assertNotClosed();
		
		if (containsTable(name))	// Can't add a table of the same name
			throw new DuplicateTableException(database, name);
				
		execute(statementFactory.getCreateTable(name, columns));
		
		return new TableConnection(this, name);
	}
	
	/**
	 * Drops a table of the specified name from the database.
	 * @param table name of table to drop
	 * @return {@code true} if table dropped successfully, {@code false} if drop failed or no such table
	 * @throws ClosedException if called on a closed connection
	 */
	public boolean dropTable(String table) {
		assertNotClosed();
		closeAllStatements();
		
		boolean success = false;
		
		if (containsTable(table)) {
			execute(statementFactory.getDropTable(table));
				
			success = true;
		}
		return success;
	}
	
	/**
	 * @param table name of table to search for
	 * @return {@code true} if the database contains a table of the specified name (ignoring case)
	 * @throws ClosedException if called on a closed connection
	 */
	public boolean containsTable(String table) {
		assertNotClosed();
		
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
	 * @return names of all tables in the database.
	 * @throws ClosedException if called on a closed connection
	 */
	public List<String> getTables() {
		assertNotClosed();
		
		List<String> tables = new LinkedList<>();
		try (ResultSet tableSet = conn.getMetaData().getTables(null, null, "%", new String[]{"TABLE"})) {
			while (tableSet.next()) {
				tables.add(tableSet.getString(3));
			}
			tableSet.close();
		} catch (SQLException e) {
			throw new UncheckedSQLException(e);
		}
		return tables;
	}
	
	/**
	 * Returns the name of the database.
	 * May be called on a closed connection.
	 * @return name of database
	 */
	public String getDatabaseName() {
		return database;
	}
	/**
	 * Returns the type of the database.
	 * May be called on a closed connection.
	 * @return type of database
	 */
	public DatabaseType getDatabaseType() {
		return databaseType;
	}
	
	/**
	 * Returns the statement log associated with this connection.
	 * May be called on a closed connection.
	 * @return associated statement log
	 */
	public StatementLog getStatementLog() {
		return statementLog;
	}
	
	/**
	 * @return a {@code StatementFactory} used to produce statements for execution by this connection
	 * @throws ClosedException if called on a closed connection
	 */
	public StatementFactory getStatementFactory() {
		return statementFactory;
	}
	
	private void closeAllStatements() {
		for (@SuppressWarnings("resource") Statement statement : openStatements) {
			try {
				statement.close();
			} catch (SQLException e) {
				throw new UncheckedSQLException(e);
			}
		}
		openStatements.clear();
	}
	
	/**
	 * Represents a specific database type.
	 */
	public static enum DatabaseType {
		/** The {@code PostgreSQL} database */
		POSTGRESQL("org.postgresql.Driver", "jdbc:postgresql://" + Marker.HOST + "/" + Marker.DATABASE),
		/** The {@code SQLite} database */
		SQLITE("org.sqlite.JDBC", "jdbc:sqlite:" + Marker.DATABASE);
		
		private String 	driverClassName,
										baseURL;
		
		private DatabaseType(String driverClassName, String header) {
			this.driverClassName = driverClassName;
			this.baseURL = header;
		}
		
		/**
		 * Returns a type-specific URL to a database.
		 * @param host hostname or address of database host, may be empty
		 * @param database database name
		 * @return appropriate URL
		 */
		public String getURL(String host, String database) {
			return baseURL.replaceFirst(Marker.HOST, host).replaceFirst(Marker.DATABASE, database);
		}
		
		/** @return name of the JDBC driver class for this database type */
		public String getDriverClassName() {
			return driverClassName;
		}
		
		private static class Marker {
			private static final String HOST = "<HOST>",
																	DATABASE = "<DATABASE>";
		}
	}
}
