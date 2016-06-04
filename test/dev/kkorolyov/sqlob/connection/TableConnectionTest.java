package dev.kkorolyov.sqlob.connection;

import static org.junit.Assert.*;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import dev.kkorolyov.sqlob.TestAssets;
import dev.kkorolyov.sqlob.construct.Column;
import dev.kkorolyov.sqlob.construct.Results;
import dev.kkorolyov.sqlob.construct.RowEntry;
import dev.kkorolyov.sqlob.construct.SqlType;
import dev.kkorolyov.sqlob.exceptions.ClosedException;
import dev.kkorolyov.sqlob.exceptions.MismatchedTypeException;

@SuppressWarnings("javadoc")
public class TableConnectionTest {
	private static final Map<SqlType, Object> matchedTypes = new HashMap<>();
	private static final String HOST = TestAssets.host(),
															DATABASE = TestAssets.database(),
															USER = TestAssets.user(),
															PASSWORD = TestAssets.password();	

	private static DatabaseConnection dbConn;

	private TableConnection conn;
	
	@BeforeClass
	public static void setUpBeforeClass() {
		setMatchedTypes();
	}
	private static void setMatchedTypes() {
		matchedTypes.put(SqlType.BOOLEAN, false);
		
		matchedTypes.put(SqlType.SMALLINT, (short) 0);
		matchedTypes.put(SqlType.INTEGER, 0);
		matchedTypes.put(SqlType.BIGINT, (long) 0);
		matchedTypes.put(SqlType.REAL, (float) 0.0);
		matchedTypes.put(SqlType.DOUBLE, 0.0);
		
		matchedTypes.put(SqlType.CHAR, 'A');
		matchedTypes.put(SqlType.VARCHAR, "String");
		
		assert (matchedTypes.size() == SqlType.values().length);
	}
	
	@Before
	public void setUp() throws Exception {
		dbConn = new DatabaseConnection(HOST, DATABASE, USER, PASSWORD);
	}
	@After
	public void tearDown() {
		dbConn.close();
	}
	
	@Test
	public void testClose() throws SQLException {
		String testTable = "TestTable_Close";
		Column[] testColumns = new Column[]{new Column("TestColumn1", SqlType.BOOLEAN)};
		
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
		Column[] testColumns = new Column[]{new Column("TestColumn1", SqlType.BOOLEAN)};
		
		conn = refreshTable(testTable, testColumns);

		assertTrue(!conn.isClosed());
		
		dbConn.dropTable(testTable);
		
		conn.close();
		
		assertTrue(conn.isClosed());
	}
	
	@Test
	public void testSelect() throws Exception {
		String testTable = "TestTable_Select";
		Column[] testColumns = buildAllColumns();
		
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
	public void testSelectParameters() throws SQLException, MismatchedTypeException {
		String testTable = "TestTable_SelectParams";
		Column[] testColumns = new Column[]{new Column("TestColumn1", SqlType.BOOLEAN)};
		
		conn = refreshTable(testTable, testColumns);
		
		conn.insert(new RowEntry[]{new RowEntry(testColumns[0], true)});
		
		RowEntry[] 	criteriaFalse = new RowEntry[]{new RowEntry(testColumns[0], false)},
								criteriaTrue = new RowEntry[]{new RowEntry(testColumns[0], true)};
		
		Results select0 = conn.select(testColumns, criteriaFalse),
						select1 = conn.select(testColumns, criteriaTrue);
		
		assertNull(select0.getNextRow());
		assertNotNull(select1.getNextRow());
		
		dbConn.dropTable(testTable);
	}
	
	@Test
	public void testInsert() throws SQLException, MismatchedTypeException {
		String testTable = "TestTable_Insert";
		Column[] testColumns = buildAllColumns();
		
		conn = refreshTable(testTable, testColumns);
		
		assertEquals(1, conn.insert(buildMatchingEntries(testColumns)));
		assertEquals(1, conn.getNumRows());
		
		dbConn.dropTable(testTable);
	}
	
	@Test
	public void testDelete() throws SQLException, MismatchedTypeException {
		String testTable = "TestTable_Delete";
		Column[] testColumns = buildAllColumns();
		
		conn = refreshTable(testTable, testColumns);
		
		conn.insert(buildMatchingEntries(testColumns));
		
		Column deleteColumn = new Column(SqlType.BOOLEAN.toString(), SqlType.BOOLEAN);
		boolean existsBool = (boolean) matchedTypes.get(deleteColumn.getType()),
						notExistsBool = !existsBool;
		
		assertEquals(1, conn.getNumRows());
		
		int delete0 = conn.delete(new RowEntry[]{new RowEntry(deleteColumn, notExistsBool)});
		assertEquals(1, conn.getNumRows());
		
		int delete1 = conn.delete(new RowEntry[]{new RowEntry(deleteColumn, existsBool)});
		assertEquals(0, conn.getNumRows());
		
		assertEquals(0, delete0);
		assertEquals(1, delete1);
		
		dbConn.dropTable(testTable);
	}
	
	@Test
	public void testUpdate() throws SQLException, MismatchedTypeException {
		String testTable = "TestTable_Update";
		Column[] testColumns = buildAllColumns();
		
		conn = refreshTable(testTable, testColumns);
		
		conn.insert(buildMatchingEntries(testColumns));
		
		Column updateColumn = new Column(SqlType.BOOLEAN.toString(), SqlType.BOOLEAN);
		boolean preUpdate = (boolean) matchedTypes.get(updateColumn.getType()),
						postUpdate = !preUpdate;
		RowEntry 	preUpdateEntry = new RowEntry(updateColumn, preUpdate),
							postUpdateEntry = new RowEntry(updateColumn, postUpdate);
		
		assertEquals(1, conn.getNumRows());
		assertNotNull(conn.select(new Column[]{updateColumn}, new RowEntry[]{preUpdateEntry}).getNextRow());
		assertNull(conn.select(new Column[]{updateColumn}, new RowEntry[]{postUpdateEntry}).getNextRow());
		
		conn.update(new RowEntry[]{postUpdateEntry}, new RowEntry[]{postUpdateEntry});	// No match
		
		assertEquals(1, conn.getNumRows());
		assertNotNull(conn.select(new Column[]{updateColumn}, new RowEntry[]{preUpdateEntry}).getNextRow());
		assertNull(conn.select(new Column[]{updateColumn}, new RowEntry[]{postUpdateEntry}).getNextRow());
		
		conn.update(new RowEntry[]{postUpdateEntry}, new RowEntry[]{preUpdateEntry});	// Match
		
		assertEquals(1, conn.getNumRows());
		assertNull(conn.select(new Column[]{updateColumn}, new RowEntry[]{preUpdateEntry}).getNextRow());
		assertNotNull(conn.select(new Column[]{updateColumn}, new RowEntry[]{postUpdateEntry}).getNextRow());
		
		dbConn.dropTable(testTable);
	}
	
	@Test
	public void testGetColumns() {
		String testTable = "TestTable_GetColumns";
		Column[] testColumns = buildAllColumns();
		
		conn = refreshTable(testTable, testColumns);
		
		assertArrayEquals(testColumns, conn.getColumns());
		
		dbConn.dropTable(testTable);
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
	private static RowEntry[] buildMatchingEntries(Column[] columns) throws MismatchedTypeException {
		RowEntry[] allEntries = new RowEntry[columns.length];
		
		for (int i = 0; i < allEntries.length; i++) {
			allEntries[i] = new RowEntry(columns[i], matchedTypes.get(columns[i].getType()));
		}
		return allEntries;
	}
}
