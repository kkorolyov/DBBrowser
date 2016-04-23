package dev.kkorolyov.sqlob.connection.concrete;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.SQLException;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import dev.kkorolyov.simpleprops.Properties;
import dev.kkorolyov.sqlob.connection.DatabaseConnection;
import dev.kkorolyov.sqlob.construct.Column;
import dev.kkorolyov.sqlob.construct.RowEntry;
import dev.kkorolyov.sqlob.construct.SqlType;
import dev.kkorolyov.sqlob.exceptions.ClosedException;

@SuppressWarnings("javadoc")
public class SimpleDatabaseConnectionTest {
	private static Properties props = Properties.getInstance("SimpleProps.txt");
	private static final String HOST_IP_ADDRESS = props.getValue("HOST"),
															DATABASE_NAME = props.getValue("DATABASE"),
															USER_NAME = props.getValue("USER"),
															USER_PASSWORD = props.getValue("PASSWORD"),
															TABLE_NAME = props.getValue("TABLE");
	
	private DatabaseConnection conn;
	private Column[] columns = {new Column("TEST_COL_1", SqlType.BOOLEAN)};
	
	@BeforeClass
	public static void setUpBeforeClass() {
		// TODO Set up proprietary test table
	}
	@Before
	public void setUp() throws Exception {
		conn = new SimpleDatabaseConnection(HOST_IP_ADDRESS, DATABASE_NAME, USER_NAME, USER_PASSWORD);	// Use a fresh connection for each test
		
		if (conn.containsTable(TABLE_NAME))
			conn.dropTable(TABLE_NAME);
		conn.createTable(TABLE_NAME, columns);
	}
	@After
	public void tearDown() throws Exception {
		if (conn.isClosed())
			conn = new SimpleDatabaseConnection(HOST_IP_ADDRESS, DATABASE_NAME, USER_NAME, USER_PASSWORD);
		conn.dropTable(TABLE_NAME);
		conn.close();	// Make sure all resources release after each test
	}
	
	@Test
	public void testConnect() {
		String testTable = "TestConnectTestTable";
		Column[] testColumns = new Column[]{new Column("TestColumn1", SqlType.CHAR)};
		
		conn.dropTable(testTable);
		conn.createTable(testTable, testColumns);
		
		assertTrue(conn.connect(testTable) != null);
		
		conn.dropTable(testTable);	// Clean up
	}
	
	@Test
	public void testClose() throws Exception {
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
	public void testIsClosed() {
		assertTrue(!conn.isClosed());
		
		conn.close();
		
		assertTrue(conn.isClosed());
	}

	@Test
	public void testExecute() throws Exception {	// Mainly for exceptions
		String testStatement = "SELECT * FROM " + TABLE_NAME;
		conn.execute(testStatement);
	}
	@Test
	public void testExecuteParams() throws Exception {	// Mainly for exceptions
		String testStatement = "SELECT " + columns[0].getName() + " FROM " + TABLE_NAME + " WHERE " + columns[0].getName() + "=?";
		conn.execute(testStatement, new RowEntry[]{new RowEntry(columns[0], false)});
	}
	
	@Test
	public void testCreateTable() throws Exception {
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
	public void testDropTable() throws Exception {
		String testTableName = "TEST_TABLE_DROP";
		
		if (conn.containsTable(testTableName))	// Clear stale test table from a previous run, if exists
			conn.dropTable(testTableName);
		
		conn.createTable(testTableName, new Column[]{new Column("TEST_COLUMN", SqlType.BOOLEAN)});
		
		assertTrue(conn.containsTable(testTableName));
		conn.dropTable(testTableName);
		assertTrue(!conn.containsTable(testTableName));
	}
	
	@Test
	public void testContainsTable() throws Exception {
		String testTableName = "TEST_TABLE_CONTAINS";
		
		if (conn.containsTable(testTableName))
			conn.dropTable(testTableName);
		
		assertTrue(!conn.containsTable(testTableName));
		conn.createTable(testTableName, new Column[]{new Column("TEST_COLUMN", SqlType.BOOLEAN)});
		assertTrue(conn.containsTable(testTableName));
		
		conn.dropTable(testTableName);
	}
	
	@Test
	public void testGetTables() throws Exception {
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
