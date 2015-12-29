package dev.kkorolyov.jdbmanager.connection.concrete;

import static org.junit.Assert.*;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import dev.kkorolyov.jdbmanager.connection.DBConnection;
import dev.kkorolyov.jdbmanager.connection.concrete.AutoCastingDBConnection;

public class AutoCastingDBConnectionTest {
	private static final String TEST_HOST = "192.168.56.101", TEST_DB = "TEST_DB", TEST_TABLE = "test_table";
	private static final String TEST_INT_COL = "test_int", TEST_STRING_COL = "test_string", TEST_BOOLEAN_COL = "test_boolean";
	private static final int TEST_INT_VAL = 0;
	private static final String TEST_STRING_VAL = "0";
	private static final boolean TEST_BOOLEAN_VAL = false;
	private static final String VALIDITY_STATEMENT = "SELECT";	// Will work as long as connection is open and valid
	
	private DBConnection conn;
	
	@Before
	public void setUp() {
		conn = new AutoCastingDBConnection(TEST_HOST, TEST_DB);	// Use a fresh connection for each test
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
		String testString = "SELECT * FROM " + TEST_TABLE + " WHERE " + TEST_INT_COL + " = ? AND " + TEST_STRING_COL + " = ? AND " + TEST_BOOLEAN_COL + " = ?";
		ResultSet rs = conn.execute(testString, 1, "test", false);
		rs.next();
		System.out.println(rs.getInt(1) + ", " + rs.getString(2) + ", " + rs.getBoolean(3));
	}
	
	@Test
	public void testContainsTable() {
		
	}
	
	@Test
	public void testGetTables() {
		String[] tables = conn.getTables();
		for (String table : tables)
			System.out.println(table);
	}
}
