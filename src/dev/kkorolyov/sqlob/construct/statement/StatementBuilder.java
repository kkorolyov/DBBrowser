package dev.kkorolyov.sqlob.construct.statement;

import dev.kkorolyov.sqlob.construct.Column;
import dev.kkorolyov.sqlob.construct.RowEntry;
import dev.kkorolyov.sqlob.logging.Logger;
import dev.kkorolyov.sqlob.logging.LoggerInterface;

/**
 * Provides methods for constructing strings for use in SQL statements.
 */
public class StatementBuilder {
	private static final LoggerInterface log = Logger.getLogger(StatementBuilder.class.getName());
	
	private static final String createStatement = "CREATE TABLE " + Marker.table + " " + Marker.columns;
	private static final String dropStatement = "DROP TABLE " + Marker.table;
	
	private static final String	selectStatement = "SELECT " + Marker.columns + " FROM " + Marker.table,
															criteriaAddOn = " WHERE " + Marker.criteria;	// Appended to SELECT when criteria specified
	private static final String insertStatement = "INSERT INTO " + Marker.table + " " + Marker.columns + " VALUES " + Marker.values;
	private static final String deleteStatement = "DELETE FROM " + Marker.table + " WHERE " + Marker.criteria;
	private static final String updateStatement = "UPDATE " + Marker.table + " SET " + Marker.columns + "=" + Marker.values + " WHERE " + Marker.criteria;
	
	private static final String wildcard = "*";
	
	/**
	 * Builds a CREATE TABLE statement. 
	 * @param table new table name
	 * @param columns new table columns
	 * @return formatted CREATE TABLE statement
	 */
	public static String buildCreate(String table, Column[] columns) {
		log.debug("Building CREATE TABLE statement...");
		
		String statement = createStatement.replaceFirst(Marker.table, table);	// Set table
		
		statement = statement.replaceFirst(Marker.columns, buildCreateColumns(columns));	// Set columns
		
		log.debug("Built CREATE TABLE statement:"
							+ System.lineSeparator() + "\t" + statement);
		
		return statement;
	}
	private static String buildCreateColumns(Column[] columns) {
		StringBuilder createColumns = new StringBuilder("(");	// Column declaration start
		String delimeterMid = " ", delimeterEnd = ",";	// Mid is in between name and type, end is after column declaration
		
		log.debug("Adding " + columns.length + " columns to CREATE statement");
		
		for (Column column : columns) {
			createColumns.append(column.getName()).append(delimeterMid).append(column.getType().getTypeName()).append(delimeterEnd);	// Append "<name> <type>,"
		}
		replaceFinalDelimeter(createColumns, delimeterEnd, ")");	// Close columns
		
		return createColumns.toString();
	}
	
	/**
	 * Builds a DROP TABLE statement.
	 * @param table table to drop
	 * @return formatted DROP TABLE statement
	 */
	public static String buildDrop(String table) {
		log.debug("Building DROP TABLE statement...");
		
		String statement = dropStatement.replaceFirst(Marker.table, table);	// Only thing to set
		
		log.debug("Built DROP TABLE statement:"
							+	System.lineSeparator() + "\t" + statement);
		
		return statement;
	}
	
	/**
	 * Builds a SELECT statement.
	 * @param table table to select from
	 * @param columns columns to return; if {@code null}, empty, or any column == "*", all columns are returned
	 * @param criteria criteria to match when selecting columns; if {@code null} or empty, no criteria is used
	 * @return formatted SELECT statement
	 */
	public static String buildSelect(String table, Column[] columns, RowEntry[] criteria) {
		log.debug("Building SELECT statement...");
		
		String statement = selectStatement.replaceFirst(Marker.table, table);	// Set table
		
		statement = statement.replaceFirst(Marker.columns, buildSelectColumns(columns));	// Set columns
		
		if (criteria != null && criteria.length > 0) {
			statement += criteriaAddOn;
			statement = statement.replaceFirst(Marker.criteria, buildSelectCriteriaMarkers(criteria));	// Set criteria '?'s
		}
		log.debug("Built SELECT statement:"
							+ System.lineSeparator() + "\t" + statement);
		
		return statement;
	}
	private static String buildSelectColumns(Column[] columns) {
		if (columns == null || columns.length <= 0) {
			log.debug("No columns to add, returning wildcard '" + wildcard + "'");
			return wildcard;
		}
		
		log.debug("Adding " + String.valueOf(columns.length) + " columns to SELECT statement");
		
		StringBuilder selectColumns = new StringBuilder();
		String delimeter = ",";
		
		for (int i = 0; i < columns.length; i++) {
			String columnName = columns[i].getName();
			
			if (columnName.equals(wildcard)) {
				log.debug("Found wildcard '" + columns[i] + "', returning");

				return wildcard;	// If any column is a wildcard, use a wildcard for statement
			}
			selectColumns.append(columns[i].getName()).append(delimeter);	// Append "<column>," to delimit columns
		}
		replaceFinalDelimeter(selectColumns, delimeter, "");	// Remove final delimiter
		
		return selectColumns.toString();
	}
	private static String buildSelectCriteriaMarkers(RowEntry[] criteria) {
		log.debug("Adding " + String.valueOf(criteria.length) + " criterion markers to SELECT statement");	// TODO Used for delete, update as well, extract into more generic method
		
		StringBuilder selectCriteria = new StringBuilder();
		String delimeter = " AND ";
		
		for (int i = 0; i < criteria.length; i++) {
			selectCriteria.append(criteria[i].getSql()).append(delimeter);
		}
		replaceFinalDelimeter(selectCriteria, delimeter, "");	// Remove final delimiter
		
		return selectCriteria.toString();
	}
	
	/**
	 * Builds an INSERT statement.
	 * @param table table to insert into
	 * @param entries entries to add
	 * @return formatted INSERT statement
	 */
	public static String buildInsert(String table, RowEntry[] entries) {
		log.debug("Building INSERT statement...");
		
		String statement = insertStatement.replaceFirst(Marker.table, table);	// Set table
		
		String[] columnsValues = buildInsertColumnsValues(entries);
		
		statement = statement.replaceFirst(Marker.columns, columnsValues[0]);	// Set column names
		statement = statement.replaceFirst(Marker.values, columnsValues[1]);	// Set values markers
		
		log.debug("Built INSERT statement:"
							+ System.lineSeparator() + "\t" + statement);
		
		return statement;
	}
	private static String[] buildInsertColumnsValues(RowEntry[] entries) {	// TODO Used for update as well, extract into more generic method
		log.debug("Adding " + String.valueOf(entries.length) + " values to INSERT statement");
		
		StringBuilder insertColumns = new StringBuilder("("), insertValues = new StringBuilder("(");	// Columns, values declared within parentheses

		String marker = "?", delimeter = ",";
		
		for (int i = 0; i < entries.length; i++) {
			insertColumns.append(entries[i].getColumn().getName()).append(delimeter);	// Append "<column>," to delimit columns
			insertValues.append(marker).append(delimeter);	// Append "?," to delimit values
		}
		replaceFinalDelimeter(insertColumns, delimeter, ")");	// Replace final delimiter with closing parenthesis
		replaceFinalDelimeter(insertValues, delimeter, ")");	// Replace final delimiter with closing parenthesis
		
		return new String[]{insertColumns.toString(), insertValues.toString()};
	}
	
	/**
	 * Builds a DELETE statement.
	 * @param table table to delete rows from
	 * @param criteria criteria to match when deleting rows; if {@code null} or empty, no criteria is used
	 * @return formatted DELETE statement
	 */
	public static String buildDelete(String table, RowEntry[] criteria) {
		log.debug("Building DELETE statement...");
		
		String statement = deleteStatement.replace(Marker.table, table);
		
		statement = statement.replaceFirst(Marker.criteria, buildSelectCriteriaMarkers(criteria));	// Set criteria markers
		
		log.debug("Built DELETE statement:"
							+ System.lineSeparator() + "\t" + statement);
		
		return statement;
	}
	
	/**
	 * Builds an UPDATE statement.
	 * @param table table to update
	 * @param newEntries new entries to use
	 * @param criteria criteria to match when updating entries; if {@code null} or empty, no criteria is used
	 * @return formatted UPDATE statement
	 */
	public static String buildUpdate(String table, RowEntry[] newEntries, RowEntry[] criteria) {
		log.debug("Building UPDATE statement...");
		
		String statement = updateStatement.replaceFirst(Marker.table, table);
		
		String[] columnsValues = buildInsertColumnsValues(newEntries);
		
		statement = statement.replaceFirst(Marker.columns, columnsValues[0]);	// Set column names
		statement = statement.replaceFirst(Marker.values, columnsValues[1]);	// Set values markers
		statement = statement.replaceFirst(Marker.criteria, buildSelectCriteriaMarkers(criteria));	// Set criteria markers
		
		log.debug("Built UPDATE statement:"
							+ System.lineSeparator() + "\t" + statement);
		
		return statement;
	}
	
	private static void replaceFinalDelimeter(StringBuilder built, String delimeter, String replaceWith) {
		built.replace(built.length() - delimeter.length(), built.length(), replaceWith);
	}
	
	// Markers to easily replace stock statement properties.
	private class Marker {
		private static final String table = "<TABLE>", columns = "<COLUMNS>", criteria = "<CRITERIA>", values = "<VALUES>";	// To easily replace statement segments in functions
	}
}
