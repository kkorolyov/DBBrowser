package dev.kkorolyov.ezdb.statement;

import dev.kkorolyov.ezdb.column.Column;
import dev.kkorolyov.ezdb.logging.DBLogger;

/**
 * Provides methods for constructing strings for use in SQL statements.
 */
public class StatementBuilder {
	private static final DBLogger log = DBLogger.getLogger(StatementBuilder.class.getName());
	
	private static final String createStatement = "CREATE TABLE " + Marker.table + " " + Marker.columns;
	private static final String dropStatement = "DROP TABLE " + Marker.table;
	
	private static final String	selectStatement = "SELECT " + Marker.columns + " FROM " + Marker.table,
															criteriaAddOn = " WHERE " + Marker.criteria;	// Appended to SELECT when criteria specified
	private static final String insertStatement = "INSERT INTO " + Marker.table + " VALUES " + Marker.values;
	
	private static final String wildcard = "*";
	
	/**
	 * Builds a CREATE TABLE statement. 
	 * @param table new table name
	 * @param columns new table columns
	 * @return formatted CREATE TABLE statement
	 */
	public static String buildCreate(String table, Column[] columns) {
		log.debug("Building CREATE statement");
		
		String statement = createStatement.replaceFirst(Marker.table, table);	// Set table
		
		statement = statement.replaceFirst(Marker.columns, buildCreateColumns(columns));	// Set columns
		
		log.debug(	"Built CREATE statement:"
							+ "\n\t" + statement);
		
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
		String statement = dropStatement.replaceFirst(Marker.table, table);	// Only thing to set
		
		return statement;
	}
	
	/**
	 * Builds a SELECT statement.
	 * @param table table to call statement on
	 * @param columns column(s) to return; if {@code null}, empty, or any column = "*", will use a wildcard for result columns
	 * @param criteria specified as columns with certain values, added in the order specified; if {@code null} or empty, will not add any criteria
	 * @return formatted SELECT statement
	 */
	public static String buildSelect(String table, String[] columns, Column[] criteria) {
		log.debug("Building SELECT statement");
		
		String statement = selectStatement.replaceFirst(Marker.table, table);	// Set table
		
		statement = statement.replaceFirst(Marker.columns, buildSelectColumns(columns));	// Set columns
		
		if (criteria != null && criteria.length > 0) {
			statement += criteriaAddOn;
			statement = statement.replaceFirst(Marker.criteria, buildSelectCriteriaMarkers(criteria));	// Set criteria '?'s
		}
		log.debug(	"Built SELECT statement:"
							+ "\n\t" + statement);
		
		return statement;
	}
	private static String buildSelectColumns(String[] columns) {
		if (columns == null || columns.length <= 0) {
			log.debug("No columns to add, returning wildcard '" + wildcard + "'");
			return wildcard;
		}
		
		log.debug("Adding " + String.valueOf(columns.length) + " columns to SELECT statement");
		
		StringBuilder selectColumns = new StringBuilder();
		String delimeter = ",";
		
		for (int i = 0; i < columns.length; i++) {
			if (columns[i].equals(wildcard)) {
				log.debug("Found wildcard '" + columns[i] + "', returning");

				return wildcard;	// If any column is a wildcard, use a wildcard for statement
			}
			selectColumns.append(columns[i]).append(delimeter);	// Append "<column>," to delimit columns
		}
		replaceFinalDelimeter(selectColumns, delimeter, "");	// Remove final delimiter
		
		return selectColumns.toString();
	}
	private static String buildSelectCriteriaMarkers(Column[] criteria) {
		log.debug("Adding " + String.valueOf(criteria.length) + " criterion markers to SELECT statement");
		
		StringBuilder selectCriteria = new StringBuilder();
		String marker = "=?", delimeter = " AND ";
		
		for (int i = 0; i < criteria.length; i++) {
			selectCriteria.append(criteria[i].getName()).append(marker).append(delimeter);	// Append "<criteria>=? AND " to delimit criteria
		}
		replaceFinalDelimeter(selectCriteria, delimeter, "");	// Remove final delimiter
		
		return selectCriteria.toString();
	}
	
	/**
	 * Builds an INSERT statement.
	 * @param table table to call statement on
	 * @param numValues number of value markers to add
	 * @return formatted INSERT statement
	 */
	public static String buildInsert(String table, int numValues) {
		log.debug("Building INSERT statement");
		
		String statement = insertStatement.replaceFirst(Marker.table, table);	// Set table
		
		statement = statement.replaceFirst(Marker.values, buildtInsertValuesMarkers(numValues));	// Set values markers
		
		log.debug(	"Built INSERT statement:"
							+ "\n\t" + statement);
		
		return statement;
	}
	private static String buildtInsertValuesMarkers(int numMarkers) {
		log.debug("Adding " + String.valueOf(numMarkers) + " value markers to INSERT statement");
		
		StringBuilder insertValues = new StringBuilder("(");	// Values declared within parentheses
		String marker = "?", delimeter = ",";
		
		for (int i = 0; i < numMarkers; i++) {
			insertValues.append(marker).append(delimeter);	// Append "?," to delimit values
		}
		replaceFinalDelimeter(insertValues, delimeter, ")");	// Replace final delimiter with closing parenthesis
		
		return insertValues.toString();
	}
	
	private static void replaceFinalDelimeter(StringBuilder built, String delimeter, String replaceWith) {
		built.replace(built.length() - delimeter.length(), built.length(), replaceWith);
	}
	
	/**
	 * Markers to easily replace stock statement properties.
	 */
	private class Marker {
		private static final String table = "<TABLE>", columns = "<COLUMNS>", criteria = "<CRITERIA>", values = "<VALUES>";	// To easily replace statement segments in functions
	}
}
