package dev.kkorolyov.jdbmanager.connection.concrete;
import java.sql.*;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import dev.kkorolyov.jdbmanager.connection.DBConnection;
import dev.kkorolyov.jdbmanager.strings.Strings;

/**
 * {@code DBConnection} implementation which automatically casts statement parameters to the appropriate type.
 * @see DBConnection
 */
public class AutoCastingDBConnection implements DBConnection {
	private static final Logger LOG = Logger.getLogger(AutoCastingDBConnection.class.getName());
	private static final String JDBC_HEADER = "jdbc:postgresql:";
	
	private String URL, DB;
	private Connection conn;
	private List<Statement> openStatements = new LinkedList<>();
	
	/**
	 * Opens a new connection to the specified host and database residing on it.
	 * @param host IP or hostname of host containing database
	 * @param db name of database to connect to
	 */
	public AutoCastingDBConnection(String host, String db) {
		URL = formatURL(host, db);
		DB = db;
		try {
			initDriver();
			initConnection();
			
			LOG.info("Successfully initialized " + getClass().getSimpleName() + " for database at: " + URL);
		} catch (ClassNotFoundException | SQLException e) {
			LOG.log(Level.SEVERE, e.getMessage(), e);
		}
	}
	private static String formatURL(String host, String db) {
		return JDBC_HEADER + "//" + host + "/" + db;
	}
	private void initDriver() throws ClassNotFoundException {
		Class.forName("org.postgresql.Driver");
	}
	private void initConnection() throws SQLException {
		conn = DriverManager.getConnection(URL, Strings.USER, "");
	}
	
	@Override
	public void close() {		
		if (conn == null && openStatements == null)	// Already closed
			return;
		
		try {
			conn.close();
		} catch (SQLException e) {
			LOG.log(Level.SEVERE, e.getMessage(), e);
		}
		conn = null;
		openStatements = null;
	}
	
	@Override
	public ResultSet execute(String statement) throws SQLException {
		return execute(statement, (Object[]) null);
	}
	@Override
	public ResultSet execute(String baseStatement, Object... parameters) throws SQLException {
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
		int closedStatements = 0;
		for (Statement openStatement : openStatements) {
			try {
				openStatement.close();
				closedStatements++;
			} catch (SQLException e) {
				LOG.log(Level.SEVERE, e.getMessage(), e);
			}
		}
		openStatements.clear();
		LOG.info("Closed " + closedStatements + " statements");
	}
	
	@Override
	public boolean containsTable(String table) {
		boolean contains = false;
		
		for (String dbTable : getTables()) {
			if (dbTable.equals(table)) {
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
			LOG.log(Level.SEVERE, e.getMessage(), e);
		}
		return tables.toArray(new String[tables.size()]);
	}
	
	@Override
	public String getDBName() {
		return DB;
	}
}
