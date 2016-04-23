package dev.kkorolyov.sqlob.connection.concrete;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import dev.kkorolyov.simplelogs.Logger;
import dev.kkorolyov.simpleprops.Properties;
import dev.kkorolyov.sqlob.connection.DatabaseConnection;
import dev.kkorolyov.sqlob.connection.TableConnection;
import dev.kkorolyov.sqlob.construct.Column;
import dev.kkorolyov.sqlob.construct.Results;
import dev.kkorolyov.sqlob.construct.RowEntry;
import dev.kkorolyov.sqlob.construct.SqlType;

@SuppressWarnings("javadoc")
public class SimpleTableConnectionTest {	// TODO Better tests
	private static Properties props = Properties.getInstance("SimpleProps.txt");
	private static final String host = props.getValue("HOST"), database = props.getValue("DATABASE"), user = props.getValue("USER"), password = props.getValue("PASSWORD"), table = props.getValue("TABLE");
	private static DatabaseConnection dbConn;

	private final Column[] columns = buildAllColumns();
	
	private TableConnection conn;
	
	@Before
	public void setUp() throws Exception{
		dbConn = new SimpleDatabaseConnection(host, database, user, password);
		
		if (dbConn.containsTable(table))
			dbConn.dropTable(table);
		dbConn.createTable(table, columns);
		
		conn = new SimpleTableConnection(dbConn, table);
		
		Logger.enableAll();
	}

	@After
	public void tearDown() {
		if (dbConn.containsTable(table))
			dbConn.dropTable(table);
		
		conn.close();
	}

	@Test
	public void testSelect() throws Exception {
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
