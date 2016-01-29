package dev.kkorolyov.dbbrowser.statement;

import dev.kkorolyov.dbbrowser.column.PGColumn;
import dev.kkorolyov.dbbrowser.logging.DBLogger;

/**
 * Single class for all custom statement formatting.
 */
public class StatementBuilder {
	private static final DBLogger log = DBLogger.getLogger(StatementBuilder.class.getName());
	
	private static final String selectStatement = "SELECT " + Marker.columns + " FROM " + Marker.table;	// No criteria
	private static final String criteriaAddOn = " WHERE " + Marker.criteria;	// Criteria addon for SELECT
	private static final String insertStatement = "INSERT INTO " + Marker.table + " VALUES " + Marker.values;
	
	/**
	 * Builds a SELECT statement with additional criteria.
	 * @param table table to call statement on
	 * @param columns column(s) to return; if any column = "*", will use a wildcard for result columns
	 * @param criteria specified as columns with certain values, added in the order specified; if {@code null} or empty, will not add any criteria
	 * @return formatted SELECT statement
	 */
	public static String buildSelect(String table, String[] columns, PGColumn[] criteria) {
		log.debug("Building SELECT statement");
		
		String statement = selectStatement.replaceFirst(Marker.table, table);	// Set table
		
		statement = statement.replaceFirst(Marker.columns, buildSelectColumns(columns));	// Set columns
		
		if (criteria != null && criteria.length > 0) {
			statement += criteriaAddOn;
			statement = statement.replaceFirst(Marker.criteria, buildSelectCriteriaMarkers(criteria));	// Set criteria '?'s
		}
		log.debug(	"Built SELECT statement:"
							+ "\t" + statement);
		
		return statement;
	}
	private static String buildSelectColumns(String[] columns) {
		log.debug("Adding " + String.valueOf(columns.length) + " columns to SELECT statement");
		
		StringBuilder selectColumns = new StringBuilder();
		String delimeter = ",";
		
		for (int i = 0; i < columns.length; i++) {
			if (columns[i].equals(Marker.wildcard)) {
				log.debug("Found wildcard '" + columns[i] + "', returning");

				return Marker.wildcard;	// If any column is a wildcard, use a wildcard for statement
			}
			selectColumns.append(columns[i]).append(delimeter);	// Append "<column>," to delimit columns
		}
		selectColumns.replace(selectColumns.length() - delimeter.length(), selectColumns.length(), "");	// Remove final delimiter
		
		return selectColumns.toString();
	}
	private static String buildSelectCriteriaMarkers(PGColumn[] criteria) {
		log.debug("Adding " + String.valueOf(criteria.length) + " criterion markers to SELECT statement");
		
		StringBuilder selectCriteria = new StringBuilder();
		String marker = "=?", delimeter = " AND ";
		
		for (int i = 0; i < criteria.length; i++) {
			selectCriteria.append(criteria[i].getName()).append(marker).append(delimeter);	// Append "<criteria>=? AND " to delimit criteria
		}
		selectCriteria.replace(selectCriteria.length() - delimeter.length(), selectCriteria.length(), "");	// Remove final delimiter
		
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
		
		String insert = insertStatement.replaceFirst(Marker.values, buildInsertValuesMarkers(numValues));	// Set values markers
		
		log.debug(	"Built INSERT statement:"
							+ "\t" + insert);
		
		return insert;
	}
	private static String buildInsertValuesMarkers(int numMarkers) {
		log.debug("Adding " + String.valueOf(numMarkers) + " value markers to INSERT statement");
		
		StringBuilder insertValues = new StringBuilder("(");	// Values declared within parentheses
		String marker = "?", delimeter = ",";
		
		for (int i = 0; i < numMarkers; i++) {
			insertValues.append(marker).append(delimeter);	// Append "?," to delimit values
		}
		insertValues.replace(insertValues.length() - delimeter.length(), insertValues.length(), ")");	// Replace final delimiter with closing parenthesis
		
		return insertValues.toString();
	}
	
	/**
	 * Markers to easily replace stock statement properties.
	 */
	private class Marker {
		private static final String table = "<TABLE>", columns = "<COLUMNS>", criteria = "<CRITERIA>", values = "<VALUES>";	// To easily replace statement segments in functions
		private static final String wildcard = "*";
	}
}
