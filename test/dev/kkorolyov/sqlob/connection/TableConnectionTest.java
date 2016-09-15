package dev.kkorolyov.sqlob.connection;

import static org.junit.Assert.*;

import java.sql.SQLException;
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
import dev.kkorolyov.sqlob.connection.DatabaseConnection.DatabaseType;
import dev.kkorolyov.sqlob.construct.*;

@SuppressWarnings("javadoc")
@RunWith(Parameterized.class)
public class TableConnectionTest {
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
	
	private final DatabaseType dbType;
	private DatabaseConnection dbConn;
	private TableConnection conn;
	
	public TableConnectionTest(DatabaseType input, DatabaseType expected) {
		dbType = input;
	}
	
	@Before
	public void setUp() throws Exception {
		dbConn = new DatabaseConnection(HOST, DATABASE, dbType, USER, PASSWORD);
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
		boolean existsBool = (boolean) TestAssets.getMatchedType(deleteColumn.getType()),
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
		boolean preUpdate = (boolean) TestAssets.getMatchedType(updateColumn.getType()),
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
	
	private TableConnection refreshTable(String table, Column[] columns) {
		dbConn.dropTable(table);
		return dbConn.createTable(table, columns);
	}
	
	private Column[] buildAllColumns() {
		SqlType[] allTypes = SqlType.values(dbType);
		Column[] allColumns = new Column[allTypes.length];
		
		for (int i = 0; i < allColumns.length; i++)
			allColumns[i] = new Column(allTypes[i].toString(), allTypes[i]);
		
		return allColumns;
	}
	private static RowEntry[] buildMatchingEntries(Column[] columns) throws MismatchedTypeException {
		RowEntry[] allEntries = new RowEntry[columns.length];
		
		for (int i = 0; i < allEntries.length; i++) {
			allEntries[i] = new RowEntry(columns[i], TestAssets.getMatchedType(columns[i].getType()));
		}
		return allEntries;
	}
}
