package dev.kkorolyov.sqlob.connection;

import static org.junit.Assert.*;

import java.io.File;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import dev.kkorolyov.sqlob.TestAssets;
import dev.kkorolyov.sqlob.construct.*;

@SuppressWarnings("javadoc")
@RunWith(Parameterized.class)
public class TableConnectionTest {
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
	private DatabaseConnection dbConn;
	private TableConnection conn;
	
	public TableConnectionTest(File input, File expected) {
		attributes = new DatabaseAttributes(input);
	}
	
	@Before
	public void setUp() throws Exception {
		dbConn = new DatabaseConnection(HOST, DATABASE, attributes, USER, PASSWORD);
	}
	@After
	public void tearDown() {
		dbConn.close();
	}
	
	@Test
	public void testClose() throws SQLException {
		String testTable = "TestTable_Close";
		List<Column> testColumns = Arrays.asList(new Column("TestColumn1", SqlType.BOOLEAN));
		
		conn = refreshTable(testTable, testColumns);
		
		conn.getColumns();	// Connection is open
		
		dbConn.dropTable(testTable);

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
		String testTable = "TestTable_IsClosed";
		List<Column> testColumns = Arrays.asList(new Column("TestColumn1", SqlType.BOOLEAN));
		
		conn = refreshTable(testTable, testColumns);

		assertTrue(!conn.isClosed());
		
		dbConn.dropTable(testTable);
		
		conn.close();
		
		assertTrue(conn.isClosed());
	}
	
	@Test
	public void testSelect() throws Exception {
		String testTable = "TestTable_Select";
		List<Column> testColumns = buildAllColumns();
		
		conn = refreshTable(testTable, testColumns);
		
		Results results = conn.select(testColumns);
		List<Column> resultColumns = results.getColumns();
		
		assertEquals(testColumns.size(), resultColumns.size());
		
		for (int i = 0; i < testColumns.size(); i++) {
			assertEquals(testColumns.get(i), resultColumns.get(i));
		}
		dbConn.dropTable(testTable);
	}
	@Test
	public void testSelectParameters() throws SQLException, MismatchedTypeException {
		String testTable = "TestTable_SelectParams";
		List<Column> testColumns = Arrays.asList(new Column("TestColumn1", SqlType.BOOLEAN));
		
		conn = refreshTable(testTable, testColumns);
		
		conn.insert(Arrays.asList(new Entry(testColumns.get(0), true)));
		
		Results select0 = conn.select(testColumns, Arrays.asList(new Entry(testColumns.get(0), false))),
						select1 = conn.select(testColumns, Arrays.asList(new Entry(testColumns.get(0), true)));
		
		assertNull(select0.getNextRow());
		assertNotNull(select1.getNextRow());
		
		dbConn.dropTable(testTable);
	}
	
	@Test
	public void testInsert() throws SQLException, MismatchedTypeException {
		String testTable = "TestTable_Insert";
		List<Column> testColumns = buildAllColumns();
		
		conn = refreshTable(testTable, testColumns);
		
		assertEquals(1, conn.insert(buildMatchingEntries(testColumns)));
		assertEquals(1, conn.getNumRows());
		
		dbConn.dropTable(testTable);
	}
	
	@Test
	public void testDelete() throws SQLException, MismatchedTypeException {
		String testTable = "TestTable_Delete";
		List<Column> testColumns = buildAllColumns();
		
		conn = refreshTable(testTable, testColumns);
		
		conn.insert(buildMatchingEntries(testColumns));
		
		Column deleteColumn = new Column(SqlType.BOOLEAN.toString(), SqlType.BOOLEAN);
		boolean existsBool = (boolean) TestAssets.getMatchedType(deleteColumn.getType()),
						notExistsBool = !existsBool;
		
		assertEquals(1, conn.getNumRows());
		
		int delete0 = conn.delete(Arrays.asList(new Entry(deleteColumn, notExistsBool)));
		assertEquals(1, conn.getNumRows());
		
		int delete1 = conn.delete(Arrays.asList(new Entry(deleteColumn, existsBool)));
		assertEquals(0, conn.getNumRows());
		
		assertEquals(0, delete0);
		assertEquals(1, delete1);
		
		dbConn.dropTable(testTable);
	}
	
	@Test
	public void testUpdate() throws SQLException, MismatchedTypeException {
		String testTable = "TestTable_Update";
		List<Column> testColumns = buildAllColumns();
		
		conn = refreshTable(testTable, testColumns);
		
		conn.insert(buildMatchingEntries(testColumns));
		
		Column updateColumn = new Column(SqlType.BOOLEAN.toString(), SqlType.BOOLEAN);
		boolean preUpdate = (boolean) TestAssets.getMatchedType(updateColumn.getType()),
						postUpdate = !preUpdate;
		Entry 	preUpdateEntry = new Entry(updateColumn, preUpdate),
							postUpdateEntry = new Entry(updateColumn, postUpdate);
		
		assertEquals(1, conn.getNumRows());
		assertNotNull(conn.select(Arrays.asList(updateColumn), Arrays.asList(preUpdateEntry)).getNextRow());
		assertNull(conn.select(Arrays.asList(updateColumn), Arrays.asList(postUpdateEntry)).getNextRow());
		
		conn.update(Arrays.asList(postUpdateEntry), Arrays.asList(postUpdateEntry));	// No match
		
		assertEquals(1, conn.getNumRows());
		assertNotNull(conn.select(Arrays.asList(updateColumn), Arrays.asList(preUpdateEntry)).getNextRow());
		assertNull(conn.select(Arrays.asList(updateColumn), Arrays.asList(postUpdateEntry)).getNextRow());
		
		conn.update(Arrays.asList(postUpdateEntry), Arrays.asList(preUpdateEntry));	// Match
		
		assertEquals(1, conn.getNumRows());
		assertNull(conn.select(Arrays.asList(updateColumn), Arrays.asList(preUpdateEntry)).getNextRow());
		assertNotNull(conn.select(Arrays.asList(updateColumn), Arrays.asList(postUpdateEntry)).getNextRow());
		
		dbConn.dropTable(testTable);
	}
	
	@Test
	public void testGetColumns() {
		String testTable = "TestTable_GetColumns";
		List<Column> testColumns = buildAllColumns();
		
		conn = refreshTable(testTable, testColumns);
		
		assertEquals(testColumns, conn.getColumns());
		
		dbConn.dropTable(testTable);
	}
	
	private TableConnection refreshTable(String table, List<Column> columns) {
		dbConn.dropTable(table);
		return dbConn.createTable(table, columns);
	}
	
	private List<Column> buildAllColumns() {
		SqlType[] allTypes = SqlType.values(dbType);
		List<Column> allColumns = new LinkedList<>();
		
		for (SqlType type : allTypes)
			allColumns.add(new Column(type.toString(), type));
		
		return allColumns;
	}
	private static List<Entry> buildMatchingEntries(List<Column> columns) throws MismatchedTypeException {
		List<Entry> allEntries = new LinkedList<>();
		
		for (Column column : columns)
			allEntries.add(new Entry(column, TestAssets.getMatchedType(column.getType())));
		
		return allEntries;
	}
}
