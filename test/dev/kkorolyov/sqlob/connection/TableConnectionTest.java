package dev.kkorolyov.sqlob.connection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

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
public class TableConnectionTest {
	private static Properties props = Properties.getInstance("SimpleProps.txt");
	private static final String HOST_IP_ADDRESS = props.getValue("HOST"),
															DATABASE_NAME = props.getValue("DATABASE"),
															USER_NAME = props.getValue("USER"),
															PASSWORD = props.getValue("PASSWORD");
	private static DatabaseConnection dbConn;

	private TableConnection conn;
	
	@Before
	public void setUp() throws Exception {
		dbConn = new DatabaseConnection(HOST_IP_ADDRESS, DATABASE_NAME, USER_NAME, PASSWORD);
	}
	@After
	public void tearDown() {
		dbConn.close();
	}
	
	@Test
	public void testClose() throws SQLException {
		String testTable = "testTable_Close";
		Column[] testColumns = new Column[]{new Column("TestColumn1", SqlType.BOOLEAN)};
		
		conn = refreshTable(testTable, testColumns);
		
		conn.getColumns();	// Connection is open
		
		conn.close();
		
		try {
			conn.getColumns();
		} catch (ClosedException e) {
			return;	// As expected
		}
		fail("Resources failed to close");
	}
	
	@Test
	public void testIsClosed() throws SQLException {
		String testTable = "testTable_IsClosed";
		Column[] testColumns = new Column[]{new Column("TestColumn1", SqlType.BOOLEAN)};
		
		conn = refreshTable(testTable, testColumns);

		assertTrue(!conn.isClosed());
		
		conn.close();
		
		assertTrue(conn.isClosed());
	}
	
	@Test
	public void testSelect() throws Exception {
		String testTable = "testTable_Select";
		Column[] testColumns = new Column[]{new Column("TestColumn1", SqlType.BOOLEAN)};
		
		conn = refreshTable(testTable, testColumns);
		
		Results results = conn.select(testColumns);
		Column[] resultColumns = results.getColumns();
		
		assertEquals(testColumns.length, resultColumns.length);
		
		for (int i = 0; i < testColumns.length; i++) {
			assertEquals(testColumns[i], resultColumns[i]);
		}
		dbConn.dropTable(testTable);
	}
	@Test
	public void testSelectParameters() {
		String testTable = "testTable_SelectParams";
		Column[] testColumns = new Column[]{new Column("TestColumn1", SqlType.BOOLEAN)};
		
		conn = refreshTable(testTable, testColumns);
		// TODO Complete
	}
	
	@Test
	public void testInsert() throws Exception {
		RowEntry[] testEntries = buildAllEntries();
		Column[] testColumns = new Column[testEntries.length];
		for (int i = 0; i < testColumns.length; i++)
			testColumns[i] = testEntries[i].getColumn();
		
		conn.insert(testEntries);
		
		RowEntry[] retrievedEntries = null;
		try (Results results = conn.select(testColumns)) {
			retrievedEntries = results.getNextRow();
		}
		for (int i = 0; i < retrievedEntries.length; i++)
			assertEquals(testEntries[i], retrievedEntries[i]);	// Input entries should be the same as selected entries
	}
	
	private static TableConnection refreshTable(String table, Column[] columns) {
		dbConn.dropTable(table);
		return dbConn.createTable(table, columns);
	}
	
	private static Column[] buildAllColumns() {
		SqlType[] allTypes = SqlType.values();
		Column[] allColumns = new Column[allTypes.length];
		
		for (int i = 0; i < allColumns.length; i++)
			allColumns[i] = new Column(allTypes[i].toString(), allTypes[i]);
		
		return allColumns;
	}
	private static RowEntry[] buildAllEntries() throws Exception {
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
