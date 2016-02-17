package dev.kkorolyov.ezdb.construct;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import dev.kkorolyov.ezdb.logging.DebugLogger;

/**
 * Results obtained from a SQL query.
 * Wraps a {@code ResultSet} object and converts its results to a list of {@code RowEntry[]} objects with values converted to the appropriate Java type.
 * Maintains a cursor initially positioned directly before the first row of results.
 * @see ResultSet
 * @see RowEntry
 */
public class Results {
	private static final DebugLogger log = DebugLogger.getLogger(Results.class.getName());
	
	private ResultSet rs;
	private List<RowEntry[]> rowEntries = new LinkedList<>();
	private int cursor = -1;	// Directly before first row
	
	/**
	 * Constructs a new {@code Results} object from a {@code ResultSet}.
	 * @param resultSet result set to wrap
	 */
	public Results(ResultSet resultSet) {
		this.rs = resultSet;
	}
	
	/**
	 * Retrieves the column information (name and type) associated with these results.
	 * @return all columns
	 */
	public Column[] getColumns() {
		
	}
	
	/**
	 * Retrieves the next row of results.
	 * @return next row of results, or {@code null} if no more rows
	 */
	public RowEntry[] getNextRow() {
		
	}
	public RowEntry[] getPreviousRow() {
		
	}
	
	
	/** @return total number of columns */
	public int getNumColumns() {
		int numColumns = -1;
		try {
			numColumns = rs.getMetaData().getColumnCount();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return numColumns;
	}
	public int getNumRows() {
		
	}
}
