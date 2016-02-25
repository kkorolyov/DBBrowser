package dev.kkorolyov.ezdb.construct;

import static org.junit.Assert.*;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import dev.kkorolyov.ezdb.connection.DatabaseConnection;
import dev.kkorolyov.ezdb.connection.TableConnection;
import dev.kkorolyov.ezdb.connection.concrete.SimpleDatabaseConnection;
import dev.kkorolyov.ezdb.connection.concrete.SimpleTableConnection;
import dev.kkorolyov.ezdb.exceptions.ClosedException;
import dev.kkorolyov.ezdb.exceptions.MismatchedTypeException;
import dev.kkorolyov.ezdb.logging.DebugLogger;

@SuppressWarnings("javadoc")
public class ResultsTest {
	private static final String host = "192.168.47.3", database = "TEST_DB", TEST_USER = "postgres", TEST_PASSWORD = "";

	private static String testTable = "RESULTS_TEST_TABLE";
	private static DatabaseConnection databaseConn;
	private static TableConnection tableConn;
	
	private Results results;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {		
		databaseConn = new SimpleDatabaseConnection(host, database, TEST_USER, TEST_PASSWORD);
		databaseConn.createTable(testTable, buildAllColumns());
		
		tableConn = new SimpleTableConnection(databaseConn, testTable);
		tableConn.insert(buildAllEntries());
		
		DebugLogger.enableAll();
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		databaseConn.dropTable(testTable);
		databaseConn.close();
	}

	@After
	public void tearDown() throws Exception {
		tableConn.flush();
	}

	@Test
	public void testClose() throws SQLException {		
		try {
			results = tableConn.select(buildAllColumns());
			
			results.getColumns();
		} catch (ClosedException e) {
			fail("Closed early");
		}
		results.close();
		try {
			results.getColumns();
		} catch (ClosedException e) {
			return;	// Success
		}
		fail("Did not close");
	}

	@Test
	public void testIsClosed() throws SQLException, ClosedException {
		results = tableConn.select(buildAllColumns());
		
		assertTrue(!results.isClosed());
		results.close();
		assertTrue(results.isClosed());
	}

	@Test
	public void testGetColumns() throws SQLException, ClosedException {
		Column[] inColumns = buildAllColumns();
		
		results = tableConn.select(inColumns);
		Column[] outColumns = results.getColumns();
		
		for (int i = 0; i < outColumns.length; i++) {
			assertEquals(inColumns[i].getName().toUpperCase(), outColumns[i].getName().toUpperCase());	// To ignore case
			assertEquals(inColumns[i].getType(), outColumns[i].getType());
		}
	}

	@Test
	public void testGetNumColumns() throws SQLException, ClosedException {
		Column[] columns = buildAllColumns();
		int numColumns = columns.length;
		
		results = tableConn.select(columns);
		
		assertEquals(numColumns, results.getNumColumns());
	}

	@Test
	public void testGetNextRow() throws SQLException, ClosedException {
		results = tableConn.select(buildAllColumns());
		
		assertTrue(results.getNextRow() != null);	// 1st row
		assertTrue(results.getNextRow() == null);	// Only 1 row
	}

	private static Column[] buildAllColumns() {
		SqlType[] allTypes = SqlType.values();
		Column[] allColumns = new Column[allTypes.length];
		
		for (int i = 0; i < allColumns.length; i++)
			allColumns[i] = new Column(allTypes[i].toString(), allTypes[i]);
		
		return allColumns;
	}
	private static RowEntry[] buildAllEntries() throws MismatchedTypeException {
		Column[] allColumns = buildAllColumns();
		RowEntry[] allEntries = new RowEntry[allColumns.length];
		
		Map<SqlType, Object> matchedTypes = buildMatchedTypes();
		for (int i = 0; i < allEntries.length; i++) {
			allEntries[i] = new RowEntry(allColumns[i], matchedTypes.get(allColumns[i].getType()));
		}
		return allEntries;
	}
	private static Map<SqlType, Object> buildMatchedTypes() {
		Map<SqlType, Object> matchedTypes = new HashMap<>();
		
		matchedTypes.put(SqlType.BOOLEAN, false);
		
		matchedTypes.put(SqlType.SMALLINT, (short) 0);
		matchedTypes.put(SqlType.INTEGER, 0);
		matchedTypes.put(SqlType.BIGINT, (long) 0);
		matchedTypes.put(SqlType.REAL, (float) 0.0);
		matchedTypes.put(SqlType.DOUBLE, 0.0);
		
		matchedTypes.put(SqlType.CHAR, 'A');
		matchedTypes.put(SqlType.VARCHAR, "String");
		
		assert (matchedTypes.size() == SqlType.values().length);
		
		return matchedTypes;
	}
}
