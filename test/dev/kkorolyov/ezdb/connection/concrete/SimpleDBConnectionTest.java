package dev.kkorolyov.ezdb.connection.concrete;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.SQLException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import dev.kkorolyov.ezdb.column.Column;
import dev.kkorolyov.ezdb.connection.DBConnection;
import dev.kkorolyov.ezdb.connection.concrete.SimpleDBConnection;
import dev.kkorolyov.ezdb.exceptions.DuplicateTableException;
import dev.kkorolyov.ezdb.exceptions.NullTableException;

@SuppressWarnings("javadoc")
public class SimpleDBConnectionTest {
	private static final String TEST_HOST = "192.168.1.157", TEST_DB = "TEST_DB";
	
	private DBConnection conn;
	
	@Before
	public void setUp() throws SQLException, DuplicateTableException, NullTableException {		
		conn = new SimpleDBConnection(TEST_HOST, TEST_DB);	// Use a fresh connection for each test
	}
	@After
	public void tearDown() throws NullTableException, SQLException {	
		conn.close();	// Make sure all resources release after each test
		conn = null;
	}
	
	@Test
	public void testClose() throws SQLException {
		String validityStatement = "SELECT";	// Will work as long as connection is open and valid
		try {
			conn.execute(validityStatement);	// Connection is open
		} catch (SQLException e) {
			fail("Statement execution failed");
		}
		conn.close();
		
		try {
			conn.execute(validityStatement);
		} catch (NullPointerException e) {	// Should not be able to hit SQLException if resource access nullified
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
	public void testCreateTable() throws DuplicateTableException, NullTableException, SQLException {
		String testTable = "TEST_TABLE_CREATE";
		
		if (conn.containsTable(testTable))	// Clear stale test table from a previous run, if exists
			conn.dropTable(testTable);
		
		Column.Type[] typeValues = Column.Type.values();
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
	public void testDropTable() throws DuplicateTableException, NullTableException, SQLException {
		String testTable = "TEST_TABLE_DROP";
		
		if (conn.containsTable(testTable))	// Clear stale test table from a previous run, if exists
			conn.dropTable(testTable);
		
		conn.createTable(testTable, new Column[]{new Column("TEST_COLUMN", Column.Type.BOOLEAN)});
		
		assertTrue(conn.containsTable(testTable));
		conn.dropTable(testTable);
		assertTrue(!conn.containsTable(testTable));
	}
	
	@Test
	public void testContainsTable() throws DuplicateTableException, SQLException, NullTableException {
		String testTable = "TEST_TABLE_CONTAINS";
		
		assertTrue(!conn.containsTable(testTable));
		conn.createTable(testTable, new Column[]{new Column("TEST_COLUMN", Column.Type.BOOLEAN)});
		assertTrue(conn.containsTable(testTable));
		
		conn.dropTable(testTable);
	}
	
	@Test
	public void testGetTables() {
		for (String table : conn.getTables()) {
			System.out.println(table);
		}
	}
}
