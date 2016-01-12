package dev.kkorolyov.dbbrowser.connection.concrete;

import static org.junit.Assert.*;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import dev.kkorolyov.dbbrowser.connection.DBConnection;
import dev.kkorolyov.dbbrowser.connection.concrete.SimpleDBConnection;

public class SimpleDBConnectionTest {
	private static final String TEST_HOST = "192.168.1.157", TEST_DB = "TEST_DB", TEST_TABLE = "TEST_TABLE";
	
	private static final int TEST_INT_COL_NUM = 1, TEST_STRING_COL_NUM = 2, TEST_BOOLEAN_COL_NUM = 3;
	private static final String TEST_INT_COL_NAME = "TEST_INT", TEST_STRING_COL_NAME = "TEST_STRING", TEST_BOOLEAN_COL_NAME = "TEST_BOOLEAN";
	
	private static final String VALIDITY_STATEMENT = "SELECT";	// Will work as long as connection is open and valid

	private static final int TEST_INT_VAL = 0;
	private static final String TEST_STRING_VAL = "0";
	private static final boolean TEST_BOOLEAN_VAL = false;
	
	private DBConnection conn;
	
	@Before
	public void setUp() throws SQLException {
		conn = new SimpleDBConnection(TEST_HOST, TEST_DB);	// Use a fresh connection for each test
	}
	@After
	public void tearDown() {
		conn.close();	// Make sure all resources release after each test
	}
	
	@Test
	public void testClose() {
		try {
			conn.execute(VALIDITY_STATEMENT);
		} catch (SQLException e) {
			fail("Statement execution failed");
		}
		
		conn.close();
		
		try {
			conn.execute(VALIDITY_STATEMENT);
		} catch (NullPointerException | SQLException e) {
			return;
		}
		fail("Statement execution failed to fail");
	}

	@Test
	public void testExecute() throws SQLException {
		String testString = "SELECT";
		conn.execute(testString);
	}
	@Test
	public void testExecuteParams() throws SQLException {
		String testString = "SELECT * FROM " + TEST_TABLE + " WHERE " + TEST_INT_COL_NAME + " = ? AND " + TEST_STRING_COL_NAME + " = ? AND " + TEST_BOOLEAN_COL_NAME + " = ?";
		ResultSet rs = conn.execute(testString, new Object[]{TEST_INT_VAL, TEST_STRING_VAL, TEST_BOOLEAN_VAL});
		rs.next();	// Move to row 1;
		
		assertEquals(TEST_INT_VAL, rs.getInt(TEST_INT_COL_NUM));
		assertEquals(TEST_STRING_VAL, rs.getString(TEST_STRING_COL_NUM));
		assertEquals(TEST_BOOLEAN_VAL, rs.getBoolean(TEST_BOOLEAN_COL_NUM));
	}
	
	@Test
	public void testContainsTable() {
		assertTrue(conn.containsTable(TEST_TABLE));
		assertTrue(!conn.containsTable("NOT_A_TABLE"));
	}
	
	@Test
	public void testGetTables() {
		String[] testTables = conn.getTables();
		assertEquals(1, testTables.length);
		assertTrue(TEST_TABLE.equalsIgnoreCase(testTables[0]));
	}
}
