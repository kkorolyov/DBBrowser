package dev.kkorolyov.sqlob.statement;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import dev.kkorolyov.simplelogs.Logger;
import dev.kkorolyov.sqlob.construct.Column;
import dev.kkorolyov.sqlob.construct.RowEntry;
import dev.kkorolyov.sqlob.construct.SqlType;
import dev.kkorolyov.sqlob.exceptions.MismatchedTypeException;

@SuppressWarnings("javadoc")
public class StatementBuilderTest {

	{
		Logger.enableAll();	// Enable all loggers before testing
	}
	
	@Test
	public void testBuildCreate() {
		String testTable = "Test Table";
		Column[] testColumns = buildAllColumns();
		
		String columnsMarker = "<COLUMNS>";
		String expectedStatement = insertCreateColumns("CREATE TABLE " + testTable + " (" + columnsMarker + ")", columnsMarker, testColumns);
		String actualStatement = StatementBuilder.buildCreate(testTable, testColumns);
		
		assertEquals(expectedStatement, actualStatement);
	}
	private static String insertCreateColumns(String baseStatement, String columnsMarker, Column[] columns) {
		StringBuilder columnsString = new StringBuilder();
		
		for (Column column : columns) {
			columnsString.append(column.getName()).append(" ").append(column.getType().getTypeName()).append(",");
		}
		
		return baseStatement.replaceFirst(columnsMarker, columnsString.substring(0, columnsString.length() - 1));	// Removes trailing ","
	}
	
	@Test
	public void testBuildDrop() {
		String testTable = "Test Table";
		
		String expectedStatement = "DROP TABLE " + testTable;
		String actualStatement = StatementBuilder.buildDrop(testTable);
		
		assertEquals(expectedStatement, actualStatement);
	}

	@Test
	public void testBuildSelectAll() {
		String testTable = "Test Table";
		String columns = "*";
		
		String expectedStatement = "SELECT " + columns + " FROM " + testTable;
		String actualStatement = StatementBuilder.buildSelect(testTable, null, null);
		
		assertEquals(expectedStatement, actualStatement);
	}
	@Test
	public void testBuildSelectColumns() {
		String testTable = "Test Table";
		Column[] testColumns = buildAllColumns();
		
		String columnsMarker = "<COLUMNS>";
		String expectedStatement = insertSelectColumns("SELECT " + columnsMarker + " FROM " + testTable, columnsMarker, testColumns);
		String actualStatement = StatementBuilder.buildSelect(testTable, testColumns, null);
		
		assertEquals(expectedStatement, actualStatement);
	}
	@Test
	public void testBuildSelectColumnsCriteria() throws MismatchedTypeException {
		String testTable = "Test Table";
		Column[] testColumns = buildAllColumns();
		RowEntry[] testCriteria = buildAllCriteria();
		
		String columnsMarker = "<COLUMNS>", criteriaMarker = "<CRITERIA>";
		String expectedStatement = insertSelectCriteria(insertSelectColumns("SELECT " + columnsMarker + " FROM " + testTable + " WHERE " + criteriaMarker, columnsMarker, testColumns), criteriaMarker, testCriteria);
		String actualStatement = StatementBuilder.buildSelect(testTable, testColumns, testCriteria);
	
		assertEquals(expectedStatement, actualStatement);
	}
	private static String insertSelectColumns(String baseStatement, String columnsMarker, Column[] columns) {
		StringBuilder columnsString = new StringBuilder();
		
		for (Column column : columns) {
			columnsString.append(column.getName()).append(",");
		}
		return baseStatement.replaceFirst(columnsMarker, columnsString.substring(0, columnsString.length() - 1));	// Removes trailing ","
	}
	private static String insertSelectCriteria(String baseStatement, String criteriaMarker, RowEntry[] criteria) {
		StringBuilder criteriaString = new StringBuilder();
		
		for (RowEntry criterion : criteria) {
			criteriaString.append(criterion.getColumn().getName()).append("=? AND ");
		}
		return baseStatement.replaceFirst(criteriaMarker, criteriaString.substring(0, criteriaString.length() - 5));	// Removes trailing " AND "
	}
	
	@Test
	public void testBuildInsert() throws MismatchedTypeException {
		String testTable = "Test Table";
		RowEntry[] testEntries = buildAllCriteria();
		
		String columnsMarker = "<COLUMNS>", valuesMarker = "<VALUES>";
		String expectedStatment = insertSelectColumnsValues("INSERT INTO " + testTable + " " + columnsMarker + " VALUES " + valuesMarker, columnsMarker, valuesMarker, testEntries);
		String actualStatement = StatementBuilder.buildInsert(testTable, testEntries);
		
		assertEquals(expectedStatment, actualStatement);
	}
	private static String insertSelectColumnsValues(String baseStatement, String columnsMarker, String valuesMarker, RowEntry[] entries) {
		StringBuilder columnsString = new StringBuilder("("), valuesString = new StringBuilder("(");
		
		for (int i = 0; i < entries.length - 1; i++) {
			columnsString.append(entries[i].getColumn().getName() + ",");
			valuesString.append("?,");
		}
		columnsString.append(entries[entries.length - 1].getColumn().getName() + ")");
		valuesString.append("?)");
		
		return baseStatement.replaceFirst(columnsMarker, columnsString.toString()).replaceFirst(valuesMarker, valuesString.toString());
	}
	
	@Test
	public void testBuildDelete() throws MismatchedTypeException {
		String testTable = "Test Table";
		RowEntry[] criteria = buildAllCriteria();
		
		String criteriaMarker = "<CRITERIA>";
		String expectedStatement = insertSelectCriteria("DELETE FROM " + testTable + " WHERE " + criteriaMarker, criteriaMarker, criteria);	// TODO Generalize insertSelectCriteria()
		String actualStatement = StatementBuilder.buildDelete(testTable, criteria);
		
		assertEquals(expectedStatement, actualStatement);
	}
	
	@Test
	public void testBuildUpdate() throws MismatchedTypeException {
		String testTable = "Test Table";
		RowEntry[] newEntries = buildAllCriteria();	// TODO Use different entries from criteria
		RowEntry[] criteria = buildAllCriteria();
		
		String columnsMarker = "<COLUMNS>", valuesMarker = "<VALUES>", criteriaMarker = "<CRITERIA>";
		String expectedStatement = insertSelectCriteria(insertSelectColumnsValues("UPDATE " + testTable + " SET " + columnsMarker + " " + valuesMarker + " WHERE " + criteriaMarker, columnsMarker, valuesMarker, newEntries), criteriaMarker, criteria);
		String actualStatement = StatementBuilder.buildUpdate(testTable, newEntries, criteria);
		
		assertEquals(expectedStatement, actualStatement);
	}
	
	private static Column[] buildAllColumns() {
		SqlType[] allTypes = SqlType.values();
		Column[] columns = new Column[allTypes.length];
		
		for (int i = 0; i < columns.length; i++)
			columns[i] = new Column(allTypes[i].getTypeName(), allTypes[i]);
		
		return columns;
	}
	
	private static RowEntry[] buildAllCriteria() throws MismatchedTypeException {
		Column[] columns = buildAllColumns();
		RowEntry[] criteria = new RowEntry[columns.length];
		
		Map<SqlType, Object> matchedTypes = buildMatchingTypesMap();
		
		for (int i = 0; i < criteria.length; i++) {
			criteria[i] = new RowEntry(columns[i], matchedTypes.get(columns[i].getType()));
		}
		return criteria;
	}
	private static Map<SqlType, Object> buildMatchingTypesMap() {
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
