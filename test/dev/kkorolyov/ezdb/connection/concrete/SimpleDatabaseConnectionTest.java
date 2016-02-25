package dev.kkorolyov.ezdb.connection.concrete;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.SQLException;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import dev.kkorolyov.ezdb.connection.DatabaseConnection;
import dev.kkorolyov.ezdb.construct.Column;
import dev.kkorolyov.ezdb.construct.RowEntry;
import dev.kkorolyov.ezdb.construct.SqlType;
import dev.kkorolyov.ezdb.exceptions.ClosedException;
import dev.kkorolyov.ezdb.exceptions.DuplicateTableException;
import dev.kkorolyov.ezdb.exceptions.MismatchedTypeException;
import dev.kkorolyov.ezdb.exceptions.NullTableException;
import dev.kkorolyov.ezdb.logging.DebugLogger;
import dev.kkorolyov.ezdb.properties.Properties;

@SuppressWarnings("javadoc")
public class SimpleDatabaseConnectionTest {
	private static final String host = Properties.getValue(Properties.HOST), database = "TEST_DB", user = Properties.getValue(Properties.USER), password = Properties.getValue(Properties.PASSWORD),table = "DATABASE_TEST_TABLE";
	
	private DatabaseConnection conn;
	private Column[] columns = {new Column("TEST_COL_1", SqlType.BOOLEAN)};
	
	@BeforeClass
	public static void setUpBeforeClass() {
		DebugLogger.enableAll();
	}
	@Before
	public void setUp() throws SQLException, DuplicateTableException, NullTableException, ClosedException {
		conn = new SimpleDatabaseConnection(host, database, user, password);	// Use a fresh connection for each test
		
		if (conn.containsTable(table))
			conn.dropTable(table);
		conn.createTable(table, columns);
	}
	@After
	public void tearDown() throws NullTableException, SQLException, ClosedException {
		if (conn.isClosed())
			conn = new SimpleDatabaseConnection(host, database, user, password);
		conn.dropTable(table);
		conn.close();	// Make sure all resources release after each test
	}
	
	@Test
	public void testClose() throws SQLException, ClosedException {
		String validityStatement = "SELECT";	// Will work as long as connection is open and valid
		try {
			conn.execute(validityStatement);	// Connection is open
		} catch (SQLException e) {
			fail("Statement execution failed");
		}
		conn.close();
		
		try {
			conn.execute(validityStatement);
		} catch (ClosedException e) {
			return;	// As expected
		}
		fail("Resources failed to close");
	}

	@Test
	public void testExecute() throws SQLException, ClosedException {	// Mainly for exceptions
		String testStatement = "SELECT * FROM " + table;
		conn.execute(testStatement);
	}
	@Test
	public void testExecuteParams() throws SQLException, ClosedException, MismatchedTypeException {	// Mainly for exceptions
		String testStatement = "SELECT " + columns[0].getName() + " FROM " + table + " WHERE " + columns[0].getName() + "=?";
		conn.execute(testStatement, new RowEntry[]{new RowEntry(columns[0], false)});
	}
	
	@Test
	public void testCreateTable() throws DuplicateTableException, NullTableException, SQLException, ClosedException {
		String testTableName = "TEST_TABLE_CREATE";
		
		if (conn.containsTable(testTableName))	// Clear stale test table from a previous run, if exists
			conn.dropTable(testTableName);
		
		Column[] columns = buildAllColumns();
		
		assertTrue(!conn.containsTable(testTableName));
		conn.createTable(testTableName, columns);
		assertTrue(conn.containsTable(testTableName));
		
		conn.dropTable(testTableName);	// Cleanup
	}
	@Test
	public void testDropTable() throws DuplicateTableException, NullTableException, SQLException, ClosedException {
		String testTableName = "TEST_TABLE_DROP";
		
		if (conn.containsTable(testTableName))	// Clear stale test table from a previous run, if exists
			conn.dropTable(testTableName);
		
		conn.createTable(testTableName, new Column[]{new Column("TEST_COLUMN", SqlType.BOOLEAN)});
		
		assertTrue(conn.containsTable(testTableName));
		conn.dropTable(testTableName);
		assertTrue(!conn.containsTable(testTableName));
	}
	
	@Test
	public void testContainsTable() throws DuplicateTableException, SQLException, NullTableException, ClosedException {
		String testTableName = "TEST_TABLE_CONTAINS";
		
		if (conn.containsTable(testTableName))
			conn.dropTable(testTableName);
		
		assertTrue(!conn.containsTable(testTableName));
		conn.createTable(testTableName, new Column[]{new Column("TEST_COLUMN", SqlType.BOOLEAN)});
		assertTrue(conn.containsTable(testTableName));
		
		conn.dropTable(testTableName);
	}
	
	@Test
	public void testGetTables() throws ClosedException {
		for (String table : conn.getTables()) {
			System.out.println(table);
		}
	}
	
	private static Column[] buildAllColumns() {
		SqlType[] allTypes = SqlType.values();
		Column[] allColumns = new Column[allTypes.length];
		
		for (int i = 0; i < allColumns.length; i++)
			allColumns[i] = new Column(allTypes[i].toString(), allTypes[i]);
		
		return allColumns;
	}
}
