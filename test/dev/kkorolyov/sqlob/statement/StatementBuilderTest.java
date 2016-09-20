package dev.kkorolyov.sqlob.statement;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import dev.kkorolyov.sqlob.construct.Column;
import dev.kkorolyov.sqlob.construct.MismatchedTypeException;
import dev.kkorolyov.sqlob.construct.RowEntry;
import dev.kkorolyov.sqlob.construct.SqlType;
import dev.kkorolyov.sqlob.construct.statement.StatementBuilder;

@SuppressWarnings("javadoc")
public class StatementBuilderTest {
	
	@Test
	public void testBuildCreate() {
		String testTable = "Test Table";
		List<Column> testColumns = buildAllColumns();
		
		String columnsMarker = "<COLUMNS>";
		String expectedStatement = insertCreateColumns("CREATE TABLE " + testTable + " (" + columnsMarker + ")", columnsMarker, testColumns);
		String actualStatement = StatementBuilder.buildCreate(testTable, testColumns);
		
		assertEquals(expectedStatement, actualStatement);
	}
	private static String insertCreateColumns(String baseStatement, String columnsMarker, List<Column> columns) {
		StringBuilder columnsString = new StringBuilder();
		
		for (Column column : columns)
			columnsString.append(column.getName()).append(" ").append(column.getType().getTypeName()).append(",");
		
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
		List<Column> testColumns = buildAllColumns();
		
		String columnsMarker = "<COLUMNS>";
		String expectedStatement = insertSelectColumns("SELECT " + columnsMarker + " FROM " + testTable, columnsMarker, testColumns);
		String actualStatement = StatementBuilder.buildSelect(testTable, testColumns, null);
		
		assertEquals(expectedStatement, actualStatement);
	}
	@Test
	public void testBuildSelectColumnsCriteria() throws MismatchedTypeException {
		String testTable = "Test Table";
		List<Column> testColumns = buildAllColumns();
		List<RowEntry> testCriteria = buildAllCriteria();
		
		String columnsMarker = "<COLUMNS>", criteriaMarker = "<CRITERIA>";
		String expectedStatement = insertSelectCriteria(insertSelectColumns("SELECT " + columnsMarker + " FROM " + testTable + " WHERE " + criteriaMarker, columnsMarker, testColumns), criteriaMarker, testCriteria);
		String actualStatement = StatementBuilder.buildSelect(testTable, testColumns, testCriteria);
	
		assertEquals(expectedStatement, actualStatement);
	}
	private static String insertSelectColumns(String baseStatement, String columnsMarker, List<Column> columns) {
		StringBuilder columnsString = new StringBuilder();
		
		for (Column column : columns)
			columnsString.append(column.getName()).append(",");

		return baseStatement.replaceFirst(columnsMarker, columnsString.substring(0, columnsString.length() - 1));	// Removes trailing ","
	}
	private static String insertSelectCriteria(String baseStatement, String criteriaMarker, List<RowEntry> criteria) {
		StringBuilder criteriaString = new StringBuilder();
		
		for (RowEntry criterion : criteria)
			criteriaString.append(criterion.getColumn().getName()).append("=? AND ");

		return baseStatement.replaceFirst(criteriaMarker, criteriaString.substring(0, criteriaString.length() - 5));	// Removes trailing " AND "
	}
	
	@Test
	public void testBuildInsert() throws MismatchedTypeException {
		String testTable = "Test Table";
		List<RowEntry> testEntries = buildAllCriteria();
		
		String columnsMarker = "<COLUMNS>", valuesMarker = "<VALUES>";
		String expectedStatment = insertSelectColumnsValues("INSERT INTO " + testTable + " " + columnsMarker + " VALUES " + valuesMarker, columnsMarker, valuesMarker, testEntries);
		String actualStatement = StatementBuilder.buildInsert(testTable, testEntries);
		
		assertEquals(expectedStatment, actualStatement);
	}
	private static String insertSelectColumnsValues(String baseStatement, String columnsMarker, String valuesMarker, List<RowEntry> entries) {
		StringBuilder columnsString = new StringBuilder("("), valuesString = new StringBuilder("(");
		
		for (int i = 0; i < entries.size() - 1; i++) {
			columnsString.append(entries.get(i).getColumn().getName() + ",");
			valuesString.append("?,");
		}
		columnsString.append(entries.get(entries.size() - 1).getColumn().getName() + ")");
		valuesString.append("?)");
		
		return baseStatement.replaceFirst(columnsMarker, columnsString.toString()).replaceFirst(valuesMarker, valuesString.toString());
	}
	
	@Test
	public void testBuildDelete() throws MismatchedTypeException {
		String testTable = "Test Table";
		List<RowEntry> criteria = buildAllCriteria();
		
		String criteriaMarker = "<CRITERIA>";
		String expectedStatement = insertSelectCriteria("DELETE FROM " + testTable + " WHERE " + criteriaMarker, criteriaMarker, criteria);	// TODO Generalize insertSelectCriteria()
		String actualStatement = StatementBuilder.buildDelete(testTable, criteria);
		
		assertEquals(expectedStatement, actualStatement);
	}
	
	@Test
	public void testBuildUpdate() throws MismatchedTypeException {
		String testTable = "Test Table";
		List<RowEntry> newEntries = buildAllCriteria();	// TODO Use different entries from criteria
		List<RowEntry> criteria = buildAllCriteria();
		
		String columnsMarker = "<COLUMNS>", valuesMarker = "<VALUES>", criteriaMarker = "<CRITERIA>";
		String expectedStatement = insertSelectCriteria(insertSelectColumnsValues("UPDATE " + testTable + " SET " + columnsMarker + "=" + valuesMarker + " WHERE " + criteriaMarker, columnsMarker, valuesMarker, newEntries), criteriaMarker, criteria);
		String actualStatement = StatementBuilder.buildUpdate(testTable, newEntries, criteria);
		
		assertEquals(expectedStatement, actualStatement);
	}
	
	private static List<Column> buildAllColumns() {
		SqlType[] allTypes = SqlType.values();
		List<Column> columns = new LinkedList<>();
		
		for (SqlType type : allTypes)
			columns.add(new Column(type.getTypeName(), type));
		
		return columns;
	}
	
	private static List<RowEntry> buildAllCriteria() throws MismatchedTypeException {
		List<Column> columns = buildAllColumns();
		List<RowEntry> criteria = new LinkedList<>();
		
		Map<SqlType, Object> matchedTypes = buildMatchingTypesMap();
		
		for (Column column : columns)
			criteria.add(new RowEntry(column, matchedTypes.get(column.getType())));

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
