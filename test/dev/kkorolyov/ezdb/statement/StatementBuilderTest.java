package dev.kkorolyov.ezdb.statement;

import static org.junit.Assert.*;

import org.junit.Test;

import dev.kkorolyov.ezdb.construct.Column;
import dev.kkorolyov.ezdb.construct.SqlType;

@SuppressWarnings("javadoc")
public class StatementBuilderTest {

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
	private static String insertSelectColumns(String baseStatement, String columnsMarker, Column[] columns) {
		StringBuilder columnsString = new StringBuilder();
		
		for (Column column : columns) {
			columnsString.append(column.getName()).append(",");
		}
		
		return baseStatement.replaceFirst(columnsMarker, columnsString.substring(0, columnsString.length() - 1));	// Removes trailing ","
	}

	@Test
	public void testBuildInsert() {
		fail("Not yet implemented");
	}
	
	private static Column[] buildAllColumns() {
		SqlType[] allTypes = SqlType.values();
		Column[] columns = new Column[allTypes.length];
		
		for (int i = 0; i < columns.length; i++)
			columns[i] = new Column(allTypes[i].getTypeName(), allTypes[i]);
		
		return columns;
	}
}
