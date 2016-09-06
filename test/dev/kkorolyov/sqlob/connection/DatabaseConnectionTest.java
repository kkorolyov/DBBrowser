package dev.kkorolyov.sqlob.connection;

import static org.junit.Assert.*;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import dev.kkorolyov.sqlob.TestAssets;
import dev.kkorolyov.sqlob.connection.DatabaseConnection.DatabaseType;
import dev.kkorolyov.sqlob.construct.Column;
import dev.kkorolyov.sqlob.construct.RowEntry;
import dev.kkorolyov.sqlob.construct.SqlType;
import dev.kkorolyov.sqlob.construct.statement.QueryStatement;
import dev.kkorolyov.sqlob.construct.statement.UpdateStatement;

@SuppressWarnings("javadoc")
@RunWith(Parameterized.class)
public class DatabaseConnectionTest {
	@Parameters
	public static Collection<Object[]> data() {
		List<Object[]> data = new LinkedList<>();
		for (DatabaseType dbType : DatabaseType.values())
			data.add(new Object[]{dbType, dbType});
		
		return data;
	}
	
	private static final String HOST = TestAssets.host(),
															DATABASE = TestAssets.database(),
															USER = TestAssets.user(),
															PASSWORD = TestAssets.password();	

	private DatabaseType dbType;
	private DatabaseConnection conn;
	
	public DatabaseConnectionTest(DatabaseType input, DatabaseType expected) {
		dbType = input;
	}
	
	@Before
	public void setUp() throws Exception {
		conn = new DatabaseConnection(HOST, DATABASE, dbType, USER, PASSWORD);	// Use a fresh connection for each test
	}
	@After
	public void tearDown() throws Exception {
		conn.close();	// Make sure all resources release after each test
	}
	
	@Test
	public void testConnect() {
		String testTable = "TestTable_Connect";
		
		refreshTable(testTable);
		
		assertTrue(conn.connect(testTable) != null);
		
		conn.dropTable(testTable);	// Clean up
	}
	
	@Test
	public void testClose() throws Exception {
		conn.close();
		
		try {
			conn.execute(conn.getStatementFactory().getSelect("Closed", null, null));
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
	public void testExecuteQueryStatement() {
		String testTable = "TestTable_ExecuteQuery";
		Column[] testColumns = new Column[]{new Column("TestColumn1", getRandomSqlType())};
		
		refreshTable(testTable, testColumns);
		
		QueryStatement testStatement = conn.getStatementFactory().getSelect(testTable, testColumns, getStubRowEntries(testColumns));
		
		assertNotNull(conn.execute(testStatement));
		
		conn.dropTable(testTable);
	}
	@Test
	public void testExecuteUpdateStatement() {
		String testTable = "TestTable_ExecuteUpdate";
		Column[] testColumns = new Column[]{new Column("TestColumn1", getRandomSqlType())};
		
		refreshTable(testTable, testColumns);
		
		UpdateStatement testStatement = conn.getStatementFactory().getInsert(testTable, getStubRowEntries(testColumns));
		
		assertEquals(1, conn.execute(testStatement));
		
		conn.dropTable(testTable);
	}

	@Test
	public void testExecute() {	// Mainly for exceptions
		String testTable = "TestTable_Execute";
		Column[] testColumns = new Column[]{new Column("TestColumn1", getRandomSqlType())};
		
		refreshTable(testTable, testColumns);

		String 	returnTestStatement = "SELECT * FROM " + testTable + " WHERE " + testColumns[0].getName() + "=?",
						nullReturnTestStatement = "INSERT INTO " + testTable + " VALUES (?)";

		assertNotNull(conn.execute(returnTestStatement, getStubRowEntries(testColumns)));
		assertNull(conn.execute(nullReturnTestStatement, getStubRowEntries(testColumns)));
		
		conn.dropTable(testTable);
	}
	@Test
	public void testUpdate() throws Exception {
		String testTable = "TestTable_Update";
		Column[] testColumns = new Column[]{new Column("TestColumn1", getRandomSqlType())};
		
		refreshTable(testTable, testColumns);
		
		String testStatement = "INSERT INTO " + testTable + " VALUES (?)";
		int updateCount = conn.update(testStatement, getStubRowEntries(testColumns));
		
		assertEquals(1, updateCount);
		
		conn.dropTable(testTable);
	}
	
	@Test
	public void testCreateTable() throws Exception {
		String testTable = "TestTable_Create";
		Column[] testColumns = new Column[]{new Column("TestColumn1", getRandomSqlType())};

		conn.dropTable(testTable);
		
		assertTrue(!conn.containsTable(testTable));
		
		conn.createTable(testTable, testColumns);
		assertTrue(conn.containsTable(testTable));
		
		conn.dropTable(testTable);
	}
	
	@Test
	public void testDropTable() throws Exception {
		String testTable = "TestTable_Drop";
		Column[] testColumns = new Column[]{new Column("TestColumn1", getRandomSqlType())};
		
		conn.dropTable(testTable);
		
		conn.createTable(testTable, testColumns);		
		assertTrue(conn.containsTable(testTable));
		
		conn.dropTable(testTable);
		assertTrue(!conn.containsTable(testTable));
	}
	
	@Test
	public void testContainsTable() throws Exception {
		String testTable = "TestTable_Contains";
		Column[] testColumns = new Column[]{new Column("TestColumn1", getRandomSqlType())};

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
			testColumnses[i] = new Column[]{new Column("TestColumn1", getRandomSqlType())};
			
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
		assertEquals(DATABASE, conn.getDatabaseName());
	}
	@Test
	public void testGetDatabaseType() {
		assertEquals(dbType, conn.getDatabaseType());
	}
	
	private void refreshTable(String table) {
		refreshTable(table, new Column[]{new Column("TestColumn1", getRandomSqlType())});
	}
	private void refreshTable(String table, Column[] columns) {
		conn.dropTable(table);
		conn.createTable(table, columns);
	}
	
	private static SqlType getRandomSqlType() {
		return SqlType.values()[new Random().nextInt(SqlType.values().length)];
	}
	
	private static Object getMatchedType(SqlType type) {
		return TestAssets.getMatchedType(type);
	}
	
	private static RowEntry[] getStubRowEntries(Column... columns) {
		RowEntry[] entries = new RowEntry[columns.length];
		
		for (int i = 0; i < entries.length; i++)
			entries[i] = new RowEntry(columns[i], getMatchedType(columns[i].getType()));
		
		return entries;
	}
}
