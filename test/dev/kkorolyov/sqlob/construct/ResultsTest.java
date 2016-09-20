package dev.kkorolyov.sqlob.construct;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import dev.kkorolyov.sqlob.TestAssets;
import dev.kkorolyov.sqlob.connection.ClosedException;
import dev.kkorolyov.sqlob.connection.DatabaseConnection;
import dev.kkorolyov.sqlob.connection.DatabaseConnection.DatabaseType;
import dev.kkorolyov.sqlob.connection.TableConnection;

@SuppressWarnings("javadoc")
public class ResultsTest {
	private static final String HOST = TestAssets.host(),
															DATABASE = TestAssets.database(),
															USER = TestAssets.user(),
															PASSWORD = TestAssets.password();	
	private static final DatabaseType DATABASE_TYPE = DatabaseType.POSTGRESQL;

	private static String testTable = "RESULTS_TEST_TABLE";
	private static DatabaseConnection databaseConn;
	private static TableConnection tableConn;
	
	private Results results;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {		
		databaseConn = new DatabaseConnection(HOST, DATABASE, DATABASE_TYPE, USER, PASSWORD);
		databaseConn.createTable(testTable, buildAllColumns());
		
		tableConn = new TableConnection(databaseConn, testTable);
		tableConn.insert(buildAllEntries());		
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		databaseConn.dropTable(testTable);
		databaseConn.close();
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
		List<Column> inColumns = buildAllColumns();
		
		results = tableConn.select(inColumns);
		List<Column> outColumns = results.getColumns();
		
		for (int i = 0; i < outColumns.size(); i++) {
			assertEquals(inColumns.get(i).getName().toUpperCase(), outColumns.get(i).getName().toUpperCase());	// To ignore case
			assertEquals(inColumns.get(i).getType(), outColumns.get(i).getType());
		}
	}

	@Test
	public void testGetNumColumns() throws SQLException, ClosedException {
		List<Column> columns = buildAllColumns();
		int numColumns = columns.size();
		
		results = tableConn.select(columns);
		
		assertEquals(numColumns, results.getNumColumns());
	}

	@Test
	public void testGetNextRow() throws SQLException, ClosedException {
		results = tableConn.select(buildAllColumns());
		
		assertTrue(results.getNextRow() != null);	// 1st row
		assertTrue(results.getNextRow() == null);	// Only 1 row
	}

	private static List<Column> buildAllColumns() {
		SqlType[] allTypes = SqlType.values();
		List<Column> allColumns = new LinkedList<>();
		
		for (SqlType type : allTypes)
			allColumns.add(new Column(type.toString(), type));
		
		return allColumns;
	}
	private static List<RowEntry> buildAllEntries() throws MismatchedTypeException {
		List<Column> allColumns = buildAllColumns();
		List<RowEntry> allEntries = new LinkedList<>();
		
		Map<SqlType, Object> matchedTypes = buildMatchedTypes();
		for (Column column : allColumns)
			allEntries.add(new RowEntry(column, matchedTypes.get(column.getType())));
		
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
