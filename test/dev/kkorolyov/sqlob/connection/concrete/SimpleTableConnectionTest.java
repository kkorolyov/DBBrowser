package dev.kkorolyov.sqlob.connection.concrete;

import static org.junit.Assert.*;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import dev.kkorolyov.sqlob.connection.DatabaseConnection;
import dev.kkorolyov.sqlob.connection.TableConnection;
import dev.kkorolyov.sqlob.connection.concrete.SimpleDatabaseConnection;
import dev.kkorolyov.sqlob.connection.concrete.SimpleTableConnection;
import dev.kkorolyov.sqlob.construct.Column;
import dev.kkorolyov.sqlob.construct.Results;
import dev.kkorolyov.sqlob.construct.RowEntry;
import dev.kkorolyov.sqlob.construct.SqlType;
import dev.kkorolyov.sqlob.exceptions.ClosedException;
import dev.kkorolyov.sqlob.exceptions.DuplicateTableException;
import dev.kkorolyov.sqlob.exceptions.MismatchedTypeException;
import dev.kkorolyov.sqlob.exceptions.NullTableException;
import dev.kkorolyov.sqlob.logging.DebugLogger;
import dev.kkorolyov.sqlob.properties.Properties;

@SuppressWarnings("javadoc")
public class SimpleTableConnectionTest {	// TODO Better tests
	private static final String host = Properties.getValue(Properties.HOST), database = "TEST_DB", user = Properties.getValue(Properties.USER), password = Properties.getValue(Properties.PASSWORD);
	private static DatabaseConnection dbConn;

	private final String table = "TABLE_TEST_TABLE";
	private final Column[] columns = buildAllColumns();
	
	private TableConnection conn;
	
	@Before
	public void setUp() throws NullTableException, DuplicateTableException, ClosedException, SQLException{
		dbConn = new SimpleDatabaseConnection(host, database, user, password);
		
		if (dbConn.containsTable(table))
			dbConn.dropTable(table);
		dbConn.createTable(table, columns);
		
		conn = new SimpleTableConnection(dbConn, table);
		
		DebugLogger.enableAll();
	}

	@After
	public void tearDown() throws ClosedException, NullTableException {
		if (dbConn.containsTable(table))
			dbConn.dropTable(table);
		
		conn.close();
	}

	@Test
	public void testClose() {
		//T\ TODO
	}
	
	@Test
	public void testSelect() throws SQLException, DuplicateTableException, NullTableException, ClosedException {
		Results results = conn.select(columns);
		Column[] resultColumns = results.getColumns();
		
		for (int i = 0; i < columns.length; i++) {
			assertEquals(columns[i], resultColumns[i]);
		}
	}
	@Test
	public void testSelectParameters() {
		// TODO
	}
	
	@Test
	public void testInsert() throws SQLException, MismatchedTypeException, ClosedException {
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
