package dev.kkorolyov.sqlob.connection;
import java.sql.*;
import java.util.LinkedList;
import java.util.List;

import dev.kkorolyov.sqlob.logging.Logger;
import dev.kkorolyov.sqlob.logging.LoggerInterface;
import dev.kkorolyov.sqlob.connection.TableConnection;
import dev.kkorolyov.sqlob.construct.Column;
import dev.kkorolyov.sqlob.construct.Results;
import dev.kkorolyov.sqlob.construct.RowEntry;
import dev.kkorolyov.sqlob.statement.StatementBuilder;

/**
 * A {@code DatabaseConnection} to a PostgreSQL database.
 */
public class PostgresDatabaseConnection implements DatabaseConnection, AutoCloseable {
	private static final LoggerInterface log = Logger.getLogger(PostgresDatabaseConnection.class.getName());

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
	public PostgresDatabaseConnection(String host, String database, String user, String password) throws SQLException {
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
	
	@Override
	public TableConnection connect(String table) {
		testClosed();
		
		return containsTable(table) ? new TableConnection(this, table) : null;
	}
	
	/**
	 * Closes this connection and releases all resources.
	 * Has no effect if called on a closed connection.
	 */
	@Override
	public void close() {
		if (isClosed())	// Already closed
			return;
		
		try {
			conn.close();	// Release JDBC resources
		} catch (SQLException e) {
			log.exception(e);	// Nothing to do for close() exception
		}
		conn = null;
		openStatements = null;
		
		log.debug("Closed " + getClass().getSimpleName() + " at URL: " + url);
	}
	
	@Override
	public boolean isClosed() {
		return (conn == null && openStatements == null);
	}
	
	@Override
	public Results execute(String statement) {
		return execute(statement, (RowEntry[]) null);
	}
	@Override
	public Results execute(String baseStatement, RowEntry[] parameters) {
		testClosed();
		
		Results results = null;
		
		try {
			PreparedStatement s = setupStatement(baseStatement, parameters);

			if (s.execute())
				results = new Results(s.getResultSet());
		} catch (SQLException e) {
			throw new UncheckedSQLException(e);
		}
		return results;
	}
	
	@Override
	public int update(String baseStatement, RowEntry[] parameters) {
		testClosed();
		
		int updated = 0;
		
		try {
			PreparedStatement s = setupStatement(baseStatement, parameters);

			if (!s.execute())
				updated = s.getUpdateCount();
		} catch(SQLException e) {
			throw new UncheckedSQLException(e);
		}
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
	
	@Override
	public void flush() {
		testClosed();
		
		int closedStatements = 0;	// Count closed statements for debugging
		for (Statement openStatement : openStatements) {
			try {
				openStatement.close();
				closedStatements++;
			} catch (SQLException e) {
				log.exception(e);	// Nothing to do for close() exception
			}
		}
		openStatements.clear();
		
		log.debug("Closed " + closedStatements + " statements");
	}
	
	@Override
	public TableConnection createTable(String name, Column[] columns) throws DuplicateTableException {
		testClosed();
		
		if (containsTable(name))	// Can't add a table of the same name
			throw new DuplicateTableException(database, name);
				
		execute(StatementBuilder.buildCreate(name, columns));
		
		return new TableConnection(this, name);
	}
	
	@Override
	public boolean dropTable(String table) {
		testClosed();
		
		boolean success = false;
		
		if (containsTable(table)) {
			execute(StatementBuilder.buildDrop(table));
				
			success = true;
		}
		return success;
	}
	
	@Override
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
	
	@Override
	public String[] getTables() {
		testClosed();
		
		List<String> tables = new LinkedList<>();
		try (ResultSet tableSet = conn.getMetaData().getTables(null, null, "%", new String[]{"TABLE"})) {
			while (tableSet.next()) {
				tables.add(tableSet.getString(3));
			}
			tableSet.close();
		} catch (SQLException e) {
			throw new UncheckedSQLException(e);
		}
		return tables.toArray(new String[tables.size()]);
	}
	
	@Override
	public String getDatabaseName() {
		return database;
	}
	
	private void testClosed() {
		if (isClosed())
			throw new ClosedException();
	}
}
