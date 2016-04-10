package dev.kkorolyov.sqlob.connection.concrete;
import java.sql.*;
import java.util.LinkedList;
import java.util.List;

import dev.kkorolyov.simplelogs.Logger;
import dev.kkorolyov.sqlob.connection.DatabaseConnection;
import dev.kkorolyov.sqlob.connection.TableConnection;
import dev.kkorolyov.sqlob.construct.Column;
import dev.kkorolyov.sqlob.construct.Results;
import dev.kkorolyov.sqlob.construct.RowEntry;
import dev.kkorolyov.sqlob.exceptions.ClosedException;
import dev.kkorolyov.sqlob.exceptions.DuplicateTableException;
import dev.kkorolyov.sqlob.exceptions.NullTableException;
import dev.kkorolyov.sqlob.statement.StatementBuilder;

/**
 * A simple {@code DBConnection} implementation.
 * This object will automatically release all resources upon exiting a {@code try-with-resources} block;
 * @see DatabaseConnection
 */
public class SimpleDatabaseConnection implements DatabaseConnection, AutoCloseable {
	private static final Logger log = Logger.getLogger(SimpleDatabaseConnection.class.getName());

	private static final String jdbcDriverClassName = "org.postgresql.Driver";
	private static final String jdbcHeader = "jdbc:postgresql:";
		
	private final String url, database;
	private Connection conn;
	private List<Statement> openStatements = new LinkedList<>();
	
	/**
	 * Opens a new connection to the specified host and database residing on it.
	 * @param host IP or hostname of host containing database
	 * @param database name of database to connect to
	 * @throws SQLException if the URL is faulty or {@code null}
	 */
	public SimpleDatabaseConnection(String host, String database, String user, String password) throws SQLException {
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
			log.exception(e);
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
	public Results execute(String statement) throws SQLException, ClosedException {
		return execute(statement, (RowEntry[]) null);
	}
	@Override
	public Results execute(String baseStatement, RowEntry[] parameters) throws SQLException, ClosedException {
		testClosed();
		
		PreparedStatement s = setupStatement(baseStatement, parameters);
		
		Results results = s.execute() ? new Results(s.getResultSet()) : null;	// New Results from ResultSet if returns one, null if otherwise
		return results;
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
	public void flush() throws ClosedException {
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
	
	@Override
	public TableConnection createTable(String name, Column[] columns) throws DuplicateTableException, ClosedException {
		testClosed();
		
		if (containsTable(name))	// Can't add a table of the same name
			throw new DuplicateTableException(database, name);
				
		TableConnection newTable = null;
		try {
			execute(StatementBuilder.buildCreate(name, columns));
			newTable = new SimpleTableConnection(this, name);
		} catch (NullTableException | SQLException e) {	// Should not be a null table or result in bad statement
			log.exception(e);
		}
		return newTable;
	}
	
	@Override
	public void dropTable(String table) throws NullTableException, ClosedException {
		testClosed();
		
		if (!containsTable(table))	// No such table to drop
			throw new NullTableException(database, table);
		
		try {
			execute(StatementBuilder.buildDrop(table));
		} catch (SQLException e) {
			log.exception(e);
		}
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
	
	@Override
	public String getDBName() {
		return database;
	}
	
	private void testClosed() throws ClosedException {
		if (isClosed())
			throw new ClosedException();
	}
}
