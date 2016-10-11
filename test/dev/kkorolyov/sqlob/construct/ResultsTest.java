package dev.kkorolyov.sqlob.construct;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.sql.*;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import dev.kkorolyov.sqlob.TestAssets;
import dev.kkorolyov.sqlob.connection.ClosedException;
import dev.kkorolyov.sqlob.connection.DatabaseAttributes;
import dev.kkorolyov.sqlob.connection.DatabaseAttributes.DatabaseTypes;

@RunWith(Parameterized.class)
@SuppressWarnings("javadoc")
public class ResultsTest {	// Requires DB connection
	private static final String TABLE = ResultsTest.class.getSimpleName();
	private static final String HOST = TestAssets.host(),
															DATABASE = TestAssets.database(),
															USER = TestAssets.user(),
															PASSWORD = TestAssets.password();
	
	@Parameters(name = "SqlobFile({0})")
	public static Object[] data() {
		return new Object[]{new File("sqlobfiles/postgresql.sqlob"),
												new File("sqlobfiles/sqlite.sqlob")};
	}
	private final Connection conn;
	private final DatabaseTypes types;

	public ResultsTest(File input) throws SQLException {
		DatabaseAttributes attributes = new DatabaseAttributes(input);
		types = attributes.getTypes();
		
		conn = DriverManager.getConnection(attributes.getURL(HOST, DATABASE), USER, PASSWORD);
	}
	
	@Before
	public void setUp() throws SQLException {
		try (Statement s = conn.createStatement()) {
			s.execute(buildCreateTable());
		}
	}
	@After
	public void tearDown() throws SQLException {
		try (Statement s = conn.createStatement()) {
			s.execute(buildDropTable());
		}
		conn.close();
	}
	@AfterClass
	public static void tearDownClass() {
		TestAssets.clean();
	}
	
	@Test
	public void testClose() {
		try (Results results = new Results(getStubResultSet(), types)) {
			assertFalse(results.isClosed());
			results.close();
			assertTrue(results.isClosed());
			
			try {
				results.getColumns();
			} catch (ClosedException e) {
				try {
					results.getNextRow();
				} catch (ClosedException e1) {
					return;	// Expected
				}
			}
			fail("No ClosedException thrown");
		}
	}
	@Test
	public void testIsClosed() {
		try (Results results = new Results(getStubResultSet(), types)) {
			assertFalse(results.isClosed());
		}
	}
	
	@Test
	public void testGetColumns() {
		ResultSet rs = getStubResultSet();
		
		try (Results results = new Results(rs, types)) {
			ResultSetMetaData rsmd = rs.getMetaData();	// Expected from this
			List<Column> columns = results.getColumns();
			
			assertEquals(rsmd.getColumnCount(), columns.size());
			
			for (int i = 0; i < rsmd.getColumnCount(); i++)
				assertEquals(new Column(rsmd.getTableName(i + 1), rsmd.getColumnName(i + 1), types.get(rsmd.getColumnType(i + 1))), columns.get(i));

		} catch (SQLException e) {
			e.printStackTrace();	// Should not happen
		}
	}
	
	@Test
	public void testGetNumColumns() {
		ResultSet rs = getStubResultSet();
		
		try (Results results = new Results(rs, types)) {
			assertEquals(rs.getMetaData().getColumnCount(), results.getNumColumns());
		} catch (SQLException e) {
			e.printStackTrace();	// Should not happen
		}
	}
	
	@Test
	public void testGetNextRow() {
		ResultSet rs = getStubResultSet();
		
		try (Results results = new Results(rs, types)) {
			ResultSetMetaData rsmd = rs.getMetaData();
			List<Entry> row;
			
			while ((row = results.getNextRow()) != null) {
				for (int i = 0; i < rsmd.getColumnCount(); i++) {
					assertEquals(new Column(rsmd.getTableName(i + 1), rsmd.getColumnName(i + 1), types.get(rsmd.getColumnType(i + 1))), row.get(i).getColumn());
					assertEquals(rs.getObject(i + 1), row.get(i).getValue());
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();	// Should not happen
		}
	}
	
	private ResultSet getStubResultSet() {
		ResultSet rs = null;
		try {
			rs = conn.createStatement().executeQuery(buildSelect());
		} catch (SQLException e) {
			e.printStackTrace();	// Should not happen
		}
		return rs;
	}
	
	private String buildCreateTable() {
		StringBuilder builder = new StringBuilder("CREATE TABLE " + TABLE + " (");
		
		for (SqlobType type : types)
			builder.append(System.lineSeparator()).append(type.getTypeClass().getSimpleName()).append(" ").append(type.getTypeName()).append(",");
		
		builder.replace(builder.length() - 1, builder.length(), ")");
		
		return builder.toString();
	}
	private static String buildDropTable() {
		return "DROP TABLE " + TABLE;
	}
	private static String buildSelect() {
		return "SELECT * FROM " + TABLE;
	}
}
