package dev.kkorolyov.ezdb.connection.concrete;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.SQLException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import dev.kkorolyov.ezdb.connection.DatabaseConnection;
import dev.kkorolyov.ezdb.construct.Column;
import dev.kkorolyov.ezdb.construct.SqlType;
import dev.kkorolyov.ezdb.exceptions.ClosedException;
import dev.kkorolyov.ezdb.exceptions.DuplicateTableException;
import dev.kkorolyov.ezdb.exceptions.NullTableException;

@SuppressWarnings("javadoc")
public class SimpleDatabaseConnectionTest {
	private static final String TEST_HOST = "192.168.1.157", TEST_DB = "TEST_DB", TEST_USER = "postgres", TEST_PASSWORD = "";
	
	private DatabaseConnection conn;
	
	@Before
	public void setUp() throws SQLException, DuplicateTableException, NullTableException {		
		conn = new SimpleDatabaseConnection(TEST_HOST, TEST_DB, TEST_USER, TEST_PASSWORD);	// Use a fresh connection for each test
	}
	@After
	public void tearDown() throws NullTableException, SQLException {	
		conn.close();	// Make sure all resources release after each test
		conn = null;
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
			return;
		}
		fail("Resources failed to nullify");
	}

	@Test
	public void testExecute() {
		fail("Unimplemented");
	}
	@Test
	public void testExecuteParams() {
		fail("Unimplemented");
	}
	
	@Test
	public void testCreateTable() throws DuplicateTableException, NullTableException, SQLException, ClosedException {
		String testTable = "TEST_TABLE_CREATE";
		
		if (conn.containsTable(testTable))	// Clear stale test table from a previous run, if exists
			conn.dropTable(testTable);
		
		SqlType[] typeValues = SqlType.values();
		Column[] testColumns = new Column[typeValues.length];	// Test all column types
		for (int i = 0; i < testColumns.length; i++) {
			testColumns[i] = new Column("TEST_COLUMN_" + i, typeValues[i]);
		}
		
		assertTrue(!conn.containsTable(testTable));
		conn.createTable(testTable, testColumns);
		assertTrue(conn.containsTable(testTable));
		
		conn.dropTable(testTable);	// Cleanup
	}
	@Test
	public void testDropTable() throws DuplicateTableException, NullTableException, SQLException, ClosedException {
		String testTable = "TEST_TABLE_DROP";
		
		if (conn.containsTable(testTable))	// Clear stale test table from a previous run, if exists
			conn.dropTable(testTable);
		
		conn.createTable(testTable, new Column[]{new Column("TEST_COLUMN", SqlType.BOOLEAN)});
		
		assertTrue(conn.containsTable(testTable));
		conn.dropTable(testTable);
		assertTrue(!conn.containsTable(testTable));
	}
	
	@Test
	public void testContainsTable() throws DuplicateTableException, SQLException, NullTableException, ClosedException {
		String testTable = "TEST_TABLE_CONTAINS";
		
		conn.dropTable(testTable);
		
		assertTrue(!conn.containsTable(testTable));
		conn.createTable(testTable, new Column[]{new Column("TEST_COLUMN", SqlType.BOOLEAN)});
		assertTrue(conn.containsTable(testTable));
		
		conn.dropTable(testTable);
	}
	
	@Test
	public void testGetTables() throws ClosedException {
		for (String table : conn.getTables()) {
			System.out.println(table);
		}
	}
}
