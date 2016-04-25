package dev.kkorolyov.sqlob.connection;

import static org.junit.Assert.*;

import java.sql.SQLException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import dev.kkorolyov.simpleprops.Properties;
import dev.kkorolyov.sqlob.construct.Column;
import dev.kkorolyov.sqlob.construct.Results;
import dev.kkorolyov.sqlob.construct.RowEntry;
import dev.kkorolyov.sqlob.construct.SqlType;
import dev.kkorolyov.sqlob.exceptions.ClosedException;

@SuppressWarnings("javadoc")
public class DatabaseConnectionTest {
	private static Properties props = Properties.getInstance("SimpleProps.txt");
	private static final String HOST_IP_ADDRESS = props.getValue("HOST"),
															DATABASE_NAME = "test_database",
															USER_NAME = props.getValue("USER"),
															USER_PASSWORD = props.getValue("PASSWORD");	
	
	private DatabaseConnection conn;
	
	@Before
	public void setUp() throws Exception {
		conn = new DatabaseConnection(HOST_IP_ADDRESS, DATABASE_NAME, USER_NAME, USER_PASSWORD);	// Use a fresh connection for each test
	}
	@After
	public void tearDown() throws Exception {
		conn.close();	// Make sure all resources release after each test
	}
	
	@Test
	public void testConnect() {
		String testTable = "TestTable_Connect";
		Column[] testColumns = new Column[]{new Column("TestColumn1", SqlType.CHAR)};
		
		refreshTable(testTable, testColumns);
		
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
		String testTable = "TestTable_Execute";
		Column[] testColumns = new Column[]{new Column("TestColumn1", SqlType.BOOLEAN)};
		
		refreshTable(testTable, testColumns);
		
		String 	returnTestStatement = "SELECT * FROM " + testTable,
						nullReturnTestStatement = "INSERT INTO " + testTable + " VALUES (true)";
		
		assertTrue(conn.execute(returnTestStatement) != null);
		assertTrue(conn.execute(nullReturnTestStatement) == null);
		
		conn.dropTable(testTable);
	}
	@Test
	public void testExecuteParams() throws Exception {	// Mainly for exceptions
		String testTable = "TestTable_ExecuteParams";
		Column[] testColumns = new Column[]{new Column("TestColumn1", SqlType.BOOLEAN)};
		
		refreshTable(testTable, testColumns);

		String testStatement = "SELECT * FROM " + testTable + " WHERE " + testColumns[0].getName() + "=?";
		Results results = conn.execute(testStatement, new RowEntry[]{new RowEntry(testColumns[0], false)});
		
		assertTrue(results != null);
		
		conn.dropTable(testTable);
	}
	
	@Test
	public void testUpdate() throws Exception {
		String testTable = "TestTable_Update";
		Column[] testColumns = new Column[]{new Column("TestColumn1", SqlType.BOOLEAN)};
		
		refreshTable(testTable, testColumns);
		
		String testStatement = "INSERT INTO " + testTable + " VALUES (?)";
		int resultsCount = conn.update(testStatement, new RowEntry[]{new RowEntry(testColumns[0], false)});
		
		assertEquals(1, resultsCount);
		
		conn.dropTable(testTable);
	}
	
	@Test
	public void testCreateTable() throws Exception {
		String testTable = "TestTable_Create";
		Column[] testColumns = new Column[]{new Column("TestColumn1", SqlType.BOOLEAN)};

		conn.dropTable(testTable);
		
		assertTrue(!conn.containsTable(testTable));
		
		conn.createTable(testTable, testColumns);
		assertTrue(conn.containsTable(testTable));
		
		conn.dropTable(testTable);
	}
	
	@Test
	public void testDropTable() throws Exception {
		String testTable = "TestTable_Drop";
		Column[] testColumns = new Column[]{new Column("TestColumn1", SqlType.BOOLEAN)};
		
		conn.dropTable(testTable);
		
		conn.createTable(testTable, testColumns);		
		assertTrue(conn.containsTable(testTable));
		
		conn.dropTable(testTable);
		assertTrue(!conn.containsTable(testTable));
	}
	
	@Test
	public void testContainsTable() throws Exception {
		String testTable = "TestTable_Contains";
		Column[] testColumns = new Column[]{new Column("TestColumn1", SqlType.BOOLEAN)};

		conn.dropTable(testTable);		
		assertTrue(!conn.containsTable(testTable));
		
		conn.createTable(testTable, testColumns);		
		assertTrue(conn.containsTable(testTable));
		
		conn.dropTable(testTable);
	}
	
	@Test
	public void testGetTables() throws Exception {
		for (String table : conn.getTables()) {
			conn.dropTable(table);
		}
		assertEquals(0, conn.getTables().length);
		
		int numTestTables = 5;
		String[] testTables = new String[numTestTables];
		Column[][] testColumnses = new Column[numTestTables][];	// What's the plural of "columns"?
		for (int i = 0; i < numTestTables; i++) {
			testTables[i] = "TestTable_GetTables" + i;
			testColumnses[i] = new Column[]{new Column("TestColumn1", SqlType.BOOLEAN)};
			
			conn.createTable(testTables[i], testColumnses[i]);
			assertEquals(i + 1, conn.getTables().length);
		}
		assertEquals(numTestTables, conn.getTables().length);
		
		for (String testTable : testTables)
			conn.dropTable(testTable);
		
		assertEquals(0, conn.getTables().length);
	}
	
	@Test
	public void testGetDatabaseName() {
		assertEquals(DATABASE_NAME, conn.getDBName());
	}
	
	private void refreshTable(String table, Column[] columns) {
		conn.dropTable(table);
		conn.createTable(table, columns);
	}
}
