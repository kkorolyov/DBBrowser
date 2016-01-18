package dev.kkorolyov.dbbrowser.connection.concrete;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import dev.kkorolyov.dbbrowser.connection.TableConnection;
import dev.kkorolyov.dbbrowser.exceptions.NullTableException;

@SuppressWarnings("javadoc")
public class SimpleTableConnectionTest {
	private static final String TEST_HOST = "192.168.1.157", TEST_DB = "TEST_DB", TEST_TABLE = "TEST_TABLE";

	private TableConnection conn;
	
	@Before
	public void setUp() throws NullTableException, SQLException{
		conn = new SimpleTableConnection(new SimpleDBConnection(TEST_HOST, TEST_DB), TEST_TABLE);
	}

	@After
	public void tearDown() {
		conn.close();
	}

	@Test
	public void testSelect() throws SQLException {
		
	}
	
	@Test
	public void testInsert() throws SQLException {
		conn.insert(new Object[]{5, "Thing", true});
	}
}
