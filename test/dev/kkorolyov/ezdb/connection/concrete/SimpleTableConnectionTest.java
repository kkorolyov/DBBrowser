package dev.kkorolyov.ezdb.connection.concrete;

import static org.junit.Assert.assertEquals;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import dev.kkorolyov.ezdb.column.PGColumn;
import dev.kkorolyov.ezdb.connection.DBConnection;
import dev.kkorolyov.ezdb.connection.TableConnection;
import dev.kkorolyov.ezdb.connection.concrete.SimpleDBConnection;
import dev.kkorolyov.ezdb.connection.concrete.SimpleTableConnection;
import dev.kkorolyov.ezdb.exceptions.DuplicateTableException;
import dev.kkorolyov.ezdb.exceptions.NullTableException;
import dev.kkorolyov.ezdb.logging.DBLogger;

@SuppressWarnings("javadoc")
public class SimpleTableConnectionTest {
	private static final String TEST_HOST = "192.168.1.157", TEST_DB = "TEST_DB", TEST_TABLE = "TEST_TABLE";

	private static DBConnection dbConn;
	private TableConnection conn;
	
	@Before
	public void setUp() throws NullTableException, SQLException, DuplicateTableException{
		dbConn = new SimpleDBConnection(TEST_HOST, TEST_DB);
		if (!dbConn.containsTable(TEST_TABLE)) {
			String[] testColumnNames = {"BOOLEAN", "INTEGER", "VARCHAR"};
			boolean testBoolean = (Math.random() < .5) ? false : true;	// Random boolean
			int testInt = (int) Math.random() * 100;	// Random int 0-99
			String testString = "TEST_STRING";
			
			PGColumn[] columns = {new PGColumn(testColumnNames[0], PGColumn.Type.BOOLEAN, testBoolean),
																new PGColumn(testColumnNames[1], PGColumn.Type.INTEGER, testInt),
																new PGColumn(testColumnNames[2], PGColumn.Type.VARCHAR, testString)};		
			
			dbConn.createTable(TEST_TABLE, columns);
		}
		conn = new SimpleTableConnection(dbConn, TEST_TABLE);
		
		DBLogger.enableAll();
	}

	@After
	public void tearDown() {
		conn.close();
	}

	@Test
	public void testSelect() throws SQLException, DuplicateTableException, NullTableException {
		String[] testColumnNames = {"BOOLEAN", "INTEGER", "VARCHAR"};
		boolean testBoolean = (Math.random() < .5) ? false : true;	// Random boolean
		int testInt = (int) Math.random() * 100;	// Random int 0-99
		String testString = "TEST_STRING";
		
		PGColumn[] testColumns = {new PGColumn(testColumnNames[0], PGColumn.Type.BOOLEAN, testBoolean),
															new PGColumn(testColumnNames[1], PGColumn.Type.INTEGER, testInt),
															new PGColumn(testColumnNames[2], PGColumn.Type.VARCHAR, testString)};		
		
		DBConnection dbConn = new SimpleDBConnection(TEST_HOST, TEST_DB);
		dbConn.dropTable(TEST_TABLE);
		conn = dbConn.createTable(TEST_TABLE, testColumns);
		
		ResultSet rs = conn.select(testColumnNames);

		while (rs.next()) {
			boolean actualBoolean = rs.getBoolean(testColumnNames[0]);
			int actualInt = rs.getInt(testColumnNames[1]);
			String actualString = rs.getString(testColumnNames[2]);
			
			assertEquals(testBoolean, actualBoolean);
			System.out.println(String.valueOf(actualBoolean));
			assertEquals(testInt, actualInt);
			System.out.println(String.valueOf(actualInt));
			assertEquals(testString, actualString);
			System.out.println(actualString);
		}
	}
	
	@Test
	public void testInsert() throws SQLException {
		conn.insert(new Object[]{true, 5, "Thing"});
	}
}
