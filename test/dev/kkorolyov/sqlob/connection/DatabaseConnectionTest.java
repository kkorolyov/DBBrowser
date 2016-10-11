package dev.kkorolyov.sqlob.connection;

import static org.junit.Assert.*;

import java.io.File;
import java.util.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import dev.kkorolyov.sqlob.TestAssets;
import dev.kkorolyov.sqlob.connection.DatabaseAttributes.DatabaseTypes;
import dev.kkorolyov.sqlob.construct.Column;
import dev.kkorolyov.sqlob.construct.Entry;
import dev.kkorolyov.sqlob.construct.SqlobType;
import dev.kkorolyov.sqlob.construct.statement.QueryStatement;
import dev.kkorolyov.sqlob.construct.statement.UpdateStatement;

@RunWith(Parameterized.class)
@SuppressWarnings("javadoc")
public class DatabaseConnectionTest {
	private static final File[] sqlobFiles = new File[]{new File("sqlobfiles/postgresql.sqlob"),
																											new File("sqlobfiles/sqlite.sqlob")};
	@Parameters
	public static Collection<Object[]> data() {
		List<Object[]> data = new LinkedList<>();
		
		for (File file : sqlobFiles)
			data.add(new Object[]{file, file});
		
		return data;
	}
	private static final String HOST = TestAssets.host(),
															DATABASE = TestAssets.database(),
															USER = TestAssets.user(),
															PASSWORD = TestAssets.password();	

	private final DatabaseAttributes attributes;
	private DatabaseConnection conn;
	
	public DatabaseConnectionTest(File input, File expected) {
		attributes = new DatabaseAttributes(input);
	}
	
	@Before
	public void setUp() throws Exception {
		conn = new DatabaseConnection(HOST, DATABASE, attributes, USER, PASSWORD);	// Use a fresh connection for each test
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
		List<Column> testColumns = Arrays.asList(new Column(testTable, "TestColumn1", getRandomSqlType()));
		
		refreshTable(testTable, testColumns);
		
		QueryStatement testStatement = conn.getStatementFactory().getSelect(testTable, testColumns, getStubRowEntries(testColumns));
		
		assertNotNull(conn.execute(testStatement));
		
		conn.dropTable(testTable);
	}
	@Test
	public void testExecuteUpdateStatement() {
		String testTable = "TestTable_ExecuteUpdate";
		List<Column> testColumns = Arrays.asList(new Column(testTable, "TestColumn1", getRandomSqlType()));
		
		refreshTable(testTable, testColumns);
		
		UpdateStatement testStatement = conn.getStatementFactory().getInsert(testTable, getStubRowEntries(testColumns));
		
		assertEquals(1, conn.execute(testStatement));
		
		conn.dropTable(testTable);
	}

	@Test
	public void testExecute() {	// Mainly for exceptions
		String testTable = "TestTable_Execute";
		List<Column> testColumns = Arrays.asList(new Column(testTable, "TestColumn1", getRandomSqlType()));
		
		refreshTable(testTable, testColumns);

		String 	returnTestStatement = "SELECT * FROM " + testTable + " WHERE " + testColumns.get(0).getName() + "=?",
						nullReturnTestStatement = "INSERT INTO " + testTable + " VALUES (?)";

		assertNotNull(conn.execute(returnTestStatement, getStubRowEntries(testColumns)));
		assertNull(conn.execute(nullReturnTestStatement, getStubRowEntries(testColumns)));
		
		conn.dropTable(testTable);
	}
	@Test
	public void testUpdate() throws Exception {
		String testTable = "TestTable_Update";
		List<Column> testColumns = Arrays.asList(new Column(testTable, "TestColumn1", getRandomSqlType()));
		
		refreshTable(testTable, testColumns);
		
		String testStatement = "INSERT INTO " + testTable + " VALUES (?)";
		int updateCount = conn.update(testStatement, getStubRowEntries(testColumns));
		
		assertEquals(1, updateCount);
		
		conn.dropTable(testTable);
	}
	
	@Test
	public void testCreateTable() throws Exception {
		String testTable = "TestTable_Create";
		List<Column> testColumns = Arrays.asList(new Column(testTable, "TestColumn1", getRandomSqlType()));

		conn.dropTable(testTable);
		
		assertTrue(!conn.containsTable(testTable));
		
		conn.createTable(testTable, testColumns);
		assertTrue(conn.containsTable(testTable));
		
		conn.dropTable(testTable);
	}
	
	@Test
	public void testDropTable() throws Exception {
		String testTable = "TestTable_Drop";
		List<Column> testColumns = Arrays.asList(new Column(testTable, "TestColumn1", getRandomSqlType()));
		
		conn.dropTable(testTable);
		
		conn.createTable(testTable, testColumns);		
		assertTrue(conn.containsTable(testTable));
		
		conn.dropTable(testTable);
		assertTrue(!conn.containsTable(testTable));
	}
	
	@Test
	public void testContainsTable() throws Exception {
		String testTable = "TestTable_Contains";
		List<Column> testColumns = Arrays.asList(new Column(testTable, "TestColumn1", getRandomSqlType()));

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
		assertEquals(0, conn.getTables().size());
		
		int numTestTables = 5;
		String[] testTables = new String[numTestTables];
		
		for (int i = 0; i < numTestTables; i++) {
			testTables[i] = "TestTable_GetTables" + i;
			
			conn.createTable(testTables[i], Arrays.asList(new Column(testTables[i], "TestColumn1", getRandomSqlType())));
			assertEquals(i + 1, conn.getTables().size());
		}
		assertEquals(numTestTables, conn.getTables().size());
		
		for (String testTable : testTables)
			conn.dropTable(testTable);
		
		assertEquals(0, conn.getTables().size());
	}
	
	@Test
	public void testGetDatabaseName() {
		assertEquals(DATABASE, conn.getDatabaseName());
	}
	@Test
	public void testGetAttributes() {
		assertEquals(attributes, conn.getAttributes());
	}
	
	private void refreshTable(String table) {
		refreshTable(table, Arrays.asList(new Column(table, "TestColumn1", getRandomSqlType())));
	}
	private void refreshTable(String table, List<Column> columns) {
		conn.dropTable(table);
		conn.createTable(table, columns);
	}
	
	private SqlobType getRandomSqlType() {
		DatabaseTypes types = attributes.getTypes();
		int random = new Random().nextInt(types.size());
		
		int counter = 0;
		for (SqlobType type : types) {
			if (counter++ == random)
				return type;
		}
		return null;
	}
	
	private static Object getMatchedType(SqlobType type) {
		return TestAssets.getMatchedType(type);
	}
	
	private static List<Entry> getStubRowEntries(List<Column> columns) {
		List<Entry> entries = new LinkedList<>();
		
		for (Column column : columns)
			entries.add(new Entry(column, getMatchedType(column.getType())));
		
		return entries;
	}
}
